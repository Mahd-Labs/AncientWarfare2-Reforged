package net.shadowmage.ancientwarfare.automation.tile.worksite.cropfarm;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.parsing.BlockStateMatcher;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CropKeepBottom extends CropDefault {
	private static final int MAX_HEIGHT = 10;
	private BlockStateMatcher stateMatcher;

	public CropKeepBottom(BlockStateMatcher stateMatcher) {
		this.stateMatcher = stateMatcher;
	}

	@Override
	public boolean harvest(Level level, BlockState state, BlockPos pos, int fortune, IItemHandler inventory) {
		NonNullList<ItemStack> stacks = NonNullList.create();

		//figuring out positions here instead of in getPositionsToHarvest because if growing quickly we could be harvesting block that's not the top one.
		// so instead just pass in the bottom one as the one to harvest and then figure out all of the blocks to harvest at once here
		Map<BlockPos, BlockState> harvestPositions = getHarvestPositions(level, pos);

		getDrops(harvestPositions, stacks, level, fortune);

		if (!InventoryTools.canInventoryHold(inventory, stacks)) {
			return false;
		}

		if (!breakCrop(harvestPositions, level)) {
			return false;
		}

		InventoryTools.insertOrDropItems(inventory, stacks, level, pos);
		return true;
	}

	private void getDrops(Map<BlockPos, BlockState> harvestPositions, NonNullList<ItemStack> stacks, Level level, int fortune) {
		for (Map.Entry<BlockPos, BlockState> entry : harvestPositions.entrySet()) {
			getDrops(stacks, level, entry.getKey(), entry.getValue(), fortune);
		}
	}

	private boolean breakCrop(Map<BlockPos, BlockState> harvestPositions, Level level) {
		for (Map.Entry<BlockPos, BlockState> entry : harvestPositions.entrySet()) {
			if (!breakCrop(level, entry.getKey(), entry.getValue())) {
				return false;
			}
		}
		return true;
	}

	private Map<BlockPos, BlockState> getHarvestPositions(Level level, BlockPos pos) {
		Map<BlockPos, BlockState> ret = new TreeMap<>(Collections.reverseOrder());
		for (BlockPos currentPos = pos.above(); currentPos.getY() < pos.getY() + MAX_HEIGHT; currentPos = currentPos.above()) {
			BlockState state = level.getBlockState(currentPos);
			if (!stateMatcher.test(state)) {
				break;
			}
			ret.put(currentPos, state);
		}
		return ret;
	}

	@Override
	public List<BlockPos> getPositionsToHarvest(Level level, BlockPos pos, BlockState state) {
		if (stateMatcher.test(level.getBlockState(pos.above()))) {
			return Collections.singletonList(pos);
		}
		return Collections.emptyList();
	}

	@Override
	public boolean canBeFertilized(BlockState state, Level level, BlockPos pos) {
		return false;
	}

	@Override
	public boolean matches(BlockState state) {
		return stateMatcher.test(state);
	}
}
