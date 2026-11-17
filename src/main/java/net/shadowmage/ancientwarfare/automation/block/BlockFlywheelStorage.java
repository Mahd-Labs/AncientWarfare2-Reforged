package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
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
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;
import net.shadowmage.ancientwarfare.automation.tile.torque.multiblock.TileFlywheelStorage;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import javax.annotation.Nullable;
import java.util.Optional;

import static net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties.TIER;

/*
 * Port notes: 1.12 meta 0/1/2 -> tier=light|medium|heavy (items flywheel_storage_light/medium/heavy). The unlisted
 * DYNAMIC, IS_CONTROL, HEIGHT, WIDTH, ROTATION properties are read from the tile by the renderer.
 */
public class BlockFlywheelStorage extends BlockBaseAutomation implements EntityBlock {

	public BlockFlywheelStorage(String regName) {
		super(rock().noOcclusion(), regName);
		registerDefaultState(stateDefinition.any().setValue(TIER, TorqueTier.LIGHT));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TIER);
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return new ItemStack(AWAutomationBlocks.getTierItem(this, state.getValue(TIER)));
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		WorldTools.getTile(world, pos, TileFlywheelStorage.class).ifPresent(TileFlywheelStorage::blockPlaced);
	}

	@Override
	protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean movedByPiston) {
		Optional<TileFlywheelStorage> te = state.is(newState.getBlock()) ? Optional.empty() : WorldTools.getTile(world, pos, TileFlywheelStorage.class);
		super.onRemove(state, world, pos, newState, movedByPiston);
		te.ifPresent(TileFlywheelStorage::blockBroken); //have to call post block-break so that the controller properly sees the block as gone
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileFlywheelStorage(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return ITickableTile.ticker();
	}
}
