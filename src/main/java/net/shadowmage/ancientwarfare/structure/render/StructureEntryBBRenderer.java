package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureMap;
import net.shadowmage.ancientwarfare.structure.network.PacketShowBoundingBoxes;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

public class StructureEntryBBRenderer {
	public static final String SHOW_BBS_TAG = PacketShowBoundingBoxes.SHOW_BBS_TAG;
	private static final int BB_RENDER_RANGE = 200;

	public void handleRenderLastEvent(RenderLevelStageEvent evt) {
		if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		renderStructureBoundingBoxes(evt, mc);
	}

	private void renderStructureBoundingBoxes(RenderLevelStageEvent evt, Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || !player.getTags().contains(SHOW_BBS_TAG)) {
			return;
		}

		ClientLevel world = mc.level;
		StructureMap map = StructureMap.get(world);
		Collection<StructureEntry> structuresNear = map.getEntriesNear(world, player.blockPosition().getX(), player.blockPosition().getZ(), BB_RENDER_RANGE / 16, true, new ArrayList<>());

		PoseStack pose = evt.getPoseStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		Vec3 camera = evt.getCamera().getPosition();
		pose.pushPose();
		pose.translate(-camera.x, -camera.y, -camera.z);
		for (StructureEntry structure : structuresNear) {
			StructureBB bb = structure.getBB();
			if (bb.getCenter().distSqr(player.blockPosition()) < (BB_RENDER_RANGE * BB_RENDER_RANGE)) {
				BoxRenderers.renderBoundingBox(pose, buffers, bb.min, bb.max, Color.BLUE);
			}
		}
		pose.popPose();
		buffers.endBatch();
	}
}
