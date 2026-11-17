package net.shadowmage.ancientwarfare.structure.network;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.shadowmage.ancientwarfare.core.network.PacketBase;


public class PacketShowBoundingBoxes extends PacketBase {
	//1.12 StructureEntryBBRenderer.SHOW_BBS_TAG
	public static final String SHOW_BBS_TAG = "ancientwarfarestructure:showBBs";

	private boolean shouldShow;

	public PacketShowBoundingBoxes() {}

	public PacketShowBoundingBoxes(boolean shouldShow) {
		this.shouldShow = shouldShow;
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		data.writeBoolean(shouldShow);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) {
		shouldShow = data.readBoolean();
	}

	@Override
	protected void execute(Player player) {
		if (shouldShow) {
			player.addTag(SHOW_BBS_TAG);
		} else {
			player.removeTag(SHOW_BBS_TAG);
		}
	}
}
