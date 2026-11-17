package net.shadowmage.ancientwarfare.npc.ai.vehicle;

import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.npc.ai.NpcAI;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.vehicle.IVehicleUser;

public class NpcAIFireVehicle<T extends NpcBase & IVehicleUser> extends NpcAI<T> {
	private int actionTick = 0;

	public NpcAIFireVehicle(T npc) {
		super(npc);
	}

	@Override
	public boolean canUse() {
		return super.canUse() && npc.getVehicleTarget().isPresent() && npc.canContinueRidingVehicle() && npc.isRidingVehicle() && isInRange() && canFire();
	}

	private boolean canFire() {
		return npc.getSiegeVehicle().map(v -> npc.getVehicleTarget().map(v::leadTarget).map(t -> v.isAimedAt(t)
				&& v.isReadyToFire()).orElse(false)).orElse(false);
	}

	private boolean isInRange() {
		return npc.getSiegeVehicle().map(v -> npc.getVehicleTarget().map(t -> v.getEffectiveRange((float) (t.getY() - v.asEntity().getY())) >= v.getMissileOffset().add(v.asEntity().position())
				.distanceTo(new Vec3(t.getX(), t.getY(), t.getZ()))).orElse(false)).orElse(false);
	}

	@Override
	public void updateTask() {
		if (actionTick <= 0) {
			npc.getSiegeVehicle().ifPresent(v -> v.handleFireUpdate());
			actionTick = 20;
		} else {
			actionTick--;
		}
	}
}
