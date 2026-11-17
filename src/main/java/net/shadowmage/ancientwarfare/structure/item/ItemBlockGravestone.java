package net.shadowmage.ancientwarfare.structure.item;

import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import net.shadowmage.ancientwarfare.core.util.NBTBuilder;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;

public class ItemBlockGravestone extends ItemBlockBaseStructure {
	public ItemBlockGravestone(Block block) {
		super(block);
	}

	public static int getVariant(ItemStack stack) {
		//noinspection ConstantConditions
		return ItemTools.hasTag(stack) ? ItemTools.getTag(stack).getInt("variant") : 1;
	}

	public static ItemStack getVariantStack(int variant) {
		ItemStack stack = new ItemStack(AWStructureBlocks.GRAVESTONE.get());
		ItemTools.setTag(stack, new NBTBuilder().setInteger("variant", variant).build());
		return stack;
	}

	public String getItemStackDisplayName(ItemStack stack) {
		if (ItemTools.hasTag(stack)) {
			String name = "tile.gravestone." + getVariant(stack) + ".name";
			return net.shadowmage.ancientwarfare.structure.util.TextHelper.translate(name);
		}

		return super.getItemStackDisplayName(stack);
	}
}
