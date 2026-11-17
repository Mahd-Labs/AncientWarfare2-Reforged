package net.shadowmage.ancientwarfare.structure.render.gate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;

import java.util.function.Function;

public final class RenderGateSingle extends RenderGateBasic {
	public RenderGateSingle(Function<ModelLayerLocation, ModelPart> baker) {
		super(baker);
	}

	@Override
	protected BlockPos getMin(EntityGate gate) {
		return BlockTools.getMin(gate.pos1, gate.pos2);
	}

	@Override
	protected BlockPos getMax(EntityGate gate) {
		return BlockTools.getMax(gate.pos1, gate.pos2);
	}

	@Override
	protected void postRender(EntityGate gate, int x, float width, int y, float height, boolean wideOnXAxis, float axisRotation, float frame, PoseStack pose, VertexConsumer buffer, int light, int color) {
		boolean opensReverse = gate.pos1.getX() > gate.pos2.getX() || gate.pos1.getZ() < gate.pos2.getZ();
		float wallTx = wideOnXAxis ? gate.edgePosition + gate.openingSpeed * (1 - frame) : 0;
		float wallTz = wideOnXAxis ? 0 : gate.edgePosition + gate.openingSpeed * (1 - frame);
		boolean render = false;
		if (opensReverse) {
			if ((wideOnXAxis && x - wallTx > -0.5f) || (!wideOnXAxis && x - wallTz > -0.5f)) {
				render = true;
			}
		} else {
			if ((wideOnXAxis && wallTx + x < gate.edgeMax - 0.5f) || (!wideOnXAxis && wallTz + x < gate.edgeMax - 0.5f)) {
				render = true;
			}
		}
		if (render) {
			if (opensReverse) {
				wallTx *= -1;
				wallTz *= -1;
			}
			pose.pushPose();
			pose.translate(wallTx, 0, wallTz);
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
