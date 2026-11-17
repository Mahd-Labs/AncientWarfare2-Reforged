package net.shadowmage.ancientwarfare.core.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/*
 * Port notes: base of every AW block entity. Keeps the 1.12 method names so ported tiles stay close to the original:
 *   readFromNBT(tag) / writeToNBT(tag)   <- loadAdditional / saveAdditional
 *   writeUpdateNBT(tag) / handleUpdateNBT(tag) <- chunk sync tag + update packet
 *   markDirty() -> setChanged(), pos -> getBlockPos(), world -> getLevel()
 * Item stacks inside tags are (de)serialized with NBTHelper, which resolves the registry lookup globally.
 * Ticking tiles implement ITickableTile and their block returns the ticker (see BlockBase.createTickerFor).
 */
public abstract class TileUpdatable extends BlockEntity {

	public TileUpdatable(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public void readFromNBT(CompoundTag tag) {
	}

	public void writeToNBT(CompoundTag tag) {
	}

	@Override
	protected final void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		readFromNBT(tag);
	}

	@Override
	protected final void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		writeToNBT(tag);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
		CompoundTag tag = pkt.getTag();
		if (tag != null) {
			handleUpdateNBT(tag);
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		writeUpdateNBT(tag);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		handleUpdateNBT(tag);
	}

	protected void writeUpdateNBT(CompoundTag tag) {
	}

	protected void handleUpdateNBT(CompoundTag tag) {
	}

	/* 1.12 alias */
	public void markDirty() {
		setChanged();
	}

	/*
	 * 1.12 BlockTools.notifyBlockUpdate equivalent for this tile: re-sends the update tag to tracking clients.
	 */
	public void sendUpdateToClients() {
		if (level != null && !level.isClientSide) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, 3);
		}
	}
}
