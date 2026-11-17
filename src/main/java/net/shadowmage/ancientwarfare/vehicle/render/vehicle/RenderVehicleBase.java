package net.shadowmage.ancientwarfare.vehicle.render.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

/*
 * Client-only. Port notes: 1.12 sub renderers were Render<VehicleBase> instances owned by RenderVehicle; only their
 * renderVehicle method was ever called (by RenderVehicle after it had set up the matrix and bound the texture). They
 * are plain helpers now: renderVehicle gets the pose stack and the vertex consumer of the vehicle's texture, sets the
 * model angles exactly as 1.12 and renders the model.
 */
public abstract class RenderVehicleBase {
	public abstract void renderVehicle(VehicleBase entity, float entityYaw, float partialTicks, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color);
}
