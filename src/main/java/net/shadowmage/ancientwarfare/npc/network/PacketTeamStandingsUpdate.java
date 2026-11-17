package net.shadowmage.ancientwarfare.npc.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.npc.faction.FactionEntry;
import net.shadowmage.ancientwarfare.npc.gamedata.TeamData;

import java.io.IOException;

public class PacketTeamStandingsUpdate extends PacketBase {
	private ResourceLocation teamName;
	private FactionEntry factionEntry;

	public PacketTeamStandingsUpdate() {}

	public PacketTeamStandingsUpdate(ResourceLocation teamName, FactionEntry factionEntry) {
		this.teamName = teamName;
		//1.21 encodes on the network thread; a snapshot is taken here, on the server thread, so a standing change
		//in the same tick cannot break the encoding
		this.factionEntry = new FactionEntry(factionEntry.writeToNBT(new CompoundTag()));
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		FriendlyByteBuf buffer = new FriendlyByteBuf(data);
		buffer.writeUtf(teamName.toString());
		buffer.writeNbt(factionEntry.writeToNBT(new CompoundTag()));
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) throws IOException {
		FriendlyByteBuf buffer = new FriendlyByteBuf(data);
		teamName = ResourceLocation.parse(buffer.readUtf(100));
		CompoundTag tag = buffer.readNbt();
		factionEntry = tag == null ? null : new FactionEntry(tag);
	}

	@Override
	protected void execute(Player player) {
		TeamData teamData = TeamData.get(player.level());
		teamData.updateTeamStandings(teamName, factionEntry);
	}
}
