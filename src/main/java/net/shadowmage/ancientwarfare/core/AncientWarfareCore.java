package net.shadowmage.ancientwarfare.core;
import net.shadowmage.ancientwarfare.core.registry.IRegistryDataParser;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.command.CommandResearch;
import net.shadowmage.ancientwarfare.core.command.CommandUtils;
import net.shadowmage.ancientwarfare.core.compat.CompatLoader;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.container.ContainerBackpack;
import net.shadowmage.ancientwarfare.core.container.ContainerEngineeringStation;
import net.shadowmage.ancientwarfare.core.container.ContainerInfoTool;
import net.shadowmage.ancientwarfare.core.container.ContainerManual;
import net.shadowmage.ancientwarfare.core.container.ContainerResearchBook;
import net.shadowmage.ancientwarfare.core.container.ContainerResearchStation;
import net.shadowmage.ancientwarfare.core.crafting.AWCraftingManager;
import net.shadowmage.ancientwarfare.core.entity.AWFakePlayer;
import net.shadowmage.ancientwarfare.core.init.AWCoreBlocks;
import net.shadowmage.ancientwarfare.core.init.AWCoreItems;
import net.shadowmage.ancientwarfare.core.init.AWCoreLoot;
import net.shadowmage.ancientwarfare.core.item.ItemManual;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.core.network.PacketBlockEvent;
import net.shadowmage.ancientwarfare.core.network.PacketEntity;
import net.shadowmage.ancientwarfare.core.network.PacketItemInteraction;
import net.shadowmage.ancientwarfare.core.network.PacketItemMouseScroll;
import net.shadowmage.ancientwarfare.core.network.PacketManualReload;
import net.shadowmage.ancientwarfare.core.network.PacketResearchInit;
import net.shadowmage.ancientwarfare.core.network.PacketResearchStart;
import net.shadowmage.ancientwarfare.core.network.PacketResearchUpdate;
import net.shadowmage.ancientwarfare.core.registry.RegistryLoader;
import net.shadowmage.ancientwarfare.core.registry.ResearchRegistry;
import net.shadowmage.ancientwarfare.core.research.ResearchTracker;
import org.apache.logging.log4j.Logger;

/*
 * Module entry point. 1.12 had one @Mod per module; the port is a single mod ("ancientwarfare") and
 * AncientWarfare calls init() of every module from the mod constructor.
 *
 * 1.12 life cycle -> port:
 *  preInit  (config, network, event handlers, containers, registry parsers, compat) -> init(), i.e. mod construction
 *  registry events (blocks, items, recipes registry)                                  -> DeferredRegisters set up in init()
 *  init     (RegistryLoader.load, recipes, compat, loot)                              -> FMLCommonSetupEvent (registry
 *           json files; all blocks and items exist by then). Recipes and loot tables are datapack files.
 *  serverStarting (commands)                                                          -> RegisterCommandsEvent
 * The client proxy is replaced by AncientWarfareCoreClient.
 */
public final class AncientWarfareCore {
	private AncientWarfareCore() {}

	public static final String MOD_ID = "ancientwarfare";

	public static final Logger LOG = AncientWarfare.LOG;

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = AWCoreTab.TAB;

	public static AWCoreStatics statics;

	public static void init(IEventBus modBus, ModContainer container) {
		statics = new AWCoreStatics("ancientwarfare-core");
		statics.register(modBus, container);

		AWCoreBlocks.register(modBus);
		AWCoreItems.register(modBus);
		AWCoreTab.register(modBus);
		AWCraftingManager.init(modBus);

		PacketBase.registerPacketType(NetworkHandler.PACKET_ITEM_KEY_INTERFACE, PacketItemInteraction.class, PacketItemInteraction::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_SERVER);
		PacketBase.registerPacketType(NetworkHandler.PACKET_ENTITY, PacketEntity.class, PacketEntity::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_RESEARCH_INIT, PacketResearchInit.class, PacketResearchInit::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_RESEARCH_ADD, PacketResearchUpdate.class, PacketResearchUpdate::new);
		PacketBase.registerPacketType(NetworkHandler.PACKET_RESEARCH_START, PacketResearchStart.class, PacketResearchStart::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_BLOCK_EVENT, PacketBlockEvent.class, PacketBlockEvent::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_MANUAL_RELOAD, PacketManualReload.class, PacketManualReload::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_ITEM_MOUSE_SCROLL, PacketItemMouseScroll.class, PacketItemMouseScroll::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_SERVER);
		//1.12 registered it from the JEI plugin; both sides need it whether or not JEI is installed
		PacketBase.registerPacketType(NetworkHandler.PACKET_JEI_TRANSFER_RECIPE, net.shadowmage.ancientwarfare.core.network.PacketTransferRecipe.class, net.shadowmage.ancientwarfare.core.network.PacketTransferRecipe::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_SERVER);

		NeoForge.EVENT_BUS.register(ResearchTracker.INSTANCE);
		NeoForge.EVENT_BUS.addListener(ItemManual::handlePlayerFirstAWCraft);
		NeoForge.EVENT_BUS.addListener(AncientWarfareCore::registerCommands);
		//stacks in the json registries may carry 1.12 enchantment NBT, which needs the datapack registries of a server
		NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerAboutToStartEvent evt) -> RegistryLoader.reload("faction_trade_lists"));
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, AncientWarfareCore::onDimensionUnload);

		NetworkHandler.registerContainer(NetworkHandler.GUI_CRAFTING, ContainerEngineeringStation::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_RESEARCH_STATION, ContainerResearchStation::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_BACKPACK, ContainerBackpack::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_RESEARCH_BOOK, ContainerResearchBook::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_MANUAL, ContainerManual::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_INFO_TOOL, ContainerInfoTool::new);

		RegistryLoader.registerParser(new ResearchRegistry.ResearchParser());
		//1.12 registered the manual parser in the client proxy only and logged "No parser defined" for every manual file on
		//a dedicated server. A parser that ignores the files is registered here; the client entry point replaces it with
		//ManualContentRegistry.ManualContentParser.
		RegistryLoader.registerParser(new IRegistryDataParser() {
			@Override
			public String getName() {
				return "manual_content";
			}

			@Override
			public void parse(com.google.gson.JsonObject json) {
				//noop
			}
		});

		modBus.addListener(AncientWarfareCore::commonSetup);
	}

	/*
	 * 1.12 init()
	 */
	private static void commonSetup(FMLCommonSetupEvent evt) {
		evt.enqueueWork(() -> {
			AWCoreItems.load();
			RegistryLoader.load();
			CompatLoader.init();
			AWCoreLoot.init();
		});
	}

	private static void registerCommands(RegisterCommandsEvent evt) {
		new CommandResearch().register(evt.getDispatcher());
		new CommandUtils().register(evt.getDispatcher());
	}

	private static void onDimensionUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel)
			AWFakePlayer.onWorldUnload();
	}
}
