package net.shadowmage.ancientwarfare.structure.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.world.level.Level;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.command.ISubCommand;
import net.shadowmage.ancientwarfare.core.command.RootCommand;
import net.shadowmage.ancientwarfare.core.command.SimpleSubCommand;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureMap;
import net.shadowmage.ancientwarfare.structure.item.ItemStructureScanner;
import net.shadowmage.ancientwarfare.structure.item.ItemStructureSettings;
import net.shadowmage.ancientwarfare.structure.network.PacketShowBoundingBoxes;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBuilder;
import net.shadowmage.ancientwarfare.structure.template.load.TemplateLoader;
import net.shadowmage.ancientwarfare.structure.tile.ScannerTracker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CommandStructure extends RootCommand {
	//1.12 CommandBase.getListOfStringsMatchingLastWord
	private static List<String> matching(String[] args, String... options) {
		String last = args.length == 0 ? "" : args[args.length - 1];
		List<String> ret = new java.util.ArrayList<>();
		for (String option : options) {
			if (option.regionMatches(true, 0, last, 0, last.length())) {
				ret.add(option);
			}
		}
		return ret;
	}

	public CommandStructure() {
		registerSubCommand(new DeleteCommand());
		registerSubCommand(new BuildCommand());
		registerSubCommand(new SaveCommand());
		registerSubCommand(new SimpleSubCommand("reload",
				(server, sender, args) -> {
					net.shadowmage.ancientwarfare.structure.template.WorldGenStructureManager.INSTANCE.clearCachedTemplates();
					net.shadowmage.ancientwarfare.structure.worldgen.TerritoryManager.clearTerritoryCache();
					net.shadowmage.ancientwarfare.structure.template.WorldGenStructureManager.INSTANCE.loadBiomeList(); //reset biome to template cache
					TemplateLoader.INSTANCE.reloadAll();
					net.shadowmage.ancientwarfare.structure.template.load.TemplateLoader.INSTANCE.registerTownTerritoryBiomes(); //port: 1.12 did this while parsing the town templates
					sender.sendSystemMessage(Component.translatable("command.aw.structure.reloaded"));
				}));
		registerSubCommand(new ReexportCommand());
		registerSubCommand(new SimpleSubCommand("scannerTp", (server, sender, args) -> {
			if (sender.getEntity() instanceof Player) {
				Tuple<net.minecraft.resources.ResourceKey<Level>, BlockPos> pos = ScannerTracker.getScannerPosByName(args[0]);
				ScannerTracker.teleportAboveScannerBlock((Player) sender.getEntity(), pos);
			}

		}) {
			@Override
	public int getMaxArgs() {
				return 1;
			}

			@Override
	public List<String> getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos targetPos) {
				return matching(args, ScannerTracker.getTrackedScannerNames().toArray(new String[0]));
			}

			@Override
	public String getUsage(CommandSourceStack sender) {
				return getName() + " <scannerTemplateName>";
			}
		});
		registerSubCommand(new SimpleSubCommand("name", (server, sender, args) -> {
			Optional<StructureEntry> structure = StructureMap.get(sender.getLevel())
					.getStructureAt(sender.getLevel(), BlockPos.containing(sender.getPosition()));

			sender.sendSystemMessage(structure.map(structureEntry -> Component.translatable("command.aw.structure.name", structureEntry.getName()))
					.orElseGet(() -> Component.translatable("command.aw.structure.no_structure")));
		}));
		registerSubCommand(new SimpleSubCommand("showBoundingBoxes", (server, sender, args) -> {
			if (sender.getEntity() instanceof ServerPlayer) {
				ServerPlayer player = (ServerPlayer) sender.getEntity();
				NetworkHandler.sendToPlayer(player, new PacketShowBoundingBoxes(true));
			}
		}));
		registerSubCommand(new SimpleSubCommand("hideBoundingBoxes", (server, sender, args) -> {
			if (sender.getEntity() instanceof ServerPlayer) {
				ServerPlayer player = (ServerPlayer) sender.getEntity();
				NetworkHandler.sendToPlayer(player, new PacketShowBoundingBoxes(false));
			}
		}));
		registerSubCommand(new StatsCommand());
	}

	@Override
	public String getName() {
		return "awstructure";
	}

	@Override
	public String getUsage(CommandSourceStack var1) {
		return "command.aw.structure.usage";
	}

	private static class SaveCommand implements ISubCommand {
		@Override
	public String getName() {
			return "save";
		}

		@Override
	public void execute(MinecraftServer server, CommandSourceStack sender, String[] subArgs) {
			if (sender.getEntity() instanceof LivingEntity) {
				ItemStack stack = ((LivingEntity) sender.getEntity()).getMainHandItem();
				if (!stack.isEmpty()) {
					ItemStructureSettings settings = ItemStructureSettings.getSettingsFor(stack);
					if (settings.hasPos1() && settings.hasPos2() && settings.hasBuildKey() && (settings.hasName() || subArgs.length > 0)) {
						scanStructure(sender, subArgs, stack, settings);
					} else {
						sender.sendSystemMessage(Component.translatable("command.aw.structure.incomplete_data"));
					}
				}
			}
		}

		private void scanStructure(CommandSourceStack sender, String[] subArgs, ItemStack stack, ItemStructureSettings settings) {
			String name = settings.hasName() ? settings.name() : subArgs[0];
			ItemStructureScanner.setStructureName(stack, name);
			if (ItemStructureScanner.scanStructure(sender.getLevel(), stack)) {
				sender.sendSystemMessage(Component.translatable("command.aw.structure.exported", subArgs[0]));
			}
		}

		@Override
	public int getMaxArgs() {
			return 1;
		}

		@Override
	public String getUsage(CommandSourceStack sender) {
			return getName() + " [templateName]";
		}
	}

	private static class BuildCommand implements ISubCommand {
		@Override
	public String getName() {
			return "build";
		}

		@Override
	public void execute(MinecraftServer server, CommandSourceStack sender, String[] subArgs) throws CommandSyntaxException {
			if (subArgs.length < 4) {
				throw ISubCommand.wrongUsage(getUsage(sender));
			}

			int x = Integer.parseInt(subArgs[1]);
			int y = Integer.parseInt(subArgs[2]);
			int z = Integer.parseInt(subArgs[3]);
			Direction face = Direction.SOUTH;
			if (subArgs.length > 4) {
				Direction faceArg = Direction.byName(subArgs[4]);
				if (faceArg != null) {
					face = faceArg;
				}
			}
			Optional<StructureTemplate> template = StructureTemplateManager.getTemplate(subArgs[0]);
			if (template.isPresent()) {
				StructureBuilder builder = new StructureBuilder(sender.getLevel(), template.get(), face, new BlockPos(x, y, z));
				//budgeted_placement (improvement, not baseline): queued, the message comes when the structure is complete;
				//with the switch off instantConstruction and the message at once
				String name = subArgs[0];
				net.shadowmage.ancientwarfare.structure.template.build.StructurePlacementQueue.INSTANCE.build(builder, null,
						() -> sender.sendSystemMessage(Component.translatable("command.aw.structure.built", name, x, y, z)));
			} else {
				sender.sendSystemMessage(Component.translatable("command.aw.structure.not_found", subArgs[0]));
			}
		}

		@Override
	public int getMaxArgs() {
			return 5;
		}

		@Override
	public String getUsage(CommandSourceStack sender) {
			return getName() + " <templateName> <x> <y> <z> [north:east:south:west]";
		}

		@Override
	public List<String> getTabCompletions(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos targetPos) {
			return args.length > 4 ? matching(args, "north", "east", "south", "west") : Collections.emptyList();
		}
	}

	private static class DeleteCommand implements ISubCommand {
		@Override
	public String getName() {
			return "delete";
		}

		@Override
	public void execute(MinecraftServer server, CommandSourceStack sender, String[] subArgs) throws CommandSyntaxException {
			if (subArgs.length < 1) {
				throw ISubCommand.wrongUsage(getUsage(sender));
			}
			String name = subArgs[0];
			boolean flag = StructureTemplateManager.removeTemplate(name);
			if (flag)//check if var2.len>=3, pull string of end...if string==true, try delete template file for name
			{
				Component txt = Component.translatable("command.aw.structure.template_removed", name);
				sender.sendSystemMessage(txt);
				if (subArgs.length > 1) {
					boolean shouldDelete = subArgs[1].equalsIgnoreCase("true");
					if (shouldDelete) {
						if (deleteTemplateFile(name)) {
							txt = Component.translatable("command.aw.structure.file_deleted", name);
						} else {
							txt = Component.translatable("command.aw.structure.file_not_found", name);
						}
						sender.sendSystemMessage(txt);
					}
				}
			} else//send template not found message
			{
				sender.sendSystemMessage(Component.translatable("command.aw.structure.not_found", name));
			}
		}

		private boolean deleteTemplateFile(String name) {
			String path = TemplateLoader.INCLUDE_DIRECTORY + name + "." + AWStructureStatics.templateExtension;
			File file = new File(path);
			if (file.exists()) {
				try {
					Files.delete(file.toPath());
				}
				catch (IOException e) {
					AncientWarfareStructure.LOG.error("Unable to delete file ", e);
				}
				return true;
			}
			return false;
		}

		@Override
	public int getMaxArgs() {
			return 2;
		}

		@Override
	public String getUsage(CommandSourceStack sender) {
			return getName() + " <templateName> [true to remove the aws file]";
		}
	}

	private static class ReexportCommand implements ISubCommand {
		@Override
	public String getName() {
			return "scannersReexport";
		}

		@Override
	public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
			if (sender.getEntity() instanceof Player) {
				boolean reloadMainSettings = false;
				if (args.length == 1) {
					reloadMainSettings = Boolean.parseBoolean(args[0]);
				}
				ScannerTracker.reexportAll((Player) sender.getEntity(), reloadMainSettings);
			}
		}

		@Override
	public int getMaxArgs() {
			return 1;
		}

		@Override
	public String getUsage(CommandSourceStack sender) {
			return getName() + " [true to reload main settings]";
		}
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
}
