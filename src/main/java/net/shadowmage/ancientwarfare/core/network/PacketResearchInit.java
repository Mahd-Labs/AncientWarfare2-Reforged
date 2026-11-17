package net.shadowmage.ancientwarfare.core.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.core.research.ResearchData;
import net.shadowmage.ancientwarfare.core.research.ResearchTracker;
import net.shadowmage.ancientwarfare.core.util.PacketHelper;

/*
 * server -> client only
 */
public class PacketResearchInit extends PacketBase {

	private CompoundTag researchDataTag;

	public PacketResearchInit(ResearchData data) {
		researchDataTag = new CompoundTag();
		data.writeToNBT(researchDataTag);
	}

	public PacketResearchInit() {}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		PacketHelper.writeNBTTag(data, researchDataTag);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) {
		researchDataTag = PacketHelper.readServerNBTTag(data);
	}

	@Override
	protected void execute(Player player) {
		if (player.level().isClientSide) {
			ResearchTracker.INSTANCE.onClientResearchReceived(researchDataTag);
		}
	}
}
