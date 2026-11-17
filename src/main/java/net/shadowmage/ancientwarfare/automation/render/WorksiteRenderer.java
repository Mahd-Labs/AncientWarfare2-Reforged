package net.shadowmage.ancientwarfare.automation.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.automation.config.AWAutomationStatics;
import net.shadowmage.ancientwarfare.core.interfaces.IBoundedSite;

/*
 * Port notes: draws the work bounds of a worksite (toggled with the "render work bounds" key / config value).
 * 1.12 drew the box with RenderTools.drawOutlinedBoundingBox2 (thin quads, width 1/16); here it is the vanilla
 * line box (RenderType.lines), white like 1.12.
 */
public class WorksiteRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

	public WorksiteRenderer() {

	}

	@Override
	public void render(T worksite, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		if (worksite instanceof IBoundedSite) {
			IBoundedSite boundedSite = (IBoundedSite) worksite;
			if (AWAutomationStatics.renderWorkBounds) {
				BlockPos min = boundedSite.getWorkBoundsMin();
				BlockPos max = boundedSite.getWorkBoundsMax();
				if (max == null) {
					max = min;
				}
				if (min != null) {
					renderBoundingBox(poseStack, buffers, worksite.getBlockPos(), min, max, 1.f, 1.f, 1.f, 0.f);
				}
			}
		}
	}

	private void renderBoundingBox(PoseStack poseStack, MultiBufferSource buffers, BlockPos pos, BlockPos min, BlockPos max, float r, float g, float b, float expansion) {
		AABB bb = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
		if (expansion != 0.f) {
			bb = bb.expandTowards(expansion, expansion, expansion);
		}
		bb = bb.move(-pos.getX(), -pos.getY(), -pos.getZ());
		LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()), bb, r, g, b, 1.f);
	}

	@Override
	public boolean shouldRenderOffScreen(T worksite) {
		return true;
	}

	@Override
	public AABB getRenderBoundingBox(T worksite) {
		return AABB.INFINITE;
	}
}
