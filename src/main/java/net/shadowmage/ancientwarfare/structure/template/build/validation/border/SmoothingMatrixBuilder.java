package net.shadowmage.ancientwarfare.structure.template.build.validation.border;

import net.shadowmage.ancientwarfare.structure.block.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.google.common.collect.ImmutableSet;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.validation.border.points.BorderPoint;
import net.shadowmage.ancientwarfare.structure.template.build.validation.border.points.PointType;
import net.shadowmage.ancientwarfare.structure.template.build.validation.border.points.SmoothingPoint;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldStructureGenerator;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;

public class SmoothingMatrixBuilder {
	private final Level world;
	private final int borderSize;
	private final int groundY;
	private SmoothingMatrix smoothingMatrix;
	private Function<BlockPos, BlockState> getStructureBorderState;

	public SmoothingMatrixBuilder(Level world, StructureBB bb, int borderSize, int groundY, Function<BlockPos, BlockState> getStructureBorderState) {
		this.world = world;
		this.borderSize = borderSize;
		this.groundY = groundY;
		this.getStructureBorderState = getStructureBorderState;

		BorderMatrix borderMatrix = BorderMatrixCache.getBorderMatrix(bb.getXSize(), bb.getZSize(), borderSize);
		smoothingMatrix = new SmoothingMatrix(borderMatrix, bb.min, borderSize);

		convertPointsToSmoothingMatrix(this.groundY, borderMatrix, PointType.STRUCTURE_BORDER);
		convertPointsToSmoothingMatrix(0, borderMatrix, PointType.REFERENCE_POINT);
		convertPointsToSmoothingMatrix(0, borderMatrix, PointType.OUTER_BORDER);
		convertPointsToSmoothingMatrix(0, borderMatrix, PointType.SMOOTHED_BORDER);
	}

	/*
	 * Improvement (not baseline, budgeted_placement): the constructor + build() above as resumable steps, the same
	 * loops in the same order, one unit per point (the border matrix lookup is the first unit). result receives the
	 * smoothing matrix that build() returns. The constructor and build() are unchanged and stay the 1.12 path.
	 */
	public static net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps buildSteps(Level world, StructureBB bb, int borderSize, int groundY,
			Function<BlockPos, BlockState> getStructureBorderState, java.util.function.Consumer<SmoothingMatrix> result) {
		SmoothingMatrixBuilder[] builder = new SmoothingMatrixBuilder[1];
		BorderMatrix[] borderMatrix = new BorderMatrix[1];
		net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps steps = new net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps();
		//a border matrix that is not cached yet (100 - 250 ms for a large area) is built on a background thread meanwhile
		@SuppressWarnings("unchecked")
		java.util.concurrent.CompletableFuture<BorderMatrix>[] future = new java.util.concurrent.CompletableFuture[1];
		steps.thenWait(() -> {
			if (BorderMatrixCache.getCachedBorderMatrix(bb.getXSize(), bb.getZSize(), borderSize) != null) {
				return true;
			}
			if (future[0] == null) {
				future[0] = java.util.concurrent.CompletableFuture.supplyAsync(() -> BorderMatrixCache.buildBorderMatrix(bb.getXSize(), bb.getZSize(), borderSize), net.minecraft.Util.backgroundExecutor());
			}
			return future[0].isDone();
		});
		steps.then(() -> {
			if (future[0] != null && future[0].isDone() && !future[0].isCompletedExceptionally()) {
				BorderMatrixCache.putBorderMatrix(bb.getXSize(), bb.getZSize(), borderSize, future[0].join());
			}
			//cached now, or built here (server stopping before the background build was done, or it failed)
			borderMatrix[0] = BorderMatrixCache.getBorderMatrix(bb.getXSize(), bb.getZSize(), borderSize);
			builder[0] = new SmoothingMatrixBuilder(world, borderSize, groundY, getStructureBorderState, new SmoothingMatrix(borderMatrix[0], bb.min, borderSize));
		});
		steps.thenEach(() -> borderMatrix[0].getPointsOfType(PointType.STRUCTURE_BORDER), point -> () -> builder[0].convertPointToSmoothingMatrix(groundY, point, PointType.STRUCTURE_BORDER), 16);
		steps.thenEach(() -> borderMatrix[0].getPointsOfType(PointType.REFERENCE_POINT), point -> () -> builder[0].convertPointToSmoothingMatrix(0, point, PointType.REFERENCE_POINT), 16);
		steps.thenEach(() -> borderMatrix[0].getPointsOfType(PointType.OUTER_BORDER), point -> () -> builder[0].convertPointToSmoothingMatrix(0, point, PointType.OUTER_BORDER), 16);
		steps.thenEach(() -> borderMatrix[0].getPointsOfType(PointType.SMOOTHED_BORDER), point -> () -> builder[0].convertPointToSmoothingMatrix(0, point, PointType.SMOOTHED_BORDER), 16);
		steps.thenEach(() -> builder[0].smoothingMatrix.getPointsOfType(PointType.SMOOTHED_BORDER), point -> () -> builder[0].calculateNewYLevelAndBlock(point), 64);
		steps.thenEach(() -> builder[0].smoothingMatrix.getPointsOfType(PointType.SMOOTHED_BORDER), point -> () -> builder[0].processSmoothing(point), 64);
		steps.then(() -> result.accept(builder[0].smoothingMatrix));
		return steps;
	}

