package net.shadowmage.ancientwarfare.structure.template.build.validation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldStructureGenerator;

import static net.shadowmage.ancientwarfare.structure.template.build.validation.properties.StructureValidationProperties.MAX_WATER_DEPTH;
import static net.shadowmage.ancientwarfare.structure.template.build.validation.properties.StructureValidationProperties.MIN_WATER_DEPTH;

public class StructureValidatorUnderwater extends StructureValidator {

	public StructureValidatorUnderwater() {
		super(StructureValidationType.UNDERWATER);
	}

	@Override
	public boolean shouldIncludeForSelection(Level world, int x, int y, int z, Direction face, StructureTemplate template) {
		int startY = y;
		y = WorldStructureGenerator.getTargetY(world, x, z, true) + 1;
		int water = startY - y;
		return !(water < getPropertyValue(MIN_WATER_DEPTH) || water > getPropertyValue(MAX_WATER_DEPTH));
	}

	@Override
	public int getAdjustedSpawnY(Level world, int x, int y, int z, Direction face, StructureTemplate template, StructureBB bb) {
		return WorldStructureGenerator.getTargetY(world, x, z, true) + 1;
	}

	@Override
	public boolean readsBorderRingOnly() {
		return true; //validatePlacement is validateBorderBlocks (plus height checks without world access)
	}

	@Override
	public boolean validatePlacement(Level world, int x, int y, int z, Direction face, StructureTemplate template, StructureBB bb) {
		int minY = getMinY(template, bb);
		int maxY = getMaxY(template, bb);
		return validateBorderBlocks(world, bb, minY, maxY, true);
	}

	@Override
	public void preGeneration(Level world, BlockPos pos, Direction face, StructureTemplate template, StructureBB bb) {
		prePlacementBorder(world, template, bb);
		prePlacementUnderfill(world, bb);
	}

	//budgeted placement: preGeneration above as resumable steps (border, then underfill)
	@Override
	public net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps preGenerationSteps(Level world, BlockPos pos, Direction face, StructureTemplate template, StructureBB bb) {
		return new net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps().thenList(() -> prePlacementBorderUnits(world, template, bb), 1).thenList(() -> prePlacementUnderfillUnits(world, bb), 1);
	}

	@Override
	public void handleClearAction(Level world, BlockPos pos, StructureTemplate template, StructureBB bb) {
		world.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
	}

}
