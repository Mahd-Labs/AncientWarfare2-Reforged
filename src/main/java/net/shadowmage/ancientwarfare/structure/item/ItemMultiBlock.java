package net.shadowmage.ancientwarfare.structure.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;

public class ItemMultiBlock extends ItemBlockBaseStructure {
	private final Vec3i minOffset;
	private final Vec3i maxOffset;

	public ItemMultiBlock(Block block, Vec3i minOffset, Vec3i maxOffset) {
		super(block);
		this.minOffset = minOffset;
		this.maxOffset = maxOffset;
	}

	@Override
	public boolean canPlaceBlockOnSide(Level world, BlockPos pos, Direction side, Player player, ItemStack stack) {
		if (!super.canPlaceBlockOnSide(world, pos, side, player, stack)) {
			return false;
		}
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(minOffset), pos.offset(maxOffset).above())) {
			if (p.equals(pos)) {
				continue;
			}
			BlockState state = world.getBlockState(p);
			if (!state.canBeReplaced()) {
				return false;
			}
		}

		return true;
	}
}
