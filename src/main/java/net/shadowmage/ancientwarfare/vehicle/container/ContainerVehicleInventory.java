package net.shadowmage.ancientwarfare.vehicle.container;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.shadowmage.ancientwarfare.vehicle.item.ItemAmmo;

/*
 * Port notes: reduced with the inventories (see VehicleInventory): only the six ammo slots are left. Upgrade and
 * armour slots are gone with upgrades / armour, the scrolling storage rows with the chest cart and boats (storage
 * size is 0 for every ported type, which made storageY / extrasY / playerY what they are below in 1.12 as well).
 * Slot positions of the ammo and player slots are the 1.12 ones.
 */
public class ContainerVehicleInventory extends ContainerVehicle {
	public int storageY;
	public int extrasY;
	public int playerY;

	public ContainerVehicleInventory(Player openingPlayer, int entityId, int dummy1, int dummy2) {
		super(openingPlayer, entityId, dummy1, dummy2);
		int y;
		int x;
		int slotNum;
		int xPos;
		int yPos;

		storageY = 4 + 10 + 20;
		extrasY = storageY;
		playerY = extrasY + 4 + 10 + 2 * 18;

		this.addPlayerSlots(8, playerY, 4);
		if (vehicle == null) {
			return;
		}

		for (y = 0; y < 2; y++) {
			for (x = 0; x < 3; x++) {
				slotNum = y * 3 + x;
				if (slotNum < vehicle.inventory.ammoInventory.getSlots()) {
					xPos = 8 + x * 18;
					yPos = y * 18 + extrasY;
					this.addSlotToContainer(new SlotItemHandler(vehicle.inventory.ammoInventory, slotNum, xPos, yPos));
				}
			}
		}
	}

	@Override
	public ItemStack transferStackInSlot(Player player, int slotClickedIndex) {
		ItemStack slotStackCopy = ItemStack.EMPTY;
		Slot theSlot = this.slots.get(slotClickedIndex);
		if (theSlot != null && theSlot.hasItem()) {
			ItemStack slotStack = theSlot.getItem();
			slotStackCopy = slotStack.copy();
			int ammoSlots = vehicle.inventory.ammoInventory.getSlots();
			if (slotClickedIndex < 36)//player slots...
			{
				if (slotStackCopy.getItem() instanceof ItemAmmo) {
					if (!this.moveItemStackTo(slotStack, 36, 36 + ammoSlots, false)) {
						return ItemStack.EMPTY;
					}
				} else {
					return ItemStack.EMPTY;
				}
			} else if (slotClickedIndex >= 36 && slotClickedIndex < 36 + ammoSlots && !this.moveItemStackTo(slotStack, 0, 36, true)) {
				return ItemStack.EMPTY;
			}
			theSlot.setChanged();
			vehicle.ammoHelper.updateAmmoCounts();
			vehicle.upgradeHelper.updateUpgrades();
			if (slotStack.getCount() == slotStackCopy.getCount()) {
				return ItemStack.EMPTY;
			}
			theSlot.onTake(player, slotStack);
		}
		return slotStackCopy;
	}

	@Override
	public void handlePacketData(CompoundTag tag) {
	}

	@Override
	public boolean canInteractWith(Player player) {
		return super.canInteractWith(player) && player != null && player.distanceTo(vehicle) < 8.d;
	}
}
