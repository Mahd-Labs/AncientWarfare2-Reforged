package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.owner.IOwnable;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

/*
 * Port note: 1.12 placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState) -> placeBlock(context, state)
 */
public class ItemBlockOwned extends ItemBlockBase {

	public ItemBlockOwned(Block block) {
		super(block);
	}

	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState newState) {
		boolean val = super.placeBlock(context, newState);
		Player player = context.getPlayer();
		if (val && player != null) {
			WorldTools.getTile(context.getLevel(), context.getClickedPos(), IOwnable.class).ifPresent(t -> t.setOwner(player));
		}
		return val;
	}
}
