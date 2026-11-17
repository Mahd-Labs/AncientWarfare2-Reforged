package net.shadowmage.ancientwarfare.structure.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import net.shadowmage.ancientwarfare.structure.util.WoodVariantHelper;

public class WoodItemBlock extends ItemBlockBaseStructure {
	public WoodItemBlock(Block block) {
		super(block);
	}

	public String getUnlocalizedName(ItemStack stack) {
		return super.getUnlocalizedName(stack) + "_" + WoodVariantHelper.getVariant(stack).getSerializedName();
	}
}
