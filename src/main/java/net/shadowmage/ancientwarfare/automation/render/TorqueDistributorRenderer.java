package net.shadowmage.ancientwarfare.automation.render;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.core.render.lib.CCModel;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;



public class TorqueDistributorRenderer extends TorqueTransportSidedRenderer {

	public static final TorqueDistributorRenderer INSTANCE = new TorqueDistributorRenderer();

	private TorqueDistributorRenderer() {
		super();
	}

	@Override
	protected void transformMovingParts(Collection<CCModel> transformedGroups, Direction frontFacing, float[] rotations, @Nullable IExtendedBlockState state) {
		if (state == null) {
			transformedGroups.addAll(gearHeads[Direction.SOUTH.ordinal()]);
			transformedGroups.addAll(gearHeads[Direction.EAST.ordinal()]);
			transformedGroups.addAll(gearHeads[Direction.WEST.ordinal()]);
		} else {
			super.transformMovingParts(transformedGroups, frontFacing, rotations, state);
		}
	}
}
