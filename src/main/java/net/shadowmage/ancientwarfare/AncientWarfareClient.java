package net.shadowmage.ancientwarfare;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.shadowmage.ancientwarfare.automation.AncientWarfareAutomationClient;
import net.shadowmage.ancientwarfare.core.AncientWarfareCoreClient;
import net.shadowmage.ancientwarfare.core.network.NetworkHandlerClient;
import net.shadowmage.ancientwarfare.core.util.RegistryAccessHelper;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPCClient;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructureClient;
import net.shadowmage.ancientwarfare.vehicle.AncientWarfareVehiclesClient;

/*
 * Client-only entry point. Replaces the 1.12 ClientProxy classes.
 */
@Mod(value = AncientWarfare.MOD_ID, dist = Dist.CLIENT)
public class AncientWarfareClient {
	public AncientWarfareClient(IEventBus modBus, ModContainer container) {
		RegistryAccessHelper.setClientProvider(() -> {
			Minecraft mc = Minecraft.getInstance();
			return mc.level != null ? mc.level.registryAccess() : null;
		});
		AncientWarfareCoreClient.init(modBus, container);
		AncientWarfareAutomationClient.init(modBus, container);
		AncientWarfareNPCClient.init(modBus, container);
		AncientWarfareStructureClient.init(modBus, container);
		AncientWarfareVehiclesClient.init(modBus, container);
		modBus.addListener(NetworkHandlerClient::onRegisterMenuScreens);
		if (net.shadowmage.ancientwarfare.core.util.ShowcaseHooks.isEnabled()) {
			net.shadowmage.ancientwarfare.core.util.ShowcaseHooks.startHarness();
		}
	}
}
