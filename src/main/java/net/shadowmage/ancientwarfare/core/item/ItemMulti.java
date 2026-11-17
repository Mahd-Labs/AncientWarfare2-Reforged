package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/*
 * Port notes: 1.12 ItemMulti was one item with meta sub items (addSubItem(num, model, ore)). Sub items are
 * flattened: every sub item is its own ItemMulti instance registered as "<regName>_<sub name>", num is the 1.12 meta
 * value and is still part of the translation key ("item.<regName>.<num>.name"). Ore dictionary names are item tags
 * (data/c/tags/item/...), models are plain models/item/<registry name>.json files.
 */
public class ItemMulti extends ItemBase {
	private final int num;

	public ItemMulti(String modID, String regName, int num) {
		this(modID, regName, num, new Item.Properties());
	}

	public ItemMulti(String modID, String regName, int num, Item.Properties properties) {
		super(modID, regName, properties);
		this.num = num;
	}

	/*
	 * the 1.12 meta value of this sub item
	 */
	public int getSubItemNum() {
		return num;
	}

	@Override
	public String getDescriptionId() {
		return "item." + getRegName() + "." + num + ".name";
	}

	@Override
	public String getDescriptionId(ItemStack par1ItemStack) {
		return getDescriptionId();
	}
}
