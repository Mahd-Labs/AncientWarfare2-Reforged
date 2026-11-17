package net.shadowmage.ancientwarfare.npc.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.npc.tile.TileTownHall;

/*
 * Port notes: Material.ROCK -> stone map colour / instrument; isOpaqueCube false -> noOcclusion; the CUTOUT render
 * layer is the "render_type" of the block model json.
 */
public class BlockTownHall extends BlockBaseNPC implements EntityBlock {

	public BlockTownHall() {
		super(baseProperties().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).noOcclusion(), "town_hall");
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileTownHall(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return ITickableTile.ticker();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		return WorldTools.clickInteractableTileWithHand(world, pos, player, InteractionHand.MAIN_HAND) ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
	}

	/*
	 * Lets the block know when one of its neighbor changes. Doesn't know which neighbor changed (coordinates passed are
	 * their own) Args: x, y, z, neighbor Block
	 */
	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
		if (!world.isClientSide) {
			WorldTools.getTile(world, pos, TileTownHall.class).ifPresent(t -> t.alarmActive = world.hasNeighborSignal(pos));
		}
	}
}
