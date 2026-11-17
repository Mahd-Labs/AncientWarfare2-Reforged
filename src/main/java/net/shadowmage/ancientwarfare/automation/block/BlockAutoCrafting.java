package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.automation.tile.worksite.TileAutoCrafting;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;

/*
 * Port note: only FACING (1.12 unlisted horizontal facing from the tile), no ACTIVE property.
 */
public class BlockAutoCrafting extends BlockWorksiteBase {

	public BlockAutoCrafting(String regName) {
		super(wood().noOcclusion(), regName);
	}

	@Override
	protected boolean hasActiveProperty() {
		return false;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (!world.isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WORKSITE_AUTO_CRAFT, pos);
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileAutoCrafting(pos, state);
	}
}
