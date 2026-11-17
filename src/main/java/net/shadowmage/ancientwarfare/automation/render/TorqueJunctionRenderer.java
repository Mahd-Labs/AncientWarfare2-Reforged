package net.shadowmage.ancientwarfare.automation.render;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.core.render.lib.CCModel;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;



public class TorqueJunctionRenderer extends TorqueTransportSidedRenderer {

	public static final TorqueJunctionRenderer INSTANCE = new TorqueJunctionRenderer();

	private TorqueJunctionRenderer() {
		super();
	}

	@Override
	protected void transformMovingParts(Collection<CCModel> transformedGroups, Direction frontFacing, float[] rotations, @Nullable IExtendedBlockState state) {
		if (state == null) {
			transformedGroups.addAll(gearHeads[Direction.NORTH.ordinal()]);
			transformedGroups.addAll(gearHeads[Direction.EAST.ordinal()]);
			transformedGroups.addAll(gearHeads[Direction.WEST.ordinal()]);
		} else {
			super.transformMovingParts(transformedGroups, frontFacing, rotations, state);
		}
	}
}
