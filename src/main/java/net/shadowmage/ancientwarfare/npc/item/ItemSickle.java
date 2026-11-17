package net.shadowmage.ancientwarfare.npc.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;

public class ItemSickle extends ItemBaseNPC {
	private final Tier material;
	protected double attackSpeed;

	public ItemSickle(Tier material, double attackSpeed) {
		super("sickle", new Item.Properties().stacksTo(1).durability(material.getUses()).attributes(weaponAttributes(material.getAttackDamageBonus(), attackSpeed)));
		this.material = material;
		this.attackSpeed = attackSpeed;
	}

	@Override
	public int getEnchantmentValue() {
		return this.material.getEnchantmentValue();
	}
}
