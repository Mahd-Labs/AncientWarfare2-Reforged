package net.shadowmage.ancientwarfare.vehicle;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleType;
import net.shadowmage.ancientwarfare.vehicle.init.AWVehicleItems;
import net.shadowmage.ancientwarfare.vehicle.item.ItemAmmo;
import net.shadowmage.ancientwarfare.vehicle.missiles.IAmmo;
import net.shadowmage.ancientwarfare.vehicle.registry.AmmoRegistry;

/*
 * Port notes: 1.12 CreativeTabs subclass ("tabs.vehicles") -> registered CreativeModeTab "ancientwarfare:vehicles"
 * with the 1.12 title key. Content in the 1.12 registration order: the spawner stacks (every ported type at every
 * material level, as 1.12 ItemSpawner.getSubItems), the misc items, the ammunition (types that are not available as
 * an item - the secondary ball shots - are left out like in 1.12).
 */
public class AWVehicleTab {
	private AWVehicleTab() {}

	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AncientWarfareVehicles.MOD_ID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("vehicles", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.tabs.vehicles"))
			.icon(() -> new ItemStack(AWVehicleItems.SPAWNER.get()))
			.displayItems((parameters, output) -> {
				VehicleType.getCreativeDisplayItems().forEach(output::accept);
				AWVehicleItems.miscItems().forEach(item -> output.accept(item.get()));
				for (IAmmo ammo : AmmoRegistry.getAmmoTypes()) {
					ItemAmmo item = AmmoRegistry.getItemForAmmo(ammo);
					if (item != null && item.isInCreativeTab()) {
						output.accept(item);
					}
				}
			})
			.build());

	public static void register(IEventBus modBus) {
		TABS.register(modBus);
	}
}
