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
public class ModelTrebuchetStandTurret {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "trebuchet_stand_turret"), "main");
	private static final float TORADIANS = 3.141592653589793f / 180.f; //core.util.Trig.TORADIANS

	public final ModelPart standBaseMidBeam;
	public final ModelPart turretMidBeam;
	public final ModelPart trebuchetAxle;
	public final ModelPart trebuchetWeightAxle;
	public final ModelPart trebuchetSlingString;

	public ModelTrebuchetStandTurret(ModelPart root) {
		standBaseMidBeam = root.getChild("standBaseMidBeam");
		turretMidBeam = root.getChild("turretMidBeam");
		trebuchetAxle = root.getChild("turretMidBeam").getChild("trebuchetAxle");
		trebuchetWeightAxle = root.getChild("turretMidBeam").getChild("trebuchetAxle").getChild("trebBeamFront").getChild("trebuchetWeightAxle");
		trebuchetSlingString = root.getChild("turretMidBeam").getChild("trebuchetAxle").getChild("trebBeamFront").getChild("trebuchetArm2").getChild("trebuchetSlingString");
		setArmRotations(-27.f, -64.f);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition standBaseMidBeam = partdefinition.addOrReplaceChild("standBaseMidBeam", CubeListBuilder.create().texOffs(0, 0).addBox(-18.0f, -2.0f, -1.5f, 36.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		standBaseMidBeam.addOrReplaceChild("standLeftBeam", CubeListBuilder.create().texOffs(0, 63).addBox(-1.0f, -2.0f, -19.5f, 3.0F, 2.0F, 39.0F), PartPose.offsetAndRotation(19.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		standBaseMidBeam.addOrReplaceChild("standRightBeam", CubeListBuilder.create().texOffs(0, 63).addBox(-1.0f, -2.0f, -19.5f, 3.0F, 2.0F, 39.0F), PartPose.offsetAndRotation(-20.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		standBaseMidBeam.addOrReplaceChild("standFrontBeam", CubeListBuilder.create().texOffs(0, 6).addBox(-21.0f, -2.0f, -1.0f, 42.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0f, 0.0f, -22.5f, 0.0f, 0.0f, 0.0f));
		standBaseMidBeam.addOrReplaceChild("standRearBeam", CubeListBuilder.create().texOffs(0, 6).addBox(-21.0f, -2.0f, 0.0f, 42.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 19.5f, 0.0f, 0.0f, 0.0f));
		standBaseMidBeam.addOrReplaceChild("standTopMidBeam", CubeListBuilder.create().texOffs(0, 13).addBox(-16.0f, -1.0f, -1.5f, 32.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0f, -7.5f, 0.0f, 0.0f, 0.0f, 0.0f));
		standBaseMidBeam.addOrReplaceChild("standTopLeftBeam", CubeListBuilder.create().texOffs(0, 25).addBox(0.0f, -1.0f, -17.5f, 3.0F, 2.0F, 35.0F), PartPose.offsetAndRotation(16.0f, -7.5f, 0.0f, 0.0f, 0.0f, 0.0f));
		standBaseMidBeam.addOrReplaceChild("standTopRightBeam", CubeListBuilder.create().texOffs(0, 25).addBox(0.0f, -1.0f, -17.5f, 3.0F, 2.0F, 35.0F), PartPose.offsetAndRotation(-19.0f, -7.5f, 0.0f, 0.0f, 0.0f, 0.0f));
		standBaseMidBeam.addOrReplaceChild("standTopFrontBeam", CubeListBuilder.create().texOffs(0, 19).addBox(-19.0f, -1.0f, 0.0f, 38.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0f, -7.5f, -20.5f, 0.0f, 0.0f, 0.0f));
		standBaseMidBeam.addOrReplaceChild("standTopRearBeam", CubeListBuilder.create().texOffs(0, 19).addBox(-19.0f, -1.0f, 0.0f, 38.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0f, -7.5f, 17.5f, 0.0f, 0.0f, 0.0f));
		standBaseMidBeam.addOrReplaceChild("standFlAngle", CubeListBuilder.create().texOffs(0, 105).addBox(-2.0f, 0.0f, 0.0f, 2.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(18.5f, -8.5f, -20.0f, -0.26179937f, 0.0f, -0.26179928f));
		standBaseMidBeam.addOrReplaceChild("standFrAngle", CubeListBuilder.create().texOffs(0, 105).addBox(0.0f, 0.0f, 0.0f, 2.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(-18.5f, -8.5f, -20.0f, -0.26179934f, 0.0f, 0.26179928f));
		standBaseMidBeam.addOrReplaceChild("standRrAngle", CubeListBuilder.create().texOffs(0, 105).addBox(0.0f, 0.0f, -2.0f, 2.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(-18.5f, -8.5f, 20.0f, 0.26179934f, 0.0f, 0.26179928f));
		standBaseMidBeam.addOrReplaceChild("standRlAngle", CubeListBuilder.create().texOffs(0, 105).addBox(-2.0f, 0.0f, -2.0f, 2.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(18.5f, -8.5f, 20.0f, 0.2617993f, 0.0f, -0.26179928f));
		standBaseMidBeam.addOrReplaceChild("standLBrace", CubeListBuilder.create().texOffs(9, 105).addBox(0.0f, 0.0f, 0.0f, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(16.0f, -6.5f, -1.0f, 0.0f, 0.0f, 0.0f));
		standBaseMidBeam.addOrReplaceChild("standRBrace", CubeListBuilder.create().texOffs(9, 105).addBox(0.0f, 0.0f, 0.0f, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(-18.0f, -6.5f, -1.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition turretMidBeam = partdefinition.addOrReplaceChild("turretMidBeam", CubeListBuilder.create().texOffs(0, 128).addBox(-21.0f, -1.0f, -1.5f, 42.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0f, -9.5f, 0.0f, 0.0f, 5.659217E-7f, 0.0f));
		turretMidBeam.addOrReplaceChild("upright1", CubeListBuilder.create().texOffs(0, 140).addBox(-2.5f, -33.0f, 0.0f, 3.0F, 33.0F, 3.0F), PartPose.offsetAndRotation(-6.0f, -1.0f, -1.5f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("upright2", CubeListBuilder.create().texOffs(0, 140).addBox(0.5f, -33.0f, 0.0f, 3.0F, 33.0F, 3.0F), PartPose.offsetAndRotation(5.0f, -1.0f, -1.5f, 1.4564162E-8f, 0.0f, 1.0402973E-9f));
		PartDefinition turretFrontBeam = turretMidBeam.addOrReplaceChild("turretFrontBeam", CubeListBuilder.create().texOffs(0, 134).addBox(-15.0f, -1.0f, -1.5f, 30.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, -18.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition chairBrace = turretFrontBeam.addOrReplaceChild("chairBrace", CubeListBuilder.create().texOffs(70, 59).addBox(0.0f, 0.0f, -8.0f, 3.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(-1.5f, -0.5f, 0.0f, 0.0f, 0.0f, 0.0f));
		chairBrace.addOrReplaceChild("chairBottom", CubeListBuilder.create().texOffs(54, 68).addBox(0.0f, 0.0f, -9.0f, 10.0F, 1.0F, 9.0F), PartPose.offsetAndRotation(-3.5f, -1.5f, -1.0f, 0.1570796f, 0.0f, 0.0f));
		chairBrace.addOrReplaceChild("chairBack", CubeListBuilder.create().texOffs(49, 79).addBox(0.0f, -10.0f, 0.0f, 10.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(-3.5f, -1.0f, -1.0f, -0.1745329f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretRearBeam", CubeListBuilder.create().texOffs(0, 134).addBox(-15.0f, -1.0f, -1.5f, 30.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 18.0f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretLeftBeam1", CubeListBuilder.create().texOffs(13, 140).addBox(0.0f, -1.0f, 0.0f, 3.0F, 2.0F, 15.0F), PartPose.offsetAndRotation(12.0f, 0.0f, -16.5f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretLeftBeam2", CubeListBuilder.create().texOffs(13, 140).addBox(0.0f, -1.0f, 0.0f, 3.0F, 2.0F, 15.0F), PartPose.offsetAndRotation(12.0f, 0.0f, 1.5f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretRightBeam1", CubeListBuilder.create().texOffs(13, 140).addBox(0.0f, -1.0f, 0.0f, 3.0F, 2.0F, 15.0F), PartPose.offsetAndRotation(-15.0f, 0.0f, -16.5f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretRightBeam2", CubeListBuilder.create().texOffs(13, 140).addBox(0.0f, -1.0f, 0.0f, 3.0F, 2.0F, 15.0F), PartPose.offsetAndRotation(-15.0f, 0.0f, 1.5f, 0.0f, 0.0f, 0.0f));
		PartDefinition trebuchetAxle = turretMidBeam.addOrReplaceChild("trebuchetAxle", CubeListBuilder.create().texOffs(67, 134).addBox(-6.0f, -0.5f, -0.5f, 12.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, -33.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition trebBeamFront = trebuchetAxle.addOrReplaceChild("trebBeamFront", CubeListBuilder.create().texOffs(94, 128).addBox(-1.5f, -1.5f, -10.5f, 3.0F, 3.0F, 40.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition trebuchetWeightAxle = trebBeamFront.addOrReplaceChild("trebuchetWeightAxle", CubeListBuilder.create().texOffs(67, 137).addBox(-3.5f, -0.5f, -0.5f, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, -9.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition trebuchetWeightSide1 = trebuchetWeightAxle.addOrReplaceChild("trebuchetWeightSide1", CubeListBuilder.create().texOffs(50, 140).addBox(0.0f, -2.0f, -1.0f, 1.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(-3.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		trebuchetWeightSide1.addOrReplaceChild("trebuchetWeight1", CubeListBuilder.create().texOffs(13, 158).addBox(-2.0f, 0.0f, -6.0f, 10.0F, 15.0F, 12.0F), PartPose.offsetAndRotation(0.0f, 5.0f, 0.0f, -1.0402973E-9f, 0.0f, 0.0f));
		trebuchetWeightAxle.addOrReplaceChild("trebuchetWeightSide2", CubeListBuilder.create().texOffs(50, 140).addBox(0.0f, -2.0f, -1.0f, 1.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(2.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition trebuchetArm2 = trebBeamFront.addOrReplaceChild("trebuchetArm2", CubeListBuilder.create().texOffs(94, 172).addBox(-1.0f, 0.0f, 0.0f, 2.0F, 2.0F, 40.0F), PartPose.offsetAndRotation(0.0f, -1.0f, 28.5f, 0.0f, 0.0f, 0.0f));
		PartDefinition trebuchetSlingString = trebuchetArm2.addOrReplaceChild("trebuchetSlingString", CubeListBuilder.create().texOffs(58, 140).addBox(-0.5f, -0.5f, -0.5f, 1.0F, 24.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 1.0f, 39.0f, 0.0f, -1.0402973E-9f, 0.0f));
		trebuchetSlingString.addOrReplaceChild("trebuchetSlingPouchT3", CubeListBuilder.create().texOffs(63, 149).addBox(-3.0f, -2.0f, -3.0f, 6.0F, 6.0F, 6.0F), PartPose.offsetAndRotation(0.0f, 23.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("trebuchetRFAngle", CubeListBuilder.create().texOffs(0, 177).addBox(0.0f, 0.0f, -1.0f, 2.0F, 38.0F, 2.0F), PartPose.offsetAndRotation(-8.0f, -33.0f, 0.0f, -0.5061452f, 0.366519f, 0.0f));
		turretMidBeam.addOrReplaceChild("trebuchetLFAngle", CubeListBuilder.create().texOffs(0, 177).addBox(-2.0f, 0.0f, -1.0f, 2.0F, 38.0F, 2.0F), PartPose.offsetAndRotation(8.0f, -33.0f, 0.0f, -0.5061452f, -0.36651903f, 0.0f));
		turretMidBeam.addOrReplaceChild("trebuchetLeftAngle", CubeListBuilder.create().texOffs(0, 218).addBox(-2.0f, 0.0f, -2.0f, 2.0F, 36.0F, 2.0F), PartPose.offsetAndRotation(8.0f, -34.0f, 1.0f, 5.0558447E-7f, 0.0f, -0.34906563f));
		turretMidBeam.addOrReplaceChild("trebuchetRightAngle", CubeListBuilder.create().texOffs(0, 218).addBox(0.0f, 0.0f, -2.0f, 2.0F, 36.0F, 2.0F), PartPose.offsetAndRotation(-8.0f, -34.0f, 1.0f, 5.0558447E-7f, 0.0f, 0.34906584f));
		turretMidBeam.addOrReplaceChild("trebuchetRRAngle", CubeListBuilder.create().texOffs(0, 177).addBox(0.0f, 0.0f, -1.0f, 2.0F, 38.0F, 2.0F), PartPose.offsetAndRotation(-8.0f, -33.0f, 0.0f, 0.5061455f, -0.36651894f, -1.2483568E-8f));
		turretMidBeam.addOrReplaceChild("trebuchetLRAngle", CubeListBuilder.create().texOffs(0, 177).addBox(-2.0f, 0.0f, -1.0f, 2.0F, 38.0F, 2.0F), PartPose.offsetAndRotation(8.0f, -33.0f, 0.0f, 0.5061455f, 0.34906572f, 0.0f));
		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		standBaseMidBeam.render(poseStack, buffer, packedLight, packedOverlay, color);
		turretMidBeam.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void setArmRotations(float main, float string) {
		trebuchetAxle.xRot = toRadians(main);
		trebuchetWeightAxle.xRot = toRadians(-main);
		trebuchetSlingString.xRot = toRadians(string);
	}

	public void setTurretRotation(float yaw) {
		this.turretMidBeam.yRot = toRadians(yaw);
	}

	private static float toRadians(float degrees) {
		return degrees * TORADIANS;
	}
}
