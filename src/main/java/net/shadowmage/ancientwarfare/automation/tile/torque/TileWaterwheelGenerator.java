package net.shadowmage.ancientwarfare.automation.tile.torque;

import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.shadowmage.ancientwarfare.automation.block.BlockWaterwheelGenerator;
import net.shadowmage.ancientwarfare.automation.config.AWAutomationStatics;
import net.shadowmage.ancientwarfare.core.interfaces.ITorque.TorqueCell;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.Trig;

public class TileWaterwheelGenerator extends TileTorqueSingleCell {

	public float wheelRotation;
	public float lastWheelRotationDiff;

	private float rotTick;
	private byte rotationDirection = 1; //TODO get rid of magic numbers used for this

	private int updateTick;

	public boolean validSetup = false;

	public TileWaterwheelGenerator(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.WATERWHEEL_GENERATOR_TILE.get(), pos, state);
		torqueCell = new TorqueCell(0, 4, AWAutomationStatics.low_transfer_max, AWAutomationStatics.low_efficiency_factor);
		float maxWheelRpm = 20;
		rotTick = maxWheelRpm * AWAutomationStatics.rpmToRpt;
	}

	@Override
	public void update() {
		super.update();
		if (!level.isClientSide) {
			updateTick--;
			if (updateTick <= 0) {
				updateTick = 20;
				boolean valid = validateBlocks();
				if (valid != validSetup) {
					validSetup = valid;
					BlockState state = getBlockState();
					if (state.hasProperty(BlockWaterwheelGenerator.VALID_SETUP) && state.getValue(BlockWaterwheelGenerator.VALID_SETUP) != valid) {
						level.setBlock(worldPosition, state.setValue(BlockWaterwheelGenerator.VALID_SETUP, valid), 3);
					}
					BlockTools.notifyBlockUpdate(this);
				}
			}
			if (validSetup)//server, update power gen
			{
				torqueCell.setEnergy(torqueCell.getEnergy() + AWAutomationStatics.waterwheel_generator_output);
			}
		}
	}

	@Override
	protected void updateRotation() {
		super.updateRotation();
		if (validSetup) {
			lastWheelRotationDiff = (rotTick * (float) rotationDirection) * Trig.TORADIANS;
			wheelRotation += lastWheelRotationDiff;
			wheelRotation %= Trig.PI * 2;
		}
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		validSetup = tag.getBoolean("validSetup");
		rotationDirection = tag.getByte("rotationDirection");
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		tag.putBoolean("validSetup", validSetup);
		tag.putByte("rotationDirection", rotationDirection);
	}

	private boolean validateBlocks() {
		Direction d = orientation.getOpposite();
		//port: a vertical orientation (only reachable by /setblock or structure data) has no wheel layout; 1.12 would throw in rotateY
		if (d.getAxis().isVertical())
			return false;
		BlockPos innerPos = worldPosition.relative(d);
		//quick check for invalid setup
		//must have air inside the inner two blocks
		if (getValidationType(innerPos.above()) != 1 || getValidationType(innerPos) != 1)
			return false;
		BlockPos right = innerPos.relative(d.getClockWise());
		BlockPos left = innerPos.relative(d.getCounterClockWise());

		byte[] validationGrid = new byte[6];
		validationGrid[0] = getValidationType(left.above());
		validationGrid[1] = getValidationType(right.above());
		validationGrid[2] = getValidationType(left);
		validationGrid[3] = getValidationType(right);
		validationGrid[4] = getValidationType(left.below());
		validationGrid[5] = getValidationType(right.below());
		for (byte aValue : validationGrid) {
			if (aValue == 0) {
				return false;
			}
		}
		if (validationGrid[2] == 2 && validationGrid[4] == 2)//left side water flowing down
		{
			//check opposite side for air (underneath has already been checked by quick block check above)
			if (validationGrid[1] == 1 && validationGrid[3] == 1) {
				rotationDirection = -1;//TODO check if these are correct rotation directions
				return true;
			}
			return false;
		} else if (validationGrid[3] == 2 && validationGrid[5] == 2)//right side water flowing down
		{
			//check opposite side for air (underneath has already been checked by quick block check above)
			if (validationGrid[0] == 1 && validationGrid[2] == 1) {
				rotationDirection = 1;
				return true;
			}
			return false;
		} else//not a direct flow downwards, check underneath for flow
		{
			if (validationGrid[4] != 2 || validationGrid[5] != 2 || getValidationType(innerPos.below()) != 2) {
				return false;
			}
			BlockState stateLeft = level.getBlockState(left.below());
			BlockState stateRight = level.getBlockState(right.below());
			int levelLeft = !stateLeft.is(Blocks.WATER) ? 0 : stateLeft.getValue(LiquidBlock.LEVEL);
			int levelRight = !stateRight.is(Blocks.WATER) ? 0 : stateRight.getValue(LiquidBlock.LEVEL);
			rotationDirection = (byte) (levelLeft < levelRight ? -1 : levelRight < levelLeft ? 1 : 0);
			return true;
		}
	}

	private byte getValidationType(BlockPos pos) {
		if (level.isEmptyBlock(pos)) {
			return 1;
		}
		BlockState state = level.getBlockState(pos);
		if (state.is(Blocks.WATER)) {
			return 2;
		}
		return 0;//TODO get rid of these magic numbers
	}

	public AABB getRenderBoundingBox() {
		BlockPos pos = worldPosition;
		return new AABB(pos.getX() - 3, pos.getY() - 3, pos.getZ() - 3, pos.getX() + 4, pos.getY() + 4, pos.getZ() + 4);
	}

	@Override
	public float getClientOutputRotation(Direction from, float delta) {
		if (from == orientation.getOpposite()) {
			return getRenderRotation(wheelRotation, lastWheelRotationDiff, delta);
		}
		return super.getClientOutputRotation(from, delta);
	}
}
