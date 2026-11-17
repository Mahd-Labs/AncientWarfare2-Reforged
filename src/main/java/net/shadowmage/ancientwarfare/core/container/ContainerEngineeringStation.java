package net.shadowmage.ancientwarfare.core.container;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper;
import net.shadowmage.ancientwarfare.core.crafting.AWCraftingManager;
import net.shadowmage.ancientwarfare.core.crafting.ICraftingRecipe;
import net.shadowmage.ancientwarfare.core.tile.TileEngineeringStation;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import java.util.List;

import static net.minecraft.world.InteractionResult.PASS;
import static net.minecraft.world.InteractionResult.SUCCESS;

/*
 * Port notes: slotClick -> clicked, setAll -> initializeContents, putStackInSlot -> setItem,
 * mergeItemStack -> moveItemStackTo.
 */
public class ContainerEngineeringStation extends ContainerTileBase<TileEngineeringStation> implements ICraftingContainer {
	private static final int BOOK_SLOT = 1;
	private static final int CRAFTING_SLOT = 0;
	public ContainerCraftingRecipeMemory containerCrafting;
	private int currentCraftTotalSize = 0;

	public ContainerEngineeringStation(Player player, int x, int y, int z) {
		super(player, x, y, z);

		containerCrafting = new ContainerCraftingRecipeMemory(tileEntity.craftingRecipeMemory, player) {
			@Override
			protected OnTakeResult handleOnTake(Player player, ItemStack stack) {
				ICraftingRecipe recipe = tileEntity.craftingRecipeMemory.getRecipe();
				NonNullList<ItemStack> reusableStacks = AWCraftingManager.getReusableStacks(recipe, containerCrafting.getCraftMatrix());
				NonNullList<ItemStack> resources = InventoryTools.removeItems(AWCraftingManager.getRecipeInventoryMatch(recipe, getCraftingStacks(),
						new CombinedInvWrapper(tileEntity.extraSlots, new ItemStackHandler(reusableStacks))), reusableStacks);
				if (!resources.isEmpty()) {
					InventoryTools.removeItems(tileEntity.extraSlots, resources);
					CommonHooks.setCraftingPlayer(player);
					NonNullList<ItemStack> remainingItems = InventoryTools.removeItems(
							tileEntity.craftingRecipeMemory.getRemainingItems(AWCraftingManager.fillCraftingMatrixFromInventory(resources)),
							reusableStacks);
					CommonHooks.setCraftingPlayer(null);
					InventoryTools.insertOrDropItems(tileEntity.extraSlots, remainingItems, tileEntity.getLevel(), tileEntity.getBlockPos());
					return new OnTakeResult(SUCCESS, stack);
				}
				return new OnTakeResult(PASS, stack);
			}

			@Override
			protected boolean canTakeStackFromOutput(Player player) {
				return true;
			}
		};

		for (Slot slot : containerCrafting.getSlots()) {
			addSlotToContainer(slot);
		}

		Slot slot;
		for (int y1 = 0; y1 < 2; y1++) {
			int y2 = y1 * 18 + 8 + 3 * 18 + 4;
			for (int x1 = 0; x1 < 9; x1++) {
				int x2 = x1 * 18 + 8;
				int slotNum = y1 * 9 + x1;
				slot = new SlotItemHandler(tileEntity.extraSlots, slotNum, x2, y2);
				addSlotToContainer(slot);
			}
		}

		int y1 = 8 + 3 * 18 + 8 + 2 * 18 + 4;
		y1 = this.addPlayerSlots(y1);
	}

	@Override
	public void handlePacketData(CompoundTag tag) {
		if (tag.contains("recipe")) {
			containerCrafting.handleRecipeUpdate(tag);
		}
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		super.clicked(slotId, dragType, clickTypeIn, player);
		currentCraftTotalSize = 0;
	}

	@Override
	public void initializeContents(int stateId, List<ItemStack> p_190896_1_, ItemStack carried) {
		containerCrafting.setOpening(true);
		super.initializeContents(stateId, p_190896_1_, carried);
		containerCrafting.setOpening(false);
	}

