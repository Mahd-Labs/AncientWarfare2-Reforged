package net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.api.IStructureBuilder;

import static net.shadowmage.ancientwarfare.structure.block.BlockProtectionFlag.ROTATION;

public class TemplateRuleFlag extends TemplateRuleBlockTile {
	public static final String PLUGIN_NAME = "flag";

	public TemplateRuleFlag(Level world, BlockPos pos, BlockState state, int turns) {
		super(world, pos, rotate(state, turns), turns);
	}

	public TemplateRuleFlag() {
		super();
	}

	@Override
	public void handlePlacement(Level world, int turns, BlockPos pos, IStructureBuilder builder) {
		upgradeLegacyData();
		builder.placeBlock(pos, getState(turns), buildPass);
		WorldTools.getTile(world, pos).ifPresent(t -> {
			tag.putInt("x", pos.getX());
			tag.putInt("y", pos.getY());
			tag.putInt("z", pos.getZ());
			t.loadWithComponents(tag, world.registryAccess());
		});
	}

	@Override
	public BlockState getState(int turns) {
		return rotate(state, turns);
	}

	private static BlockState rotate(BlockState state, int turns) {
		return state.setValue(ROTATION, (state.getValue(ROTATION) + 4 * turns) % 16);
	}

	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}
}
