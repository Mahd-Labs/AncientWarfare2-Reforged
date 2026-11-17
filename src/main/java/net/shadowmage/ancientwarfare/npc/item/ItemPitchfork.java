package net.shadowmage.ancientwarfare.npc.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;

public class ItemPitchfork extends ItemBaseNPC {
	private final Tier material;
	protected float attackSpeed;

	public ItemPitchfork(Tier material, float attackSpeed) {
		super("pitchfork", new Item.Properties().stacksTo(1).durability(material.getUses()).attributes(weaponAttributes(material.getAttackDamageBonus(), attackSpeed)));
		this.material = material;
		this.attackSpeed = attackSpeed;
	}

	@Override
	public int getEnchantmentValue() {
		return this.material.getEnchantmentValue();
	}
}
