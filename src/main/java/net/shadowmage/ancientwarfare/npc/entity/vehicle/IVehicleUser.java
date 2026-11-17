package net.shadowmage.ancientwarfare.npc.entity.vehicle;

import net.shadowmage.ancientwarfare.npc.xmod.IVehicle;

import java.util.Optional;

public interface IVehicleUser {
	void resetTarget();

	Optional<IVehicle> getSiegeVehicle();

	void setSiegeVehicle(IVehicle vehicle);

	void resetVehicle();

	boolean isRidingVehicle();

	boolean canContinueRidingVehicle();

	Optional<ITarget> getVehicleTarget();
}
