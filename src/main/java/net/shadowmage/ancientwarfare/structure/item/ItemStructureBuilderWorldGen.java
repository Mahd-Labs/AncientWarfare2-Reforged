package net.shadowmage.ancientwarfare.structure.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBuilder;

public class ItemStructureBuilderWorldGen extends ItemStructureBuilder {
	public ItemStructureBuilderWorldGen(String name) {
		super(name);
	}

	@Override
	protected void buildStructure(Player player, BlockPos hit, Direction facing, StructureBuilder builder) {
		//budgeted_placement (improvement, not baseline): queued with the generation steps around it; with the switch off
		//preGeneration, instantConstruction, postGeneration at once
		net.minecraft.world.level.Level level = player.level();
		net.shadowmage.ancientwarfare.structure.template.build.StructurePlacementQueue.INSTANCE.build(builder,
				() -> builder.getTemplate().getValidationSettings().preGeneration(level, hit, facing, builder.getTemplate(), builder.getBoundingBox()),
				() -> builder.getTemplate().getValidationSettings().postGeneration(level, hit, builder.getBoundingBox(), builder.getTemplate()));
	}
}
