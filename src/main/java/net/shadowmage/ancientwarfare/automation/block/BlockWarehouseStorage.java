package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseStorage;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseStorageLarge;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseStorageMedium;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import java.util.Locale;

/*
 * Port note: 1.12 meta 0/1/2 -> size=small|medium|large (items warehouse_storage_small/medium/large).
 */
public class BlockWarehouseStorage extends BlockBaseAutomation implements EntityBlock {
	public static final EnumProperty<Size> SIZE = EnumProperty.create("size", Size.class);

	public BlockWarehouseStorage(String regName) {
		super(wood(), regName);
		registerDefaultState(stateDefinition.any().setValue(SIZE, Size.SMALL));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SIZE);
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return new ItemStack(AWAutomationBlocks.WAREHOUSE_STORAGE_ITEMS.get(state.getValue(SIZE).getMeta()).get());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		switch (state.getValue(SIZE)) {
			case MEDIUM:
				return new TileWarehouseStorageMedium(pos, state);
			case LARGE:
				return new TileWarehouseStorageLarge(pos, state);
			default:
				return new TileWarehouseStorage(pos, state);
		}
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return ITickableTile.ticker();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		return WorldTools.clickInteractableTileWithHand(world, pos, player, InteractionHand.MAIN_HAND) ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
	}

	public enum Size implements StringRepresentable {
		SMALL(0), MEDIUM(1), LARGE(2);

		private int meta;

		Size(int meta) {
			this.meta = meta;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ENGLISH);
		}

		public String getName() {
			return getSerializedName();
		}

		public int getMeta() {
			return meta;
		}

		public static Size byMetadata(int meta) {
			return values()[meta];
		}
	}
}
