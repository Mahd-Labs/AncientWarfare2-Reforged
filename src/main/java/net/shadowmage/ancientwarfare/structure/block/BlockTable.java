package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.util.RayTraceUtils;
import net.shadowmage.ancientwarfare.structure.util.BlockStateProperties;
import net.shadowmage.ancientwarfare.structure.util.WoodVariantHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BlockTable extends BlockBaseStructure {
	private static final BooleanProperty LEG_NORTHEAST = BooleanProperty.create("leg_northeast");
	private static final BooleanProperty LEG_SOUTHEAST = BooleanProperty.create("leg_southeast");
	private static final BooleanProperty LEG_SOUTHWEST = BooleanProperty.create("leg_southwest");
	private static final BooleanProperty LEG_NORTHWEST = BooleanProperty.create("leg_northwest");

	public BlockTable() {
		super(Material.WOOD.props().noOcclusion(), "table");
	}

	@Override
	public void onBlockPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (!ItemTools.hasTag(stack)) {
			return;
		}
		world.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.VARIANT, WoodVariantHelper.getVariant(stack)));
	}

	@Override
	public ItemStack getPickBlock(BlockState state, HitResult target, Level world, BlockPos pos, Player player) {
		return WoodVariantHelper.getPickBlock(this, state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.VARIANT, LEG_NORTHEAST, LEG_NORTHWEST, LEG_SOUTHEAST, LEG_SOUTHWEST);
	}

	@Override
	public BlockState getActualState(BlockState state, BlockGetter world, BlockPos pos) {
		LegPositions legPositions = getLegPositions(world, pos);

		state = state.setValue(LEG_NORTHEAST, legPositions.northEast);
		state = state.setValue(LEG_SOUTHEAST, legPositions.southEast);
		state = state.setValue(LEG_SOUTHWEST, legPositions.southWest);
		state = state.setValue(LEG_NORTHWEST, legPositions.northWest);
		return state;
	}

	private BlockTable.LegPositions getLegPositions(BlockGetter world, BlockPos pos) {
		boolean north = world.getBlockState(pos.north()).getBlock() == this;
		boolean east = world.getBlockState(pos.east()).getBlock() == this;
		boolean south = world.getBlockState(pos.south()).getBlock() == this;
		boolean west = world.getBlockState(pos.west()).getBlock() == this;

		return new LegPositions(!(north || east), !(south || east), !(south || west), !(north || west));
	}

	private static final AABB TOP_AABB = new AABB(0, 14 / 16D, 0, 1, 1, 1);
	private static final AABB LEG_NORTHWEST_AABB = new AABB(0, 0, 0, 2 / 16D, 14 / 16D, 2 / 16D);
	private static final AABB LEG_NORTHEAST_AABB = new AABB(14 / 16D, 0, 0, 1, 14 / 16D, 2 / 16D);
	private static final AABB LEG_SOUTHEAST_AABB = new AABB(14 / 16D, 0, 14 / 16D, 1, 14 / 16D, 1);
	private static final AABB LEG_SOUTHWEST_AABB = new AABB(0, 0, 14 / 16D, 2 / 16D, 14 / 16D, 1);

	@Override
	protected List<AABB> getShapeBoxes(BlockState blockState, BlockGetter world, BlockPos pos) {
		return getAABBs(world, pos);
	}

	private List<AABB> getAABBs(BlockGetter world, BlockPos pos) {
		LegPositions legPositions = getLegPositions(world, pos);

		List<AABB> aabbs = new ArrayList<>();
		aabbs.add(TOP_AABB);
		if (legPositions.northEast) {
			aabbs.add(LEG_NORTHEAST_AABB);
		}
		if (legPositions.northWest) {
			aabbs.add(LEG_NORTHWEST_AABB);
		}
		if (legPositions.southEast) {
			aabbs.add(LEG_SOUTHEAST_AABB);
		}
		if (legPositions.southWest) {
			aabbs.add(LEG_SOUTHWEST_AABB);
		}
		return aabbs;
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
		WoodVariantHelper.getDrops(this, drops, state);
	}

	private static class LegPositions {
		private final boolean northEast;
		private final boolean southEast;
		private final boolean southWest;
		private final boolean northWest;

		private LegPositions(boolean northEast, boolean southEast, boolean southWest, boolean northWest) {
			this.northEast = northEast;
			this.southEast = southEast;
			this.southWest = southWest;
			this.northWest = northWest;
		}
	}
}
