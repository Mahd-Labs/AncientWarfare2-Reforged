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

public class StructureValidatorIsland extends StructureValidator {
	public StructureValidatorIsland() {
		super(StructureValidationType.ISLAND);
	}

	private int getMinWaterDepth() {
		return getPropertyValue(MIN_WATER_DEPTH);
	}

	private int getMaxWaterDepth() {
		return getPropertyValue(MAX_WATER_DEPTH);
	}

	private void setMinWaterDepth(int depth) {
		setPropertyValue(MIN_WATER_DEPTH, depth);
	}

	private void setMaxWaterDepth(int depth) {
		setPropertyValue(MAX_WATER_DEPTH, depth);
	}

	@Override
	protected void setDefaultSettings(StructureTemplate template) {
		setMaxWaterDepth(template.getOffset().getY());
		setMinWaterDepth(getMaxWaterDepth() / 2);
	}

	@Override
	public boolean shouldIncludeForSelection(Level world, int x, int y, int z, Direction face, StructureTemplate template) {
		int startY = y - 1;
		y = WorldStructureGenerator.getTargetY(world, x, z, true) + 1;
		int water = startY - y + 1;
		return !(water < getMinWaterDepth() || water > getMaxWaterDepth());
	}

	@Override
	public boolean readsBorderRingOnly() {
		return true; //validatePlacement is validateBorderBlocks (plus height checks without world access)
	}

	@Override
	public boolean validatePlacement(Level world, int x, int y, int z, Direction face, StructureTemplate template, StructureBB bb) {
		int minY = y - getMaxWaterDepth();
		int maxY = y - getMinWaterDepth();
		return validateBorderBlocks(world, bb, minY, maxY, true);
	}

	@Override
	public void preGeneration(Level world, BlockPos pos, Direction face, StructureTemplate template, StructureBB bb) {
		prePlacementUnderfill(world, bb);
	}

	//budgeted placement: preGeneration above as resumable steps
	@Override
	public net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps preGenerationSteps(Level world, BlockPos pos, Direction face, StructureTemplate template, StructureBB bb) {
		return new net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps().thenList(() -> prePlacementUnderfillUnits(world, bb), 1);
	}

	@Override
	public void handleClearAction(Level world, BlockPos pos, StructureTemplate template, StructureBB bb) {
		int maxWaterY = bb.min.getY() + template.getOffset().getY() - 1;
		if (pos.getY() <= maxWaterY) {
			world.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
		} else {
			super.handleClearAction(world, pos, template, bb);
		}
	}

}
