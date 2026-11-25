package net.shadowmage.ancientwarfare.automation;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.automation.chunkloader.AWChunkLoader;
import net.shadowmage.ancientwarfare.automation.command.CommandWarehouse;
import net.shadowmage.ancientwarfare.automation.config.AWAutomationStatics;
import net.shadowmage.ancientwarfare.automation.container.ContainerChunkLoaderDeluxe;
import net.shadowmage.ancientwarfare.automation.container.ContainerMailbox;
import net.shadowmage.ancientwarfare.automation.container.ContainerStirlingGenerator;
import net.shadowmage.ancientwarfare.automation.container.ContainerWarehouseControl;
import net.shadowmage.ancientwarfare.automation.container.ContainerWarehouseCraftingStation;
import net.shadowmage.ancientwarfare.automation.container.ContainerWarehouseInterface;
import net.shadowmage.ancientwarfare.automation.container.ContainerWarehouseStockLinker;
import net.shadowmage.ancientwarfare.automation.container.ContainerWarehouseStockViewer;
import net.shadowmage.ancientwarfare.automation.container.ContainerWarehouseStorage;
import net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteAnimalControl;
import net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteAnimalFarm;
import net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteAutoCrafting;
import net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteBoundsAdjust;
import net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteCropFarm;
import net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteFishControl;
import net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteFishFarm;
import net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteFruitFarm;
import net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteInventorySideSelection;
import net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteQuarry;
import net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteQuarryBounds;
import net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteTreeFarm;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationItems;
import net.shadowmage.ancientwarfare.automation.registry.CropFarmRegistry;
import net.shadowmage.ancientwarfare.automation.registry.FruitFarmRegistry;
import net.shadowmage.ancientwarfare.automation.registry.TreeFarmRegistry;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.WarehouseDebugger;
import net.shadowmage.ancientwarfare.automation.tile.worksite.WorkSiteAnimalFarm;
import net.shadowmage.ancientwarfare.automation.tile.worksite.WorkSiteAnimalFarm;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.registry.RegistryLoader;
import org.apache.logging.log4j.Logger;

/*
 * Module entry point. 1.12 had one @Mod per module; the port is a single mod ("ancientwarfare") and
 * AncientWarfare calls init() of every module from the mod constructor.
 *
 * 1.12 preInit (config, containers, event handlers, chunk loading callback, registry parsers) -> init().
 * Dropped: RFProxy (RedstoneFlux), AgricraftCompat (optional 1.12 integrations). The registry json files are loaded
 * by core (RegistryLoader.load in its common setup), the client proxy is replaced by AncientWarfareAutomationClient.
 */
public final class AncientWarfareAutomation {
	private AncientWarfareAutomation() {}

	public static final String MOD_ID = "ancientwarfare";

	public static final Logger LOG = AncientWarfare.LOG;

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = AWAutomationTab.TAB;

	public static AWAutomationStatics statics;

	public static void init(IEventBus modBus, ModContainer container) {
		statics = new AWAutomationStatics("ancientwarfare-automation");
		statics.register(modBus, container);

		AWAutomationBlocks.register(modBus);
		if (!net.neoforged.fml.loading.FMLEnvironment.production) {
			//stand-ins for other mods' broken crops, for the GameTests only; not in the released game
			net.shadowmage.ancientwarfare.automation.gametest.GameTestBlocks.register(modBus);
		}
		AWAutomationItems.register(modBus);
		AWAutomationTab.register(modBus);

		NetworkHandler.registerContainer(NetworkHandler.GUI_WORKSITE_INVENTORY_SIDE_ADJUST, ContainerWorksiteInventorySideSelection::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WORKSITE_ANIMAL_CONTROL, ContainerWorksiteAnimalControl::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WORKSITE_AUTO_CRAFT, ContainerWorksiteAutoCrafting::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WORKSITE_FISH_CONTROL, ContainerWorksiteFishControl::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_MAILBOX_INVENTORY, ContainerMailbox::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WAREHOUSE_CONTROL, ContainerWarehouseControl::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WAREHOUSE_STORAGE, ContainerWarehouseStorage::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WAREHOUSE_OUTPUT, ContainerWarehouseInterface::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WAREHOUSE_CRAFTING, ContainerWarehouseCraftingStation::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WORKSITE_QUARRY, ContainerWorksiteQuarry::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WORKSITE_QUARRY_BOUNDS, ContainerWorksiteQuarryBounds::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WORKSITE_TREE_FARM, ContainerWorksiteTreeFarm::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WORKSITE_CROP_FARM, ContainerWorksiteCropFarm::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WORKSITE_FRUIT_FARM, ContainerWorksiteFruitFarm::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WORKSITE_ANIMAL_FARM, ContainerWorksiteAnimalFarm::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WORKSITE_FISH_FARM, ContainerWorksiteFishFarm::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_STIRLING_GENERATOR, ContainerStirlingGenerator::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_CHUNK_LOADER_DELUXE, ContainerChunkLoaderDeluxe::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WAREHOUSE_STOCK, ContainerWarehouseStockViewer::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WAREHOUSE_STOCK_LINKER, ContainerWarehouseStockLinker::new);
		NetworkHandler.registerContainer(NetworkHandler.GUI_WORKSITE_BOUNDS, ContainerWorksiteBoundsAdjust::new);
		//the GUIs a client may switch to from another GUI of the same worksite
		for (int id : new int[] {NetworkHandler.GUI_WORKSITE_BOUNDS, NetworkHandler.GUI_WORKSITE_QUARRY_BOUNDS, NetworkHandler.GUI_WORKSITE_INVENTORY_SIDE_ADJUST,
				NetworkHandler.GUI_WORKSITE_ANIMAL_FARM, NetworkHandler.GUI_WORKSITE_FISH_FARM, NetworkHandler.GUI_WORKSITE_QUARRY,
				NetworkHandler.GUI_WORKSITE_ANIMAL_CONTROL, NetworkHandler.GUI_WORKSITE_FISH_CONTROL}) {
			NetworkHandler.allowClientOpen(id);
		}

		NeoForge.EVENT_BUS.register(new WarehouseDebugger());
		NeoForge.EVENT_BUS.addListener(WorkSiteAnimalFarm::onLivingDrops);
		AWChunkLoader.register(modBus);

		RegistryLoader.registerParser(new CropFarmRegistry.TillableParser());
		RegistryLoader.registerParser(new CropFarmRegistry.CropParser());
		RegistryLoader.registerParser(new CropFarmRegistry.SoilParser());
		RegistryLoader.registerParser(new FruitFarmRegistry.FruitParser());
		RegistryLoader.registerParser(new TreeFarmRegistry.PlantableParser());
		RegistryLoader.registerParser(new TreeFarmRegistry.SoilParser());
		RegistryLoader.registerParser(new TreeFarmRegistry.TreeScannerParser());

		NeoForge.EVENT_BUS.addListener(AncientWarfareAutomation::registerCommands);
	}

	/*
	 * 1.12 serverStart
	 */
	private static void registerCommands(RegisterCommandsEvent evt) {
		new CommandWarehouse().register(evt.getDispatcher());
	}
}
