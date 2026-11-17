package net.shadowmage.ancientwarfare.vehicle.network;

import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

/*
 * Port addition. 1.12: the ammo selection key called NetworkHandler.openGui(GUI_VEHICLE_AMMO_SELECTION) on the client,
 * which asked the server to open that GUI. The server of the port only accepts GUI requests of clients that come
 * from an open GUI of the same entity, so the key sends this packet instead (client -> server) and the server opens
 * the selection screen itself, for the player riding the weapon only. It uses the packet id of the unported
 * PacketTurretAnglesUpdate.
 */
public class PacketOpenAmmoSelection extends PacketVehicleBase {
	public PacketOpenAmmoSelection() {}

	public PacketOpenAmmoSelection(VehicleBase vehicle) {
		super(vehicle);
	}

	@Override
	protected void execute(Player player) {
		super.execute(player);
		if (vehicle != null && !player.level().isClientSide && senderMayControl() && vehicle.isAmmoLoaded() && !vehicle.vehicleType.getValidAmmoTypes().isEmpty()) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_VEHICLE_AMMO_SELECTION, vehicle.getId());
		}
	}
}
