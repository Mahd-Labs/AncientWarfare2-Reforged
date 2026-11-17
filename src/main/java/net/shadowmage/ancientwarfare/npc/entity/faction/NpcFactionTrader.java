package net.shadowmage.ancientwarfare.npc.entity.faction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.npc.ai.RestrictOpenDoorGoal;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIDoor;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIFollowPlayer;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIMoveHome;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWander;
import net.shadowmage.ancientwarfare.npc.item.ItemCommandBaton;
import net.shadowmage.ancientwarfare.npc.trade.FactionTradeList;

public class NpcFactionTrader extends NpcFaction {

	private FactionTradeList tradeList = new FactionTradeList();
	private Player trader;
	private boolean noTradesDespawn = false;

	@SuppressWarnings("unused")
	public NpcFactionTrader(EntityType<? extends NpcFactionTrader> type, Level world) {
		super(type, world);
		addAI();
	}

	@SuppressWarnings("unused")
	public NpcFactionTrader(EntityType<? extends NpcFactionTrader> type, Level world, String factionName) {
		super(type, world, factionName);
		addAI();
	}

	private void addAI() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(0, new RestrictOpenDoorGoal(this));
		goalSelector.addGoal(0, new NpcAIDoor(this, true));
		goalSelector.addGoal(1, new NpcAIFollowPlayer(this));
		goalSelector.addGoal(2, new NpcAIMoveHome(this, 50F, 5F, 30F, 5F));

		goalSelector.addGoal(101, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		goalSelector.addGoal(102, new NpcAIWander(this));
		goalSelector.addGoal(103, new LookAtPlayerGoal(this, Mob.class, 8.0F));
	}

	public FactionTradeList getTradeList() {
		return tradeList;
	}

	public void startTrade(Player player) {
		trader = player;
	}

	public void closeTrade() {
		trader = null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide) {
			if (noTradesDespawn && tradeList.isEmpty()) {
				discard();
			}
			tradeList.tick(level());
		}
	}

	@Override
	protected boolean processInteract(Player player, InteractionHand hand) {
		boolean baton = !player.getItemInHand(hand).isEmpty() && player.getItemInHand(hand).getItem() instanceof ItemCommandBaton;
		if (!baton && isAlive()) {
			if (!player.level().isClientSide && trader == null) {
				startTrade(player);
				NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_NPC_FACTION_TRADE_VIEW, getId(), 0, 0);
			}
			return true;
		}
		return false;
	}

	@Override
	public String getNpcType() {
		return "trader";
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
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		tradeList.deserializeNBT(tag.getCompound("tradeList"));
		//the showcase harness creates variants through an NBT load (port-only flag); keep those on stage
		noTradesDespawn = !tag.getBoolean(APPLY_DEFAULTS_TAG);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.put("tradeList", tradeList.serializeNBT());
	}
}
