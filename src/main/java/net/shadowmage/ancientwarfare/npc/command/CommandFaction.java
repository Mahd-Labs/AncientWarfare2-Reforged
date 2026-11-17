package net.shadowmage.ancientwarfare.npc.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.npc.faction.FactionTracker;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;

/*
 * Port notes: 1.12 CommandBase -> Brigadier, same name ("awfaction"), sub command ("get <player>") and permission
 * level (2). As in 1.12 any other first argument is accepted and does nothing.
 */
public class CommandFaction {

	private static final SimpleCommandExceptionType WRONG_USAGE = new SimpleCommandExceptionType(Component.translatable("command.aw.faction.usage"));

	public int getRequiredPermissionLevel() {
		return 2;
	}

	public String getName() {
		return "awfaction";
	}

	public String getUsage(CommandSourceStack var1) {
		return "command.aw.faction.usage";
	}

	public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(getName()).requires(source -> source.hasPermission(getRequiredPermissionLevel()))
				.executes(context -> {
					throw WRONG_USAGE.create();
				})
				.then(Commands.argument("cmd", StringArgumentType.word())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[] {"get"}, builder))
						.executes(context -> {
							throw WRONG_USAGE.create();
						})
						.then(Commands.argument("playerName", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(context.getSource().getOnlinePlayerNames(), builder))
								.executes(context -> {
									String cmd = StringArgumentType.getString(context, "cmd");
									String playerName = StringArgumentType.getString(context, "playerName");
									if (cmd.equalsIgnoreCase("get")) {
										showFactionStandings(context.getSource(), playerName);
									}
									return 1;
								}))));
	}

	private void showFactionStandings(CommandSourceStack var1, String playerName) {
		Level world = var1.getLevel();
		var1.sendSystemMessage(Component.translatable("command.aw.faction.status.player", playerName));
		for (String faction : FactionRegistry.getFactionNames()) {
			int standing = FactionTracker.INSTANCE.getStandingFor(world, playerName, faction);
			var1.sendSystemMessage(Component.translatable("command.aw.faction.status.value", faction, standing));
		}
	}
}
