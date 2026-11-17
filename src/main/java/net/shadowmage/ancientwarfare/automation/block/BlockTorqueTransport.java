package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueSidedCell;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.RotationType;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import java.util.Optional;

/*
 * Port notes: 1.12 meta 0/1/2 -> block state property tier=light|medium|heavy; one block item per tier
 * (<name>_light, <name>_medium, <name>_heavy, see AWAutomationBlocks). setLightOpacity(1) has no counterpart
 * (non occluding blocks let light through).
 */
public abstract class BlockTorqueTransport extends BlockTorqueBase {

	protected BlockTorqueTransport(String regName) {
		super(rock(), regName);
	}

	@Override
	protected void addProperties(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AutomationProperties.TIER);
	}

	@Override
	protected BlockState makeDefaultState(BlockState state) {
		return state.setValue(AutomationProperties.TIER, TorqueTier.LIGHT);
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return new ItemStack(AWAutomationBlocks.getTierItem(this, state.getValue(AutomationProperties.TIER)));
	}

	@Override
	public RotationType getRotationType() {
		return RotationType.SIX_WAY;
	}

	@Override
	public boolean invertFacing() {
		return false;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		//TODO static AABBs and combination created based on the boolean array
		float min = 0.1875f;
		float max = 0.8125f;
		float x1 = min;
		float y1 = min;
		float z1 = min;
		float x2 = max;
		float y2 = max;
		float z2 = max;
		Optional<TileTorqueSidedCell> te = WorldTools.getTile(world, pos, TileTorqueSidedCell.class);
		if (te.isPresent()) {
			TileTorqueSidedCell tile = te.get();
			boolean[] sides = tile.getConnections();
			if (sides[0]) {
				y1 = 0.f;
			}
			if (sides[1]) {
				y2 = 1.f;
			}
			if (sides[2]) {
				z1 = 0.f;
			}
			if (sides[3]) {
				z2 = 1.f;
			}
			if (sides[4]) {
				x1 = 0.f;
			}
			if (sides[5]) {
				x2 = 1.f;
			}
		}
		return Shapes.box(x1, y1, z1, x2, y2, z2);
	}
}
