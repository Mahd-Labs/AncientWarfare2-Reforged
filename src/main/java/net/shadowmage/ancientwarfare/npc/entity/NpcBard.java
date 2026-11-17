package net.shadowmage.ancientwarfare.npc.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
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
import net.shadowmage.ancientwarfare.npc.ai.NpcAIFleeHostiles;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIFollowPlayer;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIMoveHome;
import net.shadowmage.ancientwarfare.npc.ai.NpcAISing;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWander;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedAlarmResponse;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedFollowCommand;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedGetFood;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedIdleWhenHungry;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedRideHorse;

public class NpcBard extends NpcPlayerOwned implements ISinger {

	SongPlayData tuneData = new SongPlayData();

	public NpcBard(EntityType<? extends NpcBard> type, Level par1World) {
		super(type, par1World);
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(0, new RestrictOpenDoorGoal(this));
		this.goalSelector.addGoal(0, new NpcAIDoor(this, true));
		this.goalSelector.addGoal(0, (horseAI = new NpcAIPlayerOwnedRideHorse(this)));
		this.goalSelector.addGoal(2, new NpcAIFollowPlayer(this));
		this.goalSelector.addGoal(2, new NpcAIPlayerOwnedFollowCommand(this));
		this.goalSelector.addGoal(3, new NpcAIFleeHostiles(this));
		this.goalSelector.addGoal(3, new NpcAIPlayerOwnedAlarmResponse(this));
		this.goalSelector.addGoal(4, new NpcAIPlayerOwnedGetFood(this));
		this.goalSelector.addGoal(5, new NpcAIPlayerOwnedIdleWhenHungry(this));

		this.goalSelector.addGoal(7, new NpcAIMoveHome(this, 50F, 3F, 30F, 3F));
		this.goalSelector.addGoal(8, new NpcAISing(this));

		//post-100 -- used by delayed shared tasks (look at random stuff, wander)
		this.goalSelector.addGoal(101, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		this.goalSelector.addGoal(102, new NpcAIWander(this));
		this.goalSelector.addGoal(103, new LookAtPlayerGoal(this, Mob.class, 8.0F));
	}

	@Override
	public SongPlayData getSongs() {
		return tuneData;
	}

	@Override
	public String getNpcSubType() {
		return "";
	}

	@Override
	public String getNpcType() {
		return "bard";
	}

	@Override
	public boolean hasAltGui() {
		return true;
	}

	@Override
	public void openAltGui(Player player) {
		NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_NPC_BARD, getId(), 0, 0);
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
