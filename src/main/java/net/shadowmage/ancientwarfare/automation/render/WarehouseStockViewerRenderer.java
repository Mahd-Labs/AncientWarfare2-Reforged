package net.shadowmage.ancientwarfare.automation.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseStockViewer;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseStockViewer.WarehouseStockFilter;

import javax.annotation.Nullable;
import java.util.List;

/*
 * Port notes: GlStateManager matrix calls -> PoseStack, FontRenderer.drawString -> Font.drawInBatch,
 * RenderItem.renderItemAndEffectIntoGUI -> ItemRenderer.renderStatic(GUI) at the same sign coordinates (200 units
 * per block), full bright like 1.12 RenderTools.setFullColorLightmap. The facing comes from the block state
 * (1.12: block metadata = facing ordinal). The sign layout code is shared with WarehouseStockLinkerRenderer
 * (1.12 had the same code twice).
 */
public class WarehouseStockViewerRenderer implements BlockEntityRenderer<TileWarehouseStockViewer> {

	public WarehouseStockViewerRenderer() {

	}

	@Override
	public void render(TileWarehouseStockViewer tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		setupSign(tile, poseStack);
		renderSignContents(tile, poseStack, buffers);
		poseStack.popPose();
	}

	static void setupSign(BlockEntity tile, PoseStack poseStack) {
		BlockState state = tile.getBlockState();
		Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
		Direction d = facing.getOpposite();
		float r = getRotationFromDirection(d);
		poseStack.pushPose();
		poseStack.translate(0.5f, 1.f, 0.5f);//translate the point to the top-center of the block
		poseStack.mulPose(Axis.YP.rotationDegrees(r));//rotate for rotation
		poseStack.translate(0.5f, 0, 0.5f);//translate to top-left corner
		poseStack.translate(0, -0.125f, -0.127f);//move out and down for front-face of sign
	}

	static float getRotationFromDirection(Direction d) {
		switch (d) {
			case NORTH:
				return 180.f;
			case SOUTH:
				return 0.f;
			case EAST:
				return 90.f;
			case WEST:
				return 270.f;
			default:
				return 0.f;
		}
	}

	/*
	 * matrix should be setup so that 0,0 is upper-left-hand corner of the sign-board, with a
	 * transformation of 1 being 1 BLOCK
	 */
	private void renderSignContents(TileWarehouseStockViewer tile, PoseStack poseStack, MultiBufferSource buffers) {
		beginSignContents(poseStack);
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
			renderSignLine(poseStack, buffers, tile.getLevel(), i, filterItem, filter.getQuantity());
		}
		poseStack.popPose();
	}

	static void beginSignContents(PoseStack poseStack) {
		poseStack.pushPose();
		poseStack.translate(0, 0, -0.002f);//move out from block face slightly, for depth-buffer/z-fighting
		poseStack.scale(-1, -1, -1);//rescale for gui rendering axis flip
		poseStack.scale(0.0050f, 0.0050f, 0.0050f);//this scale puts it at 200 units(pixels) per block
		poseStack.scale(1f, 1f, 0.0001f);//squash Z axis for 'flat' rendering of 3d blocks/items..LOLS
	}

	static void renderSignLine(PoseStack poseStack, MultiBufferSource buffers, @Nullable Level level, int i, ItemStack filterItem, int quantity) {
		Minecraft mc = Minecraft.getInstance();
		Font fr = mc.font;
		if (!filterItem.isEmpty()) {
			//1.12 renderItemAndEffectIntoGUI(stack, 24, i * 18 + 14)
			poseStack.pushPose();
			poseStack.translate(24 + 8, i * 18 + 14 + 8, 0);
			poseStack.scale(16.f, -16.f, 16.f);
			mc.getItemRenderer().renderStatic(filterItem, ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack, buffers, level, 0);
			poseStack.popPose();
		}
		String name = filterItem.isEmpty() ? "Empty Filter" : filterItem.getHoverName().getString();
		if (name.length() > 20) {
			name = name.substring(0, 20);
		}
		fr.drawInBatch(name, 20 + 12 + 12, i * 18 + 4 + 14, 0xffffffff, false, poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		name = String.valueOf(quantity);
		fr.drawInBatch(name, 200 - 25 - fr.width(name), i * 18 + 4 + 14, 0xffffffff, false, poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
	}
}
