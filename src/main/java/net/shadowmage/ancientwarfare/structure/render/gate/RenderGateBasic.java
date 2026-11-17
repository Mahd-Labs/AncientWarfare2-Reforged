package net.shadowmage.ancientwarfare.structure.render.gate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;
import net.shadowmage.ancientwarfare.structure.model.ModelGateBasic;

import java.util.function.Function;

/*
 * Port notes: 1.12 Render subclass called by RenderGateHelper; the GlStateManager transforms are the same calls on
 * the PoseStack, the bound texture / colour are the buffer and colour handed in by RenderGateHelper.
 */
public class RenderGateBasic implements IGateRender {

	protected final ModelGateBasic model;
	protected static final int OVERLAY = OverlayTexture.NO_OVERLAY;

	public RenderGateBasic(Function<ModelLayerLocation, ModelPart> baker) {
		model = new ModelGateBasic(baker.apply(ModelGateBasic.LAYER));
	}

	@Override
	public final void doRender(EntityGate g, float f, float f1, PoseStack pose, VertexConsumer buffer, int light, int color) {
		pose.pushPose();
		BlockPos min = getMin(g);
		BlockPos max = getMax(g);

		boolean wideOnXAxis = min.getX() != max.getX();

		float width;
		float height = max.getY() - min.getY() + 1;
		float xOffset = 0f;
		float zOffset = 0;
		float tx = 0;
		float ty = -1;
		float tz = 0;
		float axisRotation = 0;
		if (wideOnXAxis) {
			width = max.getX() - min.getX() + 1;
			xOffset = width * 0.5f - 0.5f;
			tx = 1;
			axisRotation = 90;
		} else {
			tz = 1;
			width = max.getZ() - min.getZ() + 1;
			zOffset = -width * 0.5f + 0.5f;
		}
		pose.translate(-xOffset, 0, zOffset);
		for (int y = 0; y < height; y++) {
			pose.pushPose();
			for (int x = 0; x < width; x++) {
				model.setModelRotation(axisRotation);
				if (y == height - 1 && x > 0 && x < width - 1) {
					model.renderTop(pose, buffer, light, OVERLAY, color);
				} else if (y == height - 1 && x == 0) {
					model.renderCorner(pose, buffer, light, OVERLAY, color);
				} else if (y == height - 1 && x == width - 1) {
					model.setModelRotation(axisRotation + 180);
					model.renderCorner(pose, buffer, light, OVERLAY, color);
				} else if (x == 0) {
					model.renderSide(pose, buffer, light, OVERLAY, color);
				} else if (x == width - 1) {
					model.setModelRotation(axisRotation + 180);
					model.renderSide(pose, buffer, light, OVERLAY, color);
				}
				postRender(g, x, width, y, height, wideOnXAxis, axisRotation, f1, pose, buffer, light, color);
				pose.translate(tx, 0, tz);
			}
			pose.popPose();
			pose.translate(0, ty, 0);
		}
		pose.popPose();
	}

	protected BlockPos getMin(EntityGate gate) {
		return gate.pos1;
	}

	protected BlockPos getMax(EntityGate gate) {
		return gate.pos2;
	}

	protected void postRender(EntityGate gate, int x, float width, int y, float height, boolean wideOnXAxis, float axisRotation, float frame, PoseStack pose, VertexConsumer buffer, int light, int color) {
		if (y + gate.edgePosition <= height - 0.475f) {
			pose.pushPose();
			pose.translate(0, -gate.edgePosition - gate.openingSpeed * (1 - frame), 0);
			model.setModelRotation(axisRotation);
			if (gate.getGateType().getModelType() == 0) {
				model.renderSolidWall(pose, buffer, light, OVERLAY, color);
			} else {
				model.renderBars(pose, buffer, light, OVERLAY, color);
			}
			pose.popPose();
		}
	}
}
