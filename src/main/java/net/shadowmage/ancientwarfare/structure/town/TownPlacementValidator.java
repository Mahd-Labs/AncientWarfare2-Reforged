package net.shadowmage.ancientwarfare.structure.town;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureMap;
import net.shadowmage.ancientwarfare.structure.gamedata.TownMap;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldStructureGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/*
 * Port notes: the 1.12 search read the top blocks of every chunk it looked at, which generated all of them (up to
 * 33 x 33 chunks per attempt). Chunks that are loaded are still read block by block; for chunks that are not, the
 * height comes from the chunk generator (4 sample columns per chunk, cached, no block type test), so the search
 * itself does not generate terrain. Liquid test: 1.12 material.isLiquid -> any fluid.
 */
public class TownPlacementValidator {
	private TownPlacementValidator() {}

	private static int maxSize = 32;
	//Port notes: 1.12 used -1 / "<= 0" for "no valid top block"; heights can be negative now
	private static final int INVALID = Integer.MIN_VALUE;
	//Port addition: a column whose top is liquid that reaches below y 56 (1.12 treated it like INVALID, see isBlocked)
	private static final int DEEP_LIQUID = Integer.MIN_VALUE + 1;
	private static final int MIN_STRUCTURE_DISTANCE = TownGeneratorBorders.MAX_BORDER_WIDTH + 5;

	/*
	 * input a single X, Y, Z coordinate to examine the nearby area for potential town generation.<br>
	 * First examines the chunk that was at the input coordinates, and expands out from there attempting
	 * to create the largest town-bounding area that it can.
	 *
	 * @param x world x to search from
	 * @param z world z to search from
	 * @return maximal bounding area for a town, or null if no acceptable area was found starting in the specified chunk
	 */
	public static Optional<TownBoundingArea> findGenerationPosition(Level world, int x, int z) {
		if (isTownClose(world, x, z)) {
			return Optional.empty();
		}

		int cx = x >> 4;
		int cz = z >> 4;

		int height = getTopFilledHeight(world, x, z);
		if (height == INVALID || height == DEEP_LIQUID) {
			return Optional.empty();
		}

		int minY = Math.max(world.getMinBuildHeight(), world.getSeaLevel() - 4);

		TownBoundingArea area = new TownBoundingArea();
		//Port deviation: 1.12 allowed chunks 3 blocks below the start column (config town_search_height_drop)
		area.minY = Math.max(minY, height - AWStructureStatics.townSearchHeightDrop);
		area.maxY = Math.min(world.getMaxBuildHeight() - 1, area.minY + 40);
		area.chunkMinX = cx;
		area.chunkMaxX = cx;
		area.chunkMinZ = cz;
		area.chunkMaxZ = cz;
		if (estimateGuard) {
			guardArea = area;
		}

		StructureMap sm = AWGameData.INSTANCE.getPerWorldData(world, StructureMap.class);
		Collection<StructureEntry> structureList = sm.getEntriesNear(world, x, z, maxSize + 2, true, new ArrayList<>());

		if (isStructureInside(structureList, x, z, area.minY, area.maxY)) {
			return Optional.empty();
		}

		//Port addition: no point in searching further than the largest town template can use (an area is cut down to
		//the template's maximum anyway); searching terrain that is not generated yet is slow in 1.21
		maxSize = Math.max(4, Math.min(32, TownTemplateManager.INSTANCE.getLargestMaxSize() + 2));
		expandBoundingArea(world, area, structureList);
		AncientWarfareStructure.LOG.debug("Town search rejects so far: too low {}, too high {}, no valid top block {}, structure nearby {}; start height {}", REJECTS[0], REJECTS[1], REJECTS[2], REJECTS[3], height);
		shrinkTooLongArea(area);
		levelToAverageBorderHeight(world, area);
		return Optional.of(area);
	}

	private static final int STEP = 4;

