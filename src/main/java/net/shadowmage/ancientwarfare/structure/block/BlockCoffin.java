package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.function.BiFunction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import com.google.common.collect.ImmutableMap;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.Trig;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.tile.TileCoffin;

import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public abstract class BlockCoffin<T extends TileCoffin> extends BlockMulti<T> {
	public BlockCoffin(BlockBehaviour.Properties material, String regName, BiFunction<BlockPos, BlockState, T> instantiateTe, Class<T> teClass) {
		super(material.noOcclusion(), regName, instantiateTe, teClass);
	}

	@Override
	public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		return net.shadowmage.ancientwarfare.core.tile.ITickableTile.ticker();
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return Boolean.TRUE.equals(state.getValue(INVISIBLE)) ? RenderShape.INVISIBLE : RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
		if (!player.getAbilities().instabuild) {
			WorldTools.getTile(world, pos, TileCoffin.class)
					.ifPresent(te -> InventoryTools.dropItemInWorld(world, getVariantStack(te.getVariant()), pos));
		}
		return super.playerWillDestroy(world, pos, state, player);
	}

	protected abstract ItemStack getVariantStack(IVariant variant);

	protected abstract IVariant getDefaultVariant();

	@Override
	public ItemStack getPickBlock(BlockState state, HitResult target, Level world, BlockPos pos, Player player) {
		return getVariantStack(WorldTools.getTile(world, pos, TileCoffin.class).map(TileCoffin::getVariant).orElse(getDefaultVariant()));
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
		//drops handled in onBlockHarvested
	}

	@Override
	public boolean onBlockActivated(Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
		WorldTools.getTile(world, pos, TileCoffin.class).ifPresent(TileCoffin::open);
		return true;
	}

	public enum CoffinDirection implements StringRepresentable {
		NORTH(0, "north", Direction.NORTH),
		EAST(90, "east", Direction.EAST),
		SOUTH(180, "south", Direction.SOUTH),
		WEST(270, "west", Direction.WEST),
		NORTH_EAST(45, "north_east", Direction.NORTH),
		SOUTH_EAST(135, "south_east", Direction.NORTH),
		SOUTH_WEST(225, "south_west", Direction.NORTH),
		NORTH_WEST(315, "north_west", Direction.NORTH);

		private int rotationAngle;
		private String name;
		private Direction facing;

		CoffinDirection(int rotationAngle, String name, Direction facing) {
			this.rotationAngle = rotationAngle;
			this.name = name;
			this.facing = facing;
		}

		public static CoffinDirection fromYaw(float rotationYaw) {
			return fromRotation((int) Trig.wrapTo360(rotationYaw + 180 + 23) / 45 * 45);
		}

		public int getRotationAngle() {
			return rotationAngle;
		}

		public static CoffinDirection fromFacing(Direction facing) {
			switch (facing) {
				case SOUTH:
					return SOUTH;
				case EAST:
					return EAST;
				case WEST:
					return WEST;
				default:
				case NORTH:
					return NORTH;
			}
		}

		public CoffinDirection rotateY() {
			switch (this) {
				case EAST:
					return SOUTH;
				case SOUTH:
					return WEST;
				case WEST:
					return NORTH;
				case NORTH_EAST:
					return SOUTH_EAST;
				case SOUTH_EAST:
					return SOUTH_WEST;
				case SOUTH_WEST:
					return NORTH_WEST;
				case NORTH_WEST:
					return NORTH_EAST;
				default:
				case NORTH:
					return EAST;
			}
		}

		public Direction getFacing() {
			return facing;
		}

		@Override
	public String getSerializedName() {
			return name;
		}

		private static final Map<String, CoffinDirection> NAME_VALUES;
		private static final Map<Integer, CoffinDirection> ROTATION_VALUES;

		static {
			ImmutableMap.Builder<String, CoffinDirection> builder = ImmutableMap.builder();
			ImmutableMap.Builder<Integer, CoffinDirection> builderRotation = ImmutableMap.builder();
			for (CoffinDirection coffinDirection : values()) {
				builder.put(coffinDirection.getSerializedName(), coffinDirection);
				builderRotation.put(coffinDirection.getRotationAngle(), coffinDirection);
			}
			NAME_VALUES = builder.build();
			ROTATION_VALUES = builderRotation.build();
		}

		public static CoffinDirection fromName(String name) {
			return NAME_VALUES.getOrDefault(name, NORTH);
		}

		static CoffinDirection fromRotation(int rotationAngle) {
			return ROTATION_VALUES.getOrDefault(rotationAngle, NORTH);
		}
	}

	public interface IVariant extends StringRepresentable {
	}
}
