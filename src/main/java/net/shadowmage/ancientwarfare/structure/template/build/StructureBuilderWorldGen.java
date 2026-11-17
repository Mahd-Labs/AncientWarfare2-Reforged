package net.shadowmage.ancientwarfare.structure.template.build;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;

public class StructureBuilderWorldGen extends StructureBuilder {

	public StructureBuilderWorldGen(Level world, StructureTemplate template, Direction face, BlockPos pos) {
		super(world, template, face, pos);
	}

	@Override
	public void instantConstruction() {
		template.getValidationSettings().preGeneration(world, buildOrigin, getBuildFace(), template, bb);
		super.instantConstruction();
		template.getValidationSettings().postGeneration(world, buildOrigin, bb, template);
	}

	//budgeted placement (StructureBuilder.constructUntil): the same steps as instantConstruction above, resumable
	@Override
	protected ResumableSteps beforeConstructionSteps() {
		return template.getValidationSettings().preGenerationSteps(world, buildOrigin, getBuildFace(), template, bb);
	}

	@Override
	protected ResumableSteps afterConstructionSteps() {
		return template.getValidationSettings().postGenerationSteps(world, buildOrigin, bb, template);
	}

	//budgeted placement: the chunk ticket of the build also covers the trees the validator clears around the structure
	@Override
	public int getChunkMargin() {
		return Math.max(super.getChunkMargin(), 10);
	}

}
