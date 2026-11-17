package net.shadowmage.ancientwarfare.vehicle.entity.types;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.entity.materials.VehicleMaterial;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringVarsHelper;
import net.shadowmage.ancientwarfare.vehicle.init.AWVehicleSounds;
import net.shadowmage.ancientwarfare.vehicle.registry.AmmoRegistry;

public class VehicleTypeTrebuchet extends VehicleType {
	public VehicleTypeTrebuchet(int typeNum) {
		super(typeNum);
		configName = "trebuchet_base";
		vehicleMaterial = VehicleMaterial.materialWood;
		materialCount = 8;
		maxMissileWeight = 10.f;
		baseHealth = AWVehicleStatics.vehicleStats.vehicleTrebuchetHealth;
		validAmmoTypes.add(AmmoRegistry.ammoStoneShot10);
		validAmmoTypes.add(AmmoRegistry.ammoStoneShot15);
		validAmmoTypes.add(AmmoRegistry.ammoFireShot10);
		validAmmoTypes.add(AmmoRegistry.ammoFireShot15);
		validAmmoTypes.add(AmmoRegistry.ammoPebbleShot10);
		validAmmoTypes.add(AmmoRegistry.ammoPebbleShot15);
		validAmmoTypes.add(AmmoRegistry.ammoClusterShot10);
		validAmmoTypes.add(AmmoRegistry.ammoClusterShot15);
		validAmmoTypes.add(AmmoRegistry.ammoExplosive10);
		validAmmoTypes.add(AmmoRegistry.ammoExplosive15);
		validAmmoTypes.add(AmmoRegistry.ammoHE10);
		validAmmoTypes.add(AmmoRegistry.ammoHE15);
		validAmmoTypes.add(AmmoRegistry.ammoNapalm10);
		validAmmoTypes.add(AmmoRegistry.ammoNapalm15);

		//port: registered as oversize ammo, removed again by applyOversizeAmmoConfig when oversize_ammo_enabled is false
			addOversizeAmmo(AmmoRegistry.ammoStoneShot30);
			addOversizeAmmo(AmmoRegistry.ammoStoneShot45);
			addOversizeAmmo(AmmoRegistry.ammoFireShot30);
			addOversizeAmmo(AmmoRegistry.ammoFireShot45);
			addOversizeAmmo(AmmoRegistry.ammoPebbleShot30);
			addOversizeAmmo(AmmoRegistry.ammoPebbleShot45);
			addOversizeAmmo(AmmoRegistry.ammoClusterShot30);
			addOversizeAmmo(AmmoRegistry.ammoClusterShot45);
			addOversizeAmmo(AmmoRegistry.ammoExplosive30);
			addOversizeAmmo(AmmoRegistry.ammoExplosive45);
			addOversizeAmmo(AmmoRegistry.ammoHE30);
			addOversizeAmmo(AmmoRegistry.ammoHE45);

		ammoBySoldierRank.put(0, AmmoRegistry.ammoStoneShot10);
		ammoBySoldierRank.put(1, AmmoRegistry.ammoStoneShot10);
		ammoBySoldierRank.put(2, AmmoRegistry.ammoStoneShot10);



		displayName = "Trebuchet";
		//TODO fix display tooltip
		//  this.displayTooltip.add("Trebuchet Firing Mechanism");
		//  this.displayTooltip.add("Fixed-Emplacement");
		//  this.displayTooltip.add("No Turret");
		pitchAdjustable = false;
		powerAdjustable = true;
		yawAdjustable = false;
		mountable = true;
		combatEngine = true;
		drivable = true;
		riderSits = false;
		riderMovesWithTurret = false;
		baseForwardSpeed = 0.f;
		baseStrafeSpeed = 0.5f;
		ammoBaySize = 6;
		armorBaySize = 3;
		upgradeBaySize = 3;
		storageBaySize = 0;
		accuracy = 0.85f;

		basePitchMax = 70.f;
		basePitchMin = 70.f;
		turretVerticalOffset = (34.f + 67.5f + 24.0f) * 0.0625f;
		baseMissileVelocityMax = 40.f;
	}

	@Override
	public ResourceLocation getTextureForMaterialLevel(int level) {
		switch (level) {
			case 0:
				return ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/trebuchet_1.png");
			case 1:
				return ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/trebuchet_2.png");
			case 2:
				return ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/trebuchet_3.png");
			case 3:
				return ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/trebuchet_4.png");
			case 4:
				return ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/trebuchet_5.png");
			default:
				return ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "textures/model/vehicle/trebuchet_1.png");
		}
	}

	@Override
	public void playFiringSound(VehicleBase vehicleBase) {
		vehicleBase.playSound(AWVehicleSounds.TREBUCHET_LAUNCH.get(), 6, 1);
	}

	@Override
	public void playReloadSound(VehicleBase vehicleBase) {
		vehicleBase.playSound(AWVehicleSounds.TREBUCHET_RELOAD.get(), 2, 1);
	}

	@Override
	public VehicleFiringVarsHelper getFiringVarsHelper(VehicleBase veh) {
		return new TrebuchetVarHelper(veh);
	}

	public class TrebuchetVarHelper extends VehicleFiringVarsHelper {
		private float armAngle = -27.f;
		private float armSpeed = 0.f;
		private float stringAngle = -64.f;
		private float stringSpeed = 0.f;

		private TrebuchetVarHelper(VehicleBase vehicle) {
			super(vehicle);
		}

		@Override
		public CompoundTag serializeNBT() {
			CompoundTag tag = new CompoundTag();
			tag.putFloat("aA", armAngle);
			tag.putFloat("aS", armSpeed);
			tag.putFloat("sA", stringAngle);
			tag.putFloat("sS", stringSpeed);
			return tag;
		}

		@Override
		public void deserializeNBT(CompoundTag tag) {
			armAngle = tag.getFloat("aA");
			armSpeed = tag.getFloat("aS");
			stringAngle = tag.getFloat("sA");
			stringSpeed = tag.getFloat("sS");
		}

		@Override
		public void onFiringUpdate() {
			float increment = (90.f + 27.f) / 20.f;
			armAngle += increment;
			armSpeed = increment;
			stringAngle += 1.3162316f * increment;
			stringSpeed = 1.3162316f * increment;
			if (armAngle >= 90) {
				armSpeed = 0;
				armAngle = 90.f;
				stringAngle = 90.f;
				stringSpeed = 0.f;
				vehicle.firingHelper.startLaunching();
			}
		}

		@Override
		public void onReloadUpdate() {
			float increment = (90.f + 27.f) / (float) vehicle.currentReloadTicks;
			if (armAngle > -27) {
				armAngle -= increment;
				armSpeed = -increment;
				stringAngle -= 1.3162316f * increment;
				stringSpeed = -1.3162316f * increment;
			} else {
				armAngle = -27;
				armSpeed = 0;
				stringAngle = -64.f;
				stringSpeed = 0.f;
			}
		}

		@Override
		public void onLaunchingUpdate() {
			vehicle.firingHelper.spawnMissilesByWeightCount(0, 0, 0);
			vehicle.firingHelper.setFinishedLaunching();
		}

		@Override
		public void onReloadingFinished() {
			armAngle = -27;
			armSpeed = 0;
			stringAngle = -64.f;
			stringSpeed = 0.f;
		}

		@Override
		public float getVar1() {
			return armAngle;
		}

		@Override
		public float getVar2() {
			return armSpeed;
		}

		@Override
		public float getVar3() {
			return stringAngle;
		}

		@Override
		public float getVar4() {
			return stringSpeed;
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
