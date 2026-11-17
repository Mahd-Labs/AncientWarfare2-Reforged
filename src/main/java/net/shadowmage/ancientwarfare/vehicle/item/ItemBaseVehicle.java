package net.shadowmage.ancientwarfare.vehicle.item;

import net.minecraft.world.item.Item;
import net.shadowmage.ancientwarfare.core.item.ItemBase;
import net.shadowmage.ancientwarfare.vehicle.AncientWarfareVehicles;

/*
 * Port notes: the creative tab lists its content itself (AWVehicleTab); item models are plain json files
 * (models/item/<registry path>.json, generated from the 1.12 blockstates/vehicle/*.json variants).
 */
public abstract class ItemBaseVehicle extends ItemBase {
	public ItemBaseVehicle(String regName) {
		super(AncientWarfareVehicles.MOD_ID, regName);
	}

	public ItemBaseVehicle(String regName, Item.Properties properties) {
		super(AncientWarfareVehicles.MOD_ID, regName, properties);
	}
}
