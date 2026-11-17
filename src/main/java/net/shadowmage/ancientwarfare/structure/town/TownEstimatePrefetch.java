package net.shadowmage.ancientwarfare.structure.town;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Improvement (not baseline, background_attempt_chunks). A town attempt's search (TownPlacementValidator) reads chunks
 * that are not generated yet through height estimates from the chunk generator, about 8 ms per chunk on the server
 * thread; a search over a few hundred such chunks was a server tick of up to 4 s. Here the attempt at the head of the
 * town queue is run with TownPlacementValidator.estimateGuard: when it needs an estimate that is not cached, it stops
 * (it has changed nothing up to then) and the estimates of the search area plus PREFETCH_RING chunks around it are
 * computed on this class's own threads; the attempt waits at the head of the queue and is run again, from the start,
 * once they are in. The estimates are the same values the server thread would compute, so the search result is too.
 *
 * Bounds: the threads (THREADS, daemon, lowest priority) belong to this class and nothing else uses them; the server
 * thread never waits for them (results are collected from a concurrent map once per tick). Only the queue head has
 * work queued (at most one search area, (2 * 32 + 1 + 2 * PREFETCH_RING)^2 chunks); work of an earlier head is skipped.
 * An attempt that has waited ATTEMPT_WAIT_TICKS, or whose missing estimate was already requested and failed, runs
 * unguarded (the original synchronous way). At server stop the threads are shut down without waiting and their
 * results are dropped.
 */
public final class TownEstimatePrefetch {
	public static final TownEstimatePrefetch INSTANCE = new TownEstimatePrefetch();
	private static final int THREADS = 2;
	private static final int PREFETCH_RING = 3;

	private ExecutorService executor;
	private final Map<Long, int[]> results = new ConcurrentHashMap<>();
	private final java.util.Set<Long> requested = new java.util.HashSet<>();
	private ResourceKey<Level> resultsWorld;
	private long resultsSeed;
	//work of the current head; a new head gets a new counter and a new generation (earlier work is skipped)
	private AtomicInteger pending = new AtomicInteger();
	private final AtomicInteger generation = new AtomicInteger();
	private Object owner;
	private long startTick;
	private int rounds;
	//statistics for the log line at server stop
	private int attemptsWaited;
	private int attemptsTimedOut;
	private long longestWaitTicks;
	private int estimatesRequested;
	private final AtomicInteger estimatesFailed = new AtomicInteger();

	/* public for the GameTests, which use an instance of their own; the world generator uses INSTANCE */
	public TownEstimatePrefetch() {
	}

	/*
	 * runs the town attempt 'owner' (the queue ticket; 'attempt' is WorldTownGenerator.attemptGeneration at blockX, blockZ)
	 * when its estimates are there; returns true when it has run, false when it waits (the caller keeps it at the head
	 * and calls again next tick)
	 */
	public boolean tryAttempt(Object owner, ServerLevel level, int blockX, int blockZ, long tick, int maxWaitTicks, Runnable attempt) {
		if (owner != this.owner) {
			reset();
			this.owner = owner;
			this.startTick = tick;
		}
		collect(level);
		long waited = tick - startTick;
		if (waited >= maxWaitTicks) {
			attemptsTimedOut++;
			AncientWarfareStructure.LOG.debug("Town attempt at {} {} waited {} ticks for height estimates, runs without them", blockX, blockZ, waited);
			return run(attempt, waited);
		}
		if (pending.get() > 0) {
			return false;
		}
		//everything requested is computed: collect what came in after the collect above
		collect(level);
		TownPlacementValidator.estimateGuard = true;
		TownPlacementValidator.guardArea = null;
		try {
			return run(attempt, waited);
		}
		catch (TownPlacementValidator.MissingEstimate missing) {
			TownBoundingArea area = TownPlacementValidator.guardArea;
			long missingKey = TownPlacementValidator.missingEstimate;
			TownPlacementValidator.estimateGuard = false;
			TownPlacementValidator.guardArea = null;
			if (requested.contains(missingKey)) {
				//requested before and still not there (computation failed): the original way
				return run(attempt, waited);
			}
			int cx = blockX >> 4;
			int cz = blockZ >> 4;
			if (area != null) {
				request(level, area.chunkMinX - PREFETCH_RING, area.chunkMinZ - PREFETCH_RING, area.chunkMaxX + PREFETCH_RING, area.chunkMaxZ + PREFETCH_RING);
			} else {
				request(level, cx - PREFETCH_RING, cz - PREFETCH_RING, cx + PREFETCH_RING, cz + PREFETCH_RING);
			}
			int mx = ChunkPos.getX(missingKey);
			int mz = ChunkPos.getZ(missingKey);
			request(level, mx - 1, mz - 1, mx + 1, mz + 1);
			rounds++;
			AncientWarfareStructure.LOG.debug("Town attempt at {} {}: estimate of chunk {} {} missing (round {}, area {}), {} estimates requested, {} pending", blockX, blockZ, mx, mz, rounds,
					area == null ? "none" : area.chunkMinX + "," + area.chunkMinZ + " - " + area.chunkMaxX + "," + area.chunkMaxZ, requested.size(), pending.get());
			return false;
		}
		finally {
			TownPlacementValidator.estimateGuard = false;
			TownPlacementValidator.guardArea = null;
		}
	}

