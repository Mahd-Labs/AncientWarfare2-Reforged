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
public class ModelCatapultStandFixed {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "catapult_stand_fixed"), "main");
	private static final float TORADIANS = 3.141592653589793f / 180.f; //core.util.Trig.TORADIANS

	public final ModelPart turretMidBeam;
	public final ModelPart pulleyAxle;
	public final ModelPart armMain;

	public ModelCatapultStandFixed(ModelPart root) {
		turretMidBeam = root.getChild("turretMidBeam");
		pulleyAxle = root.getChild("turretMidBeam").getChild("pulleyAxle");
		armMain = root.getChild("turretMidBeam").getChild("armMain");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition turretMidBeam = partdefinition.addOrReplaceChild("turretMidBeam", CubeListBuilder.create().texOffs(0, 128).addBox(-12.0f, -1.0f, -1.5f, 24.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("upright1", CubeListBuilder.create().texOffs(9, 150).addBox(0.0f, 0.0f, 0.0f, 3.0F, 20.0F, 3.0F), PartPose.offsetAndRotation(-15.0f, -19.0f, -1.5f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("upright2", CubeListBuilder.create().texOffs(9, 150).addBox(0.0f, 0.0f, 0.0f, 3.0F, 20.0F, 3.0F), PartPose.offsetAndRotation(12.0f, -19.0f, -1.5f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretTopHorizontal", CubeListBuilder.create().texOffs(0, 140).addBox(0.0f, 0.0f, 0.0f, 24.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(-12.0f, -19.0f, -1.5f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretLeftAngled", CubeListBuilder.create().texOffs(0, 150).addBox(0.0f, 0.0f, 0.0f, 2.0F, 26.0F, 2.0F), PartPose.offsetAndRotation(12.5f, -19.0f, -1.0f, -0.7853982f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretRightAngled", CubeListBuilder.create().texOffs(0, 150).addBox(0.0f, 0.0f, 0.0f, 2.0F, 26.0F, 2.0F), PartPose.offsetAndRotation(-14.5f, -19.0f, -1.0f, -0.7853961f, 0.0f, 0.0f));
		PartDefinition pulleyAxle = turretMidBeam.addOrReplaceChild("pulleyAxle", CubeListBuilder.create().texOffs(0, 147).addBox(-18.0f, -0.5f, -0.5f, 30.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(2.0f, -3.0f, 0.0f, -4.9455734E-6f, 0.0f, 0.0f));
		PartDefinition pulleyArm1 = pulleyAxle.addOrReplaceChild("pulleyArm1", CubeListBuilder.create().texOffs(68, 140).addBox(-0.5f, -0.5f, -4.5f, 1.0F, 1.0F, 9.0F), PartPose.offsetAndRotation(-18.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		pulleyArm1.addOrReplaceChild("pulleyArm2", CubeListBuilder.create().texOffs(63, 140).addBox(-0.5f, -4.5f, -0.5f, 1.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretFrontBeam", CubeListBuilder.create().texOffs(0, 134).addBox(-15.0f, -1.0f, -1.5f, 30.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, -18.0f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretRearBeam", CubeListBuilder.create().texOffs(0, 134).addBox(-15.0f, -1.0f, -1.5f, 30.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 18.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition armMain = turretMidBeam.addOrReplaceChild("armMain", CubeListBuilder.create().texOffs(0, 179).addBox(-1.0f, -1.0f, -1.5f, 2.0F, 2.0F, 29.0F), PartPose.offsetAndRotation(0.0f, -3.0f, 0.0f, -0.017453417f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("bucket1", CubeListBuilder.create().texOffs(63, 151).addBox(0.0f, 0.0f, 0.0f, 8.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-4.0f, -1.5f, 27.0f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("bucket3", CubeListBuilder.create().texOffs(83, 152).addBox(0.0f, 0.0f, 0.0f, 1.0F, 4.0F, 8.0F), PartPose.offsetAndRotation(4.0f, -1.5f, 28.0f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("bucket6", CubeListBuilder.create().texOffs(63, 157).addBox(0.0f, 0.0f, 0.0f, 8.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-4.0f, -1.5f, 36.0f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("bucket4", CubeListBuilder.create().texOffs(50, 170).addBox(0.0f, 0.0f, 0.0f, 8.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(-4.0f, 2.5f, 28.0f, 0.0f, 0.0f, 0.0f));
		armMain.addOrReplaceChild("bucket2", CubeListBuilder.create().texOffs(83, 165).addBox(0.0f, 0.0f, 0.0f, 1.0F, 4.0F, 8.0F), PartPose.offsetAndRotation(-5.0f, -1.5f, 28.0f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretLeftBeam1", CubeListBuilder.create().texOffs(22, 150).addBox(0.0f, -1.0f, 0.0f, 3.0F, 2.0F, 15.0F), PartPose.offsetAndRotation(12.0f, 0.0f, -16.5f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretLeftBeam2", CubeListBuilder.create().texOffs(22, 150).addBox(0.0f, -1.0f, 0.0f, 3.0F, 2.0F, 15.0F), PartPose.offsetAndRotation(12.0f, 0.0f, 1.5f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretRightBeam1", CubeListBuilder.create().texOffs(22, 150).addBox(0.0f, -1.0f, 0.0f, 3.0F, 2.0F, 15.0F), PartPose.offsetAndRotation(-15.0f, 0.0f, -16.5f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretRightBeam2", CubeListBuilder.create().texOffs(22, 150).addBox(0.0f, -1.0f, 0.0f, 3.0F, 2.0F, 15.0F), PartPose.offsetAndRotation(-15.0f, 0.0f, 1.5f, 0.0f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretLeftAngle2", CubeListBuilder.create().texOffs(0, 150).addBox(0.0f, 0.0f, -2.0f, 2.0F, 26.0F, 2.0F), PartPose.offsetAndRotation(12.5f, -19.0f, 1.0f, 0.7853982f, 0.0f, 0.0f));
		turretMidBeam.addOrReplaceChild("turretRightAngle2", CubeListBuilder.create().texOffs(0, 150).addBox(0.0f, 0.0f, -2.0f, 2.0F, 26.0F, 2.0F), PartPose.offsetAndRotation(-14.5f, -19.0f, 1.0f, 0.7853982f, 1.4044014E-7f, 0.0f));
		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		turretMidBeam.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void setArmRotation(float pitch) {
		this.armMain.xRot = toRadians(pitch);
	}

	public void setCrankRotations(float angle) {
		this.pulleyAxle.xRot = toRadians(angle);
	}

	private static float toRadians(float degrees) {
		return degrees * TORADIANS;
	}
}
