package net.shadowmage.ancientwarfare.vehicle.render.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringVarsHelper;
import net.shadowmage.ancientwarfare.vehicle.model.ModelTrebuchetStandFixed;

public class RenderTrebuchetStandFixed extends RenderVehicleBase {
	ModelTrebuchetStandFixed model;

	public RenderTrebuchetStandFixed(EntityRendererProvider.Context context) {
		model = new ModelTrebuchetStandFixed(context.bakeLayer(ModelTrebuchetStandFixed.LAYER));
	}

	@Override
	public void renderVehicle(VehicleBase veh, float yaw, float tick, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		VehicleFiringVarsHelper var = veh.firingVarsHelper;
		model.setArmRotations(var.getVar1() + tick * var.getVar2(), var.getVar3() + tick * var.getVar4());
		model.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}
