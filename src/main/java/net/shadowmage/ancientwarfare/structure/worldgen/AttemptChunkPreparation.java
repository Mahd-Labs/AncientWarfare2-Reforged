package net.shadowmage.ancientwarfare.structure.worldgen;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/*
 * Improvement (not baseline, background_attempt_chunks). Prepares the chunks of the structure attempt at the head of
 * WorldGenTickHandler's attempt queue before the attempt runs.
 *
 * The attempt itself is not changed and still reads the world through Level.getBlockState. What changes is the state
 * the chunks it reads are in: a chunk that does not exist yet costs the whole terrain generation (noise, surface,
 * features, light) on the server thread when the attempt reads it. Here the chunks the attempt will read (found by
 * WorldStructureGenerator.planAttemptChunks) plus one chunk around them get a ticket of this mod at the ticket level of
 * the status INITIALIZE_LIGHT (level 34, below FULL) and, once the ticket is in effect, a generation request up to
 * that status (ChunkHolder.scheduleChunkGenerationTask; a ticket below FULL alone generates nothing): Minecraft's chunk
 * workers generate them up to and including their features in the background, and the attempt waits in the queue
 * until all of them are there. When it then runs,
 * a chunk it reads only needs the last steps (light, spawn, full) on the server thread. Chunks are not made FULL by the
 * ticket, so exactly the chunks the original attempt reads become full chunks (and get their own world generation
 * attempts), in the same order, as with the switch off.
 *
 * Bounds: one attempt (the queue head) holds tickets at a time; it waits at most ATTEMPT_WAIT_TICKS server ticks (also
 * counting ticks in which the queue did not run) and then runs as before, reading whatever is missing synchronously.
 * Tickets are removed when the attempt has run, when a different attempt becomes the head, at server stop and when the
 * level unloads; they also expire on their own after TICKET_LIFESPAN ticks (Minecraft removes them) in case anything
 * is missed. Nothing ever waits on a chunk future: readiness is polled once per tick.
 */
public final class AttemptChunkPreparation {
	public static final AttemptChunkPreparation INSTANCE = new AttemptChunkPreparation();
	/* how long an attempt waits for its chunks before it runs the original way */
	public static final int ATTEMPT_WAIT_TICKS = 400;
	private static final int TICKET_LIFESPAN = 1200;
	private static final TicketType<ChunkPos> TICKET = TicketType.create("ancientwarfare_attempt", Comparator.comparingLong(ChunkPos::toLong), TICKET_LIFESPAN);
	//ticket level 34: generation up to INITIALIZE_LIGHT, the chunk is not made FULL
	private static final int TICKET_LEVEL = ChunkLevel.byStatus(ChunkStatus.INITIALIZE_LIGHT);

	private Object owner;
	private ServerLevel level;
	private long startTick;
	private final Set<Long> ticketed = new HashSet<>();
	//statistics for the log line at server stop
	private int attemptsWaited;
	private int attemptsTimedOut;
	private long longestWaitTicks;
	private int chunksRequested;

	/* public for the GameTests, which use an instance of their own; the world generator uses INSTANCE */
	public AttemptChunkPreparation() {
	}

	/*
	 * true when the attempt 'owner' (the queue ticket) may run now: its chunks are ready or it has waited
	 * ATTEMPT_WAIT_TICKS. Otherwise the missing chunks get tickets and false is returned; the caller keeps the attempt at
	 * the head of its queue and asks again next tick.
	 */
	public boolean isReady(Object owner, ServerLevel level, int chunkX, int chunkZ, long tick) {
		if (owner != this.owner || level != this.level) {
			release();
			this.owner = owner;
			this.level = level;
			this.startTick = tick;
		}
		long waited = tick - startTick;
		if (waited >= ATTEMPT_WAIT_TICKS) {
			attemptsTimedOut++;
			AncientWarfareStructure.LOG.debug("Structure attempt at chunk {} {} waited {} ticks (since tick {}) for its chunks, runs without them; {} chunk tickets, not ready: {}", chunkX, chunkZ, waited, startTick, ticketed.size(), describeMissing());
			return finish(waited);
		}
		try {
			if (!prepare(WorldStructureGenerator.INSTANCE.getSelectionChunks(chunkX, chunkZ))) {
				return false;
			}
			if (!prepare(WorldStructureGenerator.INSTANCE.planAttemptChunks(chunkX, chunkZ, level))) {
				return false;
			}
		}
		catch (RuntimeException ex) {
			//the plan could not be made: the attempt runs the original way at once
			AncientWarfareStructure.LOG.warn("Could not plan the chunks of the structure attempt at chunk {} {}, it runs without waiting", chunkX, chunkZ, ex);
		}
		return finish(waited);
	}

