package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.tile.TileLootBasket;

import javax.annotation.Nullable;
import java.util.Optional;

import static net.shadowmage.ancientwarfare.core.render.property.CoreProperties.FACING;

public class BlockLootBasket extends BlockBaseStructure implements EntityBlock {
	private static final AABB SINGLE_SOUTH_NORTH = new AABB(0D, 0D, 1 / 16D, 1D, 12 / 16D, 15 / 16D);
	private static final AABB SINGLE_WEST_EAST = new AABB(1 / 16D, 0D, 0D, 15 / 16D, 12 / 16D, 1D);
	private static final AABB DOUBLE_NORTH = new AABB(0D, 0D, 0D, 1D, 12 / 16D, 13 / 16D);
	private static final AABB DOUBLE_SOUTH = new AABB(0D, 0D, 3 / 16D, 1D, 12 / 16D, 1D);
	private static final AABB DOUBLE_WEST = new AABB(0D, 0D, 0D, 13 / 16D, 12 / 16D, 1D);
	private static final AABB DOUBLE_EAST = new AABB(3 / 16D, 0D, 0D, 1D, 12 / 16D, 1D);

	private static final BooleanProperty DOUBLE = BooleanProperty.create("double");
	private static final BooleanProperty VISIBLE = BooleanProperty.create("visible");

	public BlockLootBasket() {
		//1.12 setHarvestLevel("axe", 0) -> mineable/axe tag
		super(Material.GRASS.props().noOcclusion().strength(2), "loot_basket");
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		//Port notes: the tile is a ChestBlockEntity, which reads the chest type from its block state whenever the state changes;
		//the basket carries the property (always single) for that reason only
		builder.add(FACING, DOUBLE, VISIBLE, net.minecraft.world.level.block.ChestBlock.TYPE);
	}

	@Override
	public BlockState getActualState(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return getDoubleDirection(worldIn, pos)
				.map(facing -> facing == Direction.NORTH || facing == Direction.WEST ?
						state.setValue(DOUBLE, false).setValue(VISIBLE, false) :
						state.setValue(FACING, facing.getClockWise()).setValue(DOUBLE, true).setValue(VISIBLE, true))
				.orElse(state.setValue(DOUBLE, false).setValue(VISIBLE, true));
	}

	@Override
	public AABB getBoundingBox(BlockState state, BlockGetter source, BlockPos pos) {
		if (source.getBlockState(pos.north()).getBlock() == this) {
			return DOUBLE_NORTH;
		} else if (source.getBlockState(pos.south()).getBlock() == this) {
			return DOUBLE_SOUTH;
		} else if (source.getBlockState(pos.west()).getBlock() == this) {
			return DOUBLE_WEST;
		} else if (source.getBlockState(pos.east()).getBlock() == this) {
			return DOUBLE_EAST;
		}
		return state.getValue(FACING).getAxis() == Direction.Axis.X ? SINGLE_WEST_EAST : SINGLE_SOUTH_NORTH;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileLootBasket(pos, state);
	}

	@Override
	public BlockState getStateForPlacement(Level world, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, LivingEntity placer) {
		return defaultBlockState().setValue(FACING, placer.getDirection().getOpposite());
	}

	@Override
	public boolean canPlaceBlockAt(Level world, BlockPos pos) {
		int basketsAround = 0;
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			BlockPos offsetPos = pos.relative(facing);
			if (world.getBlockState(offsetPos).getBlock() == this) {
				if (isDoubleBasket(world, offsetPos)) {
					return false;
				}
				basketsAround++;
				if (basketsAround > 1) {
					return false;
				}
			}
		}

		return true;
	}

	private Optional<Direction> getDoubleDirection(BlockGetter world, BlockPos pos) {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			if (world.getBlockState(pos.relative(facing)).getBlock() == this) {
				return Optional.of(facing);
			}
		}
		return Optional.empty();
	}

	private boolean isDoubleBasket(Level world, BlockPos pos) {
		return getDoubleDirection(world, pos).isPresent();
	}

	@Override
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		if (!world.isClientSide) {
			if (WorldTools.getTile(world, pos, TileLootBasket.class).map(te -> te.fillWithLootAndCheckIfGoodToOpen(player)).orElse(false)) {
				NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_LOOT_BASKET, pos);
			}
		}
		return true;
	}

}
