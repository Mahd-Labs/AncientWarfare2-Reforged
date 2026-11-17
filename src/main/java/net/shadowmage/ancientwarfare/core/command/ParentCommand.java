package net.shadowmage.ancientwarfare.core.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public abstract class ParentCommand implements ISubCommand {
	private Map<String, ISubCommand> subCommands = new TreeMap<>();
	private boolean isRoot;

	protected ParentCommand() {
		this(false);
	}

	ParentCommand(boolean isRoot) {
		this.isRoot = isRoot;
	}

	protected void registerSubCommand(ISubCommand subCommand) {
		subCommands.put(subCommand.getName().toLowerCase(Locale.ENGLISH), subCommand);
	}

	@Override
	public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) throws CommandSyntaxException {
		if (args.length == 0 || !subCommands.containsKey(args[0].toLowerCase())) {
			throw ISubCommand.wrongUsage(getUsage(sender));
		}

		String[] subArgs = getSubArgs(args);
		ISubCommand subCommand = getSubCommand(args[0]);

		if (subArgs.length < subCommand.getMinArgs() || subArgs.length > subCommand.getMaxArgs()) {
			throw ISubCommand.wrongUsage(getUsage(sender));
		}

		subCommand.execute(server, sender, subArgs);
	}

	public String getUsage(CommandSourceStack sender) {
		return (isRoot ? "/" : "")
				+ getName() + " < " + subCommands.values().stream().map(sc -> sc.getUsage(sender)).collect(Collectors.joining(" | ")) + " >";
	}

	private ISubCommand getSubCommand(String arg) {
		return subCommands.get(arg.toLowerCase());
	}

	private String[] getSubArgs(String[] args) {
		String[] subArgs = new String[args.length - 1];
		System.arraycopy(args, 1, subArgs, 0, args.length - 1);
		return subArgs;
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, @Nullable BlockPos targetPos) {
		if (args.length == 1) {
			return getListOfSubcommandsMatchingString(args[0]);
		} else if (args.length > 1) {
			String[] subArgs = getSubArgs(args);
			ISubCommand subCommand = getSubCommand(args[0]);
			return subCommand == null ? Collections.emptyList() : subCommand.getTabCompletions(server, sender, subArgs, targetPos);
		}
		return Collections.emptyList();
	}

	private List<String> getListOfSubcommandsMatchingString(String commandName) {
		return subCommands.keySet().stream().filter(scName -> scName.startsWith(commandName)).collect(Collectors.toList());
	}

	@Override
	public int getMaxArgs() {
		return 1 + subCommands.values().stream().map(ISubCommand::getMaxArgs).max(Comparator.naturalOrder()).orElse(0);
	}
}
