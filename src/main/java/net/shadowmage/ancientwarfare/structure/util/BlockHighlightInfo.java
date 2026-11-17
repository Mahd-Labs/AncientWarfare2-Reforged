package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class BlockHighlightInfo {
	public static final BlockHighlightInfo EXPIRED = new BlockHighlightInfo(BlockPos.ZERO, 0);
	private BlockPos pos;
	private long expirationTime;

	public BlockHighlightInfo(BlockPos pos, long expirationTime) {
		this.pos = pos;
		this.expirationTime = expirationTime;
	}

	public long getExpirationTime() {
		return expirationTime;
	}

	public BlockPos getPos() {
		return pos;
	}

	public void serializeToBuffer(RegistryFriendlyByteBuf data) {
		data.writeLong(pos.asLong());
		data.writeLong(expirationTime);
	}

	public static BlockHighlightInfo deserializeFromBuffer(RegistryFriendlyByteBuf data) {
		return new BlockHighlightInfo(BlockPos.of(data.readLong()), data.readLong());
	}
}
