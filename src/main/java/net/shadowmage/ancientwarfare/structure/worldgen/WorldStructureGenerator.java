package net.shadowmage.ancientwarfare.structure.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureMap;
import net.shadowmage.ancientwarfare.structure.gamedata.TownMap;
import net.shadowmage.ancientwarfare.structure.registry.TerritorySettingRegistry;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.WorldGenStructureManager;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBuilderWorldGen;
import net.shadowmage.ancientwarfare.structure.worldgen.stats.PlacementRejectionReason;
import net.shadowmage.ancientwarfare.structure.worldgen.stats.WorldGenStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;

/*
 * Port notes: 1.12 IWorldGenerator. generate() is called by WorldGenTickHandler for every newly generated chunk
 * (ChunkEvent.Load with isNewChunk, one tick later) instead of by the chunk generator; generateAt and the rest are
 * the 1.12 code. Static helpers: getTargetY (the 1.12 scan started at world.getActualHeight() and stopped above y 0;
 * it covers the whole build height now), sprinkleSnow and getStepNumber.
 */
public class WorldStructureGenerator {

	public static final WorldStructureGenerator INSTANCE = new WorldStructureGenerator();

	private static final int MAX_DISTANCE_WITHIN_CLUSTER = 150;

	private final Random rng;
	private boolean debugTerritoryBorders = false;
	//diagnostics (debug log of slow attempts): the template and result of the last attempt
	private String lastAttemptInfo = "";

	private WorldStructureGenerator() {
		rng = new Random();
	}

	public void generate(Random random, int chunkX, int chunkZ, Level world) {
		TerritoryManager.getTerritory(chunkX, chunkZ, world).ifPresent(territory -> {
			if (debugTerritoryBorders) {
				generateTerritoryBorders(chunkX, chunkZ, world, territory.getTerritoryId());
			}
			BlockPos cc = world.getSharedSpawnPos();
			double distSq = cc.distToCenterSqr((double) chunkX * 16, cc.getY(), (double) chunkZ * 16);
			if (AWStructureStatics.withinProtectionRange(distSq)) {
				return;
			}
			if (rng.nextFloat() < (AWStructureStatics.randomGenerationChance
					* TerritorySettingRegistry.getTerritorySettings(territory.getTerritoryName()).getStructureGenerationChanceMultiplier())) {
				WorldGenTickHandler.INSTANCE.addChunkForGeneration(world, chunkX, chunkZ);
			}
		});
	}

	/*
	 * 1.12 package private; public for the GameTests (the world generator entry point for one chunk)
	 */
	public String getLastAttemptInfo() {
		return lastAttemptInfo;
	}

	public void generateAt(int chunkX, int chunkZ, Level world) {
		lastAttemptInfo = "no template";
		long t1 = System.currentTimeMillis();
		long seed = (((long) chunkX) << 32) | (((long) chunkZ) & 0xffffffffL);
		rng.setSeed(seed);
		int x = chunkX * 16 + rng.nextInt(16);
		int z = chunkZ * 16 + rng.nextInt(16);
		int y = getTargetY(world, x, z, false) + 1;
		if (y <= world.getMinBuildHeight()) { //1.12: y <= 0
			return;
		}

		TerritoryManager.getTerritory(chunkX, chunkZ, world).ifPresent(territory -> {
			Direction face = Direction.from2DDataValue(rng.nextInt(4));
			Optional<StructureTemplate> t = WorldGenStructureManager.INSTANCE.selectTemplateForGeneration(world, rng, x, y, z, face, territory);
			AncientWarfareStructure.LOG.debug("Template selection took: {} ms.", System.currentTimeMillis() - t1);
			if (!t.isPresent()) {
				return;
			}
			StructureTemplate template = t.get();
			lastAttemptInfo = template.name + " (selection " + (System.currentTimeMillis() - t1) + " ms)";
			StructureMap map = AWGameData.INSTANCE.getPerWorldData(world, StructureMap.class);

			if (attemptStructureGenerationAt(world, new BlockPos(x, y, z), face, template, map)) {
				lastAttemptInfo += " generated";
				territory.addClusterValue(template.getValidationSettings().getClusterValue());
				AncientWarfareStructure.LOG.info("Generated structure: {} at {}, {}, {}, time: {}ms", template.name, x, y, z, System.currentTimeMillis() - t1);
			}
		});
	}

