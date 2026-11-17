package net.shadowmage.ancientwarfare.vehicle.network;

import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

public class PacketPackCommand extends PacketVehicleBase {
	public PacketPackCommand() {}

	public PacketPackCommand(VehicleBase vehicle) {
		super(vehicle);
	}

	@Override
	public void execute() {
		//port addition (1.12 trusted every sender): only the player who has the inventory screen of this weapon open may pack it
		if (sender != null && !sender.level().isClientSide && sender.containerMenu instanceof net.shadowmage.ancientwarfare.vehicle.container.ContainerVehicleInventory
				&& ((net.shadowmage.ancientwarfare.vehicle.container.ContainerVehicleInventory) sender.containerMenu).vehicle == vehicle && sender.distanceTo(vehicle) < 8.d) {
			vehicle.packVehicle();
		}
	}
}
