package net.shadowmage.ancientwarfare.npc.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.gamedata.WorldData;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;

/*
 * Port notes: 1.12 CommandBase -> Brigadier, same name ("awnpcdebug") and permission level (2).
 */
public class CommandDebugAI {

	private static final SimpleCommandExceptionType NO_DATA = new SimpleCommandExceptionType(Component.literal("Couldn't find or build relevant data"));

	private int permissionLevel = 2;

	public String getName() {
		return "awnpcdebug";
	}

	public String getUsage(CommandSourceStack var1) {
		return "command.aw.npcdebug.usage";
	}

	public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(getName()).requires(source -> source.hasPermission(permissionLevel))
				.executes(context -> execute(context.getSource())));
	}

	private int execute(CommandSourceStack var1) throws CommandSyntaxException {
		AWNPCStatics.npcAIDebugMode = !AWNPCStatics.npcAIDebugMode;
		WorldData d = AWGameData.INSTANCE.getPerWorldData(var1.getLevel(), WorldData.class);
		if (d == null) {
			throw NO_DATA.create();
		}
		d.set("NpcAIDebugMode", AWNPCStatics.npcAIDebugMode);
		var1.sendSystemMessage(Component.translatable("command.aw.npcdebug.used"));
		return 1;
	}
}
