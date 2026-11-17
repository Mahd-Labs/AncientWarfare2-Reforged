package net.shadowmage.ancientwarfare.npc.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/*
 * Port notes: 1.12 replaced the main hand damage / speed modifiers of ItemSword with material damage + attackOffset
 * and attackSpeed; the same two values are the attribute modifiers of the item here.
 */
public class ItemExtendedReachWeapon extends SwordItem implements IExtendedReachWeapon {
	private final double attackDamage;
	private double attackSpeed;
	private float reach;
	private final String regName;

	public ItemExtendedReachWeapon(Tier material, String registryName, double attackOffset, double attackSpeed, float reach) {
		this(material, registryName, attackOffset, attackSpeed, reach, new Item.Properties());
	}

	public ItemExtendedReachWeapon(Tier material, String registryName, double attackOffset, double attackSpeed, float reach, Item.Properties properties) {
		super(material, properties.attributes(ItemBaseNPC.weaponAttributes(material.getAttackDamageBonus() + attackOffset, attackSpeed)));
		this.reach = reach;
		this.regName = registryName;
		attackDamage = material.getAttackDamageBonus() + attackOffset;
		this.attackSpeed = attackSpeed;
	}

	@Override
	public String getDescriptionId() {
		return "item." + regName + ".name";
	}

	public double getAttackDamage() {
		return attackDamage;
	}

	public double getAttackSpeed() {
		return attackSpeed;
	}

	@Override
	public float getReach() {
		return reach;
	}
}
