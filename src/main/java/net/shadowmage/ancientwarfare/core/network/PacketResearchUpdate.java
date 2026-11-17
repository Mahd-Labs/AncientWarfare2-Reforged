package net.shadowmage.ancientwarfare.core.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.core.research.ResearchTracker;

public class PacketResearchUpdate extends PacketBase {

	private String playerName;
	private String toAdd;
	private boolean add;
	private boolean live;

	public PacketResearchUpdate(String playerName, String toAdd, boolean add, boolean live) {
		this.playerName = playerName;
		this.toAdd = toAdd;
		this.add = add;
		this.live = live;
	}

	public PacketResearchUpdate() {}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		RegistryFriendlyByteBuf buffer = data;
		buffer.writeUtf(toAdd);
		buffer.writeBoolean(add);
		buffer.writeBoolean(live);
		buffer.writeUtf(playerName);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) {
		RegistryFriendlyByteBuf buffer = data;
		toAdd = buffer.readUtf(40);
		add = buffer.readBoolean();
		live = buffer.readBoolean();
		playerName = buffer.readUtf(16);
	}

	@Override
	protected void execute(Player player) {
		if (!player.level().isClientSide) {
			//from a client only the research station's queue buttons are accepted, for the researcher of the
			//station the player has open; granting research ("live") is the server's own notification
			if (live || !(player.containerMenu instanceof net.shadowmage.ancientwarfare.core.container.ContainerResearchStation station)
					|| station.researcherName == null || !station.researcherName.equals(playerName)) {
				net.shadowmage.ancientwarfare.AncientWarfare.LOG.warn("Refused research packet from client {}", player.getGameProfile().getName());
				return;
			}
		}
		if (live) {
			if (add) {
				ResearchTracker.INSTANCE.addResearch(player.level(), playerName, toAdd);
			}
		} else {
			if (add) {
				ResearchTracker.INSTANCE.addQueuedGoal(player.level(), playerName, toAdd);
			} else {
				ResearchTracker.INSTANCE.removeQueuedGoal(player.level(), playerName, toAdd);
			}
		}
	}

}