	//budgeted placement: the fields of the public constructor, without its conversion loops (buildSteps does them)
	private SmoothingMatrixBuilder(Level world, int borderSize, int groundY, Function<BlockPos, BlockState> getStructureBorderState, SmoothingMatrix smoothingMatrix) {
		this.world = world;
		this.borderSize = borderSize;
		this.groundY = groundY;
		this.getStructureBorderState = getStructureBorderState;
		this.smoothingMatrix = smoothingMatrix;
	}

	//budgeted placement: the body of the loop of convertPointsToSmoothingMatrix below for one point
	private void convertPointToSmoothingMatrix(int groundY, BorderPoint point, PointType type) {
		SmoothingPoint smoothingPoint;
		if (type == PointType.SMOOTHED_BORDER) {
			smoothingPoint = addPoint(point.getX(), point.getZ(), PointType.SMOOTHED_BORDER,
					(x, z) -> getYBelowFloatingIsland(getMinPos().getX() + x, BlockTools.getTopFilledHeight(world, getMinPos().getX() + x, getMinPos().getZ() + z, false), getMinPos().getZ() + z, false));
			BorderPoint outerBorder = point.getOuterBorderPoint();
			BorderPoint ref = point.getReferencePoint();
			Optional<SmoothingPoint> outerBorderPoint = smoothingMatrix.getPoint(outerBorder.getX(), outerBorder.getZ());
			Optional<SmoothingPoint> refPoint = smoothingMatrix.getPoint(ref.getX(), ref.getZ());
			if (outerBorderPoint.isPresent() && refPoint.isPresent()) {
				smoothingPoint.setOuterBorderAndReferencePoint(outerBorderPoint.get(), refPoint.get());
			}
		} else {
			smoothingPoint = addPoint(point.getX(), point.getZ(), point.getType(), groundY);
		}
		if (type == PointType.OUTER_BORDER || type == PointType.SMOOTHED_BORDER) {
			BorderPoint borderPoint = point.getClosestBorderPoint();
			smoothingMatrix.getPoint(borderPoint.getX(), borderPoint.getZ()).ifPresent(p -> {
				smoothingPoint.setStructureBorder(p, point.getStructureBorderDistance());
				if (type == PointType.OUTER_BORDER) {
					smoothingPoint.setWaterLevel(getWaterLevel(smoothingPoint.getWorldPos()));
				}
			});
		}
	}

