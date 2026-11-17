package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.world.item.Item;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

/*
 * Port notes: 1.12 registerClient (ModelLoaderHelper.registerItem(this, "core")) is gone, the item model lives at
 * models/item/<registry name>.json. The creative tab content is listed by AWCoreTab.
 */
public class ItemBaseCore extends ItemBase {

	public ItemBaseCore(String regName) {
		super(AncientWarfareCore.MOD_ID, regName);
	}

	public ItemBaseCore(String regName, Item.Properties properties) {
		super(AncientWarfareCore.MOD_ID, regName, properties);
	}
}
