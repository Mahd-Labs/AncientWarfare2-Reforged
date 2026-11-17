package net.shadowmage.ancientwarfare.npc.entity.faction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.npc.ai.RestrictOpenDoorGoal;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIAttackMeleeLongRange;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIAttackNearest;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIDoor;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIFollowPlayer;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIMoveHome;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWander;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWatchClosest;
import net.shadowmage.ancientwarfare.npc.ai.faction.NpcAIFactionHurt;

public class NpcFactionMountedSoldier extends NpcFactionMounted {
	private NpcAIAttackMeleeLongRange meleeAI;

	@SuppressWarnings("unused") //required for deserialization
	public NpcFactionMountedSoldier(EntityType<? extends NpcFactionMountedSoldier> type, Level world) {
		super(type, world);
		addAI();
	}

	@SuppressWarnings("unused") //used in reflection
	public NpcFactionMountedSoldier(EntityType<? extends NpcFactionMountedSoldier> type, Level world, String factionName) {
		super(type, world, factionName);
		addAI();
	}

	private void addAI() {
		meleeAI = new NpcAIAttackMeleeLongRange(this);
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(0, new RestrictOpenDoorGoal(this));
		goalSelector.addGoal(0, new NpcAIDoor(this, true));
		goalSelector.addGoal(1, new NpcAIFollowPlayer(this));
		goalSelector.addGoal(3, meleeAI);
		goalSelector.addGoal(4, new NpcAIMoveHome(this, 50F, 5F, 30F, 5F));

		goalSelector.addGoal(101, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		goalSelector.addGoal(102, new NpcAIWander(this));
		goalSelector.addGoal(103, new NpcAIWatchClosest(this, Mob.class, 8.0F));

		targetSelector.addGoal(1, new NpcAIFactionHurt(this, this::isHostileTowards));
		targetSelector.addGoal(2, new NpcAIAttackNearest(this, this::isHostileTowards));
	}

	@Override
	public String getNpcType() {
		return "cavalry";
	}

	@Override
	public void onWeaponInventoryChanged() {
		super.onWeaponInventoryChanged();

		if (meleeAI != null) {
			meleeAI.setAttackReachFromWeapon(getMainHandItem());
		}
	}
}
