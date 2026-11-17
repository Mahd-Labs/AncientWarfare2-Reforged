package net.shadowmage.ancientwarfare.vehicle.missiles;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.init.AWVehicleSounds;

public class AmmoBallistaBoltIron extends Ammo {

	public AmmoBallistaBoltIron() {
		super("ammo_ballista_bolt_iron");
		ammoWeight = 2.f;
		renderScale = 0.3f;
		vehicleDamage = AWVehicleStatics.vehicleStats.ammoBallistaBoltIronDamage;
		entityDamage = AWVehicleStatics.vehicleStats.ammoBallistaBoltIronDamage;
		isArrow = true;
		isRocket = false;
		isPersistent = true;
		configName = "ballist_bolt_iron";
		modelTexture = ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/ammo/arrow_iron.png");
	}

	@Override
	public void onImpactWorld(Level world, float x, float y, float z, MissileBase missile, BlockHitResult hit) {
		//noop
	}

	@Override
	public void onImpactEntity(Level world, Entity ent, float x, float y, float z, MissileBase missile) {
		if (!world.isClientSide) {
			// using Level.playSound instead of Entity.playSound, because Entity.playSound plays the sound to everyone nearby except(!) this player
			world.playSound(null, x, y, z, AWVehicleSounds.BALLISTA_BOLT_HIT_ENTITY.get(), SoundSource.NEUTRAL, 2, 1);
			ent.hurt(DamageType.causeEntityMissileDamage(world, missile.shooterLiving, false, false), getEntityDamage());
		}
	}

}