	private static void levelToAverageBorderHeight(Level world, TownBoundingArea area) {
		int minX = area.getBlockMinX() - 1;
		int maxX = area.getBlockMaxX() + 1;
		int minZ = area.getBlockMinZ() - 1;
		int maxZ = area.getBlockMaxZ() + 1;

		int totalLevel = 0;
		int totalPoints = 0;

		for (int x = minX + STEP; x < maxX; x += STEP) {
			totalLevel += getTargetY(world, x, minZ);
			totalPoints++;
		}

		for (int z = minZ + STEP; z < maxZ; z += STEP) {
			totalLevel += getTargetY(world, minX, z);
			totalPoints++;
		}

		for (int x = maxX - STEP; x > minX; x -= STEP) {
			totalLevel += getTargetY(world, x, maxZ);
			totalPoints++;
		}

		for (int z = maxZ - STEP; z > minZ; z -= STEP) {
			totalLevel += getTargetY(world, maxX, z);
			totalPoints++;
		}
		if (totalPoints > 0) {
			area.setSurfaceY(totalLevel / totalPoints);
		}
	}

	private static void shrinkTooLongArea(TownBoundingArea area) {
		int cw = area.getChunkWidth();
		int cl = area.getChunkLength();
		if (cw > cl * 2) {
			int diff = cw - (cl * 2);
			while (diff > 0) {
				area.chunkMaxX--;
				diff--;
				if (diff > 0) {
					area.chunkMinX++;
					diff--;
				}
			}
		}
		if (cl > cw * 2) {
			int diff = cl - (cw * 2);
			while (diff > 0) {
				area.chunkMaxZ--;
				diff--;
				if (diff > 0) {
					area.chunkMinZ++;
					diff--;
				}
			}
		}
	}

	private static boolean isStructureInside(Collection<StructureEntry> structureList, int x, int z, int minY, int maxY) {
		for (StructureEntry structure : structureList) {
			if (structure.getBB().intersects(x - MIN_STRUCTURE_DISTANCE, minY, z - MIN_STRUCTURE_DISTANCE,
					x + 16 + MIN_STRUCTURE_DISTANCE, maxY, z + 16 + MIN_STRUCTURE_DISTANCE)) {
				return true;
			}
		}

		return false;
	}

	private static boolean isTownClose(Level world, int x, int z) {
		TownMap tm = AWGameData.INSTANCE.getPerWorldData(world, TownMap.class);
		int minDist = AWStructureStatics.townClosestDistance;
		float dist = tm.getClosestTown(x, z, minDist * 2);
		return dist < minDist;
	}

	private static void expandBoundingArea(Level world, TownBoundingArea area, Collection<StructureEntry> structureList) {
		boolean xneg = true;//if should try and expand on this direction next pass, once set to false it never checks that direction again
		boolean xpos = true;
		boolean zneg = true;
		boolean zpos = true;
		boolean didExpand = false;//set to true if any expansion occurred on that pass.  if false at end of pass, will break out of loop as no more expansion is possible
		do {
			didExpand = false;
			if (xneg && area.getChunkWidth() <= maxSize) {
				xneg = tryExpandXNeg(world, area, structureList);
				didExpand = xneg;
			}
			if (xpos && area.getChunkWidth() <= maxSize) {
				xpos = tryExpandXPos(world, area, structureList);
				didExpand = didExpand || xpos;
			}
			if (zneg && area.getChunkLength() <= maxSize) {
				zneg = tryExpandZNeg(world, area, structureList);
				didExpand = didExpand || zneg;
			}
			if (zpos && area.getChunkLength() <= maxSize) {
				zpos = tryExpandZPos(world, area, structureList);
				didExpand = didExpand || zpos;
			}
		} while (didExpand && (area.getChunkWidth() <= maxSize || area.getChunkLength() <= maxSize));
	}

	private static boolean tryExpandXNeg(Level world, TownBoundingArea area, Collection<StructureEntry> structureList) {
		int cx = area.chunkMinX - 1;
		int rough = 0;
		for (int z = area.chunkMinZ; z <= area.chunkMaxZ; z++) {
			int result = classify(world, cx, z, area, structureList);
			if (result == HARD || result == ROUGH && ++rough > maxRoughChunks(area.chunkMaxZ - area.chunkMinZ + 1)) {
				return false;
			}
		}
		area.chunkMinX = cx;
		return true;
	}

