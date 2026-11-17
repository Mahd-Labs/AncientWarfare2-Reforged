package net.shadowmage.ancientwarfare.structure;

import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.command.CommandUtils;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;

/*
 * Port notes: the two places where 1.12 core code called AWStructureStatics directly (CORE_API.md "Hooks other code
 * must install"): BlockTools (skippable blocks) and the /awutils exportblocks columns.
 */
final class StructureCoreHooks {
	private StructureCoreHooks() {}

	static void install() {
		BlockTools.setSkippableBlockPredicate(AWStructureStatics::isSkippable);
		CommandUtils.setBlockExportInfo(new CommandUtils.IBlockExportInfo() {
			@Override
			public boolean isSkippable(BlockState state) {
				return AWStructureStatics.isSkippable(state);
			}

			@Override
			public boolean isSkippableMaterial(BlockState state) {
				return AWStructureStatics.isSkippableMaterial(state);
			}

			@Override
			public boolean isValidTargetBlock(BlockState state) {
				return AWStructureStatics.isValidTargetBlock(state);
			}

			@Override
			public boolean isValidTargetMaterial(BlockState state) {
				return AWStructureStatics.isValidTargetMaterial(state);
			}
		});
	}
}
