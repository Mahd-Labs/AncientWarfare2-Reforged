package net.shadowmage.ancientwarfare.automation.tile.torque;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.shadowmage.ancientwarfare.automation.config.AWAutomationStatics;

public final class TileDistributorLight extends TileDistributor {

	public TileDistributorLight(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.TORQUE_DISTRIBUTOR_LIGHT_TILE.get(), pos, state);
	}


	@Override
	protected double getEfficiency() {
		return AWAutomationStatics.low_efficiency_factor;
	}

	@Override
	protected double getMaxTransfer() {
		return AWAutomationStatics.low_transfer_max;
	}

}