	@Override
	public void setItem(int slotID, int stateId, ItemStack stack) {
		containerCrafting.setOpening(true);
		super.setItem(slotID, stateId, stack);
		containerCrafting.setOpening(false);
	}

	@Override
	public ItemStack transferStackInSlot(Player par1EntityPlayer, int slotClickedIndex) {
		if (slotClickedIndex == CRAFTING_SLOT && !updateAndCheckCraftStackOrLessInTotal()) {
			return ItemStack.EMPTY;
		}

		ItemStack slotStackCopy = ItemStack.EMPTY;
		Slot theSlot = this.getSlot(slotClickedIndex);
		if (theSlot != null && theSlot.hasItem()) {
			ItemStack slotStack = theSlot.getItem();
			slotStackCopy = slotStack.copy();

			int craftSlotStart = 2;
			int storageSlotsStart = craftSlotStart + tileEntity.craftingRecipeMemory.craftMatrix.getContainerSize();
			int playerSlotStart = storageSlotsStart + tileEntity.extraSlots.getSlots();
			int playerSlotEnd = playerSlotStart + playerSlots;
			if (slotClickedIndex < craftSlotStart)//book or result slot
			{
				if (!this.moveItemStackTo(slotStack, playerSlotStart, playerSlotEnd, false))//merge into player inventory
				{ return ItemStack.EMPTY; }
			} else {
				if (slotClickedIndex < storageSlotsStart) {//craft matrix
					if (!this.moveItemStackTo(slotStack, storageSlotsStart, playerSlotStart, false))//merge into storage
						return ItemStack.EMPTY;
				} else if (slotClickedIndex < playerSlotStart) {//storage slots
					if (!this.moveItemStackTo(slotStack, playerSlotStart, playerSlotEnd, false))//merge into player inventory
						return ItemStack.EMPTY;
				} else if (slotClickedIndex < playerSlotEnd && !moveItemStackTo(slotStack, BOOK_SLOT, BOOK_SLOT + 1, false)
						&& !this.moveItemStackTo(slotStack, storageSlotsStart, playerSlotStart, false)) {
					return ItemStack.EMPTY;
				}
			}
			if (slotStack.getCount() == 0) {
				theSlot.set(ItemStack.EMPTY);
			} else {
				theSlot.setChanged();
			}
			if (slotStack.getCount() == slotStackCopy.getCount()) {
				return ItemStack.EMPTY;
			}
			theSlot.onTake(par1EntityPlayer, slotStack);
		}
		return slotStackCopy;
	}

	private boolean updateAndCheckCraftStackOrLessInTotal() {
		ItemStack craftedStack = getSlot(CRAFTING_SLOT).getItem();
		currentCraftTotalSize += craftedStack.getCount();
		return currentCraftTotalSize <= craftedStack.getMaxStackSize();
	}

	@Override
	public ContainerCraftingRecipeMemory getCraftingMemoryContainer() {
		return containerCrafting;
	}

	@Override
	public IItemHandlerModifiable[] getInventories() {
		return new IItemHandlerModifiable[] {tileEntity.extraSlots, new PlayerInvWrapper(player.getInventory())};
	}

	@Override
	public boolean pushCraftingMatrixToInventories() {
		IItemHandler craftMatrixWrapper = new InvWrapper(tileEntity.craftingRecipeMemory.craftMatrix);
		NonNullList<ItemStack> craftingItems = InventoryTools.getItems(craftMatrixWrapper);
		IItemHandler inventories = new CombinedInvWrapper(tileEntity.extraSlots, new PlayerInvWrapper(player.getInventory()));
		if (InventoryTools.insertItems(inventories, craftingItems, true).isEmpty()) {
			InventoryTools.insertItems(inventories, craftingItems, false);
			InventoryTools.removeItems(craftMatrixWrapper, craftingItems);
			return true;
		}
		return false;
	}
}
