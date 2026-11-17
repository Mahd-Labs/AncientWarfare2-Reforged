package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileStirlingGenerator;

public class BlockStirlingGenerator extends BlockTorqueGenerator {
	public BlockStirlingGenerator(String regName) {
		super(regName);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileStirlingGenerator(pos, state);
	}
}
