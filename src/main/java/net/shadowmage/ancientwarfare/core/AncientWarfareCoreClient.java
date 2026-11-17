package net.shadowmage.ancientwarfare.core;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.shadowmage.ancientwarfare.core.client.SilentSoundFilter;
import net.shadowmage.ancientwarfare.core.crafting.AWCraftingManager;
import net.shadowmage.ancientwarfare.core.gui.GuiBackpack;
import net.shadowmage.ancientwarfare.core.gui.GuiInfoTool;
import net.shadowmage.ancientwarfare.core.gui.GuiResearchBook;
import net.shadowmage.ancientwarfare.core.gui.crafting.GuiEngineeringStation;
import net.shadowmage.ancientwarfare.core.gui.manual.GuiManual;
import net.shadowmage.ancientwarfare.core.gui.research.GuiResearchStation;
import net.shadowmage.ancientwarfare.core.input.InputHandler;
import net.shadowmage.ancientwarfare.core.manual.ManualContentRegistry;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.NetworkHandlerClient;
import net.shadowmage.ancientwarfare.core.registry.RegistryLoader;
import net.shadowmage.ancientwarfare.core.util.ModelLoaderHelper;

/*
 * Client-only module entry point (replaces the 1.12 ClientProxy). Called from AncientWarfareClient,
 * which only loads on the physical client. Never reference this class from common code.
 *
 * 1.12 core.proxy.ClientProxy / ClientProxyBase and AncientWarfareCore client registrations:
 *  - NetworkHandler.registerGui(GUI_X, GuiX.class)       -> NetworkHandlerClient.registerGui(GUI_X, GuiX::new)
 *  - InputHandler.initKeyBindings()                      -> RegisterKeyMappingsEvent listener (InputHandler)
 *  - RegistryLoader.registerParser(ManualContentParser)  -> same call, plus the language supplier the common side
 *                                                           ManualContentRegistry asks for
 *  - IClientRegister / ModelRegistryEvent                -> gone: item and block models are found by registry name;
 *                                                           custom model locations go through ModelLoaderHelper
 *  - unicode FontRenderer                                -> core.gui.UnicodeFont (style with the minecraft:uniform font)
 *  - OptionsGuiFactory (Forge config gui)                -> NeoForge ConfigurationScreen for all ancientwarfare configs
 *  - setDebugResolution (512x288 window when DEBUG)      -> dropped, developer aid that resized the game window
 *  - (port addition) SilentSoundFilter: sound events whose files were removed for licensing play silently, without a log line
 */
public final class AncientWarfareCoreClient {
	private AncientWarfareCoreClient() {}

	public static void init(IEventBus modBus, ModContainer container) {
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_CRAFTING, GuiEngineeringStation::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_RESEARCH_STATION, GuiResearchStation::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_BACKPACK, GuiBackpack::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_RESEARCH_BOOK, GuiResearchBook::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_MANUAL, GuiManual::new);
		NetworkHandlerClient.registerGui(NetworkHandler.GUI_INFO_TOOL, GuiInfoTool::new);

		modBus.addListener(InputHandler::initKeyBindings);

		ManualContentRegistry.setCurrentLanguage(() -> Minecraft.getInstance().getLanguageManager().getSelected());
		RegistryLoader.registerParser(new ManualContentRegistry.ManualContentParser());

		AWCraftingManager.setClientRecipeManager(() -> {
			Minecraft mc = Minecraft.getInstance();
			return mc.level != null ? mc.level.getRecipeManager() : null;
		});

		//after the module listeners (default priority) have put their BakeryModels into the registry
		modBus.addListener(EventPriority.LOW, ModelLoaderHelper::onModifyBakingResult);

		container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

		NeoForge.EVENT_BUS.addListener(SilentSoundFilter::onPlaySound);
	}
}
