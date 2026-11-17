package net.shadowmage.ancientwarfare.structure.tile;

import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.util.INBTSerializable;
import net.shadowmage.ancientwarfare.core.interfaces.ISinger;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.network.PacketSoundBlockPlayerSpecValues;
import net.shadowmage.ancientwarfare.structure.util.BlockSongPlayData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TileSoundBlock extends TileUpdatable implements ISinger, ITickableTile {
	private static final long MIN_TIME_BETWEEN_ENTRY_PLAYS = 1200;
	private int currentDelay;
	private int tuneIndex = -1;
	private int playerCheckDelay;
	private BlockSongPlayData tuneData;
	/* 1.12 BlockSoundBlock.DISGUISE_BLOCK (unlisted property of the extended state) -> model data read by SoundBlockRenderer */
	public static final net.neoforged.neoforge.client.model.data.ModelProperty<BlockState> DISGUISE_BLOCK = new net.neoforged.neoforge.client.model.data.ModelProperty<>();
	private BlockState disguiseState;

	@Override
	public net.neoforged.neoforge.client.model.data.ModelData getModelData() {
		return disguiseState == null ? net.neoforged.neoforge.client.model.data.ModelData.EMPTY
				: net.neoforged.neoforge.client.model.data.ModelData.builder().with(DISGUISE_BLOCK, disguiseState).build();
	}

	private boolean stoppedForAll = false;
	private Map<UUID, PersistentValues> playerSpecificValues = new HashMap<>();

	public TileSoundBlock(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.SOUND_BLOCK_TILE.get(), pos, state);
	}

	protected TileSoundBlock(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		tuneData = new BlockSongPlayData();
	}

	@Override
	public void update() {
		if (!level.isClientSide || isStopped() || validateAndGetPlaying() || !tuneData.getTimeOfDay().takesPlaceNow(level)) {
			return;
		}

		if (tuneData.getPlayOnPlayerEntry()) {
			processPlayerEntry();
		} else {
			if (tuneData.getWhenInRange() && !isPlayerInRange()) {
				return;
			}

			if (--currentDelay <= 0) {
				startSong();
				if (tuneData.getLimitedRepetitions() && incNumberOfTimesRepeated() >= tuneData.getRepetitions()) {
					setStopped();
				}
			}
		}
	}

	private int incNumberOfTimesRepeated() {
		PersistentValues values = getOrCreatePlayerSpecificValues();
		values.numberOfTimesRepeated++;
		syncValuesToServer(values);
		return values.numberOfTimesRepeated;
	}

	private void setStopped() {
		if (level.isClientSide) {
			PersistentValues values = getOrCreatePlayerSpecificValues();
			values.stopped = true;
			syncValuesToServer(values);
		}
	}

	private void syncValuesToServer(PersistentValues values) {
		NetworkHandler.sendToServer(new PacketSoundBlockPlayerSpecValues(worldPosition, values));
	}

	private boolean isStopped() {
		return stoppedForAll || level.isClientSide && getPlayerSpecificValues().map(values -> values.stopped).orElse(false);
	}

	private PersistentValues getOrCreatePlayerSpecificValues() {
		return net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().getPlayer().map(player -> {
			if (!playerSpecificValues.containsKey(player.getUUID())) {
				playerSpecificValues.put(player.getUUID(), new PersistentValues());
			}
			return playerSpecificValues.get(player.getUUID());
		}).orElse(new PersistentValues());
	}

	private Optional<PersistentValues> getPlayerSpecificValues() {
		return net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().getPlayer().map(player -> Optional.ofNullable(playerSpecificValues.get(player.getUUID())))
				.orElse(Optional.empty());
	}

	public void turnOffByProtectionFlag() {
		if (tuneData.getProtectionFlagTurnOff()) {
			stoppedForAll = true;
			setChanged();
			BlockTools.notifyBlockUpdate(this);
		}
	}

	private void processPlayerEntry() {
		if (playerCheckDelay-- <= 0) {
			playerCheckDelay = 20;
			if (isPlayerInRange()) {
				if (getLastTimePlayerNear() < 0 || level.getGameTime() - getLastTimePlayerNear() > MIN_TIME_BETWEEN_ENTRY_PLAYS) {
					startSong();
					if (tuneData.getPlayOnce()) {
						setStopped();
					} else {
						setLastTimePlayerNear();
					}
				} else {
					setLastTimePlayerNear();
				}
				setChanged();
			}
		}
	}

	private void setLastTimePlayerNear() {
		PersistentValues values = getOrCreatePlayerSpecificValues();
		values.lastTimePlayerNear = level.getGameTime();
		syncValuesToServer(values);
	}

	private long getLastTimePlayerNear() {
		return getPlayerSpecificValues().map(PersistentValues::getLastTimePlayerNear).orElse(Long.MAX_VALUE);
	}

	private boolean isPlayerInRange() {
		return net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().getClientPlayerDistanceTo(worldPosition) <= tuneData.getPlayerRange();
	}

	private void startSong() {
		if (tuneData.size() == 0) {
			return;
		}

		if (tuneData.getIsRandom()) {
			tuneIndex = 0;
			if (tuneData.size() > 0) {
				tuneIndex = level.random.nextInt(tuneData.size());
				tuneData.get(tuneIndex).getSound().ifPresent(s -> net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().setSoundAt(worldPosition, s, tuneData.getSoundRange() / 16f));
			}
		} else {
			tuneIndex = tuneIndex + 1 < tuneData.size() ? tuneIndex + 1 : 0;
			tuneData.get(tuneIndex).getSound().ifPresent(s -> net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().setSoundAt(worldPosition, s, tuneData.getSoundRange() / 16f));
		}
		net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().playSoundAt(worldPosition);
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		writeToNBT(tag);
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		readFromNBT(tag);

		if (isStopped() && net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().isSoundPlayingAt(worldPosition)) {
			net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().stopSoundAt(worldPosition);
		}
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		tuneData.readFromNBT(tag.getCompound("tuneData"));
		tuneIndex = tag.getInt("tuneIndex");
		if (tag.contains("range")) {
			tuneData.setPlayerRange(tag.getInt("range"));
		}
		//Port notes: 1.12 saved block id + meta; the port saves the block state tag
		if (tag.contains("disguiseState")) {
			disguiseState = NBTHelper.getBlockState(tag.getCompound("disguiseState"));
		}
		if (level != null && level.isClientSide) {
			requestModelDataUpdate();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
		}
		stoppedForAll = tag.getBoolean("stoppedForAll");
		readPlayerSpecificValues(tag);
	}

	private void readPlayerSpecificValues(CompoundTag tag) {
		playerSpecificValues.clear();
		ListTag list = tag.getList("playerSpecificValues", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag pair = list.getCompound(i);
			PersistentValues values = new PersistentValues();
			values.deserializeNBT(pair.getCompound("values"));
			if (pair.hasUUID("playerId")) { //data of 1.12 templates without a usable id is skipped
				playerSpecificValues.put(pair.getUUID("playerId"), values);
			}
		}
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		tag.put("tuneData", tuneData.writeToNBT(new CompoundTag()));
		tag.putInt("tuneIndex", tuneIndex);
		if (disguiseState != null) {
			tag.put("disguiseState", NBTHelper.getBlockStateTag(disguiseState));
		}
		tag.putBoolean("stoppedForAll", stoppedForAll);
		writePlayerSpecificValues(tag);
	}

	private void writePlayerSpecificValues(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Map.Entry<UUID, PersistentValues> entry : playerSpecificValues.entrySet()) {
			CompoundTag pair = new CompoundTag();
			pair.putUUID("playerId", entry.getKey());
			pair.put("values", entry.getValue().serializeNBT());
			list.add(pair);
		}
		tag.put("playerSpecificValues", list);
	}

	public BlockSongPlayData getSongs() {
		return tuneData;
	}

	public BlockState getDisguiseState() {
		return disguiseState;
	}

	public void setDisguiseState(ItemStack itemStack) {
		Block block = Block.byItem(itemStack.getItem());
		if (block != AWStructureBlocks.SOUND_BLOCK.get() && block.defaultBlockState().isSolidRender(level, worldPosition)) {
			//1.12 block.isFullCube(null) && block.isOpaqueCube(null); getStateFromMeta(item damage) -> default state of the flattened block
			disguiseState = block.defaultBlockState();
			BlockTools.notifyBlockUpdate(this);
			level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
			setChanged();
		}
	}

	private boolean validateAndGetPlaying() {
		if (!net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().hasSoundAt(worldPosition)) {
			return false;
		}
		boolean isPlaying = net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().isSoundPlayingAt(worldPosition);
		if (!isPlaying) {
			resetCurrentTune();
		}

		return isPlaying;
	}

	private void resetCurrentTune() {
		tuneIndex = -1;
		net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().resetSoundAt(worldPosition);
		if (!tuneData.getPlayOnPlayerEntry()) {
			int diff = Math.abs(tuneData.getMaxDelay() - tuneData.getMinDelay()) * 20;
			currentDelay = tuneData.getMinDelay() * 20 + (diff > 0 ? level.random.nextInt(diff) : 0);
		}
	}

	@Override
	public void setRemoved() {
		net.shadowmage.ancientwarfare.structure.util.StructureClientHooks.get().stopSoundAt(worldPosition);
		super.setRemoved();
	}

	public void updatePlayerSpecValues(UUID playerId, boolean stopped, long lastTimePlayerNear, int numberOfTimesRepeated) {
		if (!playerSpecificValues.containsKey(playerId)) {
			playerSpecificValues.put(playerId, new PersistentValues());
		}
		PersistentValues values = playerSpecificValues.get(playerId);
		values.stopped = stopped;
		values.lastTimePlayerNear = lastTimePlayerNear;
		values.numberOfTimesRepeated = numberOfTimesRepeated;
		setChanged();
	}

	public void resetStateValues() {
		stoppedForAll = false;
		playerSpecificValues.clear();
		BlockTools.notifyBlockUpdate(this);
	}

	public class PersistentValues implements INBTSerializable<CompoundTag> {
		private long lastTimePlayerNear = -1;
		private boolean stopped = false;
		private int numberOfTimesRepeated = 0;

		public boolean isStopped() {
			return stopped;
		}

		public int getNumberOfTimesRepeated() {
			return numberOfTimesRepeated;
		}

		public long getLastTimePlayerNear() {
			return lastTimePlayerNear;
		}

		@Override
	public CompoundTag serializeNBT() {
			CompoundTag tag = new CompoundTag();
			tag.putLong("lastTimePlayerNear", lastTimePlayerNear);
			tag.putBoolean("stopped", stopped);
			tag.putInt("numberOfTimesRepeated", numberOfTimesRepeated);
			return tag;
		}

		@Override
	public void deserializeNBT(CompoundTag tag) {
			lastTimePlayerNear = tag.getLong("lastTimePlayerNear");
			stopped = tag.getBoolean("stopped");
			numberOfTimesRepeated = tag.getInt("numberOfTimesRepeated");
		}
	}
}
