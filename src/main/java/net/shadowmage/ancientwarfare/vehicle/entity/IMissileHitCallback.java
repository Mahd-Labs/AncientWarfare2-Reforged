package net.shadowmage.ancientwarfare.vehicle.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * used by missiles to communicate hit information to vastly different classes
 * (soldiers and vehicles)
 * soldiers and vehicles use this for stat tracking purposes
 * effectively server-side only.
 *
 * @author Shadowmage
 */
public interface IMissileHitCallback {
	/**
	 * callback for when a fired missile impacts a position
	 *
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 */
	void onMissileImpact(Level world, double x, double y, double z);

	/**
	 * callback for when a fired missile impacts an entity
	 *
	 * @param world
	 * @param entity
	 */
	void onMissileImpactEntity(Level world, Entity entity);
}
