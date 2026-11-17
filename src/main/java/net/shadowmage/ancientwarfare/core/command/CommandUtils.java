package net.shadowmage.ancientwarfare.core.command;

import com.google.common.collect.AbstractIterator;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.PacketManualReload;
import net.shadowmage.ancientwarfare.core.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/*
 * Port notes:
 * - /awutils keeps its sub commands (exportentities, exportbiomes, exportblocks, reloadmanual, exportloottables,
 *   loadChunks) and the CommandBase default permission level 4.
 * - exportblocks asked the structure module (AWStructureStatics) for the skippable / target columns. The structure
 *   module installs an IBlockExportInfo (setBlockExportInfo); without it the columns are "N".
 * - exportbiomes: biomes have no name field, temperature category, height variation, top block, biome dictionary
 *   types or class anymore. The columns are kept; they hold the translation key, the base temperature, an empty
 *   value, an empty value, the biome tags and an empty value.
 */
public class CommandUtils extends RootCommand {

	/*
	 * port addition, see the class comment
	 */
	public interface IBlockExportInfo {
		boolean isSkippable(BlockState state);

		boolean isSkippableMaterial(BlockState state);

		boolean isValidTargetBlock(BlockState state);

		boolean isValidTargetMaterial(BlockState state);
	}

	private static IBlockExportInfo blockExportInfo = new IBlockExportInfo() {
		@Override
		public boolean isSkippable(BlockState state) {
			return false;
		}

		@Override
		public boolean isSkippableMaterial(BlockState state) {
			return false;
		}

		@Override
		public boolean isValidTargetBlock(BlockState state) {
			return false;
		}

		@Override
		public boolean isValidTargetMaterial(BlockState state) {
			return false;
		}
	};

	public static void setBlockExportInfo(IBlockExportInfo info) {
		blockExportInfo = info;
	}

	/*
	 * 1.12 registered the player mover of every new CommandUtils instance on the event bus (one instance per
	 * server start); here the single mover is registered once.
	 */
	private static final ChunkLoadCommand.PlayerMover PLAYER_MOVER = new ChunkLoadCommand.PlayerMover();
	private static boolean playerMoverRegistered = false;

	public CommandUtils() {
		registerSubCommand(new EntityListCommand());
		registerSubCommand(new EntityListCommand());
		registerSubCommand(new BiomeListCommand());
		registerSubCommand(new BlockListCommand());
		registerSubCommand(new ReloadManualCommand());
		registerSubCommand(new LootTableListCommand());
		registerSubCommand(new ChunkLoadCommand());
	}

	@Override
	public String getName() {
		return "awutils";
	}

	private abstract static class ExportCommand implements ISubCommand {
		@Override
		public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
			List<String> lines = getLines();
			String fileName = args.length > 0 ? args[0] : getDefaultFileName();
			String filePath = AWCoreStatics.utilsExportPath;
			File file = new File(filePath, fileName);
			exportToFile(file, getHeader(), lines);
			notifyPlayer(sender, file);
		}

		protected abstract String getHeader();

		protected abstract String getDefaultFileName();

		protected abstract List<String> getLines();

		private static void exportToFile(File exportFile, String header, List<String> data) {
			ArrayList<String> rows = new ArrayList<>();
			rows.add(header);
			rows.addAll(data);
			FileUtils.exportToFile(exportFile, rows);
		}

		private static void notifyPlayer(CommandSourceStack sender, File exportFile) {
			sender.sendSystemMessage(Component.literal("File exported to " + exportFile.getAbsoluteFile()));
		}