	private static boolean tryExpandXPos(Level world, TownBoundingArea area, Collection<StructureEntry> structureList) {
		int cx = area.chunkMaxX + 1;
		int rough = 0;
		for (int z = area.chunkMinZ; z <= area.chunkMaxZ; z++) {
			int result = classify(world, cx, z, area, structureList);
			if (result == HARD || result == ROUGH && ++rough > maxRoughChunks(area.chunkMaxZ - area.chunkMinZ + 1)) {
				return false;
			}
		}
		area.chunkMaxX = cx;
		return true;
	}

	private static boolean tryExpandZNeg(Level world, TownBoundingArea area, Collection<StructureEntry> structureList) {
		int cz = area.chunkMinZ - 1;
		int rough = 0;
		for (int x = area.chunkMinX; x <= area.chunkMaxX; x++) {
			int result = classify(world, x, cz, area, structureList);
			if (result == HARD || result == ROUGH && ++rough > maxRoughChunks(area.chunkMaxX - area.chunkMinX + 1)) {
				return false;
			}
		}
		area.chunkMinZ = cz;
		return true;
	}

	private static boolean tryExpandZPos(Level world, TownBoundingArea area, Collection<StructureEntry> structureList) {
		int cz = area.chunkMaxZ + 1;
		int rough = 0;
		for (int x = area.chunkMinX; x <= area.chunkMaxX; x++) {
			int result = classify(world, x, cz, area, structureList);
			if (result == HARD || result == ROUGH && ++rough > maxRoughChunks(area.chunkMaxX - area.chunkMinX + 1)) {
				return false;
			}
		}
		area.chunkMaxZ = cz;
		return true;
	}

	/* chunks counted against town areas while growing: [0] too low or too wet, [1] too high, [2] non target top block, [3] structure nearby */
	static final int[] REJECTS = new int[4];

	private static final int OK = 0;
	private static final int ROUGH = 1;
	private static final int HARD = 2;

	/*
	 * Port deviation: 1.12 stopped growing a town area at the first chunk of a row that was outside
	 * the height band or held deep water. 1.18+ terrain is rougher, so a share of the chunks of each new row
	 * (config town_search_rough_fraction, 1.12 = 0) may be such chunks; the town levels them when it is built.
	 * Chunks with a non target top block (other buildings) or next to a generated structure still stop the area.
	 */
	private static int maxRoughChunks(int rowLength) {
		return (int) (rowLength * AWStructureStatics.townSearchRoughFraction);
	}

	private static int classify(Level world, int cx, int cz, TownBoundingArea area, Collection<StructureEntry> structureList) {
		int height = getAverageHeight(world, cx, cz);
		if (height == INVALID) {
			REJECTS[2]++;
			return HARD;
		}
		if (isStructureInside(structureList, cx << 4, cz << 4, area.minY, area.maxY)) {
			REJECTS[3]++;
			return HARD;
		}
		if (height == DEEP_LIQUID || height < area.minY) {
			REJECTS[0]++;
			return ROUGH;
		}
		if (height > area.maxY) {
			REJECTS[1]++;
			return ROUGH;
		}
		return OK;
	}

	private static int getAverageHeight(Level world, int cx, int cz) {
		LevelChunk chunk = getLoadedChunk(world, cx, cz);
		if (chunk == null) {
			if (!(world instanceof ServerLevel)) {
				return INVALID;
			}
			int[] estimate = getChunkEstimate((ServerLevel) world, cx, cz);
			return estimate[3] != 0 ? INVALID : estimate[0] == INVALID || estimate[2] > 4 * AWStructureStatics.townSearchWaterFraction ? DEEP_LIQUID : estimate[0];
		}
		int total = 0;
		int deep = 0;
		for (int x = (cx << 4); x < ((cx << 4) + 16); x++) {
			for (int z = (cz << 4); z < ((cz << 4) + 16); z++) {
				int val = getTopFilledHeight(chunk, x, z);
				if (val == INVALID) {
					return INVALID;
				}
				if (val == DEEP_LIQUID) {
					deep++;
				} else {
					total += val;
				}
			}
		}
		if (deep == 256 || deep > 256 * AWStructureStatics.townSearchWaterFraction) {
			return DEEP_LIQUID;
		}
		return Math.floorDiv(total, 256 - deep);
	}

