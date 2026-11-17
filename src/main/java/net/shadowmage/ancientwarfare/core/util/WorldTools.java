package net.shadowmage.ancientwarfare.core.util;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.interfaces.IInteractableTile;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class WorldTools {
	private WorldTools() {}

	/*
	 * SERVER ONLY
	 */
	public static List<BlockEntity> getTileEntitiesInArea(Level world, int x1, int y1, int z1, int x2, int y2, int z2) {
		if (world instanceof ServerLevel) {
			List<BlockEntity> tileEntities = Lists.newArrayList();
			for (int x = (x1 >> 4); x <= (x2 >> 4); x++) {
				for (int z = (z1 >> 4); z <= (z2 >> 4); z++) {
					addValidTilesInChunkArea(world, new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2), tileEntities, x, z);
				}
			}
			return tileEntities;
		}
		return Collections.emptyList();
	}

	private static void addValidTilesInChunkArea(Level world, BlockPos min, BlockPos max, List<BlockEntity> tileEntities, int x, int z) {
		//port fix: only chunks that are loaded. 1.12 getChunkFromChunkCoords and 1.21 getChunk load (or generate) the chunk
		//on the spot; in 1.21 that can happen while chunks are being unloaded or saved (a block entity looking for its
		//neighbours in setRemoved), which stalls or fails the save. A chunk that is not loaded has no reachable tiles.
		LevelChunk chunk = world instanceof ServerLevel server ? server.getChunkSource().getChunkNow(x, z) : world.getChunk(x, z);
		if (chunk == null) {
			return;
		}
		for (BlockEntity tile : chunk.getBlockEntities().values()) {
			if (!tile.isRemoved() && isTileInArea(min, max, tile)) {
				tileEntities.add(tile);
			}
		}
	}

	private static boolean isTileInArea(BlockPos min, BlockPos max, BlockEntity tile) {
		BlockPos tilePos = tile.getBlockPos();
		return tilePos.getX() >= min.getX() && tilePos.getY() >= min.getY() && tilePos.getZ() >= min.getZ() && tilePos.getX() <= max.getX() && tilePos.getY() <= max.getY() && tilePos.getZ() <= max.getZ();
	}

	public static Optional<IItemHandler> getItemHandlerFromTile(BlockGetter world, BlockPos pos, @Nullable Direction side) {
		return getTile(world, pos, BlockEntity.class).map(t -> InventoryTools.getItemHandlerFrom(t, side)).orElse(Optional.empty());
	}

	public static boolean sendClientEventToTile(BlockGetter world, BlockPos pos, int id, int param) {
		return getTile(world, pos).map(t -> t.triggerEvent(id, param)).orElse(false);
	}

	public static Optional<BlockEntity> getTile(BlockGetter world, BlockPos pos) {
		return getTile(world, pos, BlockEntity.class);
	}

	public static <T> Optional<T> getTile(@Nullable BlockGetter world, @Nullable BlockPos pos, Class<T> teClass) {
		if (world == null || pos == null) {
			return Optional.empty();
		}

		//port fix: never load a chunk for a tile lookup (see addValidTilesInChunkArea)
		if (world instanceof ServerLevel server && !isChunkLoaded(server, pos)) {
			return Optional.empty();
		}
		BlockEntity te = world.getBlockEntity(pos);

		if (teClass.isInstance(te)) {
			return Optional.of(teClass.cast(te));
		}

		return Optional.empty();
	}

	public static boolean clickInteractableTileWithHand(Level world, BlockPos pos, Player player, InteractionHand hand) {
		return getTile(world, pos, IInteractableTile.class).map(t -> t.onBlockClicked(player, hand)).orElse(false);
	}

	/*
	 * Port note: dimensions are ResourceKey<Level> (1.12: int ids)
	 */
	private static final Map<ResourceKey<Level>, Predicate<Level>> DIMENSION_DAY_TIMES = new HashMap<>();

	public static void registerDimensionDaytimeLogic(ResourceKey<Level> dimension, Predicate<Level> isDayTime) {
		DIMENSION_DAY_TIMES.put(dimension, isDayTime);
	}

	public static boolean isDaytimeInDimension(Level world) {
		return DIMENSION_DAY_TIMES.getOrDefault(world.dimension(), Level::isDay).test(world);
	}

	public static boolean hasItemHandler(Level world, BlockPos pos) {
		return getTile(world, pos, BlockEntity.class).map(InventoryTools::isInventory).orElse(false);
	}

	/*
	 * Port note: data classes extend AWGameData.AWSavedData semantics (constructor with a single String parameter and
	 * readFromNBT / writeToNBT), see AWGameData. Empty on the client.
	 */
	public static <T extends SavedData> Optional<T> getWorldSavedData(Level world, Class<T> dataClazz, String name, boolean perWorldStorage) {
		if (!(world instanceof ServerLevel)) {
			return Optional.empty();
		}
		return Optional.of(AWGameData.INSTANCE.getNamedData((ServerLevel) world, dataClazz, name, perWorldStorage));
	}

	/*
	 * Port note: 1.12 wrote the biome id into the chunk's 16x16 biome array for one column. Biomes are stored in
	 * 4x4x4 cells now; the whole column of cells containing pos is set (same as /fillbiome on that column).
	 */
	public static void changeBiome(Level world, BlockPos pos, Holder<Biome> biome) {
		if (world instanceof ServerLevel) {
			ServerLevel level = (ServerLevel) world;
			FillBiomeCommand.fill(level, new BlockPos(pos.getX(), level.getMinBuildHeight(), pos.getZ()), new BlockPos(pos.getX(), level.getMaxBuildHeight() - 1, pos.getZ()), biome);
		}
	}

	/* port addition: true when the chunk of the position is loaded right now; never loads it */
	public static boolean isChunkLoaded(Level world, BlockPos pos) {
		if (world instanceof ServerLevel server) {
			return server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
		}
		return world.hasChunkAt(pos);
	}
}
