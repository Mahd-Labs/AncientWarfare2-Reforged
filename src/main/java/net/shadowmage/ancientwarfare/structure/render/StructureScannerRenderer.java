package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.structure.item.ItemStructureScanner;
import net.shadowmage.ancientwarfare.structure.tile.TileStructureScanner;

/*
 * Port notes: 1.12 called ((IBoxRenderer) scanner.getItem()).renderBox(...); the scanner box drawing is
 * BoxRenderers.renderStructureScanner, which wants the pose at the world origin, so the tile position is taken off.
 */
public class StructureScannerRenderer implements BlockEntityRenderer<TileStructureScanner> {

	public StructureScannerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(TileStructureScanner te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		Minecraft mc = Minecraft.getInstance();
		ItemStack scanner = te.getScannerInventory().getStackInSlot(0);
		if (mc.player != null && te.getBoundsActive() && !scanner.isEmpty() && ItemStructureScanner.readyToExport(scanner)) {
			BlockPos pos = te.getBlockPos();
			pose.pushPose();
			pose.translate(-pos.getX(), -pos.getY(), -pos.getZ());
			BoxRenderers.renderStructureScanner(pose, buffers, mc.player, scanner);
			pose.popPose();
		}
	}

	@Override
	public boolean shouldRenderOffScreen(TileStructureScanner te) {
		return true;
	}

	@Override
	public AABB getRenderBoundingBox(TileStructureScanner te) {
		return AABB.INFINITE;
	}
}
