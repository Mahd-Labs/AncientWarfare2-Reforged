package net.shadowmage.ancientwarfare.automation.container;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ClickType;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseBase;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseCraftingStation;
import net.shadowmage.ancientwarfare.core.container.ContainerCraftingRecipeMemory;
import net.shadowmage.ancientwarfare.core.container.ContainerTileBase;
import net.shadowmage.ancientwarfare.core.container.ICraftingContainer;
import net.shadowmage.ancientwarfare.core.crafting.AWCraftingManager;
import net.shadowmage.ancientwarfare.core.crafting.ICraftingRecipe;
import net.shadowmage.ancientwarfare.core.inventory.ItemHashEntry;
import net.shadowmage.ancientwarfare.core.inventory.ItemQuantityMap;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import javax.annotation.Nonnull;
import java.util.List;


public class ContainerWarehouseCraftingStation extends ContainerTileBase<TileWarehouseCraftingStation> implements ICraftingContainer {
	private static final int BOOK_SLOT = 1;
	private static final String CHANGE_LIST_TAG = "changeList";
	private static final int CRAFTING_SLOT = 0;
	public ContainerCraftingRecipeMemory containerCrafting;

	private ItemQuantityMap itemMap = new ItemQuantityMap();
	private final ItemQuantityMap cache = new ItemQuantityMap();
	private boolean shouldUpdate = true;
	private int currentCraftTotalSize = 0;
	private IItemHandlerModifiable warehouseItemHandler = null;

	public ContainerWarehouseCraftingStation(final Player player, int x, int y, int z) {
		super(player, x, y, z);

		containerCrafting = new ContainerCraftingRecipeMemory(tileEntity.craftingRecipeMemory, player) {
			@Override
			protected OnTakeResult handleOnTake(Player player, ItemStack stack) {
				if (warehouseItemHandler == null) {
					return new OnTakeResult(InteractionResult.PASS, stack);
				}
				ICraftingRecipe recipe = tileEntity.craftingRecipeMemory.getRecipe();
				NonNullList<ItemStack> reusableStacks = AWCraftingManager.getReusableStacks(recipe, tileEntity.craftingRecipeMemory.craftMatrix);
				CombinedInvWrapper combinedHandler = new CombinedInvWrapper(new ItemStackHandler(reusableStacks), warehouseItemHandler);
				NonNullList<ItemStack> resources = AWCraftingManager.getRecipeInventoryMatch(recipe,
						containerCrafting.getCraftingStacks(), s -> tileEntity.getWarehouse().getCountOf(s) >= s.getCount(), combinedHandler);
				if (!resources.isEmpty()) {
					resources = InventoryTools.removeItems(resources, reusableStacks);
					InventoryTools.removeItems(warehouseItemHandler, resources);

					NonNullList<ItemStack> remainingItems = InventoryTools.removeItems(tileEntity.craftingRecipeMemory.getRemainingItems(
							AWCraftingManager.fillCraftingMatrixFromInventory(resources)), reusableStacks);
					InventoryTools.insertOrDropItems(warehouseItemHandler, remainingItems, tileEntity.getLevel(), tileEntity.getBlockPos());

					return new OnTakeResult(InteractionResult.SUCCESS, stack);
				}
				return new OnTakeResult(InteractionResult.PASS, stack);
			}

			@Override
			protected boolean canTakeStackFromOutput(Player player) {
				return true;
			}
		};
		for (Slot slot : containerCrafting.getSlots()) {
			addSlotToContainer(slot);
		}

		int y1 = 8 + 3 * 18 + 8;
		addPlayerSlots(y1);
		TileWarehouseBase warehouse = tileEntity.getWarehouse();
		if (warehouse != null) {
			warehouse.addCraftingViewer(this);
		}
	}

	@Override
	public void initializeContents(int stateId, List<ItemStack> stacks, ItemStack carried) {
		containerCrafting.setOpening(true);
		super.initializeContents(stateId, stacks, carried);
		containerCrafting.setOpening(false);
	}

	@Override
	public void setItem(int slotID, int stateId, ItemStack stack) {
		containerCrafting.setOpening(true);
		super.setItem(slotID, stateId, stack);
		containerCrafting.setOpening(false);
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		if (slotId == CRAFTING_SLOT) {
			warehouseItemHandler = tileEntity.getWarehouse() != null ? tileEntity.getWarehouse().getItemHandler() : null;
		}
		super.clicked(slotId, dragType, clickTypeIn, player);
		currentCraftTotalSize = 0;
	}

	@Override
	public void onContainerClosed(Player par1EntityPlayer) {
		TileWarehouseBase warehouse = tileEntity.getWarehouse();
		if (warehouse != null) {
			warehouse.removeCraftingViewer(this);
		}
		super.onContainerClosed(par1EntityPlayer);
	}

