package net.shadowmage.ancientwarfare.automation.tile.worksite.cropfarm;

import net.shadowmage.ancientwarfare.automation.tile.worksite.WorksiteDrops;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.parsing.BlockStateMatcher;
import net.shadowmage.ancientwarfare.core.util.parsing.ItemStackMatcher;

import java.util.Collections;
import java.util.List;

public class CropBreakOnly implements ICrop {

	private BlockStateMatcher stateMatcher;
	private ItemStackMatcher stackMatcher;

	CropBreakOnly() {
	}

	public CropBreakOnly(BlockStateMatcher stateMatcher, ItemStackMatcher stackMatcher) {
		this.stateMatcher = stateMatcher;
		this.stackMatcher = stackMatcher;
	}

	@Override
	public List<BlockPos> getPositionsToHarvest(Level level, BlockPos pos, BlockState state) {
		return Collections.singletonList(pos);
	}

	@Override
	public boolean canBeFertilized(BlockState state, Level level, BlockPos pos) {
		return false;
	}

	@Override
	public boolean harvest(Level level, BlockState state, BlockPos pos, int fortune, IItemHandler inventory) {
		Block block = state.getBlock();
		NonNullList<ItemStack> stacks = NonNullList.create();

		WorksiteDrops.getDrops(stacks, level, pos, state, fortune);

		if (!InventoryTools.canInventoryHold(inventory, stacks)) {
			return false;
		}

		if (!BlockTools.breakBlockNoDrops(level, pos, state)) {
			return false;
		}

		InventoryTools.insertOrDropItems(inventory, stacks, level, pos);
		return true;
	}

	@Override
	public boolean matches(BlockState state) {
		return stateMatcher.test(state);
	}

	@Override
	public boolean matches(ItemStack stack) {
		return stackMatcher.test(stack);
	}

	@Override
	public boolean isPlantable(ItemStack stack) {
		return false;
	}
}
