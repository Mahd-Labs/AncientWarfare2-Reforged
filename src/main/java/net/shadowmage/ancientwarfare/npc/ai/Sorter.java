package net.shadowmage.ancientwarfare.npc.ai;

import net.minecraft.world.entity.Entity;

import java.util.Comparator;

/*
 * Port addition: 1.12 vanilla EntityAINearestAttackableTarget.Sorter (nearest entity first), which the NPC AI used
 * for its own target lists and which no longer exists in 1.21.1.
 */
public class Sorter implements Comparator<Entity> {
	private final Entity entity;

	public Sorter(Entity entityIn) {
		this.entity = entityIn;
	}

	@Override
	public int compare(Entity p_compare_1_, Entity p_compare_2_) {
		double d0 = this.entity.distanceToSqr(p_compare_1_);
		double d1 = this.entity.distanceToSqr(p_compare_2_);

		if (d0 < d1) {
			return -1;
		} else {
			return d0 > d1 ? 1 : 0;
		}
	}
}
