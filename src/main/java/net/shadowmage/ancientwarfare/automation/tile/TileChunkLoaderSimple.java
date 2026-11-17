package net.shadowmage.ancientwarfare.automation.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.chunkloader.AWChunkLoader;
import net.shadowmage.ancientwarfare.automation.chunkloader.AWChunkLoader.Ticket;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;
import net.shadowmage.ancientwarfare.core.interfaces.IChunkLoaderTile;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;

import javax.annotation.Nullable;

/*
 * Port notes: ForgeChunkManager -> AWChunkLoader (NeoForge TicketController). 1.12 got its ticket back from the
 * chunk manager after a world load (IChunkLoaderTile.setTicket); now the tile requests its ticket in onLoad and forces
 * its chunks again (this also covers blocks placed without a placer, e.g. /setblock). 1.12 invalidate() released the
 * ticket only when the block was removed (chunk unloads went through onChunkUnload); setRemoved is called for both,
 * so the release is skipped after onChunkUnloaded. Extends TileUpdatable (1.12: TileEntity) for the NBT hooks.
 */



public class TileChunkLoaderSimple extends TileUpdatable implements IChunkLoaderTile {

	Ticket chunkTicket = null;
	private boolean chunkUnloaded = false;

	public TileChunkLoaderSimple(BlockPos pos, BlockState state) {
		this(AWAutomationBlocks.CHUNK_LOADER_SIMPLE_TILE.get(), pos, state);
	}

	protected TileChunkLoaderSimple(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);

	}

	public void releaseTicket() {
		AWChunkLoader.releaseTicket(chunkTicket);
		chunkTicket = null;
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null && !level.isClientSide && !chunkUnloaded) {
			releaseTicket();
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		chunkUnloaded = true;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide && chunkTicket == null) {
			setupInitialTicket();
		}
	}

	public void setTicket(@Nullable Ticket tk) {
		if (this.chunkTicket != tk) {
			releaseTicket();
			if (tk != null) {
				this.chunkTicket = tk;
				forceTicketChunks();
			}
		}
	}

	public void setupInitialTicket() {
		this.chunkTicket = AWChunkLoader.requestTicket(level, worldPosition);
		if (this.chunkTicket != null) {
			writeDataToTicket();
			forceTicketChunks();
		}
	}

	protected void writeDataToTicket() {
		AWChunkLoader.INSTANCE.writeDataToTicket(chunkTicket, worldPosition);
	}

	protected void forceTicketChunks() {
		int cx = worldPosition.getX() >> 4;
		int cz = worldPosition.getZ() >> 4;
		for (int x = cx - 1; x <= cx + 1; x++) {
			for (int z = cz - 1; z <= cz + 1; z++) {
				ChunkPos chunkPos = new ChunkPos(x, z);
				AWChunkLoader.forceChunk(this.chunkTicket, chunkPos);
			}
		}
		//TODO either uncomment and log chunk loading info or just remove this
		//  AWLog.logDebug("ticket now has chunks: "+tk.getChunkList());
		//  AWLog.logDebug("total forced chunks are: "+AWChunkLoader.getPersistentChunksFor(level));
	}
}
