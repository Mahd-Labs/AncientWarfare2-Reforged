package net.shadowmage.ancientwarfare.structure.template.build.validation.border;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.template.build.validation.border.points.PointType;
import net.shadowmage.ancientwarfare.structure.template.build.validation.border.points.SmoothingPoint;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldStructureGenerator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

public class SmoothingMatrix {
	private SmoothingPoint[][] smoothingPoints;
	private final int fullXSize;
	private final int fullZSize;
	private Map<PointType, Set<SmoothingPoint>> typePoints = new HashMap<>();
	private final BlockPos minPos;

	public SmoothingMatrix(BorderMatrix borderMatrix, BlockPos structureMinPos, int borderSize) {
		fullXSize = borderMatrix.getFullXSize();
		fullZSize = borderMatrix.getFullZSize();
		smoothingPoints = initMatrix(fullXSize, fullZSize);
		minPos = structureMinPos.offset(-borderSize - 2, 0, -borderSize - 2);
	}

	Optional<SmoothingPoint> getPoint(HorizontalCoords coords) {
		return getPoint(coords.getX(), coords.getZ());
	}

	public Optional<SmoothingPoint> getPoint(int x, int z) {
		if (smoothingPoints.length == 0 || x < 0 || x >= smoothingPoints.length || z < 0 || z >= smoothingPoints[0].length) {
			return Optional.empty();
		}

		return Optional.ofNullable(smoothingPoints[x][z]);
	}

	private void printMatrix() {
		printTypes();
		printDistances();
		printHeights();
	}

	private void printHeights() {
		for (int x = 0; x < fullXSize; x++) {
			for (int z = 0; z < fullZSize; z++) {
				System.out.print(String.format("%02d", getPoint(x, z).map(point -> !point.hasSmoothedPosSet() ? 0 : point.getSmoothedPos().getY()).orElse(0)) + " ");
			}
			System.out.println();
		}
	}

	private void printDistances() {
		for (int x = 0; x < fullXSize; x++) {
			for (int z = 0; z < fullZSize; z++) {
				System.out.print(getPoint(x, z).map(point -> point.getStructureBorderDistance() == Integer.MAX_VALUE ? "00"
						: String.format("%02d", point.getStructureBorderDistance())).orElse("00") + " ");
			}
			System.out.println();
		}
	}

	private void printTypes() {
		for (int x = 0; x < fullXSize; x++) {
			for (int z = 0; z < fullZSize; z++) {
				System.out.print(getPoint(x, z).map(point -> point.getType().getAcronym()).orElse(" ") + " ");
			}
			System.out.println();
		}
	}

	public boolean isEmpty(HorizontalCoords point) {
		return !getPoint(point).isPresent();
	}

	public SmoothingPoint addPoint(int x, int z, BlockPos pos, PointType type) {
		SmoothingPoint point = new SmoothingPoint(x, z, pos, type);
		addPoint(point);
		return point;
	}

	public SmoothingPoint addPoint(int x, int z, BlockPos pos, PointType type, BlockState state) {
		SmoothingPoint point = addPoint(x, z, pos, type);
		point.setBlockAndUpdate(state);
		return point;
	}

	void addPoint(SmoothingPoint point) {
		smoothingPoints[point.getX()][point.getZ()] = point;
		addTypePoint(point.getType(), point);
	}

	private SmoothingPoint[][] initMatrix(int fullXSize, int fullZSize) {
		SmoothingPoint[][] ret = new SmoothingPoint[fullXSize][];

		for (int x = 0; x < fullXSize; x++) {
			ret[x] = new SmoothingPoint[fullZSize];
		}

		return ret;
	}

	private void addTypePoint(PointType type, SmoothingPoint point) {
		if (!typePoints.containsKey(type)) {
			typePoints.put(type, new HashSet<>());
		}
		typePoints.get(type).add(point);
	}

