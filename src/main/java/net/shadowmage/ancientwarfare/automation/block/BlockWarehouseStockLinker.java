package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.automation.item.ItemBlockWarehouseStockLinker;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseStockLinker;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableBlock;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.RotationType;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import javax.annotation.Nullable;

/*
 * Port notes: the linked warehouse position is in the item's custom data under WAREHOUSE_POS_TAG as {X,Y,Z}
 * (ItemBlockWarehouseStockLinker.readPos / writePos; 1.12 NBTUtil pos tag layout). The unused 1.12 field "filters"
 * is dropped.
 */
public class BlockWarehouseStockLinker extends BlockBaseAutomation implements IRotatableBlock, EntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BooleanProperty.create("lit");

	public BlockWarehouseStockLinker(String regName) {
		super(rock().noOcclusion(), regName);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileWarehouseStockLinker(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return ITickableTile.ticker();
	}

	@Override
	protected boolean isSignalSource(BlockState iBlockState) {
		return true;
	}

	@Override
	protected int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
		return isPowered(blockAccess, pos) ? 15 : 0;
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
		boolean doPower = isPowered(world, pos);
		if (!doPower) {
			return 0;
		} else {
			return state.getValue(FACING) == side ? 15 : 0;
		}
	}

	private boolean isPowered(BlockGetter world, BlockPos pos) {
		boolean doPower = false;
		BlockEntity tileentity = world.getBlockEntity(pos);
		if (tileentity instanceof TileWarehouseStockLinker) {
			TileWarehouseStockLinker tileWarehouseStockLinker = (TileWarehouseStockLinker) tileentity;
			doPower = tileWarehouseStockLinker.getEqualityHandle();
		}
		return doPower;
	}

	public static void setActiveState(boolean active, Level world, BlockPos pos) {
		world.setBlockAndUpdate(pos, world.getBlockState(pos).setValue(LIT, active));
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity player, ItemStack stack) {
		super.setPlacedBy(world, pos, state, player, stack);
		if (!world.isClientSide && ItemTools.hasTag(stack)) {
			BlockEntity tileentity = world.getBlockEntity(pos);
			if (tileentity instanceof TileWarehouseStockLinker) {
				TileWarehouseStockLinker tileWarehouseStockLinker = (TileWarehouseStockLinker) tileentity;
				CompoundTag tag = ItemTools.getTag(stack).getCompound(ItemBlockWarehouseStockLinker.WAREHOUSE_POS_TAG);
				tileWarehouseStockLinker.setWarehousePos(ItemBlockWarehouseStockLinker.readPos(tag));
			}
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(LIT, false);
	}

	@Override
	public RotationType getRotationType() {
		return RotationType.FOUR_WAY;
	}

	@Override
	public boolean invertFacing() {
		return true;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext context) {
		return BlockWarehouseStockViewer.AABBS.get(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		return WorldTools.clickInteractableTileWithHand(world, pos, player, InteractionHand.MAIN_HAND) ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
	}
}
