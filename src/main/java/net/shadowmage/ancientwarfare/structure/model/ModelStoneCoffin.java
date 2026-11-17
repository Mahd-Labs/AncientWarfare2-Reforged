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
 * ModelAW2CoffinStone - Silentine
 * Created using Tabula 7.1.0
 */
public class ModelStoneCoffin {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "stone_coffin"), "main");

	public final ModelPart lid;
	public final ModelPart baseface;

	public ModelStoneCoffin(ModelPart root) {
		lid = root.getChild("lid");
		baseface = root.getChild("baseface");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition lid = partdefinition.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(48, 0).addBox(-10.0F, -37.0F, -4.0F, 20.0F, 44.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 12.0F, -15.0F, 0.0F, 0.0F, 0.0F));
		lid.addOrReplaceChild("lidInside", CubeListBuilder.create().mirror().texOffs(2, 1).addBox(-9.5F, -36.5F, -3.5F, 19.0F, 43.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
		PartDefinition baseface = partdefinition.addOrReplaceChild("baseface", CubeListBuilder.create().texOffs(-48, 0).addBox(-12.0F, -4.0F, -24.0F, 24.0F, 4.0F, 48.0F), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));
		baseface.addOrReplaceChild("southface", CubeListBuilder.create().texOffs(0, 52).addBox(-12.0F, -12.0F, -24.0F, 24.0F, 12.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
		baseface.addOrReplaceChild("northface", CubeListBuilder.create().texOffs(0, 52).addBox(-12.0F, -12.0F, -24.0F, 24.0F, 12.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.141592653589793F, 0.0F));
		baseface.addOrReplaceChild("leftface", CubeListBuilder.create().texOffs(0, 68).addBox(-24.0F, -12.0F, -12.0F, 48.0F, 12.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.5707963267948966F, 0.0F));
		baseface.addOrReplaceChild("rightface", CubeListBuilder.create().texOffs(0, 68).addBox(-24.0F, -12.0F, -12.0F, 48.0F, 12.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5707963267948966F, 0.0F));
		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	public void renderAll(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		renderAll(poseStack, buffer, packedLight, packedOverlay, color, 0F);
	}

	public void renderAll(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color, float lidAngle) {
		//1.12 rendered these parts with scale = 1f (not 0.0625f); ModelPart always applies 1/16, so scale back up
		poseStack.pushPose();
		poseStack.scale(16F, 16F, 16F);
		baseface.render(poseStack, buffer, packedLight, packedOverlay, color);
		lid.setRotation(-1.5707963267948966F, lidAngle, 0F);
		lid.render(poseStack, buffer, packedLight, packedOverlay, color);
		poseStack.popPose();
	}
}
