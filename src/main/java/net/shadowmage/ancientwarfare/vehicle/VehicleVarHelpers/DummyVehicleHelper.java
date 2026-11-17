package net.shadowmage.ancientwarfare.vehicle.VehicleVarHelpers;

import net.minecraft.nbt.CompoundTag;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringVarsHelper;

public class DummyVehicleHelper extends VehicleFiringVarsHelper {

	/**
	 * @param vehicle
	 */
	public DummyVehicleHelper(VehicleBase vehicle) {
		super(vehicle);
	}

	@Override
	public CompoundTag serializeNBT() {
		// TODO Auto-generated method stub
		return new CompoundTag();
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {

	}

	@Override
	public void onFiringUpdate() {

	}

	@Override
	public void onReloadUpdate() {

	}

	@Override
	public void onLaunchingUpdate() {

	}

	@Override
	public void onReloadingFinished() {

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
