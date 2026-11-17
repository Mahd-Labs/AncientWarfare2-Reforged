package net.shadowmage.ancientwarfare.npc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.item.ItemOrders;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/*
 * Port notes: RenderWorldLastEvent -> RenderLevelStageEvent (AFTER_TRANSLUCENT_BLOCKS). 1.12 drew with immediate mode
 * GL relative to the interpolated player position; here everything is drawn through the level's buffer source in
 * camera-relative coordinates (vanilla "lines" render type, vanilla text rendering). Same content: a white box per
 * order position, its index above it, a line between consecutive positions (alpha 0.4).
 */
public final class RenderWorkLines {
	public static final RenderWorkLines INSTANCE = new RenderWorkLines();

	private final List<BlockPos> positionList;

	private RenderWorkLines() {
		positionList = new ArrayList<>();
	}

	@SuppressWarnings("unused")
	@SubscribeEvent
	public void renderLastEvent(RenderLevelStageEvent evt) {
		if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			return;
		}
		boolean render = AWNPCStatics.renderWorkPoints;
		if (!render) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) {
			return;
		}
		ItemStack stack = player.getMainHandItem();
		Item item = stack.getItem();
		if (stack.isEmpty() || !(stack.getItem() instanceof ItemOrders)) {
			stack = player.getOffhandItem();
			item = stack.getItem();
		}
		if (stack.isEmpty() || !(stack.getItem() instanceof ItemOrders)) {
			return;
		}

		positionList.addAll(((ItemOrders) item).getPositionsForRender(stack));

		if (positionList.size() > 0) {
			renderListOfPoints(evt);
			positionList.clear();
		}
	}

	private void renderListOfPoints(RenderLevelStageEvent evt) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 camera = evt.getCamera().getPosition();
		PoseStack poseStack = evt.getPoseStack();
		MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
		poseStack.pushPose();
		poseStack.translate(-camera.x, -camera.y, -camera.z);
		BlockPos prev = null;
		int index = 1;
		for (BlockPos point : positionList) {
			AABB bb = new AABB(0, 0, 0, 1, 1, 1);
			bb = bb.move(point.getX(), point.getY(), point.getZ());
			LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), bb, 1.f, 1.f, 1.f, 1.f);
			renderTextAt(evt, poseStack, buffer, point.getX() + 0.5d, point.getY() + 1.5d, point.getZ() + 0.5d, String.valueOf(index));
			if (prev != null) {
				renderLineBetween(poseStack, buffer, point.getX() + 0.5d, point.getY() + 0.5d, point.getZ() + 0.5d, prev.getX() + 0.5d, prev.getY() + 0.5d, prev.getZ() + 0.5d);
			}
			prev = point;
			index++;
		}
		poseStack.popPose();
		buffer.endBatch();
	}

	private void renderLineBetween(PoseStack poseStack, MultiBufferSource buffer, double x1, double y1, double z1, double x2, double y2, double z2) {
		VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
		PoseStack.Pose pose = poseStack.last();
		float nx = (float) (x2 - x1);
		float ny = (float) (y2 - y1);
		float nz = (float) (z2 - z1);
		float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (length > 0) {
			nx /= length;
			ny /= length;
			nz /= length;
		}
		consumer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(1.f, 1.f, 1.f, 0.4F).setNormal(pose, nx, ny, nz);
		consumer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(1.f, 1.f, 1.f, 0.4F).setNormal(pose, nx, ny, nz);
	}

	private void renderTextAt(RenderLevelStageEvent evt, PoseStack poseStack, MultiBufferSource buffer, double x, double y, double z, String text) {
		float f = 1.6F;
		float f1 = 0.016666668F * f;
		poseStack.pushPose();
		poseStack.translate(x, y, z);
		poseStack.mulPose(Axis.YP.rotationDegrees(-evt.getCamera().getYRot()));
		poseStack.mulPose(Axis.XP.rotationDegrees(evt.getCamera().getXRot()));
		poseStack.scale(-f1, -f1, f1);
		Matrix4f matrix = poseStack.last().pose();
		Minecraft.getInstance().font.drawInBatch(text, 0, 0, 0xffffffff, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		poseStack.popPose();
	}
}
