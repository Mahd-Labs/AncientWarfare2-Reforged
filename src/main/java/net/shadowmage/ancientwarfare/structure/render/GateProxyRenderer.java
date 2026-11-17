package net.shadowmage.ancientwarfare.structure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.structure.tile.TEGateProxy;

/*
 * Port notes: 1.12 TESR. x / y / z (camera relative tile position) is the pose handed in, so only the offset of the
 * gate from the tile is translated. isGlobalRenderer -> shouldRenderOffScreen; the render box is the one of the gate.
 */
public class GateProxyRenderer implements BlockEntityRenderer<TEGateProxy> {
	private final RenderGateHelper renderGateHelper;

	public GateProxyRenderer(BlockEntityRendererProvider.Context context) {
		renderGateHelper = new RenderGateHelper(context::bakeLayer);
	}

	@Override
	public void render(TEGateProxy te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
		if (!te.doesRender()) {
			return;
		}
		BlockPos pos = te.getBlockPos();
		te.getGate().ifPresent(gate -> {
			pose.pushPose();
			pose.translate(gate.getX() - pos.getX(), gate.getY() - pos.getY(), gate.getZ() - pos.getZ());
			renderGateHelper.doRender(gate, pose, buffers, 0, partialTicks);
			pose.popPose();
		});
	}

	@Override
	public boolean shouldRenderOffScreen(TEGateProxy te) {
		return true;
	}

	@Override
	public AABB getRenderBoundingBox(TEGateProxy te) {
		return te.getRenderBoundingBox();
	}
}
