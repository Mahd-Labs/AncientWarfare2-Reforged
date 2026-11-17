package net.shadowmage.ancientwarfare.npc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.shadowmage.ancientwarfare.npc.ai.NpcAI;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/*
 * Port notes: 1.12 RenderBiped subclass -> HumanoidMobRenderer over two ModelNpc instances (regular / slim arms,
 * vanilla player layer definitions). LayerBipedArmor -> HumanoidArmorLayer, LayerHeldItem is the ItemInHandLayer
 * HumanoidMobRenderer already adds. The name / health labels and AI task icons keep the 1.12 layout (scale, colours,
 * offsets) and are drawn with the vanilla name tag technique (camera facing pose, Font.drawInBatch twice: see-through
 * faint pass + normal pass); the icons are textured quads of the "text" render type.
 * transformHeldFull3DItemLayer has no 1.21.1 counterpart (item transforms come from the item model).
 */
public class RenderNpcBase<T extends NpcBase> extends HumanoidMobRenderer<T, ModelNpc<T>> {
	private final ModelNpc<T> smallArms;
	private final ModelNpc<T> regularArms;
	private boolean isSleeping;

	private List<Integer> renderTasks = new ArrayList<>();

	public RenderNpcBase(EntityRendererProvider.Context context) {
		super(context, new ModelNpc<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.6f);
		regularArms = getModel();
		smallArms = new ModelNpc<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
		addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
				new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
	}

	@Override
	protected void setupRotations(T npc, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
		isSleeping = npc.isSleeping();
		if (isSleeping) {
			float bedDirection = npc.getBedOrientationInDegrees();
			if (bedDirection != -1) {
				poseStack.mulPose(Axis.YP.rotationDegrees(bedDirection));
				poseStack.mulPose(Axis.ZP.rotationDegrees(this.getFlipDegrees(npc)));
				poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
				return;
			}
			isSleeping = false;
		}
		super.setupRotations(npc, poseStack, bob, yBodyRot, partialTick, scale);
	}

	@Override
	public void render(T npc, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		model = npc.getSkinSettings().renderFemaleModel(npc) ? smallArms : regularArms;


		isSleeping = npc.isSleeping();
		if (isSleeping) {
			// render the body a bit offset because we're manually shifting the bounding box
			poseStack.pushPose();
			poseStack.translate(-(npc.getBedDirection().getStepX() * 0.5), 0, -(npc.getBedDirection().getStepZ() * 0.5));
			super.render(npc, entityYaw, partialTicks, poseStack, buffer, packedLight);
			poseStack.popPose();
		} else {
			super.render(npc, entityYaw, partialTicks, poseStack, buffer, packedLight);
		}

		double y = 0;
		if (isSleeping)
			y -= 1.5f;
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		if (npc.isHostileTowards(player)) {
			if (AWNPCStatics.renderHostileNames) {
				Component name = getNameForRender(npc, true);
				if (AWNPCStatics.renderTeamColors) {
					name = addTeamDecoration(npc, player, name);
				}
				renderColoredLabel(npc, name, y, 0x20ff0000, 0xffff0000, poseStack, buffer, packedLight);
			}
		} else {
			boolean canBeCommandedBy = npc.hasCommandPermissions(player.getUUID(), player.getName().getString());
			if (AWNPCStatics.renderFriendlyNames) {
				Component name = getNameForRender(npc, false);
				if (AWNPCStatics.renderTeamColors) {
					name = addTeamDecoration(npc, player, name);
				} else if (!canBeCommandedBy) {
					name = Component.empty().append(name).withStyle(ChatFormatting.DARK_GRAY);
				}
				renderColoredLabel(npc, name, y, 0x20ffffff, 0xffffffff, poseStack, buffer, packedLight);
			}
			if (canBeCommandedBy && AWNPCStatics.renderAI) {
				renderNpcAITasks(npc, y, poseStack, buffer);
			}
		}
	}

	private Component addTeamDecoration(NpcBase npc, Player player, Component name) {
		PlayerTeam playerTeam = player.level().getScoreboard().getPlayersTeam(player.getName().getString());
		PlayerTeam npcTeam = npc.getTeam();
		if (npcTeam != null && npcTeam != playerTeam) {
			return Component.empty().append(npcTeam.getPlayerPrefix()).append(name).append(npcTeam.getPlayerSuffix());
		}
		return name;
	}

	@Override
	protected boolean shouldShowName(T par1EntityLivingBase) {
		//disables vanilla nameplate rendering, custom label rendering handled through custom rendering
		return false;
	}

	private Component getNameForRender(NpcBase npc, boolean hostile) {
		MutableComponent customName = Component.empty().append(npc.hasCustomName() ? npc.getCustomName() : npc.getName());
		boolean addHealth = (hostile && AWNPCStatics.renderHostileHealth) || (!hostile && AWNPCStatics.renderFriendlyHealth);
		if (addHealth) {
			customName.append(" " + getHealthForRender(npc));
		}
		return customName;
	}

	private String getHealthForRender(NpcBase npc) {
		return String.format("%.1f", npc.getHealth());
	}

	private boolean isLabelVisible(NpcBase entity) {
		Entity camera = entityRenderDispatcher.camera == null ? null : entityRenderDispatcher.camera.getEntity();
		if (camera == null) {
			return false;
		}
		double d3 = entity.distanceToSqr(camera);
		return d3 <= (double) (64 * 64) && entity.hasLineOfSight(camera);
	}

