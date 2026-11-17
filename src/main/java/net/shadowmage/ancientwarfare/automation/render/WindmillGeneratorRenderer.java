package net.shadowmage.ancientwarfare.automation.render;

import java.util.Collection;
import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileWindmillController;
import net.shadowmage.ancientwarfare.core.render.lib.CCModel;
import net.shadowmage.ancientwarfare.core.render.lib.Rotation;
import net.shadowmage.ancientwarfare.core.render.lib.Vector3;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;



public class WindmillGeneratorRenderer extends BaseTorqueRenderer<TileWindmillController> {

	public static final WindmillGeneratorRenderer INSTANCE = new WindmillGeneratorRenderer();

	private final Collection<CCModel> outputGear;

	private WindmillGeneratorRenderer() {
		super("automation/windmill_generator.obj");
		outputGear = removeGroups(s -> s.startsWith("base.outputGear."));
	}

	@Override
	protected void transformMovingParts(Collection<CCModel> transformedGroups, Direction frontFacing, float[] rotations, IExtendedBlockState state) {
		float outR = rotations[frontFacing.ordinal()];
		transformedGroups.addAll(rotateModels(outputGear, frontFacing, new Rotation(outR, 0, 0, 1).at(new Vector3(8d / 16d, 8d / 16d, 8d / 16d))));
	}
}