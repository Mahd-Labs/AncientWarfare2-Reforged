package net.shadowmage.ancientwarfare.npc.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/*
 * Port notes: stands in for the vanilla 1.12 EntityAIRestrictOpenDoor that every NPC registered at priority 0.
 * That task only ran at night while the mob stood next to a *village door* (VillageCollection door info) and then
 * kept the mob from pathing through doors. Village door data does not exist in 1.21.1, so the condition can never
 * be met: the goal is kept at the same priority for a faithful task list but never becomes active - which is also
 * what the 1.12 task did for every NPC that was not inside a vanilla village.
 */
public class RestrictOpenDoorGoal extends Goal {
	@SuppressWarnings({"unused", "FieldCanBeLocal"})
	private final PathfinderMob entity;

	public RestrictOpenDoorGoal(PathfinderMob creature) {
		this.entity = creature;
	}

	@Override
	public boolean canUse() {
		return false;
	}
}
