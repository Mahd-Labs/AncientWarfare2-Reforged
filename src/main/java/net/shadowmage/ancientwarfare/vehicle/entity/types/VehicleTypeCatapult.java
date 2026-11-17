package net.shadowmage.ancientwarfare.vehicle.entity.types;

import net.shadowmage.ancientwarfare.vehicle.VehicleVarHelpers.CatapultVarHelper;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.entity.materials.VehicleMaterial;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringVarsHelper;
import net.shadowmage.ancientwarfare.vehicle.init.AWVehicleSounds;
import net.shadowmage.ancientwarfare.vehicle.registry.AmmoRegistry;

public class VehicleTypeCatapult extends VehicleType {

	/**
	 * @param typeNum
	 */
	public VehicleTypeCatapult(int typeNum) {
		super(typeNum);
		configName = "catapult_base";
		vehicleMaterial = VehicleMaterial.materialWood;
		materialCount = 5;
		maxMissileWeight = 10.f;
		baseHealth = AWVehicleStatics.vehicleStats.vehicleCatapultHealth;

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


		mountable = true;
		combatEngine = true;
		powerAdjustable = true;
		pitchAdjustable = false;
		accuracy = 0.95f;
		baseStrafeSpeed = 2.f;
		baseForwardSpeed = 6.f * 0.05f;
		basePitchMax = 20;
		basePitchMin = 20;


		width = 2;
		height = 2;
		baseMissileVelocityMax = 32.f;
		missileVerticalOffset = 0;
		missileForwardsOffset = -2.0f;
		riderForwardsOffset = 1.2f;
		riderVerticalOffset = 0.55f;
		displayName = "Catapult";
		storageBaySize = 0;
		armorBaySize = 3;
		upgradeBaySize = 3;
		yawAdjustable = false;
		drivable = false;
		riderSits = true;
		riderMovesWithTurret = false;
	}

	@Override
	public void playFiringSound(VehicleBase vehicleBase) {
		vehicleBase.playSound(AWVehicleSounds.CATAPULT_LAUNCH.get(), 6, 1);
	}

	@Override
	public void playReloadSound(VehicleBase vehicleBase) {
		vehicleBase.playSound(AWVehicleSounds.CATAPULT_RELOAD.get(), 1, 1);
	}

	@Override
	public VehicleFiringVarsHelper getFiringVarsHelper(VehicleBase veh) {
		return new CatapultVarHelper(veh);
	}
}
