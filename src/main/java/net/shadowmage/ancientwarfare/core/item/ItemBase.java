package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.world.item.Item;

/*
 * Port notes: registry names are given by the DeferredRegister; regName is kept for the translation key
 * ("item.<regName>.name", the key 1.12 setUnlocalizedName produced). Stack size, durability etc. go into the
 * Item.Properties passed to the three argument constructor.
 */
public abstract class ItemBase extends Item {
	private final String regName;

	public ItemBase(String modID, String regName) {
		this(modID, regName, new Item.Properties());
	}

	public ItemBase(String modID, String regName, Item.Properties properties) {
		super(properties);
		this.regName = regName;
	}

	public String getRegName() {
		return regName;
	}

	@Override
	public String getDescriptionId() {
		return "item." + regName + ".name";
	}
}