	public void apply(Level world, Consumer<BlockPos> handleClearing) {
		Set<BlockPos> chunkPoints = new HashSet<>();
		typePoints.get(PointType.SMOOTHED_BORDER).forEach(point -> {
			levelTerrain(world, point, handleClearing);
			chunkPoints.add(new BlockPos(getChunkCornerCoord(point.getSmoothedPos().getX()), 0, getChunkCornerCoord(point.getSmoothedPos().getZ())));
		});
		chunkPoints.forEach(pos -> decorate(world, pos));
		typePoints.get(PointType.SMOOTHED_BORDER).forEach(point -> {
			BlockPos posAbove = point.getSmoothedPos().above();
			if (world.getBiome(posAbove).value().shouldSnow(world, posAbove)) {
				world.setBlock(posAbove, Blocks.SNOW.defaultBlockState(), 2);
			}
		});
	}

	/*
	 * Improvement (not baseline, budgeted_placement): apply above as resumable steps, the same three loops in the same
	 * order (one unit per point / chunk corner). apply is unchanged and stays the 1.12 path.
	 */
	public net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps applySteps(Level world, Consumer<BlockPos> handleClearing) {
		Set<BlockPos> chunkPoints = new HashSet<>();
		return new net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps()
				.thenEach(() -> typePoints.get(PointType.SMOOTHED_BORDER), point -> () -> {
					levelTerrain(world, point, handleClearing);
					chunkPoints.add(new BlockPos(getChunkCornerCoord(point.getSmoothedPos().getX()), 0, getChunkCornerCoord(point.getSmoothedPos().getZ())));
				}, 1)
				.thenEach(() -> chunkPoints, pos -> () -> decorate(world, pos), 16)
				.thenEach(() -> typePoints.get(PointType.SMOOTHED_BORDER), point -> () -> {
					BlockPos posAbove = point.getSmoothedPos().above();
					if (world.getBiome(posAbove).value().shouldSnow(world, posAbove)) {
						world.setBlock(posAbove, Blocks.SNOW.defaultBlockState(), 2);
					}
				}, 16);
	}

	private int getChunkCornerCoord(int coord) {
		return (coord >> 4) * 16;
	}

	@SuppressWarnings("squid:S2245")
	private void decorate(Level world, BlockPos point) {
		Random random = new Random((long) (point.getX() >> 4) * 341873128712L + (long) (point.getZ() >> 4) * 132897987541L);
		//Port notes: NOT PORTED - 1.12 re-ran the biome decorator (Biome.decorate) on the smoothed border column; 1.21 biomes have no decorator
	}

	private void levelTerrain(Level world, SmoothingPoint point, Consumer<BlockPos> handleClearing) {
		BlockPos originalPos = point.getWorldPos();
		BlockPos smoothedPos = point.getSmoothedPos();

		net.minecraft.core.Holder<Biome> biome = world.getBiome(originalPos);
		int topNonWaterY = WorldStructureGenerator.getTargetY(world, originalPos.getX(), originalPos.getZ(), true, originalPos.getY());
		int topOuterBorderWaterY = point.getOuterBorderPoint().getWaterLevel();

		if (originalPos.getY() == smoothedPos.getY() && topNonWaterY == originalPos.getY()) {
			return;
		}

		if (originalPos.getY() > smoothedPos.getY()) {
			if (smoothedPos.getY() < topOuterBorderWaterY) {
				BlockTools.getAllInBoxTopDown(smoothedPos, new BlockPos(smoothedPos.getX(), topOuterBorderWaterY, smoothedPos.getZ()))
						.forEach(pos -> world.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState()));
			} else {
				BlockTools.getAllInBoxTopDown(smoothedPos, originalPos).forEach(handleClearing);
			}
		}
		if (smoothedPos.getY() - topNonWaterY > 1) {
			BlockState fillerBlock = net.shadowmage.ancientwarfare.structure.util.BiomeBlocks.getFillerBlock(biome);
			BlockPos.betweenClosed(originalPos.getX(), topNonWaterY + 1, originalPos.getZ(), originalPos.getX(), smoothedPos.getY() - 1, originalPos.getZ())
					.forEach(pos -> world.setBlockAndUpdate(pos, fillerBlock));
		}
		world.setBlockAndUpdate(smoothedPos, point.getBlockState());
	}

	public BlockPos getMinPos() {
		return minPos;
	}

	public Set<SmoothingPoint> getPointsOfType(PointType type) {
		return typePoints.get(type);
	}
}
