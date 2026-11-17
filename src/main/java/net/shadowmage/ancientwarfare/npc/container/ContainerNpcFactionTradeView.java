package net.shadowmage.ancientwarfare.npc.container;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionTrader;
import net.shadowmage.ancientwarfare.npc.faction.FactionTracker;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;
import net.shadowmage.ancientwarfare.npc.registry.StandingChanges;
import net.shadowmage.ancientwarfare.npc.trade.FactionTradeList;

public class ContainerNpcFactionTradeView extends ContainerNpcBase<NpcFactionTrader> {
	private static final String DO_TRADE_TAG = "doTrade";
	private static final String TRADE_DATA_TAG = "tradeData";
	public final FactionTradeList tradeList;

	@SuppressWarnings("unused") //used in reflection
	public ContainerNpcFactionTradeView(Player player, int x, int y, int z) {
		super(player, x);
		tradeList = entity.getTradeList();
		entity.startTrade(player);

		addPlayerSlots();
	}

	@Override
	public void sendInitData() {
		tradeList.updateTradesForView();
		CompoundTag packetTag = new CompoundTag();
		packetTag.put(TRADE_DATA_TAG, tradeList.serializeNBT());
		sendDataToClient(packetTag);
	}

	@Override
	public void handlePacketData(CompoundTag tag) {
		if (tag.contains(TRADE_DATA_TAG)) {
			tradeList.deserializeNBT(tag.getCompound(TRADE_DATA_TAG));
		} else if (tag.contains(DO_TRADE_TAG) && tradeList.performTrade(player, tag.getInt(DO_TRADE_TAG))) {
			FactionTracker.INSTANCE.adjustStandingFor(entity.level(), player.getName().getString(), entity.getFaction(), FactionRegistry.getFaction(entity.getFaction()).getStandingSettings().getStandingChange(StandingChanges.TRADE));
		}
		refreshGui();
	}

	@Override
	public void onContainerClosed(Player player) {
		entity.closeTrade();
		super.onContainerClosed(player);
	}

	public void doTrade(int tradeNum) {
		CompoundTag tag = new CompoundTag();
		tag.putInt(DO_TRADE_TAG, tradeNum);
		sendDataToServer(tag);
	}

	public Level getWorld() {
		return player.level();
	}
}
