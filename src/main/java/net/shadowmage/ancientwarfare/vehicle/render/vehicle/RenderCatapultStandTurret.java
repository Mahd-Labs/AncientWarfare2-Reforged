package net.shadowmage.ancientwarfare.vehicle.render.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringVarsHelper;
import net.shadowmage.ancientwarfare.vehicle.model.ModelCatapultStandTurret;

public class RenderCatapultStandTurret extends RenderVehicleBase {
	ModelCatapultStandTurret model;

	public RenderCatapultStandTurret(EntityRendererProvider.Context context) {
		model = new ModelCatapultStandTurret(context.bakeLayer(ModelCatapultStandTurret.LAYER));
	}

	@Override
	public void renderVehicle(VehicleBase veh, float yaw, float tick, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		VehicleFiringVarsHelper var = veh.firingVarsHelper;
		model.setTurretRotation(yaw - veh.localTurretRotation - tick * veh.currentTurretYawSpeed);
		model.setArmRotation(var.getVar1() + (tick * var.getVar2()));
		model.setCrankRotations(var.getVar3() + (tick * var.getVar4()));
		model.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}
