package net.shadowmage.ancientwarfare.structure.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;

/*
 * Port addition: client -> server, asks for the data of one template. 1.12 sent this as a PacketStructure
 * with a "syncTemplate" tag; with its own packet PacketStructure only travels to the client, so its NBT (templates
 * of up to about 105 MB) can be read without a limit while everything a server reads stays bounded.
 */
public class PacketStructureRequest extends PacketBase {
	private String name = "";

	public PacketStructureRequest() {}

	public PacketStructureRequest(String name) {
		this.name = name;
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		data.writeUtf(name, 256);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) {
		name = data.readUtf(256);
	}

	@Override
	protected void execute(Player player) {
		if (!player.level().isClientSide) {
			StructureTemplateManager.onTemplateRequest(name);
		}
	}
}