	/*
	 * Improvement (not baseline, background_attempt_chunks): the chunks the first half of generateAt (spawn column, template
	 * selection) reads: the attempt's own chunk and, when harbor templates exist, the chunks within their reach (a harbor
	 * template's selection test reads a column in front of the position). Chunk positions as ChunkPos.toLong.
	 */
	public java.util.Set<Long> getSelectionChunks(int chunkX, int chunkZ) {
		java.util.Set<Long> chunks = new java.util.LinkedHashSet<>();
		int reach = WorldGenStructureManager.INSTANCE.getMaxHarborReach();
		addChunks(chunks, chunkX * 16 - reach, chunkZ * 16 - reach, chunkX * 16 + 15 + reach, chunkZ * 16 + 15 + reach);
		return chunks;
	}

	/*
	 * Improvement (not baseline, background_attempt_chunks): a dry run of the first half of generateAt (the same seed,
	 * position, facing and template selection, with an rng of its own and the statistics switched off, so nothing
	 * changes) that returns the chunks the whole attempt will read: the selection chunks, the spawn column and, when a
	 * template is selected, the area getAdjustedSpawnY and validatePlacement read (bounding box plus border; only the
	 * border ring for validators that read nothing else). The selection chunks must be loaded (or prepared) before this
	 * is called, as it reads them.
	 */
	public java.util.Set<Long> planAttemptChunks(int chunkX, int chunkZ, Level world) {
		java.util.Set<Long> chunks = getSelectionChunks(chunkX, chunkZ);
		long seed = (((long) chunkX) << 32) | (((long) chunkZ) & 0xffffffffL);
		Random dry = new Random(seed);
		int x = chunkX * 16 + dry.nextInt(16);
		int z = chunkZ * 16 + dry.nextInt(16);
		int y = getTargetY(world, x, z, false) + 1;
		if (y <= world.getMinBuildHeight()) {
			return chunks;
		}
		Optional<Territory> territory = TerritoryManager.getTerritory(chunkX, chunkZ, world);
		if (territory.isEmpty()) {
			return chunks;
		}
		Direction face = Direction.from2DDataValue(dry.nextInt(4));
		Optional<StructureTemplate> t;
		WorldGenStatistics.setSuppressed(true);
		try {
			t = WorldGenStructureManager.INSTANCE.selectTemplateForGeneration(world, dry, x, y, z, face, territory.get());
		}
		finally {
			WorldGenStatistics.setSuppressed(false);
		}
		if (t.isEmpty()) {
			return chunks;
		}
		StructureTemplate template = t.get();
		StructureBB bb = new StructureBB(new BlockPos(x, y, z), face, template.getSize(), template.getOffset());
		int border = Math.max(0, template.getValidationSettings().getBorderSize());
		int minX = bb.min.getX() - border;
		int minZ = bb.min.getZ() - border;
		int maxX = bb.max.getX() + border;
		int maxZ = bb.max.getZ() + border;
		if (template.getValidationSettings().readsBorderRingOnly()) {
			addChunks(chunks, minX, minZ, maxX, minZ);
			addChunks(chunks, minX, maxZ, maxX, maxZ);
			addChunks(chunks, minX, minZ, minX, maxZ);
			addChunks(chunks, maxX, minZ, maxX, maxZ);
		} else {
			addChunks(chunks, minX, minZ, maxX, maxZ);
		}
		//getAdjustedSpawnY reads the spawn column (underground, underwater) or the column in front of it (harbor)
		BlockPos front = new BlockPos(x, y, z).relative(face, template.getOffset().getZ());
		addChunks(chunks, front.getX(), front.getZ(), front.getX(), front.getZ());
		return chunks;
	}

