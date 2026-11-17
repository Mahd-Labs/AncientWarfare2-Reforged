package net.shadowmage.ancientwarfare.structure.network;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.core.util.PacketHelper;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureMap;

public class PacketStructureEntry extends PacketBase {
	private String dimension;
	private int cx;
	private int cz;
	private StructureEntry entry;

	public PacketStructureEntry() {}

	public PacketStructureEntry(String dimension, int cx, int cz, StructureEntry entry) {
		this.dimension = dimension;
		this.cx = cx;
		this.cz = cz;
		//snapshot on the server thread (1.21 encodes on the network thread)
		CompoundTag snapshot = new CompoundTag();
		entry.writeToNBT(snapshot);
		this.entry = new StructureEntry();
		this.entry.readFromNBT(snapshot);
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		data.writeUtf(dimension);
		data.writeInt(cx);
		data.writeInt(cz);
		CompoundTag entryTag = new CompoundTag();
		entry.writeToNBT(entryTag);
		PacketHelper.writeNBTTag(data, entryTag);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) {
		dimension = data.readUtf();
		cx = data.readInt();
		cz = data.readInt();
		entry = new StructureEntry();
		entry.readFromNBT(PacketHelper.readServerNBTTag(data));
	}

	@Override
	protected void execute(Player player) {
		Level world = player.level();
		if (world != null) {
			//passing false to unique as it's not needed client side, but client side still uses the same structure data storage that needs some value there
			StructureMap.get(world).setGeneratedAt(dimension, cx, cz, entry, false);
		}
	}
}
