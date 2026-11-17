package net.shadowmage.ancientwarfare.structure.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.BudgetedBuild;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBuilder;
import net.shadowmage.ancientwarfare.structure.template.build.StructurePlacementQueue;
import net.shadowmage.ancientwarfare.structure.town.TownEstimatePrefetch;
import net.shadowmage.ancientwarfare.structure.town.WorldTownGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/*
 * Port notes: 1.12 IWorldGenerator.generate(chunk) has no equivalent. A chunk that was generated for the first time
 * shows up as ChunkEvent.Load with isNewChunk(); its coordinates are queued (newChunks) and handed to
 * WorldStructureGenerator.generate / WorldTownGenerator.generate (the 1.12 IWorldGenerator bodies) from the next
 * server tick, never from inside chunk loading. Everything else is the 1.12 handler: chunk tickets, town tickets and
 * structure tickets are worked off at the end of the server tick. Dimension ints -> dimension keys.
 * Additions: the time spent per tick is accumulated (getGenerationNanos) for the log line at server stop, and towns
 * wait for their chunks (addTownForGeneration): 1.12 generated a town at once and the chunk provider created the
 * missing chunks on the fly; doing that in 1.21 means generating up to a few hundred chunks synchronously in one tick.
 * A town that was selected asks for its chunks with chunk tickets and is generated as soon as all of them are
 * loaded (or after TOWN_WAIT_TICKS); the tickets are dropped when the structure queue has run empty after that.
 */
public final class WorldGenTickHandler {
	private static final int MAX_BLOCKS_TO_GEN_PER_TICK = 10000;
	public static final WorldGenTickHandler INSTANCE = new WorldGenTickHandler();
	private final List<ChunkGenerationTicket> newWorldGenTickets;
	private final List<ChunkGenerationTicket> newTownGenTickets;
	private final List<ChunkGenerationTicket> chunksToGen;
	private final List<ChunkGenerationTicket> townChunksToGen;
	private final List<StructureTicket> newStructureGenTickets;
	private final List<StructureTicket> structuresToGen;
	private final List<ChunkGenerationTicket> newChunks = new ArrayList<>();
	private final List<PendingTown> pendingTowns = new ArrayList<>();
	//budgeted_placement (improvement): terrain preparations of towns in progress, worked off first in first out
	private final List<TownPreparation> townPreparations = new ArrayList<>();
	private static final int TOWN_WAIT_TICKS = 6000;
	private static final TicketType<ChunkPos> TOWN_TICKET = TicketType.create("ancientwarfare_town", Comparator.comparingLong(ChunkPos::toLong));
	private final java.util.Random random = new java.util.Random();
	private long generationNanos;
	private long maxTickNanos;
	private int structuresGenerated;
	private int chunksSeen;
	private int chunkTickets;
	private boolean finishing;
	//diagnostics (debug log): new chunks that finished loading on the server thread (counted inside attempts), handler ticks over 200 ms
	private int newChunkLoads;
	private int slowTicks;
	private int maxStructureQueue;
	private int maxTownQueue;
	private static final long SLOW_ATTEMPT_NANOS = 100000000L;
	private static final long SLOW_TICK_NANOS = 200000000L;

	private WorldGenTickHandler() {
		newWorldGenTickets = new ArrayList<>();
		newTownGenTickets = new ArrayList<>();
		newStructureGenTickets = new ArrayList<>();
		chunksToGen = new ArrayList<>();
		townChunksToGen = new ArrayList<>();
		structuresToGen = new ArrayList<>();
	}

	void addChunkForGeneration(Level world, int chunkX, int chunkZ) {
		newWorldGenTickets.add(new ChunkGenerationTicket(world, chunkX, chunkZ));
	}

	public void addChunkForTownGeneration(Level world, int chunkX, int chunkZ) {
		newTownGenTickets.add(new ChunkGenerationTicket(world, chunkX, chunkZ));
	}

	public void addStructureForGeneration(StructureBuilder builder) {
		newStructureGenTickets.add(new StructureGenerationTicket(builder));
	}

	public void addStructureGenCallback(StructureTicket tk) {
		newStructureGenTickets.add(tk);
	}