	private static LevelChunk getLoadedChunk(Level world, int cx, int cz) {
		return world instanceof ServerLevel ? ((ServerLevel) world).getChunkSource().getChunkNow(cx, cz) : null;
	}

	private static int getTopFilledHeight(Level world, int x, int z) {
		LevelChunk chunk = getLoadedChunk(world, x >> 4, z >> 4);
		return chunk == null ? getEstimatedTopFilledHeight(world, x, z) : getTopFilledHeight(chunk, x, z);
	}

	private static int getTargetY(Level world, int x, int z) {
		if (getLoadedChunk(world, x >> 4, z >> 4) != null || !(world instanceof ServerLevel)) {
			return WorldStructureGenerator.getTargetY(world, x, z, false);
		}
		int[] estimate = getChunkEstimate((ServerLevel) world, x >> 4, z >> 4);
		return estimate[1];
	}

	/*
	 * Port addition: heights of chunks that are not generated yet, from the chunk generator (4 sample columns per
	 * chunk, one generator column each). The values are cached: every new chunk of an area asks for the same chunks
	 * again, and a generator column costs about 0.2 ms. [0] average top solid height of the columns without deep liquid, or
	 * INVALID, [1] average top block height including liquids, [2] number of sample columns with liquid that reaches
	 * below y 56 (as getTopFilledHeight), [3] 1 when a column has no solid block at all.
	 */
	private static final java.util.Map<Long, int[]> ESTIMATES = new java.util.HashMap<>();
	//diagnostics (debug log of slow town attempts): estimates computed and the time spent on them
	public static int estimatesComputed;
	public static long estimateNanos;
	private static net.minecraft.resources.ResourceKey<Level> estimatesWorld;
	private static long estimatesSeed;

	public static void clearEstimates() {
		ESTIMATES.clear();
		estimatesWorld = null;
	}

	private static int[] getChunkEstimate(ServerLevel level, int cx, int cz) {
		if (estimatesWorld != level.dimension() || estimatesSeed != level.getSeed() || ESTIMATES.size() > 200000) {
			ESTIMATES.clear();
			estimatesWorld = level.dimension();
			estimatesSeed = level.getSeed();
		}
		long key = net.minecraft.world.level.ChunkPos.asLong(cx, cz);
		int[] cached = ESTIMATES.get(key);
		if (cached != null) {
			return cached;
		}
		if (estimateGuard) {
			//background_attempt_chunks: the guarded attempt stops here and is run again once the estimate is there
			missingEstimate = key;
			throw MissingEstimate.INSTANCE;
		}
		long start = System.nanoTime();
		try {
			int[] estimate = computeChunkEstimate(level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level, cx, cz);
			ESTIMATES.put(key, estimate);
			return estimate;
		}
		finally {
			estimatesComputed++;
			estimateNanos += System.nanoTime() - start;
		}
	}

	/*
	 * Improvement (not baseline, background_attempt_chunks). While estimateGuard is set (only during a town attempt run
	 * by TownEstimatePrefetch), an estimate that is not cached is not computed: the attempt is abandoned with
	 * MissingEstimate (before it has changed anything: the search reads only) and TownEstimatePrefetch computes the
	 * estimates around the search area on a background thread, then runs the attempt again from the start. The
	 * estimates are a pure function of the chunk generator, so they are the same wherever they are computed.
	 */
	static boolean estimateGuard;
	static long missingEstimate;
	//the search area being grown (for the estimates to prefetch), null before the first chunk is known
	static TownBoundingArea guardArea;

	static final class MissingEstimate extends RuntimeException {
		static final MissingEstimate INSTANCE = new MissingEstimate();

