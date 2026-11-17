package net.shadowmage.ancientwarfare.vehicle.entity.types;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.entity.materials.VehicleMaterial;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringVarsHelper;
import net.shadowmage.ancientwarfare.vehicle.registry.AmmoRegistry;

public class VehicleTypeCannon extends VehicleType {
	public VehicleTypeCannon(int typeNum) {
		super(typeNum);
		configName = "cannon_base";
		baseHealth = AWVehicleStatics.vehicleStats.vehicleCannonHealth;
		vehicleMaterial = VehicleMaterial.materialIron;
		materialCount = 5;
		maxMissileWeight = 10.f;

		validAmmoTypes.add(AmmoRegistry.ammoIronShot5);
		validAmmoTypes.add(AmmoRegistry.ammoIronShot10);
		validAmmoTypes.add(AmmoRegistry.ammoGrapeShot5);
		validAmmoTypes.add(AmmoRegistry.ammoGrapeShot10);
		validAmmoTypes.add(AmmoRegistry.ammoCanisterShot5);
		validAmmoTypes.add(AmmoRegistry.ammoCanisterShot10);

		//port: registered as oversize ammo, removed again by applyOversizeAmmoConfig when oversize_ammo_enabled is false
			addOversizeAmmo(AmmoRegistry.ammoIronShot15);
			addOversizeAmmo(AmmoRegistry.ammoIronShot25);
			addOversizeAmmo(AmmoRegistry.ammoGrapeShot15);
			addOversizeAmmo(AmmoRegistry.ammoGrapeShot25);
			addOversizeAmmo(AmmoRegistry.ammoCanisterShot15);
			addOversizeAmmo(AmmoRegistry.ammoCanisterShot25);

		ammoBySoldierRank.put(0, AmmoRegistry.ammoIronShot5);
		ammoBySoldierRank.put(1, AmmoRegistry.ammoIronShot5);
		ammoBySoldierRank.put(2, AmmoRegistry.ammoIronShot5);



		turretVerticalOffset = 11.5f * 0.0625f;
		storageBaySize = 0;
		accuracy = 0.98f;
		drivable = true;
		baseForwardSpeed = 0.f;
		baseStrafeSpeed = .75f;
		basePitchMax = 15;
		basePitchMin = -15;
		mountable = true;
		combatEngine = true;
		pitchAdjustable = true;
		powerAdjustable = false;
		yawAdjustable = false;

		baseMissileVelocityMax = 42.f;
		width = 2;
		height = 2;

		armorBaySize = 3;
		upgradeBaySize = 3;
		ammoBaySize = 6;
	}

	@Override
	public ResourceLocation getTextureForMaterialLevel(int level) {
		switch (level) {
			case 0:
				return ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/cannon_1.png");
			case 1:
				return ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/cannon_2.png");
			case 2:
				return ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/cannon_3.png");
			case 3:
				return ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/cannon_4.png");
			case 4:
				return ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/cannon_5.png");
			default:
				return ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/cannon_1.png");
		}
	}

	@Override
	public VehicleFiringVarsHelper getFiringVarsHelper(VehicleBase veh) {
		return new CannonVarHelper(veh);
	}

	public class CannonVarHelper extends VehicleFiringVarsHelper {

		private int firingTicks = 0;

		private CannonVarHelper(VehicleBase vehicle) {
			super(vehicle);
		}

		@Override
		public CompoundTag serializeNBT() {
			CompoundTag tag = new CompoundTag();
			tag.putInt("fT", firingTicks);
			return tag;
		}

		@Override
		public void deserializeNBT(CompoundTag tag) {
			//noop
		}

		@Override
		public void onFiringUpdate() {
			if (firingTicks == 0 && !vehicle.level().isClientSide) {
				vehicle.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.50F, .25F);
				vehicle.playSound(SoundEvents.TNT_PRIMED, 1.0F, 0.5F);
			}
			firingTicks++;
			if (vehicle.level().isClientSide) {
				//TODO offset
				vehicle.level().addParticle(ParticleTypes.SMOKE, vehicle.getX(), vehicle.getY() + 1.2d, vehicle.getZ(), 0.0D, 0.05D, 0.0D);
			}
			if (firingTicks > 10) {
				if (!vehicle.level().isClientSide) {
					vehicle.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.f, 1.f);
				}
				vehicle.firingHelper.startLaunching();
				firingTicks = 0;
			}
		}

		@Override
		public void onReloadUpdate() {
			//noop
		}

		@Override
		public void onLaunchingUpdate() {
			vehicle.firingHelper.spawnMissile(0, 0, 0);
			//TODO play explosion sound
			//TODO spawn particles for explosion in direction of missile flight @ end of barrel (translate/offset)
			vehicle.firingHelper.setFinishedLaunching();
		}

		@Override
		public void onReloadingFinished() {
			firingTicks = 0;
		}

		@Override
		public float getVar1() {
			return 0;
		}

		@Override
		public float getVar2() {
			return 0;
		}

		@Override
		public float getVar3() {
			return 0;
		}

		@Override
		public float getVar4() {
			return 0;
		}

		@Override
		public float getVar5() {
			return 0;
		}

		@Override
		public float getVar6() {
			return 0;
		}

		@Override
		public float getVar7() {
			return 0;
		}

		@Override
		public float getVar8() {
			return 0;
		}
	}
}
