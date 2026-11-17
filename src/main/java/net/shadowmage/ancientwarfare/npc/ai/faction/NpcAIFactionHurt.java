package net.shadowmage.ancientwarfare.npc.ai.faction;

import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import com.google.common.base.Predicate;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;

import javax.annotation.Nullable;

public class NpcAIFactionHurt extends HurtByTargetGoal {

	private Predicate<Entity> targetSelector;

	public NpcAIFactionHurt(NpcFaction npc, final Predicate<Entity> targetSelector) {
		super(npc);
		setAlertOthers(); //1.12 entityCallsForHelp = true
		this.targetSelector = targetSelector;
	}

	@Override
	protected boolean canAttack(LivingEntity target, TargetingConditions targetPredicate) {
		return targetSelector.test(target);
	}

}
