package net.shadowmage.ancientwarfare.vehicle.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemMisc extends ItemBaseVehicle {
	private String itemTypeTooltip;

	public ItemMisc(String regName, VehicleItemType itemType) {
		super(regName);
		itemTypeTooltip = "item." + itemType.getItemTypeString() + ".tooltip";
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		tooltip.add(Component.translatable(itemTypeTooltip));
	}

	public enum VehicleItemType {
		AMMO_MATERIAL("ammo_material"),
		VEHICLE_COMPONENT("vehicle_component");

		private String itemType;

		VehicleItemType(String itemType) {
			this.itemType = itemType;
		}

		public String getItemTypeString() {
			return itemType;
		}
	}
}
