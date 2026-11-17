package net.shadowmage.ancientwarfare.npc.ai.vehicle;

import net.shadowmage.ancientwarfare.npc.xmod.NpcXmod;
import net.shadowmage.ancientwarfare.npc.xmod.IVehicle;
import com.google.common.base.Predicate;
import net.shadowmage.ancientwarfare.npc.ai.NpcAI;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.vehicle.IVehicleUser;

import java.util.Comparator;
import java.util.List;

public class NpcAIFindVehicle<T extends NpcBase & IVehicleUser> extends NpcAI<T> {
	private static final double SEARCH_DISTANCE = 30D;
	@SuppressWarnings({"Guava", "java:S4738"}) // need to use Guava Predicate because of vanilla getEntitiesWithinAABB uses it
	private static final Predicate<IVehicle> SELECTOR = v -> v != null && v.isDrivable() && v.asEntity().getPassengers().isEmpty();

	public NpcAIFindVehicle(T npc) {
		super(npc);
	}

	@Override
	public boolean canUse() {
		return super.canUse() && !npc.getSiegeVehicle().isPresent() && npc.canContinueRidingVehicle() && (!npc.isPassenger() || NpcXmod.vehicle().asVehicle(npc.getVehicle()) != null);
	}

	@Override
	public void updateTask() {
		if (npc.isPassenger()) {
			//noinspection ConstantConditions
			npc.setSiegeVehicle(NpcXmod.vehicle().asVehicle(npc.getVehicle()));
			return;
		}

		List<IVehicle> vehicles = NpcXmod.vehicle().getVehiclesWithinAABB(npc.level(), npc.getBoundingBox().inflate(SEARCH_DISTANCE)).stream().filter(SELECTOR::test).collect(java.util.stream.Collectors.toList()); // TODO-CROSS(vehicle): world.getEntitiesWithinAABB(VehicleBase.class, bb, SELECTOR)
		vehicles.stream().filter(v -> !v.asEntity().isVehicle() && v.canSoldiersPilot()).min(Comparator.comparing(v -> v.asEntity().distanceToSqr(npc))).ifPresent(v -> npc.setSiegeVehicle(v));
	}
}
