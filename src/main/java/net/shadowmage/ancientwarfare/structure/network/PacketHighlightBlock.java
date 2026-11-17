package net.shadowmage.ancientwarfare.structure.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.structure.util.BlockHighlightInfo;

import java.io.IOException;

public class PacketHighlightBlock extends PacketBase {
	private BlockHighlightInfo blockHighlightInfo;

	public PacketHighlightBlock(BlockHighlightInfo blockHighlightInfo) {
		this.blockHighlightInfo = blockHighlightInfo;
	}

	public PacketHighlightBlock() {
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		blockHighlightInfo.serializeToBuffer(data);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) throws IOException {
		blockHighlightInfo = BlockHighlightInfo.deserializeFromBuffer(data);
	}

	@Override
	protected void execute() {
		net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().setBlockHighlightInfo(blockHighlightInfo);
	}
}