	private void renderNpcAITasks(NpcBase entity, double y, PoseStack poseStack, MultiBufferSource buffer) {
		if (isLabelVisible(entity)) {
			float f = 1.6F;
			float f1 = 0.016666668F * f;
			poseStack.pushPose();
			poseStack.translate(0.0F, (float) y + entity.getBbHeight() + 0.5F, 0.0F);
			poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
			poseStack.scale(f1, -f1, f1);
			int tasks = entity.getAITasks();
			int mask;
			String icon;

			for (int i = 0; i < NpcAI.NUMBER_OF_TASKS; i++) {
				mask = 1 << i;
				if ((tasks & mask) != 0) {
					renderTasks.add(mask);
				}
			}

			int offset = (renderTasks.size() * 10 / 2);
			int startX = -offset;

			for (int i = 0; i < renderTasks.size(); i++) {
				icon = getIconFor(renderTasks.get(i));
				if (icon != null) {
					renderIcon(icon, startX + i * 20, -16, poseStack, buffer);
				}
			}
			poseStack.popPose();
			this.renderTasks.clear();
		}
	}

	private void renderColoredLabel(NpcBase entity, Component string, double y, int color1, int color2, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		if (isLabelVisible(entity)) {
			Font fontrenderer = this.getFont();
			float f = 0.8F;
			float f1 = 0.016666668F * f;
			poseStack.pushPose();
			float heightScalingOffset = entity.getBbHeight() / 6 - 0.3F;
			float verticalOffset = offsetForRiddenEntity(entity, heightScalingOffset);
			poseStack.translate(0.0F, (float) y + entity.getBbHeight() + verticalOffset + 0.5F, 0.0F);
			poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
			poseStack.scale(f1, -f1, f1);
			Matrix4f matrix = poseStack.last().pose();
			int background = 0x40000000; //black, alpha 0.25
			float x = -fontrenderer.width(string) / 2F;
			fontrenderer.drawInBatch(string, x, 0, color1, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, background, packedLight);
			fontrenderer.drawInBatch(string, x, 0, color2, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
			poseStack.popPose();
		}
	}

	private float offsetForRiddenEntity(NpcBase entity, float heightScalingOffset) {
		float ret = heightScalingOffset;
		if (!entity.isPassenger()) {
			return ret;
		}

		if (entity.getVehicle() instanceof Pig) {
			ret += 0.4f;
		}

		return ret;
	}

	@Override
	public ResourceLocation getTextureLocation(T npc) {
		return npc.getTexture();
	}

	private void renderIcon(String tex, int x, int y, PoseStack poseStack, MultiBufferSource buffer) {
		VertexConsumer consumer = buffer.getBuffer(RenderType.text(ResourceLocation.parse(tex)));
		Matrix4f matrix = poseStack.last().pose();
		int halfW = 16 / 2;
		int halfH = 16 / 2;
		int light = LightTexture.FULL_BRIGHT;
		consumer.addVertex(matrix, x - halfW, y - halfH, 0).setColor(255, 255, 255, 255).setUv(0, 0).setLight(light);
		consumer.addVertex(matrix, x - halfW, y + halfH, 0).setColor(255, 255, 255, 255).setUv(0, 1).setLight(light);
		consumer.addVertex(matrix, x + halfW, y + halfH, 0).setColor(255, 255, 255, 255).setUv(1, 1).setLight(light);
		consumer.addVertex(matrix, x + halfW, y + -halfH, 0).setColor(255, 255, 255, 255).setUv(1, 0).setLight(light);
	}

	private String getIconFor(int task) {
		switch (task) {
			case 0:
				return null;
			case NpcAI.TASK_ATTACK:
				return "ancientwarfare:textures/entity/npc/ai/task_attack.png";
			case NpcAI.TASK_UPKEEP:
				return "ancientwarfare:textures/entity/npc/ai/task_upkeep.png";
			case NpcAI.TASK_IDLE_HUNGRY:
				return "ancientwarfare:textures/entity/npc/ai/task_upkeep2.png";
			case NpcAI.TASK_GO_HOME:
				return "ancientwarfare:textures/entity/npc/ai/task_home.png";
			case NpcAI.TASK_WORK:
				return "ancientwarfare:textures/entity/npc/ai/task_work.png";
			case NpcAI.TASK_PATROL:
				return "ancientwarfare:textures/entity/npc/ai/task_patrol.png";
			case NpcAI.TASK_GUARD:
				return "ancientwarfare:textures/entity/npc/ai/task_guard.png";
			case NpcAI.TASK_FOLLOW:
				return "ancientwarfare:textures/entity/npc/ai/task_follow.png";
			case NpcAI.TASK_WANDER:
				return "ancientwarfare:textures/entity/npc/ai/task_wander.png";
			case NpcAI.TASK_MOVE:
				return "ancientwarfare:textures/entity/npc/ai/task_move.png";
			case NpcAI.TASK_ALARM:
				return "ancientwarfare:textures/entity/npc/ai/task_alarm.png";
			case NpcAI.TASK_FLEE:
				return "ancientwarfare:textures/entity/npc/ai/task_flee.png";
			case NpcAI.TASK_SLEEP:
				return "ancientwarfare:textures/entity/npc/ai/task_sleep.png";
			case NpcAI.TASK_RAIN:
				return "ancientwarfare:textures/entity/npc/ai/task_rain.png";
			default:
				return null;
		}
	}

}
