package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.util.ModelLoaderHelper;
import net.shadowmage.ancientwarfare.core.util.RayTraceUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static net.shadowmage.ancientwarfare.core.render.property.CoreProperties.FACING;
import static net.shadowmage.ancientwarfare.core.render.property.CoreProperties.VISIBLE;

public class BlockStretchingRack extends BlockBaseStructure {
	private static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

	public BlockStretchingRack() {
		super(Material.WOOD.props().noOcclusion(), "stretching_rack");
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, VISIBLE, PART);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return state.getValue(VISIBLE) ? super.getRenderShape(state) : RenderShape.INVISIBLE;
	}

	@Override
	public void onBlockPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		Direction facing = placer.getDirection();
		BlockState placeState = state.setValue(FACING, placer.getDirection().getOpposite());
		world.setBlockAndUpdate(pos, placeState.setValue(PART, Part.SOUTH).setValue(VISIBLE, true));
		world.setBlockAndUpdate(pos.relative(facing), placeState.setValue(PART, Part.SOUTH).setValue(VISIBLE, false));
		world.setBlockAndUpdate(pos.relative(facing, 2), placeState.setValue(PART, Part.NORTH).setValue(VISIBLE, true));
		world.setBlockAndUpdate(pos.relative(facing, 3), placeState.setValue(PART, Part.NORTH).setValue(VISIBLE, false));
	}

	@Override
	public void breakBlock(Level world, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(FACING).getOpposite();
		if (state.getValue(PART) != Part.SOUTH || !state.getValue(VISIBLE)) {
			BlockState currentState = state;
			BlockPos currentPos = pos;
			for (int i = 1; i < 4 && (currentState.getValue(PART) !=  Part.SOUTH || !currentState.getValue(VISIBLE)); i++) {
				currentPos = pos.relative(facing.getOpposite(), i);
				currentState = world.getBlockState(currentPos);
				if (currentState.getBlock() != this) {
					return;
				}
			}
			if (currentState.getValue(PART) ==  Part.SOUTH && currentState.getValue(VISIBLE)) {
				breakBlock(world, currentPos, currentState);
				return;
			}
		}
		world.removeBlock(pos, false);
		world.removeBlock(pos.relative(facing), false);
		world.removeBlock(pos.relative(facing, 2), false);
		world.removeBlock(pos.relative(facing, 3), false);
		super.breakBlock(world, pos, state);
	}

	private static final Map<Direction, List<AABB>> SOUTH_AABBs = ImmutableMap.of(
			Direction.NORTH, ImmutableList.of(
					new AABB(1 / 16D, 0, 0, 15 / 16D, 17/ 16D, 1)),
			Direction.SOUTH, ImmutableList.of(
					new AABB(1 / 16D, 0, 0, 15/ 16D, 17/ 16D, 1)),
			Direction.EAST, ImmutableList.of(
					new AABB(0, 0, 1 / 16D, 1, 17/ 16D, 15/ 16D)),
			Direction.WEST, ImmutableList.of(
					new AABB(0, 0, 1 / 16D, 1, 17/ 16D, 15/ 16D))
	);

	private static final Map<Direction, List<AABB>> MID_AABBs = ImmutableMap.of(
			Direction.NORTH, ImmutableList.of(
					new AABB(1 / 16D, 13 / 16D, 0, 15 / 16D, 17/ 16D, 1)),
			Direction.SOUTH, ImmutableList.of(
					new AABB(1 / 16D, 13 / 16D, 0, 15/ 16D, 17/ 16D, 1)),
			Direction.EAST, ImmutableList.of(
					new AABB(0, 13 / 16D, 1 / 16D, 1, 17/ 16D, 15/ 16D)),
			Direction.WEST, ImmutableList.of(
					new AABB(0, 13 / 16D, 1 / 16D, 1, 17/ 16D, 15/ 16D))
	);

	private static final Map<Direction,List<AABB>> NORTH_AABBs = ImmutableMap.of(
			Direction.NORTH, ImmutableList.of(
					new AABB(0, 0, 0, 1, 17/ 16D, 8 / 16D)),
			Direction.SOUTH, ImmutableList.of(
					new AABB(0, 0, 8 / 16D, 1, 17/ 16D, 1)),
			Direction.EAST, ImmutableList.of(
					new AABB(8 / 16D, 0, 0, 1, 17/ 16D, 1)),
			Direction.WEST, ImmutableList.of(
					new AABB(0, 0, 0, 8 / 16D, 17/ 16D, 1))
	);

	@Override
	protected List<AABB> getShapeBoxes(BlockState state, BlockGetter world, BlockPos pos) {
		if (state.getValue(PART) == Part.SOUTH) {
			if (state.getValue(VISIBLE)) {
				return SOUTH_AABBs.get(state.getValue(FACING));
			} else {
				return MID_AABBs.get(state.getValue(FACING));
			}
		} else {
			if (state.getValue(VISIBLE)) {
				return MID_AABBs.get(state.getValue(FACING));
			} else {
				return NORTH_AABBs.get(state.getValue(FACING));
			}
		}
	}

	@Override
	protected List<AABB> getCollisionBoxes(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return NORTH_AABBs.get(state.getValue(FACING));
	}

	public enum Part implements StringRepresentable {
		NORTH("north", 0),
		SOUTH("south", 1);

		private String name;
		private int meta;

		Part(String name, int meta) {
			this.name = name;
			this.meta = meta;
		}

		@Override
	public String getSerializedName() {
			return name;
		}

		public int getMeta() {
			return meta;
		}

		private static final Map<Integer, Part> META_TO_PART;

		static {
			ImmutableMap.Builder<Integer, Part> builder = new ImmutableMap.Builder<>();
			for (Part part : values()) {
				builder.put(part.getMeta(), part);
			}
			META_TO_PART = builder.build();
		}

		public static Part byMeta(int meta) {
			return META_TO_PART.get(meta);
		}
	}

}
