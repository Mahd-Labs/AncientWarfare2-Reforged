package net.shadowmage.ancientwarfare.structure.item;

import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import org.apache.commons.lang3.StringUtils;

public class ItemBlockFlag extends ItemBlockBaseStructure {
	public ItemBlockFlag(Block block) {
		super(block);
	}

	public String getItemStackDisplayName(ItemStack stack) {
		String name = "";
		if (ItemTools.hasTag(stack)) {
			//noinspection ConstantConditions
			name = StringUtils.capitalize(ItemTools.getTag(stack).getString("name"));
		}

		return net.shadowmage.ancientwarfare.structure.util.TextHelper.translate(getLegacyName(stack) + ".name", name).trim();
	}
}
