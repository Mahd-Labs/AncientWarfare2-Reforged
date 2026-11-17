package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.shadowmage.ancientwarfare.structure.event.IBoxRenderer;

/*
 * Port notes: RenderWorldLastEvent -> RenderLevelStageEvent (AFTER_TRANSLUCENT_BLOCKS), as the other modules. The
 * pose stack is moved to the world origin before the item box is drawn (see BoxRenderers).
 */
public class StructureBoundingBoxRenderer {
	public void handleRenderLastEvent(RenderLevelStageEvent evt) {
		if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) {
			return;
		}
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.isEmpty()) {
				continue;
			}
			Item item = stack.getItem();
			if (item instanceof IBoxRenderer) {
				PoseStack pose = evt.getPoseStack();
				MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
				Vec3 camera = evt.getCamera().getPosition();
				pose.pushPose();
				pose.translate(-camera.x, -camera.y, -camera.z);
				BoxRenderers.renderBox(pose, buffers, player, hand, stack, evt.getPartialTick().getGameTimeDeltaPartialTick(false));
				pose.popPose();
				buffers.endBatch();
			}
		}
	}
}
