package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.shadowmage.ancientwarfare.core.util.RayTraceUtils;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.tile.TileChair;
import net.shadowmage.ancientwarfare.structure.util.RotationLimit;
import net.shadowmage.ancientwarfare.structure.util.WoodVariantHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static net.shadowmage.ancientwarfare.core.render.property.CoreProperties.FACING;
import static net.shadowmage.ancientwarfare.core.render.property.CoreProperties.VISIBLE;
import static net.shadowmage.ancientwarfare.structure.util.BlockStateProperties.VARIANT;

public class BlockChair extends BlockSeat implements EntityBlock {
	private static final Vec3 SEAT_OFFSET = new Vec3(0.5, 0.47, 0.5);

	public BlockChair() {
		super(Material.WOOD.props(), "chair");
	}

	@Override
	public void onBlockPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (!ItemTools.hasTag(stack)) {
			return;
		}
		//Port notes: FACING could not be stored in the 1.12 meta (taken from the tile in getActualState), it is a real state property now
		world.setBlockAndUpdate(pos, state.setValue(VARIANT, WoodVariantHelper.getVariant(stack)).setValue(VISIBLE, true).setValue(FACING, placer.getDirection().getOpposite()));
		WorldTools.getTile(world, pos, TileChair.class).ifPresent(te -> te.setPrimaryFacing(placer.getDirection().getOpposite()));
		world.setBlockAndUpdate(pos.above(), defaultBlockState().setValue(VARIANT, WoodVariantHelper.getVariant(stack)).setValue(VISIBLE, false).setValue(FACING, placer.getDirection().getOpposite()));
		WorldTools.getTile(world, pos.above(), TileChair.class).ifPresent(te -> {
			te.setPrimaryFacing(placer.getDirection().getOpposite());
			te.setMainBlockPos(pos);
		});
	}

	@Override
	public ItemStack getPickBlock(BlockState state, HitResult target, Level world, BlockPos pos, Player player) {
		return WoodVariantHelper.getPickBlock(this, state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(VARIANT, FACING, VISIBLE);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileChair(pos, state);
	}

	private static final List<AABB> AABBs = ImmutableList.of(
			new AABB(1 / 16D, 9 / 16D, 1 / 16D, 15 / 16D, 11 / 16D, 15 / 16D),
			new AABB(1 / 16D, 0, 1 / 16D, 3 / 16D, 9 / 16D, 3 / 16D),
			new AABB(13 / 16D, 0, 1 / 16D, 15 / 16D, 9 / 16D, 3 / 16D),
			new AABB(13 / 16D, 0, 13 / 16D, 15 / 16D, 9 / 16D, 15 / 16D),
			new AABB(1 / 16D, 0, 13 / 16D, 3 / 16D, 9 / 16D, 15 / 16D));

	private static final Map<Direction, List<AABB>> BOTTOM_AABBs = ImmutableMap.of(
			Direction.NORTH, new ImmutableList.Builder<AABB>().addAll(AABBs)
					.add(new AABB(1 / 16D, 11 / 16D, 13 / 16D, 15 / 16D, 1, 15 / 16D)).build(),
			Direction.SOUTH, new ImmutableList.Builder<AABB>().addAll(AABBs)
					.add(new AABB(1 / 16D, 11 / 16D, 1 / 16D, 15 / 16D, 1, 3 / 16D)).build(),
			Direction.EAST, new ImmutableList.Builder<AABB>().addAll(AABBs)
					.add(new AABB(1 / 16D, 11 / 16D, 1 / 16D, 3 / 16D, 1, 15 / 16D)).build(),
			Direction.WEST, new ImmutableList.Builder<AABB>().addAll(AABBs)
					.add(new AABB(13 / 16D, 11 / 16D, 1 / 16D, 15 / 16D, 1, 15 / 16D)).build()
	);

	private static final Map<Direction, AABB> TOP_AABBs = ImmutableMap.of(
			Direction.NORTH, new AABB(1 / 16D, 0, 13 / 16D, 15 / 16D, 9 / 16D, 15 / 16D),
			Direction.SOUTH, new AABB(1 / 16D, 0, 1 / 16D, 15 / 16D, 9 / 16D, 3 / 16D),
			Direction.EAST, new AABB(1 / 16D, 0, 1 / 16D, 3 / 16D, 9 / 16D, 15 / 16D),
			Direction.WEST, new AABB(13 / 16D, 0, 1 / 16D, 15 / 16D, 9 / 16D, 15 / 16D)
	);

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return state.getValue(VISIBLE) ? RenderShape.MODEL : RenderShape.INVISIBLE;
	}

	@Override
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player playerIn, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		if (state.getValue(VISIBLE)) {
			return super.onBlockActivated(world, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
		}
		BlockState stateDown = world.getBlockState(pos.below());
		return activateOther(stateDown.getBlock(), world, pos.below(), stateDown, playerIn, hand, facing, hitX, hitY, hitZ);
	}

	@Override
	protected List<AABB> getShapeBoxes(BlockState blockState, BlockGetter world, BlockPos pos) {
		Direction facing = blockState.getValue(FACING);
		return blockState.getValue(VISIBLE) ? BOTTOM_AABBs.get(facing) : List.of(TOP_AABBs.get(facing));
	}

	@Override
	protected List<AABB> getCollisionBoxes(BlockState state, BlockGetter world, BlockPos pos) {
		return null;
	}

	@Override
	public AABB getCollisionBoundingBox(BlockState blockState, BlockGetter world, BlockPos pos) {
		return blockState.getValue(VISIBLE) ? AABBs.get(0) : TOP_AABBs.get(blockState.getValue(FACING));
	}

	@Override
	protected Vec3 getSeatOffset() {
		return SEAT_OFFSET;
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
		WoodVariantHelper.getDrops(this, drops, state);
	}

	@Override
	public RotationLimit getRotationLimit(Level world, BlockPos seatPos, BlockState state) {
		return new RotationLimit.FacingThreeQuarters(state.getValue(FACING));
	}
}
