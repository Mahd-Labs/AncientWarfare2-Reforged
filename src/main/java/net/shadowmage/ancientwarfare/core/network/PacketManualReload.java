package net.shadowmage.ancientwarfare.core.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.core.manual.ManualContentRegistry;
import net.shadowmage.ancientwarfare.core.registry.RegistryLoader;

import java.io.IOException;

/*
 * server -> client only (sent by /awutils reloadmanual)
 */
public class PacketManualReload extends PacketBase {
	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		//noop
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) throws IOException {
		//noop
	}

	@Override
	protected void execute(Player player) {
		if (!player.level().isClientSide) {
			return;
		}
		ManualContentRegistry.clearContents();
		RegistryLoader.reload("manual_content");
		player.sendSystemMessage(Component.literal("Manual content reloaded"));
	}
}
