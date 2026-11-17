package net.shadowmage.ancientwarfare.npc.ai.faction;

import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.phys.Vec3;

public class NpcAIFactionPanic extends PanicGoal {

	public NpcAIFactionPanic(PathfinderMob creature, double speedIn) {
		super(creature, speedIn);
	}

	/*
	 * 1.12 EntityAIPanic.shouldExecute: revenge target set or burning (1.21 looks at the last damage type instead)
	 */
	@Override
	protected boolean shouldPanic() {
		return mob.getLastHurtByMob() != null || mob.isOnFire();
	}

	@Override
	protected boolean findRandomPosition() {
		PathfinderMob creature = mob;

		Vec3 vec3d;
		if (creature.getLastHurtByMob() != null) {
			vec3d = DefaultRandomPos.getPosAway(creature, 10, 4, creature.getLastHurtByMob().position());
		} else {
			vec3d = DefaultRandomPos.getPos(creature, 5, 4);
		}

		if (vec3d == null) {
			return false;
		} else {
			posX = vec3d.x;
			posY = vec3d.y;
			posZ = vec3d.z;
			return true;
		}
	}

}
