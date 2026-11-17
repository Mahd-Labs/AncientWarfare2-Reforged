package net.shadowmage.ancientwarfare.structure;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.core.registry.RegistryLoader;
import net.shadowmage.ancientwarfare.structure.command.CommandStructure;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;
import net.shadowmage.ancientwarfare.structure.container.ContainerDraftingStation;
import net.shadowmage.ancientwarfare.structure.container.ContainerGateControl;
import net.shadowmage.ancientwarfare.structure.container.ContainerLootBasket;
import net.shadowmage.ancientwarfare.structure.container.ContainerLootChestPlacer;
import net.shadowmage.ancientwarfare.structure.container.ContainerSoundBlock;
import net.shadowmage.ancientwarfare.structure.container.ContainerSpawnerAdvanced;
import net.shadowmage.ancientwarfare.structure.container.ContainerSpawnerAdvancedBlock;
import net.shadowmage.ancientwarfare.structure.container.ContainerSpawnerAdvancedInventoryBlock;
import net.shadowmage.ancientwarfare.structure.container.ContainerSpawnerAdvancedInventoryItem;
import net.shadowmage.ancientwarfare.structure.container.ContainerStake;
import net.shadowmage.ancientwarfare.structure.container.ContainerStatue;
import net.shadowmage.ancientwarfare.structure.container.ContainerStructureScanner;
import net.shadowmage.ancientwarfare.structure.container.ContainerStructureSelection;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.init.AWStructureEntities;
import net.shadowmage.ancientwarfare.structure.init.AWStructureItems;
import net.shadowmage.ancientwarfare.structure.init.AWStructureSounds;
import net.shadowmage.ancientwarfare.structure.network.PacketHighlightBlock;
import net.shadowmage.ancientwarfare.structure.network.PacketShowBoundingBoxes;
import net.shadowmage.ancientwarfare.structure.network.PacketSoundBlockPlayerSpecValues;
import net.shadowmage.ancientwarfare.structure.network.PacketStructure;
import net.shadowmage.ancientwarfare.structure.network.PacketStructureEntry;
import net.shadowmage.ancientwarfare.structure.network.PacketStructureRemove;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.shadowmage.ancientwarfare.structure.container.ContainerTownSelection;
import net.shadowmage.ancientwarfare.structure.registry.BiomeGroupRegistry;
import net.shadowmage.ancientwarfare.structure.registry.TerritorySettingRegistry;
import net.shadowmage.ancientwarfare.structure.template.WorldGenStructureManager;
import net.shadowmage.ancientwarfare.structure.worldgen.TerritoryManager;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldGenTickHandler;
import net.shadowmage.ancientwarfare.structure.registry.EntitySpawnNBTRegistry;
import net.shadowmage.ancientwarfare.structure.registry.StructureBlockRegistry;
import net.shadowmage.ancientwarfare.structure.template.StructurePluginManager;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.template.load.TemplateLoader;
import net.shadowmage.ancientwarfare.structure.util.CapabilityRespawnData;
import org.apache.logging.log4j.Logger;

/*
 * Module entry point. 1.12 had one @Mod per module; the port is a single mod ("ancientwarfare") and
 * AncientWarfare calls init() of every module from the mod constructor.
 *
 * 1.12 preInit (config, event handlers, entities, packets, containers, registry parsers, respawn data capability)
 * -> init(); 1.12 init / postInit (plugins, template loading) -> common setup. The client proxy is replaced by
 * AncientWarfareStructureClient + util.StructureClientHooks.
 * World generation: 1.12 GameRegistry.registerWorldGenerator -> WorldGenTickHandler listens for newly generated chunks
 * (see there); the biome -> template tables are built when a server starts (WorldGenStructureManager.rebuild), the
 * territory data capability is saved data (worldgen.TerritoryData).
 * Not ported: the template data fixers for pre-2.7 templates; the 1.12 world event listener
 * OneShotEntityDespawnListener is replaced by an EntityLeaveLevelEvent listener.
 */
public final class AncientWarfareStructure {
	private AncientWarfareStructure() {}

