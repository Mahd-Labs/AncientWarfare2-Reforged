package net.shadowmage.ancientwarfare.structure.render.gate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;

import java.util.function.Function;

public final class RenderGateDouble extends RenderGateBasic {

	public RenderGateDouble(Function<ModelLayerLocation, ModelPart> baker) {
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
		float move;
		boolean render = false;
		if (x < width * 0.5f) {
			move = -gate.edgePosition - gate.openingSpeed * (1 - frame);
			if (x + move > -0.5f) {
				render = true;
			}
		} else {
			move = gate.edgePosition + gate.openingSpeed * (1 - frame);
			if (x + move <= width - 0.475f) {
				render = true;
			}
		}
		float wallTx = wideOnXAxis ? move : 0;
		float wallTz = wideOnXAxis ? 0 : move;
		if (render) {
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
