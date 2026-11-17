package net.shadowmage.ancientwarfare.structure.render.statue;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;

import java.util.Map;

public class BipedStatueModel extends StatueModelBase {
	BipedStatueModel(ModelLayerLocation layer) {super(layer);}

	@Override
	protected Map<String, ModelPart> getNameRendererMap() {
		return new ImmutableMap.Builder<String, ModelPart>()
				.put("Head", model.getChild("head"))
				.put("Headwear", model.getChild("hat"))
				.put("Body", model.getChild("body"))
				.put("Right Arm", model.getChild("right_arm"))
				.put("Left Arm", model.getChild("left_arm"))
				.put("Right Leg", model.getChild("right_leg"))
				.put("Left Leg", model.getChild("left_leg"))
				.build();
	}
}
