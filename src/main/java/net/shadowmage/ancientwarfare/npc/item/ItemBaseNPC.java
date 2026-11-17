package net.shadowmage.ancientwarfare.npc.item;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.item.ItemBase;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;

/*
 * Port notes: 1.12 registerClient (ModelLoaderHelper.registerItem(this, "npc")) is gone, the item model lives at
 * models/item/<registry name>.json. The creative tab content is listed by AWNPCTab.
 * weaponAttributes builds what 1.12 getAttributeModifiers returned for the main hand ("Weapon modifier" damage and
 * speed with operation 0).
 */
public abstract class ItemBaseNPC extends ItemBase {
	public ItemBaseNPC(String regName) {
		super(AncientWarfareNPC.MOD_ID, regName);
	}

	public ItemBaseNPC(String regName, Item.Properties properties) {
		super(AncientWarfareNPC.MOD_ID, regName, properties);
	}

	public static ItemAttributeModifiers weaponAttributes(double attackDamage, double attackSpeed) {
		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, attackDamage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.build();
	}

	/*
	 * a tier that differs from the given one only in durability (1.12 setMaxDamage after the ItemSword constructor)
	 */
	public static Tier tierWithUses(Tier tier, int uses) {
		return new Tier() {
			@Override
			public int getUses() {
				return uses;
			}

			@Override
			public float getSpeed() {
				return tier.getSpeed();
			}

			@Override
			public float getAttackDamageBonus() {
				return tier.getAttackDamageBonus();
			}

			@Override
			public TagKey<Block> getIncorrectBlocksForDrops() {
				return tier.getIncorrectBlocksForDrops();
			}

			@Override
			public int getEnchantmentValue() {
				return tier.getEnchantmentValue();
			}

			@Override
			public Ingredient getRepairIngredient() {
				return tier.getRepairIngredient();
			}
		};
	}
}
