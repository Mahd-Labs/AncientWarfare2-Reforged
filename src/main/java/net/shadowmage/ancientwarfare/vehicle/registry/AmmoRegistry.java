package net.shadowmage.ancientwarfare.vehicle.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.item.ItemAmmo;
import net.shadowmage.ancientwarfare.vehicle.missiles.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
 * Port notes: 1.12 created the ammo types inside the item registry event (registerAmmo(IForgeRegistry)). Here the
 * ammo types are created when the class loads (vehicle types reference them from their constructors) and
 * registerAmmo(DeferredRegister.Items) registers one ItemAmmo per type, in the 1.12 order. Ammo damage values that
 * come from the config are (re)applied by applyConfig() when the config loads.
 */
public class AmmoRegistry {
	private static final Map<ResourceLocation, IAmmo> ammoInstances = new HashMap<>();
	private static final List<IAmmo> ammoList = new ArrayList<>();
	private static final Map<ResourceLocation, DeferredItem<ItemAmmo>> ammoItemInstances = new HashMap<>();

	public static final IAmmo ammoBallShot = registerAmmoType(new AmmoBallShot());
	public static final IAmmo ammoBallIronShot = registerAmmoType(new AmmoIronBallShot());
	public static final IAmmo ammoStoneShot10 = registerAmmoType(new AmmoStoneShot(10));
	public static final IAmmo ammoStoneShot15 = registerAmmoType(new AmmoStoneShot(15));
	public static final IAmmo ammoStoneShot30 = registerAmmoType(new AmmoStoneShot(30));
	public static final IAmmo ammoStoneShot45 = registerAmmoType(new AmmoStoneShot(45));
	public static final IAmmo ammoFireShot10 = registerAmmoType(new AmmoFlameShot(10));
	public static final IAmmo ammoFireShot15 = registerAmmoType(new AmmoFlameShot(15));
	public static final IAmmo ammoFireShot30 = registerAmmoType(new AmmoFlameShot(30));
	public static final IAmmo ammoFireShot45 = registerAmmoType(new AmmoFlameShot(45));
	public static final IAmmo ammoExplosive10 = registerAmmoType(new AmmoExplosiveShot(10, false));
	public static final IAmmo ammoExplosive15 = registerAmmoType(new AmmoExplosiveShot(15, false));
	public static final IAmmo ammoExplosive30 = registerAmmoType(new AmmoExplosiveShot(30, false));
	public static final IAmmo ammoExplosive45 = registerAmmoType(new AmmoExplosiveShot(45, false));
	public static final IAmmo ammoHE10 = registerAmmoType(new AmmoExplosiveShot(10, true));
	public static final IAmmo ammoHE15 = registerAmmoType(new AmmoExplosiveShot(15, true));
	public static final IAmmo ammoHE30 = registerAmmoType(new AmmoExplosiveShot(30, true));
	public static final IAmmo ammoHE45 = registerAmmoType(new AmmoExplosiveShot(45, true));
	public static final IAmmo ammoNapalm10 = registerAmmoType(new AmmoNapalmShot(10));
	public static final IAmmo ammoNapalm15 = registerAmmoType(new AmmoNapalmShot(15));
	public static final IAmmo ammoNapalm30 = registerAmmoType(new AmmoNapalmShot(30));
	public static final IAmmo ammoNapalm45 = registerAmmoType(new AmmoNapalmShot(45));
	public static final IAmmo ammoClusterShot10 = registerAmmoType(new AmmoClusterShot(10));
	public static final IAmmo ammoClusterShot15 = registerAmmoType(new AmmoClusterShot(15));
	public static final IAmmo ammoClusterShot30 = registerAmmoType(new AmmoClusterShot(30));
	public static final IAmmo ammoClusterShot45 = registerAmmoType(new AmmoClusterShot(45));
	public static final IAmmo ammoPebbleShot10 = registerAmmoType(new AmmoPebbleShot(10));
	public static final IAmmo ammoPebbleShot15 = registerAmmoType(new AmmoPebbleShot(15));
	public static final IAmmo ammoPebbleShot30 = registerAmmoType(new AmmoPebbleShot(30));
	public static final IAmmo ammoPebbleShot45 = registerAmmoType(new AmmoPebbleShot(45));
	public static final IAmmo ammoIronShot5 = registerAmmoType(new AmmoIronShot(5, AWVehicleStatics.vehicleStats.ammoCannonBall5kgDamage));
	public static final IAmmo ammoIronShot10 = registerAmmoType(new AmmoIronShot(10, AWVehicleStatics.vehicleStats.ammoCannonBall10kgDamage));
	public static final IAmmo ammoIronShot15 = registerAmmoType(new AmmoIronShot(15, AWVehicleStatics.vehicleStats.ammoCannonBall15kgDamage));
	public static final IAmmo ammoIronShot25 = registerAmmoType(new AmmoIronShot(25, AWVehicleStatics.vehicleStats.ammoCannonBall25kgDamage));
	public static final IAmmo ammoCanisterShot5 = registerAmmoType(new AmmoCanisterShot(5));
	public static final IAmmo ammoCanisterShot10 = registerAmmoType(new AmmoCanisterShot(10));
	public static final IAmmo ammoCanisterShot15 = registerAmmoType(new AmmoCanisterShot(15));
	public static final IAmmo ammoCanisterShot25 = registerAmmoType(new AmmoCanisterShot(25));
	public static final IAmmo ammoGrapeShot5 = registerAmmoType(new AmmoGrapeShot(5));
	public static final IAmmo ammoGrapeShot10 = registerAmmoType(new AmmoGrapeShot(10));
	public static final IAmmo ammoGrapeShot15 = registerAmmoType(new AmmoGrapeShot(15));
	public static final IAmmo ammoGrapeShot25 = registerAmmoType(new AmmoGrapeShot(25));
	public static final IAmmo ammoArrow = registerAmmoType(new AmmoArrow());
	public static final IAmmo ammoBallistaBolt = registerAmmoType(new AmmoBallistaBolt());
	public static final IAmmo ammoBallistaBoltFlame = registerAmmoType(new AmmoBallistaBoltFlame());
	public static final IAmmo ammoBallistaBoltExplosive = registerAmmoType(new AmmoBallistaBoltExplosive());
	public static final IAmmo ammoBallistaBoltIron = registerAmmoType(new AmmoBallistaBoltIron());
	public static final IAmmo ammoRocket = registerAmmoType(new AmmoHwachaRocket());
	public static final IAmmo ammoHwachaRocketFlame = registerAmmoType(new AmmoHwachaRocketFlame());
	public static final IAmmo ammoHwachaRocketExplosive = registerAmmoType(new AmmoHwachaRocketExplosive());
	public static final IAmmo ammoHwachaRocketAirburst = registerAmmoType(new AmmoHwachaRocketAirburst());

