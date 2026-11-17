package net.shadowmage.ancientwarfare.vehicle.render.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringVarsHelper;
import net.shadowmage.ancientwarfare.vehicle.model.ModelHwacha;

public class RenderHwacha extends RenderVehicleBase {
	ModelHwacha model;

	public RenderHwacha(EntityRendererProvider.Context context) {
		model = new ModelHwacha(context.bakeLayer(ModelHwacha.LAYER));
	}

	@Override
	public void renderVehicle(VehicleBase veh, float yaw, float tick, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		VehicleFiringVarsHelper var = veh.firingVarsHelper;
		//port: the hwacha does not drive in the reduced module, so the launcher always stands at its firing pitch (1.12: -8 while moving)
		model.setModelPitch(-39);
		model.setWheelRotations(0, 0, 0, 0);
		model.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}
