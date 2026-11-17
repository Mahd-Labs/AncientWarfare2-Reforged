package net.shadowmage.ancientwarfare.structure.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;
import net.shadowmage.ancientwarfare.structure.registry.TerritorySettingRegistry;
import net.shadowmage.ancientwarfare.structure.template.WorldGenStructureManager;
import net.shadowmage.ancientwarfare.structure.util.CollectionUtils;
import net.shadowmage.ancientwarfare.structure.util.LegacyBiomes;
import net.shadowmage.ancientwarfare.structure.worldgen.stats.WorldGenStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

/*
 * Port notes: biomes are keyed by their id string (1.12: Biome objects; the biome registry is per server in 1.21).
 * The biome of a chunk is the surface biome of its centre column taken from the biome source
 * (LegacyBiomes.getSurfaceBiome), so growing a territory does not load or generate chunks. The territory data is
 * saved data of the dimension (1.12: world capability). The 1.12 getTerritory fetched the chunk only to read its
 * coordinates back; dropped.
 */
public class TerritoryManager {
	private static HashMap<String, List<String>> territoryNamesByBiome = new HashMap<>();
	private static HashMap<String, Set<String>> biomesByTerritoryNames = new HashMap<>();

	private static final java.util.Random RNG = new java.util.Random();
	//diagnostics (debug log of slow new chunk processing): territories generated, chunks included, time spent
	public static int territoriesGenerated;
	public static int territoryChunks;
	public static long territoryNanos;

	private TerritoryManager() {}

	public static Optional<Territory> getTerritory(int chunkX, int chunkZ, Level world) {
		long chunkPosValue = ChunkPos.asLong(chunkX, chunkZ);
		if (world.isClientSide) {
			return Optional.empty();
		}
		ITerritoryData territoryData = TerritoryData.get(world);

		if (!territoryData.isOwned(chunkPosValue)) {
			long start = System.nanoTime();
			TerritoryGenerator generator = new TerritoryGenerator(world);
			generator.generateTerritory(chunkX, chunkZ, chunkPosValue, territoryData);
			territoriesGenerated++;
			territoryChunks += generator.includedChunks.size();
			territoryNanos += System.nanoTime() - start;
		}

		return territoryData.getTerritory(chunkPosValue);
	}

	static BlockPos getChunkCenterPos(int chunkX, int chunkZ) {
		int x = chunkX * 16 + 8;
		int z = chunkZ * 16 + 8;

		return new BlockPos(x, 1, z);
	}

	public static void clearTerritoryCache() {
		territoryNamesByBiome.clear();
		biomesByTerritoryNames.clear();
	}

	public static Set<String> getTerritoryNames() {
		return biomesByTerritoryNames.keySet();
	}

	public static Optional<Set<String>> getTerritoryBiomes(String territoryName) {
		return Optional.ofNullable(biomesByTerritoryNames.get(territoryName));
	}

	private static int getTerritoryWeight(String territoryName) {
		return (int) ((WorldGenStructureManager.INSTANCE.getTerritoryTemplates(territoryName).map(Set::size).orElse(0)
				+ (!territoryName.equals(WorldGenStructureManager.GENERIC_TERRITORY_NAME) ? WorldGenStructureManager.INSTANCE.getTerritoryTemplates(WorldGenStructureManager.GENERIC_TERRITORY_NAME).map(Set::size).orElse(0) : 0))
				* TerritorySettingRegistry.getTerritorySettings(territoryName).getTerritoryWeightMultiplier());
	}

	@SuppressWarnings("java:S3518")
	public static float getTerritoryChanceInBiome(String territoryName, String biomeName) {
		String biome = biomeName;
		if (!territoryNamesByBiome.containsKey(biome)) {
			return 0;
		}
		List<String> territoryNames = territoryNamesByBiome.get(biome);
		if (!territoryNames.contains(territoryName)) {
			return 0;
		}
		int weight = getTerritoryWeight(territoryName);

		float totalWeight = 0;

		for (String name : territoryNames) {
			totalWeight += getTerritoryWeight(name);
		}

		return weight / totalWeight;
	}

	public static void addTerritoryInBiome(String territoryName, String biomeName) {
		String biome = biomeName;
		if (LegacyBiomes.exists(biome)) {
			List<String> territoryNames = territoryNamesByBiome.getOrDefault(biome, new ArrayList<>());
			if (!territoryNames.contains(territoryName)) {
				territoryNames.add(territoryName);
				territoryNamesByBiome.put(biome, territoryNames);
			}

			Set<String> biomes = biomesByTerritoryNames.getOrDefault(territoryName, new HashSet<>());
			biomes.add(biome);
			biomesByTerritoryNames.put(territoryName, biomes);
		}
	}

