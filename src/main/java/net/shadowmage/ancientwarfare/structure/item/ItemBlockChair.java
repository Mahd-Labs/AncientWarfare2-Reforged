package net.shadowmage.ancientwarfare.structure.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.structure.util.MultiBlockHelper;

public class ItemBlockChair extends WoodItemBlock {
	public ItemBlockChair(Block block) {
		super(block);
	}

	@Override
	protected MultiBlockHelper.IPlaceChecker getPlaceChecker() {
		return (w, p, side, placer) -> w.isEmptyBlock(p.above());
	}
}
