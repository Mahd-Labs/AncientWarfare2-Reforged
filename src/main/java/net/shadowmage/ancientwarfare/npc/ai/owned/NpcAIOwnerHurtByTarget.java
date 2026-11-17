package net.shadowmage.ancientwarfare.npc.ai.owned;

import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.shadowmage.ancientwarfare.npc.ai.AIHelper;
import net.shadowmage.ancientwarfare.npc.entity.NpcPlayerOwned;

import javax.annotation.Nullable;

public class NpcAIOwnerHurtByTarget extends TargetGoal {
	private final NpcPlayerOwned npc;
	private LivingEntity attacker;
	private int timestamp;

	public NpcAIOwnerHurtByTarget(NpcPlayerOwned npc) {
		super(npc, false);
		this.npc = npc;
		setFlags(java.util.EnumSet.of(Flag.TARGET)); //1.12 mutex 1 in the target task list, like the vanilla target goals
	}

	@Override
	public boolean canUse() {
		LivingEntity entitylivingbase = npc.level().getPlayerByUUID(npc.getOwner().getUUID());

		if (entitylivingbase == null) {
			return false;
		} else {
			attacker = entitylivingbase.getLastHurtByMob();
			int i = entitylivingbase.getLastHurtByMobTimestamp();
			return i != timestamp && canAttack(attacker, TargetingConditions.DEFAULT) && AIHelper.isWithinFollowRange(npc, attacker);
		}
	}

	@Override
	public void start() {
		mob.setTarget(attacker);
		LivingEntity entitylivingbase = npc.level().getPlayerByUUID(npc.getOwner().getUUID());

		if (entitylivingbase != null) {
			timestamp = entitylivingbase.getLastHurtByMobTimestamp();
		}

		super.start();
	}

	@Override
	protected boolean canAttack(LivingEntity target, TargetingConditions targetPredicate) {
		return AIHelper.isTarget(npc, target, mustSee);
	}
}
