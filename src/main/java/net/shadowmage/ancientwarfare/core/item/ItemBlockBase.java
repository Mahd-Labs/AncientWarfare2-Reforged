package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/*
 * Port note: the translation key is the block's ("tile.<regName>.name"), like the 1.12 ItemBlock.
 */
public class ItemBlockBase extends BlockItem {
	public ItemBlockBase(Block block) {
		this(block, new Item.Properties());
	}

	public ItemBlockBase(Block block, Item.Properties properties) {
		super(block, properties);
	}
}