	private AmmoRegistry() {
	}

	private static IAmmo registerAmmoType(IAmmo ammo) {
		ammoInstances.put(ammo.getRegistryName(), ammo);
		ammoList.add(ammo);
		return ammo;
	}

	public static void registerAmmo(DeferredRegister.Items registry) {
		for (IAmmo ammo : ammoList) {
			ResourceLocation name = ammo.getRegistryName();
			ammoItemInstances.put(name, registry.register(name.getPath(), () -> new ItemAmmo(name, ammo)));
		}
	}

	/* all ammo types in the 1.12 registration order */
	public static List<IAmmo> getAmmoTypes() {
		return Collections.unmodifiableList(ammoList);
	}

	public static void applyConfig() {
		AWVehicleStatics.VehicleStats stats = AWVehicleStatics.vehicleStats;
		setDamage(ammoIronShot5, stats.ammoCannonBall5kgDamage);
		setDamage(ammoIronShot10, stats.ammoCannonBall10kgDamage);
		setDamage(ammoIronShot15, stats.ammoCannonBall15kgDamage);
		setDamage(ammoIronShot25, stats.ammoCannonBall25kgDamage);
		setDamage(ammoCanisterShot5, stats.ammoCanisterDamage);
		setDamage(ammoCanisterShot10, stats.ammoCanisterDamage);
		setDamage(ammoCanisterShot15, stats.ammoCanisterDamage);
		setDamage(ammoCanisterShot25, stats.ammoCanisterDamage);
		setDamage(ammoBallistaBolt, stats.ammoBallistaBoltDamage);
		setDamage(ammoBallistaBoltFlame, stats.ammoBallistaBoltFlameDamage);
		setDamage(ammoBallistaBoltExplosive, stats.ammoBallistaBoltExplosiveDamage);
		setDamage(ammoBallistaBoltIron, stats.ammoBallistaBoltIronDamage);
		setDamage(ammoRocket, stats.ammoHwachaRocketDamage);
		setDamage(ammoHwachaRocketFlame, stats.ammoHwachaRocketFlameDamage);
		setDamage(ammoHwachaRocketExplosive, stats.ammoHwachaRocketExplosiveDamage);
	}

	private static void setDamage(IAmmo ammo, int damage) {
		ammo.setEntityDamage(damage);
		ammo.setVehicleDamage(damage);
	}

	public static Optional<IAmmo> getAmmoForStack(ItemStack stack) {
		return Optional.ofNullable(ammoInstances.get(BuiltInRegistries.ITEM.getKey(stack.getItem())));
	}

	public static IAmmo getAmmo(ResourceLocation registryName) {
		return ammoInstances.get(registryName);
	}

	public static ItemAmmo getItemForAmmo(IAmmo ammo) {
		return getItem(ammo.getRegistryName());
	}

	public static ItemAmmo getItem(ResourceLocation ammoRegistryName) {
		DeferredItem<ItemAmmo> item = ammoItemInstances.get(ammoRegistryName);
		return item == null ? null : item.get();
	}
}
