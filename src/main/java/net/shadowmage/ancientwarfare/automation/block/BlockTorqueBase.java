package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueBase;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableBlock;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableTile;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

/*
 * Port notes:
 * - The 1.12 unlisted facing (tile orientation shown through the extended state) is the real block state property
 *   FACING (all six directions; four-way blocks only ever use the horizontal ones). TileTorqueBase.getPrimaryFacing /
 *   setPrimaryFacing read and write it, so "/setblock ...[facing=east]" works without tile data.
 * - The other unlisted properties (rotations etc.) are gone, see AutomationProperties.
 * - isOpaqueCube/isNormalCube/isFullCube/isSideSolid false -> noOcclusion().
 */
public abstract class BlockTorqueBase extends BlockBaseAutomation implements IRotatableBlock, EntityBlock {
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	protected BlockTorqueBase(BlockBehaviour.Properties properties, String regName) {
		super(properties.noOcclusion(), regName);
		registerDefaultState(makeDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)));
	}

	/*
	 * sub classes set the default values of the properties they added in addProperties
	 */
	protected BlockState makeDefaultState(BlockState state) {
		return state;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		return WorldTools.clickInteractableTileWithHand(world, pos, player, InteractionHand.MAIN_HAND) ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
	}

	@Override
	protected final void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		addProperties(builder);
	}

	protected void addProperties(StateDefinition.Builder<Block, BlockState> builder) {
	}

	@Override
	public void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbor) {
		WorldTools.getTile(world, pos, TileTorqueBase.class).ifPresent(TileTorqueBase::onNeighborTileChanged);
		super.onNeighborChange(state, world, pos, neighbor);
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
		WorldTools.getTile(world, pos, TileTorqueBase.class).ifPresent(TileTorqueBase::onNeighborTileChanged);
		super.neighborChanged(state, world, pos, block, fromPos, movedByPiston);
	}

	@Override
	public boolean rotateBlock(Level world, BlockPos pos, Direction axis) {
		if (world.isClientSide) { //TODO is this needed or is it always server side?
			return false;
		}
		//noinspection ConstantConditions
		IRotatableTile tt = WorldTools.getTile(world, pos, IRotatableTile.class).get();
		Direction facing = tt.getPrimaryFacing();
		Direction rotatedFacing = facing;
		if (axis.getAxis() == Direction.Axis.Y || getRotationType() == BlockRotationHandler.RotationType.SIX_WAY) {
			rotatedFacing = facing.getClockWise(axis.getAxis());
		}
		if (facing != rotatedFacing) {
			tt.setPrimaryFacing(rotatedFacing);
			return true;
		}
		return false;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return ITickableTile.ticker();
	}
}
