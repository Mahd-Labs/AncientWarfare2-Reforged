package net.shadowmage.ancientwarfare.automation.tile.torque;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.shadowmage.ancientwarfare.automation.config.AWAutomationStatics;

public final class TileConduitHeavy extends TileTorqueSidedCell {

	public TileConduitHeavy(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.TORQUE_JUNCTION_HEAVY_TILE.get(), pos, state);
	}


	@Override
	protected double getEfficiency() {
		return AWAutomationStatics.high_efficiency_factor;
	}

	@Override
	protected double getMaxTransfer() {
		return AWAutomationStatics.high_transfer_max;
	}

}
