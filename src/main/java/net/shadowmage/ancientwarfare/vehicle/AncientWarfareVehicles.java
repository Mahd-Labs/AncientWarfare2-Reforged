package net.shadowmage.ancientwarfare.vehicle;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.npc.xmod.IVehicle;
import net.shadowmage.ancientwarfare.npc.xmod.IVehicleHooks;
import net.shadowmage.ancientwarfare.npc.xmod.NpcXmod;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.container.ContainerVehicle;
import net.shadowmage.ancientwarfare.vehicle.container.ContainerVehicleInventory;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.init.AWVehicleEntities;
import net.shadowmage.ancientwarfare.vehicle.init.AWVehicleItems;
import net.shadowmage.ancientwarfare.vehicle.init.AWVehicleSounds;
import net.shadowmage.ancientwarfare.vehicle.network.PacketAimUpdate;
import net.shadowmage.ancientwarfare.vehicle.network.PacketAmmoSelect;
import net.shadowmage.ancientwarfare.vehicle.network.PacketAmmoUpdate;
import net.shadowmage.ancientwarfare.vehicle.network.PacketFireUpdate;
import net.shadowmage.ancientwarfare.vehicle.network.PacketOpenAmmoSelection;
import net.shadowmage.ancientwarfare.vehicle.network.PacketPackCommand;
import net.shadowmage.ancientwarfare.vehicle.network.PacketSingleAmmoUpdate;
import net.shadowmage.ancientwarfare.vehicle.network.PacketVehicleInput;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/*
 * Module entry point. 1.12 had one @Mod per module; the port is a single mod ("ancientwarfare") and
 * AncientWarfare calls init() of every module from the mod constructor.
 *
 * Reduced module: stationary siege weapons only (see docs/DETAILS.md, "What is in, and what is not").
 * 1.12 preInit (config, entities, packets, containers) -> init(); the client proxy is AncientWarfareVehiclesClient.
 * Not registered: PacketUpgradeUpdate (upgrades dropped), PacketVehicleMove (vanilla entity tracking),
 * PacketTurretAnglesUpdate (never sent in 1.12: "TODO figure out if this is needed at all"; its id carries the
 * port's PacketOpenAmmoSelection). Both vehicle GUIs are opened by the server only (interaction / that packet).
 * PacketVehicleInput is reduced to the turn input.
 * The NPC module reaches vehicles through npc.xmod.IVehicleHooks, installed here.
 */
public final class AncientWarfareVehicles {
	private AncientWarfareVehicles() {}

	public static final String MOD_ID = "ancientwarfare";

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = AWVehicleTab.TAB;

	public static final Logger LOG = AncientWarfare.LOG;
	public static AWVehicleStatics statics;

	public static void init(IEventBus modBus, ModContainer container) {
		statics = new AWVehicleStatics("ancientwarfare-vehicle");
		statics.register(modBus, container);

		AWVehicleEntities.register(modBus);
		AWVehicleItems.register(modBus);
		AWVehicleSounds.register(modBus);
		AWVehicleTab.register(modBus);

		//aim, ammo selection and fire: sent by the riding player, checked and relayed to the tracking clients by the server
		PacketBase.registerPacketType(NetworkHandler.PACKET_AIM_UPDATE, PacketAimUpdate.class, PacketAimUpdate::new, PacketBase.Direction.BOTH);
		PacketBase.registerPacketType(NetworkHandler.PACKET_AMMO_SELECT, PacketAmmoSelect.class, PacketAmmoSelect::new, PacketBase.Direction.BOTH);
		PacketBase.registerPacketType(NetworkHandler.PACKET_FIRE_UPDATE, PacketFireUpdate.class, PacketFireUpdate::new, PacketBase.Direction.BOTH);
		PacketBase.registerPacketType(NetworkHandler.PACKET_AMMO_UPDATE, PacketAmmoUpdate.class, PacketAmmoUpdate::new, PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_SINGLE_AMMO_UPDATE, PacketSingleAmmoUpdate.class, PacketSingleAmmoUpdate::new, PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_VEHICLE_MOVE, net.shadowmage.ancientwarfare.vehicle.network.PacketVehicleMove.class, net.shadowmage.ancientwarfare.vehicle.network.PacketVehicleMove::new, PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_PACK_COMMAND, PacketPackCommand.class, PacketPackCommand::new, PacketBase.Direction.TO_SERVER);
		PacketBase.registerPacketType(NetworkHandler.PACKET_VEHICLE_INPUT, PacketVehicleInput.class, PacketVehicleInput::new, PacketBase.Direction.TO_SERVER);
		PacketBase.registerPacketType(NetworkHandler.PACKET_TURRET_ANGLES_UPDATE, PacketOpenAmmoSelection.class, PacketOpenAmmoSelection::new, PacketBase.Direction.TO_SERVER);

		NetworkHandler.registerContainer(NetworkHandler.GUI_VEHICLE_INVENTORY, ContainerVehicleInventory::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_VEHICLE_AMMO_SELECTION, ContainerVehicle::new);

		NpcXmod.setVehicleHooks(new IVehicleHooks() {
			@Nullable
			@Override
			public IVehicle asVehicle(@Nullable Entity entity) {
				return entity instanceof VehicleBase ? (VehicleBase) entity : null;
			}

			@Override
			public List<IVehicle> getVehiclesWithinAABB(Level level, AABB bb) {
				return new ArrayList<>(level.getEntitiesOfClass(VehicleBase.class, bb));
			}
		});
	}
}
