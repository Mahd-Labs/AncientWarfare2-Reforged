package net.shadowmage.ancientwarfare.vehicle.missiles;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.vehicle.AncientWarfareVehicles;

import javax.annotation.Nullable;

/*
 * Port notes: 1.12 subclassed DamageSource ("AWMissile", projectile, optionally fire / explosion, true source = the
 * rider of the weapon). 1.21 damage types are data: data/ancientwarfare/damage_type/aw_missile*.json (message id
 * "AWMissile", so the 1.12 death message key death.attack.AWMissile applies) with the tags is_projectile and, for the
 * variants, is_fire / is_explosion. The 1.12 constants fireMissile / explosiveMissile / genericMissile /
 * piercingMissile / batteringDamage were only compared against, never dealt (battering: battering ram, dropped).
 */
public final class DamageType {
	private DamageType() {}

	public static final ResourceKey<net.minecraft.world.damagesource.DamageType> MISSILE = key("aw_missile");
	public static final ResourceKey<net.minecraft.world.damagesource.DamageType> MISSILE_FIRE = key("aw_missile_fire");
	public static final ResourceKey<net.minecraft.world.damagesource.DamageType> MISSILE_EXPLOSIVE = key("aw_missile_explosive");
	public static final ResourceKey<net.minecraft.world.damagesource.DamageType> MISSILE_FIRE_EXPLOSIVE = key("aw_missile_fire_explosive");

	private static ResourceKey<net.minecraft.world.damagesource.DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(AncientWarfareVehicles.MOD_ID, name));
	}

	public static DamageSource causeEntityMissileDamage(Level world, @Nullable Entity attacker, boolean fire, boolean expl) {
		ResourceKey<net.minecraft.world.damagesource.DamageType> key = fire ? (expl ? MISSILE_FIRE_EXPLOSIVE : MISSILE_FIRE) : (expl ? MISSILE_EXPLOSIVE : MISSILE);
		return new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), attacker);
	}
}
