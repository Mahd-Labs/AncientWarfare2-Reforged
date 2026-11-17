package net.shadowmage.ancientwarfare.automation.block;

import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.RotationType;

public abstract class BlockTorqueGenerator extends BlockTorqueBase {

	protected BlockTorqueGenerator(String regName) {
		super(rock(), regName);
	}

	@Override
	public RotationType getRotationType() {
		return RotationType.FOUR_WAY;
	}

	@Override
	public boolean invertFacing() {
		return false;
	}

}
