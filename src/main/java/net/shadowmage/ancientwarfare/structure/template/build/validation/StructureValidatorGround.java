package net.shadowmage.ancientwarfare.structure.template.build.validation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.api.TemplateRuleBlock;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.validation.border.SmoothingMatrixBuilder;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldGenDetailedLogHelper;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldStructureGenerator;
import net.shadowmage.ancientwarfare.structure.worldgen.stats.PlacementRejectionReason;
import net.shadowmage.ancientwarfare.structure.worldgen.stats.WorldGenStatistics;

import java.util.Optional;

import static net.shadowmage.ancientwarfare.structure.template.build.validation.properties.StructureValidationProperties.MAX_GENERATION_HEIGHT;
import static net.shadowmage.ancientwarfare.structure.template.build.validation.properties.StructureValidationProperties.MIN_GENERATION_HEIGHT;

public class StructureValidatorGround extends StructureValidator {
	StructureValidatorGround() {
		super(StructureValidationType.GROUND);
	}

	@Override
	public boolean shouldIncludeForSelection(Level world, int x, int y, int z, Direction face, StructureTemplate template) {
		BlockState state = world.getBlockState(new BlockPos(x, y - 1, z));
		Block block = state.getBlock();
		if (!AWStructureStatics.isValidTargetBlock(state)) {
			//noinspection ConstantConditions
			WorldGenDetailedLogHelper.log("Rejecting due to target block mismatch of: {} at: {},{},{}", () -> BuiltInRegistries.BLOCK.getKey(block).toString(), () -> x, () -> y, () -> z);
			return false;
		}
		return true;
	}

	@Override
	public boolean readsBorderRingOnly() {
		return true; //validatePlacement is validateBorderBlocks (plus height checks without world access)
	}

	@Override
	public boolean validatePlacement(Level world, int x, int y, int z, Direction face, StructureTemplate template, StructureBB bb) {
		if (y - template.offset.getY() <= world.getMinBuildHeight()) { //1.12: <= 0
			WorldGenStatistics.addStructurePlacementRejection(template.name, PlacementRejectionReason.TOO_SHALLOW_GROUND);
			WorldGenDetailedLogHelper.log("Ground isn't deep enough for the structure \"{}\" required: {}, found: {}", () -> template.name, () -> Math.abs(bb.min.getY()), () -> y);
			return false;
		}

		if (y < getMinGenerationHeight() || y > getMaxGenerationHeight()) {
			WorldGenStatistics.addStructurePlacementRejection(template.name, PlacementRejectionReason.NOT_WITHIN_Y_LIMIT);
			WorldGenDetailedLogHelper.log("Structure \"{}\" isn't within required Y level bounds of min {} and max {}", () -> template.name, this::getMinGenerationHeight, this::getMaxGenerationHeight);
			return false;
		}

		int minY = getMinY(template, bb);
		int maxY = getMaxY(template, bb);
		boolean ret = validateBorderBlocks(world, bb, minY, maxY, false);
		if (!ret) {
			WorldGenStatistics.addStructurePlacementRejection(template.name, PlacementRejectionReason.INVALID_BORDER_HEIGHT_OR_BIOME);
		}
		return ret;
	}

	private int getMaxGenerationHeight() {
		return getPropertyValue(MAX_GENERATION_HEIGHT);
	}

	private int getMinGenerationHeight() {
		return getPropertyValue(MIN_GENERATION_HEIGHT);
	}

	@Override
	public void preGeneration(Level world, BlockPos pos, Direction face, StructureTemplate template, StructureBB bb) {
		if (!isPreserveBlocks()) {
			smoothoutBorder(world, bb, face, template);
			clearAboveBB(world, template, bb);
			prePlacementUnderfill(world, bb);
		}
	}

	//budgeted placement: preGeneration above as resumable steps (same order: smoothing, clearing above, underfill)
	@Override
	public net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps preGenerationSteps(Level world, BlockPos pos, Direction face, StructureTemplate template, StructureBB bb) {
		net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps steps = new net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps();
		if (!isPreserveBlocks()) {
			int borderSize = getBorderSize();
			int turns = (face.get2DDataValue() + 2) % 4;
			if (borderSize > 0) {
				net.shadowmage.ancientwarfare.structure.template.build.validation.border.SmoothingMatrix[] matrix = new net.shadowmage.ancientwarfare.structure.template.build.validation.border.SmoothingMatrix[1];
				steps.thenSteps(() -> SmoothingMatrixBuilder.buildSteps(world, bb, borderSize, bb.min.getY() + template.getOffset().getY() - 1,
						p -> getStateFromTemplate(template, bb, turns, p), m -> matrix[0] = m));
				steps.thenSteps(() -> matrix[0].applySteps(world, p -> handleClearAction(world, p, template, bb)));
			}
			steps.thenEach(() -> BlockTools.getAllInBoxTopDown(bb.min.offset(0, bb.max.getY() - bb.min.getY() + 1, 0), bb.max.offset(0, 50 + getMaxLeveling(), 0)),
					p -> () -> handleClearAction(world, p, template, bb), 32);
			steps.thenList(() -> prePlacementUnderfillUnits(world, bb), 1);
		}
		return steps;
	}

	@Override
	public net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps postGenerationSteps(Level world, BlockPos origin, StructureBB bb, StructureTemplate template) {
		return new net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps().thenList(() -> world.getBiome(origin.above()).value().coldEnoughToSnow(origin.above())
				? WorldStructureGenerator.sprinkleSnowUnits(world, bb, getBorderSize()) : java.util.Collections.emptyList(), 4);
	}

	private void smoothoutBorder(Level world, StructureBB bb, Direction face, StructureTemplate template) {
		int borderSize = getBorderSize();
		int turns = (face.get2DDataValue() + 2) % 4;

		if (borderSize > 0) {
			new SmoothingMatrixBuilder(world, bb, borderSize, bb.min.getY() + template.getOffset().getY() - 1,
					p -> getStateFromTemplate(template, bb, turns, p)).build()
					.apply(world, pos -> handleClearAction(world, pos, template, bb));
		}
	}

	private BlockState getStateFromTemplate(StructureTemplate template, StructureBB bb, int turns, BlockPos pos) {
		int xSize = turns % 2 == 0 ? template.getSize().getX() : template.getSize().getZ();
		int zSize = turns % 2 == 0 ? template.getSize().getZ() : template.getSize().getX();

		Optional<TemplateRuleBlock> rule = template.getRuleAt(BlockTools.rotateInArea(pos.offset(-bb.min.getX(), -bb.min.getY(), -bb.min.getZ()), xSize, zSize, -turns));
		return rule.map(r -> r.getState(turns)).orElse(Blocks.DIRT.defaultBlockState());
	}

	private void clearAboveBB(Level world, StructureTemplate template, StructureBB bb) {
		BlockTools.getAllInBoxTopDown(bb.min.offset(0, bb.max.getY() - bb.min.getY() + 1, 0), bb.max.offset(0, 50 + getMaxLeveling(), 0)).forEach(pos -> handleClearAction(world, pos, template, bb));
	}

	@Override
	public void postGeneration(Level world, BlockPos origin, StructureBB bb, StructureTemplate template) {
		if (world.getBiome(origin.above()).value().coldEnoughToSnow(origin.above())) {
			WorldStructureGenerator.sprinkleSnow(world, bb, getBorderSize());
		}
	}
}
