package net.shadowmage.ancientwarfare.core.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

public class SimpleSubCommand implements ISubCommand {
	private String name;
	private ISubCommandExecutor executor;

	public SimpleSubCommand(String name, ISubCommandExecutor executor) {
		this.name = name;
		this.executor = executor;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) throws CommandSyntaxException {
		executor.execute(server, sender, args);
	}

	public interface ISubCommandExecutor {
		void execute(MinecraftServer server, CommandSourceStack sender, String[] args) throws CommandSyntaxException;
	}

	@Override
	public int getMaxArgs() {
		return 0;
	}

	@Override
	public int getMinArgs() {
		return getMaxArgs();
	}

	@Override
	public String getUsage(CommandSourceStack sender) {
		return getName();
	}
}
