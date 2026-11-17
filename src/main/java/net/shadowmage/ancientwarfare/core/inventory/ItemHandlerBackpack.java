package net.shadowmage.ancientwarfare.core.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.shadowmage.ancientwarfare.core.item.ItemBackpack;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;

public class ItemHandlerBackpack implements IItemHandlerModifiable {
	private static final String BACKPACK_ITEMS_TAG = "backpackItems";
	private final ItemStackHandler backpackInventory;
	private final ItemStack backpackStack;

	public ItemHandlerBackpack(ItemStack backpackStack) {
		backpackInventory = getHandler(backpackStack);
		this.backpackStack = backpackStack;
	}

	@Override
	public int getSlots() {
		return backpackInventory.getSlots();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return backpackInventory.getStackInSlot(slot);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		ItemStack ret = stack;
		if (!(stack.getItem() instanceof ItemBackpack)) {
			ret = backpackInventory.insertItem(slot, stack, simulate);
			if (ret.getCount() < stack.getCount()) {
				saveToStack(backpackInventory);
			}
		}
		return ret;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		ItemStack ret = backpackInventory.extractItem(slot, amount, simulate);
		if (!ret.isEmpty()) {
			saveToStack(backpackInventory);
		}
		return ret;
	}

	@Override
	public int getSlotLimit(int slot) {
		return backpackInventory.getSlotLimit(slot);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return backpackInventory.isItemValid(slot, stack);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		backpackInventory.setStackInSlot(slot, stack);
		saveToStack(backpackInventory);
	}

	private ItemStackHandler getHandler(ItemStack stack) {
		if (!stack.isEmpty() && stack.getItem() instanceof ItemBackpack) {
			ItemStackHandler handler = new ItemStackHandler((ItemBackpack.getSize(stack) + 1) * 9);
			CompoundTag tag = ItemTools.getTag(stack);
			if (tag.contains(BACKPACK_ITEMS_TAG)) {
				NBTHelper.deserializeHandler(handler, tag.getCompound(BACKPACK_ITEMS_TAG));
			}
			return handler;
		}
		return new ItemStackHandler();
	}

	private void saveToStack(ItemStackHandler handler) {
		CompoundTag invTag = NBTHelper.serializeHandler(handler);
		ItemTools.setTagInfo(backpackStack, BACKPACK_ITEMS_TAG, invTag);
	}
}
