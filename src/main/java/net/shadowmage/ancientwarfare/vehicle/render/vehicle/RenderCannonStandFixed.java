package net.shadowmage.ancientwarfare.vehicle.render.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringVarsHelper;
import net.shadowmage.ancientwarfare.vehicle.model.ModelCannonStandFixed;

public class RenderCannonStandFixed extends RenderVehicleBase {
	ModelCannonStandFixed model;

	public RenderCannonStandFixed(EntityRendererProvider.Context context) {
		model = new ModelCannonStandFixed(context.bakeLayer(ModelCannonStandFixed.LAYER));
	}

	@Override
	public void renderVehicle(VehicleBase veh, float yaw, float tick, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		VehicleFiringVarsHelper var = veh.firingVarsHelper;
		model.setTurretRotation(yaw - veh.localTurretRotation - tick * veh.currentTurretYawSpeed, veh.localTurretPitch - tick * veh.currentTurretPitchSpeed);
		model.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}
