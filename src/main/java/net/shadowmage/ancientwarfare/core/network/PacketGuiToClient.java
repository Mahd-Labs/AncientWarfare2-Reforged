package net.shadowmage.ancientwarfare.core.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.RegistryFriendlyByteBuf;

/*
 * Port addition: the server -> client half of PacketGui. 1.21 reads packet NBT with a 2 MiB limit, which a
 * big warehouse (thousands of item types, filled shulker boxes) exceeds, and the player opening it was disconnected.
 * Data from the server is read without a limit, as vanilla does for its own packets; what a client sends
 * (PacketGui, registered TO_SERVER) keeps the limit. NetworkHandler.sendToPlayer swaps the class, so containers
 * keep building a PacketGui as in 1.12.
 */
public class PacketGuiToClient extends PacketGui {
	public PacketGuiToClient() {}

	public PacketGuiToClient(PacketGui source) {
		super(source.packetData);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) {
		packetData = data.readNbt(NbtAccounter.unlimitedHeap()) instanceof CompoundTag tag ? tag : new CompoundTag();
	}
}
