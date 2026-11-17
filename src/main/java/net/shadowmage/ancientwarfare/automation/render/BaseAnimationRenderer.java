package net.shadowmage.ancientwarfare.automation.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.render.lib.CCRenderState;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;

/*
 * Port notes: 1.12 FastTESR (shared block buffer) -> BlockEntityRenderer drawing the moving groups into the cutout
 * block render type through the CCRenderState stand-in. The pose stack is already at the block position
 * (1.12 renderer.setTranslation(x, y, z)), the packed light replaces ccrs.setBrightness(world, pos).
 */
public abstract class BaseAnimationRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
	private final ITESRRenderer bakery;

	public BaseAnimationRenderer(ITESRRenderer bakery) {
		this.bakery = bakery;
	}

	@Override
	public void render(T te, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		IExtendedBlockState exState = new IExtendedBlockState(te.getBlockState());
		exState = handleState(te, partialTicks, exState);
		CCRenderState ccrs = CCRenderState.instance();
		ccrs.reset();
		ccrs.bind(buffers.getBuffer(RenderType.cutout()), poseStack.last(), packedLight, OverlayTexture.NO_OVERLAY);
		bakery.renderTransformedBlockModels(ccrs, exState);
	}

	/*
	 * 1.12 TileEntity.getRenderBoundingBox of the tile (multiblocks and the waterwheel draw outside of their block)
	 */
	@Override
	public AABB getRenderBoundingBox(T te) {
		return BlockEntityRenderer.super.getRenderBoundingBox(te);
	}

	protected abstract IExtendedBlockState handleState(T te, float partialTicks, IExtendedBlockState state);
}
