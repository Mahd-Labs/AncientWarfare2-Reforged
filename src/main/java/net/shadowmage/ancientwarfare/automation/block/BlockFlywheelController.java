package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileFlywheelControllerHeavy;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileFlywheelControllerLight;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileFlywheelControllerMedium;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.RotationType;

/*
 * Port notes: 1.12 meta 0/1/2 -> tier=light|medium|heavy (items flywheel_controller_light/medium/heavy).
 * 1.12 isSideSolid = true has no direct counterpart for a non occluding block (support shapes come from the full cube
 * collision shape, which this block keeps).
 */
public class BlockFlywheelController extends BlockTorqueBase {

	public BlockFlywheelController(String regName) {
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
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		switch (state.getValue(AutomationProperties.TIER)) {
			case LIGHT:
				return new TileFlywheelControllerLight(pos, state);
			case MEDIUM:
				return new TileFlywheelControllerMedium(pos, state);
			case HEAVY:
				return new TileFlywheelControllerHeavy(pos, state);
		}
		return null;
	}

	@Override
	public boolean invertFacing() {
		return false;
	}

	@Override
	public RotationType getRotationType() {
		return RotationType.FOUR_WAY;
	}
}
