package net.shadowmage.ancientwarfare.vehicle.missiles;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

public class AmmoStoneShot extends Ammo {

	public AmmoStoneShot(int weight) {
		super("ammo_stone_shot_" + weight);
		isPersistent = false;
		isArrow = false;
		isRocket = false;
		ammoWeight = weight;
		configName = "stone_shot_" + weight;
		entityDamage = weight;
		vehicleDamage = weight;
		float scaleFactor = weight + 45.f;
		renderScale = (weight / scaleFactor) * 2;
		modelTexture = ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/ammo/ammo_stone_shot.png");
	}

	@Override
	public void onImpactWorld(Level world, float x, float y, float z, MissileBase missile, BlockHitResult hit) {
		BlockPos origin = BlockPos.containing(x, y, z);

		float maxHardness = 5 + (ammoWeight * 0.2f + ammoWeight * 0.8f * world.random.nextFloat()) * 0.6f;

		breakAroundOnLevel(world, origin, origin, maxHardness);
		breakAroundOnLevel(world, origin, origin.above(), maxHardness);
		breakAroundOnLevel(world, origin, origin.below(), maxHardness);
	}

	@Override
	public void onImpactEntity(Level world, Entity ent, float x, float y, float z, MissileBase missile) {
		if (!world.isClientSide) {
			ent.hurt(DamageType.causeEntityMissileDamage(world, missile.shooterLiving, false, false), getEntityDamage());
		}
	}

}
