package net.shadowmage.ancientwarfare.structure.render.statue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.shadowmage.ancientwarfare.structure.tile.EntityStatueInfo;

import java.util.Map;
import java.util.Set;

/*
 * Port notes: ModelBase / ModelRenderer -> the baked ModelPart tree of the vanilla layer (parts are looked up by
 * their 1.21 names instead of reflection on obfuscated fields). getModel() (only used for setLivingAnimations, which
 * did nothing for these models) is gone; render(scale) -> render(pose, buffer, light, overlay).
 */
public interface IStatueModel {
	Set<String> getModelPartNames();

	ModelPart getModelPart(String name);

	void render(PoseStack pose, VertexConsumer buffer, int packedLight, int packedOverlay);

	Map<String, EntityStatueInfo.Transform> getBaseTransforms();
}
