package net.shadowmage.ancientwarfare.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/*
 * Port notes: 1.12 CommandBase subclass. register(dispatcher) adds "/<name> [args...]" to Brigadier (one greedy string
 * argument that is split on spaces and handed to the 1.12 style sub command tree, with its tab completions as
 * suggestions). The permission level is the CommandBase default of 4 unless overridden.
 */
public abstract class RootCommand {
	private final ParentCommand delegate = new ParentCommand(true) {
		@Override
		public String getName() {
			return RootCommand.this.getName();
		}
	};

	public abstract String getName();

	public int getRequiredPermissionLevel() {
		return 4;
	}

	public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(getName()).requires(source -> source.hasPermission(getRequiredPermissionLevel()))
				.executes(context -> run(context, new String[0]))
				.then(Commands.argument("args", StringArgumentType.greedyString())
						.suggests(this::suggest)
						.executes(context -> run(context, StringArgumentType.getString(context, "args").trim().split(" +")))));
	}

	private int run(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
		execute(context.getSource().getServer(), context.getSource(), args);
		return 1;
	}

	private CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		String remaining = builder.getRemaining();
		//the last (possibly empty) word is the one being completed
		String[] args = remaining.split(" ", -1);
		int lastWordStart = builder.getStart() + remaining.lastIndexOf(' ') + 1;
		SuggestionsBuilder wordBuilder = builder.createOffset(lastWordStart);
		for (String completion : getTabCompletions(context.getSource().getServer(), context.getSource(), args, null)) {
			wordBuilder.suggest(completion);
		}
		return wordBuilder.buildFuture();
	}

	public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) throws CommandSyntaxException {
		delegate.execute(server, sender, args);
	}

	public List<String> getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, @Nullable BlockPos targetPos) {
		return delegate.getTabCompletions(server, sender, args, targetPos);
	}

	protected void registerSubCommand(ISubCommand subCommand) {
		delegate.registerSubCommand(subCommand);
	}

	public String getUsage(CommandSourceStack sender) {
		return delegate.getUsage(sender);
	}
}
