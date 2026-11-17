package net.shadowmage.ancientwarfare.vehicle.missiles;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

public class AmmoArrow extends Ammo {

	public AmmoArrow() {
		super("ammo_arrow");
		ammoWeight = 1.f;
		renderScale = 0.2f;
		vehicleDamage = 8;
		entityDamage = 8;
		isArrow = true;
		isRocket = false;
		isPersistent = true;
		isCraftable = false;
		configName = "arrow";
		modelTexture = ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/ammo/arrow_wood.png");
	}

	@Override
	public void onImpactWorld(Level world, float x, float y, float z, MissileBase missile, BlockHitResult hit) {
		//noop
	}

	@Override
	public void onImpactEntity(Level world, Entity ent, float x, float y, float z, MissileBase missile) {
		if (!world.isClientSide) {
			ent.hurt(DamageType.causeEntityMissileDamage(world, missile.shooterLiving, false, false), getEntityDamage());
		}
	}

}
