package net.shadowmage.ancientwarfare.core.container;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import net.shadowmage.ancientwarfare.core.item.ItemBackpack;
import net.shadowmage.ancientwarfare.core.util.EntityTools;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

public class ContainerBackpack extends ContainerBase {

	private ItemStack backpackStack;
	public final int backpackSlotIndex;
	public final InteractionHand hand;
	public final int guiHeight;

	private final IItemHandler handler;

	public ContainerBackpack(Player player, int x, int y, int z) {
		super(player);
		this.hand = EntityTools.getHandHoldingItem(player, ItemBackpack.class);
		if (hand == null) {
			throw new IllegalArgumentException("No backpack in hand");
		}
		backpackStack = player.getItemInHand(hand);
		backpackSlotIndex = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
		handler = InventoryTools.cloneItemHandler(backpackStack.getCapability(Capabilities.ItemHandler.ITEM));
		int xPos, yPos;
		for (int i = 0; i < handler.getSlots(); i++) {
			xPos = (i % 9) * 18 + 8;
			yPos = (i / 9) * 18 + 8;
			addSlotToContainer(new SlotItemHandler(handler, i, xPos, yPos) {
				@Override
				public boolean mayPlace(ItemStack itemStack) {
					return !(itemStack.getItem() instanceof ItemBackpack) && super.mayPlace(itemStack);
				}
			});
		}
		int height = (ItemBackpack.getSize(backpackStack) + 1) * 18 + 8;
		guiHeight = addPlayerSlots(height + 8) + 8;
	}

	@Override
	protected int addPlayerSlots(int tx, int ty, int gap) {
		int y;
		int x;
		int slotNum;
		int xPos;
		int yPos;
		IItemHandler playerInventory = new PlayerMainInvWrapper(player.getInventory());
		for (x = 0; x < 9; ++x)//add player hotbar slots
		{
			slotNum = x;
			if (slotNum == backpackSlotIndex) {
				continue;
			}//TODO add fake slot in gui
			xPos = tx + x * 18;
			yPos = ty + gap + 3 * 18;
			this.addSlotToContainer(new SlotItemHandler(playerInventory, x, xPos, yPos));
		}
		for (y = 0; y < 3; ++y) {
			for (x = 0; x < 9; ++x) {
				slotNum = y * 9 + x + 9;// +9 is to increment past hotbar slots
				xPos = tx + x * 18;
				yPos = ty + y * 18;
				this.addSlotToContainer(new SlotItemHandler(playerInventory, slotNum, xPos, yPos));
			}
		}
		playerSlots = 35;
		return ty + (4 * 18) + gap;
	}

	@Override
	public void onContainerClosed(Player playerIn) {
		IItemHandlerModifiable backpackHandler = (IItemHandlerModifiable) backpackStack.getCapability(Capabilities.ItemHandler.ITEM);
		if (backpackHandler != null) {
			for (int slot = 0; slot < handler.getSlots(); slot++) {
				backpackHandler.setStackInSlot(slot, handler.getStackInSlot(slot));
			}
		}
		super.onContainerClosed(playerIn);
	}

	@Override
	public ItemStack transferStackInSlot(Player par1EntityPlayer, int slotClickedIndex) {
		ItemStack slotStackCopy = ItemStack.EMPTY;
		Slot theSlot = this.getSlot(slotClickedIndex);
		int size = handler.getSlots();
		if (theSlot != null && theSlot.hasItem()) {
			ItemStack slotStack = theSlot.getItem();
			slotStackCopy = slotStack.copy();
			if (slotClickedIndex < size)//clicked in backpack
			{
				if (!this.moveItemStackTo(slotStack, size, size + playerSlots, false))//merge into player inventory
				{
					return ItemStack.EMPTY;
				}
			} else {
				if (!this.moveItemStackTo(slotStack, 0, size, false))//merge into backpack
				{
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
}
