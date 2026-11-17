package net.shadowmage.ancientwarfare.automation.tile.warehouse2;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

public final class TileWarehouseStorageMedium extends TileWarehouseStorage {

	public TileWarehouseStorageMedium(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.WAREHOUSE_STORAGE_MEDIUM_TILE.get(), pos, state);

	}

	@Override
	public int getStorageAdditionSize() {
		return 2 * super.getStorageAdditionSize();
	}
}
