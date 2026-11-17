package net.shadowmage.ancientwarfare.npc.xmod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/*
 * Port notes: everything the NPC module asks of the (unported) vehicle module. NONE behaves like "vehicle module
 * absent": no entity is a vehicle and none can be found.
 */
public interface IVehicleHooks {
	IVehicleHooks NONE = new IVehicleHooks() {};

	/* 1.12: entity instanceof VehicleBase ? (VehicleBase) entity : null */
	@Nullable
	default IVehicle asVehicle(@Nullable Entity entity) {
		return null;
	}

	/* 1.12: world.getEntitiesWithinAABB(VehicleBase.class, bb) */
	default List<IVehicle> getVehiclesWithinAABB(Level level, AABB bb) {
		return Collections.emptyList();
	}
}
