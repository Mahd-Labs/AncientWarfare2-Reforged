package net.shadowmage.ancientwarfare.npc.gamedata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.gamedata.AWSavedData;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.owner.TeamViewerRegistry;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;
import net.shadowmage.ancientwarfare.npc.faction.FactionEntry;
import net.shadowmage.ancientwarfare.npc.network.PacketTeamMembershipUpdate;
import net.shadowmage.ancientwarfare.npc.network.PacketTeamStandingUpdate;
import net.shadowmage.ancientwarfare.npc.network.PacketTeamStandingsUpdate;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class TeamData extends AWSavedData {
	private HashMap<String, Set<Team>> playerTeamsLookup = new HashMap<>();
	private HashMap<ResourceLocation, Team> teams = new HashMap<>();
	private Map<String, Set<LeavingTeam>> playerLeavingTeams = new HashMap<>();

	private static final int LEAVING_EXPIRATION_TIME = 5 * 20 * 60 * 20; //leaving teams expire in 5 minecraft days
	private static final int LEAVING_CHECK_INTERVAL = 20 * 60; //check once a minute
	private long nextLeavingCheck = Long.MIN_VALUE;
	private static final int MEMBERSHIP_CHECK_INTERVAL = 20 * 5; //check once in 5 seconds
	private long nextMembershipCheck = Long.MIN_VALUE;

	private void addTeamMemberUpdateStanding(ResourceLocation teamName, String playerName, FactionData factionData) {
		Team team = addTeamMember(teamName, playerName);
		NetworkHandler.sendToAllPlayers(new PacketTeamMembershipUpdate(teamName, playerName, PacketTeamMembershipUpdate.Action.ADD));

		team.updateFactionStandings(factionData, playerName);
		NetworkHandler.sendToAllPlayers(new PacketTeamStandingsUpdate(teamName, team.getFactionStandings()));

		setDirty();
	}

	public Team addTeamMember(ResourceLocation teamName, String playerName) {
		if (!teams.containsKey(teamName)) {
			teams.put(teamName, new Team(teamName));
		}
		Team team = teams.get(teamName);
		team.addMember(playerName);
		if (!playerTeamsLookup.containsKey(playerName)) {
			playerTeamsLookup.put(playerName, new HashSet<>());
		}
		playerTeamsLookup.get(playerName).add(team);

		if (playerLeavingTeams.containsKey(playerName)) {
			playerLeavingTeams.get(playerName).removeIf(leavingTeam -> leavingTeam.getTeamName().equals(teamName));
		}
		setDirty();
		return team;
	}

	private void removeTeamMember(ResourceLocation teamName, String player, long totalWorldTime) {
		removeTeamMember(teamName, player, totalWorldTime, true);
	}

	public void removeTeamMember(ResourceLocation teamName, String player, long totalWorldTime, boolean isServer) {
		if (!teams.containsKey(teamName)) {
			AncientWarfareNPC.LOG.error("Non existent team can't have members removed");
			return;
		}
		Team team = teams.get(teamName);
		team.removeMember(player);
		if (playerTeamsLookup.containsKey(player)) {
			playerTeamsLookup.get(player).remove(team);
		}
		addToPlayerLeavingTeams(teamName, player, totalWorldTime);

		if (isServer) {
			NetworkHandler.sendToAllPlayers(new PacketTeamMembershipUpdate(teamName, player, PacketTeamMembershipUpdate.Action.REMOVE));
		}
		setDirty();
	}

	private void addToPlayerLeavingTeams(ResourceLocation teamName, String player, long totalWorldTime) {
		if (!playerLeavingTeams.containsKey(player)) {
			playerLeavingTeams.put(player, new HashSet<>());
		}
		playerLeavingTeams.get(player).add(new LeavingTeam(totalWorldTime, teamName));
	}

	/*
	 * Port notes: see FactionData.get
	 */
	private static TeamData clientInstance;

	public static TeamData get(Level world) {
		if (world.isClientSide) {
			if (clientInstance == null) {
				clientInstance = new TeamData("AWTeamData");
			}
			return clientInstance;
		}
		return net.shadowmage.ancientwarfare.core.gamedata.AWGameData.INSTANCE.<TeamData>getData(world, TeamData.class);
	}

	public TeamData(String name) {
		super(name);
	}

	@Override
	public void readFromNBT(CompoundTag nbt) {
		ListTag teamsNBT = nbt.getList("teams", Tag.TAG_COMPOUND);

		for (int i = 0; i < teamsNBT.size(); i++) {
			CompoundTag teamNBT = teamsNBT.getCompound(i);
			Team team = Team.deserializeNBT(teamNBT);

			teams.put(team.getName(), team);
			for (String member : team.getMembers()) {
				if (!playerTeamsLookup.containsKey(member)) {
					playerTeamsLookup.put(member, new HashSet<>());
				}

				playerTeamsLookup.get(member).add(team);
			}
		}
		playerLeavingTeams = NBTHelper.getMap(nbt.getList("playerLeavingTeams", Tag.TAG_COMPOUND), t -> t.getString("playerName"),
				t -> NBTHelper.getSet(t.getList("leavingTeams", Tag.TAG_COMPOUND), leavingTeamNBT -> LeavingTeam.deserializeNBT((CompoundTag) leavingTeamNBT)));
	}

	@Override
	public CompoundTag writeToNBT(CompoundTag compound) {
		ListTag teamsNBT = new ListTag();
		for (Team team : teams.values()) {
			teamsNBT.add(team.serializeNBT());
		}
		if (teamsNBT.size() > 0) {
			compound.put("teams", teamsNBT);
		}

		NBTHelper.mapToCompoundList(playerLeavingTeams, (tag, key) -> tag.putString("playerName", key),
				(tag, leavingTeams) -> tag.put("leavingTeams", NBTHelper.getTagList(leavingTeams, LeavingTeam::serializeNBT)));

		return compound;
	}

	public void checkAndUpdatePlayerTeamMemberships(Player player) {
		String playerName = player.getName().getString();
		Set<ResourceLocation> teamNames = TeamViewerRegistry.getPlayerTeamNames(player.level(), player.getUUID(), playerName);

		FactionData factionData = FactionData.get(player.level());
		for (ResourceLocation teamName : teamNames) {
			addTeamMemberUpdateStanding(teamName, playerName, factionData);
		}

		if (playerTeamsLookup.containsKey(playerName)) {
			for (Team team : playerTeamsLookup.get(playerName)) {
				if (!teamNames.contains(team.getName())) {
					removeTeamMember(team.getName(), playerName, player.level().getGameTime());
				}
			}
		}
	}

	public int getWorstStandingFor(Level world, UUID playerUUID, String playerName, String factionName, long totalWorldTime) {
		if (!world.isClientSide) {
			checkTeamMembershipChanges(world, playerUUID, playerName);
		}
		return getWorstStandingFor(playerName, factionName, totalWorldTime);
	}

	public int getWorstStandingFor(String playerName, String factionName, long totalWorldTime) {
		int currentStanding = getWorstCurrentTeamsStanding(playerName, factionName);
		int leavingStanding = getWorstLeavingTeamStanding(playerName, factionName, totalWorldTime);

		return currentStanding < leavingStanding ? currentStanding : leavingStanding;
	}

	private void checkTeamMembershipChanges(Level world, UUID playerUUID, String playerName) {
		if (nextMembershipCheck > world.getGameTime()) {
			return;
		}
		nextMembershipCheck = world.getGameTime() + MEMBERSHIP_CHECK_INTERVAL;

		Set<String> domainsToRefresh = TeamViewerRegistry.getRegularlyCheckedViewerNames();
		Set<ResourceLocation> currentPlayerTeamNames = TeamViewerRegistry.getRegularlyCheckedPlayerTeamNames(world, playerUUID, playerName);

		Set<ResourceLocation> teamsToRemoveFrom = new HashSet<>();
		Set<Team> playerTeams = playerTeamsLookup.getOrDefault(playerName, new HashSet<>());
		for (Team team : playerTeams) {
			ResourceLocation teamName = team.getName();
			if (domainsToRefresh.contains(teamName.getNamespace()) && !currentPlayerTeamNames.contains(teamName)) {
				teamsToRemoveFrom.add(teamName);
			}
		}

		teamsToRemoveFrom.forEach(teamName -> removeTeamMember(teamName, playerName, world.getGameTime()));

		for (ResourceLocation teamName : currentPlayerTeamNames) {
			if (!teams.containsKey(teamName) || !teams.get(teamName).isMember(playerName)) {
				addTeamMemberUpdateStanding(teamName, playerName, FactionData.get(world));
			}
		}
	}

	private int getWorstCurrentTeamsStanding(String playerName, String factionName) {
		int worstStanding = Integer.MAX_VALUE;

		if (!playerTeamsLookup.containsKey(playerName)) {
			return worstStanding;
		}

		Set<Team> playersTeams = playerTeamsLookup.get(playerName);

		for (Team team : playersTeams) {
			int teamStanding = team.getFactionStanding(factionName);
			if (teamStanding < worstStanding) {
				worstStanding = teamStanding;
			}
		}
		return worstStanding;
	}

	private int getWorstLeavingTeamStanding(String playerName, String factionName, long totalWorldTime) {
		checkLeavingExpiration(totalWorldTime);

		int worstStanding = Integer.MAX_VALUE;
		if (playerLeavingTeams.containsKey(playerName)) {
			for (LeavingTeam team : playerLeavingTeams.get(playerName)) {
				ResourceLocation teamName = team.getTeamName();
				if (teams.containsKey(teamName)) {
					int teamStanding = teams.get(teamName).getFactionStanding(factionName);
					if (teamStanding < worstStanding) {
						worstStanding = teamStanding;
					}
				}
			}
		}
		return worstStanding;
	}

	private void checkLeavingExpiration(long totalWorldTime) {
		if (nextLeavingCheck > totalWorldTime) {
			return;
		}
		nextLeavingCheck = totalWorldTime + LEAVING_CHECK_INTERVAL;

		for (Set<LeavingTeam> leavingTeams : playerLeavingTeams.values()) {
			leavingTeams.removeIf(leavingTeam -> leavingTeam.getLeftWorldTime() + LEAVING_EXPIRATION_TIME < totalWorldTime);
		}
	}

	public void adjustStanding(Level world, FactionData factionData, String playerName, String factionName, int change) {
		Player player = world.getServer().getPlayerList().getPlayerByName(playerName);
		if (player != null && !world.isClientSide) {
			checkTeamMembershipChanges(world, player.getUUID(), playerName);
		}

		if (!playerTeamsLookup.containsKey(playerName)) {
			factionData.adjustStandingFor(world, playerName, factionName, change);
			return;
		}

		factionData.adjustStandingFor(world, playerName, factionName, change);

		Set<Team> playersTeams = playerTeamsLookup.get(playerName);

		for (Team team : playersTeams) {
			team.adjustStandingFor(world, factionData, factionName, change, playerName);
			NetworkHandler.sendToAllPlayers(new PacketTeamStandingUpdate(team.getName(), factionName, team.getFactionStanding(factionName)));
		}
		setDirty();
	}

	public void updateTeamStandings(ResourceLocation teamName, FactionEntry factionEntry) {
		if (teams.containsKey(teamName)) {
			teams.get(teamName).setFactionStandings(factionEntry);
		}
		setDirty();
	}

	public void updateTeamStanding(ResourceLocation teamName, String factionName, int standing) {
		if (teams.containsKey(teamName)) {
			teams.get(teamName).setFactionStanding(factionName, standing);
		}
		setDirty();
	}

	public Collection<Team> getTeams() {
		return teams.values();
	}

	public Optional<Team> getTeam(ResourceLocation teamName) {
		return Optional.ofNullable(teams.get(teamName));
	}

	public Set<Team> getPlayerTeams(String playerName) {
		return playerTeamsLookup.getOrDefault(playerName, new HashSet<>());
	}

	private static class LeavingTeam {
		private final long leftWorldTime;
		private final ResourceLocation teamName;

		private LeavingTeam(long leftWorldTime, ResourceLocation teamName) {
			this.leftWorldTime = leftWorldTime;
			this.teamName = teamName;
		}

		private long getLeftWorldTime() {
			return leftWorldTime;
		}

		private ResourceLocation getTeamName() {
			return teamName;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			LeavingTeam that = (LeavingTeam) o;
			return teamName.equals(that.teamName);
		}

		@Override
		public int hashCode() {
			return teamName.hashCode();
		}

		public CompoundTag serializeNBT() {
			CompoundTag ret = new CompoundTag();
			ret.putLong("leftWorldTime", leftWorldTime);
			ret.putString("teamName", teamName.toString());
			return ret;
		}

		public static LeavingTeam deserializeNBT(CompoundTag tag) {
			return new LeavingTeam(tag.getLong("leftWorldTime"), java.util.Objects.requireNonNullElse(net.shadowmage.ancientwarfare.core.util.RegistryTools.parseOrNull(tag.getString("teamName")), ResourceLocation.fromNamespaceAndPath("minecraft", tag.getString("teamName").toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_"))));
		}
	}
}