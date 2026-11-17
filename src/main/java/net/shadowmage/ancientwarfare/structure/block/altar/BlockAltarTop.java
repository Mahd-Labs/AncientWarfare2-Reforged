package net.shadowmage.ancientwarfare.structure.block.altar;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.structure.block.BlockBaseStructure;

public abstract class BlockAltarTop extends BlockBaseStructure {
	public BlockAltarTop(BlockBehaviour.Properties material, String regName) {
		super(material.noOcclusion(), regName);
	}

	@Override
	public boolean canPlaceBlockAt(Level world, BlockPos pos) {
		//1.12 canPlaceBlockOnSide: stateDown.getBlock().canPlaceTorchOnTop(stateDown, world, pos.down())
		return Block.canSupportCenter(world, pos.below(), Direction.UP);
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean movedByPiston) {
		if (!fromPos.equals(pos.below())) {
			return;
		}
		if (!canPlaceBlockAt(world, pos)) {
			dropBlockAsItem(world, pos, world.getBlockState(pos), 0);
			world.removeBlock(pos, false);
		}
	}
}
