package net.shadowmage.ancientwarfare.vehicle.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.entity.IVehicleType;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.registry.VehicleRegistry;
import net.shadowmage.ancientwarfare.vehicle.render.vehicle.RenderBallistaStand;
import net.shadowmage.ancientwarfare.vehicle.render.vehicle.RenderCannonStandFixed;
import net.shadowmage.ancientwarfare.vehicle.render.vehicle.RenderCannonStandTurret;
import net.shadowmage.ancientwarfare.vehicle.render.vehicle.RenderCatapultStandFixed;
import net.shadowmage.ancientwarfare.vehicle.render.vehicle.RenderCatapultStandTurret;
import net.shadowmage.ancientwarfare.vehicle.render.vehicle.RenderHwacha;
import net.shadowmage.ancientwarfare.vehicle.render.vehicle.RenderTrebuchetLarge;
import net.shadowmage.ancientwarfare.vehicle.render.vehicle.RenderTrebuchetStandFixed;
import net.shadowmage.ancientwarfare.vehicle.render.vehicle.RenderTrebuchetStandTurret;
import net.shadowmage.ancientwarfare.vehicle.render.vehicle.RenderVehicleBase;
import org.joml.Matrix4f;

import java.text.DecimalFormat;
import java.util.HashMap;

/*
 * Client-only. Port notes:
 * - doRender(x, y, z, yaw, partial) -> render(entity, yaw, partial, poseStack, buffers, light); the pose stack is
 *   already at the entity position. The matrix steps are the 1.12 ones: rotate(yaw, Y), scale(-1, -1, 1).
 *   The yaw is interpolated with rotLerp (the vanilla tracker delivers the yaw wrapped to +-180).
 * - GlStateManager.color tints -> the ARGB colour passed to the model: red tint while hitAnimationTicks > 0, alpha
 *   0.2 for the rider in first person when render_vehicles_in_first_person is off (translucent render type then).
 * - Name plate: same text, height (entity height + 0.75), scale (1.6 x the vanilla name tag scale), background and
 *   two pass text as 1.12, drawn with Font.drawInBatch like the vanilla name tag. The 1.12 "isInWorld" check
 *   (y > 0, to skip vehicles drawn in a GUI) is not needed.
 */
public class RenderVehicle extends EntityRenderer<VehicleBase> {

	private HashMap<IVehicleType, RenderVehicleBase> vehicleRenders = new HashMap<>();

	public RenderVehicle(EntityRendererProvider.Context context) {
		super(context);
		RenderBallistaStand ballista = new RenderBallistaStand(context);
		vehicleRenders.put(VehicleRegistry.CATAPULT_STAND_FIXED, new RenderCatapultStandFixed(context));
		vehicleRenders.put(VehicleRegistry.CATAPULT_STAND_TURRET, new RenderCatapultStandTurret(context));
		vehicleRenders.put(VehicleRegistry.BALLISTA_STAND_FIXED, ballista);
		vehicleRenders.put(VehicleRegistry.BALLISTA_STAND_TURRET, new RenderBallistaStand(context));
		vehicleRenders.put(VehicleRegistry.CANNON_STAND_FIXED, new RenderCannonStandFixed(context));
		vehicleRenders.put(VehicleRegistry.CANNON_STAND_TURRET, new RenderCannonStandTurret(context));
		vehicleRenders.put(VehicleRegistry.HWACHA, new RenderHwacha(context));
		vehicleRenders.put(VehicleRegistry.TREBUCHET_STAND_FIXED, new RenderTrebuchetStandFixed(context));
		vehicleRenders.put(VehicleRegistry.TREBUCHET_STAND_TURRET, new RenderTrebuchetStandTurret(context));
		vehicleRenders.put(VehicleRegistry.TREBUCHET_LARGE, new RenderTrebuchetLarge(context));
	}

	@Override
	public void render(VehicleBase vehicle, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		Minecraft mc = Minecraft.getInstance();
		float renderYaw = Mth.rotLerp(partialTicks, vehicle.yRotO, vehicle.getYRot());
		boolean useAlpha = !AWVehicleStatics.clientSettings.renderVehiclesInFirstPerson && vehicle.getFirstPassenger() == mc.player && mc.options.getCameraType().isFirstPerson();
		float red = 1.f;
		float green = 1.f;
		float blue = 1.f;
		float alpha = useAlpha ? 0.2f : 1.f;
		if (vehicle.hitAnimationTicks > 0) {
			float percent = ((float) vehicle.hitAnimationTicks / 20.f);
			green = 1.f - percent;
			blue = 1.f - percent;
			alpha = 1.f;
		}
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(renderYaw));
		poseStack.scale(-1, -1, 1);
		ResourceLocation texture = getTextureLocation(vehicle);
		VertexConsumer buffer = bufferSource.getBuffer(alpha < 1.f ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture));
		RenderVehicleBase render = vehicleRenders.get(vehicle.vehicleType);
		if (render != null) {
			render.renderVehicle(vehicle, renderYaw, partialTicks, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.colorFromFloat(alpha, red, green, blue));
		}
		poseStack.popPose();

		// dont' render nameplate for the vehicle that thePlayer is on
		if (AWVehicleStatics.clientSettings.renderVehicleNameplates && vehicle.getFirstPassenger() != mc.player) {
			renderNamePlate(vehicle, poseStack, bufferSource, packedLight);
		}
	}

	private DecimalFormat formatter1d = new DecimalFormat("#.#");

	private void renderNamePlate(VehicleBase vehicle, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		double distanceSq = this.entityRenderDispatcher.distanceToSqr(vehicle);
		int maxDistance = 64;
		String text = I18n.get(vehicle.vehicleType.getDisplayName() + ".name");
		if (AWVehicleStatics.clientSettings.renderVehicleNameplateHealth) {
			text = text + " " + formatter1d.format(vehicle.getHealth()) + "/" + formatter1d.format(vehicle.baseHealth);
		}
		if (distanceSq <= (double) (maxDistance * maxDistance)) {
			Font font = this.getFont();
			float scale = 0.016666668F * 1.6F;
			float namePlateHeight = vehicle.getBbHeight() + 0.75f;
			poseStack.pushPose();
			poseStack.translate(0.0F, namePlateHeight, 0.0F);
			poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
			poseStack.scale(scale, -scale, scale);
			Matrix4f matrix = poseStack.last().pose();
			float x = (float) (-font.width(text) / 2);
			int background = (int) (0.25F * 255.0F) << 24;
			font.drawInBatch(text, x, 0, 553648127, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, background, packedLight);
			font.drawInBatch(text, x, 0, -1, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
			poseStack.popPose();
		}
	}

	@Override
	public ResourceLocation getTextureLocation(VehicleBase entity) {
		return entity.getTexture();
	}
}
