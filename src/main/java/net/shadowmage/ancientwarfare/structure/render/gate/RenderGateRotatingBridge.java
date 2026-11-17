package net.shadowmage.ancientwarfare.structure.render.gate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;
import net.shadowmage.ancientwarfare.structure.model.ModelGateBridge;

import java.util.function.Function;

public final class RenderGateRotatingBridge implements IGateRender {

	private final ModelGateBridge model;
	private static final int OVERLAY = OverlayTexture.NO_OVERLAY;

	public RenderGateRotatingBridge(Function<ModelLayerLocation, ModelPart> baker) {
		model = new ModelGateBridge(baker.apply(ModelGateBridge.LAYER));
	}

	@Override
	public void doRender(EntityGate g, float f, float f1, PoseStack pose, VertexConsumer buffer, int light, int color) {
		pose.pushPose();
		BlockPos min = g.pos1;
		BlockPos max = g.pos2;

		boolean wideOnXAxis = min.getX() != max.getX();

		float rx = wideOnXAxis ? g.edgePosition + g.openingSpeed * (1 - f1) : 0;
		float rz = wideOnXAxis ? 0 : g.edgePosition + g.openingSpeed * (1 - f1);
		boolean invert = g.gateOrientation == Direction.SOUTH || g.gateOrientation == Direction.EAST;
		if (invert) {
			rx *= -1;
			rz *= -1;
		}
		pose.translate(0, -0.5f, 0);
		pose.mulPose(Axis.XP.rotationDegrees(rx));
		pose.mulPose(Axis.ZP.rotationDegrees(rz));
		float width = wideOnXAxis ? max.getX() - min.getX() + 1 : max.getZ() - min.getZ() + 1;
		float height = max.getY() - min.getY() + 1;
		float xOffset = wideOnXAxis ? width * 0.5f - 0.5f : 0f;
		float zOffset = wideOnXAxis ? 0f : -width * 0.5f + 0.5f;

		float tx = wideOnXAxis ? 1 : 0;
		float ty = -1;
		float tz = wideOnXAxis ? 0 : 1;
		float axisRotation = wideOnXAxis ? 180 : 90;
		if (invert) {
			pose.mulPose(Axis.YP.rotationDegrees(180));
		}
		pose.translate(-xOffset, 0, zOffset);
		for (int y = 0; y < height; y++) {
			pose.pushPose();
			for (int x = 0; x < width; x++) {
				model.setModelRotation(axisRotation);
				if (y == 0) {
					model.renderGateBlock(pose, buffer, light, OVERLAY, color);
				}
				if (y == height - 1 && x > 0 && x < width - 1) {
					model.renderTop(pose, buffer, light, OVERLAY, color);
				} else if (y == height - 1 && x == 0) {
					model.renderCorner(pose, buffer, light, OVERLAY, color);
				} else if (y == height - 1 && x == width - 1) {
					model.renderCorner2(pose, buffer, light, OVERLAY, color);
				} else if (x == 0 && y > 0) {
					model.renderSide1(pose, buffer, light, OVERLAY, color);
				} else if (x == width - 1 && y > 0) {
					model.renderSide2(pose, buffer, light, OVERLAY, color);
				}
				if (y > 0) {
					pose.pushPose();
					model.setModelRotation(axisRotation);
					model.renderSolidWall(pose, buffer, light, OVERLAY, color);
					pose.popPose();
				}
				pose.translate(tx, 0, tz);
			}
			pose.popPose();
			pose.translate(0, ty, 0);
		}
		pose.popPose();
	}
}
