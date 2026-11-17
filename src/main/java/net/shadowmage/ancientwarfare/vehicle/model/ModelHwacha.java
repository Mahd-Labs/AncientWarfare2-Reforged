package net.shadowmage.ancientwarfare.vehicle.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/*
 * Client-only. Generated from the 1.12.2 ModelBase class of the same name by a one-off converter script (not part of this repository)
 * (part tree, boxes, texture offsets, pivots and rotations are unchanged; see vehicle/model/README.md).
 */
public class ModelHwacha {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "hwacha"), "main");
	private static final float TORADIANS = 3.141592653589793f / 180.f; //core.util.Trig.TORADIANS

	public final ModelPart frontAxle;
	public final ModelPart FRWheelPivot;
	public final ModelPart FLWheelPivot;

	public ModelHwacha(ModelPart root) {
		frontAxle = root.getChild("frontAxle");
		FRWheelPivot = root.getChild("frontAxle").getChild("FRWheelPivot");
		FLWheelPivot = root.getChild("frontAxle").getChild("FLWheelPivot");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition frontAxle = partdefinition.addOrReplaceChild("frontAxle", CubeListBuilder.create().texOffs(0, 59).addBox(-10.5f, -0.5f, -0.5f, 21.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, -8.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition FRWheelPivot = frontAxle.addOrReplaceChild("FRWheelPivot", CubeListBuilder.create().texOffs(0, 62).addBox(-0.5f, -7.0f, -0.5f, 1.0F, 14.0F, 1.0F), PartPose.offsetAndRotation(-10.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		FRWheelPivot.addOrReplaceChild("FRS2", CubeListBuilder.create().texOffs(23, 62).addBox(-0.5f, -0.5f, -7.0f, 1.0F, 1.0F, 14.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.7853982f, 0.0f, 0.0f));
		FRWheelPivot.addOrReplaceChild("FRS3", CubeListBuilder.create().texOffs(23, 62).addBox(-0.5f, -0.5f, -7.0f, 1.0F, 1.0F, 14.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.7853982f, 0.0f, 0.0f));
		FRWheelPivot.addOrReplaceChild("FRS4", CubeListBuilder.create().texOffs(23, 62).addBox(-0.5f, -0.5f, -7.0f, 1.0F, 1.0F, 14.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		FRWheelPivot.addOrReplaceChild("frw1", CubeListBuilder.create().texOffs(0, 84).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(-1.0f, -8.0f, -3.0f, 0.0f, 0.0f, 0.0f));
		FRWheelPivot.addOrReplaceChild("Frw4", CubeListBuilder.create().texOffs(0, 75).addBox(0.0f, 0.0f, -7.0f, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(-1.0f, -8.0f, -3.0f, 0.7853982f, 0.0f, 0.0f));
		FRWheelPivot.addOrReplaceChild("FLr5", CubeListBuilder.create().texOffs(0, 84).addBox(0.0f, 0.0f, -6.0f, 2.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(-1.0f, -3.0f, -8.0f, 1.5707958f, 0.0f, 0.0f));
		FRWheelPivot.addOrReplaceChild("Frw6", CubeListBuilder.create().texOffs(0, 75).addBox(0.0f, -1.0f, 0.0f, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(-1.0f, 3.0f, -8.0f, -0.7853982f, 0.0f, 0.0f));
		FRWheelPivot.addOrReplaceChild("Frw7", CubeListBuilder.create().texOffs(0, 84).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(-1.0f, 7.0f, -3.0f, 0.0f, 0.0f, 0.0f));
		FRWheelPivot.addOrReplaceChild("Frw8", CubeListBuilder.create().texOffs(0, 75).addBox(0.0f, -1.0f, -7.0f, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(-1.0f, 3.0f, 8.0f, 0.7853982f, 0.0f, 0.0f));
		FRWheelPivot.addOrReplaceChild("Frw3", CubeListBuilder.create().texOffs(0, 84).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(-1.0f, -3.0f, 8.0f, -1.570796f, 0.0f, 0.0f));
		FRWheelPivot.addOrReplaceChild("Frw2", CubeListBuilder.create().texOffs(0, 75).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(-1.0f, -8.0f, 3.0f, -0.7853982f, 0.0f, 0.0f));
		PartDefinition FLWheelPivot = frontAxle.addOrReplaceChild("FLWheelPivot", CubeListBuilder.create().texOffs(0, 62).addBox(-0.5f, -7.0f, -0.5f, 1.0F, 14.0F, 1.0F), PartPose.offsetAndRotation(10.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		FLWheelPivot.addOrReplaceChild("FLw3", CubeListBuilder.create().texOffs(0, 84).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(-1.0f, -3.0f, 8.0f, -1.570796f, 0.0f, 0.0f));
		FLWheelPivot.addOrReplaceChild("FLw2", CubeListBuilder.create().texOffs(0, 75).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(-1.0f, -8.0f, 3.0f, -0.7853982f, 0.0f, 0.0f));
		FLWheelPivot.addOrReplaceChild("FS4", CubeListBuilder.create().texOffs(23, 62).addBox(-0.5f, -0.5f, -7.0f, 1.0F, 1.0F, 14.0F), PartPose.offsetAndRotation(0.0f, 0.0f, -0.0f, 0.0f, 0.0f, 0.0f));
		FLWheelPivot.addOrReplaceChild("FLS2", CubeListBuilder.create().texOffs(23, 62).addBox(-0.5f, -0.5f, -7.0f, 1.0F, 1.0F, 14.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.7853982f, 0.0f, 0.0f));
		FLWheelPivot.addOrReplaceChild("FS3", CubeListBuilder.create().texOffs(23, 62).addBox(-0.5f, -0.5f, -7.0f, 1.0F, 1.0F, 14.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.7853982f, 0.0f, 0.0f));
		FLWheelPivot.addOrReplaceChild("flw1", CubeListBuilder.create().texOffs(0, 84).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(-1.0f, -8.0f, -3.0f, 0.0f, 0.0f, 0.0f));
		FLWheelPivot.addOrReplaceChild("FLw4", CubeListBuilder.create().texOffs(0, 75).addBox(0.0f, 0.0f, -7.0f, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(-1.0f, -8.0f, -3.0f, 0.7853982f, 0.0f, 0.0f));
		FLWheelPivot.addOrReplaceChild("FLw5", CubeListBuilder.create().texOffs(0, 84).addBox(0.0f, 0.0f, -6.0f, 2.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(-1.0f, -3.0f, -8.0f, 1.5707955f, 0.0f, 0.0f));
		FLWheelPivot.addOrReplaceChild("FLw6", CubeListBuilder.create().texOffs(0, 75).addBox(0.0f, -1.0f, 0.0f, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(-1.0f, 3.0f, -8.0f, -0.7853982f, 0.0f, 0.0f));
		FLWheelPivot.addOrReplaceChild("FLw7", CubeListBuilder.create().texOffs(0, 84).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(-1.0f, 7.0f, -3.0f, 0.0f, 0.0f, 0.0f));
		FLWheelPivot.addOrReplaceChild("FLw8", CubeListBuilder.create().texOffs(0, 75).addBox(0.0f, -1.0f, -7.0f, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(-1.0f, 3.0f, 8.0f, 0.7853982f, 0.0f, 0.0f));
		PartDefinition hwachaBase = frontAxle.addOrReplaceChild("hwachaBase", CubeListBuilder.create().texOffs(0, 153).addBox(-6.5f, -6.5f, -0.5f, 13.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		hwachaBase.addOrReplaceChild("hwachaBaseAxleMountLeft", CubeListBuilder.create().texOffs(29, 153).addBox(-1.0f, -0.5f, -1.0f, 2.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(7.5f, -6.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		hwachaBase.addOrReplaceChild("hwachaBaseAxleMountMid", CubeListBuilder.create().texOffs(0, 162).addBox(-1.0f, -0.5f, -1.0f, 2.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(0.0f, -6.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		hwachaBase.addOrReplaceChild("hwachaBaseAxleMountRight", CubeListBuilder.create().texOffs(29, 153).addBox(-1.0f, -0.5f, -1.0f, 2.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(-7.5f, -6.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		hwachaBase.addOrReplaceChild("hwachaBaseBottom", CubeListBuilder.create().texOffs(0, 172).addBox(-1.0f, -1.0f, -1.0f, 13.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-5.5f, 1.5f, 0.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition hwachaBaseLeft = hwachaBase.addOrReplaceChild("hwachaBaseLeft", CubeListBuilder.create().texOffs(0, 128).addBox(-1.0f, -1.0f, -1.0f, 2.0F, 2.0F, 22.0F), PartPose.offsetAndRotation(7.5f, -7.5f, -13.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition hwachaHandleLeft = hwachaBaseLeft.addOrReplaceChild("hwachaHandleLeft", CubeListBuilder.create().texOffs(49, 128).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(-1.0f, -1.0f, 21.0f, 0.0f, 0.0f, 0.0f));
		hwachaHandleLeft.addOrReplaceChild("hwachaHandleLeftTip", CubeListBuilder.create().texOffs(49, 138).addBox(0.5f, 0.0f, 0.0f, 1.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 8.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition hwachaBaseRight = hwachaBase.addOrReplaceChild("hwachaBaseRight", CubeListBuilder.create().texOffs(0, 128).addBox(-1.0f, -1.0f, -1.0f, 2.0F, 2.0F, 22.0F), PartPose.offsetAndRotation(-7.5f, -7.5f, -13.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition hwachaHandleRight = hwachaBaseRight.addOrReplaceChild("hwachaHandleRight", CubeListBuilder.create().texOffs(49, 128).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(-1.0f, -1.0f, 21.0f, 0.0f, 0.0f, 0.0f));
		hwachaHandleRight.addOrReplaceChild("hwachaHandleRightTip", CubeListBuilder.create().texOffs(49, 138).addBox(0.5f, 0.0f, 0.0f, 1.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 8.0f, 0.0f, 0.0f, 0.0f));
		hwachaBase.addOrReplaceChild("hwachaBaseMiddle", CubeListBuilder.create().texOffs(0, 177).addBox(0.0f, 0.0f, 0.0f, 14.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-7.0f, -8.5f, -1.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition hwachaRackBase = hwachaBase.addOrReplaceChild("hwachaRackBase", CubeListBuilder.create().texOffs(0, 182).addBox(-13.5f, 0.0f, -1.5f, 27.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-0.0f, -9.5f, -0.0f, 0.0f, 0.0f, 0.0f));
		hwachaRackBase.addOrReplaceChild("hwachaRackLeft", CubeListBuilder.create().texOffs(38, 153).addBox(0.0f, 0.0f, 0.0f, 1.0F, 14.0F, 3.0F), PartPose.offsetAndRotation(10.5f, -14.0f, -1.5f, 0.0f, 0.0f, 0.0f));
		hwachaRackBase.addOrReplaceChild("hwachaRackRight", CubeListBuilder.create().texOffs(38, 153).addBox(0.0f, 0.0f, 0.0f, 1.0F, 14.0F, 3.0F), PartPose.offsetAndRotation(-11.5f, -14.0f, -1.5f, 0.0f, 0.0f, 0.0f));
		hwachaRackBase.addOrReplaceChild("hwachaRackMid", CubeListBuilder.create().texOffs(0, 187).addBox(0.0f, 0.0f, -1.5f, 22.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(-11.0f, -3.0f, -0.5f, 0.0f, 0.0f, 0.0f));
		hwachaRackBase.addOrReplaceChild("hwachaRackTop", CubeListBuilder.create().texOffs(0, 193).addBox(0.0f, 0.0f, 0.0f, 23.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-11.5f, -15.0f, -1.5f, 0.0f, 0.0f, 0.0f));
		hwachaRackBase.addOrReplaceChild("hwachaBoxLeft", CubeListBuilder.create().texOffs(47, 153).addBox(0.0f, 0.0f, 0.0f, 1.0F, 11.0F, 6.0F), PartPose.offsetAndRotation(9.5f, -14.0f, -3.0f, 2.0805946E-9f, 0.0f, 0.0f));
		hwachaRackBase.addOrReplaceChild("hwachaBoxRight", CubeListBuilder.create().texOffs(47, 153).addBox(0.0f, 0.0f, 0.0f, 1.0F, 11.0F, 6.0F), PartPose.offsetAndRotation(-10.5f, -14.0f, -3.0f, 2.0805946E-9f, 0.0f, 0.0f));
		hwachaRackBase.addOrReplaceChild("hwachaBoxBottom", CubeListBuilder.create().texOffs(33, 174).addBox(0.0f, 0.0f, 0.0f, 19.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(-9.5f, -4.0f, -3.0f, 0.0f, 0.0f, 0.0f));
		hwachaRackBase.addOrReplaceChild("hwachaBoxTop", CubeListBuilder.create().texOffs(33, 174).addBox(0.0f, 0.0f, 0.0f, 19.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(-9.5f, -14.0f, -3.0f, 0.0f, 0.0f, 0.0f));
		hwachaRackBase.addOrReplaceChild("hwachaBoxFront", CubeListBuilder.create().texOffs(70, 128).addBox(0.0f, 0.0f, 0.0f, 19.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(-9.5f, -13.0f, -2.5f, 0.0f, 0.0f, 0.0f));
		hwachaRackBase.addOrReplaceChild("hwachaBoxBack", CubeListBuilder.create().texOffs(70, 139).addBox(0.0f, 0.0f, 0.0f, 19.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(-9.5f, -13.0f, 1.5f, 0.0f, 0.0f, 0.0f));
		hwachaRackBase.addOrReplaceChild("hwachaRackBit1", CubeListBuilder.create().texOffs(9, 162).addBox(0.0f, 0.0f, 0.0f, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-1.0f, -2.0f, -1.0f, 0.0f, 0.0f, 0.0f));
		hwachaRackBase.addOrReplaceChild("hwachaRackBit2", CubeListBuilder.create().texOffs(18, 162).addBox(0.0f, 0.0f, 0.0f, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(3.0f, -2.0f, -0.5f, 0.0f, 0.0f, 0.0f));
		hwachaRackBase.addOrReplaceChild("hwachaRackBit3", CubeListBuilder.create().texOffs(18, 162).addBox(0.0f, 0.0f, 0.0f, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-4.0f, -2.0f, -0.5f, 0.0f, 0.0f, 0.0f));
		hwachaBase.addOrReplaceChild("hwachaBaseFront", CubeListBuilder.create().texOffs(62, 150).addBox(0.0f, 0.0f, 0.0f, 13.0F, 2.0F, 12.0F), PartPose.offsetAndRotation(-6.5f, -8.5f, -13.0f, 0.0f, 0.0f, 0.0f));
		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		frontAxle.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void setModelPitch(float pitch) {
		frontAxle.xRot = toRadians(pitch);
	}

	public void setWheelRotations(float fl, float fr, float rl, float rr) {
		this.FRWheelPivot.xRot = toRadians(fr);
		this.FLWheelPivot.xRot = toRadians(fl);
	}

	private static float toRadians(float degrees) {
		return degrees * TORADIANS;
	}
}
