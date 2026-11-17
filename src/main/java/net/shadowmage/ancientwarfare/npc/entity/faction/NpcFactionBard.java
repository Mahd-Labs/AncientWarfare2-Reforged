package net.shadowmage.ancientwarfare.npc.entity.faction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.npc.ai.RestrictOpenDoorGoal;
import net.shadowmage.ancientwarfare.core.interfaces.ISinger;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.SongPlayData;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIDoor;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIFollowPlayer;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIMoveHome;
import net.shadowmage.ancientwarfare.npc.ai.NpcAISing;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWander;

public class NpcFactionBard extends NpcFaction implements ISinger {

	private SongPlayData tuneData = new SongPlayData();

	@SuppressWarnings("unused")
	public NpcFactionBard(EntityType<? extends NpcFactionBard> type, Level world) {
		super(type, world);
		addAI();
	}

	@SuppressWarnings("unused")
	public NpcFactionBard(EntityType<? extends NpcFactionBard> type, Level world, String factionName) {
		super(type, world, factionName);
		addAI();
	}

	private void addAI() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(0, new RestrictOpenDoorGoal(this));
		goalSelector.addGoal(0, new NpcAIDoor(this, true));
		goalSelector.addGoal(1, new NpcAIFollowPlayer(this));
		goalSelector.addGoal(2, new NpcAIMoveHome(this, 50F, 3F, 30F, 3F));
		goalSelector.addGoal(3, new NpcAISing(this));

		goalSelector.addGoal(101, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		goalSelector.addGoal(102, new NpcAIWander(this));
		goalSelector.addGoal(103, new LookAtPlayerGoal(this, Mob.class, 8.0F));
	}

	@Override
	public SongPlayData getSongs() {
		return tuneData;
	}

	@Override
	public boolean isHostileTowards(Entity e) {
		return false;
	}

	@Override
	public boolean canTarget(Entity e) {
		return false;
	}

	@Override
	public boolean hasAltGui() {
		return true;
	}

	@Override
	public String getNpcType() {
		return "bard";
	}

	@Override
	public void openAltGui(Player player) {
		NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_NPC_FACTION_BARD, getId(), 0, 0);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		tuneData.readFromNBT(tag.getCompound("tuneData"));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.put("tuneData", tuneData.writeToNBT(new CompoundTag()));
	}
}
