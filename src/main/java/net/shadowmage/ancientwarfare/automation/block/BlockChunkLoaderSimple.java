package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.automation.tile.TileChunkLoaderSimple;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import javax.annotation.Nullable;

public class BlockChunkLoaderSimple extends BlockBaseAutomation implements EntityBlock {

	public BlockChunkLoaderSimple(String regName) {
		super(rock(), regName);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileChunkLoaderSimple(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return ITickableTile.ticker();
	}

	@Override
	public void breakBlock(Level world, BlockPos pos, BlockState state) {
		WorldTools.getTile(world, pos, TileChunkLoaderSimple.class).ifPresent(TileChunkLoaderSimple::releaseTicket);
		super.breakBlock(world, pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		return WorldTools.clickInteractableTileWithHand(world, pos, player, InteractionHand.MAIN_HAND) ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		if (!world.isClientSide) {
			WorldTools.getTile(world, pos, TileChunkLoaderSimple.class).ifPresent(TileChunkLoaderSimple::setupInitialTicket);
		}
	}
}
