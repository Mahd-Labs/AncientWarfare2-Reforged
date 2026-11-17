package net.shadowmage.ancientwarfare.npc.xmod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.npc.entity.vehicle.ITarget;

import java.util.List;

/*
 * Port notes: view of vehicle.entity.VehicleBase as far as the NPC module needs it. The vehicle module is not ported
 * (and may never be); when it lands VehicleBase implements this interface and installs an IVehicleHooks
 * implementation through NpcXmod.setVehicleHooks. Every method names the 1.12 member it stands for.
 */
public interface IVehicle {
	/* the VehicleBase entity itself */
	Entity asEntity();

	/* VehicleBase.isDrivable() */
	boolean isDrivable();

	/* VehicleBase.vehicleType.canSoldiersPilot() */
	boolean canSoldiersPilot();

	/* VehicleBase.vehicleType.getConfigName() */
	String getConfigName();

	/* VehicleBase.vehicleType.canAdjustYaw() */
	boolean canAdjustYaw();

	/* VehicleBase.vehicleType.getBaseTurretRotationAmount() */
	float getBaseTurretRotationAmount();

	/* VehicleBase.localTurretRotationHome */
	float getLocalTurretRotationHome();

	/* VehicleBase.currentTurretRotationMax */
	float getCurrentTurretRotationMax();

	/* VehicleBase.getEffectiveRange(float) */
	float getEffectiveRange(float verticalOffset);

	/* VehicleBase.getMissileOffset() */
	Vec3 getMissileOffset();

	/* VehicleBase.firingHelper.isAimedAt(ITarget) */
	boolean isAimedAt(ITarget target);

	/* VehicleBase.firingHelper.getAimYaw(ITarget) */
	float getAimYaw(ITarget target);

	/* VehicleBase.firingHelper.isReadyToFire() */
	boolean isReadyToFire();

	/* VehicleBase.firingHelper.handleFireUpdate() */
	void handleFireUpdate();

	/* VehicleBase.firingHelper.handleSoldierTargetInput(double, double, double) */
	void handleSoldierTargetInput(double x, double y, double z);

	/* VehicleBase.moveHelper.setStrafeInput(byte) */
	void setStrafeInput(byte input);

	/* VehicleBase.moveHelper.setForwardInput(byte) */
	void setForwardInput(byte input);

	/* VehicleBase.moveHelper.stopMotion() */
	void stopMotion();

	/* VehicleBase.nav.forcePath(List<vehicle.pathing.Node>) */
	void forcePath(List<?> path);

	/*
	 * Port addition (improvement crew_leads_targets): the point an NPC crew should aim at to hit this target - where
	 * it will be when the missile arrives. The target itself with the option off or for a target that stands still.
	 */
	default ITarget leadTarget(ITarget target) {
		return target;
	}
}
