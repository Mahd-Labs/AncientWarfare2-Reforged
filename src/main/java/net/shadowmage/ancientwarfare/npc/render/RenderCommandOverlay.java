package net.shadowmage.ancientwarfare.npc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.shadowmage.ancientwarfare.core.util.EntityTools;
import net.shadowmage.ancientwarfare.core.util.RayTraceUtils;
import net.shadowmage.ancientwarfare.npc.item.ItemCommandBaton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Port notes: 1.12 used TickEvent.RenderTickEvent (START: find the target, END: draw the 2d text) and
 * RenderWorldLastEvent (boxes). 1.21.1: the target is updated once per client tick (ClientTickEvent.Pre), the text is
 * drawn in RenderGuiEvent.Post, the boxes in RenderLevelStageEvent (AFTER_TRANSLUCENT_BLOCKS) with the vanilla line
 * box renderer in camera-relative coordinates.
 */
public final class RenderCommandOverlay {
	public static final RenderCommandOverlay INSTANCE = new RenderCommandOverlay();
	/*
	 * TODO move this off into separate class for datas, as this is a _render_ class...
	 */
	private List<Entity> targetEntities;
	private HitResult target;
	private String targetString;

	private RenderCommandOverlay() {
		targetEntities = Collections.emptyList();
	}

	@SuppressWarnings("unused")
	@SubscribeEvent
	public void onClientTick(ClientTickEvent.Pre evt) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.screen != null) {
			return;
		}
		ItemStack commandBaton = EntityTools.getItemFromEitherHand(mc.player, ItemCommandBaton.class);
		if (commandBaton.isEmpty()) {
			target = null;
			targetEntities = Collections.emptyList();
			return;
		}

		target = RayTraceUtils.getPlayerTarget(mc.player, 120, 0);
		if (target != null) {
			if (target.getType() == HitResult.Type.BLOCK) {
				BlockPos pos = ((BlockHitResult) target).getBlockPos();
				targetString = pos.getX() + "," + pos.getY() + "," + pos.getZ();
			} else if (target.getType() == HitResult.Type.ENTITY) {
				targetString = ((EntityHitResult) target).getEntity().getName().getString();
			}
		}
		targetEntities = ItemCommandBaton.getCommandedEntities(mc.level, commandBaton);
	}

	@SuppressWarnings("unused")
	@SubscribeEvent
	public void onRenderGui(RenderGuiEvent.Post evt) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.screen != null) {
			return;
		}
		ItemStack commandBaton = EntityTools.getItemFromEitherHand(mc.player, ItemCommandBaton.class);
		if (commandBaton.isEmpty()) {
			return;
		}
		if (!mc.getDebugOverlay().showDebugScreen()) {
			List<String> entityNames = new ArrayList<>();
			for (Entity e : targetEntities) {
				entityNames.add(e.getName().getString());
			}
			GuiGraphics graphics = evt.getGuiGraphics();
			int x = graphics.guiWidth() - 10;
			String header = I18n.get("guistrings.npc.target");
			graphics.drawString(mc.font, header, x - mc.font.width(header), 0, 0xffffffff, true);
			if (targetString != null) {
				graphics.drawString(mc.font, targetString, x - mc.font.width(targetString), 10, 0xffffffff, true);
			}
			graphics.drawString(mc.font, I18n.get("guistrings.npc.commanded"), 10, 0, 0xffffffff, true);
			for (int i = 0; i < entityNames.size(); i++) {
				graphics.drawString(mc.font, entityNames.get(i), 10, 10 + 10 * i, 0xffffffff, true);
			}
		}
	}

	@SuppressWarnings("unused")
	@SubscribeEvent
	public void onRenderLast(RenderLevelStageEvent evt) {
		if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null || mc.player == null) {
			return;
		}
		ItemStack commandBaton = EntityTools.getItemFromEitherHand(mc.player, ItemCommandBaton.class);
		if (commandBaton.isEmpty()) {
			return;
		}
		float partialTicks = evt.getPartialTick().getGameTimeDeltaPartialTick(false);
		Vec3 camera = evt.getCamera().getPosition();
		PoseStack poseStack = evt.getPoseStack();
		MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
		poseStack.pushPose();
		poseStack.translate(-camera.x, -camera.y, -camera.z);
		HitResult pos = target;
		if (pos != null) {
			AABB bb = null;
			if (pos.getType() == HitResult.Type.BLOCK) {
				bb = new AABB(((BlockHitResult) pos).getBlockPos()).inflate(0.1d, 0.1d, 0.1d);
			} else if (pos.getType() == HitResult.Type.ENTITY && ((EntityHitResult) pos).getEntity() instanceof LivingEntity) {
				bb = interpolatedBox(((EntityHitResult) pos).getEntity(), partialTicks);
			}
			if (bb != null) {
				LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), bb, 1.f, 1.f, 1.f, 1.f);
			}
		}
		for (Entity e : targetEntities) {
			//TODO all this bb-rendering could potentially be moved to the entity itself
			LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), interpolatedBox(e, partialTicks), 1.f, 0.f, 0.f, 1.f);
		}
		poseStack.popPose();
		buffer.endBatch();
	}

	private static AABB interpolatedBox(Entity e, float partialTicks) {
		float t = 1.f - partialTicks;
		double dx = e.getX() - e.xOld;
		double dy = e.getY() - e.yOld;
		double dz = e.getZ() - e.zOld;
		return e.getBoundingBox().move(t * -dx, t * -dy, t * -dz);
	}
}
