package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileWindmillController;

public class BlockWindmillGenerator extends BlockTorqueGenerator {
	public BlockWindmillGenerator(String regName) {
		super(regName);
	}

	@Override
	public boolean invertFacing() {
		return true;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileWindmillController(pos, state);
	}
}
