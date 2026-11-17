package net.shadowmage.ancientwarfare.automation.chunkloader;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.common.world.chunk.TicketSet;
import net.shadowmage.ancientwarfare.automation.AncientWarfareAutomation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * Port notes: 1.12 ForgeChunkManager (ticket objects, LoadingCallback) -> NeoForge TicketController. NeoForge has no
 * ticket objects: chunks are forced per owning block position and persist with the world. To keep the tile code
 * close to 1.12 this class provides a small Ticket handle with the ForgeChunkManager calls the tiles used:
 *   ForgeChunkManager.requestTicket(mod, world, NORMAL) -> AWChunkLoader.requestTicket(level, tilePos)
 *   ForgeChunkManager.forceChunk / unforceChunk / releaseTicket -> same names here (null ticket is a no-op)
 * 1.12 handed the saved ticket back to the tile after a world load (ticketsLoaded -> IChunkLoaderTile.setTicket).
 * Here the chunks forced for a position are remembered when the world loads (validateTickets) and the next
 * requestTicket for that position starts with them, so the tile re-requests its ticket in onLoad and a later
 * releaseTicket also frees chunks forced before the restart. writeDataToTicket is a no-op (the owner position is
 * the ticket key).
 */
public final class AWChunkLoader {
	public static final AWChunkLoader INSTANCE = new AWChunkLoader();

	private static final Map<ResourceKey<Level>, Map<BlockPos, Set<ChunkPos>>> LOADED_TICKETS = new HashMap<>();

	private static final TicketController CONTROLLER = new TicketController(ResourceLocation.fromNamespaceAndPath(AncientWarfareAutomation.MOD_ID, "automation"), AWChunkLoader::ticketsLoaded);

	private AWChunkLoader() {
	}

	public static void register(IEventBus modBus) {
		modBus.addListener((RegisterTicketControllersEvent evt) -> evt.register(CONTROLLER));
	}

	private static void ticketsLoaded(ServerLevel level, TicketHelper ticketHelper) {
		Map<BlockPos, Set<ChunkPos>> loaded = new HashMap<>();
		for (Map.Entry<BlockPos, TicketSet> entry : ticketHelper.getBlockTickets().entrySet()) {
			Set<ChunkPos> chunks = new HashSet<>();
			entry.getValue().ticking().forEach(l -> chunks.add(new ChunkPos(l)));
			entry.getValue().nonTicking().forEach(l -> chunks.add(new ChunkPos(l)));
			loaded.put(entry.getKey().immutable(), chunks);
		}
		LOADED_TICKETS.put(level.dimension(), loaded);
	}

	/*
	 * Server side only, returns null on the client.
	 */
	@Nullable
	public static Ticket requestTicket(@Nullable Level world, BlockPos owner) {
		if (!(world instanceof ServerLevel)) {
			return null;
		}
		Ticket ticket = new Ticket((ServerLevel) world, owner.immutable());
		Map<BlockPos, Set<ChunkPos>> loaded = LOADED_TICKETS.get(world.dimension());
		if (loaded != null) {
			Set<ChunkPos> chunks = loaded.remove(ticket.owner);
			if (chunks != null) {
				for (ChunkPos chunk : chunks) {
					if (ticket.chunks.size() < MAX_CHUNKS_PER_TICKET) {
						ticket.chunks.add(chunk);
					} else {
						CONTROLLER.forceChunk(ticket.level, ticket.owner, chunk.x, chunk.z, false, true);
					}
				}
			}
		}
		return ticket;
	}

	/*
	 * Forge 1.12 allowed 25 chunks per ticket (maximumChunksPerTicket) and dropped the oldest one beyond that;
	 * NeoForge has no such limit, so worksite bounds or a chunk loader could force any number of chunks for good.
	 */
	public static final int MAX_CHUNKS_PER_TICKET = 25;

	public static void forceChunk(@Nullable Ticket ticket, ChunkPos chunk) {
		if (ticket != null && ticket.chunks.add(chunk)) {
			CONTROLLER.forceChunk(ticket.level, ticket.owner, chunk.x, chunk.z, true, true);
			while (ticket.chunks.size() > MAX_CHUNKS_PER_TICKET) {
				ChunkPos oldest = ticket.chunks.iterator().next();
				ticket.chunks.remove(oldest);
				CONTROLLER.forceChunk(ticket.level, ticket.owner, oldest.x, oldest.z, false, true);
			}
		}
	}

	public static void unforceChunk(@Nullable Ticket ticket, ChunkPos chunk) {
		if (ticket != null && ticket.chunks.remove(chunk)) {
			CONTROLLER.forceChunk(ticket.level, ticket.owner, chunk.x, chunk.z, false, true);
		}
	}

	public static void releaseTicket(@Nullable Ticket ticket) {
		if (ticket != null) {
			for (ChunkPos chunk : ticket.chunks) {
				CONTROLLER.forceChunk(ticket.level, ticket.owner, chunk.x, chunk.z, false, true);
			}
			ticket.chunks.clear();
		}
	}

	public void writeDataToTicket(@Nullable Ticket tk, BlockPos pos) {
	}

	public static final class Ticket {
		private final ServerLevel level;
		private final BlockPos owner;
		private final Set<ChunkPos> chunks = new java.util.LinkedHashSet<>(); //oldest first

		private Ticket(ServerLevel level, BlockPos owner) {
			this.level = level;
			this.owner = owner;
		}

		public BlockPos getOwner() {
			return owner;
		}

		public Set<ChunkPos> getChunkList() {
			return chunks;
		}
	}
}
