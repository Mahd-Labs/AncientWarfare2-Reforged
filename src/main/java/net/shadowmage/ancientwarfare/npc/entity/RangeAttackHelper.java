package net.shadowmage.ancientwarfare.npc.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/*
 * Port notes: same numbers as 1.12. The arrow gets the bow as "fired from weapon", so 1.21 applies the bow's
 * enchantments itself (power: +0.5 * level + 0.5 base damage, punch knockback, flame 100 s - the same values the 1.12
 * code set by hand); they are therefore not applied a second time here.
 */
public class RangeAttackHelper {
	private RangeAttackHelper() {}

	/*
	 * @author Funwayguy for the speedFactor (range) calculations
	 */
	public static void doRangedAttack(LivingEntity attacker, LivingEntity target, float force, float inaccuracy) {
		double tx = target.getX() + (target.getX() - target.xOld);
		double ty = target.getBoundingBox().minY;
		double tz = target.getZ() + (target.getZ() - target.zOld);
		double targetDist = Math.sqrt(attacker.distanceToSqr(tx, ty, tz));
		float speedFactor = (float) ((0.00013 * (targetDist) * (targetDist)) + (0.02 * targetDist) + 1.25);

		ItemStack weapon = attacker.getMainHandItem();
		AbstractArrow entityarrow = new Arrow(attacker.level(), attacker, new ItemStack(Items.ARROW), weapon.isEmpty() ? null : weapon);
		entityarrow.pickup = AbstractArrow.Pickup.DISALLOWED;
		double d0 = target.getX() - attacker.getX();
		double d1 = target.getBoundingBox().minY + (double) (target.getBbHeight() / 3.0F) - entityarrow.getY();
		double d2 = target.getZ() - attacker.getZ();
		double d3 = Math.sqrt(d0 * d0 + d2 * d2);
		entityarrow.shoot(d0, d1 + d3 * 0.20000000298023224D, d2, speedFactor, inaccuracy);

		entityarrow.setBaseDamage(force * 2.0D + attacker.getRandom().nextGaussian() * 0.25D);

		attacker.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (attacker.getRandom().nextFloat() * 0.4F + 0.8F));
		attacker.level().addFreshEntity(entityarrow);
	}

	public static int getEnchantmentLevel(LivingEntity entity, ResourceKey<Enchantment> enchantment, ItemStack stack) {
		Holder<Enchantment> holder = entity.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
		return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
	}
}
