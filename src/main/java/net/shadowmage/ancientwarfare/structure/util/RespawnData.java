package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/*
 * Port notes: also carries the (de)serialisation the 1.12 Capability.IStorage did (same keys).
 */
public class RespawnData implements IRespawnData, INBTSerializable<CompoundTag> {
	private BlockPos respawnPos = BlockPos.ZERO;
	private CompoundTag spawnerSettings = new CompoundTag();
	private boolean canRespawn = false;
	private long spawnTime = 0;

	@Override
	public boolean canRespawn() {
		return canRespawn;
	}

	@Override
	public BlockPos getRespawnPos() {
		return respawnPos;
	}

	@Override
	public CompoundTag getSpawnerSettings() {
		return spawnerSettings;
	}

	@Override
	public long getSpawnTime() {
		return spawnTime;
	}

	@Override
	public void setRespawnPos(BlockPos pos) {
		respawnPos = pos;
		canRespawn = !respawnPos.equals(BlockPos.ZERO);
	}

	@Override
	public void setSpawnerSettings(CompoundTag tag) {
		spawnerSettings = tag;
	}

	@Override
	public void setSpawnTime(long time) {
		spawnTime = time;
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider provider) {
		CompoundTag tag = new CompoundTag();
		tag.putLong("respawnPos", respawnPos.asLong());
		tag.put("spawnerSettings", spawnerSettings);
		tag.putLong("spawnTime", spawnTime);
		return tag;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
		setRespawnPos(BlockPos.of(tag.getLong("respawnPos")));
		spawnerSettings = tag.getCompound("spawnerSettings");
		spawnTime = tag.getLong("spawnTime");
	}
}