	public static final String MOD_ID = "ancientwarfare";

	public static final Logger LOG = AncientWarfare.LOG;

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = AWStructureTab.TAB;

	public static AWStructureStatics statics;

	public static void init(IEventBus modBus, ModContainer container) {
		statics = new AWStructureStatics("ancientwarfare-structure");
		statics.register(modBus, container);

		AWStructureSounds.register(modBus);
		AWStructureBlocks.register(modBus);
		AWStructureItems.register(modBus);
		AWStructureEntities.register(modBus);
		AWStructureTab.register(modBus);
		CapabilityRespawnData.register(modBus);

		NeoForge.EVENT_BUS.register(net.shadowmage.ancientwarfare.structure.event.EventHandler.INSTANCE);
		NeoForge.EVENT_BUS.addListener(AncientWarfareStructure::onLogin);
		NeoForge.EVENT_BUS.addListener(AncientWarfareStructure::registerCommands);
		//1.12: only registered when enable_world_generation was set at startup; the handler checks the option itself now (the config loads later)
		NeoForge.EVENT_BUS.register(WorldGenTickHandler.INSTANCE);
		NeoForge.EVENT_BUS.register(net.shadowmage.ancientwarfare.structure.template.build.StructurePlacementQueue.INSTANCE);
		NeoForge.EVENT_BUS.addListener(AncientWarfareStructure::serverAboutToStart);
		NeoForge.EVENT_BUS.addListener(AncientWarfareStructure::serverStop);
		NeoForge.EVENT_BUS.addListener(AncientWarfareStructure::serverStopped);

		PacketBase.registerPacketType(NetworkHandler.PACKET_STRUCTURE, PacketStructure.class, PacketStructure::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_STRUCTURE_REQUEST, net.shadowmage.ancientwarfare.structure.network.PacketStructureRequest.class, net.shadowmage.ancientwarfare.structure.network.PacketStructureRequest::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_SERVER);
		PacketBase.registerPacketType(NetworkHandler.PACKET_STRUCTURE_REMOVE, PacketStructureRemove.class, PacketStructureRemove::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_STRUCTURE_ENTRY, PacketStructureEntry.class, PacketStructureEntry::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_SOUND_BLOCK_PLAYER_SPEC_VALUES, PacketSoundBlockPlayerSpecValues.class, PacketSoundBlockPlayerSpecValues::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_SERVER);
		PacketBase.registerPacketType(NetworkHandler.PACKET_HIGHLIGHT_BLOCK, PacketHighlightBlock.class, PacketHighlightBlock::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);
		PacketBase.registerPacketType(NetworkHandler.PACKET_SHOW_BBS, PacketShowBoundingBoxes.class, PacketShowBoundingBoxes::new, net.shadowmage.ancientwarfare.core.network.PacketBase.Direction.TO_CLIENT);
		NetworkHandler.registerContainer(NetworkHandler.GUI_SCANNER, ContainerStructureScanner::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_BUILDER, ContainerStructureSelection::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_TOWN_BUILDER, ContainerTownSelection::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_SPAWNER_ADVANCED, ContainerSpawnerAdvanced::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_SPAWNER_ADVANCED_BLOCK, ContainerSpawnerAdvancedBlock::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_SPAWNER_ADVANCED_INVENTORY, ContainerSpawnerAdvancedInventoryItem::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_SPAWNER_ADVANCED_BLOCK_INVENTORY, ContainerSpawnerAdvancedInventoryBlock::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_GATE_CONTROL, ContainerGateControl::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_GATE_CONTROL_CREATIVE, ContainerGateControl::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_DRAFTING_STATION, ContainerDraftingStation::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_SOUND_BLOCK, ContainerSoundBlock::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_LOOT_CHEST_PLACER, ContainerLootChestPlacer::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_LOOT_BASKET, ContainerLootBasket::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_STAKE, ContainerStake::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_STATUE, ContainerStatue::new);

		RegistryLoader.registerParser(new EntitySpawnNBTRegistry.Parser());
		RegistryLoader.registerParser(new BiomeGroupRegistry.Parser());
		RegistryLoader.registerParser(new StructureBlockRegistry.Parser());
		RegistryLoader.registerParser(new TerritorySettingRegistry.Parser());

		StructureHooks.install();

		modBus.addListener(AncientWarfareStructure::commonSetup);
		modBus.addListener(AncientWarfareStructure::registerCapabilities);
	}

