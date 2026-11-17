package net.shadowmage.ancientwarfare.vehicle.entity.materials;

public class VehicleMaterialIron extends VehicleMaterial {

	public VehicleMaterialIron() {
		super(5);
		MaterialLevel level0 = new MaterialLevel();
		MaterialLevel level1 = new MaterialLevel();
		MaterialLevel level2 = new MaterialLevel();
		MaterialLevel level3 = new MaterialLevel();
		MaterialLevel level4 = new MaterialLevel();
		this.materialLevels[0] = level0;
		this.materialLevels[1] = level1;
		this.materialLevels[2] = level2;
		this.materialLevels[3] = level3;
		this.materialLevels[4] = level4;
		level0.accuracyFactor = 0.85f;
		level1.accuracyFactor = 0.9f;
		level2.accuracyFactor = 0.93f;
		level3.accuracyFactor = 0.96f;
		level4.accuracyFactor = 1.f;
		level0.hpFactor = 0.75f;
		level1.hpFactor = 1.f;
		level2.hpFactor = 1.2f;
		level3.hpFactor = 1.4f;
		level4.hpFactor = 2.f;
		level0.misfireChance = 0.1f;
		level1.misfireChance = 0.06f;
		level2.misfireChance = 0.03f;
		level3.misfireChance = 0.01f;
		level4.misfireChance = 0.002f;
		level0.speedForwardFactor = 0.85f;
		level1.speedForwardFactor = 1.f;
		level2.speedForwardFactor = 1.15f;
		level3.speedForwardFactor = 1.05f;
		level4.speedForwardFactor = 0.95f;
		level0.speedStrafeFactor = 0.85f;
		level1.speedStrafeFactor = 1.f;
		level2.speedStrafeFactor = 1.15f;
		level3.speedStrafeFactor = 1.05f;
		level4.speedStrafeFactor = 0.95f;
		level0.weightFactor = 1.25f;
		level1.weightFactor = 1.f;
		level2.weightFactor = 1.1f;
		level3.weightFactor = 1.3f;
		level4.weightFactor = 1.7f;
		level0.displayName = "Rough Iron";
		level1.displayName = "Fine Iron";
		level2.displayName = "Tempered Iron";
		level3.displayName = "Minor Alloy";
		level4.displayName = "Major Alloy";
	}
}
