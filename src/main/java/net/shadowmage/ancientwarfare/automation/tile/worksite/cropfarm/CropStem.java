package net.shadowmage.ancientwarfare.automation.tile.worksite.cropfarm;

import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class CropStem implements ICrop {
	@Override
	public List<BlockPos> getPositionsToHarvest(Level level, BlockPos pos, BlockState state) {
		List<BlockPos> ret = new ArrayList<>();

		addPositionIfGourd(ret, level, pos.north());
		addPositionIfGourd(ret, level, pos.west());
		addPositionIfGourd(ret, level, pos.south());
		addPositionIfGourd(ret, level, pos.east());
		return ret;
	}

	@Override
	public boolean canBeFertilized(BlockState state, Level level, BlockPos pos) {
		return ((StemBlock) state.getBlock()).isValidBonemealTarget(level, pos, state);
	}

	private void addPositionIfGourd(List<BlockPos> list, Level level, BlockPos pos) {
		if ((level.getBlockState(pos).is(Blocks.PUMPKIN) || level.getBlockState(pos).is(Blocks.MELON) || level.getBlockState(pos).is(Blocks.CARVED_PUMPKIN) || level.getBlockState(pos).is(Blocks.JACK_O_LANTERN))) {
			list.add(pos);
		}
	}

	@Override
	public boolean harvest(Level level, BlockState state, BlockPos pos, int fortune, IItemHandler inventory) {
		return false;
	}

	@Override
	public boolean matches(BlockState state) {
		return state.getBlock() instanceof StemBlock;
	}
}