	private static class TerritoryGenerator {
		private Map<Long, ProcessedChunk> chunksToProcess = new HashMap<>();
		private Map<Long, BlockPos> includedChunks = new HashMap<>();
		private Set<Long> visitedChunks = new HashSet<>();
		private Level world;
		private double maxTerritoryCenterDistanceSq = 0;

		private TerritoryGenerator(Level world) {
			this.world = world;
		}

		private String getBiome(BlockPos pos) {
			return LegacyBiomes.getSurfaceBiomeName(world, pos.getX(), pos.getZ());
		}

		private void generateTerritory(int chunkX, int chunkZ, long chunkPosValue, ITerritoryData territoryData) {
			BlockPos firstPos = getChunkCenterPos(chunkX, chunkZ);
			String biome = getBiome(firstPos);
			if (!territoryNamesByBiome.containsKey(biome)) {
				return;
			}

			//noinspection ConstantConditions
			WorldGenDetailedLogHelper.log("Generating new territory at x {} z {} in biome \"{}\"", firstPos::getX, firstPos::getZ, () -> biome);

			List<String> territoryNames = territoryNamesByBiome.get(biome);

			String territoryName = getRandomTerritory(territoryNames);
			String territoryId = territoryName + territoryData.getNextTerritoryId(territoryName);
			maxTerritoryCenterDistanceSq = TerritorySettingRegistry.getMaxTerritoryCenterDistanceSq(territoryName);

			includedChunks.put(chunkPosValue, firstPos);

			chunksToProcess.put(chunkPosValue, new ProcessedChunk(chunkPosValue, firstPos, chunkX, chunkZ));
			getTerritoryBiomes(territoryName).ifPresent(biomes -> processChunks(biomes, territoryData));

			if (isTooFewChunksForNewTerritory()) {
				territoryId = getFirstDifferentTerritoryIdFromNeighbors(chunkX, chunkZ, territoryId, territoryData);
				WorldGenDetailedLogHelper.log("Too few chunks in new territory - connecting to existing territory");
			}

			String finalTerritoryId = territoryId;
			includedChunks.forEach((hash, pos) -> territoryData.setOwned(hash, finalTerritoryId, territoryName));
			WorldGenDetailedLogHelper.log("{} chunks included in territory \"{}\"", includedChunks::size, () -> finalTerritoryId);
			if (!isTooFewChunksForNewTerritory()) {
				//noinspection ConstantConditions
				WorldGenStatistics.addTerritoryInfo(territoryName, biome, includedChunks.size()
						* TerritorySettingRegistry.getTerritorySettings(territoryName).getPerChunkClusterValueMultiplier() * AWStructureStatics.chunkClusterValue);
			}
		}

		private boolean isTooFewChunksForNewTerritory() {
			return includedChunks.size() < 3;
		}

		private BlockPos getTerritoryCenter() {
			return TerritoryManager.getTerritoryCenter(includedChunks.values());
		}

		private void addChunkToProcess(Set<String> biomes, BlockPos territoryCenterPos, ChunkPos chunkPos, ITerritoryData territoryData) {
			long chunkPosValue = ChunkPos.asLong(chunkPos.x, chunkPos.z);
			BlockPos centerPos = getChunkCenterPos(chunkPos.x, chunkPos.z);
			if (territoryData.isOwned(chunkPosValue) || visitedChunks.contains(chunkPosValue) || chunksToProcess.containsKey(chunkPosValue)) {
				return;
			}
			if (biomes.contains(getBiome(centerPos)) && centerPos.distSqr(territoryCenterPos) < 1.1 * maxTerritoryCenterDistanceSq) {
				chunksToProcess.put(chunkPosValue, new ProcessedChunk(chunkPosValue, centerPos, chunkPos.x, chunkPos.z));
			}
		}

		private void includeChunk(ProcessedChunk processedChunk) {
			includedChunks.put(processedChunk.getChunkPosValue(), processedChunk.getCenterPos());
			visitedChunks.add(processedChunk.getChunkPosValue());
			WorldGenDetailedLogHelper.log("Chunk #{} {}, territory center: {}", () -> includedChunks.entrySet().size(), processedChunk::getCenterPos, this::getTerritoryCenter);
		}

		private void processChunks(Set<String> biomes, ITerritoryData territoryData) {
			while (!chunksToProcess.isEmpty()) {
				if (includedChunks.isEmpty() && chunksToProcess.size() == 1) {
					ProcessedChunk processedChunk = chunksToProcess.values().iterator().next();
					includeChunk(processedChunk);
				} else {
					processClosestChunk(biomes, territoryData);
				}
			}
		}

