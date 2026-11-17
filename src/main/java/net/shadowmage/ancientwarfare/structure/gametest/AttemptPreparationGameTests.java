package net.shadowmage.ancientwarfare.structure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureMap;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.template.WorldGenStructureManager;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.validation.StructureValidationType;
import net.shadowmage.ancientwarfare.structure.template.build.validation.StructureValidator;
import net.shadowmage.ancientwarfare.structure.template.build.validation.properties.StructureValidationProperties;
import net.shadowmage.ancientwarfare.structure.template.scan.TemplateScanner;
import net.shadowmage.ancientwarfare.structure.town.TownBoundingArea;
import net.shadowmage.ancientwarfare.structure.town.TownEstimatePrefetch;
import net.shadowmage.ancientwarfare.structure.town.TownPlacementValidator;
import net.shadowmage.ancientwarfare.structure.worldgen.AttemptChunkPreparation;
import net.shadowmage.ancientwarfare.structure.worldgen.TerritoryManager;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldGenTickHandler;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldStructureGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/*
 * Improvement (not baseline, background_attempt_chunks). The tests use instances of AttemptChunkPreparation /
 * TownEstimatePrefetch of their own (not the world generator's) and run in a batch of their own: they register a test
 * template as the only world generation template and switch off the world generator's own attempts while they run, so
 * nothing else may generate at the same time. The game test world is flat; the attempts are made 20000 - 44000 blocks
 * away, where no chunk exists yet.
 */
@GameTestHolder(AncientWarfare.MOD_ID)
@PrefixGameTestTemplate(false)
public class AttemptPreparationGameTests {
	private static final String TEMPLATE = "automation_empty";
	//one batch per test: the structure tests change the world generation template tables, the town test the town switch
	private static final String BATCH = "attempt_preparation";
	private static final String NAME = "gametest_attempt_hut";
	private static final int BORDER = 24;
	//chunks around an attempt's chunk that are compared (the hut plus its border spans at most 4 chunks each way)
	private static final int WINDOW = 5;

	private static void check(GameTestHelper helper, boolean condition, String message) {
		if (!condition) {
			helper.fail(message);
		}
	}

