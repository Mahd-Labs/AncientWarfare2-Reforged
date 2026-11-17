package net.shadowmage.ancientwarfare.structure.model;

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
public class ModelGateBridge {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "gate_bridge"), "main");
	private static final float TORADIANS = 3.141592653589793f / 180.f; //core.util.Trig.TORADIANS

	public final ModelPart part1;
	public final ModelPart bridgePiece;
	public final ModelPart bridgeSide1;
	public final ModelPart bridgeSide4;
	public final ModelPart bridgeCorner1;
	public final ModelPart bridgeCorner2;
	public final ModelPart bridgeTop1;
	public final ModelPart bridgeTop2;
	public final ModelPart bridgeSide3;
	public final ModelPart bridgeSide2;
	public final ModelPart bridgeCorner4;
	public final ModelPart bridgeCorner3;

	public ModelGateBridge(ModelPart root) {
		part1 = root.getChild("part1");
		bridgePiece = root.getChild("bridgePiece");
		bridgeSide1 = root.getChild("bridgeSide1");
		bridgeSide4 = root.getChild("bridgeSide4");
		bridgeCorner1 = root.getChild("bridgeCorner1");
		bridgeCorner2 = root.getChild("bridgeCorner2");
		bridgeTop1 = root.getChild("bridgeTop1");
		bridgeTop2 = root.getChild("bridgeTop2");
		bridgeSide3 = root.getChild("bridgeSide3");
		bridgeSide2 = root.getChild("bridgeSide2");
		bridgeCorner4 = root.getChild("bridgeCorner4");
		bridgeCorner3 = root.getChild("bridgeCorner3");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		partdefinition.addOrReplaceChild("part1", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -8.0f, -8.0f, 16.0F, 16.0F, 16.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		partdefinition.addOrReplaceChild("bridgePiece", CubeListBuilder.create().texOffs(0, 33).addBox(-8.0f, -8.0f, -8.0f, 16.0F, 16.0F, 8.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		partdefinition.addOrReplaceChild("bridgeSide1", CubeListBuilder.create().texOffs(0, 58).addBox(-2.0f, -8.0f, 0.0f, 10.0F, 16.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		partdefinition.addOrReplaceChild("bridgeSide4", CubeListBuilder.create().texOffs(27, 58).addBox(-8.0f, -8.0f, 3.0f, 6.0F, 16.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		partdefinition.addOrReplaceChild("bridgeCorner1", CubeListBuilder.create().texOffs(49, 34).addBox(-8.0f, -8.0f, 0.0f, 6.0F, 9.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		partdefinition.addOrReplaceChild("bridgeCorner2", CubeListBuilder.create().texOffs(49, 47).addBox(-8.0f, -8.0f, 3.0f, 10.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		partdefinition.addOrReplaceChild("bridgeTop1", CubeListBuilder.create().texOffs(49, 58).addBox(-8.0f, -8.0f, 0.0f, 16.0F, 9.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		partdefinition.addOrReplaceChild("bridgeTop2", CubeListBuilder.create().texOffs(49, 72).addBox(-8.0f, -8.0f, 3.0f, 16.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		partdefinition.addOrReplaceChild("bridgeSide3", CubeListBuilder.create().texOffs(0, 58).addBox(-8.0f, -8.0f, 0.0f, 10.0F, 16.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		partdefinition.addOrReplaceChild("bridgeSide2", CubeListBuilder.create().texOffs(27, 58).addBox(2.0f, -8.0f, 3.0f, 6.0F, 16.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		partdefinition.addOrReplaceChild("bridgeCorner4", CubeListBuilder.create().texOffs(49, 47).addBox(-2.0f, -8.0f, 3.0f, 10.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		partdefinition.addOrReplaceChild("bridgeCorner3", CubeListBuilder.create().texOffs(49, 34).addBox(2.0f, -8.0f, 0.0f, 6.0F, 9.0F, 3.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		part1.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgePiece.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeSide1.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeSide4.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeCorner1.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeCorner2.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeTop1.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeTop2.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeSide3.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeSide2.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeCorner4.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeCorner3.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void renderGateBlock(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		this.part1.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void renderTop(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		bridgeTop1.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeTop2.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void renderCorner(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		bridgeCorner1.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeCorner2.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeSide1.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeSide2.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void renderCorner2(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		bridgeCorner3.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeCorner4.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeSide3.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeSide4.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void renderSide1(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		bridgeSide1.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeSide2.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void renderSide2(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		bridgeSide3.render(poseStack, buffer, packedLight, packedOverlay, color);
		bridgeSide4.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void renderSolidWall(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		bridgePiece.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void setModelRotation(float rot) {
		float val = toRadians(rot);
		part1.yRot = val;
		bridgePiece.yRot = val;
		bridgeTop1.yRot = val;
		bridgeTop2.yRot = val;
		bridgeCorner1.yRot = val;
		bridgeCorner2.yRot = val;
		bridgeCorner3.yRot = val;
		bridgeCorner4.yRot = val;
		bridgeSide1.yRot = val;
		bridgeSide2.yRot = val;
		bridgeSide3.yRot = val;
		bridgeSide4.yRot = val;
		this.modelRotation = val;
	}

	public float getModelRotation() {
		return modelRotation;
	}

	float modelRotation = 0;

	private static float toRadians(float degrees) {
		return degrees * TORADIANS;
	}
}
