package net.shadowmage.ancientwarfare.structure.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;

import java.io.IOException;

public class PacketStructure extends PacketBase {
	public CompoundTag packetData = new CompoundTag();

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		net.shadowmage.ancientwarfare.core.util.PacketHelper.writeNBTTag(data, packetData);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) {
		packetData = net.shadowmage.ancientwarfare.core.util.PacketHelper.readServerNBTTag(data);
	}

	@Override
	protected void execute(net.minecraft.world.entity.player.Player player) {
		//only the server sends this packet (registered TO_CLIENT); requests travel as PacketStructureRequest
		if (!player.level().isClientSide) {
			return;
		}
		StructureTemplateManager.onTemplateData(packetData);
	}
}
