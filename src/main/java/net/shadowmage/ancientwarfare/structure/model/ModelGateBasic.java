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
public class ModelGateBasic {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "gate_basic"), "main");
	private static final float TORADIANS = 3.141592653589793f / 180.f; //core.util.Trig.TORADIANS

	public final ModelPart part1;
	public final ModelPart top1;
	public final ModelPart barsTop;
	public final ModelPart cornerTop1;
	public final ModelPart side1;

	public ModelGateBasic(ModelPart root) {
		part1 = root.getChild("part1");
		top1 = root.getChild("top1");
		barsTop = root.getChild("barsTop");
		cornerTop1 = root.getChild("cornerTop1");
		side1 = root.getChild("side1");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		partdefinition.addOrReplaceChild("part1", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -16.0f, -8.0f, 2.0F, 16.0F, 16.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition top1 = partdefinition.addOrReplaceChild("top1", CubeListBuilder.create().texOffs(41, 26).addBox(4.0f, 0.0f, -8.0f, 2.0F, 6.0F, 16.0F), PartPose.offsetAndRotation(0.0f, -16.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		top1.addOrReplaceChild("top4", CubeListBuilder.create().texOffs(39, 0).addBox(1.0f, 0.0f, -8.0f, 3.0F, 8.0F, 16.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		top1.addOrReplaceChild("top3", CubeListBuilder.create().texOffs(39, 0).addBox(0.0f, 0.0f, -8.0f, 3.0F, 8.0F, 16.0F), PartPose.offsetAndRotation(-4.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		top1.addOrReplaceChild("top2", CubeListBuilder.create().texOffs(41, 26).addBox(0.0f, 0.0f, -8.0f, 2.0F, 6.0F, 16.0F), PartPose.offsetAndRotation(-6.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition barsTop = partdefinition.addOrReplaceChild("barsTop", CubeListBuilder.create().texOffs(0, 75).addBox(-1.0f, -13.0f, -8.0f, 2.0F, 2.0F, 16.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		barsTop.addOrReplaceChild("barsLeft1", CubeListBuilder.create().texOffs(0, 52).addBox(-1.0f, -16.0f, -5.0f, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		barsTop.addOrReplaceChild("barsLeft3", CubeListBuilder.create().texOffs(0, 58).addBox(-1.0f, -11.0f, -5.0f, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		barsTop.addOrReplaceChild("barsLeft5", CubeListBuilder.create().texOffs(0, 67).addBox(-1.0f, -3.0f, -5.0f, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		barsTop.addOrReplaceChild("barsRight1", CubeListBuilder.create().texOffs(27, 52).addBox(-1.0f, -16.0f, 3.0f, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		barsTop.addOrReplaceChild("barsRight3", CubeListBuilder.create().texOffs(27, 58).addBox(-1.0f, -11.0f, 3.0f, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		barsTop.addOrReplaceChild("barsRight5", CubeListBuilder.create().texOffs(27, 67).addBox(-1.0f, -3.0f, 3.0f, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		barsTop.addOrReplaceChild("barsBottom", CubeListBuilder.create().texOffs(0, 33).addBox(-1.0f, -5.0f, -8.0f, 2.0F, 2.0F, 16.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition cornerTop1 = partdefinition.addOrReplaceChild("cornerTop1", CubeListBuilder.create().texOffs(39, 49).addBox(1.0f, 0.0f, 0.0f, 3.0F, 8.0F, 8.0F), PartPose.offsetAndRotation(0.0f, -16.0f, 0.0f, 0.0f, 2.1014006E-7f, 0.0f));
		cornerTop1.addOrReplaceChild("cornerTop2", CubeListBuilder.create().texOffs(39, 66).addBox(0.0f, 0.0f, 0.0f, 2.0F, 6.0F, 10.0F), PartPose.offsetAndRotation(4.0f, 0.0f, -2.0f, 0.0f, 0.0f, 0.0f));
		cornerTop1.addOrReplaceChild("cornerSide2", CubeListBuilder.create().texOffs(62, 83).addBox(0.0f, 0.0f, 0.0f, 2.0F, 16.0F, 6.0F), PartPose.offsetAndRotation(4.0f, 0.0f, -8.0f, 0.0f, 0.0f, 0.0f));
		cornerTop1.addOrReplaceChild("cornerSide1", CubeListBuilder.create().texOffs(39, 83).addBox(0.0f, 0.0f, 0.0f, 3.0F, 16.0F, 8.0F), PartPose.offsetAndRotation(1.0f, 0.0f, -8.0f, 0.0f, 0.0f, 0.0f));
		cornerTop1.addOrReplaceChild("cornerTop3", CubeListBuilder.create().texOffs(62, 49).addBox(0.0f, 0.0f, 0.0f, 3.0F, 8.0F, 8.0F), PartPose.offsetAndRotation(-4.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		cornerTop1.addOrReplaceChild("cornerTop4", CubeListBuilder.create().texOffs(64, 66).addBox(0.0f, 0.0f, 0.0f, 2.0F, 6.0F, 10.0F), PartPose.offsetAndRotation(-6.0f, 0.0f, -2.0f, 0.0f, 0.0f, 0.0f));
		cornerTop1.addOrReplaceChild("cornerSide3", CubeListBuilder.create().texOffs(39, 108).addBox(0.0f, 0.0f, 0.0f, 3.0F, 16.0F, 8.0F), PartPose.offsetAndRotation(-4.0f, 0.0f, -8.0f, 0.0f, 0.0f, 0.0f));
		cornerTop1.addOrReplaceChild("cornerSide4", CubeListBuilder.create().texOffs(62, 108).addBox(0.0f, 0.0f, 0.0f, 2.0F, 16.0F, 6.0F), PartPose.offsetAndRotation(-6.0f, 0.0f, -8.0f, 0.0f, 0.0f, 0.0f));
		PartDefinition side1 = partdefinition.addOrReplaceChild("side1", CubeListBuilder.create().texOffs(39, 83).addBox(1.0f, 0.0f, -8.0f, 3.0F, 16.0F, 8.0F), PartPose.offsetAndRotation(0.0f, -16.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		side1.addOrReplaceChild("side2", CubeListBuilder.create().texOffs(62, 83).addBox(0.0f, 0.0f, -8.0f, 2.0F, 16.0F, 6.0F), PartPose.offsetAndRotation(4.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		side1.addOrReplaceChild("side3", CubeListBuilder.create().texOffs(39, 108).addBox(0.0f, 0.0f, -8.0f, 3.0F, 16.0F, 8.0F), PartPose.offsetAndRotation(-4.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		side1.addOrReplaceChild("side4", CubeListBuilder.create().texOffs(62, 108).addBox(0.0f, 0.0f, -8.0f, 2.0F, 16.0F, 6.0F), PartPose.offsetAndRotation(-6.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		part1.render(poseStack, buffer, packedLight, packedOverlay, color);
		top1.render(poseStack, buffer, packedLight, packedOverlay, color);
		barsTop.render(poseStack, buffer, packedLight, packedOverlay, color);
		cornerTop1.render(poseStack, buffer, packedLight, packedOverlay, color);
		side1.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void renderTop(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		top1.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void renderCorner(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		cornerTop1.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void renderSide(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		side1.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void renderSolidWall(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		part1.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void renderBars(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		barsTop.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	public void setModelRotation(float rot) {
		float val = toRadians(rot);
		top1.yRot = val;
		cornerTop1.yRot = val;
		side1.yRot = val;
		part1.yRot = val;
		barsTop.yRot = val;
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
