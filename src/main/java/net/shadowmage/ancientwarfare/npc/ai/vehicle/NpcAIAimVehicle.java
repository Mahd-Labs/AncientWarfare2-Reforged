package net.shadowmage.ancientwarfare.npc.ai.vehicle;

import net.shadowmage.ancientwarfare.npc.xmod.IVehicle;
import net.shadowmage.ancientwarfare.core.util.Trig;
import net.shadowmage.ancientwarfare.npc.ai.NpcAI;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.vehicle.ITarget;
import net.shadowmage.ancientwarfare.npc.entity.vehicle.IVehicleUser;

public class NpcAIAimVehicle<T extends NpcBase & IVehicleUser> extends NpcAI<T> {
	public NpcAIAimVehicle(T npc) {
		super(npc);
	}

	@Override
	@SuppressWarnings("squid:S3655")
	public boolean canUse() {
		return super.canUse() && npc.getVehicleTarget().isPresent() && npc.canContinueRidingVehicle() && npc.isRidingVehicle()
				&& npc.getVehicleTarget().map(t -> !npc.getSiegeVehicle().map(v -> v.isAimedAt(v.leadTarget(t))).orElse(false)).orElse(false);
	}

	@Override
	@SuppressWarnings("squid:S3655")
	public void updateTask() {
		npc.getSiegeVehicle().ifPresent(vehicle -> npc.getVehicleTarget().map(vehicle::leadTarget).ifPresent(target -> {
					if (turnVehicleIfYawDifferenceGreat(vehicle, target)) {
						return;
					}
					vehicle.setStrafeInput((byte) 0);
					vehicle.setForwardInput((byte) 0);
					vehicle.handleSoldierTargetInput(target.getX(), target.getY(), target.getZ());
				}
		));
	}

	@SuppressWarnings("squid:S1066")
	private boolean turnVehicleIfYawDifferenceGreat(IVehicle vehicle, ITarget target) {
		float yawDiff = Trig.getAngleDiffSigned(vehicle.asEntity().getYRot(), vehicle.getAimYaw(target));

		if (!vehicle.canAdjustYaw() && Math.abs(yawDiff) < 2) {
			//if there's a minor difference in the rotation just set the rotation to it instead of continues steps to one side and back
			vehicle.asEntity().setYRot(vehicle.asEntity().getYRot() + yawDiff);
			vehicle.stopMotion();
		} else if (vehicle.getBaseTurretRotationAmount() < 180 || Math.abs(yawDiff) > 120) {
			//if turret cannot rotate fully around, or if it can but yaw diff is great, turn towards target
			if (!Trig.isAngleBetween(vehicle.asEntity().getYRot() + yawDiff, vehicle.getLocalTurretRotationHome() - getMaxRotDifference(vehicle),
					vehicle.getLocalTurretRotationHome() + getMaxRotDifference(vehicle))) {
				if (yawDiff < 0) {
					vehicle.setStrafeInput((byte) 1); //left
				} else {
					vehicle.setStrafeInput((byte) -1); //right
				}
				vehicle.setForwardInput((byte) 0);
				vehicle.handleSoldierTargetInput(target.getX(), target.getY(), target.getZ());
				return true;
			}
		}
		return false;
	}

	private float getMaxRotDifference(IVehicle vehicle) {
		return Math.min(vehicle.getCurrentTurretRotationMax() + 1.5f, 180f);
	}

	@Override
	public void stop() {
		npc.getSiegeVehicle().ifPresent(v -> v.setStrafeInput((byte) 0));
	}
}
