package net.shadowmage.ancientwarfare.npc.entity.faction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.npc.ai.RestrictOpenDoorGoal;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIAttackNearest;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIDoor;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIFollowPlayer;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIMoveHome;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWander;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWatchClosest;
import net.shadowmage.ancientwarfare.npc.ai.faction.NpcAIFactionArcherStayAtHome;
import net.shadowmage.ancientwarfare.npc.ai.faction.NpcAIFactionHurt;

/* Spellcaster NPC class for the base spellcaster NPCs, casting some basic AW spells.
 * This class is only used if EBWizardry is not present. Cannot have any reference to EBWizardry objects. */
public class NpcFactionSpellcaster extends NpcFaction {

	@SuppressWarnings("unused")
	public NpcFactionSpellcaster(EntityType<? extends NpcFactionSpellcaster> type, Level world) {
		super(type, world);
		addAI();
	}

	@SuppressWarnings("unused")
	public NpcFactionSpellcaster(EntityType<? extends NpcFactionSpellcaster> type, Level world, String factionName) {
		super(type, world, factionName);
		addAI();
	}

	@SuppressWarnings("Duplicates")
	private void addAI() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(0, new RestrictOpenDoorGoal(this));
		goalSelector.addGoal(0, new NpcAIDoor(this, true));
		goalSelector.addGoal(1, new NpcAIFollowPlayer(this));
		goalSelector.addGoal(2, new NpcAIMoveHome(this, 50.f, 5.f, 30.f, 5.f));
		goalSelector.addGoal(2, new NpcAIFactionArcherStayAtHome(this));

		goalSelector.addGoal(101, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		goalSelector.addGoal(102, new NpcAIWander(this));
		goalSelector.addGoal(103, new NpcAIWatchClosest(this, Mob.class, 8.0F));

		targetSelector.addGoal(1, new NpcAIFactionHurt(this, this::isHostileTowards));
		targetSelector.addGoal(2, new NpcAIAttackNearest(this, this::isHostileTowards));
	}

	@Override
	protected boolean canAttackFlying() {
		return true;
	}

	@Override
	public boolean worksInRain() {
		return true;
	}

	@Override
	public boolean isPassive() {
		return false;
	}

	@Override
	public String getNpcType() {
		return "spellcaster";
	}

	@Override
	public boolean shouldSleep() {
		return false;
	}
}
