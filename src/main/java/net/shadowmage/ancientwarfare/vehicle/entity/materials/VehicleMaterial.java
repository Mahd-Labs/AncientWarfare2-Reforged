package net.shadowmage.ancientwarfare.vehicle.entity.materials;

import net.shadowmage.ancientwarfare.vehicle.AncientWarfareVehicles;

public class VehicleMaterial implements IVehicleMaterial {

	public static VehicleMaterial materialWood = new VehicleMaterialWood();
	public static VehicleMaterial materialIron = new VehicleMaterialIron();

	public MaterialLevel[] materialLevels;

	public VehicleMaterial(int levelCount) {
		this.materialLevels = new MaterialLevel[levelCount];
	}

	private MaterialLevel getLevel(int num) {
		if (num < 0 || num >= this.materialLevels.length) {
			AncientWarfareVehicles.LOG.error("out of range vehicle material requested. num: " + num + " current levels: " + this.materialLevels.length);
			return new MaterialLevel();
		}
		return this.materialLevels[num];
	}

	@Override
	public int getNumOfLevels() {
		return this.materialLevels.length;
	}

	@Override
	public float getHPFactor(int level) {
		return getLevel(level).hpFactor;
	}

	@Override
	public float getSpeedForwardFactor(int level) {
		return getLevel(level).speedForwardFactor;
	}

	@Override
	public float getSpeedStrafeFactor(int level) {
		return getLevel(level).speedStrafeFactor;
	}

	@Override
	public float getWeightFactor(int level) {
		return getLevel(level).weightFactor;
	}

	@Override
	public float getAccuracyFactor(int level) {
		return getLevel(level).accuracyFactor;
	}

	@Override
	public float getMisfireChance(int level) {
		return getLevel(level).misfireChance;
	}

	@Override
	public String getDisplayName(int level) {
		return getLevel(level).displayName;
	}
}
