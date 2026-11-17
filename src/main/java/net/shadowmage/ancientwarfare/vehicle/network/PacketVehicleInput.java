package net.shadowmage.ancientwarfare.vehicle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

import java.io.IOException;

/*
 * Port notes: 1.12 sent forward / turn / power / rotation input bytes; driving is not ported, so only the turn
 * input (left / right keys, turning a stand in place) is left. Client -> server; only the riding player may send it
 * (1.12 did not check the sender).
 */
public class PacketVehicleInput extends PacketVehicleBase {
	private byte turnInput;

	public PacketVehicleInput() {}

	public PacketVehicleInput(VehicleBase vehicle, byte turnInput) {
		super(vehicle);
		this.turnInput = turnInput;
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		super.writeToStream(data);
		data.writeByte(turnInput);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) throws IOException {
		super.readFromStream(data);
		turnInput = data.readByte();
	}

	@Override
	protected void execute(Player player) {
		super.execute(player);
		if (vehicle != null && !player.level().isClientSide && senderMayControl() && vehicle.isDrivable()) {
			vehicle.moveHelper.handleInputData((byte) 0, turnInput < 0 ? (byte) -1 : turnInput > 0 ? (byte) 1 : (byte) 0, (byte) 0, (byte) 0);
		}
	}
}
