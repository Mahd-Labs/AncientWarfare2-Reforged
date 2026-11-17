package net.shadowmage.ancientwarfare.vehicle.render.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringVarsHelper;
import net.shadowmage.ancientwarfare.vehicle.model.ModelBallistaStand;

public class RenderBallistaStand extends RenderVehicleBase {
	ModelBallistaStand model;

	public RenderBallistaStand(EntityRendererProvider.Context context) {
		model = new ModelBallistaStand(context.bakeLayer(ModelBallistaStand.LAYER));
	}

	@Override
	public void renderVehicle(VehicleBase veh, float yaw, float tick, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		VehicleFiringVarsHelper var = veh.firingVarsHelper;
		model.setTurretRotation(yaw - veh.localTurretRotation - tick * veh.currentTurretYawSpeed, -veh.localTurretPitch + tick * veh.currentTurretPitchSpeed);
		model.setCrankRotations(var.getVar1() + (tick * var.getVar2()));
		model.setBowAndStringRotation(var.getVar3() + tick * var.getVar4(), var.getVar5() + tick * var.getVar6());
		model.setTriggerAngle(var.getVar7());
		model.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}