	@Override
	public ItemStack transferStackInSlot(Player par1EntityPlayer, int slotClickedIndex) {
		if (slotClickedIndex == 0 && !updateAndCheckCraftStackOrLessInTotal()) {
			return ItemStack.EMPTY;
		}

		ItemStack slotStackCopy = ItemStack.EMPTY;
		Slot theSlot = this.getSlot(slotClickedIndex);
		if (theSlot.hasItem()) {
			ItemStack slotStack = theSlot.getItem();
			slotStackCopy = slotStack.copy();
			int playerSlotStart = 2 + tileEntity.craftingRecipeMemory.craftMatrix.getContainerSize();
			if (slotClickedIndex < playerSlotStart)//result slot, book slot
			{
				if (!this.moveItemStackTo(slotStack, playerSlotStart, playerSlotStart + playerSlots, false))//merge into player inventory
				{
					return ItemStack.EMPTY;
				}
			} else if (slotClickedIndex < playerSlotStart + playerSlots && !moveItemStackTo(slotStack, BOOK_SLOT, BOOK_SLOT + 1, false)) {
				return ItemStack.EMPTY;
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
	public void handlePacketData(CompoundTag tag) {
		if (tag.contains(CHANGE_LIST_TAG)) {
			handleChangeList(tag.getList(CHANGE_LIST_TAG, Tag.TAG_COMPOUND));
		} else if (tag.contains("recipe")) {
			containerCrafting.handleRecipeUpdate(tag);
		}
		refreshGui();
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		if (shouldUpdate) {
			synchItemMaps();
			shouldUpdate = false;
		}
	}

	private void handleChangeList(ListTag changeList) {
		CompoundTag tag;
		for (int i = 0; i < changeList.size(); i++) {
			tag = changeList.getCompound(i);
			itemMap.putEntryFromNBT(tag);
		}
		TileWarehouseBase warehouse = tileEntity.getWarehouse();
		if (warehouse != null) {
			warehouse.clearItemCache();
			warehouse.addItemsToCache(itemMap);
		}
	}

	private void synchItemMaps() {
		/*
		 * need to loop through this.itemMap and compare quantities to warehouse.itemMap
		 *    add any changes to change-list
		 * need to loop through warehouse.itemMap and find new entries
		 *    add any new entries to change-list
		 */

		cache.clear();
		TileWarehouseBase warehouse = tileEntity.getWarehouse();
		if (warehouse != null) {
			warehouse.getItems(cache);
		}
		ItemQuantityMap warehouseItemMap = cache;
		int qty;
		ListTag changeList = new ListTag();
		CompoundTag tag;
		for (ItemHashEntry wrap : this.itemMap.keySet()) {
			qty = this.itemMap.getCount(wrap);
			if (qty != warehouseItemMap.getCount(wrap)) {
				qty = warehouseItemMap.getCount(wrap);
				changeList.add(warehouseItemMap.writeEntryToNBT(wrap));
				this.itemMap.put(wrap, qty);
			}
		}
		for (ItemHashEntry entry : warehouseItemMap.keySet()) {
			if (!itemMap.contains(entry)) {
				qty = warehouseItemMap.getCount(entry);
				changeList.add(warehouseItemMap.writeEntryToNBT(entry));
				this.itemMap.put(entry, qty);
			}
		}
		if (changeList.size() > 0) {
			tag = new CompoundTag();
			tag.put(CHANGE_LIST_TAG, changeList);
			sendDataToClient(tag);
		}
	}

	public void onWarehouseInventoryUpdated() {
		shouldUpdate = true;
	}

	@Override
	public ContainerCraftingRecipeMemory getCraftingMemoryContainer() {
		return containerCrafting;
	}

	@Override
	public IItemHandlerModifiable[] getInventories() {
		if (tileEntity.getWarehouse() == null) {
			return new IItemHandlerModifiable[] {new PlayerInvWrapper(player.getInventory())};
		}

		return new IItemHandlerModifiable[] {tileEntity.getWarehouse().getItemHandler(), new PlayerInvWrapper(player.getInventory())};
	}

	@Override
	public boolean pushCraftingMatrixToInventories() {
		IItemHandler craftMatrixWrapper = new InvWrapper(tileEntity.craftingRecipeMemory.craftMatrix);
		NonNullList<ItemStack> craftingItems = InventoryTools.getItems(craftMatrixWrapper);

		IItemHandler inventories;
		if (tileEntity.getWarehouse() == null) {
			inventories = new PlayerInvWrapper(player.getInventory());
		} else {
			inventories = new CombinedInvWrapper(tileEntity.getWarehouse().getItemHandler(), new PlayerInvWrapper(player.getInventory()));
		}

		if (InventoryTools.insertItems(inventories, craftingItems, true).isEmpty()) {
			List<ItemStack> remainingItems = InventoryTools.insertItems(inventories, craftingItems, false);
			InventoryTools.emptyInventory(craftMatrixWrapper);
			if (!remainingItems.isEmpty()) {
				InventoryTools.insertItems(craftMatrixWrapper, remainingItems, false);
				return false;
			}
			return true;
		}

		return false;
	}
}
