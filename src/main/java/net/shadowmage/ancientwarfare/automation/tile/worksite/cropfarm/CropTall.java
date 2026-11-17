package net.shadowmage.ancientwarfare.automation.tile.worksite.cropfarm;

import net.minecraft.world.level.block.BonemealableBlock;
import net.shadowmage.ancientwarfare.automation.tile.worksite.WorksiteDrops;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.util.parsing.BlockStateMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class CropTall extends CropDefault {
	private int height = 1;
	private BlockStateMatcher stateMatcher;

	public CropTall(BlockStateMatcher stateMatcher, int height) {
		this.stateMatcher = stateMatcher;
		this.height = height;
	}

	@Override
	public List<BlockPos> getPositionsToHarvest(Level level, BlockPos pos, BlockState state) {
		List<BlockPos> ret = new ArrayList<>();
		applyToCrop(level, pos, (p, s) -> {
			if (s.getBlock() instanceof BonemealableBlock && !((BonemealableBlock) s.getBlock()).isValidBonemealTarget(level, p, s)) {
				ret.add(p);
			}
			return true;
		});
		return ret;
	}

	@Override
	protected boolean breakCrop(Level level, BlockPos pos, BlockState state) {
		return applyToCrop(level, pos, (p, s) -> super.breakCrop(level, p, s));
	}

	@Override
	protected void getDrops(NonNullList<ItemStack> stacks, Level level, BlockPos pos, BlockState state, int fortune) {
		applyToCrop(level, pos, (p, s) -> {
			WorksiteDrops.getDrops(stacks, level, p, s, fortune);
			return true;
		});
	}

	private boolean applyToCrop(Level level, BlockPos pos, BiFunction<BlockPos, BlockState, Boolean> applyToBlock) {
		boolean ret = true;
		for (BlockPos curPos = new BlockPos(pos.getX(), pos.getY() + (height - 1), pos.getZ()); curPos.getY() >= pos.getY(); curPos = curPos.below()) {
			BlockState curState = level.getBlockState(curPos);
			ret = ret && applyToBlock.apply(curPos, curState);
		}
		return ret;
	}

	@Override
	public boolean matches(BlockState state) {
		return stateMatcher.test(state);
	}
}
