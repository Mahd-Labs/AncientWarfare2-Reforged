package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/*
 * Port notes: 1.12 Biome.topBlock / Biome.fillerBlock. Biomes have no surface blocks any more (surface rules build
 * the surface), so the 1.12 values of the vanilla biomes are looked up by biome key / tag; everything else gets the
 * 1.12 default (grass / dirt).
 */
public class BiomeBlocks {
	private BiomeBlocks() {}

	public static BlockState getTopBlock(Level world, BlockPos pos) {
		return getTopBlock(LegacyBiomes.getSurfaceBiome(world, pos.getX(), pos.getZ())); //1.12 callers pass y = 1 (2D biomes)
	}

	public static BlockState getTopBlock(Holder<Biome> biome) {
		if (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BEACH) && !biome.is(Biomes.STONY_SHORE)) {
			return Blocks.SAND.defaultBlockState();
		} else if (biome.is(BiomeTags.IS_BADLANDS)) {
			return Blocks.RED_SAND.defaultBlockState();
		} else if (biome.is(Biomes.MUSHROOM_FIELDS)) {
			return Blocks.MYCELIUM.defaultBlockState();
		} else if (biome.is(Biomes.STONY_SHORE)) {
			return Blocks.STONE.defaultBlockState();
		} else if (biome.is(BiomeTags.IS_NETHER)) {
			return Blocks.NETHERRACK.defaultBlockState();
		} else if (biome.is(BiomeTags.IS_END)) {
			return Blocks.END_STONE.defaultBlockState();
		}
		return Blocks.GRASS_BLOCK.defaultBlockState();
	}

	public static BlockState getFillerBlock(Holder<Biome> biome) {
		if (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BEACH) && !biome.is(Biomes.STONY_SHORE)) {
			return Blocks.SAND.defaultBlockState();
		} else if (biome.is(BiomeTags.IS_BADLANDS)) {
			return Blocks.TERRACOTTA.defaultBlockState();
		} else if (biome.is(Biomes.STONY_SHORE)) {
			return Blocks.STONE.defaultBlockState();
		} else if (biome.is(BiomeTags.IS_NETHER)) {
			return Blocks.NETHERRACK.defaultBlockState();
		} else if (biome.is(BiomeTags.IS_END)) {
			return Blocks.END_STONE.defaultBlockState();
		}
		return Blocks.DIRT.defaultBlockState();
	}
}