	/*
	 * 1.12 preInit tail (TemplateLoader.initializeAndExportDefaults), init and postInit
	 */
	private static void commonSetup(FMLCommonSetupEvent evt) {
		evt.enqueueWork(() -> {
			AWStructureBlocks.registerLootContainers();
			TemplateLoader.INSTANCE.initializeAndExportDefaults();
			StructurePluginManager.INSTANCE.loadPlugins();
			TemplateLoader.INSTANCE.loadTemplates();
			AWStructureStatics.logSkippableBlocksCoveredByMaterial();
		});
	}

	/*
	 * 1.12: the loot chest / loot basket tiles were TileEntityChest subclasses and had the vanilla item handler capability
	 */
	private static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent evt) {
		evt.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, AWStructureBlocks.ADVANCED_LOOT_CHEST_TILE.get(),
				(tile, side) -> new net.neoforged.neoforge.items.wrapper.InvWrapper(tile));
		evt.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, AWStructureBlocks.LOOT_BASKET_TILE.get(),
				(tile, side) -> new net.neoforged.neoforge.items.wrapper.InvWrapper(tile));
	}

	private static void onLogin(PlayerEvent.PlayerLoggedInEvent evt) {
		if (!evt.getEntity().level().isClientSide) {
			StructureTemplateManager.onPlayerConnect((ServerPlayer) evt.getEntity());
		}
	}

	/*
	 * 1.12 postInit WorldGenStructureManager.loadBiomeList + the world gen registration of every loaded template (the
	 * biome registry exists once a server is there)
	 */
	private static void serverAboutToStart(ServerAboutToStartEvent evt) {
		WorldGenStructureManager.INSTANCE.rebuild();
	}

	/*
	 * 1.12 serverStop
	 */
	private static void serverStop(ServerStoppingEvent evt) {
		//budgeted_placement (improvement): builds of the structure builder item / command still queued are completed
		//now, before the levels are saved (the world generation queue is completed by finalTick below)
		net.shadowmage.ancientwarfare.structure.template.build.StructurePlacementQueue.INSTANCE.finishAll();
		if (AWStructureStatics.enableWorldGen) {
			WorldGenTickHandler.INSTANCE.finalTick();
			LOG.info("World generation: {} new chunks, {} structure attempts, {} structures built, {} ms in total, longest tick {} ms, ticks over 200 ms {}", WorldGenTickHandler.INSTANCE.getChunksSeen(),
					WorldGenTickHandler.INSTANCE.getChunkTickets(), WorldGenTickHandler.INSTANCE.getStructuresGenerated(),
					WorldGenTickHandler.INSTANCE.getGenerationNanos() / 1000000, WorldGenTickHandler.INSTANCE.getMaxTickNanos() / 1000000, WorldGenTickHandler.INSTANCE.getSlowTicks());
			if (AWStructureStatics.backgroundAttemptChunks) {
				LOG.info("World generation attempts (background_attempt_chunks): {}", WorldGenTickHandler.INSTANCE.getAttemptPreparationSummary());
			}
		}
	}

	private static void serverStopped(ServerStoppedEvent evt) {
		net.shadowmage.ancientwarfare.structure.template.build.StructurePlacementQueue.INSTANCE.clear();
		WorldGenTickHandler.INSTANCE.clear();
		WorldGenStructureManager.INSTANCE.clearCachedTemplates();
		TerritoryManager.clearTerritoryCache();
	}

	/*
	 * 1.12 serverStart
	 */
	private static void registerCommands(RegisterCommandsEvent evt) {
		new CommandStructure().register(evt.getDispatcher());
	}
}