		private MissingEstimate() {
			super("town height estimate not computed yet", null, false, false);
		}
	}

	static boolean hasEstimate(ServerLevel level, long key) {
		return estimatesWorld == level.dimension() && estimatesSeed == level.getSeed() && ESTIMATES.containsKey(key);
	}

	/* stores an estimate computed in the background (same checks of world and seed as getChunkEstimate) */
	static void putEstimate(ServerLevel level, long key, int[] estimate) {
		if (estimatesWorld != level.dimension() || estimatesSeed != level.getSeed() || ESTIMATES.size() > 200000) {
			ESTIMATES.clear();
			estimatesWorld = level.dimension();
			estimatesSeed = level.getSeed();
		}
		ESTIMATES.putIfAbsent(key, estimate);
	}

	/* the estimate itself; pure computation (chunk generator columns), also called from TownEstimatePrefetch's threads */
	static int[] computeChunkEstimate(ChunkGenerator generator, RandomState randomState, net.minecraft.world.level.LevelHeightAccessor level, int cx, int cz) {
		{
			int solidTotal = 0;
			int surfaceTotal = 0;
			boolean invalid = false;
			int deep = 0;
			for (int x = (cx << 4) + 4; x < (cx << 4) + 16; x += 8) {
				for (int z = (cz << 4) + 4; z < (cz << 4) + 16; z += 8) {
					net.minecraft.world.level.NoiseColumn column = generator.getBaseColumn(x, z, level, randomState);
					int surface = INVALID;
					int floor = INVALID;
					for (int y = level.getMaxBuildHeight() - 1; y > level.getMinBuildHeight(); y--) {
						BlockState state = column.getBlock(y);
						if (state.isAir()) {
							continue;
						}
						if (surface == INVALID) {
							surface = y;
						}
						if (state.blocksMotion()) {
							floor = y;
							break;
						}
					}
					surfaceTotal += surface == INVALID ? level.getMinBuildHeight() : surface;
					if (floor == INVALID) {
						invalid = true;
					} else if (surface > floor && floor + 1 < 56) {
						deep++;
					} else {
						solidTotal += floor;
					}
				}
			}
			return new int[] {invalid || deep == 4 ? INVALID : Math.floorDiv(solidTotal, 4 - deep), Math.floorDiv(surfaceTotal, 4), deep, invalid ? 1 : 0};
		}
	}

	private static int getEstimatedTopFilledHeight(Level world, int x, int z) {
		if (!(world instanceof ServerLevel)) {
			return INVALID;
		}
		int[] estimate = getChunkEstimate((ServerLevel) world, x >> 4, z >> 4);
		return estimate[3] != 0 ? INVALID : estimate[2] > 0 ? DEEP_LIQUID : estimate[0];
	}

	/*
	 * return the highest Y that has a solid non-skipped block in it<br>
	 * This implementation skips water, air, and any blocks on the world-gen skippable blocks list (trees, plants, etc)
	 *
	 * @return top solid block height, or -1 for invalid top block or no top block found (void, bedrock...)
	 */
	private static int getTopFilledHeight(LevelChunk chunk, int x, int z) {
		int maxY = chunk.getHighestSectionPosition() + 15;
		Block block;
		for (int y = maxY; y > chunk.getMinBuildHeight(); y--) {
			BlockState state = chunk.getBlockState(new BlockPos(x, y, z));
			block = state.getBlock();
			if (AWStructureStatics.isSkippable(state)) {
				continue;
			}
			if (!state.getFluidState().isEmpty()) {
				if (y >= 56) {
					continue;
				}// >=56 is fillable through underfill/border settings.  below that is too deep for a proper gradient on the border.
				return DEEP_LIQUID;//liquid block is too low
			}
			if (!AWStructureStatics.isValidTargetBlock(state)) {
				AncientWarfareStructure.LOG.debug("rejecting town chunk for non-target block: {} :: {}:{}", block, chunk.getPos().x, chunk.getPos().z);
				return INVALID;
			}
			return y;//if not skippable and is valid target block, return that y-level
		}
		return INVALID;
	}
}