	private boolean finish(long waited) {
		if (waited > 0) {
			attemptsWaited++;
			longestWaitTicks = Math.max(longestWaitTicks, waited);
		}
		return true;
	}

	/* the attempt has run: its tickets are released */
	public void done(Object owner) {
		if (owner == this.owner) {
			release();
		}
	}

	public void onLevelUnload(Level unloaded) {
		if (unloaded == level) {
			release();
		}
	}

	/* removes every ticket of this class (server stop, level unload, a new head, the attempt has run) */
	public void release() {
		if (level != null) {
			for (long pos : ticketed) {
				ChunkPos chunkPos = new ChunkPos(pos);
				level.getChunkSource().chunkMap.getDistanceManager().removeTicket(TICKET, chunkPos, TICKET_LEVEL, chunkPos);
			}
		}
		ticketed.clear();
		owner = null;
		level = null;
	}

	/* GameTests: the ticket type (to look for left-over tickets) */
	public static TicketType<ChunkPos> getTicketType() {
		return TICKET;
	}

	/* GameTests: the chunks that have a ticket of this instance */
	public java.util.List<Long> getTicketedChunks() {
		return new java.util.ArrayList<>(ticketed);
	}

	public int getTicketCount() {
		return ticketed.size();
	}

	public String getSummary() {
		return attemptsWaited + " structure attempts waited for chunks (longest " + longestWaitTicks + " ticks, " + attemptsTimedOut + " ran without all of them), " + chunksRequested + " chunk tickets";
	}

	/*
	 * true when every chunk of the set and the chunks around them are full or generated to at least INITIALIZE_LIGHT;
	 * adds tickets for the others
	 */
	private boolean prepare(Set<Long> chunks) {
		Set<Long> withMargin = new HashSet<>();
		for (long pos : chunks) {
			int x = ChunkPos.getX(pos);
			int z = ChunkPos.getZ(pos);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					withMargin.add(ChunkPos.asLong(x + dx, z + dz));
				}
			}
		}
		boolean ready = true;
		for (long pos : withMargin) {
			if (isPrepared(pos)) {
				continue;
			}
			ready = false;
			if (ticketed.add(pos)) {
				ChunkPos chunkPos = new ChunkPos(pos);
				level.getChunkSource().chunkMap.getDistanceManager().addTicket(TICKET, chunkPos, TICKET_LEVEL, chunkPos);
				chunksRequested++;
			} else {
				//a ticket below FULL only keeps a chunk loaded; generation up to INITIALIZE_LIGHT is requested here once the
				//ticket has taken effect (the returned future is not waited on, readiness is polled next tick)
				ChunkHolder holder = level.getChunkSource().chunkMap.getVisibleChunkIfPresent(pos);
				if (holder != null && holder.getTicketLevel() <= TICKET_LEVEL) {
					holder.scheduleChunkGenerationTask(ChunkStatus.INITIALIZE_LIGHT, level.getChunkSource().chunkMap);
				}
			}
		}
		return ready;
	}

	/* diagnostics: the first chunks with a ticket that are not ready, with their state */
	private String describeMissing() {
		StringBuilder text = new StringBuilder();
		int n = 0;
		for (long pos : ticketed) {
			if (isPrepared(pos)) {
				continue;
			}
			if (++n > 5) {
				text.append(" ...");
				break;
			}
			ChunkHolder holder = level.getChunkSource().chunkMap.getVisibleChunkIfPresent(pos);
			text.append(' ').append(new ChunkPos(pos)).append(holder == null ? " no holder" : " level " + holder.getTicketLevel() + " status " + holder.getLatestStatus());
		}
		return text.toString();
	}

	private boolean isPrepared(long pos) {
		if (level.getChunkSource().getChunkNow(ChunkPos.getX(pos), ChunkPos.getZ(pos)) != null) {
			return true;
		}
		ChunkHolder holder = level.getChunkSource().chunkMap.getVisibleChunkIfPresent(pos);
		if (holder == null) {
			return false;
		}
		ChunkStatus status = holder.getLatestStatus();
		return status != null && status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT);
	}
}
