package net.shadowmage.ancientwarfare.structure.worldgen;


import java.util.Optional;

public interface ITerritoryData {
	int getNextTerritoryId(String territoryName);

	Optional<Territory> getTerritory(long chunkPosValue);

	boolean isOwned(long chunkPos);

	void setOwned(long chunkPos, String territoryId, String territoryName);

	boolean isDifferentTerritory(String territoryId, int x, int z);
}