	//budgeted placement: the body of the loop of processSmoothing below for one point
	private void processSmoothing(SmoothingPoint point) {
		int totalY = point.getSmoothedPos().getY();
		BlockPos currentPos = point.getSmoothedPos();
		HorizontalCoords current = new HorizontalCoords(point.getX(), point.getZ());
		for (HorizontalCoords adjacent : HorizontalCoords.ADJACENT_OFFSETS) {
			Optional<SmoothingPoint> adjacentPoint = smoothingMatrix.getPoint(current.add(adjacent));
			if (adjacentPoint.isPresent()) {
				totalY += adjacentPoint.get().getSmoothedPos().getY();
			}
		}
		int averageY = Math.round(totalY / 5f);
		if (currentPos.getY() != averageY) {
			point.setSmoothedPos(new BlockPos(currentPos.getX(), averageY, currentPos.getZ()));
		}
	}

	//budgeted placement: the body of the loop of calculateNewYLevelsAndBlocks below for one point
	private void calculateNewYLevelAndBlock(SmoothingPoint pointToSmooth) {
		SmoothingPoint referencePoint = pointToSmooth.getReferencePoint();
		SmoothingPoint outerBorderPoint = pointToSmooth.getOuterBorderPoint();
		double outerDistToStructure = outerBorderPoint.getStructureBorderDistance();
		int structureBorderY = outerBorderPoint.getClosestBorderPoint().getWorldPos().getY();
		int yDiff = outerBorderPoint.getWorldPos().getY() - structureBorderY;
		boolean steepFormula = yDiff >= 0 ? referencePoint.getWorldPos().getY() - outerBorderPoint.getWorldPos().getY() > 2 : referencePoint.getWorldPos().getY() - outerBorderPoint.getWorldPos().getY() < -2;
		double pointDist = pointToSmooth.getStructureBorderDistance();
		if (steepFormula) {
			double ratio = pointDist / outerDistToStructure;
			setNewSmoothedY(pointToSmooth, structureBorderY + (int) Math.round(yDiff * ratio * ratio));
		} else {
			double halfDist = outerDistToStructure / 2;
			int halfYDiff = yDiff / 2;
			if (pointDist <= halfDist) {
				double ratio = pointDist / halfDist;
				setNewSmoothedY(pointToSmooth, structureBorderY + (int) Math.round(halfYDiff * ratio * ratio));
			} else {
				double ratio = (outerDistToStructure - pointDist) / halfDist;
				setNewSmoothedY(pointToSmooth, outerBorderPoint.getWorldPos().getY() - (int) Math.round(halfYDiff * ratio * ratio));
			}
		}
		setBlockStateForPoint(outerBorderPoint, outerDistToStructure, yDiff, pointToSmooth, pointDist);
	}

	private void convertPointsToSmoothingMatrix(int groundY, BorderMatrix borderMatrix, PointType type) {
		for (BorderPoint point : borderMatrix.getPointsOfType(type)) {
			SmoothingPoint smoothingPoint;
			if (type == PointType.SMOOTHED_BORDER) {
				smoothingPoint = addPoint(point.getX(), point.getZ(), PointType.SMOOTHED_BORDER,
						(x, z) -> getYBelowFloatingIsland(getMinPos().getX() + x, BlockTools.getTopFilledHeight(world, getMinPos().getX() + x, getMinPos().getZ() + z, false), getMinPos().getZ() + z, false));
				BorderPoint outerBorder = point.getOuterBorderPoint();
				BorderPoint ref = point.getReferencePoint();
				Optional<SmoothingPoint> outerBorderPoint = smoothingMatrix.getPoint(outerBorder.getX(), outerBorder.getZ());
				Optional<SmoothingPoint> refPoint = smoothingMatrix.getPoint(ref.getX(), ref.getZ());
				if (outerBorderPoint.isPresent() && refPoint.isPresent()) {
					smoothingPoint.setOuterBorderAndReferencePoint(outerBorderPoint.get(), refPoint.get());
				}
			} else {
				smoothingPoint = addPoint(point.getX(), point.getZ(), point.getType(), groundY);
			}
			if (type == PointType.OUTER_BORDER || type == PointType.SMOOTHED_BORDER) {
				BorderPoint borderPoint = point.getClosestBorderPoint();
				smoothingMatrix.getPoint(borderPoint.getX(), borderPoint.getZ()).ifPresent(p -> {
					smoothingPoint.setStructureBorder(p, point.getStructureBorderDistance());
					if (type == PointType.OUTER_BORDER) {
						smoothingPoint.setWaterLevel(getWaterLevel(smoothingPoint.getWorldPos()));
					}
				});
			}
		}
	}

