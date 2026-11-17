package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import com.google.common.collect.ImmutableMap;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.ModelLoaderHelper;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.util.TopBottomPart;
import net.shadowmage.ancientwarfare.structure.tile.TileStake;

import javax.annotation.Nullable;
import java.util.Map;

import static net.shadowmage.ancientwarfare.core.render.property.CoreProperties.FACING;
import static net.shadowmage.ancientwarfare.core.render.property.CoreProperties.VISIBLE;
import static net.shadowmage.ancientwarfare.structure.util.StructureProperties.TOP_BOTTOM_PART;
import static net.shadowmage.ancientwarfare.structure.util.TopBottomPart.BOTTOM;
import static net.shadowmage.ancientwarfare.structure.util.TopBottomPart.TOP;

public class BlockStake extends BlockBaseStructure implements EntityBlock {
	public static final BooleanProperty ON_FIRE = BooleanProperty.create("on_fire");

	public BlockStake() {
		super(Material.WOOD.props().noOcclusion(), "stake");
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, VISIBLE, TOP_BOTTOM_PART, ON_FIRE);
	}

	@Override
	public BlockState getActualState(BlockState state, BlockGetter world, BlockPos pos) {
		if (state.getValue(TOP_BOTTOM_PART) == BOTTOM) {
			return state.setValue(ON_FIRE, WorldTools.getTile(world, pos, TileStake.class).map(TileStake::burns).orElse(false));
		}
		return state.setValue(ON_FIRE, false);
	}

	//the light comes from the block entity; without this the light engine only asks the state after a chunk load
	@Override
	public boolean hasDynamicLightEmission(BlockState state) {
		return true;
	}

	@Override
	public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
		TopBottomPart part = state.getValue(TOP_BOTTOM_PART);
		BlockPos tePos = pos;
		if (!state.getValue(VISIBLE)) {
			tePos = pos.below(2);
		} else if (part == TOP) {
			tePos = pos.below();
		}
		return WorldTools.getTile(world, tePos, TileStake.class).map(te -> part == BOTTOM && te.burns() || te.isEntityOnFire()).orElse(false) ? 15 : 0;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return state.getValue(VISIBLE) ? super.getRenderShape(state) : RenderShape.INVISIBLE;
	}

	@Override
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		if (state.getValue(TOP_BOTTOM_PART) != BOTTOM) {
			return false;
		}

		if (!world.isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_STAKE, pos);
		}
		return true;
	}

	@Override
	public void onBlockPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		world.setBlockAndUpdate(pos.above(2), state.setValue(FACING, placer.getDirection().getOpposite()).setValue(TOP_BOTTOM_PART, TopBottomPart.TOP).setValue(VISIBLE, false));
		world.setBlockAndUpdate(pos.above(), state.setValue(FACING, placer.getDirection().getOpposite()).setValue(TOP_BOTTOM_PART, TopBottomPart.TOP).setValue(VISIBLE, true));
		world.setBlockAndUpdate(pos, state.setValue(FACING, placer.getDirection().getOpposite()).setValue(TOP_BOTTOM_PART, BOTTOM).setValue(VISIBLE, true));
	}

	private static final AABB POST_AABB = new AABB(6 / 16D, 0, 6 / 16D, 10 / 16D, 1, 10 / 16D);

	private static final Map<Direction, AABB> MIDDLE_AABB = ImmutableMap.of(
			Direction.NORTH, new AABB(3 / 16D, 0, 0, 13 / 16D, 1, 11 / 16D),
			Direction.SOUTH, new AABB(3 / 16D, 0, 5 / 16D, 13 / 16D, 1, 1),
			Direction.EAST, new AABB(5 / 16D, 0, 3 / 16D, 1, 1, 13 / 16D),
			Direction.WEST, new AABB(0, 0, 3 / 16D, 11 / 16D, 1, 13 / 16D)
	);

	@Override
	public AABB getBoundingBox(BlockState state, BlockGetter source, BlockPos pos) {
		TopBottomPart part = state.getValue(TOP_BOTTOM_PART);
		if (part == BOTTOM) {
			return super.getBoundingBox(state, source, pos);
		}
		return state.getValue(VISIBLE) ? MIDDLE_AABB.get(state.getValue(FACING)) : POST_AABB;
	}

	@Override
	public void breakBlock(Level world, BlockPos pos, BlockState state) {
		if (state.getValue(TOP_BOTTOM_PART) != BOTTOM) {
			BlockState currentState = state;
			BlockPos currentPos = pos;
			for (int i = 1; i < 3 && (currentState.getValue(TOP_BOTTOM_PART) != BOTTOM); i++) {
				currentPos = pos.below(i);
				currentState = world.getBlockState(currentPos);
				if (currentState.getBlock() != this) {
					return;
				}
			}
			if (currentState.getValue(TOP_BOTTOM_PART) == BOTTOM) {
				breakBlock(world, currentPos, currentState);
				return;
			}
		}

		world.removeBlock(pos.above(2), false);
		world.removeBlock(pos.above(), false);
		world.removeBlock(pos, false);
		super.breakBlock(world, pos, state);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileStake(pos, state);
	}

}
