package net.shadowmage.ancientwarfare.structure.render.statue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.shadowmage.ancientwarfare.structure.tile.EntityStatueInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class StatueModelBase implements IStatueModel {
	protected final ModelPart model;
	private Map<String, ModelPart> nameRenderer;
	private static final ModelPart EMPTY = new ModelPart(List.of(), Map.of());

	public StatueModelBase(ModelLayerLocation layer) {
		this.model = Minecraft.getInstance().getEntityModels().bakeLayer(layer);
		initiateNameRendererMap();
	}

	private void initiateNameRendererMap() {
		nameRenderer = getNameRendererMap();
	}

	protected abstract Map<String, ModelPart> getNameRendererMap();

	@Override
	public Set<String> getModelPartNames() {
		return nameRenderer.keySet();
	}

	@Override
	public ModelPart getModelPart(String name) {
		return nameRenderer.getOrDefault(name, EMPTY);
	}

	@Override
	public void render(PoseStack pose, VertexConsumer buffer, int packedLight, int packedOverlay) {
		nameRenderer.forEach((name, renderer) -> renderer.render(pose, buffer, packedLight, packedOverlay));
	}

	@Override
	public Map<String, EntityStatueInfo.Transform> getBaseTransforms() {
		return Collections.emptyMap();
	}
}
