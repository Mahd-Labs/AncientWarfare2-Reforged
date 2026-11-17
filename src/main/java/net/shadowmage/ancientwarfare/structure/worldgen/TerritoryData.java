package net.shadowmage.ancientwarfare.structure.worldgen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.gamedata.AWSavedData;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/*
 * Port notes: 1.12 kept this as a world capability (CapabilityTerritoryData, saved with the world's capability data).
 * World capabilities are gone; it is per dimension saved data now (data/AWTerritoryData.dat of the dimension), same
 * NBT layout as the 1.12 capability tag.
 */
public class TerritoryData extends AWSavedData implements ITerritoryData {
	public TerritoryData(String name) {
		super(name);
	}

	public static TerritoryData get(Level world) {
		return AWGameData.INSTANCE.getPerWorldData(world, TerritoryData.class);
	}

	private Map<String, Territory> territories = new HashMap<>();
	private Map<Long, Territory> chunkOwnership = new HashMap<>();
	private Map<String, Integer> territoryIds = new HashMap<>();

	@Override
	public int getNextTerritoryId(String territoryName) {
		int id = territoryIds.getOrDefault(territoryName, 0);
		territoryIds.put(territoryName, ++id);
		markDirty();
		return id;
	}

	@Override
	public Optional<Territory> getTerritory(long chunkPosValue) {
		return Optional.ofNullable(chunkOwnership.get(chunkPosValue));
	}

	@Override
	public boolean isOwned(long chunkPos) {
		return chunkOwnership.containsKey(chunkPos);
	}

	@Override
	public void setOwned(long chunkPos, String territoryId, String territoryName) {
		Territory territory = territories.getOrDefault(territoryId, new Territory(territoryId, territoryName));
		territory.setOnChange(this::markDirty);
		territory.addChunk(chunkPos);
		territories.put(territoryId, territory);
		chunkOwnership.put(chunkPos, territory);
		markDirty();
	}

	@Override
	public boolean isDifferentTerritory(String territoryId, int x, int z) {
		long chunkPosValue = ChunkPos.asLong(x, z);
		return !chunkOwnership.containsKey(chunkPosValue) || !chunkOwnership.get(chunkPosValue).getTerritoryId().equals(territoryId);
	}

	@Override
	public CompoundTag writeToNBT(CompoundTag ret) {
		ret.put("territories", NBTHelper.getTagList(territories.values(), t -> t.serializeNBT()));
		ret.put("territoryIds", NBTHelper.mapToCompoundList(territoryIds, (t, k) -> t.putString("territoryName", k), (t, v) -> t.putInt("id", v)));
		return ret;
	}

	@Override
	public void readFromNBT(CompoundTag nbt) {
		territories.clear();
		territoryIds.clear();
		chunkOwnership.clear();

		Set<Territory> territorySet = NBTHelper.getSet(nbt.getList("territories", Tag.TAG_COMPOUND), n -> {
			Territory territory = new Territory();
			territory.deserializeNBT((CompoundTag) n);
			territory.setOnChange(this::markDirty);
			return territory;
		});

		for (Territory t : territorySet) {
			territories.put(t.getTerritoryId(), t);
			for (long chunkPos : t.getChunkPositions()) {
				chunkOwnership.put(chunkPos, t);
			}
		}
		territoryIds = NBTHelper.getMap(nbt.getList("territoryIds", Tag.TAG_COMPOUND), n -> n.getString("territoryName"), n -> n.getInt("id"));
	}
}
