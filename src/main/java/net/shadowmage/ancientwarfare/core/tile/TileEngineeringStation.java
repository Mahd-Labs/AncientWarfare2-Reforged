package net.shadowmage.ancientwarfare.core.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableTile;
import net.shadowmage.ancientwarfare.core.init.AWCoreBlocks;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;

public class TileEngineeringStation extends TileUpdatable implements IRotatableTile {

	public CraftingRecipeMemory craftingRecipeMemory = new CraftingRecipeMemory(this);
	Direction facing = Direction.NORTH;

	public final ItemStackHandler extraSlots;

	public TileEngineeringStation(BlockPos pos, BlockState state) {
		super(AWCoreBlocks.ENGINEERING_STATION_TILE.get(), pos, state);
		extraSlots = new ItemStackHandler(18) {
			@Override
			protected void onContentsChanged(int slot) {
				markDirty();
			}
		};
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		tag.putInt("facing", facing.ordinal());
		craftingRecipeMemory.writeToNBT(tag);
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		facing = Direction.values()[tag.getInt("facing")];
		BlockTools.notifyBlockUpdate(this);
		craftingRecipeMemory.readFromNBT(tag);
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		craftingRecipeMemory.readFromNBT(tag);
		NBTHelper.deserializeHandler(extraSlots, tag.getCompound("extraInventory"));
		facing = Direction.values()[tag.getInt("facing")];
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		craftingRecipeMemory.writeToNBT(tag);
		tag.put("extraInventory", NBTHelper.serializeHandler(extraSlots));
		tag.putInt("facing", facing.ordinal());
	}

	@Override
	public Direction getPrimaryFacing() {
		return facing;
	}

	@Override
	public void setPrimaryFacing(Direction face) {
		this.facing = face;
		BlockTools.notifyBlockUpdate(this);
	}

	public void onBlockBreak() {
		craftingRecipeMemory.dropInventory();
		InventoryTools.dropItemsInWorld(level, extraSlots, worldPosition);
	}
}
