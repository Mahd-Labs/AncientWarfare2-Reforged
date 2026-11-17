package net.shadowmage.ancientwarfare.npc.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;
import net.shadowmage.ancientwarfare.npc.gamedata.FactionData;

import java.io.IOException;

public class PacketFactionUpdate extends PacketBase {
	private CompoundTag packetData;

	public PacketFactionUpdate(CompoundTag tag) {
		this.packetData = tag;
	}

	public PacketFactionUpdate() {}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		net.shadowmage.ancientwarfare.core.util.PacketHelper.writeNBTTag(data, packetData);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) {
		packetData = net.shadowmage.ancientwarfare.core.util.PacketHelper.readServerNBTTag(data);
	}

	@Override
	protected void execute(Player player) {
		if (packetData != null) {
			FactionData.get(player.level()).handlePacketData(player, packetData);
		}
	}
}
