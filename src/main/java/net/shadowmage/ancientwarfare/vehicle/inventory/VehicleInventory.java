package net.shadowmage.ancientwarfare.vehicle.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.registry.AmmoRegistry;
import net.shadowmage.ancientwarfare.vehicle.registry.VehicleAmmoEntry;

import java.util.ArrayList;
import java.util.List;

/*
 * Port notes: only the ammo inventory is left. The upgrade and armour inventories went with upgrades / armour, the
 * storage inventory with the chest cart and boats (every ported type has storageBaySize 0). The NBT layout
 * ("inventory" -> "ammoInventory") is the 1.12 one.
 */
public class VehicleInventory {
	private static final String INVENTORY_TAG = "inventory";
	private VehicleBase vehicle;

	public ItemStackHandler ammoInventory;

	public VehicleInventory(VehicleBase vehicle) {
		this.vehicle = vehicle;
	}

	public void setInventorySizes(int ammo) {
		this.ammoInventory = new AmmoStackHandler(vehicle, ammo);
	}

	public void writeToNBT(CompoundTag commonTag) {
		CompoundTag tag = new CompoundTag();
		if (this.ammoInventory != null) {
			tag.put("ammoInventory", NBTHelper.serializeHandler(this.ammoInventory));
		}
		commonTag.put(INVENTORY_TAG, tag);
	}

	public void readFromNBT(CompoundTag commonTag) {
		if (!commonTag.contains(INVENTORY_TAG)) {
			return;
		}
		CompoundTag tag = commonTag.getCompound(INVENTORY_TAG);
		if (this.ammoInventory != null) {
			NBTHelper.deserializeHandler(this.ammoInventory, tag.getCompound("ammoInventory"));
		}
	}

	public List<VehicleAmmoEntry> getAmmoCounts() {
		ArrayList<VehicleAmmoEntry> counts = new ArrayList<VehicleAmmoEntry>();
		for (int i = 0; i < this.ammoInventory.getSlots(); i++) {
			ItemStack stack = this.ammoInventory.getStackInSlot(i);
			AmmoRegistry.getAmmoForStack(stack).ifPresent(ammo -> {
				boolean found = false;
				for (VehicleAmmoEntry ent : counts) {
					if (ent.baseAmmoType == ammo) {
						found = true;
						ent.ammoCount += stack.getCount();
						break;
					}
				}
				if (!found) {
					counts.add(new VehicleAmmoEntry(ammo));
					counts.get(counts.size() - 1).ammoCount += stack.getCount();
				}
			});
		}
		return counts;
	}
}
