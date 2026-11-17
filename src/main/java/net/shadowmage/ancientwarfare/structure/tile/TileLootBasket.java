package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.shadowmage.ancientwarfare.core.tile.IBlockBreakHandler;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;

/*
 * Port notes: still a chest block entity like 1.12, but its block is not a ChestBlock: the vanilla open / close
 * handling (lid animation, chest sound, both need ChestBlock properties) is switched off.
 */
public class TileLootBasket extends TileAdvancedLootChest implements IBlockBreakHandler {
	public TileLootBasket(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.LOOT_BASKET_TILE.get(), pos, state);
	}

	protected TileLootBasket(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void startOpen(Player player) {
	}

	@Override
	public void stopOpen(Player player) {
	}

	@Override
	public void onBlockBroken(BlockState state) {
		InventoryTools.dropItemsInWorld(level, new InvWrapper(this), worldPosition);
	}
}
