package net.shadowmage.ancientwarfare.structure.item;

import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import net.shadowmage.ancientwarfare.structure.block.BlockTotemPart;

public class ItemBlockTotemPart extends ItemBlockBaseStructure {
	public ItemBlockTotemPart(Block block) {
		super(block);
	}

	public static BlockTotemPart.Variant getVariant(ItemStack stack) {
		if (ItemTools.hasTag(stack)) {
			//noinspection ConstantConditions
			return BlockTotemPart.Variant.fromId(ItemTools.getTag(stack).getByte("variant"));
		}
		return BlockTotemPart.Variant.BASE;
	}

	@Override
	public boolean canPlaceBlockOnSide(Level world, BlockPos pos, Direction side, Player player, ItemStack stack) {
		return getVariant(stack).canPlace(world, pos.relative(side), player);
	}

	public String getUnlocalizedName(ItemStack stack) {
		if (!ItemTools.hasTag(stack)) {
			return super.getUnlocalizedName(stack);
		}

		//noinspection ConstantConditions
		return String.format("%s.%s", super.getUnlocalizedName(stack),
				BlockTotemPart.Variant.fromId(ItemTools.getTag(stack).getByte("variant")).name().toLowerCase());
	}
}
