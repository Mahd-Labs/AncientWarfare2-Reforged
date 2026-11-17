package net.shadowmage.ancientwarfare.automation.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableBlock;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableTile;
import net.shadowmage.ancientwarfare.core.interfaces.IBoundedSite;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import net.shadowmage.ancientwarfare.core.owner.IOwnable;
import net.shadowmage.ancientwarfare.core.util.BlockTools;

public class ItemBlockWorksiteStatic extends ItemBlockBase {

	public ItemBlockWorksiteStatic(Block block) {
		super(block);
	}

	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState newState) {
		Player player = context.getPlayer();
		if (player == null) {
			return super.placeBlock(context, newState);
		}
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction playerFacing = player.getDirection();
		BlockPos pos1 = pos.relative(playerFacing).relative(playerFacing.getCounterClockWise(), 2);
		BlockPos pos2 = pos.relative(playerFacing, 4).relative(playerFacing.getClockWise(), 4);
		/*
		 * TODO validate that block is not inside work bounds of any other nearby worksites ??
         * TODO validate that worksite does not intersect any others
         */
		boolean val = super.placeBlock(context, newState);
		if (val) {
			BlockEntity worksite = world.getBlockEntity(pos);
			if (worksite instanceof IBoundedSite) {
				((IBoundedSite) worksite).setBounds(pos1, pos2);
			}
			if (worksite instanceof IOwnable) {
				((IOwnable) worksite).setOwner(player);
			}
			if (worksite instanceof IRotatableTile) {
				Direction facing = BlockRotationHandler.getFaceForPlacement(player, (IRotatableBlock) getBlock(), context.getClickedFace());
				((IRotatableTile) worksite).setPrimaryFacing(facing);
			}
			BlockTools.notifyBlockUpdate(world, pos);
		}
		return val;
	}

}
