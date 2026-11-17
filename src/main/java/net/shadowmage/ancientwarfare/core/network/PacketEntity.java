package net.shadowmage.ancientwarfare.core.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.core.interfaces.IEntityPacketHandler;
import net.shadowmage.ancientwarfare.core.util.PacketHelper;

public class PacketEntity extends PacketBase {

	private int entityId;
	public CompoundTag packetData = new CompoundTag();

	public PacketEntity() {}

	public PacketEntity(Entity e) {
		this.entityId = e.getId();
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		data.writeInt(entityId);
		PacketHelper.writeNBTTag(data, packetData != null ? packetData : new CompoundTag());
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) {
		entityId = data.readInt();
		packetData = PacketHelper.readServerNBTTag(data);
	}

	@Override
	protected void execute(Player player) {
		Entity e = player.level().getEntity(entityId);
		if (e instanceof IEntityPacketHandler) {
			((IEntityPacketHandler) e).handlePacketData(packetData);
		}
	}
}