	private boolean run(Runnable attempt, long waited) {
		attempt.run();
		if (waited > 0) {
			attemptsWaited++;
			longestWaitTicks = Math.max(longestWaitTicks, waited);
		}
		return true;
	}

	private void request(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
		if (resultsWorld != level.dimension() || resultsSeed != level.getSeed()) {
			results.clear();
			resultsWorld = level.dimension();
			resultsSeed = level.getSeed();
		}
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomState randomState = level.getChunkSource().randomState();
		int gen = generation.get();
		AtomicInteger counter = pending;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				long key = ChunkPos.asLong(x, z);
				if (TownPlacementValidator.hasEstimate(level, key) || results.containsKey(key) || !requested.add(key)) {
					continue;
				}
				int cx = x;
				int cz = z;
				counter.incrementAndGet();
				estimatesRequested++;
				getExecutor().execute(() -> {
					try {
						if (generation.get() == gen) {
							results.put(key, TownPlacementValidator.computeChunkEstimate(generator, randomState, level, cx, cz));
						}
					}
					catch (Throwable t) {
						if (estimatesFailed.getAndIncrement() == 0) {
							AncientWarfareStructure.LOG.warn("Town height estimate for chunk {} {} failed on the background thread (the town search computes it on the server thread instead)", cx, cz, t);
						}
					}
					finally {
						counter.decrementAndGet();
					}
				});
			}
		}
	}

	/* moves the estimates computed so far into TownPlacementValidator's cache (server thread) */
	private void collect(ServerLevel level) {
		if (results.isEmpty()) {
			return;
		}
		if (resultsWorld != level.dimension() || resultsSeed != level.getSeed()) {
			results.clear();
			return;
		}
		java.util.Iterator<Map.Entry<Long, int[]>> it = results.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, int[]> entry = it.next();
			TownPlacementValidator.putEstimate(level, entry.getKey(), entry.getValue());
			it.remove();
		}
	}

	/* a new head (or the switch was turned off): earlier work is skipped and forgotten */
	public void reset() {
		generation.incrementAndGet();
		pending = new AtomicInteger();
		requested.clear();
		owner = null;
		rounds = 0;
	}

	/* server stopped: threads are shut down without waiting, results dropped */
	public void shutdown() {
		reset();
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
		results.clear();
		resultsWorld = null;
	}

	public int getEstimatesRequested() {
		return estimatesRequested;
	}

	public int getAttemptsWaited() {
		return attemptsWaited;
	}

	public String getSummary() {
		return attemptsWaited + " town attempts waited for height estimates (longest " + longestWaitTicks + " ticks, " + attemptsTimedOut + " ran without all of them), " + estimatesRequested + " estimates computed in the background, " + estimatesFailed.get() + " failed";
	}

	private ExecutorService getExecutor() {
		if (executor == null) {
			AtomicInteger threadNumber = new AtomicInteger();
			executor = new ThreadPoolExecutor(THREADS, THREADS, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), runnable -> {
				Thread thread = new Thread(runnable, "AncientWarfare town height estimates #" + threadNumber.incrementAndGet());
				thread.setDaemon(true);
				thread.setPriority(Thread.MIN_PRIORITY);
				return thread;
			});
			((ThreadPoolExecutor) executor).allowCoreThreadTimeOut(true);
		}
		return executor;
	}
}
