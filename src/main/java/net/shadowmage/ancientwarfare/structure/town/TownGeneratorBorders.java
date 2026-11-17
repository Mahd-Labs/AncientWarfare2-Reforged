package net.shadowmage.ancientwarfare.structure.town;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.shadowmage.ancientwarfare.structure.util.BiomeBlocks;
import net.shadowmage.ancientwarfare.structure.util.LegacyBiomes;
import net.shadowmage.ancientwarfare.automation.registry.TreeFarmRegistry;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.ITree;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.ITreeScanner;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.validation.border.SmoothingMatrixBuilder;

import java.util.Optional;

public class TownGeneratorBorders {
	private static final int CLEAR_TREE_MAX_BORDER_DISTANCE = 10;
	public static final int MAX_BORDER_WIDTH = 20;

	private TownGeneratorBorders() {}

	public static void generateBorders(Level world, StructureBB exterior) {
		BlockTools.getAllInBoxTopDown(exterior.min, exterior.max.offset(0, 50, 0)).forEach(pos -> handleClearing(world, pos));

		new SmoothingMatrixBuilder(world, exterior, Math.min(Math.max(exterior.getXSize(), exterior.getZSize()) / 8, MAX_BORDER_WIDTH),
				exterior.min.getY() - 1, p -> BiomeBlocks.getTopBlock(LegacyBiomes.getSurfaceBiome(world, p.getX(), p.getZ()))).build()
				.apply(world, pos -> handleClearing(world, pos));
	}

	/*
	 * Improvement (not baseline, budgeted_placement): generateBorders above as resumable steps (clearing position by
	 * position top down, the smoothing matrix, its application), same order. generateBorders is unchanged.
	 */
	public static net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps generateBordersSteps(Level world, StructureBB exterior) {
		net.shadowmage.ancientwarfare.structure.template.build.validation.border.SmoothingMatrix[] matrix = new net.shadowmage.ancientwarfare.structure.template.build.validation.border.SmoothingMatrix[1];
		return new net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps()
				.thenEach(() -> BlockTools.getAllInBoxTopDown(exterior.min, exterior.max.offset(0, 50, 0)), pos -> () -> handleClearing(world, pos), 64)
				.thenSteps(() -> SmoothingMatrixBuilder.buildSteps(world, exterior, Math.min(Math.max(exterior.getXSize(), exterior.getZSize()) / 8, MAX_BORDER_WIDTH),
						exterior.min.getY() - 1, p -> BiomeBlocks.getTopBlock(LegacyBiomes.getSurfaceBiome(world, p.getX(), p.getZ())), m -> matrix[0] = m))
				.thenSteps(() -> matrix[0].applySteps(world, pos -> handleClearing(world, pos)));
	}

	/*
	 * budgeted placement: levelTownArea below as resumable steps, one unit per column in the same order
	 */
	public static net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps levelTownAreaSteps(Level world, StructureBB bb) {
		return new net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps().thenList(() -> {
			java.util.List<Runnable> units = new java.util.ArrayList<>();
			int minX = bb.min.getX();
			int minZ = bb.min.getZ();
			int maxX = bb.max.getX();
			int maxZ = bb.max.getZ();
			int desiredTopBlockHeight = bb.min.getY() - 1;
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					int cx = x;
					int cz = z;
					units.add(() -> {
						handleBorderBlock(world, cx, cz, desiredTopBlockHeight, desiredTopBlockHeight, getFillBlock(world, cx, cz, false), getFillBlock(world, cx, cz, true));
						world.setBlockAndUpdate(new BlockPos(cx, desiredTopBlockHeight - 5, cz), Blocks.COBBLESTONE.defaultBlockState());
					});
				}
			}
			return units;
		}, 4);
	}

	public static void levelTownArea(Level world, StructureBB bb) {
		int minX = bb.min.getX();
		int minZ = bb.min.getZ();
		int maxX = bb.max.getX();
		int maxZ = bb.max.getZ();
		int desiredTopBlockHeight = bb.min.getY() - 1;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				handleBorderBlock(world, x, z, desiredTopBlockHeight, desiredTopBlockHeight, getFillBlock(world, x, z, false), getFillBlock(world, x, z, true));
				world.setBlockAndUpdate(new BlockPos(x, desiredTopBlockHeight - 5, z), Blocks.COBBLESTONE.defaultBlockState());
			}
		}
	}

	private static void handleBorderBlock(Level world, int x, int z, int fillLevel, int cutLevel, BlockState fillBlock, BlockState topBlock) {
		int y = BlockTools.getTopFilledHeight(world, x, z, false);
		int topSolidY = BlockTools.getTopFilledHeight(world, x, z, true);
		if (y >= cutLevel) {
			for (int py = y; py > Math.min(topSolidY, cutLevel); py--) {
				BlockPos clearPos = new BlockPos(x, py, z);
				handleClearing(world, clearPos);
			}
			if (topSolidY > cutLevel) {
				world.setBlockAndUpdate(new BlockPos(x, cutLevel, z), topBlock);
			}
		}
		if (topSolidY <= fillLevel) {
			for (int py = topSolidY + 1; py < fillLevel; py++) {
				world.setBlockAndUpdate(new BlockPos(x, py, z), fillBlock);
			}
			world.setBlockAndUpdate(new BlockPos(x, fillLevel, z), topBlock);
		}
	}

	private static void handleClearing(Level world, BlockPos clearPos) {
		BlockState state = world.getBlockState(clearPos);
		if (!state.isAir()) {
			Optional<ITreeScanner> treeScanner = TreeFarmRegistry.getRegisteredTreeScanner(state);
			if (!treeScanner.isPresent()) {
				world.removeBlock(clearPos, false);
				return;
			}
			ITree tree = treeScanner.get().scanTree(world, clearPos,
					clearPos.offset(-CLEAR_TREE_MAX_BORDER_DISTANCE, 0, -CLEAR_TREE_MAX_BORDER_DISTANCE),
					clearPos.offset(CLEAR_TREE_MAX_BORDER_DISTANCE, 0, CLEAR_TREE_MAX_BORDER_DISTANCE));
			tree.getLeafPositions().forEach(p -> world.removeBlock(p, false));
			tree.getTrunkPositions().forEach(p -> world.removeBlock(p, false));
		}
	}

	private static BlockState getFillBlock(Level world, int x, int z, boolean surface) {
		Holder<Biome> biome = LegacyBiomes.getSurfaceBiome(world, x, z);
		if (surface) {
			return BiomeBlocks.getTopBlock(biome);
		} else {
			return BiomeBlocks.getFillerBlock(biome);
		}
	}
}
