package net.shadowmage.ancientwarfare.structure.render.statue;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;

import java.util.Map;

/*
 * Port notes: 1.12 set the spine rotations (base transforms) and the spine / eye / tail rotation points by hand
 * because ModelGuardian only did that in setRotationAngles. The 1.21 layer definition already carries exactly those
 * values as the initial pose of each part (GuardianModel.createBodyLayer), and StatueRenderer starts every part from
 * its initial pose, so initRenderers / getBaseTransforms are not needed.
 */
public class GuardianStatueModel extends StatueModelBase {

	public GuardianStatueModel(ModelLayerLocation layer) {
		super(layer);
	}

	@Override
	protected Map<String, ModelPart> getNameRendererMap() {
		ModelPart body = getBody();
		ModelPart tail0 = body.getChild("tail0");
		ModelPart tail1 = tail0.getChild("tail1");
		ImmutableMap.Builder<String, ModelPart> builder = new ImmutableMap.Builder<String, ModelPart>()
				.put("Body", body)
				.put("Eye", body.getChild("eye"));
		for (int i = 0; i < 12; i++) {
			builder.put("Spine " + (i + 1), body.getChild("spike" + i));
		}
		return builder
				.put("Tail 1", tail0)
				.put("Tail 2", tail1)
				.put("Tail 3", tail1.getChild("tail2"))
				.build();
	}

	private ModelPart getBody() {
		return model.getChild("head");
	}

	@Override
	public void render(PoseStack pose, VertexConsumer buffer, int packedLight, int packedOverlay) {
		getBody().render(pose, buffer, packedLight, packedOverlay);
	}
}
