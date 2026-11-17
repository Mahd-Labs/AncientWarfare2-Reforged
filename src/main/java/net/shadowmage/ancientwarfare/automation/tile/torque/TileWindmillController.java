package net.shadowmage.ancientwarfare.automation.tile.torque;

import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.shadowmage.ancientwarfare.automation.config.AWAutomationStatics;
import net.shadowmage.ancientwarfare.automation.tile.torque.multiblock.TileWindmillBlade;
import net.shadowmage.ancientwarfare.core.interfaces.ITorque.TorqueCell;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import javax.annotation.Nullable;

public class TileWindmillController extends TileTorqueSingleCell {

	public TileWindmillController(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.WINDMILL_GENERATOR_TILE.get(), pos, state);
		double max = AWAutomationStatics.low_transfer_max;
		torqueCell = new TorqueCell(max, max, max, AWAutomationStatics.low_efficiency_factor);
	}

	@Override
	public void update() {
		super.update();
		if (!level.isClientSide) {
			TileWindmillBlade blade = getControlledBlade();
			if (blade != null && blade.getEnergy() > 0) {
				double d = blade.getEnergy();
				blade.setEnergy(0);
				torqueCell.setEnergy(torqueCell.getEnergy() + d);
			}
		}
	}

	private TileWindmillBlade getControlledBlade() {
		BlockPos behind = worldPosition.relative(getPrimaryFacing().getOpposite());
		if (level.isLoaded(behind)) {
			return WorldTools.getTile(level, behind, TileWindmillBlade.class).orElse(null);
		}
		return null;
	}

	@Override
	public boolean canInputTorque(@Nullable Direction from) {
		return false;
	}

}
