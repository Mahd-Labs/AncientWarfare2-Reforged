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
public class ModelShot {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "shot"), "main");

	public final ModelPart Shape1;

	public ModelShot(ModelPart root) {
		Shape1 = root.getChild("Shape1");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition Shape1 = partdefinition.addOrReplaceChild("Shape1", CubeListBuilder.create().texOffs(0, 28).addBox(0.0f, 0.0f, 0.0f, 12.0F, 1.0F, 12.0F), PartPose.offsetAndRotation(-6.0f, 6.0f, -6.0f, 0.0f, 0.0f, 0.0f));
		Shape1.addOrReplaceChild("Shape5", CubeListBuilder.create().texOffs(27, 71).addBox(0.0f, 0.0f, 0.0f, 1.0F, 12.0F, 12.0F), PartPose.offsetAndRotation(12.0f, -12.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		Shape1.addOrReplaceChild("Shape4", CubeListBuilder.create().texOffs(27, 45).addBox(0.0f, 0.0f, 0.0f, 12.0F, 12.0F, 1.0F), PartPose.offsetAndRotation(0.0f, -12.0f, -1.0f, 0.0f, 0.0f, 0.0f));
		Shape1.addOrReplaceChild("Shape3", CubeListBuilder.create().texOffs(49, 28).addBox(0.0f, 0.0f, 0.0f, 12.0F, 1.0F, 12.0F), PartPose.offsetAndRotation(0.0f, -13.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		Shape1.addOrReplaceChild("Shape7", CubeListBuilder.create().texOffs(0, 71).addBox(0.0f, 0.0f, 0.0f, 1.0F, 12.0F, 12.0F), PartPose.offsetAndRotation(-1.0f, -12.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		Shape1.addOrReplaceChild("Shape2", CubeListBuilder.create().texOffs(41, 59).addBox(0.0f, 0.0f, 0.0f, 10.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(1.0f, -14.0f, 1.0f, 0.0f, 0.0f, 0.0f));
		Shape1.addOrReplaceChild("Shape6", CubeListBuilder.create().texOffs(0, 59).addBox(0.0f, 0.0f, 0.0f, 10.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f));
		Shape1.addOrReplaceChild("Shape11", CubeListBuilder.create().texOffs(23, 96).addBox(0.0f, 0.0f, 0.0f, 1.0F, 10.0F, 10.0F), PartPose.offsetAndRotation(-2.0f, -11.0f, 1.0f, 0.0f, 0.0f, 0.0f));
		Shape1.addOrReplaceChild("Shape8", CubeListBuilder.create().texOffs(0, 45).addBox(0.0f, 0.0f, 0.0f, 12.0F, 12.0F, 1.0F), PartPose.offsetAndRotation(0.0f, -12.0f, 12.0f, 0.0f, 0.0f, 0.0f));
		Shape1.addOrReplaceChild("Shape10", CubeListBuilder.create().texOffs(0, 16).addBox(0.0f, 0.0f, 0.0f, 10.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(1.0f, -11.0f, 13.0f, 0.0f, 0.0f, 0.0f));
		Shape1.addOrReplaceChild("Shape12", CubeListBuilder.create().texOffs(0, 96).addBox(0.0f, 0.0f, 0.0f, 1.0F, 10.0F, 10.0F), PartPose.offsetAndRotation(13.0f, -11.0f, 1.0f, 0.0f, 0.0f, 0.0f));
		Shape1.addOrReplaceChild("Shape9", CubeListBuilder.create().texOffs(23, 16).addBox(0.0f, 0.0f, 0.0f, 10.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(1.0f, -11.0f, -2.0f, 0.0f, 0.0f, 0.0f));
		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		Shape1.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}
