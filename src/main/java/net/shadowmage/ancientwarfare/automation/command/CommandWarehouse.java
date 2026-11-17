package net.shadowmage.ancientwarfare.automation.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.WarehouseDebugger;

/*
 * Port notes: 1.12 CommandBase -> Brigadier. Same command name and sub commands:
 * /awwarehouse startdebug <x> <y> <z> OR /awwarehouse stopdebug <x> <y> <z> (relative coordinates allowed, like the
 * 1.12 parseBlockPos), permission level 4 (the 1.12 CommandBase default).
 */
public class CommandWarehouse {

	public String getName() {
		return "awwarehouse";
	}

	public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(getName()).requires(source -> source.hasPermission(4))
				.then(Commands.literal("startdebug").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> {
					WarehouseDebugger.startTrackingWarehouse(ctx.getSource().getLevel(), BlockPosArgument.getBlockPos(ctx, "pos"));
					return 1;
				})))
				.then(Commands.literal("stopdebug").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> {
					WarehouseDebugger.stopTrackingWarehouse(ctx.getSource().getLevel(), BlockPosArgument.getBlockPos(ctx, "pos"));
					return 1;
				}))));
	}
}
