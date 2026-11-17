package net.shadowmage.ancientwarfare.core.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/*
 * Port notes: ICommandSender -> CommandSourceStack, CommandException -> CommandSyntaxException. The sub command tree
 * still works on the 1.12 style String[] args; RootCommand registers it with Brigadier.
 */
public interface ISubCommand {
	String getName();

	void execute(MinecraftServer server, CommandSourceStack sender, String[] args) throws CommandSyntaxException;

	int getMaxArgs();

	default int getMinArgs() {
		return 0;
	}

	default List<String> getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, @Nullable BlockPos targetPos) {
		return Collections.emptyList();
	}

	String getUsage(CommandSourceStack sender);

	/*
	 * 1.12 new WrongUsageException(usage)
	 */
	static CommandSyntaxException wrongUsage(String usage) {
		return new SimpleCommandExceptionType(Component.translatable(usage)).create();
	}
}
