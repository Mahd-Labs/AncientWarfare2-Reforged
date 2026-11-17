package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.shadowmage.ancientwarfare.core.util.RayTraceUtils;
import net.shadowmage.ancientwarfare.structure.util.RotationLimit;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static net.shadowmage.ancientwarfare.core.render.property.CoreProperties.FACING;

public class BlockScissorSeat extends BlockSeat {
	public BlockScissorSeat() {
		super(Material.WOOD.props(), "scissor_seat");
	}

	@Override
	public RotationLimit getRotationLimit(Level world, BlockPos seatPos, BlockState state) {
		return new RotationLimit.FacingQuarter(state.getValue(FACING));
	}

	@Override
	public void onBlockPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		world.setBlockAndUpdate(pos, state.setValue(FACING, placer.getDirection().getOpposite()));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	protected Vec3 getSeatOffset() {
		return new Vec3(0.5, 0.27, 0.5);
	}

	private static final Map<Direction, List<AABB>> AABBs = ImmutableMap.of(
			Direction.NORTH, new ImmutableList.Builder<AABB>()
					.add(new AABB(3 / 16D, 0, 3 / 16D, 13 / 16D, 8 / 16D, 12 / 16D))
					.add(new AABB(1.5 / 16D, 11 / 16D, 12.5 / 16D, 14.5 / 16D, 15.5 / 16D, 13.5 / 16D))
					.add(new AABB(2 / 16D, 8 / 16D, 2 / 16D, 3 / 16D, 11 / 16D, 13 / 16D))
					.add(new AABB(13 / 16D, 8 / 16D, 2 / 16D, 14 / 16D, 11 / 16D, 13 / 16D))
					.build(),
			Direction.SOUTH, new ImmutableList.Builder<AABB>()
					.add(new AABB(3 / 16D, 0, 4 / 16D, 13 / 16D, 8 / 16D, 13 / 16D))
					.add(new AABB(1.5 / 16D, 11 / 16D, 2.5 / 16D, 14.5 / 16D, 15.5 / 16D, 3.5 / 16D))
					.add(new AABB(2 / 16D, 8 / 16D, 3 / 16D, 3 / 16D, 11 / 16D, 14 / 16D))
					.add(new AABB(13 / 16D, 8 / 16D, 3 / 16D, 14 / 16D, 11 / 16D, 14 / 16D))
					.build(),
			Direction.EAST, new ImmutableList.Builder<AABB>()
					.add(new AABB(4 / 16D, 0, 3 / 16D, 13 / 16D, 8 / 16D, 13 / 16D))
					.add(new AABB(2.5 / 16D, 11 / 16D, 1.5 / 16D, 3.5 / 16D, 15.5 / 16D, 14.5 / 16D))
					.add(new AABB(3 / 16D, 8 / 16D, 2 / 16D, 14 / 16D, 11 / 16D, 3 / 16D))
					.add(new AABB(3 / 16D, 8 / 16D, 13 / 16D, 14 / 16D, 11 / 16D, 14 / 16D))
					.build(),
			Direction.WEST, new ImmutableList.Builder<AABB>()
					.add(new AABB(3 / 16D, 0, 3 / 16D, 12 / 16D, 8 / 16D, 13 / 16D))
					.add(new AABB(12.5 / 16D, 11 / 16D, 1.5 / 16D, 13.5 / 16D, 15.5 / 16D, 14.5 / 16D))
					.add(new AABB(2 / 16D, 8 / 16D, 2 / 16D, 13 / 16D, 11 / 16D, 3 / 16D))
					.add(new AABB(2 / 16D, 8 / 16D, 13 / 16D, 13 / 16D, 11 / 16D, 14 / 16D))
					.build()
	);

	@Override
	protected List<AABB> getShapeBoxes(BlockState blockState, BlockGetter world, BlockPos pos) {

		Direction facing = blockState.getValue(FACING);

		return AABBs.get(facing);
	}

	@Override
	protected List<AABB> getCollisionBoxes(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return AABBs.get(state.getValue(FACING));
	}

}
