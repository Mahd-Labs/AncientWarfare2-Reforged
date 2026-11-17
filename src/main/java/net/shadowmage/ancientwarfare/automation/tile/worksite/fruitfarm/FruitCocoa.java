package net.shadowmage.ancientwarfare.automation.tile.worksite.fruitfarm;

import net.shadowmage.ancientwarfare.automation.tile.worksite.WorksiteDrops;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.parsing.BlockStateMatcher;
import net.shadowmage.ancientwarfare.core.util.parsing.ItemStackMatcher;

import java.util.Optional;
import java.util.stream.Stream;

public class FruitCocoa implements IFruit {
	private BlockStateMatcher stateMatcher;
	private ItemStackMatcher stackMatcher;

	public FruitCocoa() {
		this.stateMatcher = new BlockStateMatcher(Blocks.COCOA);
		this.stackMatcher = new ItemStackMatcher.Builder(Items.COCOA_BEANS).build();
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
	public boolean canPlant(Level level, BlockPos pos, BlockState state) {
		return level.isEmptyBlock(pos) && Direction.Plane.HORIZONTAL.stream().anyMatch(h -> isJungleLog(level.getBlockState(pos.relative(h))));
	}

	private boolean isJungleLog(BlockState state) {
		return state.is(BlockTags.JUNGLE_LOGS);
	}

	@Override
	public boolean plant(Level level, BlockPos plantPos) {
		if (!level.isEmptyBlock(plantPos)) {
			return false;
		}

		Optional<Direction> facing = Direction.Plane.HORIZONTAL.stream().filter(h -> isJungleLog(level.getBlockState(plantPos.relative(h)))).findFirst();

		return facing.isPresent() && plantBean(level, plantPos, facing.get());
	}

	private boolean plantBean(Level level, BlockPos pos, Direction facing) {
		return level.setBlockAndUpdate(pos, Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, facing).setValue(CocoaBlock.AGE, 0));
	}

	@Override
	public boolean isRipe(BlockState state) {
		return state.getValue(CocoaBlock.AGE) == 2;
	}

	@Override
	public boolean pick(Level level, BlockState state, BlockPos pos, int fortune, IItemHandler inventory) {
		NonNullList<ItemStack> drops = NonNullList.create();
		WorksiteDrops.getDrops(drops, level, pos, state, fortune);

		if (!InventoryTools.canInventoryHold(inventory, drops)) {
			return false;
		}

		BlockState newState = state.setValue(CocoaBlock.AGE, 0);

		level.setBlockAndUpdate(pos, newState);
		level.levelEvent(2001, pos, Block.getId(newState));

		//remove that one cocoa bean that was just "replanted"
		InventoryTools.removeItem(drops, s -> s.getItem() == Items.COCOA_BEANS, 1);

		InventoryTools.insertOrDropItems(inventory, drops, level, pos);

		return true;
	}

	@Override
	public boolean isPlantable() {
		return true;
	}
}
