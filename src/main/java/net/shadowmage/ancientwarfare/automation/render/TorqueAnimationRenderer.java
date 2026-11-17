package net.shadowmage.ancientwarfare.automation.render;

import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueBase;
import net.shadowmage.ancientwarfare.core.render.property.CoreProperties;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;



public class TorqueAnimationRenderer<T extends TileTorqueBase> extends BaseAnimationRenderer<T> {

	public TorqueAnimationRenderer(AnimatedBlockRenderer bakery) {
		super(bakery);
	}

	@Override
	protected IExtendedBlockState handleState(T te, float partialTicks, IExtendedBlockState state) {
		Direction facing = te.getPrimaryFacing();
		state = state.withProperty(CoreProperties.UNLISTED_FACING, facing);
		float[] rotations = new float[6];
		for (Direction f : Direction.values()) {
			if (state.getValue(AutomationProperties.ROTATIONS[f.ordinal()]) != null || true) {
				float rotation = te.getClientOutputRotation(f, partialTicks);
				rotations[f.ordinal()] = rotation;
				state = state.withProperty(AutomationProperties.ROTATIONS[f.ordinal()], rotation);

			} else {
				state = state.withProperty(AutomationProperties.ROTATIONS[f.ordinal()], 0f);
			}
		}
		state = state.withProperty(AutomationProperties.DYNAMIC, true);
		state = updateAdditionalProperties(state, te);
		return state;
	}

	protected IExtendedBlockState updateAdditionalProperties(IExtendedBlockState state, TileTorqueBase te) {
		return state;
	}
}
