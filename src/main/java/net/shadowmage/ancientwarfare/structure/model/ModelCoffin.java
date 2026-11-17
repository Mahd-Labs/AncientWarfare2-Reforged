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
/**
 * Coffin - Sir Squidly
 * Created using Tabula 7.0.1
 */
public class ModelCoffin {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "coffin"), "main");

	public final ModelPart sideR2;
	public final ModelPart bottom;
	public final ModelPart sideL2;
	public final ModelPart lid;
	public final ModelPart sideBottom;
	public final ModelPart sideTop;
	public final ModelPart sideR1;
	public final ModelPart sideL1;

	public ModelCoffin(ModelPart root) {
		sideR2 = root.getChild("sideR2");
		bottom = root.getChild("bottom");
		sideL2 = root.getChild("sideL2");
		lid = root.getChild("lid");
		sideBottom = root.getChild("sideBottom");
		sideTop = root.getChild("sideTop");
		sideR1 = root.getChild("sideR1");
		sideL1 = root.getChild("sideL1");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		partdefinition.addOrReplaceChild("sideR2", CubeListBuilder.create().texOffs(0, 35).addBox(0.0F, 0.0F, 0.0F, 1.0F, 9.0F, 26.0F), PartPose.offsetAndRotation(-5.0F, 15.0F, -7.89F, 0F, -0.11519173063162574F, 0F));
		partdefinition.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(24, 47).addBox(0.0F, 0.0F, 0.0F, 16.0F, 1.0F, 32.0F), PartPose.offsetAndRotation(-8.0F, 22.9F, -8.0F, 0.0F, 0.0F, 0.0F));
		partdefinition.addOrReplaceChild("sideL2", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 9.0F, 26.0F), PartPose.offsetAndRotation(4.0F, 15.0F, -7.89F, 0F, 0.11519173063162574F, 0F));
		PartDefinition lid = partdefinition.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(24, 0).addBox(0.0F, 0.0F, 0.0F, 16.0F, 0.0F, 32.0F), PartPose.offsetAndRotation(-8.0F, 14.100000000000001F, -8.0F, 0.0F, 0.0F, 0.0F));
		lid.addOrReplaceChild("lidsideR1", CubeListBuilder.create().texOffs(118, 45).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(0.0F, -0.1F, 25.6F, 0F, 0.4363323129985824F, 0F));
		lid.addOrReplaceChild("lidsideR2", CubeListBuilder.create().texOffs(90, 27).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 26.0F), PartPose.offsetAndRotation(3.0F, -0.1F, 0.0F, 0F, -0.11519173063162574F, 0F));
		lid.addOrReplaceChild("lidsideL2", CubeListBuilder.create().texOffs(90, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 26.0F), PartPose.offsetAndRotation(12.0F, -0.09F, 0.0F, 0F, 0.11519173063162574F, 0F));
		lid.addOrReplaceChild("lidsideBottom", CubeListBuilder.create().texOffs(90, 2).addBox(0.0F, 0.0F, 0.0F, 10.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(3.0F, -0.11F, 0.0F, 0.0F, 0.0F, 0.0F));
		lid.addOrReplaceChild("lidsideL1", CubeListBuilder.create().texOffs(118, 18).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(15.1F, -0.1F, 25.2F, 0F, -0.4363323129985824F, 0F));
		lid.addOrReplaceChild("lidsideTop", CubeListBuilder.create().texOffs(90, 0).addBox(0.0F, 0.0F, 0.0F, 10.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(3.0F, -0.11F, 31.0F, 0.0F, 0.0F, 0.0F));
		partdefinition.addOrReplaceChild("sideBottom", CubeListBuilder.create().texOffs(0, 10).addBox(0.0F, 0.0F, 0.0F, 10.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(-5.0F, 15.0F, -8.0F, 0.0F, 0.0F, 0.0F));
		partdefinition.addOrReplaceChild("sideTop", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 10.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(-5.0F, 15.0F, 23.0F, 0.0F, 0.0F, 0.0F));
		partdefinition.addOrReplaceChild("sideR1", CubeListBuilder.create().texOffs(28, 45).addBox(0.0F, 0.0F, 0.0F, 1.0F, 9.0F, 7.0F), PartPose.offsetAndRotation(-7.96F, 15.0F, 17.75F, 0F, 0.4363323129985824F, 0F));
		partdefinition.addOrReplaceChild("sideL1", CubeListBuilder.create().texOffs(28, 10).addBox(0.0F, 0.0F, 0.0F, 1.0F, 9.0F, 7.0F), PartPose.offsetAndRotation(7.05F, 15.0F, 17.3F, 0F, -0.4363323129985824F, 0F));
		return LayerDefinition.create(meshdefinition, 144, 80);
	}

	public void renderAll(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		renderAll(poseStack, buffer, packedLight, packedOverlay, color, 0F);
	}

	public void renderAll(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color, float lidAngle) {
		//1.12 rendered these parts with scale = 1f (not 0.0625f); ModelPart always applies 1/16, so scale back up
		poseStack.pushPose();
		poseStack.scale(16F, 16F, 16F);
		sideR2.render(poseStack, buffer, packedLight, packedOverlay, color);
		bottom.render(poseStack, buffer, packedLight, packedOverlay, color);
		sideL2.render(poseStack, buffer, packedLight, packedOverlay, color);
		lid.setRotation(0F, 0F, lidAngle);
		lid.render(poseStack, buffer, packedLight, packedOverlay, color);
		sideBottom.render(poseStack, buffer, packedLight, packedOverlay, color);
		sideTop.render(poseStack, buffer, packedLight, packedOverlay, color);
		sideR1.render(poseStack, buffer, packedLight, packedOverlay, color);
		sideL1.render(poseStack, buffer, packedLight, packedOverlay, color);
		poseStack.popPose();
	}
}
