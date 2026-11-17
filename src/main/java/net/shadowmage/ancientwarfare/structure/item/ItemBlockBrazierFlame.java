package net.shadowmage.ancientwarfare.structure.item;

import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import net.shadowmage.ancientwarfare.structure.block.BlockBrazierFlame;

public class ItemBlockBrazierFlame extends ItemBlockBaseStructure {
	public ItemBlockBrazierFlame(Block block) {
		super(block);
	}

	public String getUnlocalizedName(ItemStack stack) {
		if (!ItemTools.hasTag(stack)) {
			return super.getUnlocalizedName(stack);
		}

		//noinspection ConstantConditions
		return String.format("%s.%s", super.getUnlocalizedName(stack),
				ItemTools.getTag(stack).getBoolean(BlockBrazierFlame.LIT_TAG) ? "lit" : "unlit");
	}
}
