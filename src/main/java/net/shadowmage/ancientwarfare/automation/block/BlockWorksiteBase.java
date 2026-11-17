package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableBlock;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableTile;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.RotationType;
import net.shadowmage.ancientwarfare.core.interfaces.IInteractableTile;
import net.shadowmage.ancientwarfare.core.interfaces.IWorkSite;
import net.shadowmage.ancientwarfare.core.owner.IOwnable;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import javax.annotation.Nullable;
import java.util.Optional;

/*
 * Port notes:
 * - 1.12 filled FACING and ACTIVE from the tile in getActualState. Both are real block state properties now:
 *   TileWorksiteBase.getPrimaryFacing / setPrimaryFacing read and write FACING, and the tile calls setActiveState
 *   when isActive() changes on the server.
 * - the tile factory is a BlockEntityType.BlockEntitySupplier (pos, state).
 */
public class BlockWorksiteBase extends BlockBaseAutomation implements IRotatableBlock, EntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	private BlockEntityType.BlockEntitySupplier<? extends BlockEntity> tileFactory;

	public BlockWorksiteBase(String regName) {
		this(wood(), regName);
	}

	protected BlockWorksiteBase(BlockBehaviour.Properties properties, String regName) {
		super(properties, regName);
		BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
		if (hasActiveProperty()) {
			state = state.setValue(ACTIVE, false);
		}
		registerDefaultState(state);
	}

	/*
	 * called from the constructor of Block: must not use fields
	 */
	protected boolean hasActiveProperty() {
		return true;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		if (hasActiveProperty()) {
			builder.add(ACTIVE);
		}
	}

	/*
	 * Port addition: mirrors TileWorksiteBase.isActive() into the block state (server side)
	 */
	public static void setActiveState(Level world, BlockPos pos, boolean active) {
		BlockState state = world.getBlockState(pos);
		if (state.hasProperty(ACTIVE) && state.getValue(ACTIVE) != active) {
			world.setBlock(pos, state.setValue(ACTIVE, active), 3);
		}
	}

	public BlockWorksiteBase setTileFactory(BlockEntityType.BlockEntitySupplier<? extends BlockEntity> renderFactory) {
		this.tileFactory = renderFactory;
		return this;
	}

	/*
	 * made into a generic method so that farm blocks are easier to setup
	 * returned tiles must implement IWorksite (for team reference) and IInteractableTile (for interaction callback) if they wish to receive onBlockActivated calls<br>
	 * returned tiles must implement IBoundedTile if they want workbounds set from ItemBlockWorksite<br>
	 * returned tiles must implement IOwnable if they want owner-name set from ItemBlockWorksite<br>
	 */
	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return tileFactory == null ? null : tileFactory.create(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return ITickableTile.ticker();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		BlockEntity te = world.getBlockEntity(pos);
		if (te instanceof IInteractableTile) {
			boolean canClick = false;
			if (te instanceof IOwnable && ((IOwnable) te).isOwner(player))
				canClick = true;
			else if (te instanceof IWorkSite && ((IWorkSite) te).getOwner().isOwnerOrSameTeamOrFriend(player)) {
				canClick = true;
			}
			if (canClick) {
				return ((IInteractableTile) te).onBlockClicked(player, InteractionHand.MAIN_HAND) ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	public final RotationType getRotationType() {
		return RotationType.FOUR_WAY;
	}

	@Override
	public boolean invertFacing() {
		return true;
	}

	@Override
	public final boolean rotateBlock(Level world, BlockPos pos, Direction facing) {
		if (facing == Direction.DOWN || facing == Direction.UP) {
			Optional<IRotatableTile> te = WorldTools.getTile(world, pos, IRotatableTile.class);
			if (te.isPresent()) {
				if (!world.isClientSide) {
					rotateTile(facing, te.get());
				}
				return true;
			}
		}
		return false;
	}

	private void rotateTile(Direction facing, IRotatableTile te) {
		Direction o = te.getPrimaryFacing().getClockWise(facing.getAxis());
		te.setPrimaryFacing(o);//twb will send update packets / etc
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 5;
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 20;
	}
}
