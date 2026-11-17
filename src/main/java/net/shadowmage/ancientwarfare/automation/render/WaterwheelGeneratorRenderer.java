package net.shadowmage.ancientwarfare.automation.render;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.automation.block.BlockWaterwheelGenerator;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileWaterwheelGenerator;
import net.shadowmage.ancientwarfare.core.render.lib.CCModel;
import net.shadowmage.ancientwarfare.core.render.lib.Rotation;
import net.shadowmage.ancientwarfare.core.render.lib.Vector3;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;



public class WaterwheelGeneratorRenderer extends BaseTorqueRenderer<TileWaterwheelGenerator> {

	public static final WaterwheelGeneratorRenderer INSTANCE = new WaterwheelGeneratorRenderer();

	private final Collection<CCModel> waterwheel;
	private final Collection<CCModel> outputGear;

	private WaterwheelGeneratorRenderer() {
		super("automation/waterwheel_generator.obj");

		waterwheel = removeGroups(s -> s.startsWith("waterwheelBase.waterwheelSpindle."));
		outputGear = removeGroups(s -> s.startsWith("base.outputGear."));
	}

	@Override
	protected void transformMovingParts(Collection<CCModel> transformedGroups, Direction frontFacing, float[] rotations, @Nullable IExtendedBlockState state) {
		float wheelR = rotations[frontFacing.getOpposite().ordinal()];
		float outR = rotations[frontFacing.ordinal()];
		transformedGroups.addAll(rotateModels(outputGear, frontFacing, new Rotation(outR, 0, 0, 1).at(new Vector3(8d / 16d, 8d / 16d, 8d / 16d))));
		if (state != null && state.getValue(BlockWaterwheelGenerator.VALID_SETUP)) {
			transformedGroups.addAll(rotateModels(waterwheel, frontFacing, new Rotation(wheelR, 0, 0, 1).at(new Vector3(8d / 16d, 8d / 16d, 16d / 16d))));
		}
	}
}