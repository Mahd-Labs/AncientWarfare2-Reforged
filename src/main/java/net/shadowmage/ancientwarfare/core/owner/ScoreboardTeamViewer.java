package net.shadowmage.ancientwarfare.core.owner;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class ScoreboardTeamViewer implements ITeamViewer {
	@Override
	public boolean areTeamMates(Level world, UUID player1, UUID player2, String playerName1, String playerName2) {
		return player1.equals(player2) || isSameTeam(world, playerName1, playerName2);
	}

	@Override
	public boolean areFriendly(Level world, UUID player1, @Nullable UUID player2, String playerName1, String playerName2) {
		return playerName1.equals(playerName2) || player1.equals(player2) || isSameTeam(world, playerName1, playerName2);
	}

	@Override
	public Set<ResourceLocation> getPlayerTeamNames(Level world, UUID playerId, String playerName) {
		PlayerTeam team = world.getScoreboard().getPlayersTeam(playerName);
		if (team == null) {
			return Collections.emptySet();
		}
		//1.12 ResourceLocation lower cased its input; names that still are no valid resource location are skipped
		ResourceLocation teamName = ResourceLocation.tryParse(team.getName().toLowerCase(Locale.ROOT));
		if (teamName == null) {
			//1.12 accepted any team name; characters a resource location cannot hold become '_'
			teamName = ResourceLocation.tryParse(team.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_"));
		}
		return teamName == null ? Collections.emptySet() : Collections.singleton(teamName);
	}

	@Override
	public String getName() {
		return "minecraft";
	}

	private boolean isSameTeam(Level world, String playerName1, String playerName2) {
		Team team = world.getScoreboard().getPlayersTeam(playerName1);
		return team != null && team.isAlliedTo(world.getScoreboard().getPlayersTeam(playerName2));
	}
}
