package net.shadowmage.ancientwarfare.vehicle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

import javax.annotation.Nullable;
import java.io.IOException;

public abstract class PacketVehicleBase extends PacketBase {
	private int entityID;
	protected VehicleBase vehicle = null;
	/* port addition: the player the packet is handled for (server: the sender, client: the local player) */
	protected Player sender = null;

	public PacketVehicleBase() {
	}

	public PacketVehicleBase(VehicleBase vehicle) {
		this.entityID = vehicle.getId();
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		data.writeInt(entityID);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) throws IOException {
		entityID = data.readInt();
	}

	private VehicleBase getVehicle(Level world) {
		Entity ret = world.getEntity(entityID);
		return ret instanceof VehicleBase ? (VehicleBase) ret : null;
	}

	@Override
	protected void execute(Player player) {
		vehicle = getVehicle(player.level());
		if (vehicle == null) {
			return;
		}
		sender = player;
		super.execute(player);
	}

	/*
	 * Port addition (1.12 trusted every sender): a packet that arrives on the server may only change the vehicle if
	 * the sending player is the one riding it. On the client (packets relayed by the server) this is always true.
	 */
	protected boolean senderMayControl() {
		return vehicle != null && sender != null && (sender.level().isClientSide || vehicle.getFirstPassenger() == sender);
	}
}
