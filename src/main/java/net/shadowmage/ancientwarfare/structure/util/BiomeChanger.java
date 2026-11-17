package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/*
 * Port addition: 1.12 WorldTools.changeBiome(world, pos, biome) wrote one entry of the chunk's 2D biome array. The
 * 1.21 equivalent of doing that for many columns: collect the columns, then refill the biome containers of the
 * touched chunks once (what /fillbiome does, for the whole height) and resend them once.
 */
public class BiomeChanger {
	private final ServerLevel level;
	private final Holder<Biome> biome;
	private final Set<Long> columns = new HashSet<>(); //quart x / z packed like a chunk pos

	private BiomeChanger(ServerLevel level, Holder<Biome> biome) {
		this.level = level;
		this.biome = biome;
	}

	/*
	 * empty when the world is not a server world or the (1.12 or 1.21) biome name is not a biome of this game
	 */
	public static Optional<BiomeChanger> create(Level world, ResourceLocation biomeName) {
		if (!(world instanceof ServerLevel) || biomeName == null || biomeName.getPath().isEmpty()) {
			return Optional.empty();
		}
		return LegacyBiomes.translate(biomeName.toString()).stream().findFirst()
				.flatMap(name -> world.registryAccess().registryOrThrow(Registries.BIOME).getHolder(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(name))))
				.map(holder -> new BiomeChanger((ServerLevel) world, holder));
	}

	public void addColumn(BlockPos pos) {
		columns.add(ChunkPos.asLong(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getZ())));
	}

	public void apply() {
		Set<Long> chunkPositions = new HashSet<>();
		for (long column : columns) {
			chunkPositions.add(ChunkPos.asLong(QuartPos.toSection(ChunkPos.getX(column)), QuartPos.toSection(ChunkPos.getZ(column))));
		}
		List<ChunkAccess> chunks = new ArrayList<>();
		for (long chunkPos : chunkPositions) {
			LevelChunk chunk = level.getChunk(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
			chunk.fillBiomesFromNoise((x, y, z, sampler) -> columns.contains(ChunkPos.asLong(x, z)) ? biome : chunk.getNoiseBiome(x, y, z), level.getChunkSource().randomState().sampler());
			chunk.setUnsaved(true);
			chunks.add(chunk);
		}
		if (!chunks.isEmpty()) {
			level.getChunkSource().chunkMap.resendBiomesForChunks(chunks);
		}
		columns.clear();
	}

	/*
	 * Improvement (not baseline, budgeted_placement): apply above as resumable steps (one unit per chunk, in the same
	 * order, then the resend). apply is unchanged and stays the 1.12 path.
	 */
	public net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps applySteps() {
		Set<Long> chunkPositions = new HashSet<>();
		List<ChunkAccess> chunks = new ArrayList<>();
		return new net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps()
				.then(() -> {
					for (long column : columns) {
						chunkPositions.add(ChunkPos.asLong(QuartPos.toSection(ChunkPos.getX(column)), QuartPos.toSection(ChunkPos.getZ(column))));
					}
				})
				.thenEach(() -> chunkPositions, chunkPos -> () -> {
					LevelChunk chunk = level.getChunk(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
					chunk.fillBiomesFromNoise((x, y, z, sampler) -> columns.contains(ChunkPos.asLong(x, z)) ? biome : chunk.getNoiseBiome(x, y, z), level.getChunkSource().randomState().sampler());
					chunk.setUnsaved(true);
					chunks.add(chunk);
				}, 1)
				.then(() -> {
					if (!chunks.isEmpty()) {
						level.getChunkSource().chunkMap.resendBiomesForChunks(chunks);
					}
					columns.clear();
				});
	}
}
