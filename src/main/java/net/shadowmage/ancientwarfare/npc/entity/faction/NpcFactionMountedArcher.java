package net.shadowmage.ancientwarfare.npc.entity.faction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.npc.ai.RestrictOpenDoorGoal;
import com.google.common.base.Predicate;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIAttackNearest;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIDoor;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIFollowPlayer;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWander;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWatchClosest;
import net.shadowmage.ancientwarfare.npc.ai.faction.NpcAIFactionArcherStayAtHome;
import net.shadowmage.ancientwarfare.npc.ai.faction.NpcAIFactionHurt;
import net.shadowmage.ancientwarfare.npc.ai.faction.NpcAIFactionRangedAttack;
import net.shadowmage.ancientwarfare.npc.entity.RangeAttackHelper;

public class NpcFactionMountedArcher extends NpcFactionMounted implements RangedAttackMob {
	@SuppressWarnings("unused") //used when deserializing
	public NpcFactionMountedArcher(EntityType<? extends NpcFactionMountedArcher> type, Level world) {
		super(type, world);
		addAI();
	}

	@SuppressWarnings("unused") //used in reflection
	public NpcFactionMountedArcher(EntityType<? extends NpcFactionMountedArcher> type, Level world, String factionName) {
		super(type, world, factionName);
		addAI();
	}

	private void addAI() {
		//noinspection Guava - because dependency on what vanilla does
		Predicate<Entity> selector = entity -> {
			//noinspection ConstantConditions
			if (!isHostileTowards(entity)) {
				return false;
			}
			if (hasRestriction()) {
				BlockPos home = getRestrictCenter();
				double dist = entity.distanceToSqr(home.getX() + 0.5d, home.getY(), home.getZ() + 0.5d);
				if (dist > 30 * 30) {
					return false;
				}
			}
			return true;
		};

		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(0, new RestrictOpenDoorGoal(this));
		goalSelector.addGoal(0, new NpcAIDoor(this, true));
		goalSelector.addGoal(1, new NpcAIFollowPlayer(this));
		goalSelector.addGoal(2, new NpcAIFactionArcherStayAtHome(this));
		goalSelector.addGoal(3, new NpcAIFactionRangedAttack(this));

		goalSelector.addGoal(101, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		goalSelector.addGoal(102, new NpcAIWander(this));
		goalSelector.addGoal(103, new NpcAIWatchClosest(this, Mob.class, 8.0F));

		targetSelector.addGoal(1, new NpcAIFactionHurt(this, selector));
		targetSelector.addGoal(2, new NpcAIAttackNearest(this, selector));
	}

	@Override
	public String getNpcType() {
		return "mounted_archer";
	}

	@Override
	public void performRangedAttack(LivingEntity target, float force) {
		RangeAttackHelper.doRangedAttack(this, target, force, 1.0f);
	}
}
