package net.shadowmage.ancientwarfare.vehicle.entity.materials;

public interface IVehicleMaterial {

	int getNumOfLevels();

	float getHPFactor(int level);

	float getSpeedForwardFactor(int level);

	float getSpeedStrafeFactor(int level);

	float getWeightFactor(int level);

	float getAccuracyFactor(int level);

	float getMisfireChance(int level);

	String getDisplayName(int level);
}
