package net.shadowmage.ancientwarfare.vehicle;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredItem;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.NetworkHandlerClient;
import net.shadowmage.ancientwarfare.vehicle.entity.IVehicleType;
import net.shadowmage.ancientwarfare.vehicle.gui.GuiVehicleAmmoSelection;
import net.shadowmage.ancientwarfare.vehicle.gui.GuiVehicleInventory;
import net.shadowmage.ancientwarfare.vehicle.init.AWVehicleEntities;
import net.shadowmage.ancientwarfare.vehicle.init.AWVehicleItems;
import net.shadowmage.ancientwarfare.vehicle.input.VehicleInputHandler;
import net.shadowmage.ancientwarfare.vehicle.item.ItemSpawner;
import net.shadowmage.ancientwarfare.vehicle.missiles.IAmmo;
import net.shadowmage.ancientwarfare.vehicle.model.VehicleModelLayers;
import net.shadowmage.ancientwarfare.vehicle.registry.AmmoRegistry;
import net.shadowmage.ancientwarfare.vehicle.registry.VehicleRegistry;
import net.shadowmage.ancientwarfare.vehicle.render.RenderMissile;
import net.shadowmage.ancientwarfare.vehicle.render.RenderOverlay;
import net.shadowmage.ancientwarfare.vehicle.render.RenderOverlayAdvanced;
import net.shadowmage.ancientwarfare.vehicle.render.RenderVehicle;

/*
 * Client-only module entry point (replaces the 1.12 ClientProxy). Called from AncientWarfareClient,
 * which only loads on the physical client. Never reference this class from common code.
 *
 * 1.12 ClientProxy.preInit: registerGui x3, entity renderers, overlays -> here (GUI_VEHICLE_STATS is not registered:
 * the stats screen is only ever opened from the inventory screen, see GuiVehicleStats); init: key bindings ->
 * RegisterKeyMappingsEvent. Item model registration (ItemSpawner custom mesh definition) -> the item property
 * "ancientwarfare:level" used by the overrides of models/item/spawner_*.json.
 */
public final class AncientWarfareVehiclesClient {
	private AncientWarfareVehiclesClient() {}

	public static void init(IEventBus modBus, ModContainer container) {
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_VEHICLE_AMMO_SELECTION, GuiVehicleAmmoSelection::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_VEHICLE_INVENTORY, GuiVehicleInventory::new);

		modBus.addListener(VehicleModelLayers::register);
		modBus.addListener(AncientWarfareVehiclesClient::registerRenderers);
		modBus.addListener(VehicleInputHandler::initKeyBindings);
		modBus.addListener(AncientWarfareVehiclesClient::clientSetup);

		NeoForge.EVENT_BUS.register(new RenderOverlay());
		NeoForge.EVENT_BUS.register(new RenderOverlayAdvanced());
	}

	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(AWVehicleEntities.MISSILE.get(), RenderMissile::new);
		event.registerEntityRenderer(AWVehicleEntities.VEHICLE.get(), RenderVehicle::new);
	}

	private static void clientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			ResourceLocation level = ResourceLocation.fromNamespaceAndPath(AncientWarfareVehicles.MOD_ID, "level");
			for (DeferredItem<ItemSpawner> spawner : AWVehicleItems.SPAWNERS.values()) {
				ItemProperties.register(spawner.get(), level, (stack, world, entity, seed) -> ItemSpawner.getLevel(stack));
			}

			if (net.shadowmage.ancientwarfare.core.util.ShowcaseHooks.isEnabled()) {
				//same variant names as the 1.12 harness: vehicle config name, ammo registry path
				ResourceLocation vehicleId = BuiltInRegistries.ENTITY_TYPE.getKey(AWVehicleEntities.VEHICLE.get());
				for (IVehicleType type : VehicleRegistry.getTypes()) {
					CompoundTag nbt = new CompoundTag();
					nbt.putInt("vehType", type.getGlobalVehicleType());
					nbt.putInt("matLvl", 0);
					net.shadowmage.ancientwarfare.core.util.ShowcaseHooks.registerEntityVariant(vehicleId, type.getConfigName(), nbt);
				}
				ResourceLocation missileId = BuiltInRegistries.ENTITY_TYPE.getKey(AWVehicleEntities.MISSILE.get());
				for (IAmmo ammo : AmmoRegistry.getAmmoTypes()) {
					CompoundTag nbt = new CompoundTag();
					nbt.putString("ammoRegistryName", ammo.getRegistryName().toString());
					nbt.putBoolean("inGround", true);
					net.shadowmage.ancientwarfare.core.util.ShowcaseHooks.registerEntityVariant(missileId, ammo.getRegistryName().getPath(), nbt);
				}
			}
		});
	}
}
