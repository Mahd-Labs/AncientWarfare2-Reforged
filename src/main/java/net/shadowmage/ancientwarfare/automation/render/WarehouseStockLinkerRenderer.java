package net.shadowmage.ancientwarfare.automation.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseStockLinker;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseStockLinker.WarehouseStockFilter;

import java.util.List;

/*
 * Port note: see WarehouseStockViewerRenderer (same sign layout, shared helper methods).
 */
public class WarehouseStockLinkerRenderer implements BlockEntityRenderer<TileWarehouseStockLinker> {

	public WarehouseStockLinkerRenderer() {

	}

	@Override
	public void render(TileWarehouseStockLinker tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		WarehouseStockViewerRenderer.setupSign(tile, poseStack);
		renderSignContents(tile, poseStack, buffers);
		poseStack.popPose();
	}

	/*
	 * matrix should be setup so that 0,0 is upper-left-hand corner of the sign-board, with a
	 * transformation of 1 being 1 BLOCK
	 */
	private void renderSignContents(TileWarehouseStockLinker tile, PoseStack poseStack, MultiBufferSource buffers) {
		WarehouseStockViewerRenderer.beginSignContents(poseStack);
		ItemStack filterItem;
		WarehouseStockFilter filter;
		List<WarehouseStockFilter> filters = tile.getFilters();
		int max = filters.size();
		if (10 < max) {
			max = 10;
		}
		for (int i = 0; i < max; i++) {
			filter = filters.get(i);
			filterItem = filter.getFilterItem();
			WarehouseStockViewerRenderer.renderSignLine(poseStack, buffers, tile.getLevel(), i, filterItem, filter.getQuantity());
		}
		poseStack.popPose();
	}
}