	private int getWaterLevel(BlockPos worldPos) {
		BlockPos tempPos = worldPos.above();
		while (tempPos.getY() <= groundY && isWaterMaterial(world.getBlockState(tempPos))) {
			tempPos = tempPos.above();
		}
		tempPos = tempPos.below();

		return tempPos.getY() > worldPos.getY() ? tempPos.getY() : 0;
	}

	private boolean isWaterMaterial(BlockState material) {
		return material.getFluidState().is(net.minecraft.tags.FluidTags.WATER) || material.is(net.minecraft.tags.BlockTags.ICE);
	}

	public SmoothingMatrix build() {
		calculateNewYLevelsAndBlocks();
		processSmoothing();

		return smoothingMatrix;
	}

	private void processSmoothing() {
		for (SmoothingPoint point : smoothingMatrix.getPointsOfType(PointType.SMOOTHED_BORDER)) {
			int totalY = point.getSmoothedPos().getY();
			BlockPos currentPos = point.getSmoothedPos();
			HorizontalCoords current = new HorizontalCoords(point.getX(), point.getZ());
			for (HorizontalCoords adjacent : HorizontalCoords.ADJACENT_OFFSETS) {
				Optional<SmoothingPoint> adjacentPoint = smoothingMatrix.getPoint(current.add(adjacent));
				if (adjacentPoint.isPresent()) {
					totalY += adjacentPoint.get().getSmoothedPos().getY();
				}
			}
			int averageY = Math.round(totalY / 5f);
			if (currentPos.getY() != averageY) {
				point.setSmoothedPos(new BlockPos(currentPos.getX(), averageY, currentPos.getZ()));
			}
		}
	}

	private void calculateNewYLevelsAndBlocks() {
		for (SmoothingPoint pointToSmooth : smoothingMatrix.getPointsOfType(PointType.SMOOTHED_BORDER)) {
			SmoothingPoint referencePoint = pointToSmooth.getReferencePoint();
			SmoothingPoint outerBorderPoint = pointToSmooth.getOuterBorderPoint();
			double outerDistToStructure = outerBorderPoint.getStructureBorderDistance();
			int structureBorderY = outerBorderPoint.getClosestBorderPoint().getWorldPos().getY();
			int yDiff = outerBorderPoint.getWorldPos().getY() - structureBorderY;
			boolean steepFormula = yDiff >= 0 ? referencePoint.getWorldPos().getY() - outerBorderPoint.getWorldPos().getY() > 2 : referencePoint.getWorldPos().getY() - outerBorderPoint.getWorldPos().getY() < -2;
			double pointDist = pointToSmooth.getStructureBorderDistance();
			if (steepFormula) {
				double ratio = pointDist / outerDistToStructure;
				setNewSmoothedY(pointToSmooth, structureBorderY + (int) Math.round(yDiff * ratio * ratio));
			} else {
				double halfDist = outerDistToStructure / 2;
				int halfYDiff = yDiff / 2;
				if (pointDist <= halfDist) {
					double ratio = pointDist / halfDist;
					setNewSmoothedY(pointToSmooth, structureBorderY + (int) Math.round(halfYDiff * ratio * ratio));
				} else {
					double ratio = (outerDistToStructure - pointDist) / halfDist;
					setNewSmoothedY(pointToSmooth, outerBorderPoint.getWorldPos().getY() - (int) Math.round(halfYDiff * ratio * ratio));
				}
			}
			setBlockStateForPoint(outerBorderPoint, outerDistToStructure, yDiff, pointToSmooth, pointDist);
		}
	}

