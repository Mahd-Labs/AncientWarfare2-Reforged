package net.shadowmage.ancientwarfare.automation.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.block.BlockWarehouseStorage;
import net.shadowmage.ancientwarfare.core.block.BlockBase;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;

import javax.annotation.Nullable;
import java.util.Map;

/*
 * Port note: flattened, one item per size (warehouse_storage_small / _medium / _large) placing the block with that
 * size state. Translation key keeps the 1.12 meta suffix.
 */
public class ItemBlockWarehouseStorage extends ItemBlockBase {
	private final BlockWarehouseStorage.Size size;

	public ItemBlockWarehouseStorage(Block block, BlockWarehouseStorage.Size size) {
		super(block);
		this.size = size;
	}

	public BlockWarehouseStorage.Size getSize() {
		return size;
	}

	@Nullable
	@Override
	protected BlockState getPlacementState(BlockPlaceContext context) {
		BlockState state = super.getPlacementState(context);
		return state == null ? null : state.setValue(BlockWarehouseStorage.SIZE, size);
	}

	@Override
	public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
		if (size == BlockWarehouseStorage.Size.SMALL) {
			super.registerBlocks(blockToItemMap, item);
		}
	}

	@Override
	public String getDescriptionId() {
		return "tile." + ((BlockBase) getBlock()).getRegName() + "." + size.getMeta() + ".name";
	}

	@Override
	public String getDescriptionId(ItemStack par1ItemStack) {
		return getDescriptionId();
	}
}
