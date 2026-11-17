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
public class ModelCannonStandFixed {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "cannon_stand_fixed"), "main");
	private static final float TORADIANS = 3.141592653589793f / 180.f; //core.util.Trig.TORADIANS

	public final ModelPart cannonBaseMid;
	public final ModelPart axleRight;

	public ModelCannonStandFixed(ModelPart root) {
		cannonBaseMid = root.getChild("cannonBaseMid");
		axleRight = root.getChild("cannonBaseMid").getChild("cannonBaseMid2").getChild("axleRight");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition cannonBaseMid = partdefinition.addOrReplaceChild("cannonBaseMid", CubeListBuilder.create().texOffs(134, 155).addBox(-8.0f, -2.0f, -21.5f, 16.0F, 2.0F, 28.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 3.1415927f, 0.0f));
		PartDefinition cannonBaseMid2 = cannonBaseMid.addOrReplaceChild("cannonBaseMid2", CubeListBuilder.create().texOffs(134, 186).addBox(-6.0f, -2.0f, -14.5f, 12.0F, 2.0F, 24.0F), PartPose.offsetAndRotation(0.0f, -2.0f, -3.0f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseLeft1", CubeListBuilder.create().texOffs(134, 213).addBox(-6.0f, -2.0f, -14.5f, 2.0F, 2.0F, 25.0F), PartPose.offsetAndRotation(-2.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseLeft2", CubeListBuilder.create().texOffs(189, 213).addBox(-6.0f, -2.0f, -14.5f, 2.0F, 2.0F, 22.0F), PartPose.offsetAndRotation(-2.0f, -2.0f, 2.0f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseLeft3", CubeListBuilder.create().texOffs(207, 190).addBox(-6.0f, -2.0f, -14.5f, 2.0F, 2.0F, 20.0F), PartPose.offsetAndRotation(-2.0f, -4.0f, 4.0f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseLeft4", CubeListBuilder.create().texOffs(93, 213).addBox(-6.0f, -2.0f, -14.5f, 2.0F, 2.0F, 18.0F), PartPose.offsetAndRotation(-2.0f, -6.0f, 6.0f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseTopLeft", CubeListBuilder.create().texOffs(204, 128).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 14.0F), PartPose.offsetAndRotation(-8.0f, -9.0f, -4.5f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseTopLeft2", CubeListBuilder.create().texOffs(204, 144).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-8.0f, -10.0f, -0.5f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseTopLeft4", CubeListBuilder.create().texOffs(204, 144).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-8.0f, -11.0f, 1.5f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseTopLeft3", CubeListBuilder.create().texOffs(204, 144).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-8.0f, -10.0f, 3.5f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseLeft2copy10", CubeListBuilder.create().texOffs(189, 213).addBox(-6.0f, -2.0f, -14.5f, 2.0F, 2.0F, 22.0F), PartPose.offsetAndRotation(12.0f, -2.0f, 2.0f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseLeft1copy9", CubeListBuilder.create().texOffs(134, 213).addBox(-6.0f, -2.0f, -14.5f, 2.0F, 2.0F, 25.0F), PartPose.offsetAndRotation(12.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseLeft3copy11", CubeListBuilder.create().texOffs(207, 190).addBox(-6.0f, -2.0f, -14.5f, 2.0F, 2.0F, 20.0F), PartPose.offsetAndRotation(12.0f, -4.0f, 4.0f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseLeft4copy12", CubeListBuilder.create().texOffs(93, 213).addBox(-6.0f, -2.0f, -14.5f, 2.0F, 2.0F, 18.0F), PartPose.offsetAndRotation(12.0f, -6.0f, 6.0f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseTopLeftcopy13", CubeListBuilder.create().texOffs(204, 128).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 14.0F), PartPose.offsetAndRotation(6.0f, -9.0f, -4.5f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseTopLeft2copy14", CubeListBuilder.create().texOffs(204, 144).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(6.0f, -10.0f, -0.5f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseTopLeft4copy15", CubeListBuilder.create().texOffs(204, 144).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(6.0f, -11.0f, 1.5f, 0.0f, 0.0f, 0.0f));
		cannonBaseMid2.addOrReplaceChild("cannonBaseTopLeft3copy16", CubeListBuilder.create().texOffs(204, 144).addBox(0.0f, 0.0f, 0.0f, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(6.0f, -10.0f, 3.5f, 0.0f, 0.0f, 0.0f));
		PartDefinition axleRight = cannonBaseMid2.addOrReplaceChild("axleRight", CubeListBuilder.create().texOffs(0, 207).addBox(-0.5f, -0.5f, -0.5f, 3.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(5.5f, -9.5f, 3.0f, -7.1052307E-7f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonRightPanel", CubeListBuilder.create().texOffs(0, 128).addBox(0.0f, 0.0f, 0.0f, 1.0F, 6.0F, 11.0F), PartPose.offsetAndRotation(-1.5f, -3.0f, -9.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareRearRight", CubeListBuilder.create().texOffs(0, 213).addBox(0.0f, 0.0f, 0.0f, 1.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(-1.0f, -3.5f, -5.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareRearTopRight", CubeListBuilder.create().texOffs(65, 207).addBox(0.0f, 0.0f, 0.0f, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-3.0f, -4.5f, -5.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareRearTop", CubeListBuilder.create().texOffs(72, 207).addBox(0.0f, 0.0f, 0.0f, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-9.0f, -5.5f, -5.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareRearTopLeft", CubeListBuilder.create().texOffs(89, 207).addBox(0.0f, 0.0f, 0.0f, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-10.0f, -4.5f, -5.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareRearLeft", CubeListBuilder.create().texOffs(5, 213).addBox(0.0f, 0.0f, 0.0f, 1.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(-11.0f, -3.5f, -5.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareRearBottomLeft", CubeListBuilder.create().texOffs(89, 211).addBox(0.0f, 0.0f, 0.0f, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-10.0f, 2.5f, -5.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareRearBottom", CubeListBuilder.create().texOffs(72, 211).addBox(0.0f, 0.0f, 0.0f, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-9.0f, 4.5f, -5.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareRearBottomRight", CubeListBuilder.create().texOffs(65, 211).addBox(0.0f, 0.0f, 0.0f, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-3.0f, 2.5f, -5.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonBottomRear", CubeListBuilder.create().texOffs(106, 128).addBox(0.0f, 0.0f, 0.0f, 8.0F, 1.0F, 12.0F), PartPose.offsetAndRotation(-9.5f, 3.0f, -10.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonRightSide", CubeListBuilder.create().texOffs(0, 146).addBox(0.0f, 0.0f, 0.0f, 1.0F, 6.0F, 32.0F), PartPose.offsetAndRotation(-2.5f, -3.0f, -10.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonBottomPanel", CubeListBuilder.create().texOffs(134, 142).addBox(0.0f, 0.0f, 0.0f, 6.0F, 1.0F, 11.0F), PartPose.offsetAndRotation(-8.5f, 4.0f, -9.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonRearBreech", CubeListBuilder.create().texOffs(50, 128).addBox(0.0f, 0.0f, 0.0f, 6.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(-8.5f, -3.0f, -11.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonTopRear", CubeListBuilder.create().texOffs(147, 128).addBox(0.0f, 0.0f, 0.0f, 8.0F, 1.0F, 12.0F), PartPose.offsetAndRotation(-9.5f, -4.0f, -10.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonLeftPanel", CubeListBuilder.create().texOffs(25, 128).addBox(0.0f, 0.0f, 0.0f, 1.0F, 6.0F, 11.0F), PartPose.offsetAndRotation(-10.5f, -3.0f, -9.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonLeftSide", CubeListBuilder.create().texOffs(67, 146).addBox(0.0f, 0.0f, 0.0f, 1.0F, 6.0F, 32.0F), PartPose.offsetAndRotation(-9.5f, -3.0f, -10.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonTopPanel", CubeListBuilder.create().texOffs(169, 142).addBox(0.0f, 0.0f, 0.0f, 6.0F, 1.0F, 11.0F), PartPose.offsetAndRotation(-8.5f, -5.0f, -9.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonBottomFront", CubeListBuilder.create().texOffs(0, 185).addBox(0.0f, 0.0f, 0.0f, 6.0F, 1.0F, 20.0F), PartPose.offsetAndRotation(-8.5f, 3.0f, 2.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonTopFront", CubeListBuilder.create().texOffs(53, 185).addBox(0.0f, 0.0f, 0.0f, 6.0F, 1.0F, 20.0F), PartPose.offsetAndRotation(-8.5f, -4.0f, 2.0f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("axleLeft", CubeListBuilder.create().texOffs(0, 207).addBox(0.0f, 0.0f, 0.0f, 3.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-13.5f, -0.5f, -0.5f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareLipTop", CubeListBuilder.create().texOffs(17, 210).addBox(0.0f, 0.0f, 0.0f, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-9.0f, -4.5f, 21.5f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareLipTR", CubeListBuilder.create().texOffs(15, 207).addBox(0.0f, 0.0f, 0.0f, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.5f, -4.0f, 21.5f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareLipRight", CubeListBuilder.create().texOffs(0, 222).addBox(0.0f, 0.0f, 0.0f, 1.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(-2.0f, -3.5f, 21.5f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareLipBR", CubeListBuilder.create().texOffs(10, 207).addBox(0.0f, 0.0f, 0.0f, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.5f, 3.0f, 21.5f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareLipBottom", CubeListBuilder.create().texOffs(0, 210).addBox(0.0f, 0.0f, 0.0f, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-9.0f, 3.5f, 21.5f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareLipBL", CubeListBuilder.create().texOffs(10, 213).addBox(0.0f, 0.0f, 0.0f, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-9.5f, 3.0f, 21.5f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareLipLeft", CubeListBuilder.create().texOffs(5, 222).addBox(0.0f, 0.0f, 0.0f, 1.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(-10.0f, -3.5f, 21.5f, 0.0f, 0.0f, 0.0f));
		axleRight.addOrReplaceChild("cannonFlareLipTL", CubeListBuilder.create().texOffs(15, 213).addBox(0.0f, 0.0f, 0.0f, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-9.5f, -4.0f, 21.5f, 0.0f, 0.0f, 0.0f));
		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		cannonBaseMid.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void setTurretRotation(float yaw, float pitch) {
		this.axleRight.xRot = toRadians(pitch);
	}

	private static float toRadians(float degrees) {
		return degrees * TORADIANS;
	}
}
