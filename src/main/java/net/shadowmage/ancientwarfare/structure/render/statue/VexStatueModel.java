package net.shadowmage.ancientwarfare.structure.render.statue;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;

import java.util.Map;

/*
 * Port notes: the 1.12 vex was a biped model with two wings. The 1.21 vex model has no legs and no head wear and its
 * arms and wings hang off the body, so those two part names are gone and the whole tree is drawn from its root.
 */
public class VexStatueModel extends StatueModelBase {
	public VexStatueModel() {
		super(ModelLayers.VEX);
	}

	@Override
	protected Map<String, ModelPart> getNameRendererMap() {
		ModelPart root = model.getChild("root");
		ModelPart body = root.getChild("body");
		return new ImmutableMap.Builder<String, ModelPart>()
				.put("Head", root.getChild("head"))
				.put("Body", body)
				.put("Right Arm", body.getChild("right_arm"))
				.put("Left Arm", body.getChild("left_arm"))
				.put("Left Wing", body.getChild("left_wing"))
				.put("Right Wing", body.getChild("right_wing"))
				.build();
	}

	@Override
	public void render(PoseStack pose, VertexConsumer buffer, int packedLight, int packedOverlay) {
		model.render(pose, buffer, packedLight, packedOverlay);
	}
}
