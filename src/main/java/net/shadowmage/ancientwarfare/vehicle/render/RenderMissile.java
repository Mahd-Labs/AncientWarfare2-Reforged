package net.shadowmage.ancientwarfare.vehicle.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.vehicle.missiles.MissileBase;
import net.shadowmage.ancientwarfare.vehicle.model.ModelArrow2;
import net.shadowmage.ancientwarfare.vehicle.model.ModelShot;

/*
 * Client-only. Port notes: 1.12 kept a map ammo -> RenderArrow / RenderShot (render/missile package); every ammo
 * whose updateAsArrow() is true (arrow, bolts, rockets) used the arrow model and all others the shot model, which
 * is what the map said, so the map and the two one-line sub renderers are folded into this class. Matrix steps as
 * 1.12 RenderMissileBase: rotate(yaw - 90, Y), rotate(pitch - 90, X), scale(-1, -1, 1), scale(ammo render scale).
 * 1.12 passed the interpolated yaw and the current (not interpolated) pitch; kept.
 */
public class RenderMissile extends EntityRenderer<MissileBase> {
	private final ModelArrow2 arrow2;
	private final ModelShot shot;

	public RenderMissile(EntityRendererProvider.Context context) {
		super(context);
		arrow2 = new ModelArrow2(context.bakeLayer(ModelArrow2.LAYER));
		shot = new ModelShot(context.bakeLayer(ModelShot.LAYER));
	}

	@Override
	public void render(MissileBase missile, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		float yaw = Mth.lerp(partialTicks, missile.yRotO, missile.getYRot());
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90));
		poseStack.mulPose(Axis.XP.rotationDegrees(missile.getXRot() - 90));
		poseStack.scale(-1, -1, 1);
		float scale = missile.ammoType.getRenderScale();
		poseStack.scale(scale, scale, scale);
		VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(missile)));
		if (missile.ammoType.updateAsArrow()) {
			arrow2.render(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, -1);
		} else {
			shot.render(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, -1);
		}
		poseStack.popPose();
		super.render(missile, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(MissileBase entity) {
		return entity.getTexture();
	}
}
