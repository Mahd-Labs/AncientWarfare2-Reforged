package net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.api.IStructureBuilder;

import java.util.Collections;
import java.util.List;

public class TemplateRuleBlockDoors extends TemplateRuleVanillaBlocks {

	public static final String PLUGIN_NAME = "doors";

	public TemplateRuleBlockDoors(Level world, BlockPos pos, BlockState state, int turns) {
		super(world, pos, state, turns);
	}

	public TemplateRuleBlockDoors() {
		super();
	}

	@Override
	public List<ItemStack> getResources() {
		if (state.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
			return super.getResources();
		}

		return Collections.emptyList();
	}

	@Override
	public void handlePlacement(Level world, int turns, BlockPos pos, IStructureBuilder builder) {
		if (state.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
			BlockState state = BlockTools.rotateFacing(this.state, turns);
			builder.placeBlock(pos, state, buildPass);
			builder.placeBlock(pos.below(), state.setValue(DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), buildPass);
		}
	}

	/*
	 * Port addition: 1.12 kept a door's facing in the lower half's metadata only, so the vanilla flattening
	 * table turns every upper half of a 1.12 template into facing=east (and every lower half into hinge=right). The
	 * 1.12 rule placed both halves from the state saved in the template; for rules of original 1.12 templates (no
	 * data version) that saved facing, hinge, open and powered are put back after the upgrade.
	 */
	@Override
	public void parseRule(CompoundTag tag) {
		super.parseRule(tag);
		if (legacyData && state.getBlock() instanceof DoorBlock) {
			state = net.shadowmage.ancientwarfare.structure.util.LegacyBlockStates.withLegacyProperties(state, tag.getCompound("blockState"), "facing", "hinge", "open", "powered");
		}
	}

	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}
}
