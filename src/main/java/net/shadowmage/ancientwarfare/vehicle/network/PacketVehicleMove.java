package net.shadowmage.ancientwarfare.vehicle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

import java.io.IOException;

/*
 * Port notes: 1.12 PacketVehicleMove (server -> client) carried position, motion, yaw and pitch of a vehicle on
 * every tick it moved or had a rider, and the client eased towards those values over 4 ticks. The stationary weapons
 * of the port only turn, so the packet is reduced to the exact yaw. It was first left out (the vanilla entity tracker
 * sends the yaw every 3 ticks, rounded to 1.4 degree steps, applied at once), which made turning a stand visibly
 * jerky on a real client (user, 2026-11-14).
 */
public class PacketVehicleMove extends PacketVehicleBase {
	private float yaw;

	public PacketVehicleMove() {}

	public PacketVehicleMove(VehicleBase vehicle, float yaw) {
		super(vehicle);
		this.yaw = yaw;
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		super.writeToStream(data);
		data.writeFloat(yaw);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) throws IOException {
		super.readFromStream(data);
		yaw = data.readFloat();
	}

	@Override
	public void execute() {
		if (vehicle.level().isClientSide && Float.isFinite(yaw)) {
			vehicle.moveHelper.updateMoveData(yaw);
		}
	}
}
