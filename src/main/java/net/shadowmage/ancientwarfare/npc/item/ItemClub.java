package net.shadowmage.ancientwarfare.npc.item;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class ItemClub extends ItemExtendedReachWeapon {
	public ItemClub(Tier material, String registryName, double attackOffset, double attackSpeed, float reach) {
		super(material, registryName, attackOffset, attackSpeed, reach);
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		int knockback = 1;
		double mx = target.getDeltaMovement().x;
		double mz = target.getDeltaMovement().z;
		float f1 = (float) Math.sqrt(mx * mx + mz * mz);
		if (f1 > 0) { //1.12 divided by zero here when the target stood still
			target.push(mx * (double) knockback * 0.6000000238418579D / (double) f1, 0.1D, mz * (double) knockback * 0.6000000238418579D / (double) f1);
		}
		return true;
	}

	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
		return EnchantmentHelper.getEnchantmentsForCrafting(book).keySet().stream().noneMatch(e -> e.is(Enchantments.KNOCKBACK));
	}

	/* 1.12 canApplyAtEnchantingTable */
	@Override
	public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
		return !enchantment.is(Enchantments.KNOCKBACK) && super.supportsEnchantment(stack, enchantment);
	}

	@Override
	public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
		return true;
	}
}
