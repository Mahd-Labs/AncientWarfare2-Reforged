package net.shadowmage.ancientwarfare.structure.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.core.util.StringTools;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;

public class PacketStructureRemove extends PacketBase {
	private String structureName;

	public PacketStructureRemove() {}

	public PacketStructureRemove(String name) {
		structureName = name;
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		data.writeUtf(structureName);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) {
		structureName = data.readUtf();
	}

	@Override
	protected void execute() {
		StructureTemplateManager.removeTemplate(structureName);
	}
}
