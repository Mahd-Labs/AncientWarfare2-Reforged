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
public class ModelBallistaStand {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "ballista_stand"), "main");
	private static final float TORADIANS = 3.141592653589793f / 180.f; //core.util.Trig.TORADIANS

	public final ModelPart baseMain;
	public final ModelPart armMain;
	public final ModelPart trigger1;
	public final ModelPart crankAxle;
	public final ModelPart armRightMain;
	public final ModelPart stringRight;
	public final ModelPart armLeftMain;
	public final ModelPart stringLeft;

	public ModelBallistaStand(ModelPart root) {
		baseMain = root.getChild("baseMain");
		armMain = root.getChild("armMain");
		trigger1 = root.getChild("armMain").getChild("trigger1");
		crankAxle = root.getChild("armMain").getChild("crankAxle");
		armRightMain = root.getChild("armMain").getChild("armRightMain");
		stringRight = root.getChild("armMain").getChild("armRightMain").getChild("stringRight");
		armLeftMain = root.getChild("armMain").getChild("armLeftMain");
		stringLeft = root.getChild("armMain").getChild("armLeftMain").getChild("stringLeft");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition baseMain = partdefinition.addOrReplaceChild("baseMain", CubeListBuilder.create().texOffs(0, 21).addBox(-7.0f, -2.0f, -7.0f, 14.0F, 2.0F, 14.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition basePillar = baseMain.addOrReplaceChild("basePillar", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, 0.0f, -2.0f, 4.0F, 10.0F, 4.0F), PartPose.offsetAndRotation(0.0f, -12.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		basePillar.addOrReplaceChild("pivot", CubeListBuilder.create().texOffs(17, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0f, -2.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition armMain = partdefinition.addOrReplaceChild("armMain", CubeListBuilder.create().texOffs(0, 128).addBox(-1.5f, -2.0f, -4.5f, 3.0F, 2.0F, 28.0F), PartPose.offsetAndRotation(0.0f, -14.0f, 0.0f, -6.585082E-7f, -2.3593943E-6f, 0.0f));
		armMain.addOrReplaceChild("armFront", CubeListBuilder.create().texOffs(63, 128).addBox(0.0f, 0.0f, 0.0f, 3.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(-1.5f, -2.0f, -11.5f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("turretHorizontalBrace2", CubeListBuilder.create().texOffs(63, 135).addBox(-13.0f, 0.0f, -3.0f, 13.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(11.0f, -1.5f, -4.5f, 0.0f, 0.5410521f, 0.0f));
		armMain.addOrReplaceChild("turretHorizontalBrace3", CubeListBuilder.create().texOffs(63, 140).addBox(0.0f, 0.0f, 0.0f, 22.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(-11.0f, -2.0f, -7.5f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("armMidBrace", CubeListBuilder.create().texOffs(0, 159).addBox(0.0f, 0.0f, 0.0f, 3.0F, 1.0F, 35.0F), PartPose.offsetAndRotation(-1.5f, -3.0f, -11.5f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("armSlotLeft", CubeListBuilder.create().texOffs(77, 159).addBox(0.0f, 0.0f, 0.0f, 1.0F, 1.0F, 35.0F), PartPose.offsetAndRotation(0.5f, -4.0f, -11.5f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("armSlotRight", CubeListBuilder.create().texOffs(77, 159).addBox(0.0f, 0.0f, 0.0f, 1.0F, 1.0F, 35.0F), PartPose.offsetAndRotation(-1.5f, -4.0f, -11.5f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("armleftVertical3", CubeListBuilder.create().texOffs(78, 128).addBox(0.0f, 0.0f, 0.0f, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(10.0f, -3.0f, -6.0f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("armLeftVertical2", CubeListBuilder.create().texOffs(78, 128).addBox(0.0f, 0.0f, 0.0f, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(10.0f, -7.0f, -6.0f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("armLeftVertical1", CubeListBuilder.create().texOffs(83, 128).addBox(0.0f, 0.0f, 0.0f, 1.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(10.0f, -7.0f, -7.0f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("turretHorizontalBrace4", CubeListBuilder.create().texOffs(63, 140).addBox(0.0f, 0.0f, 0.0f, 22.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(-11.0f, -9.0f, -7.5f, 0.0f, 0.0f, 0.0f));
		PartDefinition leftTensionerRope = armMain.addOrReplaceChild("leftTensionerRope", CubeListBuilder.create().texOffs(114, 128).addBox(-0.5f, -3.0f, -0.5f, 1.0F, 11.0F, 1.0F), PartPose.offsetAndRotation(5.5f, -7.0f, -6.0f, 0.0f, 0.0f, 0.0f));
		leftTensionerRope.addOrReplaceChild("leftTensioner", CubeListBuilder.create().texOffs(88, 128).addBox(-1.0f, -0.5f, -0.5f, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, -3.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		leftTensionerRope.addOrReplaceChild("leftTensioner2", CubeListBuilder.create().texOffs(88, 131).addBox(-0.5f, -0.5f, -1.0f, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0f, -3.0f, -0.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition rightTensionerRope = armMain.addOrReplaceChild("rightTensionerRope", CubeListBuilder.create().texOffs(114, 128).addBox(-0.5f, -3.0f, -0.5f, 1.0F, 11.0F, 1.0F), PartPose.offsetAndRotation(-5.5f, -7.0f, -6.0f, 0.0f, 0.0f, 0.0f));
		rightTensionerRope.addOrReplaceChild("rightTensioner", CubeListBuilder.create().texOffs(88, 128).addBox(-1.0f, -0.5f, -0.5f, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, -3.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		rightTensionerRope.addOrReplaceChild("rightTensioner2", CubeListBuilder.create().texOffs(88, 131).addBox(-0.5f, -0.5f, -1.0f, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0f, -3.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("turretHorizontalBrace1", CubeListBuilder.create().texOffs(63, 135).addBox(0.0f, 0.0f, -3.0f, 13.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-11.0f, -1.5f, -4.5f, 0.0f, -0.5410521f, 0.0f));
		armMain.addOrReplaceChild("armRightVertical3", CubeListBuilder.create().texOffs(78, 128).addBox(0.0f, 0.0f, 0.0f, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-11.0f, -3.0f, -6.0f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("armRightVertical2", CubeListBuilder.create().texOffs(78, 128).addBox(0.0f, 0.0f, 0.0f, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-11.0f, -7.0f, -6.0f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("armRightVertical1", CubeListBuilder.create().texOffs(83, 128).addBox(0.0f, 0.0f, 0.0f, 1.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-11.0f, -7.0f, -7.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition trigger1 = armMain.addOrReplaceChild("trigger1", CubeListBuilder.create().texOffs(63, 146).addBox(-0.5f, -1.0f, 0.0f, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(-0.0f, -1.0f, 17.5f, -1.256629f, 0.0f, 0.0f));
		trigger1.addOrReplaceChild("trigger2", CubeListBuilder.create().texOffs(76, 146).addBox(-0.5f, -4.0f, 0.0f, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 1.2217292f, 1.934953E-7f, 3.1625038E-7f));
		PartDefinition crankAxle = armMain.addOrReplaceChild("crankAxle", CubeListBuilder.create().texOffs(63, 153).addBox(0.0f, -0.5f, -0.5f, 4.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.0f, -2.0f, 21.0f, 0.0f, 0.0f, 0.0f));
		crankAxle.addOrReplaceChild("crankHandle1", CubeListBuilder.create().texOffs(81, 146).addBox(-0.5f, -2.5f, -0.5f, 1.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		crankAxle.addOrReplaceChild("crankHandle2", CubeListBuilder.create().texOffs(86, 146).addBox(-0.5f, -0.5f, -2.5f, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(-0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("catch2", CubeListBuilder.create().texOffs(99, 146).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(-1.0f, -6.0f, 20.5f, -0.8552113f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("catch1", CubeListBuilder.create().texOffs(99, 152).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-1.0f, -6.0f, 17.5f, 0.0f, 0.0f, 0.0f));
		PartDefinition armRightMain = armMain.addOrReplaceChild("armRightMain", CubeListBuilder.create().texOffs(0, 215).addBox(-6.5f, -1.0f, -1.0f, 8.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-5.5f, -5.0f, -6.0f, 0.0f, 0.5235982f, 0.0f));
		armRightMain.addOrReplaceChild("armRightMainInner", CubeListBuilder.create().texOffs(0, 203).addBox(-2.5f, -1.0f, 0.0f, 4.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		armRightMain.addOrReplaceChild("armRightMainInner3", CubeListBuilder.create().texOffs(0, 196).addBox(-6.5f, 0.0f, 0.0f, 4.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		armRightMain.addOrReplaceChild("armRightMainInner2", CubeListBuilder.create().texOffs(0, 199).addBox(-6.5f, -0.5f, -0.5f, 4.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		armRightMain.addOrReplaceChild("armRightInner", CubeListBuilder.create().texOffs(0, 208).addBox(-13.25f, 0.0f, -0.5f, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		armRightMain.addOrReplaceChild("armRightOuter", CubeListBuilder.create().texOffs(0, 211).addBox(-13.5f, -0.5f, -1.0f, 7.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		armRightMain.addOrReplaceChild("stringRight", CubeListBuilder.create().texOffs(0, 220).addBox(0.0f, -0.5f, 0.0f, 17.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-13.0f, 0.5f, 0.0f, 0.0f, -0.5235985f, 0.0f));
		PartDefinition armLeftMain = armMain.addOrReplaceChild("armLeftMain", CubeListBuilder.create().texOffs(0, 215).addBox(-1.5f, 0.0f, -1.0f, 8.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(5.5f, -6.0f, -6.0f, 0.0f, -0.5235988f, 0.0f));
		armLeftMain.addOrReplaceChild("armLeftMainInner", CubeListBuilder.create().texOffs(0, 203).addBox(-1.5f, 0.0f, 0.0f, 4.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		armLeftMain.addOrReplaceChild("armLeftOuter", CubeListBuilder.create().texOffs(0, 211).addBox(6.5f, 0.5f, -1.0f, 7.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		armLeftMain.addOrReplaceChild("armLeftMainInner2", CubeListBuilder.create().texOffs(0, 199).addBox(2.5f, 0.5f, -0.5f, 4.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		armLeftMain.addOrReplaceChild("armLeftMainInner3", CubeListBuilder.create().texOffs(0, 196).addBox(2.5f, 1.0f, -0.0f, 4.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		armLeftMain.addOrReplaceChild("armLeftInner", CubeListBuilder.create().texOffs(0, 208).addBox(6.25f, 1.0f, -0.5f, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		armLeftMain.addOrReplaceChild("stringLeft", CubeListBuilder.create().texOffs(0, 220).addBox(-17.0f, -0.5f, 0.0f, 17.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(13.0f, 1.5f, 0.0f, 0.0f, 0.5235985f, 0.0f));
		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		baseMain.render(poseStack, buffer, packedLight, packedOverlay, color);
		armMain.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void setBowAndStringRotation(float bow, float string) {
		this.armLeftMain.yRot = toRadians(-bow);
		this.armRightMain.yRot = toRadians(bow);
		this.stringLeft.yRot = toRadians(-string);
		this.stringRight.yRot = toRadians(string);
	}

	public void setTurretRotation(float yaw, float pitch) {
		this.armMain.yRot = toRadians(yaw);
		this.armMain.xRot = toRadians(pitch);
	}

	public void setCrankRotations(float angle) {
		this.crankAxle.xRot = toRadians(angle);
	}

	public void setTriggerAngle(float angle) {
		this.trigger1.xRot = toRadians(angle);
	}

	private static float toRadians(float degrees) {
		return degrees * TORADIANS;
	}
}
