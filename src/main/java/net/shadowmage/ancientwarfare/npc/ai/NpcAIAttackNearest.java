package net.shadowmage.ancientwarfare.npc.ai;

import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.AABB;
import com.google.common.base.Predicate;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;

import javax.annotation.Nullable;

public class NpcAIAttackNearest extends NearestAttackableTargetGoal<LivingEntity> {
	@SuppressWarnings({"java:S4738", "Guava"})
	public NpcAIAttackNearest(NpcBase npc, final Predicate<Entity> targetSelector) {
		super(npc, LivingEntity.class, 0, true, false, null);
		this.selector = targetSelector;
	}

	private final Predicate<Entity> selector;

	/*
	 * Port notes: 1.12 EntityAINearestAttackableTarget.shouldExecute: every entity of the class within the targetable
	 * area that passes the selector and isSuitableTarget, nearest first. 1.21 would filter through TargetingConditions
	 * (visibility scaled range, team checks) instead, so the 1.12 selection is reproduced here.
	 */
	@Override
	protected void findTarget() {
		java.util.List<LivingEntity> list = mob.level().getEntitiesOfClass(LivingEntity.class, getTargetSearchArea(getFollowDistance()),
				e -> e != null && selector.test(e) && !e.isSpectator() && canAttack(e, TargetingConditions.DEFAULT));
		target = list.stream().min(java.util.Comparator.comparingDouble(e -> mob.distanceToSqr(e))).orElse(null);
	}

	@Override
	public boolean canUse() {
		boolean ret = super.canUse();
		if (!ret && mob.getTarget() != null && mob.getTarget().isRemoved()) {
			mob.setTarget(null);
		}
		return ret;
	}

	@Override
	protected boolean canAttack(LivingEntity target, TargetingConditions targetPredicate) {
		return AIHelper.isTarget((NpcBase) mob, target, mustSee);
	}

	@Override
	protected AABB getTargetSearchArea(double targetDistance) {
		return mob.getBoundingBox().inflate(targetDistance, targetDistance, targetDistance);
	}
}
