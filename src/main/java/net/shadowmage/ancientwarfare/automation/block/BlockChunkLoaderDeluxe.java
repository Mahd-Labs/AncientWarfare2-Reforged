package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.tile.TileChunkLoaderDeluxe;

public class BlockChunkLoaderDeluxe extends BlockChunkLoaderSimple {

	public BlockChunkLoaderDeluxe(String regName) {
		super(regName);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileChunkLoaderDeluxe(pos, state);
	}
}
