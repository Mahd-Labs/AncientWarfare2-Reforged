package net.shadowmage.ancientwarfare.core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

public class PacketBlockEvent extends PacketBase {

	private BlockPos pos;
	private int id;
	private short a;
	private short b;

	public PacketBlockEvent() {}

	public PacketBlockEvent(BlockPos pos, Block block, short a, short b) {
		this.pos = pos;
		this.id = BuiltInRegistries.BLOCK.getId(block);
		this.a = a;
		this.b = b;
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		data.writeLong(pos.asLong());
		data.writeVarInt(id);
		data.writeByte(a & 0xff);
		data.writeByte(b & 0xff);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) {
		pos = BlockPos.of(data.readLong());
		id = data.readVarInt();
		a = data.readUnsignedByte();
		b = data.readUnsignedByte();
	}

	@Override
	protected void execute(Player player) {
		player.level().blockEvent(pos, BuiltInRegistries.BLOCK.byId(id), a, b);
	}
}
