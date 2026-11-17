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
public class ModelArrow2 {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "arrow2"), "main");
	private static final float TORADIANS = 3.141592653589793f / 180.f; //core.util.Trig.TORADIANS

	public final ModelPart headPoint;

	public ModelArrow2(ModelPart root) {
		headPoint = root.getChild("headPoint");
		headPoint.yRot = toRadians(90.f);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition headPoint = partdefinition.addOrReplaceChild("headPoint", CubeListBuilder.create().texOffs(0, 7).addBox(-0.5f, -0.5f, -0.5f, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		headPoint.addOrReplaceChild("shaftCoreTop", CubeListBuilder.create().texOffs(0, 9).addBox(0.5f, -2.5f, -1.0f, 80.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		headPoint.addOrReplaceChild("shaftCoreBottom", CubeListBuilder.create().texOffs(0, 9).addBox(0.5f, 1.5f, -1.0f, 80.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		headPoint.addOrReplaceChild("shaftCoreLeft", CubeListBuilder.create().texOffs(0, 13).addBox(0.5f, -1.0f, -2.5f, 80.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		headPoint.addOrReplaceChild("shaftCoreRight", CubeListBuilder.create().texOffs(0, 13).addBox(0.5f, -1.0f, 1.5f, 80.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		headPoint.addOrReplaceChild("shaftCoreRightT1", CubeListBuilder.create().texOffs(0, 13).addBox(0.5f, -1.5f, 1.0f, 80.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		headPoint.addOrReplaceChild("shaftCoreRightB1", CubeListBuilder.create().texOffs(0, 13).addBox(0.5f, 0.5f, 1.0f, 80.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		headPoint.addOrReplaceChild("shaftCoreLeftB1", CubeListBuilder.create().texOffs(0, 13).addBox(0.5f, 0.5f, -2.0f, 80.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		headPoint.addOrReplaceChild("shaftCoreLeftT1", CubeListBuilder.create().texOffs(0, 13).addBox(0.5f, -1.5f, -2.0f, 80.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		headPoint.addOrReplaceChild("shaftCoreTopR", CubeListBuilder.create().texOffs(0, 13).addBox(0.5f, -2.0f, 0.5f, 80.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		headPoint.addOrReplaceChild("shaftCoreTopL", CubeListBuilder.create().texOffs(0, 13).addBox(0.5f, -2.0f, -1.5f, 80.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		headPoint.addOrReplaceChild("shaftCoreBottomL", CubeListBuilder.create().texOffs(0, 13).addBox(0.5f, 1.0f, -1.5f, 80.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		headPoint.addOrReplaceChild("shaftCoreBottomR", CubeListBuilder.create().texOffs(0, 13).addBox(0.5f, 1.0f, 0.5f, 80.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		headPoint.addOrReplaceChild("veinTop", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5f, -0.5f, -0.5f, 15.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(66.0f, -2.0f, 0.0f, 1.0402973E-9f, 0.0f, -0.29670483f));
		headPoint.addOrReplaceChild("veinBottom", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5f, -4.5f, -0.5f, 15.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(66.0f, 2.0f, 0.0f, 1.0402973E-9f, 0.0f, 0.29670596f));
		headPoint.addOrReplaceChild("veinRight", CubeListBuilder.create().texOffs(33, 0).addBox(-0.5f, -0.5f, -4.5f, 15.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(66.0f, 0.0f, 2.0f, -3.2249215E-8f, -0.2967058f, 0.0f));
		headPoint.addOrReplaceChild("veinLeft", CubeListBuilder.create().texOffs(33, 0).addBox(-0.5f, -0.5f, -0.5f, 15.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(66.0f, 0.0f, -2.0f, -3.2249215E-8f, 0.29670596f, 0.0f));
		headPoint.addOrReplaceChild("tipRight", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5f, -0.5f, -3.5f, 15.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(-7.0f, 0.0f, 2.0f, -3.2249215E-8f, -0.26179862f, 0.0f));
		headPoint.addOrReplaceChild("tipLeft", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5f, -0.5f, -0.5f, 15.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(-7.0f, 0.0f, -2.0f, -3.2249215E-8f, 0.2617993f, 0.0f));
		headPoint.addOrReplaceChild("tipTop", CubeListBuilder.create().texOffs(39, 17).addBox(-0.5f, -0.5f, -0.5f, 15.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-7.0f, -2.0f, 0.0f, 1.0402973E-9f, 0.0f, -0.261798f));
		headPoint.addOrReplaceChild("tipBottom", CubeListBuilder.create().texOffs(39, 17).addBox(-0.5f, -3.5f, -0.5f, 15.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-7.0f, 2.0f, 0.0f, 1.0402973E-9f, 0.0f, 0.2617993f));
		headPoint.addOrReplaceChild("tipVert", CubeListBuilder.create().texOffs(29, 23).addBox(-0.5f, -0.5f, -0.5f, 4.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-10.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.7853976f));
		headPoint.addOrReplaceChild("tipHoriz", CubeListBuilder.create().texOffs(40, 23).addBox(-0.5f, -0.5f, -0.5f, 4.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(-10.0f, 0.0f, 0.0f, 0.0f, 0.7853982f, 0.0f));
		headPoint.addOrReplaceChild("tipShaft", CubeListBuilder.create().texOffs(0, 23).addBox(-0.5f, -2.0f, -2.0f, 10.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(-7.0f, 0.0f, 0.0f, 0.7853982f, 0.0f, -8.530438E-8f));
		return LayerDefinition.create(meshdefinition, 192, 36);
	}

	public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		headPoint.render(poseStack, buffer, packedLight, packedOverlay, color);
	}

	private static float toRadians(float degrees) {
		return degrees * TORADIANS;
	}
}
