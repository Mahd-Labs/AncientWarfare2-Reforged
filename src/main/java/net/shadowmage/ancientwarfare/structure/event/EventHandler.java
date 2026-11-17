package net.shadowmage.ancientwarfare.structure.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureMap;
import net.shadowmage.ancientwarfare.structure.network.PacketStructureEntry;

public class EventHandler {
	private EventHandler() {}

	public static final EventHandler INSTANCE = new EventHandler();

	@SubscribeEvent
	public void onChunkWatch(net.neoforged.neoforge.event.level.ChunkWatchEvent.Sent evt) {
		net.minecraft.world.level.ChunkPos chunk = evt.getPos();
		ServerPlayer player = evt.getPlayer();

		StructureMap.get(player.level()).getStructureAt(player.level(), chunk.x, chunk.z)
				.ifPresent(structureEntry -> NetworkHandler.sendToPlayer(player,
						new PacketStructureEntry(StructureMap.dim(player.level()), chunk.x, chunk.z, structureEntry)
				));
	}
}
