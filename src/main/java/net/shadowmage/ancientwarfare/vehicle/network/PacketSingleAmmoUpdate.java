package net.shadowmage.ancientwarfare.vehicle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

import java.io.IOException;

public class PacketSingleAmmoUpdate extends PacketVehicleBase {
	private String ammoRegistryName;
	private int count;

	public PacketSingleAmmoUpdate() {}

	public PacketSingleAmmoUpdate(VehicleBase vehicle, String ammoRegistryName, int count) {
		super(vehicle);
		this.ammoRegistryName = ammoRegistryName;
		this.count = count;
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		super.writeToStream(data);
		FriendlyByteBuf pb = new FriendlyByteBuf(data);
		pb.writeUtf(ammoRegistryName);
		pb.writeInt(count);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) throws IOException {
		super.readFromStream(data);
		FriendlyByteBuf pb = new FriendlyByteBuf(data);
		ammoRegistryName = pb.readUtf(64);
		count = pb.readInt();
	}

	@Override
	public void execute() {
		//server -> client only
		if (vehicle.level().isClientSide && net.minecraft.resources.ResourceLocation.tryParse(ammoRegistryName) != null) {
			vehicle.ammoHelper.updateAmmoCount(ammoRegistryName, count);
		}
	}
}
