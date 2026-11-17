package net.shadowmage.ancientwarfare.vehicle.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

import java.io.IOException;

public class PacketAmmoUpdate extends PacketVehicleBase {
	private CompoundTag ammoTag;

	public PacketAmmoUpdate() {}

	public PacketAmmoUpdate(VehicleBase vehicle, CompoundTag ammoTag) {
		super(vehicle);
		this.ammoTag = ammoTag;
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		super.writeToStream(data);
		FriendlyByteBuf pb = new FriendlyByteBuf(data);
		pb.writeNbt(ammoTag);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) throws IOException {
		super.readFromStream(data);
		FriendlyByteBuf pb = new FriendlyByteBuf(data);
		ammoTag = pb.readNbt();
	}

	@Override
	public void execute() {
		//server -> client only
		if (vehicle.level().isClientSide && ammoTag != null) {
			vehicle.ammoHelper.updateAmmo(ammoTag);
		}
	}
}
