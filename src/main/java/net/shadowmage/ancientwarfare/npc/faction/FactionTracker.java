package net.shadowmage.ancientwarfare.npc.faction;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.npc.gamedata.FactionData;
import net.shadowmage.ancientwarfare.npc.gamedata.TeamData;

import java.util.UUID;
import java.util.function.ToIntFunction;

public class FactionTracker {
	public static final FactionTracker INSTANCE = new FactionTracker();

	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent evt) {
		onPlayerLogin(evt.getEntity());
	}

	private void onPlayerLogin(Player player) {
		FactionData data = FactionData.get(player.level());
		data.onPlayerLogin(player);

		TeamData teamData = TeamData.get(player.level());
		teamData.checkAndUpdatePlayerTeamMemberships(player);
	}

	public void adjustStandingFor(Level world, String playerName, String factionName, int adjustment) {
		if (world.isClientSide) {
			throw new IllegalArgumentException("Cannot adjust standing on client world!");
		}
		FactionData data = FactionData.get(world);
		TeamData teamData = TeamData.get(world);
		teamData.adjustStanding(world, data, playerName, factionName, adjustment);
	}

	public void setStandingFor(Level world, String playerName, String factionName, int standing) {
		if (world.isClientSide) {
			throw new IllegalArgumentException("Cannot set standing on client world!");
		}
		FactionData data = FactionData.get(world);
		data.setStandingFor(playerName, factionName, standing);
	}

	public boolean isHostileToPlayer(Level world, UUID playerUUID, String playerName, String factionName) {
		return getStandingFor(world, playerUUID, playerName, factionName) < 0;
	}

	private int getStandingFor(Level world, UUID playerUUID, String playerName, String factionName) {
		return getStandingFor(world, playerName, factionName, teamData -> teamData.getWorstStandingFor(world, playerUUID, playerName, factionName, world.getGameTime()));
	}

	public int getStandingFor(Level world, String playerName, String factionName) {
		return getStandingFor(world, playerName, factionName, teamData -> teamData.getWorstStandingFor(playerName, factionName, world.getGameTime()));
	}

	private int getStandingFor(Level world, String playerName, String factionName, ToIntFunction<TeamData> getTeamStanding) {
		FactionData data = FactionData.get(world);

		int teamStanding = getTeamStanding.applyAsInt(TeamData.get(world));
		int playerStanding = data.getStandingFor(playerName, factionName);
		return playerStanding < teamStanding ? playerStanding : teamStanding;
	}
}
