package net.shadowmage.ancientwarfare.vehicle.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

import java.io.IOException;

public class PacketAmmoSelect extends PacketVehicleBase {
	private String ammoRegistryName;

	public PacketAmmoSelect() {}

	public PacketAmmoSelect(VehicleBase vehicle, String ammoRegistryName) {
		super(vehicle);
		this.ammoRegistryName = ammoRegistryName;
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		super.writeToStream(data);
		FriendlyByteBuf pb = new FriendlyByteBuf(data);
		pb.writeUtf(ammoRegistryName);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) throws IOException {
		super.readFromStream(data);
		FriendlyByteBuf pb = new FriendlyByteBuf(data);
		ammoRegistryName = pb.readUtf(64);
	}

	@Override
	public void execute() {
		//port addition: sender check and name validation (1.12 trusted both)
		if (senderMayControl() && net.minecraft.resources.ResourceLocation.tryParse(ammoRegistryName) != null) {
			vehicle.ammoHelper.updateSelectedAmmo(ammoRegistryName);
		}
		super.execute();
	}
}
