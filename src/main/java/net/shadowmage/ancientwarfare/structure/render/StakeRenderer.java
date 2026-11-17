package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.structure.block.BlockStake;
import net.shadowmage.ancientwarfare.structure.tile.TileStake;

public class StakeRenderer implements BlockEntityRenderer<TileStake> {

	public StakeRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(TileStake te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		BlockState state = te.getBlockState();
		if (!(state.getBlock() instanceof BlockStake)) {
			return;
		}
		Direction facing = state.getValue(net.shadowmage.ancientwarfare.core.render.property.CoreProperties.FACING);
		EntityRenderDispatcher rendermanager = Minecraft.getInstance().getEntityRenderDispatcher();

		te.getRenderEntity().ifPresent(e -> {
			if (te.isEntityOnFire()) {
				e.setRemainingFireTicks(1);
			} else {
				e.clearFire();
			}
			e.setYBodyRot(facing.toYRot());
			e.setYHeadRot(facing.toYRot());
			rendermanager.render(e, 0.5 + facing.getStepX() * 0.3, 0.6, 0.5 + facing.getStepZ() * 0.3, facing.toYRot(), 1.0F, pose, buffers, packedLight);
		});
	}

	@Override
	public AABB getRenderBoundingBox(TileStake te) {
		return te.getRenderBoundingBox();
	}
}
