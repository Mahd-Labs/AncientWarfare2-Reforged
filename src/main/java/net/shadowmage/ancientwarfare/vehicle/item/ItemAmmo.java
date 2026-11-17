package net.shadowmage.ancientwarfare.vehicle.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.shadowmage.ancientwarfare.vehicle.missiles.IAmmo;

import java.util.List;

public class ItemAmmo extends ItemBaseVehicle {
	private final IAmmo ammo;
	private String tooltipName;
	private String tooltipVehicleList;

	public ItemAmmo(ResourceLocation registryName, IAmmo ammo) {
		super(registryName.getPath());
		this.ammo = ammo;
		tooltipName = "item." + registryName.getPath() + ".tooltip";
		tooltipVehicleList = "item." + registryName.getPath() + ".tooltipVehicleList";
	}

	public IAmmo getAmmo() {
		return ammo;
	}

	/* 1.12: setCreativeTab(null) when !ammo.isAvailableAsItem() */
	public boolean isInCreativeTab() {
		return ammo.isAvailableAsItem();
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		tooltip.add(Component.translatable(tooltipName));
		tooltip.add(Component.translatable(tooltipVehicleList));
	}
}
