package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableBlock;
import net.shadowmage.ancientwarfare.core.owner.IOwnable;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

/*
 * Port note: FACING is the 1.12 CoreProperties.FACING (BlockHorizontal.FACING)
 */
public class ItemBlockOwnedRotatable extends ItemBlockBase {
	private IRotatableBlock rotatable;

	public <T extends Block & IRotatableBlock> ItemBlockOwnedRotatable(T block) {
		super(block);
		rotatable = block;
	}

	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState newState) {
		Player player = context.getPlayer();
		if (player == null) {
			return super.placeBlock(context, newState);
		}
		Direction facing = BlockRotationHandler.getFaceForPlacement(player, rotatable, context.getClickedFace());
		boolean val = super.placeBlock(context, newState.setValue(HorizontalDirectionalBlock.FACING, facing));
		if (val) {
			WorldTools.getTile(context.getLevel(), context.getClickedPos(), IOwnable.class).ifPresent(t -> t.setOwner(player));
		}
		return val;
	}
}