	@SubscribeEvent
	public void onChunkLoad(ChunkEvent.Load evt) {
		if (evt.isNewChunk() && evt.getLevel() instanceof ServerLevel) {
			newChunkLoads++;
		}
		if (evt.isNewChunk() && evt.getLevel() instanceof ServerLevel && AWStructureStatics.enableWorldGen) {
			newChunks.add(new ChunkGenerationTicket((ServerLevel) evt.getLevel(), evt.getChunk().getPos().x, evt.getChunk().getPos().z));
		}
	}

	@SubscribeEvent
	public void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload evt) {
		if (evt.getLevel() instanceof Level level) {
			AttemptChunkPreparation.INSTANCE.onLevelUnload(level);
		}
	}

	/* background_attempt_chunks: summary for the log line at server stop */
	public String getAttemptPreparationSummary() {
		return AttemptChunkPreparation.INSTANCE.getSummary() + "; " + TownEstimatePrefetch.INSTANCE.getSummary() + "; longest queues: structure attempts " + maxStructureQueue + ", town attempts " + maxTownQueue;
	}

	@SubscribeEvent
	public void serverTick(ServerTickEvent.Post evt) {
		if (!AWStructureStatics.enableWorldGen) {
			newChunks.clear();
			return;
		}
		if (newChunks.isEmpty() && isIdle()) {
			return;
		}
		long start = System.nanoTime();
		processNewChunks();
		long tChunks = System.nanoTime();
		//Port deviation (towns): structure attempts of new chunks wait while town attempts are queued, so a
		//town search sees the terrain before the structures of the same batch of chunks take the room
		if (townChunksToGen.isEmpty() && newTownGenTickets.isEmpty()) {
			genChunks();
		}
		long tAttempts = System.nanoTime();
		genStructures();
		long tStructures = System.nanoTime();
		genTowns();
		long tTowns = System.nanoTime();
		genTownPreparations();
		long tPreparations = System.nanoTime();
		tickPendingTowns(false);
		long end = System.nanoTime();
		long time = end - start;
		maxStructureQueue = Math.max(maxStructureQueue, chunksToGen.size() + newWorldGenTickets.size());
		maxTownQueue = Math.max(maxTownQueue, townChunksToGen.size() + newTownGenTickets.size());
		generationNanos += time;
		maxTickNanos = Math.max(maxTickNanos, time);
		if (time > SLOW_TICK_NANOS) {
			slowTicks++;
			AncientWarfareStructure.LOG.debug("World generation took {} ms of this tick (new chunks {}, structure attempts {}, structures {}, town attempts {}, town preparations {}, towns {} ms)", time / 1000000,
					(tChunks - start) / 1000000, (tAttempts - tChunks) / 1000000, (tStructures - tAttempts) / 1000000, (tTowns - tStructures) / 1000000, (tPreparations - tTowns) / 1000000, (end - tPreparations) / 1000000);
		}
	}

	/*
	 * the 1.12 IWorldGenerator calls (structure generator weight 1, town generator weight 2)
	 */
	private void processNewChunks() {
		if (newChunks.isEmpty()) {
			return;
		}
		List<ChunkGenerationTicket> chunks = new ArrayList<>(newChunks);
		newChunks.clear();
		for (ChunkGenerationTicket tk : chunks) {
			Level world = tk.getWorld();
			if (world == null) {
				continue;
			}
			//budgeted placement: a chunk under a structure that is still being placed (or a town whose terrain is still
			//being prepared) waits until it is complete
			if (!finishing && (StructurePlacementQueue.isInBuildArea(world, tk.chunkX, tk.chunkZ) || isInTownPreparation(world, tk.chunkX, tk.chunkZ))) {
				newChunks.add(tk);
				continue;
			}
			chunksSeen++;
			long t0 = System.nanoTime();
			int territories = TerritoryManager.territoriesGenerated;
			int territoryChunks = TerritoryManager.territoryChunks;
			long territoryNanos = TerritoryManager.territoryNanos;
			if (AWStructureStatics.enableStructureGeneration) {
				WorldStructureGenerator.INSTANCE.generate(random, tk.chunkX, tk.chunkZ, world);
			}
			if (AWStructureStatics.enableTownGeneration) {
				WorldTownGenerator.INSTANCE.generate(random, tk.chunkX, tk.chunkZ, world);
			}
			long time = System.nanoTime() - t0;
			if (time > 50000000L) {
				AncientWarfareStructure.LOG.debug("Slow new chunk {} {}: {} ms, {} territories generated ({} chunks included) in {} ms", tk.chunkX, tk.chunkZ, time / 1000000,
						TerritoryManager.territoriesGenerated - territories, TerritoryManager.territoryChunks - territoryChunks, (TerritoryManager.territoryNanos - territoryNanos) / 1000000);
			}
		}
	}

	public boolean isIdle() {
		return pendingTowns.isEmpty() && townPreparations.isEmpty() && newChunks.isEmpty() && newWorldGenTickets.isEmpty() && newTownGenTickets.isEmpty() && newStructureGenTickets.isEmpty()
				&& chunksToGen.isEmpty() && townChunksToGen.isEmpty() && structuresToGen.isEmpty();
	}

	public long getGenerationNanos() {
		return generationNanos;
	}

	public long getMaxTickNanos() {
		return maxTickNanos;
	}

	public int getChunksSeen() {
		return chunksSeen;
	}

	/* diagnostics: new chunks that have finished loading on the server thread so far */
	public int getNewChunkLoads() {
		return newChunkLoads;
	}

	/* diagnostics: handler ticks that took more than 200 ms */
	public int getSlowTicks() {
		return slowTicks;
	}

	private void logSlowAttempt(String kind, ChunkGenerationTicket tk, long start, int chunkLoadsBefore, String detail) {
		long time = System.nanoTime() - start;
		if (time > SLOW_ATTEMPT_NANOS) {
			AncientWarfareStructure.LOG.debug("Slow {} attempt at chunk {} {}: {} ms, {} new chunks loaded during the attempt; {}", kind, tk.chunkX, tk.chunkZ, time / 1000000, newChunkLoads - chunkLoadsBefore, detail);
		}
	}

	public int getChunkTickets() {
		return chunkTickets;
	}

	public int getStructuresGenerated() {
		return structuresGenerated;
	}

	/*
	 * drops everything that is queued (server stopped; the tickets hold levels of that server)
	 */
	public void clear() {
		AttemptChunkPreparation.INSTANCE.release();
		TownEstimatePrefetch.INSTANCE.shutdown();
		pendingTowns.clear();
		townPreparations.clear();
		net.shadowmage.ancientwarfare.structure.town.TownPlacementValidator.clearEstimates();
		newChunks.clear();
		newWorldGenTickets.clear();
		newTownGenTickets.clear();
		newStructureGenTickets.clear();
		chunksToGen.clear();
		townChunksToGen.clear();
		structuresToGen.clear();
	}

	public void finalTick() {
		//budgeted placement: the server is stopping, every structure (also one half built) is completed now
		finishing = true;
		//background_attempt_chunks: attempts waiting for chunks or estimates run now, the original way (as with the switch
		//off); nothing waits for the background work
		AttemptChunkPreparation.INSTANCE.release();
		TownEstimatePrefetch.INSTANCE.reset();
		long start = System.nanoTime();
		int structureAttempts = chunksToGen.size() + newWorldGenTickets.size();
		int townAttempts = townChunksToGen.size() + newTownGenTickets.size();
		int structures = structuresToGen.size() + newStructureGenTickets.size();
		int chunks = newChunks.size();
		int preparations = townPreparations.size();
		try {
			finalTickInternal();
		}
		finally {
			finishing = false;
			//diagnostics: what the server stop had to complete synchronously
			AncientWarfareStructure.LOG.debug("World generation at server stop: {} new chunks, {} structure attempts, {} town attempts, {} structure tickets, {} town preparations were queued; completed in {} ms",
					chunks, structureAttempts, townAttempts, structures, preparations, (System.nanoTime() - start) / 1000000);
		}
	}

	private void finalTickInternal() {
		//budgeted placement: town terrain preparations in progress are completed first (they queue the town's structures)
		finishTownPreparations();
		processNewChunks();
		genChunks();
		genTowns();
		tickPendingTowns(true);
		while (!chunksToGen.isEmpty()) {
			genChunks();
		}
		while (!structuresToGen.isEmpty()) {
			genStructures();
		}
		while (!townChunksToGen.isEmpty()) {
			genTowns();
		}
		tickPendingTowns(true);
		while (!structuresToGen.isEmpty() || !newStructureGenTickets.isEmpty()) {
			genStructures();
		}
		tickPendingTowns(true);
	}

	/*
	 * Port addition, see the class comment. generate builds the town (TownGenerator.generate).
	 */
	public void addTownForGeneration(Level world, int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ, Runnable generate) {
		if (!(world instanceof ServerLevel)) {
			generate.run();
			return;
		}
		//budgeted placement: one more chunk around the town, so the border smoothing (up to 13 blocks beyond the town area)
		//and its tree clearing (10 blocks around a cleared block) find their chunks loaded instead of loading them inside
		//a slice; the 1.12 path keeps the margin of one chunk
		int margin = StructurePlacementQueue.isBudgeted() ? 2 : 1;
		PendingTown town = new PendingTown(world.dimension(), chunkMinX - margin, chunkMinZ - margin, chunkMaxX + margin, chunkMaxZ + margin, generate);
		town.forEachChunk(pos -> ((ServerLevel) world).getChunkSource().addRegionTicket(TOWN_TICKET, pos, 0, pos));
		pendingTowns.add(town);
	}

	private void tickPendingTowns(boolean serverStopping) {
		if (pendingTowns.isEmpty()) {
			return;
		}
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		Iterator<PendingTown> it = pendingTowns.iterator();
		List<PendingTown> ready = new ArrayList<>();
		while (it.hasNext()) {
			PendingTown town = it.next();
			ServerLevel level = server == null ? null : server.getLevel(town.world);
			if (level == null) {
				it.remove();
				continue;
			}
			town.ticks++;
			if (!town.generated) {
				if (serverStopping || town.ticks > TOWN_WAIT_TICKS || town.allChunksLoaded(level)) {
					AncientWarfareStructure.LOG.debug("Town chunks {} after {} ticks, generating", town.allChunksLoaded(level) ? "loaded" : "not all loaded", town.ticks);
					town.generated = true;
					ready.add(town);
				}
			} else if (serverStopping || structuresToGen.isEmpty() && newStructureGenTickets.isEmpty() && townPreparations.isEmpty()) {
				town.forEachChunk(pos -> level.getChunkSource().removeRegionTicket(TOWN_TICKET, pos, 0, pos));
				it.remove();
			}
		}
		//generate after the iteration: a town queues structure tickets and may be the reason another town is added
		for (PendingTown town : ready) {
			try {
				town.generate.run();
			}
			catch (Exception ex) {
				AncientWarfareStructure.LOG.error("Error generating town: ", ex);
			}
		}
	}

	/*
	 * Improvement (not baseline, budgeted_placement): true when TownGenerator.generate should hand its terrain
	 * preparation to addTownPreparation instead of doing it at once (switch on, a server level, server not stopping)
	 */
	public boolean isTownPreparationBudgeted(Level world) {
		return StructurePlacementQueue.isBudgeted() && !finishing && world instanceof ServerLevel;
	}

	/*
	 * budgeted placement: the terrain preparation of a town (biome, borders, levelling) followed by the rest of
	 * TownGenerator.generate (grid, walls, the road / plot callback) as one resumable sequence. It runs inside the
	 * town's chunk tickets (the town ticket of addTownForGeneration covers the town area plus two chunks; the tickets are
	 * kept while a preparation is in progress), within the shared tick budget of StructurePlacementQueue.
	 */
	public void addTownPreparation(Level world, StructureBB bounds, String name, net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps steps) {
		townPreparations.add(new TownPreparation(world.dimension(), (bounds.min.getX() >> 4) - 2, (bounds.min.getZ() >> 4) - 2, (bounds.max.getX() >> 4) + 2, (bounds.max.getZ() >> 4) + 2, name, steps));
	}

	private boolean isInTownPreparation(Level world, int chunkX, int chunkZ) {
		for (TownPreparation prep : townPreparations) {
			if (prep.world == world.dimension() && chunkX >= prep.chunkMinX && chunkX <= prep.chunkMaxX && chunkZ >= prep.chunkMinZ && chunkZ <= prep.chunkMaxZ) {
				return true;
			}
		}
		return false;
	}

	private void genTownPreparations() {
		if (townPreparations.isEmpty()) {
			return;
		}
		if (!StructurePlacementQueue.isBudgeted()) {
			//switched off while preparations were in progress: complete them as the original path would have
			finishTownPreparations();
			return;
		}
		if (!StructurePlacementQueue.hasBudget()) {
			return;
		}
		long start = System.nanoTime();
		long deadline = StructurePlacementQueue.deadline(start);
		while (!townPreparations.isEmpty()) {
			if (!townPreparations.get(0).work(deadline, false)) {
				break;
			}
			townPreparations.remove(0);
			if (System.nanoTime() - deadline >= 0) {
				break;
			}
		}
		StructurePlacementQueue.consume(System.nanoTime() - start);
	}

	private void finishTownPreparations() {
		while (!townPreparations.isEmpty()) {
			townPreparations.remove(0).work(0, true);
		}
	}

	private static final class TownPreparation {
		private final ResourceKey<Level> world;
		private final int chunkMinX;
		private final int chunkMinZ;
		private final int chunkMaxX;
		private final int chunkMaxZ;
		private final String name;
		private final net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps steps;
		private int ticks;
		private long nanos;
		private long longestNanos;

		private TownPreparation(ResourceKey<Level> world, int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ, String name, net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps steps) {
			this.world = world;
			this.chunkMinX = chunkMinX;
			this.chunkMinZ = chunkMinZ;
			this.chunkMaxX = chunkMaxX;
			this.chunkMaxZ = chunkMaxZ;
			this.name = name;
			this.steps = steps;
		}

		/* true when complete (or failed; logged) */
		private boolean work(long deadline, boolean unlimited) {
			long start = System.nanoTime();
			boolean done;
			try {
				done = steps.run(deadline, unlimited);
			}
			catch (Exception ex) {
				AncientWarfareStructure.LOG.error("Error generating town: ", ex);
				return true;
			}
			finally {
				long time = System.nanoTime() - start;
				ticks++;
				nanos += time;
				longestNanos = Math.max(longestNanos, time);
			}
			if (done) {
				AncientWarfareStructure.LOG.info("Budgeted town preparation of {} complete: {} ticks, {} ms in total, longest slice {} ms, {} units", name, ticks, nanos / 1000000, longestNanos / 1000000, steps.getUnits());
			}
			return done;
		}
	}

	private static final class PendingTown {
		private final ResourceKey<Level> world;
		private final int chunkMinX;
		private final int chunkMinZ;
		private final int chunkMaxX;
		private final int chunkMaxZ;
		private final Runnable generate;
		private int ticks;
		private boolean generated;

		private PendingTown(ResourceKey<Level> world, int chunkMinX, int chunkMinZ, int chunkMaxX, int chunkMaxZ, Runnable generate) {
			this.world = world;
			this.chunkMinX = chunkMinX;
			this.chunkMinZ = chunkMinZ;
			this.chunkMaxX = chunkMaxX;
			this.chunkMaxZ = chunkMaxZ;
			this.generate = generate;
		}

		private void forEachChunk(java.util.function.Consumer<ChunkPos> action) {
			for (int x = chunkMinX; x <= chunkMaxX; x++) {
				for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
					action.accept(new ChunkPos(x, z));
				}
			}
		}

		private boolean allChunksLoaded(ServerLevel level) {
			for (int x = chunkMinX; x <= chunkMaxX; x++) {
				for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
					if (level.getChunkSource().getChunkNow(x, z) == null) {
						return false;
					}
				}
			}
			return true;
		}
	}

	/*
	 * Improvement (not baseline, background_attempt_chunks): true while attempts wait for their chunks / height estimates
	 * (switch on, server not stopping)
	 */
	private boolean isAttemptPreparationActive() {
		return AWStructureStatics.backgroundAttemptChunks && !finishing;
	}

	private static long serverTick() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		return server == null ? 0 : server.getTickCount();
	}

	private void genChunks() {
		while (!chunksToGen.isEmpty()) {
			ChunkGenerationTicket tk = chunksToGen.get(0);
			Level world = tk.getWorld();
			//background_attempt_chunks: the head of the queue waits until the chunks it will read are generated; nothing
			//behind it runs before it (same order)
			if (world instanceof ServerLevel level && isAttemptPreparationActive() && !AttemptChunkPreparation.INSTANCE.isReady(tk, level, tk.chunkX, tk.chunkZ, serverTick())) {
				break;
			}
			chunksToGen.remove(0);
			if (world != null) {
				chunkTickets++;
				long t0 = System.nanoTime();
				int c0 = newChunkLoads;
				try {
					WorldStructureGenerator.INSTANCE.generateAt(tk.chunkX, tk.chunkZ, world);
				}
				finally {
					//background_attempt_chunks: the attempt has run (or failed), its chunk tickets are removed
					AttemptChunkPreparation.INSTANCE.done(tk);
				}
				logSlowAttempt("structure", tk, t0, c0, WorldStructureGenerator.INSTANCE.getLastAttemptInfo());
			} else {
				AttemptChunkPreparation.INSTANCE.done(tk);
			}
		}
		if (!newWorldGenTickets.isEmpty()) {
			chunksToGen.addAll(newWorldGenTickets);
			newWorldGenTickets.clear();
		}
	}

	private void genTowns() {
		int countGenerated = 0;
		//Port addition: besides the 1.12 limit of 20 attempts, no new attempt is started after 50 ms (an attempt over
		//terrain that is not generated yet asks the chunk generator for heights, which is slow in 1.21)
		long start = System.nanoTime();
		while (!townChunksToGen.isEmpty() && countGenerated < 20 && System.nanoTime() - start < 50000000L) {
			ChunkGenerationTicket tk = townChunksToGen.get(0);
			Level world = tk.getWorld();
			long t0 = System.nanoTime();
			int c0 = newChunkLoads;
			int e0 = net.shadowmage.ancientwarfare.structure.town.TownPlacementValidator.estimatesComputed;
			long en0 = net.shadowmage.ancientwarfare.structure.town.TownPlacementValidator.estimateNanos;
			if (world instanceof ServerLevel level && isAttemptPreparationActive()) {
				//background_attempt_chunks: the head waits until the height estimates its search needs are computed
				if (!TownEstimatePrefetch.INSTANCE.tryAttempt(tk, level, tk.chunkX * 16, tk.chunkZ * 16, serverTick(), AttemptChunkPreparation.ATTEMPT_WAIT_TICKS,
						() -> WorldTownGenerator.INSTANCE.attemptGeneration(world, tk.chunkX * 16, tk.chunkZ * 16))) {
					break;
				}
				townChunksToGen.remove(0);
			} else {
				townChunksToGen.remove(0);
				if (world != null) {
					WorldTownGenerator.INSTANCE.attemptGeneration(world, tk.chunkX * 16, tk.chunkZ * 16);
				}
			}
			if (world != null) {
				logSlowAttempt("town", tk, t0, c0, (net.shadowmage.ancientwarfare.structure.town.TownPlacementValidator.estimatesComputed - e0) + " chunk height estimates computed in "
						+ (net.shadowmage.ancientwarfare.structure.town.TownPlacementValidator.estimateNanos - en0) / 1000000 + " ms");
			}
			countGenerated++;
		}
		if (!newTownGenTickets.isEmpty()) {
			townChunksToGen.addAll(newTownGenTickets);
			newTownGenTickets.clear();
		}
	}

	private void genStructures() {
		if (StructurePlacementQueue.isBudgeted() && !finishing) {
			genStructuresBudgeted();
			return;
		}
		int totalBlocks = 0;
		while (!structuresToGen.isEmpty() && totalBlocks < MAX_BLOCKS_TO_GEN_PER_TICK) {
			StructureTicket structureTicket = structuresToGen.remove(0);
			totalBlocks += structureTicket.getBlocksToGenerate();
			structureTicket.call();
		}
		if (!newStructureGenTickets.isEmpty()) {
			structuresToGen.addAll(newStructureGenTickets);
			newStructureGenTickets.clear();
		}
	}

	/*
	 * Improvement (not baseline, budgeted_placement): the queue is worked off in the same order, but a structure is
	 * placed within the tick's time budget (StructurePlacementQueue) and continued on the next tick; the ticket stays
	 * at the head of the queue until the structure is complete, so the callbacks of a town (roads after the walls,
	 * lamps and villagers after the structures) still run after everything queued before them, and the town's chunk
	 * tickets are kept until its last structure is complete. The 1.12 limit of 10000 blocks per tick is replaced by
	 * the time budget.
	 */
	private void genStructuresBudgeted() {
		if (!structuresToGen.isEmpty() && StructurePlacementQueue.hasBudget()) {
			long start = System.nanoTime();
			long deadline = StructurePlacementQueue.deadline(start);
			while (!structuresToGen.isEmpty()) {
				StructureTicket structureTicket = structuresToGen.get(0);
				boolean done;
				if (structureTicket instanceof StructureGenerationTicket generationTicket) {
					done = generationTicket.callBudgeted(deadline);
				} else {
					structureTicket.call();
					done = true;
				}
				if (!done) {
					break;
				}
				structuresToGen.remove(0);
				if (System.nanoTime() - deadline >= 0) {
					break;
				}
			}
			StructurePlacementQueue.consume(System.nanoTime() - start);
		}
		if (!newStructureGenTickets.isEmpty()) {
			structuresToGen.addAll(newStructureGenTickets);
			newStructureGenTickets.clear();
		}
	}

	private static class ChunkGenerationTicket {
		private final ResourceKey<Level> world;
		private final int chunkX;
		private final int chunkZ;

		private ChunkGenerationTicket(Level world, int x, int z) {
			this.world = world.dimension();
			chunkX = x;
			chunkZ = z;
		}

		public Level getWorld() {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			return server == null ? null : server.getLevel(world);
		}
	}

	/*
	 * Base structure ticket class.  Changed to a callback mechanism to allow anonymous callback classes,
	 * to inform town-gen of when first / second pass structures are finished being generated; to allow
	 * the road to generate after walls, etc
	 *
	 * @author Shadowmage
	 */
	public interface StructureTicket {
		void call();
		int getBlocksToGenerate();
	}

	private static final class StructureGenerationTicket implements StructureTicket {
		private final StructureBuilder builder;
		private BudgetedBuild budgeted;

		private StructureGenerationTicket(StructureBuilder builder) {
			this.builder = builder;
		}

		@Override
		public void call() {
			try {
				long start = System.nanoTime();
				if (budgeted != null) {
					//begun with budgeted placement: completed now (server stop or the switch turned off)
					budgeted.work(0, true);
					if (budgeted.isFailed()) {
						return;
					}
				} else {
					builder.instantConstruction();
				}
				INSTANCE.structuresGenerated++;
				AncientWarfareStructure.LOG.debug("Built structure {} ({} blocks) in {} ms", builder.getTemplate().name, getBlocksToGenerate(), (System.nanoTime() - start) / 1000000);
			}
			catch (Exception ex) {
				AncientWarfareStructure.LOG.error("Error building structure {}: ", builder.getTemplate().name, ex);
			}
		}

		private boolean callBudgeted(long deadline) {
			if (budgeted == null) {
				budgeted = new BudgetedBuild(builder, null, null, false);
			}
			if (!budgeted.work(deadline, false)) {
				return false;
			}
			if (!budgeted.isFailed()) {
				INSTANCE.structuresGenerated++;
			}
			return true;
		}

		@Override
		public int getBlocksToGenerate() {
			StructureBB bb = builder.getBoundingBox();
			return bb.getXSize() * bb.getZSize() * bb.getYSize();
		}
	}
}
