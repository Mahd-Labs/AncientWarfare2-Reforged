package net.shadowmage.ancientwarfare.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.registry.ResearchRegistry;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchTracker;

/*
 * Port notes: Brigadier command with the 1.12 shape: /awresearch (add|remove|fill|clear) <playerName> [goal],
 * permission level 2. Player names and research goal names are suggested like the 1.12 tab completion. Like 1.12 the
 * command prints nothing on success (the result is written to the debug log).
 */
public class CommandResearch {

	private static final String COMMAND_AW_RESEARCH_USAGE = "command.aw.research.usage";
	private static final SimpleCommandExceptionType WRONG_USAGE = new SimpleCommandExceptionType(Component.translatable(COMMAND_AW_RESEARCH_USAGE));

	public int getRequiredPermissionLevel() {
		return 2;
	}

	public String getName() {
		return "awresearch";
	}

	public String getUsage(CommandSourceStack sender) {
		return COMMAND_AW_RESEARCH_USAGE;
	}

	public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(getName()).requires(source -> source.hasPermission(getRequiredPermissionLevel()))
				.executes(context -> {
					throw WRONG_USAGE.create();
				})
				.then(Commands.literal("add").then(playerName().then(goal().executes(context -> execute(context, "add")))))
				.then(Commands.literal("remove").then(playerName().then(goal().executes(context -> execute(context, "remove")))))
				.then(Commands.literal("fill").then(playerName().executes(context -> execute(context, "fill"))))
				.then(Commands.literal("clear").then(playerName().executes(context -> execute(context, "clear")))));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> playerName() {
		return Commands.argument("playerName", StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(context.getSource().getOnlinePlayerNames(), builder));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> goal() {
		return Commands.argument("goal", StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(ResearchRegistry.getAllResearchGoals().stream().map(ResearchGoal::getName), builder));
	}

	private int execute(CommandContext<CommandSourceStack> context, String action) throws CommandSyntaxException {
		CommandSourceStack sender = context.getSource();
		String playerName = StringArgumentType.getString(context, "playerName");
		if ("add".equals(action)) {
			String goal = StringArgumentType.getString(context, "goal");
			if (!ResearchRegistry.researchExists(goal)) {
				throw WRONG_USAGE.create();
			}
			ResearchTracker.INSTANCE.addResearchFromNotes(sender.getLevel(), playerName, goal);
		} else if ("remove".equals(action)) {
			String goal = StringArgumentType.getString(context, "goal");
			if (!ResearchRegistry.researchExists(goal)) {
				throw WRONG_USAGE.create();
			}
			ResearchTracker.INSTANCE.removeResearch(sender.getLevel(), playerName, goal);
		} else if ("fill".equals(action)) {
			ResearchTracker.INSTANCE.fillResearch(sender.getLevel(), playerName);
		} else if ("clear".equals(action)) {
			ResearchTracker.INSTANCE.clearResearch(sender.getLevel(), playerName);
		} else {
			throw WRONG_USAGE.create();
		}
		AncientWarfare.LOG.debug("awresearch {} {}: completed research now {}", action, playerName, ResearchTracker.INSTANCE.getCompletedResearchFor(sender.getLevel(), playerName));
		return 1;
	}
}