		private void processClosestChunk(Set<String> biomes, ITerritoryData territoryData) {
			ProcessedChunk closestChunkToProcess = null;
			double closestDist = Double.MAX_VALUE;
			BlockPos territoryCenterPos = getTerritoryCenter();
			for (ProcessedChunk processedChunk : chunksToProcess.values()) {
				double dist = territoryCenterPos.distSqr(processedChunk.getCenterPos());
				if (dist < closestDist) {
					closestDist = dist;
					closestChunkToProcess = processedChunk;
				}
			}
			if (closestChunkToProcess != null) {
				if (closestDist < maxTerritoryCenterDistanceSq) {
					includeChunk(closestChunkToProcess);

					addChunkToProcess(biomes, territoryCenterPos, new ChunkPos(closestChunkToProcess.getChunkX() + 1, closestChunkToProcess.getChunkZ()), territoryData);
					addChunkToProcess(biomes, territoryCenterPos, new ChunkPos(closestChunkToProcess.getChunkX() - 1, closestChunkToProcess.getChunkZ()), territoryData);
					addChunkToProcess(biomes, territoryCenterPos, new ChunkPos(closestChunkToProcess.getChunkX(), closestChunkToProcess.getChunkZ() + 1), territoryData);
					addChunkToProcess(biomes, territoryCenterPos, new ChunkPos(closestChunkToProcess.getChunkX(), closestChunkToProcess.getChunkZ() - 1), territoryData);
				}
				chunksToProcess.remove(closestChunkToProcess.getChunkPosValue());
			}
		}

		private Optional<String> getDifferentTerritoryId(int chunkX, int chunkZ, String territoryId, ITerritoryData territoryData) {
			Optional<Territory> territory = territoryData.getTerritory(ChunkPos.asLong(chunkX, chunkZ));
			if (territory.isPresent()) {
				String id = territory.get().getTerritoryId();
				if (!id.equals(territoryId)) {
					return Optional.of(id);
				}
			}
			return Optional.empty();
		}

		private String getFirstDifferentTerritoryIdFromNeighbors(int chunkX, int chunkZ, String territoryId, ITerritoryData territoryData) {
			return getDifferentTerritoryId(chunkX + 1, chunkZ, territoryId, territoryData)
					.orElse(getDifferentTerritoryId(chunkX - 1, chunkZ, territoryId, territoryData)
							.orElse(getDifferentTerritoryId(chunkX, chunkZ + 1, territoryId, territoryData)
									.orElse(getDifferentTerritoryId(chunkX, chunkZ - 1, territoryId, territoryData).orElse(territoryId))
							)
					);
		}

		private String getRandomTerritory(List<String> names) {
			return CollectionUtils.getWeightedRandomElement(RNG, names,
					TerritoryManager::getTerritoryWeight, (totalWeight, selected) -> {
						WorldGenDetailedLogHelper.log("Out of total of {} territories with weight total of {} territory \"{}\" with weight {} was selected",
								names::size, () -> totalWeight, () -> selected, () -> selected == null ? "" : TerritoryManager.getTerritoryWeight(selected));
						WorldGenDetailedLogHelper.log("Following territories and weights were considered: \n{}",
								() -> {
									StringJoiner joiner = new StringJoiner(", ");
									names.forEach(name -> joiner.add(name + ":" + TerritoryManager.getTerritoryWeight(name)));
									return joiner.toString();
								});
					})
					.orElse(WorldGenStructureManager.GENERIC_TERRITORY_NAME);
		}
	}

	static BlockPos getTerritoryCenter(Collection<BlockPos> positions) {
		BlockPos territoryCenterPos;
		double averageX = 0;
		double averageZ = 0;
		for (BlockPos pos : positions) {
			averageX += (float) pos.getX() / positions.size();
			averageZ += (float) pos.getZ() / positions.size();
		}
		territoryCenterPos = BlockPos.containing(averageX, 1, averageZ);
		return territoryCenterPos;
	}

	private static class ProcessedChunk {
		private long chunkPosValue;
		private BlockPos centerPos;
		private int chunkX;
		private int chunkZ;

		private ProcessedChunk(long chunkPosValue, BlockPos centerPos, int chunkX, int chunkZ) {
			this.chunkPosValue = chunkPosValue;
			this.centerPos = centerPos;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
		}

		private long getChunkPosValue() {
			return chunkPosValue;
		}

		private BlockPos getCenterPos() {
			return centerPos;
		}

		private int getChunkX() {
			return chunkX;
		}

		private int getChunkZ() {
			return chunkZ;
		}
	}
}
