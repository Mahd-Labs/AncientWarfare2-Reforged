package net.shadowmage.ancientwarfare.core.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.AncientWarfare;

import java.io.IOException;

public class PacketHelper {
	private PacketHelper() {}

	/*
	 * Compressed NBT with a length prefix. Port fix: 1.12 wrote the gzip stream straight into the buffer and read it
	 * back with a stream reader. That reader stops at the end of the tag and can leave the 8 byte gzip trailer in the
	 * buffer; 1.21 rejects a packet that was not read completely ("found 8 bytes extra"), which disconnected every
	 * client joining a dedicated server. The reader now takes exactly the bytes the writer wrote.
	 */
	public static void writeNBTTag(ByteBuf data, CompoundTag tag) {
		java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
		try {
			NbtIo.writeCompressed(tag == null ? new CompoundTag() : tag, bytes);
		}
		catch (IOException e) {
			AncientWarfare.LOG.error("Error writing tag to buffer:\n", e);
			bytes.reset();
		}
		net.minecraft.network.VarInt.write(data, bytes.size());
		try {
			bytes.writeTo(new io.netty.buffer.ByteBufOutputStream(data)); //no second copy: templates are large
		}
		catch (IOException e) {
			throw new io.netty.handler.codec.EncoderException(e);
		}
	}

	/* NBT a client sent: bounded like vanilla readNbt (2 MiB), wherever the server runs */
	public static CompoundTag readNBTTag(ByteBuf data) {
		return readNBTTag(data, NbtAccounter.create(2097152L));
	}

	/*
	 * NBT the server sent (only for packets registered TO_CLIENT, which a server never decodes): no limit, as
	 * vanilla trusts server data. The largest template of the pack is about 105 MB of NBT.
	 */
	public static CompoundTag readServerNBTTag(ByteBuf data) {
		return readNBTTag(data, NbtAccounter.unlimitedHeap());
	}

	private static CompoundTag readNBTTag(ByteBuf data, NbtAccounter accounter) {
		int length = net.minecraft.network.VarInt.read(data);
		if (length < 0 || length > data.readableBytes()) {
			throw new io.netty.handler.codec.DecoderException("NBT length " + length + " exceeds the packet (" + data.readableBytes() + " bytes left)");
		}
		byte[] bytes = new byte[length];
		data.readBytes(bytes);
		if (length == 0) {
			return new CompoundTag();
		}
		try {
			return NbtIo.readCompressed(new java.io.ByteArrayInputStream(bytes), accounter);
		}
		catch (IOException e) {
			AncientWarfare.LOG.error("Error reading tag from buffer:\n", e);
			return new CompoundTag();
		}
	}

	/*
	 * Port additions: 1.12 ByteBufUtils.writeItemStack / readItemStack (empty stacks allowed)
	 */
	public static void writeItemStack(RegistryFriendlyByteBuf data, ItemStack stack) {
		ItemStack.OPTIONAL_STREAM_CODEC.encode(data, stack);
	}

	public static ItemStack readItemStack(RegistryFriendlyByteBuf data) {
		return ItemStack.OPTIONAL_STREAM_CODEC.decode(data);
	}
}
