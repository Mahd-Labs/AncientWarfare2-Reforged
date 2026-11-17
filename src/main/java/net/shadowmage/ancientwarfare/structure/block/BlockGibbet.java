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

public class BlockGibbet extends BlockBaseStructure {
	private static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

	public BlockGibbet() {
		super(Material.WOOD.props().noOcclusion(), "gibbet");
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
		BlockState placeState = state.setValue(FACING, placer.getDirection().getOpposite());
		world.setBlockAndUpdate(pos, placeState.setValue(PART, Part.BOTTOM).setValue(VISIBLE, true));
		world.setBlockAndUpdate(pos.above(), placeState.setValue(PART, Part.BOTTOM).setValue(VISIBLE, false));
		world.setBlockAndUpdate(pos.above(2), placeState.setValue(PART, Part.TOP).setValue(VISIBLE, true));
		world.setBlockAndUpdate(pos.above(3), placeState.setValue(PART, Part.TOP).setValue(VISIBLE, false));
	}

	@Override
	public void breakBlock(Level world, BlockPos pos, BlockState state) {
		if (state.getValue(PART) != Part.BOTTOM || !state.getValue(VISIBLE)) {
			BlockState currentState = state;
			BlockPos currentPos = pos;
			for (int i = 1; i < 4 && (currentState.getValue(PART) != Part.BOTTOM || !currentState.getValue(VISIBLE)); i++) {
				currentPos = pos.below(i);
				currentState = world.getBlockState(currentPos);
				if (currentState.getBlock() != this) {
					return;
				}
			}
			if (currentState.getValue(PART) == Part.BOTTOM && currentState.getValue(VISIBLE)) {
				breakBlock(world, currentPos, currentState);
				return;
			}
		}

		world.removeBlock(pos.above(3), false);
		world.removeBlock(pos.above(2), false);
		world.removeBlock(pos.above(), false);
		world.removeBlock(pos, false);
		super.breakBlock(world, pos, state);
	}

	private static final Map<Direction, List<AABB>> BOTTOM_BASE_AABBs = ImmutableMap.of(
			Direction.NORTH, ImmutableList.of(
					new AABB(2 / 16D, 0, 0, 14 / 16D, 2 / 16D, 1)),
			Direction.SOUTH, ImmutableList.of(
					new AABB(2 / 16D, 0, 0, 14 / 16D, 2 / 16D, 1)),
			Direction.EAST, ImmutableList.of(
					new AABB(0, 0, 2 / 16D, 1, 2 / 16D, 14 / 16D)),
			Direction.WEST, ImmutableList.of(
					new AABB(0, 0, 2 / 16D, 1, 2 / 16D, 14 / 16D))
	);

	private static final AABB MIDDLE_AABB = new AABB(5.5 / 16D, 0, 5.5 / 16D, 10.5 / 16D, 1, 10.5 / 16D);

	private static final Map<Direction, List<AABB>> TOP_AABBs = ImmutableMap.of(
			Direction.NORTH, ImmutableList.of(
					new AABB(7 / 16D, 0, 7 / 16D, 9 / 16D, 10 / 16D, 9 / 16D),
					new AABB(6 / 16D, 10 / 16D, 0, 10 / 16D, 14 / 16D, 1)),
			Direction.SOUTH, ImmutableList.of(
					new AABB(7 / 16D, 0, 7 / 16D, 9 / 16D, 10 / 16D, 9 / 16D),
					new AABB(6 / 16D, 10 / 16D, 0, 10 / 16D, 14 / 16D, 1)),
			Direction.EAST, ImmutableList.of(
					new AABB(7 / 16D, 0, 7 / 16D, 9 / 16D, 10 / 16D, 9 / 16D),
					new AABB(0, 10 / 16D, 6 / 16D, 1, 14 / 16D, 10 / 16D)),
			Direction.WEST, ImmutableList.of(
					new AABB(7 / 16D, 0, 7 / 16D, 9 / 16D, 10 / 16D, 9 / 16D),
					new AABB(0, 10 / 16D, 6 / 16D, 1, 14 / 16D, 10 / 16D))
	);

	@Override
	protected List<AABB> getShapeBoxes(BlockState state, BlockGetter world, BlockPos pos) {
		if (state.getValue(PART) == Part.BOTTOM) {
			if (state.getValue(VISIBLE)) {
				return BOTTOM_BASE_AABBs.get(state.getValue(FACING));
			} else {
				return List.of(MIDDLE_AABB);
			}
		} else {
			if (state.getValue(VISIBLE)) {
				return List.of(MIDDLE_AABB);
			} else {
				return TOP_AABBs.get(state.getValue(FACING));
			}
		}
	}

	@Override
	protected List<AABB> getCollisionBoxes(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return TOP_AABBs.get(state.getValue(FACING));
	}
	public enum Part implements StringRepresentable {
		TOP("top", 0),
		BOTTOM("bottom", 1);

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
