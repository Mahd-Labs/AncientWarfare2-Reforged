package net.shadowmage.ancientwarfare.npc.ai.vehicle;

import net.minecraft.world.entity.Entity;
import net.shadowmage.ancientwarfare.npc.ai.NpcAI;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.vehicle.IVehicleUser;

public class NpcAIMountVehicle<T extends NpcBase & IVehicleUser> extends NpcAI<T> {
	private static final double MOUNT_REACH = 1.0D;

	public NpcAIMountVehicle(T npc) {
		super(npc);
	}

	@Override
	public boolean canUse() {
		return super.canUse() && !npc.isPassenger() && npc.canContinueRidingVehicle() && !npc.getSiegeVehicle().map(v -> v.asEntity().isVehicle()).orElse(false);
	}

	@Override
	public void updateTask() {
		npc.getSiegeVehicle().ifPresent(vehicle -> {
			double distance = npc.distanceToSqr(vehicle.asEntity().blockPosition());

			if (npc.getBoundingBox().inflate(MOUNT_REACH).intersects(vehicle.asEntity().getBoundingBox())) {
				npc.startRiding(vehicle.asEntity());
			} else {
				moveToPosition(vehicle.asEntity().blockPosition(), distance);
				npc.addAITask(TASK_MOVE);
			}
		});
	}

	@Override
	public void stop() {
		super.stop();
		npc.resetVehicle();
		npc.removeAITask(TASK_MOVE);
	}
}
