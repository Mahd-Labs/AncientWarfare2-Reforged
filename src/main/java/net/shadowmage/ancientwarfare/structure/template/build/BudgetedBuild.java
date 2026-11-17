package net.shadowmage.ancientwarfare.structure.template.build;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/*
 * Improvement (not baseline, budgeted_placement): one structure that is placed over several server ticks.
 * - Holds a chunk ticket of its own (ticket level FULL, no ticking) on every chunk of the bounding box plus the
 *   template's border, from the first call until the build is complete or has failed; nothing more.
 * - Waits up to CHUNK_WAIT_TICKS for those chunks to load in the background before the first block is placed
 *   (1.12 / the instant path generate missing chunks synchronously inside the build tick).
 * - Then StructureBuilder.constructUntil, which places the positions in the order of instantConstruction.
 * before / after run around the blocks like the code around instantConstruction at the call site.
 * work(..., true) completes the build in the current call (server stop, switch turned off).
 */
public final class BudgetedBuild {
	private static final TicketType<ChunkPos> TICKET = TicketType.create("ancientwarfare_structure_build", Comparator.comparingLong(ChunkPos::toLong));
	static final int CHUNK_WAIT_TICKS = 200;

	private final StructureBuilder builder;
	@Nullable
	private final Runnable before;
	@Nullable
	private final Runnable after;
	private final boolean logInfo;
	private List<ChunkPos> tickets;
	private final int chunkMinX;
	private final int chunkMinZ;
	private final int chunkMaxX;
	private final int chunkMaxZ;
	private int chunksMissingAtStart;
	private ServerLevel ticketLevel;
	private int waitedTicks;
	private boolean started;
	private boolean done;
	private boolean failed;
	private int ticks;
	private int placingTicks;
	private long placingNanos;
	private long longestSliceNanos;
	private long firstSliceNanos;
	private long lastSliceNanos;

	public BudgetedBuild(StructureBuilder builder, @Nullable Runnable before, @Nullable Runnable after, boolean logInfo) {
		this.builder = builder;
		this.before = before;
		this.after = after;
		this.logInfo = logInfo;
		StructureBB bb = builder.getBoundingBox();
		int border = builder.getChunkMargin();
		chunkMinX = (bb.min.getX() - border) >> 4;
		chunkMinZ = (bb.min.getZ() - border) >> 4;
		chunkMaxX = (bb.max.getX() + border) >> 4;
		chunkMaxZ = (bb.max.getZ() + border) >> 4;
	}

	boolean coversChunk(net.minecraft.world.level.Level level, int chunkX, int chunkZ) {
		return !done && level == builder.getWorld() && chunkX >= chunkMinX && chunkX <= chunkMaxX && chunkZ >= chunkMinZ && chunkZ <= chunkMaxZ;
	}

	public StructureBuilder getBuilder() {
		return builder;
	}

	public boolean isDone() {
		return done;
	}

	public boolean isFailed() {
		return failed;
	}

	public boolean isStarted() {
		return started;
	}

	/* ticks on which blocks were placed */
	public int getPlacingTicks() {
		return placingTicks;
	}

	public long getLongestSliceNanos() {
		return longestSliceNanos;
	}

	/*
	 * returns true when the build is complete (or failed; logged); deadline in System.nanoTime
	 */
	public boolean work(long deadlineNanos, boolean unlimited) {
		if (done) {
			return true;
		}
		ticks++;
		try {
			if (tickets == null) {
				addTickets();
			}
			if (!started) {
				if (!unlimited && waitedTicks < CHUNK_WAIT_TICKS && !allChunksLoaded()) {
					waitedTicks++;
					return false;
				}
				started = true;
				chunksMissingAtStart = countMissingChunks();
				if (before != null) {
					before.run();
				}
			}
			long start = System.nanoTime();
			boolean finished = builder.constructUntil(deadlineNanos, unlimited);
			long time = System.nanoTime() - start;
			placingTicks++;
			placingNanos += time;
			longestSliceNanos = Math.max(longestSliceNanos, time);
			if (placingTicks == 1) {
				firstSliceNanos = time;
			}
			lastSliceNanos = time;
			if (!finished) {
				return false;
			}
			if (after != null) {
				after.run();
			}
			int ticketCount = tickets == null ? 0 : tickets.size();
			finish();
			String name = builder.getTemplate().name;
			String steps = "preparation " + builder.getBeforeCalls() + " ticks " + builder.getBeforeNanos() / 1000000 + " ms (longest " + builder.getLongestBeforeNanos() / 1000000 + " ms), after " + builder.getAfterCalls() + " ticks " + builder.getAfterNanos() / 1000000 + " ms (longest " + builder.getLongestAfterNanos() / 1000000 + " ms)";
			if (logInfo) {
				AncientWarfareStructure.LOG.info("Budgeted build of {} complete: {} ticks ({} waiting for chunks, {} of {} chunks still missing then, {} placing), {} ms placing in total, longest slice {} ms (first {} ms, last {} ms); {}", name, ticks, waitedTicks, chunksMissingAtStart, ticketCount, placingTicks, placingNanos / 1000000, longestSliceNanos / 1000000, firstSliceNanos / 1000000, lastSliceNanos / 1000000, steps);
			} else {
				AncientWarfareStructure.LOG.debug("Budgeted build of {} complete: {} ticks ({} waiting for chunks, {} chunks still missing then, {} placing), {} ms placing in total, longest slice {} ms (first {} ms, last {} ms); {}", name, ticks, waitedTicks, chunksMissingAtStart, placingTicks, placingNanos / 1000000, longestSliceNanos / 1000000, firstSliceNanos / 1000000, lastSliceNanos / 1000000, steps);
			}
			return true;
		}
		catch (Exception ex) {
			AncientWarfareStructure.LOG.error("Error building structure {}: ", builder.getTemplate().name, ex);
			failed = true;
			finish();
			return true;
		}
	}

	/*
	 * drops the build without placing the rest (only when its level is gone)
	 */
	void abandon() {
		finish();
	}

	private void finish() {
		done = true;
		StructurePlacementQueue.releaseArea(this);
		if (tickets != null && ticketLevel != null) {
			for (ChunkPos pos : tickets) {
				ticketLevel.getChunkSource().removeRegionTicket(TICKET, pos, 0, pos);
			}
		}
		tickets = null;
		ticketLevel = null;
	}

	private void addTickets() {
		tickets = new ArrayList<>();
		if (!(builder.getWorld() instanceof ServerLevel level)) {
			return;
		}
		ticketLevel = level;
		StructurePlacementQueue.reserveArea(this);
		for (int x = chunkMinX; x <= chunkMaxX; x++) {
			for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
				ChunkPos pos = new ChunkPos(x, z);
				level.getChunkSource().addRegionTicket(TICKET, pos, 0, pos);
				tickets.add(pos);
			}
		}
	}

	private boolean allChunksLoaded() {
		return countMissingChunks() == 0;
	}

	private int countMissingChunks() {
		if (ticketLevel == null) {
			return 0;
		}
		int missing = 0;
		for (ChunkPos pos : tickets) {
			if (ticketLevel.getChunkSource().getChunkNow(pos.x, pos.z) == null) {
				missing++;
			}
		}
		return missing;
	}
}
