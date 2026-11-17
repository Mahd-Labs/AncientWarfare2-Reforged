package net.shadowmage.ancientwarfare.structure.template.build.validation;

import net.shadowmage.ancientwarfare.structure.block.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import com.google.common.collect.ImmutableSet;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldStructureGenerator;

import java.util.Set;

public class StructureValidatorHarbor extends StructureValidator {
	//TODO get rid of the weird way these get values assigned through seemingly unrelated calls
	private BlockPos testMin = BlockPos.ZERO;
	private BlockPos testMax = BlockPos.ZERO;

	private Set<Block> validTargetBlocks = ImmutableSet.of(
			Blocks.DIRT,
			Blocks.GRASS_BLOCK,
			Blocks.STONE,
			Blocks.SAND,
			Blocks.GRAVEL,
			Blocks.SANDSTONE,
			Blocks.CLAY,
			Blocks.IRON_ORE,
			Blocks.COAL_ORE
	);

	public StructureValidatorHarbor() {
		super(StructureValidationType.HARBOR);
	}

	@Override
	protected void setDefaultSettings(StructureTemplate template) {
		//noop
	}

	@Override
	public boolean shouldIncludeForSelection(Level world, int x, int y, int z, Direction face, StructureTemplate template) {
		/*
		 * testing that front target position is valid block
         * then test back target position to ensure that it has water at same level
         * or at an acceptable level difference
         */
		Block block = world.getBlockState(new BlockPos(x, y - 1, z)).getBlock();
		if (validTargetBlocks.contains(block)) {
			testMin = new BlockPos(x, y, z).relative(face, template.getOffset().getZ());
			int by = WorldStructureGenerator.getTargetY(world, testMin.getX(), testMin.getZ(), false);
			if (y - by > getMaxFill()) {
				return false;
			}
			block = world.getBlockState(new BlockPos(testMin.getX(), by, testMin.getZ())).getBlock();
			if (block == Blocks.WATER) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getAdjustedSpawnY(Level world, int x, int y, int z, Direction face, StructureTemplate template, StructureBB bb) {
		testMin = new BlockPos(x, y, z).relative(face, template.getOffset().getZ());
		return WorldStructureGenerator.getTargetY(world, testMin.getX(), testMin.getZ(), false) + 1;
	}

	@Override
	public boolean validatePlacement(Level world, int x, int y, int z, Direction face, StructureTemplate template, StructureBB bb) {
		int minY = getMinY(template, bb);
		int maxY = getMaxY(template, bb);
		StructureBB temp = bb.getFrontCorners(face, testMin, testMax);
		testMin = temp.min;
		testMax = temp.max;
		for (int bx = testMin.getX(); bx <= testMax.getX(); bx++) {
			for (int bz = testMin.getZ(); bz <= testMax.getZ(); bz++) {
				if (!validateBlockHeightTypeAndBiome(world, bx, bz, minY, maxY, false)) {
					return false;
				}
			}
		}

		temp = bb.getRearCorners(face, testMin, testMax);
		testMin = temp.min;
		testMax = temp.max;
		for (int bx = testMin.getX(); bx <= testMax.getX(); bx++) {
			for (int bz = testMin.getZ(); bz <= testMax.getZ(); bz++) {
				if (!validateBlockHeightAndType(world, bx, bz, minY, maxY, false, state -> state.getFluidState().is(net.minecraft.tags.FluidTags.WATER))) {
					return false;
				}
			}
		}

		temp = bb.getRightCorners(face, testMin, testMax);
		testMin = temp.min;
		testMax = temp.max;
		for (int bx = testMin.getX(); bx <= testMax.getX(); bx++) {
			for (int bz = testMin.getZ(); bz <= testMax.getZ(); bz++) {
				if (!validateBlockHeightAndType(world, bx, bz, minY, maxY, false,
						state -> AWStructureStatics.isValidTargetBlock(state) || state.getFluidState().is(net.minecraft.tags.FluidTags.WATER))) {
					return false;
				}
			}
		}

		temp = bb.getLeftCorners(face, testMin, testMax);
		testMin = temp.min;
		testMax = temp.max;
		for (int bx = testMin.getX(); bx <= testMax.getX(); bx++) {
			for (int bz = testMin.getZ(); bz <= testMax.getZ(); bz++) {
				if (!validateBlockHeightAndType(world, bx, bz, minY, maxY, false,
						state -> AWStructureStatics.isValidTargetBlock(state) || state.getFluidState().is(net.minecraft.tags.FluidTags.WATER))) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public void preGeneration(Level world, BlockPos pos, Direction face, StructureTemplate template, StructureBB bb) {
		prePlacementBorder(world, template, bb);
	}

	//budgeted placement: preGeneration above as resumable steps
	@Override
	public net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps preGenerationSteps(Level world, BlockPos pos, Direction face, StructureTemplate template, StructureBB bb) {
		return new net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps().thenList(() -> prePlacementBorderUnits(world, template, bb), 1);
	}

	@Override
	public void handleClearAction(Level world, BlockPos pos, StructureTemplate template, StructureBB bb) {
		if (pos.getY() >= bb.min.getY() + template.getOffset().getY()) {
			super.handleClearAction(world, pos, template, bb);
		} else {
			world.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
		}
	}

}