	private void setBlockStateForPoint(SmoothingPoint outerBorderPoint, double outerDistToStructure, int yDiff, SmoothingPoint pointToSmooth, double pointDist) {
		if (!pointToSmooth.getClosestBorderPoint().useStateForBlending()) {
			pointToSmooth.setBlockAndUpdate(outerBorderPoint.getBlockState());
			return;
		}

		if (Math.round(yDiff / outerDistToStructure) < 2) {
			pointToSmooth.setBlockAndUpdate(
					world.random.nextDouble() > pointDist / outerDistToStructure ? pointToSmooth.getClosestBorderPoint().getBlockState() : outerBorderPoint.getBlockState());
		} else {
			pointToSmooth.setBlockAndUpdate(net.shadowmage.ancientwarfare.structure.util.BiomeBlocks.getTopBlock(world.getBiome(pointToSmooth.getWorldPos())));
		}
	}

	private void setNewSmoothedY(SmoothingPoint pointToSmooth, int newY) {
		pointToSmooth.setSmoothedPos(new BlockPos(pointToSmooth.getWorldPos().getX(), newY, pointToSmooth.getWorldPos().getZ()));
	}

	private SmoothingPoint addPoint(int x, int z, PointType type, int yLevel) {
		return addPoint(x, z, type, (xCoord, zCoord) -> getY(xCoord, zCoord, yLevel));
	}

	private SmoothingPoint addPoint(int x, int z, PointType type, IntBinaryOperator getY) {
		BlockPos pos = new BlockPos(getMinPos().getX() + x, getY.applyAsInt(x, z), getMinPos().getZ() + z);
		Optional<BlockState> state = getPointBlockState(pos, type);
		return state.map(iBlockState -> smoothingMatrix.addPoint(x, z, pos, type, iBlockState)).orElseGet(() -> smoothingMatrix.addPoint(x, z, pos, type));
	}

	private int getY(int x, int z, int yLevel) {
		int y = yLevel;
		if (yLevel == 0) {
			y = WorldStructureGenerator.getTargetY(world, getMinPos().getX() + x, getMinPos().getZ() + z, true);
			y = getYBelowFloatingIsland(getMinPos().getX() + x, y, getMinPos().getZ() + z);
		}
		return y;
	}

	private BlockPos getMinPos() {
		return smoothingMatrix.getMinPos();
	}

	private static final Set<Block> STRUCTURE_BORDER_BLOCK_WHITELIST = ImmutableSet.of(
			Blocks.STONE,
			Blocks.GRAVEL,
			Blocks.DIRT,
			Blocks.GRASS_BLOCK,
			Blocks.SAND,
			Blocks.SANDSTONE,
			Blocks.COBBLESTONE,
			Blocks.MOSSY_COBBLESTONE,
			Blocks.OBSIDIAN,
			Blocks.SNOW,
			Blocks.NETHERRACK,
			Blocks.SOUL_SAND,
			Blocks.MYCELIUM,
			Blocks.END_STONE,
			Blocks.TERRACOTTA,
			Blocks.CLAY,
			Blocks.DIRT_PATH,
			Blocks.ICE,
			Blocks.PACKED_ICE,
			Blocks.WATER,
			Blocks.LAVA,
			Blocks.RED_SANDSTONE,
			Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA, Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA
	);

	private Optional<BlockState> getPointBlockState(BlockPos pos, PointType type) {
		BlockState state = type == PointType.STRUCTURE_BORDER ? getStructureBorderState.apply(pos) : world.getBlockState(pos);
		if (type == PointType.STRUCTURE_BORDER && !STRUCTURE_BORDER_BLOCK_WHITELIST.contains(state.getBlock())) {
			return Optional.empty();
		}
		return Optional.of(state);
	}

	private int getYBelowFloatingIsland(int x, int y, int z) {
		return getYBelowFloatingIsland(x, y, z, true);
	}

	private int getYBelowFloatingIsland(int x, int y, int z, boolean useSkippables) {
		if (y - groundY > borderSize) {
			//try restarting search in between groundY and found Y
			int startAtY = groundY + ((y - groundY) / 2);
			int newY = useSkippables ? WorldStructureGenerator.getTargetY(world, x, z, true, startAtY) :
					BlockTools.getTopFilledHeight(world, x, z, false, startAtY);
			if (newY != startAtY) {
				return newY;
			}
		}
		return y;
	}
}