		@Override
		public int getMaxArgs() {
			return 1;
		}
	}

	private static class EntityListCommand extends ExportCommand {
		@Override
		protected String getHeader() {
			return "Registry Name,Entity Name,Entity Class";
		}

		@Override
		protected String getDefaultFileName() {
			return "entitylist.csv";
		}

		@Override
		protected List<String> getLines() {
			return BuiltInRegistries.ENTITY_TYPE.stream()
					.map(e -> String.join(",", BuiltInRegistries.ENTITY_TYPE.getKey(e).toString(), e.getDescriptionId(), e.getBaseClass().toString()))
					.sorted(Comparator.naturalOrder()).collect(Collectors.toList());
		}

		@Override
		public String getName() {
			return "exportentities";
		}

		@Override
		public String getUsage(CommandSourceStack sender) {
			return getName() + " [fileName - defaults to \"entitylist.csv\"]";
		}
	}

	private static class BiomeListCommand extends ExportCommand {
		@Override
		protected String getHeader() {
			return "Registry Name,Biome Name,Temperature Category,High Humidity,Height Variation,Top Block,Biome Types,Biome Class";
		}

		@Override
		protected String getDefaultFileName() {
			return "biomelist.csv";
		}

		@Override
		protected List<String> getLines() {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server == null) {
				return new ArrayList<>();
			}
			Registry<Biome> biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
			return biomes.holders()
					.map(b -> String.join(",", b.key().location().toString(), getBiomeName(b.key().location()), Float.toString(b.value().getBaseTemperature())
							, Boolean.toString(b.value().getModifiedClimateSettings().downfall() > 0.85F), "", ""
							, getBiomeTypes(b), ""))
					.sorted(Comparator.naturalOrder()).collect(Collectors.toList());
		}

		private String getBiomeName(ResourceLocation registryName) {
			return "biome." + registryName.getNamespace() + "." + registryName.getPath();
		}

		private String getBiomeTypes(Holder<Biome> biome) {
			return biome.tags().map(tag -> tag.location().toString()).sorted().collect(Collectors.joining("|"));
		}

		@Override
		public String getName() {
			return "exportbiomes";
		}

		@Override
		public String getUsage(CommandSourceStack sender) {
			return getName() + " [fileName - defaults to \"biomelist.csv\"]";
		}
	}

	private static class BlockListCommand extends ExportCommand {
		@Override
		protected String getHeader() {
			return "Registry Name,Block Name,Skippable,Skippable Material,Target,Target Material";
		}

		@Override
		protected String getDefaultFileName() {
			return "blocklist.csv";
		}

		@Override
		protected List<String> getLines() {
			return BuiltInRegistries.BLOCK.stream()
					.map(b -> String.join(",", BuiltInRegistries.BLOCK.getKey(b).toString(), b.getName().getString(),
							blockExportInfo.isSkippable(b.defaultBlockState()) ? "Y" : "N",
							blockExportInfo.isSkippableMaterial(b.defaultBlockState()) ? "Y" : "N",
							blockExportInfo.isValidTargetBlock(b.defaultBlockState()) ? "Y" : "N",
							blockExportInfo.isValidTargetMaterial(b.defaultBlockState()) ? "Y" : "N"
					)).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
		}

		@Override
		public String getName() {
			return "exportblocks";
		}

		@Override
		public String getUsage(CommandSourceStack sender) {
			return getName() + " [fileName - defaults to \"blocklist.csv\"]";
		}
	}

	private static class ReloadManualCommand implements ISubCommand {
		@Override
		public String getName() {
			return "reloadmanual";
		}

		@Override
		public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) {
			Entity senderEntity = sender.getEntity();
			if (senderEntity instanceof Player) {
				NetworkHandler.sendToPlayer((ServerPlayer) senderEntity, new PacketManualReload());
			}
		}

		@Override
		public int getMaxArgs() {
			return 0;
		}

		@Override
		public String getUsage(CommandSourceStack sender) {
			return getName();
		}
	}

	private static class LootTableListCommand extends ExportCommand {
		@Override
		protected String getHeader() {
			return "Registry Name";
		}

		@Override
		protected String getDefaultFileName() {
			return "loottablelist.csv";
		}

		@Override
		protected List<String> getLines() {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server == null) {
				return new ArrayList<>();
			}
			return server.reloadableRegistries().getKeys(Registries.LOOT_TABLE).stream().map(ResourceLocation::toString).sorted().collect(Collectors.toList());
		}

		@Override
		public String getName() {
			return "exportloottables";
		}

		@Override
		public String getUsage(CommandSourceStack sender) {
			return getName() + " [fileName - defaults to \"loottablelist.csv\"]";
		}
	}

	private static class ChunkLoadCommand implements ISubCommand {
		private PlayerMover playerMover = PLAYER_MOVER;

		private ChunkLoadCommand() {
			if (!playerMoverRegistered) {
				playerMoverRegistered = true;
				NeoForge.EVENT_BUS.addListener(playerMover::serverTick);
			}
		}

		@Override
		public String getName() {
			return "loadChunks";
		}

		@Override
		public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) throws CommandSyntaxException {
			if (args.length < 1) {
				throw ISubCommand.wrongUsage(getUsage(sender));
			}
			if (!(sender.getEntity() instanceof ServerPlayer)) {
				return;
			}

			int chunkLoadRadius = server.getPlayerList().getViewDistance();
			int range;
			try {
				range = Integer.parseInt(args[0]);
			}
			catch (NumberFormatException e) {
				throw ISubCommand.wrongUsage(getUsage(sender));
			}
			playerMover.startMoving((ServerPlayer) sender.getEntity(), sender.getLevel(), chunkLoadRadius, range);
		}

		@Override
		public int getMaxArgs() {
			return 1;
		}

		@Override
		public String getUsage(CommandSourceStack sender) {
			return getName() + " <diameterInChunks>";
		}

		private static class PlayerMover {
			private ServerPlayer player;
			private int chunkLoadRadius;
			private int range;
			private BlockPos originalPosition;
			private ChunkPos originalChunkPos;
			private boolean finishedMoving = true;
			private Iterator<ChunkPos> iterator;
			private int timeout = 0;

			public void serverTick(ServerTickEvent.Post evt) {
				if (timeout <= 0) {
					movePlayer();
					timeout = 200;
				} else {
					timeout--;
				}
			}

			private void startMoving(ServerPlayer player, Level world, int chunkLoadRadius, int range) {
				this.player = player;
				originalPosition = player.blockPosition();
				originalChunkPos = new ChunkPos(originalPosition);
				this.chunkLoadRadius = chunkLoadRadius;
				this.range = range;
				finishedMoving = false;
				iterator = getAllChunkPosStops();
			}

			private Iterator<ChunkPos> getAllChunkPosStops() {
				return new AbstractIterator<ChunkPos>() {
					private boolean first = true;
					private int currentX;
					private int currentZ;

					@Override
					protected ChunkPos computeNext() {
						if (first) {
							currentX = getInitialX();
							currentZ = getInitialZ();
							first = false;
						} else if (currentX + chunkLoadRadius >= originalChunkPos.x + range && currentZ + chunkLoadRadius >= originalChunkPos.z + range) {
							return endOfData();
						} else {
							if (currentX + chunkLoadRadius < originalChunkPos.x + range) {
								currentX += 2 * chunkLoadRadius;
							} else if (currentZ + chunkLoadRadius < originalChunkPos.z + range) {
								currentX = getInitialX();
								currentZ += 2 * chunkLoadRadius;
							}
						}
						return new ChunkPos(currentX, currentZ);
					}

					private int getInitialZ() {
						return originalChunkPos.z - range + chunkLoadRadius;
					}

					private int getInitialX() {
						return originalChunkPos.x - range + chunkLoadRadius;
					}
				};
			}

			private void movePlayer() {
				if (!finishedMoving) {
					if (!iterator.hasNext()) {
						player.connection.teleport(originalPosition.getX(), originalPosition.getY(), originalPosition.getZ(), player.getYRot(), player.getXRot());
						finishedMoving = true;
						return;
					}

					ChunkPos chunkPos = iterator.next();
					player.connection.teleport(chunkPos.getMinBlockX() + 8d, 255, chunkPos.getMinBlockZ() + 8d, player.getYRot(), player.getXRot());
				}
			}
		}
	}
}
