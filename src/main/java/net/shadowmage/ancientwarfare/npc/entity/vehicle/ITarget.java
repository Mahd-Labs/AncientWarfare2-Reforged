package net.shadowmage.ancientwarfare.npc.entity.vehicle;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public interface ITarget {
	double getX();

	double getY();

	double getZ();

	AABB getBoundigBox();

	boolean exists(Level world);

	/*
	 * Port addition (improvement crew_leads_targets): blocks per tick the target moves, measured over the last ticks.
	 * Zero for anything that does not move.
	 */
	default net.minecraft.world.phys.Vec3 getVelocity(long gameTime) {
		return net.minecraft.world.phys.Vec3.ZERO;
	}
}
