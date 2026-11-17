package net.shadowmage.ancientwarfare.automation.tile.warehouse2;

import net.minecraft.core.BlockPos;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.interfaces.IInteractableTile;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.tile.CraftingRecipeMemory;
import net.shadowmage.ancientwarfare.core.tile.IBlockBreakHandler;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import javax.annotation.Nullable;

public class TileWarehouseCraftingStation extends TileUpdatable implements IInteractableTile, IBlockBreakHandler {

	public TileWarehouseCraftingStation(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.WAREHOUSE_CRAFTING_TILE.get(), pos, state);
	}


	public CraftingRecipeMemory craftingRecipeMemory = new CraftingRecipeMemory(this);

	@Nullable
	public final TileWarehouse getWarehouse() {
		if (level == null || worldPosition.getY() <= level.getMinBuildHeight() + 1)//could not possibly be a warehouse below... (port: 1.12 "y <= 1", the world bottom is not 0 any more)
		{
			return null;
		}
		return WorldTools.getTile(level, worldPosition.below(), TileWarehouse.class).orElse(null);
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		craftingRecipeMemory.writeToNBT(tag);
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		craftingRecipeMemory.readFromNBT(tag);
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		craftingRecipeMemory.readFromNBT(tag);
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		craftingRecipeMemory.writeToNBT(tag);
	}

	@Override
	public boolean onBlockClicked(Player player, InteractionHand hand) {
		if (!player.level().isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WAREHOUSE_CRAFTING, worldPosition);
		}
		return true;
	}

	@Override
	public void onBlockBroken(BlockState state) {
		craftingRecipeMemory.dropInventory();
	}
}
