package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.util.RenderTools;
import net.shadowmage.ancientwarfare.structure.tile.TileStructureBuilder;

public class RenderStructureBuilder implements BlockEntityRenderer<TileStructureBuilder> {

	public RenderStructureBuilder(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(TileStructureBuilder builder, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		if (builder.clientBB != null) {
			pose.pushPose();
			BlockPos min = builder.clientBB.min;
			BlockPos max = builder.clientBB.max;
			if (max == null) {
				max = min;
			}
			if (min != null) {
				renderBoundingBox(pose, buffers, builder.getBlockPos(), min, max, 1.f, 1.f, 1.f, 0.f);
			}
			pose.popPose();
		}
	}

	private void renderBoundingBox(PoseStack pose, MultiBufferSource buffers, BlockPos pos, BlockPos min, BlockPos max, float r, float g, float b, float expansion) {
		AABB bb = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
		if (expansion != 0.f) {
			bb = bb.expandTowards(expansion, expansion, expansion);
		}
		bb = bb.move(-pos.getX(), -pos.getY(), -pos.getZ());
		RenderTools.drawOutlinedBoundingBox2(pose, buffers, bb, 1.f, 1.f, 1.f, 0.0625f);
	}

	@Override
	public boolean shouldRenderOffScreen(TileStructureBuilder te) {
		return true;
	}

	@Override
	public AABB getRenderBoundingBox(TileStructureBuilder te) {
		return AABB.INFINITE;
	}
}