	private static void addChunks(java.util.Set<Long> chunks, int minX, int minZ, int maxX, int maxZ) {
		for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
			for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
				chunks.add(net.minecraft.world.level.ChunkPos.asLong(cx, cz));
			}
		}
	}

	private void generateTerritoryBorders(int chunkX, int chunkZ, Level world, String territoryId) {
		ITerritoryData territoryData = TerritoryData.get(world);
		if (territoryData.isDifferentTerritory(territoryId, chunkX - 1, chunkZ)) {
			for (int z = chunkZ * 16 + 1; z < (chunkZ * 16 + 15); z++) {
				int x = chunkX * 16 + 1;
				world.setBlock(new BlockPos(x, WorldStructureGenerator.getTargetY(world, x, z, false), z), Blocks.WHITE_CONCRETE.defaultBlockState(), 2);
			}
		}

		if (territoryData.isDifferentTerritory(territoryId, chunkX + 1, chunkZ)) {
			for (int z = chunkZ * 16 + 1; z < (chunkZ * 16 + 15); z++) {
				int x = chunkX * 16 + 14;
				world.setBlock(new BlockPos(x, WorldStructureGenerator.getTargetY(world, x, z, false), z), Blocks.WHITE_CONCRETE.defaultBlockState(), 2);
			}
		}

		if (territoryData.isDifferentTerritory(territoryId, chunkX, chunkZ - 1)) {
			for (int x = chunkX * 16 + 1; x < (chunkX * 16 + 15); x++) {
				int z = chunkZ * 16 + 1;
				world.setBlock(new BlockPos(x, WorldStructureGenerator.getTargetY(world, x, z, false), z), Blocks.WHITE_CONCRETE.defaultBlockState(), 2);
			}
		}

		if (territoryData.isDifferentTerritory(territoryId, chunkX, chunkZ + 1)) {
			for (int x = chunkX * 16 + 1; x < (chunkX * 16 + 15); x++) {
				int z = chunkZ * 16 + 14;
				world.setBlock(new BlockPos(x, WorldStructureGenerator.getTargetY(world, x, z, false), z), Blocks.WHITE_CONCRETE.defaultBlockState(), 2);
			}
		}
	}

	public static int getTargetY(Level world, int x, int z, boolean skipWater) {
		return getTargetY(world, x, z, skipWater, world.getMaxBuildHeight());
	}

	public static int getTargetY(Level world, int x, int z, boolean skipWater, int startAtY) {
		for (int y = startAtY; y > world.getMinBuildHeight(); y--) {
			BlockState state = world.getBlockState(new BlockPos(x, y, z));
			if (AWStructureStatics.isSkippable(state) || skipWater && (state.is(Blocks.WATER) || state.getFluidState().is(Fluids.FLOWING_WATER))) {
				continue;
			}
			return y;
		}
		return -1;
	}

	public static void sprinkleSnow(Level world, StructureBB bb, int border) {
		BlockPos p1 = bb.min.offset(-border, 0, -border);
		BlockPos p2 = bb.max.offset(border, 0, border);
		for (int x = p1.getX(); x <= p2.getX(); x++) {
			for (int z = p1.getZ(); z <= p2.getZ(); z++) {
				int y = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 1, z)).getY() - 1;
				BlockPos pos = new BlockPos(x, y, z);
				if (p2.getY() >= y && y > world.getMinBuildHeight() && world.getBiome(pos.above()).value().shouldSnow(world, pos.above())) {
					BlockState state = world.getBlockState(pos);
					if (!state.isAir() && state.isFaceSturdy(world, pos, Direction.UP)) {
						world.setBlockAndUpdate(pos.above(), Blocks.SNOW.defaultBlockState());
					}
				}
			}
		}
	}

	/*
	 * Improvement (not baseline, budgeted_placement): sprinkleSnow above as units, one per column in the same order.
	 * sprinkleSnow is unchanged and stays the 1.12 path.
	 */
	public static java.util.List<Runnable> sprinkleSnowUnits(Level world, StructureBB bb, int border) {
		BlockPos p1 = bb.min.offset(-border, 0, -border);
		BlockPos p2 = bb.max.offset(border, 0, border);
		java.util.List<Runnable> units = new java.util.ArrayList<>();
		for (int x = p1.getX(); x <= p2.getX(); x++) {
			for (int z = p1.getZ(); z <= p2.getZ(); z++) {
				int cx = x;
				int cz = z;
				units.add(() -> {
					int y = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(cx, 1, cz)).getY() - 1;
					BlockPos pos = new BlockPos(cx, y, cz);
					if (p2.getY() >= y && y > world.getMinBuildHeight() && world.getBiome(pos.above()).value().shouldSnow(world, pos.above())) {
						BlockState state = world.getBlockState(pos);
						if (!state.isAir() && state.isFaceSturdy(world, pos, Direction.UP)) {
							world.setBlockAndUpdate(pos.above(), Blocks.SNOW.defaultBlockState());
						}
					}
				});
			}
		}
		return units;
	}

	public static int getStepNumber(int x, int z, int minX, int maxX, int minZ, int maxZ) {
		int steps = 0;
		if (x < minX - 1) {
			steps += (minX - 1) - x;
		} else if (x > maxX + 1) {
			steps += x - (maxX + 1);
		}
		if (z < minZ - 1) {
			steps += (minZ - 1) - z;
		} else if (z > maxZ + 1) {
			steps += z - (maxZ + 1);
		}
		return steps;
	}

	private boolean attemptStructureGenerationAt(Level world, BlockPos pos, Direction face, StructureTemplate template, StructureMap map) {
		long t1 = System.currentTimeMillis();
		int prevY = pos.getY();
		StructureBB bb = new StructureBB(pos, face, template.getSize(), template.getOffset());
		int y = template.getValidationSettings().getAdjustedSpawnY(world, pos.getX(), pos.getY(), pos.getZ(), face, template, bb);
		pos = new BlockPos(pos.getX(), y, pos.getZ());
		bb.min = bb.min.above(y - prevY);
		bb.max = bb.max.above(y - prevY);
		int xs = bb.getXSize();
		int zs = bb.getZSize();
		int size = ((Math.max(xs, zs)) / 16) + 3;
		if (!checkOtherStructureCrossAndCloseness(world, pos, map, bb, size, template.getValidationSettings().getBorderSize())) {
			WorldGenStatistics.addStructurePlacementRejection(template.name, PlacementRejectionReason.STRUCTURE_BB_OVERLAP);
			WorldGenDetailedLogHelper.log("Structure \"{}\" failed placement, because its bounding box {} intersects an existing structure", () -> template.name, () -> bb);
			return false;
		}

		TownMap townMap = AWGameData.INSTANCE.getPerWorldData(world, TownMap.class);
		if (townMap.intersectsWithTown(bb)) {
			WorldGenStatistics.addStructurePlacementRejection(template.name, PlacementRejectionReason.TOWN_BB_OVERLAP);
			WorldGenDetailedLogHelper.log("Structure \"{}\" failed placement, because its bounding box {} intersects an existing town", () -> template.name, () -> bb);
			return false;
		}
		if (template.getValidationSettings().validatePlacement(world, pos.getX(), pos.getY(), pos.getZ(), face, template, bb)) {
			WorldGenStatistics.addStructureGeneratedInfo(template.name, world, pos);
			AncientWarfareStructure.LOG.debug("Validation took: {} ms", System.currentTimeMillis() - t1);
			generateStructureAt(world, pos, face, template, map);
			return true;
		}
		return false;
	}

	private boolean checkOtherStructureCrossAndCloseness(Level world, BlockPos pos, StructureMap map, StructureBB bb, int size, int borderSize) {
		Collection<StructureEntry> bbCheckList = map.getEntriesNear(world, pos.getX(), pos.getZ(), size, true, new ArrayList<>());
		double maxDistance = 0;
		StructureBB bbWithBorder = new StructureBB(bb.min, bb.max).expand(borderSize, 0, borderSize);
		for (StructureEntry entry : bbCheckList) {
			if (bbWithBorder.intersects(entry.getBB())) {
				return false;
			}
			double distance = bb.getDistanceTo(entry.getBB());
			if (distance < MAX_DISTANCE_WITHIN_CLUSTER && distance > maxDistance) {
				maxDistance = distance;
			}
		}
		return !(maxDistance > 30 && world.random.nextFloat() * (MAX_DISTANCE_WITHIN_CLUSTER - maxDistance) > 30);
	}

	private void generateStructureAt(Level world, BlockPos pos, Direction face, StructureTemplate template, StructureMap map) {
		map.setGeneratedAt(world, pos.getX(), pos.getZ(), new StructureEntry(pos.getX(), pos.getY(), pos.getZ(), face, template), template.getValidationSettings().isUnique());
		WorldGenTickHandler.INSTANCE.addStructureForGeneration(new StructureBuilderWorldGen(world, template, face, pos));
	}

}
