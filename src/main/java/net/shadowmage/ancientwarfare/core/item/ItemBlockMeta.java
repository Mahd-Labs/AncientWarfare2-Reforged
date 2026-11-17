package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.block.BlockBase;

/*
 * Port notes: 1.12 item blocks with meta sub types. Sub types are flattened into separate blocks (one item block
 * each); meta is the 1.12 meta value of the variant and is only used for the translation key
 * ("tile.<regName>.<meta>.name").
 */
public class ItemBlockMeta extends ItemBlockBase {
	private final int meta;

	public ItemBlockMeta(Block block) {
		this(block, 0);
	}

	public ItemBlockMeta(Block block, int meta) {
		super(block);
		this.meta = meta;
	}

	public int getMeta() {
		return meta;
	}

	@Override
	public String getDescriptionId() {
		return getMetaDescriptionId(getBlock(), meta);
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		return getDescriptionId();
	}

	static String getMetaDescriptionId(Block block, int meta) {
		if (block instanceof BlockBase) {
			return "tile." + ((BlockBase) block).getRegName() + "." + meta + ".name";
		}
		return block.getDescriptionId() + "." + meta;
	}
}
