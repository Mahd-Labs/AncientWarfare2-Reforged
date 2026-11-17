package net.shadowmage.ancientwarfare.automation.render;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueShaft;
import net.shadowmage.ancientwarfare.core.render.lib.CCModel;
import net.shadowmage.ancientwarfare.core.render.lib.Rotation;
import net.shadowmage.ancientwarfare.core.render.lib.Transformation;
import net.shadowmage.ancientwarfare.core.render.lib.Translation;
import net.shadowmage.ancientwarfare.core.render.lib.Vector3;
import net.shadowmage.ancientwarfare.core.render.property.CoreProperties;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;



public class TorqueShaftRenderer extends TorqueTieredRenderer<TileTorqueShaft> {

	public static final TorqueShaftRenderer INSTANCE = new TorqueShaftRenderer();

	private Collection<CCModel> inputHead;
	private Collection<CCModel> outputHead;
	private Collection<CCModel> shaft;
	private Collection<CCModel> gearbox;

	private TorqueShaftRenderer() {
		super("automation/torque_shaft.obj");
		inputHead = removeGroups(s -> s.startsWith("southShaft."));
		outputHead = removeGroups(s -> s.startsWith("northShaft."));
		shaft = removeGroups(s -> s.startsWith("shaft."));
		gearbox = removeGroups(s -> s.startsWith("gearBox."));
	}

	@Override
	protected Transformation getBaseTransformation() {
		return new Translation(0d, 0.5d, 0d);
	}

	@Override
	protected Collection<CCModel> applyModelTransforms(Collection<CCModel> modelGroups, Direction face, IExtendedBlockState state) {
		modelGroups = super.applyModelTransforms(modelGroups, face, state);
		if (!state.getValue(AutomationProperties.DYNAMIC) && !Boolean.TRUE.equals(state.getValue(AutomationProperties.HAS_PREVIOUS))) {
			modelGroups.addAll(rotateFacing(gearbox, state.getValue(CoreProperties.UNLISTED_FACING)));
		}

		return modelGroups;
	}

	@Override
	protected void transformMovingParts(Collection<CCModel> transformedGroups, Direction frontFacing, float[] rotations, @Nullable IExtendedBlockState state) {
		float rotation = rotations[frontFacing.ordinal()];

		transformedGroups.addAll(rotateModels(shaft, frontFacing, new Rotation(rotation, 0, 0, 1).at(new Vector3(8d / 16d, 8d / 16d, 8d / 16d))));
		boolean hasPrevious = state != null && state.getValue(AutomationProperties.HAS_PREVIOUS);
		boolean useInput = state != null && state.getValue(AutomationProperties.USE_INPUT);
		boolean hasNext = state != null && state.getValue(AutomationProperties.HAS_NEXT);
		if (!hasNext) {
			transformedGroups.addAll(rotateModels(outputHead, frontFacing, new Rotation(rotation, 0, 0, 1).at(new Vector3(8d / 16d, 8d / 16d, 8d / 16d))));
		}

		if (!hasPrevious) {
			if (useInput) {
				rotation = state.getValue(AutomationProperties.INPUT_ROTATION);
			}
			transformedGroups.addAll(rotateModels(inputHead, frontFacing, new Rotation(rotation, 0, 0, 1).at(new Vector3(8d / 16d, 8d / 16d, 8d / 16d))));
		}
	}

	@Override
	protected IExtendedBlockState handleAdditionalProperties(IExtendedBlockState state, TileTorqueShaft tileEntity) {
		state = super.handleAdditionalProperties(state, tileEntity);
		TileTorqueShaft prev = tileEntity.prev();
		state = state.withProperty(AutomationProperties.HAS_NEXT, false);
		state = state.withProperty(AutomationProperties.HAS_PREVIOUS, prev != null);
		state = state.withProperty(AutomationProperties.USE_INPUT, false);
		state = state.withProperty(AutomationProperties.INPUT_ROTATION, 0f);
		return state;
	}
}
