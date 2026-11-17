package net.shadowmage.ancientwarfare.npc.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.command.ParentCommand;
import net.shadowmage.ancientwarfare.core.command.RootCommand;
import net.shadowmage.ancientwarfare.core.command.SimpleSubCommand;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.util.StringTools;
import net.shadowmage.ancientwarfare.npc.gamedata.Team;
import net.shadowmage.ancientwarfare.npc.gamedata.TeamData;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CommandTeams extends RootCommand {
	public CommandTeams() {
		registerSubCommand(new SimpleSubCommand("list", (server, sender, args) -> {
			for (Team team : TeamData.get(sender.getLevel()).getTeams()) {
				sender.sendSystemMessage(Component.literal(team.getName().toString()));
			}
		}));
		registerSubCommand(new SimpleSubCommand("describe", (server, sender, args) -> {
			if (args.length == 0) {
				sender.sendSystemMessage(Component.translatable(CommandTeams.this.getUsage(sender)));
				return;
			}
			ResourceLocation teamName = ResourceLocation.tryParse(args[0]);
			Optional<Team> t = teamName == null ? Optional.empty() : TeamData.get(sender.getLevel()).getTeam(teamName);

			if (t.isPresent()) {
				Team team = t.get();
				sender.sendSystemMessage(Component.translatable("command.aw.teams.team_members", StringTools.joinElements(", ", team.getMembers())));
				outputTeamStandings(sender, team);
			} else {
				sender.sendSystemMessage(Component.translatable("command.aw.teams.team_does_not_exist"));
			}
		}) {
			@Override
			public int getMaxArgs() {
				return 1;
			}
		});
		registerSubCommand(new SimpleSubCommand("player", (server, sender, args) -> {
			sender.sendSystemMessage(Component.translatable("command.aw.teams.member_of"));
			String playerName = args.length == 0 ? sender.getTextName() : args[0];
			for (Team team : TeamData.get(sender.getLevel()).getPlayerTeams(playerName)) {
				sender.sendSystemMessage(Component.translatable("command.aw.teams.team_name", team.getName().toString()));
				outputTeamStandings(sender, team);
			}
		}) {
			@Override
			public int getMaxArgs() {
				return 1;
			}
		});
	}

	private void outputTeamStandings(CommandSourceStack sender, Team team) {
		sender.sendSystemMessage(Component.translatable("command.aw.teams.standings"));
		for (Map.Entry<String, Integer> standing : StreamSupport.stream(team.getFactionStandings().spliterator(), false)
				.sorted(Comparator.comparing(Map.Entry::getKey)).collect(Collectors.toList())) {
			sender.sendSystemMessage(Component.literal(standing.getKey() + ": " + standing.getValue()));
		}
	}

	@Override
	public String getName() {
		return "awteams";
	}

	@Override
	public String getUsage(CommandSourceStack sender) {
		return "command.aw.teams.usage";
	}
}
