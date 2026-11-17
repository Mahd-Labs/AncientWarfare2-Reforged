package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.tile.torque.multiblock.TileWindmillBlade;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import javax.annotation.Nullable;
import java.util.Optional;

/*
 * Port note: the unlisted FORMED, IS_CONTROL, HEIGHT, ROTATION, facing and DYNAMIC properties are read from the tile
 * by the renderer.
 */
public class BlockWindmillBlade extends BlockBaseAutomation implements EntityBlock {

	public BlockWindmillBlade(String regName) {
		super(wood().noOcclusion(), regName);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		WorldTools.getTile(world, pos, TileWindmillBlade.class).ifPresent(TileWindmillBlade::blockPlaced);
	}

	@Override
	protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean movedByPiston) {
		Optional<TileWindmillBlade> te = state.is(newState.getBlock()) ? Optional.empty() : WorldTools.getTile(world, pos, TileWindmillBlade.class);
		super.onRemove(state, world, pos, newState, movedByPiston);
		te.ifPresent(TileWindmillBlade::blockBroken); //have to call post block-break so that the tile properly sees the block/tile as gone
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileWindmillBlade(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return ITickableTile.ticker();
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 60;
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 20;
	}
}
