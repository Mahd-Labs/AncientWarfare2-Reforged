package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.shadowmage.ancientwarfare.core.util.RenderTools;
import net.shadowmage.ancientwarfare.structure.util.BlockHighlightInfo;

import java.awt.*;

public class BlockHighlightRenderer {
	private static BlockHighlightInfo blockHighlightInfo = BlockHighlightInfo.EXPIRED;

	public static void setBlockHighlightInfo(BlockHighlightInfo blockHighlightInfo) {
		BlockHighlightRenderer.blockHighlightInfo = blockHighlightInfo;
	}

	public void handleRenderLastEvent(RenderLevelStageEvent evt) {
		if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		if (mc.level.getGameTime() < blockHighlightInfo.getExpirationTime()) {
			AABB bb = new AABB(blockHighlightInfo.getPos()).expandTowards(0.1, 0.1, 0.1);
			PoseStack pose = evt.getPoseStack();
			MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
			Vec3 camera = evt.getCamera().getPosition();
			pose.pushPose();
			pose.translate(-camera.x, -camera.y, -camera.z);
			RenderTools.drawOutlinedBoundingBox(pose, buffers, bb, Color.WHITE, true);
			pose.popPose();
			buffers.endBatch();
		}
	}
}