	/* a 5 x 4 x 5 hut, scanned from the test area and cleared again */
	private static StructureTemplate scanHut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos min = helper.absolutePos(new BlockPos(2, 2, 2));
		BlockPos max = min.offset(4, 3, 4);
		for (BlockPos p : BlockPos.betweenClosed(min, max)) {
			BlockState state = Blocks.AIR.defaultBlockState();
			boolean corner = (p.getX() == min.getX() || p.getX() == max.getX()) && (p.getZ() == min.getZ() || p.getZ() == max.getZ());
			if (p.getY() == min.getY()) {
				state = Blocks.STONE_BRICKS.defaultBlockState();
			} else if (corner) {
				state = Blocks.LAPIS_BLOCK.defaultBlockState();
			} else if (p.getY() == max.getY() && p.getX() == min.getX() + 2 && p.getZ() == min.getZ() + 2) {
				state = Blocks.GLOWSTONE.defaultBlockState();
			}
			level.setBlock(p, state, 3);
		}
		StructureTemplate template = TemplateScanner.scan(level, Collections.emptySet(), min, max, min.offset(2, 0, 0), 0, NAME);
		for (BlockPos p : BlockPos.betweenClosed(min, max)) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
		}
		StructureValidator validator = StructureValidationType.GROUND.getValidator();
		validator.setPropertyValue(StructureValidationProperties.WORLD_GEN, true);
		validator.setPropertyValue(StructureValidationProperties.BIOME_WHITE_LIST, false);
		validator.setPropertyValue(StructureValidationProperties.DIMENSION_WHITE_LIST, true);
		validator.setPropertyValue(StructureValidationProperties.DIMENSION_LIST, new int[] {0});
		validator.setPropertyValue(StructureValidationProperties.MIN_GENERATION_HEIGHT, level.getMinBuildHeight());
		validator.setPropertyValue(StructureValidationProperties.CLUSTER_VALUE, 0);
		validator.setPropertyValue(StructureValidationProperties.BORDER_SIZE, BORDER);
		validator.setPropertyValue(StructureValidationProperties.TERRITORY_NAME, "gametest_attempt");
		template.setValidationSettings(validator);
		return template;
	}

	/* the test template becomes the only world generation template; the world generator's own attempts are off */
	private static final class Setup implements AutoCloseable {
		private final boolean structures = AWStructureStatics.enableStructureGeneration;
		private final boolean towns = AWStructureStatics.enableTownGeneration;
		private boolean closed;

		private Setup(StructureTemplate template) {
			AWStructureStatics.enableStructureGeneration = false;
			AWStructureStatics.enableTownGeneration = false;
			WorldGenStructureManager.INSTANCE.clearCachedTemplates();
			TerritoryManager.clearTerritoryCache();
			WorldGenStructureManager.INSTANCE.loadBiomeList();
			StructureTemplateManager.addTemplate(template);
		}

		@Override
		public void close() {
			if (closed) {
				return;
			}
			closed = true;
			StructureTemplateManager.removeTemplate(NAME);
			WorldGenStructureManager.INSTANCE.rebuild();
			AWStructureStatics.enableStructureGeneration = structures;
			AWStructureStatics.enableTownGeneration = towns;
		}
	}

	/* what generateAt draws first from the chunk's seed: x and z offset in the chunk and the facing */
	private static int[] seedDraws(int chunkX, int chunkZ) {
		Random rng = new Random((((long) chunkX) << 32) | (((long) chunkZ) & 0xffffffffL));
		return new int[] {rng.nextInt(16), rng.nextInt(16), rng.nextInt(4)};
	}

	/* a chunk near (fromX, chunkZ) whose seed gives the same position in the chunk and facing as 'like' */
	private static ChunkPos findTwin(ChunkPos like, int fromX) {
		int[] want = seedDraws(like.x, like.z);
		for (int x = fromX; x < fromX + 200000; x++) {
			int[] got = seedDraws(x, like.z);
			if (got[0] == want[0] && got[1] == want[1] && got[2] == want[2]) {
				return new ChunkPos(x, like.z);
			}
		}
		return null;
	}

	/* chunk offsets (relative to 'center') of the full chunks in the window around it */
	private static Set<String> fullChunks(ServerLevel level, ChunkPos center) {
		Set<String> full = new TreeSet<>();
		for (int dx = -WINDOW; dx <= WINDOW; dx++) {
			for (int dz = -WINDOW; dz <= WINDOW; dz++) {
				if (level.getChunkSource().getChunkNow(center.x + dx, center.z + dz) != null) {
					full.add(dx + "," + dz);
				}
			}
		}
		return full;
	}

	private static Optional<StructureEntry> entry(ServerLevel level, ChunkPos chunk) {
		List<StructureEntry> entries = new ArrayList<>(StructureMap.get(level).getEntriesNear(level, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), 1, true, new ArrayList<>()));
		entries.removeIf(e -> !e.getName().equals(NAME));
		return entries.size() == 1 ? Optional.of(entries.get(0)) : Optional.empty();
	}

	private static String relative(StructureBB bb, ChunkPos chunk) {
		return bb.min.offset(-chunk.getMinBlockX(), 0, -chunk.getMinBlockZ()) + " - " + bb.max.offset(-chunk.getMinBlockX(), 0, -chunk.getMinBlockZ());
	}

	/* true when the chunk still has a ticket of AttemptChunkPreparation (the ticket map is read by reflection) */
	@SuppressWarnings("unchecked")
	private static boolean hasAttemptTicket(ServerLevel level, long pos) {
		try {
			java.lang.reflect.Field field = net.minecraft.server.level.DistanceManager.class.getDeclaredField("tickets");
			field.setAccessible(true);
			java.util.Map<Long, ? extends Iterable<net.minecraft.server.level.Ticket<?>>> tickets = (java.util.Map<Long, ? extends Iterable<net.minecraft.server.level.Ticket<?>>>) field.get(level.getChunkSource().chunkMap.getDistanceManager());
			Iterable<net.minecraft.server.level.Ticket<?>> set = tickets.get(pos);
			if (set != null) {
				for (net.minecraft.server.level.Ticket<?> ticket : set) {
					if (ticket.getType() == AttemptChunkPreparation.getTicketType()) {
						return true;
					}
				}
			}
			return false;
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("cannot read the chunk tickets", ex);
		}
	}

	/*
	 * An attempt whose border reaches chunks that do not exist yet waits until they are generated (the preparation says
	 * "not ready" and holds tickets), then the unchanged attempt gives the same result as the synchronous path at a twin
	 * chunk (same position in the chunk, same facing, flat terrain): the same structure map entry relative to the chunk,
	 * the same chunks made full, the same blocks once built; the tickets are gone afterwards.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, batch = BATCH, timeoutTicks = 1200)
	public static void improvementAttemptWaitsForChunksAndPlacesTheSame(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Setup setup = new Setup(scanHut(helper));
		AttemptChunkPreparation preparation = new AttemptChunkPreparation();
		Object owner = new Object();
		ChunkPos sync = new ChunkPos(1250, 1250);
		ChunkPos async = findTwin(sync, 1400);
		AtomicInteger phase = new AtomicInteger();
		AtomicInteger ticks = new AtomicInteger();
		AtomicReference<Set<String>> fullSync = new AtomicReference<>();
		AtomicReference<List<Long>> ticketed = new AtomicReference<>();
		AtomicReference<StructureBB[]> bbs = new AtomicReference<>();
		int builtBefore = WorldGenTickHandler.INSTANCE.getStructuresGenerated();
		try {
			check(helper, async != null, "no twin chunk found");
			//synchronous (original) path at the first chunk
			level.getChunk(sync.x, sync.z);
			check(helper, fullChunks(level, sync).size() == 1, "chunks around the first attempt exist already: " + fullChunks(level, sync));
			WorldStructureGenerator.INSTANCE.generateAt(sync.x, sync.z, level);
			fullSync.set(fullChunks(level, sync));
			check(helper, entry(level, sync).isPresent(), "the synchronous attempt generated nothing");
			check(helper, fullSync.get().size() > 4, "the synchronous attempt read too few chunks to test anything: " + fullSync.get());
			//prepared path at the twin
			level.getChunk(async.x, async.z);
			check(helper, !preparation.isReady(owner, level, async.x, async.z, level.getServer().getTickCount()), "the attempt did not wait for its chunks");
			check(helper, preparation.getTicketCount() > 0, "no chunk tickets for the waiting attempt");
		}
		catch (RuntimeException ex) {
			preparation.release();
			setup.close();
			throw ex;
		}
		helper.onEachTick(() -> {
			try {
				ticks.incrementAndGet();
				check(helper, ticks.get() < 1100, "phase " + phase.get() + " not complete after " + ticks.get() + " ticks");
				if (phase.get() == 0) {
					if (!preparation.isReady(owner, level, async.x, async.z, level.getServer().getTickCount())) {
						return;
					}
					check(helper, ticks.get() < AttemptChunkPreparation.ATTEMPT_WAIT_TICKS, "the attempt timed out instead of getting its chunks");
					ticketed.set(preparation.getTicketedChunks());
					WorldStructureGenerator.INSTANCE.generateAt(async.x, async.z, level);
					Set<String> fullAsync = fullChunks(level, async);
					preparation.done(owner);
					check(helper, preparation.getTicketCount() == 0, "tickets left after the attempt: " + preparation.getTicketCount());
					check(helper, fullAsync.equals(fullSync.get()), "full chunks after the attempt differ: synchronous " + fullSync.get() + ", prepared " + fullAsync);
					Optional<StructureEntry> a = entry(level, sync);
					Optional<StructureEntry> b = entry(level, async);
					check(helper, b.isPresent(), "the prepared attempt generated nothing");
					check(helper, relative(a.get().getBB(), sync).equals(relative(b.get().getBB(), async)), "structure map entries differ: " + relative(a.get().getBB(), sync) + " / " + relative(b.get().getBB(), async));
					bbs.set(new StructureBB[] {a.get().getBB(), b.get().getBB()});
					AncientWarfare.LOG.info("improvementAttemptWaitsForChunksAndPlacesTheSame: waited {} ticks, {} chunk tickets, full chunks {}", ticks.get(), ticketed.get().size(), fullAsync.size());
					phase.set(1);
					ticks.set(0);
					return;
				}
				if (phase.get() == 1) {
					//no ticket of the preparation is left on any of its chunks
					for (long pos : ticketed.get()) {
						check(helper, !hasAttemptTicket(level, pos), "chunk " + new ChunkPos(pos) + " still has an attempt ticket");
					}
					phase.set(2);
					return;
				}
				if (phase.get() == 2) {
					if (WorldGenTickHandler.INSTANCE.getStructuresGenerated() < builtBefore + 2 || !WorldGenTickHandler.INSTANCE.isIdle()) {
						return;
					}
					StructureBB a = bbs.get()[0];
					StructureBB b = bbs.get()[1];
					BlockPos shift = b.min.subtract(a.min);
					int compared = 0;
					for (BlockPos p : BlockPos.betweenClosed(a.min.offset(-BORDER, -2, -BORDER), a.max.offset(BORDER, 2, BORDER))) {
						BlockState sa = level.getBlockState(p);
						BlockState sb = level.getBlockState(p.offset(shift));
						check(helper, sa == sb, "block at " + p + ": synchronous " + sa + ", prepared " + sb);
						compared++;
					}
					check(helper, a.min.getY() == b.min.getY(), "different heights");
					check(helper, level.getBlockState(a.min).is(Blocks.STONE_BRICKS) || count(level, a, Blocks.STONE_BRICKS) == 25, "the structure was not built");
					AncientWarfare.LOG.info("improvementAttemptWaitsForChunksAndPlacesTheSame: {} block states identical", compared);
					phase.set(3);
					preparation.release();
					setup.close();
					helper.succeed();
				}
			}
			catch (RuntimeException ex) {
				preparation.release();
				setup.close();
				throw ex;
			}
		});
	}

	private static int count(ServerLevel level, StructureBB bb, net.minecraft.world.level.block.Block block) {
		int found = 0;
		for (BlockPos p : BlockPos.betweenClosed(bb.min, bb.max)) {
			if (level.getBlockState(p).is(block)) {
				found++;
			}
		}
		return found;
	}

	/*
	 * An attempt that has waited ATTEMPT_WAIT_TICKS runs at once, the original way (its chunks are generated
	 * synchronously inside the attempt), and its tickets are removed.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, batch = BATCH + "_timeout", timeoutTicks = 200)
	public static void improvementAttemptTimeoutRunsSynchronously(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Setup setup = new Setup(scanHut(helper));
		AttemptChunkPreparation preparation = new AttemptChunkPreparation();
		Object owner = new Object();
		ChunkPos chunk = new ChunkPos(1250, 2000);
		List<Long> ticketed;
		try {
			level.getChunk(chunk.x, chunk.z);
			long tick = level.getServer().getTickCount();
			check(helper, !preparation.isReady(owner, level, chunk.x, chunk.z, tick), "the attempt did not wait for its chunks");
			ticketed = preparation.getTicketedChunks();
			boolean missing = false;
			for (long pos : ticketed) {
				ChunkHolder holder = level.getChunkSource().chunkMap.getVisibleChunkIfPresent(pos);
				ChunkStatus status = holder == null ? null : holder.getLatestStatus();
				missing |= status == null || !status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT);
			}
			check(helper, missing, "all chunks were there already");
			check(helper, preparation.isReady(owner, level, chunk.x, chunk.z, tick + AttemptChunkPreparation.ATTEMPT_WAIT_TICKS), "the attempt still waits after " + AttemptChunkPreparation.ATTEMPT_WAIT_TICKS + " ticks");
			WorldStructureGenerator.INSTANCE.generateAt(chunk.x, chunk.z, level);
			preparation.done(owner);
			check(helper, entry(level, chunk).isPresent(), "the timed out attempt generated nothing");
			check(helper, preparation.getTicketCount() == 0, "tickets left after the attempt");
		}
		finally {
			preparation.release();
			setup.close();
		}
		List<Long> released = ticketed;
		helper.runAfterDelay(2, () -> {
			for (long pos : released) {
				check(helper, !hasAttemptTicket(level, pos), "chunk " + new ChunkPos(pos) + " still has an attempt ticket");
			}
			helper.succeed();
		});
	}

	private static String describe(Optional<TownBoundingArea> area) {
		return area.map(a -> a.getChunkMinX() + "," + a.getChunkMinZ() + " - " + a.getChunkMaxX() + "," + a.getChunkMaxZ() + " y " + a.getMinY() + "-" + a.getMaxY() + " surface " + a.getSurfaceY()).orElse("none");
	}

	/*
	 * A town search over chunks that do not exist yet finds the same area when its height estimates are computed on the
	 * background threads (the attempt waits for them, the server thread computes none) as when the server thread
	 * computes them during the search.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, batch = BATCH + "_town", timeoutTicks = 7000)
	public static void improvementTownSearchSameWithBackgroundEstimates(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int x = 44000;
		int z = 20000;
		boolean towns = AWStructureStatics.enableTownGeneration;
		float rough = AWStructureStatics.townSearchRoughFraction;
		AWStructureStatics.enableTownGeneration = false;
		//the flat test world is below the town height band everywhere ("too low"); with every such chunk allowed the search
		//grows to its full size over chunks that do not exist, which is what the test needs
		AWStructureStatics.townSearchRoughFraction = 1.0f;
		TownEstimatePrefetch prefetch = new TownEstimatePrefetch();
		//the GameTest server does not pace its ticks: 400 ticks (20 s in a game) can pass before the estimate threads are
		//done, so this test gives the attempt a longer wait limit and checks that it did not reach it
		int waitTicks = 6000;
		TownPlacementValidator.clearEstimates();
		int before = TownPlacementValidator.estimatesComputed;
		String expected = describe(TownPlacementValidator.findGenerationPosition(level, x, z));
		int computedSync = TownPlacementValidator.estimatesComputed - before;
		TownPlacementValidator.clearEstimates();
		Object owner = new Object();
		AtomicReference<String> result = new AtomicReference<>();
		AtomicInteger ticks = new AtomicInteger();
		int computedBefore = TownPlacementValidator.estimatesComputed;
		helper.onEachTick(() -> {
			if (result.get() != null) {
				return;
			}
			try {
				ticks.incrementAndGet();
				if (ticks.get() > 6500) {
					result.set("none (timed out)");
					helper.fail("the town attempt did not run within 6500 ticks");
				}
				if (!prefetch.tryAttempt(owner, level, x, z, level.getServer().getTickCount(), waitTicks,
						() -> result.set(describe(TownPlacementValidator.findGenerationPosition(level, x, z))))) {
					return;
				}
				check(helper, computedSync > 10, "the search used only " + computedSync + " estimates; nothing to test");
				check(helper, expected.equals(result.get()), "town area: synchronous " + expected + ", background " + result.get());
				check(helper, prefetch.getAttemptsWaited() == 1 && ticks.get() < waitTicks, "the attempt did not wait for its estimates, or timed out (" + ticks.get() + " ticks)");
				check(helper, TownPlacementValidator.estimatesComputed == computedBefore, (TownPlacementValidator.estimatesComputed - computedBefore) + " estimates computed on the server thread");
				AncientWarfare.LOG.info("improvementTownSearchSameWithBackgroundEstimates: {} after {} ticks, {} estimates in the background ({} needed synchronously)", result.get(), ticks.get(), prefetch.getEstimatesRequested(), computedSync);
				helper.succeed();
			}
			finally {
				if (result.get() != null) {
					prefetch.shutdown();
					AWStructureStatics.enableTownGeneration = towns;
					AWStructureStatics.townSearchRoughFraction = rough;
				}
			}
		});
	}
}
