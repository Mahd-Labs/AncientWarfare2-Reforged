package net.shadowmage.ancientwarfare.structure.template.build.validation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;

public class StructureValidatorWater extends StructureValidator {

	public StructureValidatorWater() {
		super(StructureValidationType.WATER);
	}

	@Override
	public boolean shouldIncludeForSelection(Level world, int x, int y, int z, Direction face, StructureTemplate template) {
		Block block = world.getBlockState(new BlockPos(x, y - 1, z)).getBlock();
		return block == Blocks.WATER;
	}

	@Override
	public boolean readsBorderRingOnly() {
		return true; //validatePlacement is validateBorderBlocks (plus height checks without world access)
	}

	@Override
	public boolean validatePlacement(Level world, int x, int y, int z, Direction face, StructureTemplate template, StructureBB bb) {
		int minY = getMinY(template, bb);
		return validateBorderBlocks(world, bb, 0, minY, true);
	}

	@Override
	public void preGeneration(Level world, BlockPos pos, Direction face, StructureTemplate template, StructureBB bb) {
		//noop
	}

	@Override
	public void handleClearAction(Level world, BlockPos pos, StructureTemplate template, StructureBB bb) {
		if (pos.getY() < bb.min.getY() + template.getOffset().getY()) {
			world.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
		} else {
			super.handleClearAction(world, pos, template, bb);
		}
	}
}
