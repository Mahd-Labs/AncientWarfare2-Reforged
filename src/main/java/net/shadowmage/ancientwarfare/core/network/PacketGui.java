package net.shadowmage.ancientwarfare.core.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.container.ContainerBase;

public class PacketGui extends PacketBase {
	protected CompoundTag packetData;

	public PacketGui(CompoundTag packetData) {
		this.packetData = packetData;
	}

	public PacketGui() {
		packetData = new CompoundTag();
	}

	public void setOpenGui(int id, int x, int y, int z) {
		packetData.putBoolean("openGui", true);
		packetData.putInt("id", id);
		packetData.putInt("x", x);
		packetData.putInt("y", y);
		packetData.putInt("z", z);
	}

	public void setTag(String key, CompoundTag tag) {
		packetData.put(key, tag);
	}

	public void setData(CompoundTag tag) {
		this.packetData = tag;
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		data.writeNbt(packetData == null ? new CompoundTag() : packetData);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) {
		CompoundTag tag = data.readNbt();
		packetData = tag == null ? new CompoundTag() : tag;
	}

	@Override
	protected void execute(Player player) {
		if (packetData.contains("openGui")) {
			if (!player.level().isClientSide && !NetworkHandler.INSTANCE.clientMayOpen(player, packetData.getInt("id"), packetData.getInt("x"), packetData.getInt("y"), packetData.getInt("z"))) {
				AncientWarfare.LOG.warn("Refused GUI {} requested by client {} (not a GUI a client may open from where it is)", packetData.getInt("id"), player.getGameProfile().getName());
				return;
			}
			NetworkHandler.INSTANCE.openGui(player, packetData.getInt("id"), packetData.getInt("x"), packetData.getInt("y"), packetData.getInt("z"));
		} else if (player.containerMenu instanceof ContainerBase) {
			((ContainerBase) player.containerMenu).onPacketData(packetData);
		} else {
			AncientWarfare.LOG.error("Invalid target found when processing GUI/Container packet : {} packet: {}", player.containerMenu, packetData);
		}
	}
}
