package net.shadowmage.ancientwarfare.core.interfaces;

/*
 * Port note: 1.12 had setTicket(ForgeChunkManager.Ticket). NeoForge 1.21.1 has no ticket objects (chunks are forced
 * through a TicketController keyed by the owning block position), so this is a marker interface now.
 */
public interface IChunkLoaderTile {
}
