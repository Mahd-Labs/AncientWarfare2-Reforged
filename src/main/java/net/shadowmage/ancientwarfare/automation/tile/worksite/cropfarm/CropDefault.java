package net.shadowmage.ancientwarfare.automation.tile.worksite.cropfarm;

import net.shadowmage.ancientwarfare.automation.tile.worksite.TileWorksiteFarm;
import net.shadowmage.ancientwarfare.automation.tile.worksite.WorksiteDrops;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import java.util.Collections;
import java.util.List;

public class CropDefault implements ICrop {
	@Override
	public List<BlockPos> getPositionsToHarvest(Level level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof BonemealableBlock && !((BonemealableBlock) state.getBlock()).isValidBonemealTarget(level, pos, state)) {
			return Collections.singletonList(pos);
		}
		return Collections.emptyList();
	}

	@Override
	public boolean canBeFertilized(BlockState state, Level level, BlockPos pos) {
		if (!(state.getBlock() instanceof BonemealableBlock)) {
			return false;
		}

		BonemealableBlock growable = (BonemealableBlock) state.getBlock();

		return growable.isValidBonemealTarget(level, pos, state) && growable.isBonemealSuccess(level, level.random, pos, state);
	}

	@Override
	public boolean harvest(Level level, BlockState state, BlockPos pos, int fortune, IItemHandler inventory) {
		NonNullList<ItemStack> stacks = NonNullList.create();

		getDrops(stacks, level, pos, state, fortune);

		if (!InventoryTools.canInventoryHold(inventory, stacks)) {
			return false;
		}

		if (!breakCrop(level, pos, state)) {
			return false;
		}

		ItemStack plantable = InventoryTools.removeItem(stacks, TileWorksiteFarm::isPlantableItem, 1);

		if (!plantable.isEmpty()) {
			BlockTools.placeItemBlock(plantable, level, pos, Direction.UP);
		}

		InventoryTools.insertOrDropItems(inventory, stacks, level, pos);
		return true;
	}

	protected boolean breakCrop(Level level, BlockPos pos, BlockState state) {
		return BlockTools.breakBlockNoDrops(level, pos, state);
	}

	protected void getDrops(NonNullList<ItemStack> stacks, Level level, BlockPos pos, BlockState state, int fortune) {
		WorksiteDrops.getDrops(stacks, level, pos, state, fortune);
	}

	@Override
	public boolean matches(BlockState state) {
		return true;
	}

	@Override
	public boolean matches(ItemStack stack) {
		return true;
	}

	@Override
	public boolean isPlantable(ItemStack stack) {
		return isPlantableItemBlock(stack); //1.12: item instanceof IPlantable (seeds) or item block of an IPlantable block; seeds are block items now
	}

	private boolean isPlantableItemBlock(ItemStack stack) {
		return TileWorksiteFarm.isPlantableItem(stack);
	}
}
