package net.shadowmage.ancientwarfare.automation.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.automation.AncientWarfareAutomation;
import net.shadowmage.ancientwarfare.automation.block.BlockAutoCrafting;
import net.shadowmage.ancientwarfare.automation.block.BlockChunkLoaderDeluxe;
import net.shadowmage.ancientwarfare.automation.block.BlockChunkLoaderSimple;
import net.shadowmage.ancientwarfare.automation.block.BlockFlywheelController;
import net.shadowmage.ancientwarfare.automation.block.BlockFlywheelStorage;
import net.shadowmage.ancientwarfare.automation.block.BlockHandCrankedGenerator;
import net.shadowmage.ancientwarfare.automation.block.BlockMailbox;
import net.shadowmage.ancientwarfare.automation.block.BlockStirlingGenerator;
import net.shadowmage.ancientwarfare.automation.block.BlockTorqueDistributor;
import net.shadowmage.ancientwarfare.automation.block.BlockTorqueJunction;
import net.shadowmage.ancientwarfare.automation.block.BlockTorqueTransportShaft;
import net.shadowmage.ancientwarfare.automation.block.BlockWarehouseCraftingStation;
import net.shadowmage.ancientwarfare.automation.block.BlockWarehouseInterface;
import net.shadowmage.ancientwarfare.automation.block.BlockWarehouseStockLinker;
import net.shadowmage.ancientwarfare.automation.block.BlockWarehouseStockViewer;
import net.shadowmage.ancientwarfare.automation.block.BlockWarehouseStorage;
import net.shadowmage.ancientwarfare.automation.block.BlockWaterwheelGenerator;
import net.shadowmage.ancientwarfare.automation.block.BlockWindmillBlade;
import net.shadowmage.ancientwarfare.automation.block.BlockWindmillGenerator;
import net.shadowmage.ancientwarfare.automation.block.BlockWorksiteBase;
import net.shadowmage.ancientwarfare.automation.block.TorqueTier;
import net.shadowmage.ancientwarfare.automation.item.ItemBlockFlywheelStorage;
import net.shadowmage.ancientwarfare.automation.item.ItemBlockTorqueTile;
import net.shadowmage.ancientwarfare.automation.item.ItemBlockWarehouseStockLinker;
import net.shadowmage.ancientwarfare.automation.item.ItemBlockWarehouseStorage;
import net.shadowmage.ancientwarfare.automation.item.ItemBlockWorksiteStatic;
import net.shadowmage.ancientwarfare.automation.tile.ISidedItemHandlerTile;
import net.shadowmage.ancientwarfare.automation.tile.TileChunkLoaderDeluxe;
import net.shadowmage.ancientwarfare.automation.tile.TileChunkLoaderSimple;
import net.shadowmage.ancientwarfare.automation.tile.TileMailbox;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileConduitHeavy;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileConduitLight;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileConduitMedium;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileDistributorHeavy;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileDistributorLight;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileDistributorMedium;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileFlywheelControllerHeavy;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileFlywheelControllerLight;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileFlywheelControllerMedium;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileHandCrankedGenerator;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileStirlingGenerator;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueShaftHeavy;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueShaftLight;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueShaftMedium;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileWaterwheelGenerator;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileWindmillController;
import net.shadowmage.ancientwarfare.automation.tile.torque.multiblock.TileFlywheelStorage;
import net.shadowmage.ancientwarfare.automation.tile.torque.multiblock.TileWindmillBlade;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouse;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseCraftingStation;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseInterface;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseStockLinker;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseStockViewer;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseStorage;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseStorageLarge;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseStorageMedium;
import net.shadowmage.ancientwarfare.automation.tile.worksite.TileAutoCrafting;
import net.shadowmage.ancientwarfare.automation.tile.worksite.WorkSiteAnimalFarm;
import net.shadowmage.ancientwarfare.automation.tile.worksite.WorkSiteFishFarm;
import net.shadowmage.ancientwarfare.automation.tile.worksite.WorkSiteQuarry;
import net.shadowmage.ancientwarfare.automation.tile.worksite.cropfarm.WorkSiteCropFarm;
import net.shadowmage.ancientwarfare.automation.tile.worksite.fruitfarm.WorkSiteFruitFarm;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.WorkSiteTreeFarm;
import net.shadowmage.ancientwarfare.core.item.ItemBlockBase;
import net.shadowmage.ancientwarfare.core.item.ItemBlockOwnedRotatable;
import net.shadowmage.ancientwarfare.core.item.ItemBlockRotatableMetaTile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/*
 * Port notes: @ObjectHolder fields -> DeferredBlock holders (call .get() at use sites). Item blocks and block entity
 * types are registered here as well; block entity type ids are the 1.12 tile ids.
 * Flattening: the tiered blocks keep ONE block with a "tier" (warehouse_storage: "size") block state
 * property and get one block item per variant: <block>_light / _medium / _heavy (warehouse_storage_small / _medium /
 * _large). The *_ITEMS lists are indexed by the 1.12 meta value.
 */
@SuppressWarnings("WeakerAccess")
public class AWAutomationBlocks {
	private AWAutomationBlocks() {}

	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AncientWarfareAutomation.MOD_ID);
	private static final DeferredRegister.Items ITEM_BLOCKS = DeferredRegister.createItems(AncientWarfareAutomation.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AncientWarfareAutomation.MOD_ID);

	public static final DeferredBlock<BlockWorksiteBase> QUARRY = BLOCKS.register("quarry", () -> new BlockWorksiteBase("quarry").setTileFactory(WorkSiteQuarry::new));
	public static final DeferredBlock<BlockWorksiteBase> TREE_FARM = BLOCKS.register("tree_farm", () -> new BlockWorksiteBase("tree_farm").setTileFactory(WorkSiteTreeFarm::new));
	public static final DeferredBlock<BlockWorksiteBase> CROP_FARM = BLOCKS.register("crop_farm", () -> new BlockWorksiteBase("crop_farm").setTileFactory(WorkSiteCropFarm::new));
	public static final DeferredBlock<BlockWorksiteBase> FRUIT_FARM = BLOCKS.register("fruit_farm", () -> new BlockWorksiteBase("fruit_farm").setTileFactory(WorkSiteFruitFarm::new));
	public static final DeferredBlock<BlockWorksiteBase> ANIMAL_FARM = BLOCKS.register("animal_farm", () -> new BlockWorksiteBase("animal_farm").setTileFactory(WorkSiteAnimalFarm::new));
	public static final DeferredBlock<BlockWorksiteBase> FISH_FARM = BLOCKS.register("fish_farm", () -> new BlockWorksiteBase("fish_farm").setTileFactory(WorkSiteFishFarm::new));
	public static final DeferredBlock<BlockWorksiteBase> WAREHOUSE_CONTROL = BLOCKS.register("warehouse_control", () -> new BlockWorksiteBase("warehouse_control").setTileFactory(TileWarehouse::new));
	public static final DeferredBlock<BlockWarehouseStorage> WAREHOUSE_STORAGE = BLOCKS.register("warehouse_storage", () -> new BlockWarehouseStorage("warehouse_storage"));
	public static final DeferredBlock<BlockWarehouseInterface> WAREHOUSE_INTERFACE = BLOCKS.register("warehouse_interface", () -> new BlockWarehouseInterface("warehouse_interface"));
	public static final DeferredBlock<BlockWarehouseCraftingStation> WAREHOUSE_CRAFTING = BLOCKS.register("warehouse_crafting", () -> new BlockWarehouseCraftingStation("warehouse_crafting"));
	public static final DeferredBlock<BlockWarehouseStockViewer> WAREHOUSE_STOCK_VIEWER = BLOCKS.register("warehouse_stock_viewer", () -> new BlockWarehouseStockViewer("warehouse_stock_viewer"));
	public static final DeferredBlock<BlockWarehouseStockLinker> WAREHOUSE_STOCK_LINKER = BLOCKS.register("warehouse_stock_linker", () -> new BlockWarehouseStockLinker("warehouse_stock_linker"));
	public static final DeferredBlock<BlockAutoCrafting> AUTO_CRAFTING = BLOCKS.register("auto_crafting", () -> new BlockAutoCrafting("auto_crafting"));
	public static final DeferredBlock<BlockMailbox> MAILBOX = BLOCKS.register("mailbox", () -> new BlockMailbox("mailbox"));
	public static final DeferredBlock<BlockFlywheelController> FLYWHEEL_CONTROLLER = BLOCKS.register("flywheel_controller", () -> new BlockFlywheelController("flywheel_controller"));
	public static final DeferredBlock<BlockFlywheelStorage> FLYWHEEL_STORAGE = BLOCKS.register("flywheel_storage", () -> new BlockFlywheelStorage("flywheel_storage"));
	public static final DeferredBlock<BlockTorqueJunction> TORQUE_JUNCTION = BLOCKS.register("torque_junction", () -> new BlockTorqueJunction("torque_junction"));
	public static final DeferredBlock<BlockTorqueTransportShaft> TORQUE_SHAFT = BLOCKS.register("torque_shaft", () -> new BlockTorqueTransportShaft("torque_shaft"));
	public static final DeferredBlock<BlockTorqueDistributor> TORQUE_DISTRIBUTOR = BLOCKS.register("torque_distributor", () -> new BlockTorqueDistributor("torque_distributor"));
	public static final DeferredBlock<BlockStirlingGenerator> STIRLING_GENERATOR = BLOCKS.register("stirling_generator", () -> new BlockStirlingGenerator("stirling_generator"));
	public static final DeferredBlock<BlockWaterwheelGenerator> WATERWHEEL_GENERATOR = BLOCKS.register("waterwheel_generator", () -> new BlockWaterwheelGenerator("waterwheel_generator"));
	public static final DeferredBlock<BlockHandCrankedGenerator> HAND_CRANKED_GENERATOR = BLOCKS.register("hand_cranked_generator", () -> new BlockHandCrankedGenerator("hand_cranked_generator"));
	public static final DeferredBlock<BlockWindmillBlade> WINDMILL_BLADE = BLOCKS.register("windmill_blade", () -> new BlockWindmillBlade("windmill_blade"));
	public static final DeferredBlock<BlockWindmillGenerator> WINDMILL_GENERATOR = BLOCKS.register("windmill_generator", () -> new BlockWindmillGenerator("windmill_generator"));
	public static final DeferredBlock<BlockChunkLoaderSimple> CHUNK_LOADER_SIMPLE = BLOCKS.register("chunk_loader_simple", () -> new BlockChunkLoaderSimple("chunk_loader_simple"));
	public static final DeferredBlock<BlockChunkLoaderDeluxe> CHUNK_LOADER_DELUXE = BLOCKS.register("chunk_loader_deluxe", () -> new BlockChunkLoaderDeluxe("chunk_loader_deluxe"));

	/* item blocks, 1.12 registration order */
	public static final DeferredItem<BlockItem> QUARRY_ITEM = ITEM_BLOCKS.register("quarry", () -> new ItemBlockWorksiteStatic(QUARRY.get()));
	public static final DeferredItem<BlockItem> TREE_FARM_ITEM = ITEM_BLOCKS.register("tree_farm", () -> new ItemBlockWorksiteStatic(TREE_FARM.get()));
	public static final DeferredItem<BlockItem> CROP_FARM_ITEM = ITEM_BLOCKS.register("crop_farm", () -> new ItemBlockWorksiteStatic(CROP_FARM.get()));
	public static final DeferredItem<BlockItem> FRUIT_FARM_ITEM = ITEM_BLOCKS.register("fruit_farm", () -> new ItemBlockWorksiteStatic(FRUIT_FARM.get()));
	public static final DeferredItem<BlockItem> ANIMAL_FARM_ITEM = ITEM_BLOCKS.register("animal_farm", () -> new ItemBlockWorksiteStatic(ANIMAL_FARM.get()));
	public static final DeferredItem<BlockItem> FISH_FARM_ITEM = ITEM_BLOCKS.register("fish_farm", () -> new ItemBlockWorksiteStatic(FISH_FARM.get()));
	public static final DeferredItem<BlockItem> WAREHOUSE_CONTROL_ITEM = ITEM_BLOCKS.register("warehouse_control", () -> new ItemBlockWorksiteStatic(WAREHOUSE_CONTROL.get()));
	public static final List<DeferredItem<BlockItem>> WAREHOUSE_STORAGE_ITEMS;
	public static final DeferredItem<BlockItem> WAREHOUSE_INTERFACE_ITEM = ITEM_BLOCKS.register("warehouse_interface", () -> new ItemBlockBase(WAREHOUSE_INTERFACE.get()));
	public static final DeferredItem<BlockItem> WAREHOUSE_CRAFTING_ITEM = ITEM_BLOCKS.register("warehouse_crafting", () -> new ItemBlockBase(WAREHOUSE_CRAFTING.get()));
	public static final DeferredItem<BlockItem> WAREHOUSE_STOCK_VIEWER_ITEM = ITEM_BLOCKS.register("warehouse_stock_viewer", () -> new ItemBlockOwnedRotatable(WAREHOUSE_STOCK_VIEWER.get()));
	public static final DeferredItem<BlockItem> WAREHOUSE_STOCK_LINKER_ITEM = ITEM_BLOCKS.register("warehouse_stock_linker", () -> new ItemBlockWarehouseStockLinker(WAREHOUSE_STOCK_LINKER.get()));
	public static final DeferredItem<BlockItem> AUTO_CRAFTING_ITEM = ITEM_BLOCKS.register("auto_crafting", () -> new ItemBlockRotatableMetaTile(AUTO_CRAFTING.get()));
	public static final DeferredItem<BlockItem> MAILBOX_ITEM = ITEM_BLOCKS.register("mailbox", () -> new ItemBlockOwnedRotatable(MAILBOX.get()));
	public static final List<DeferredItem<BlockItem>> FLYWHEEL_CONTROLLER_ITEMS;
	public static final List<DeferredItem<BlockItem>> FLYWHEEL_STORAGE_ITEMS;
	public static final List<DeferredItem<BlockItem>> TORQUE_JUNCTION_ITEMS;
	public static final List<DeferredItem<BlockItem>> TORQUE_SHAFT_ITEMS;
	public static final List<DeferredItem<BlockItem>> TORQUE_DISTRIBUTOR_ITEMS;
	public static final DeferredItem<BlockItem> STIRLING_GENERATOR_ITEM;
	public static final DeferredItem<BlockItem> WATERWHEEL_GENERATOR_ITEM;
	public static final DeferredItem<BlockItem> HAND_CRANKED_GENERATOR_ITEM;
	public static final DeferredItem<BlockItem> WINDMILL_BLADE_ITEM;
	public static final DeferredItem<BlockItem> WINDMILL_GENERATOR_ITEM;
	public static final DeferredItem<BlockItem> CHUNK_LOADER_SIMPLE_ITEM;
	public static final DeferredItem<BlockItem> CHUNK_LOADER_DELUXE_ITEM;

	static {
		List<DeferredItem<BlockItem>> storage = new ArrayList<>();
		for (BlockWarehouseStorage.Size size : BlockWarehouseStorage.Size.values()) {
			storage.add(ITEM_BLOCKS.register("warehouse_storage_" + size.getSerializedName(), () -> new ItemBlockWarehouseStorage(WAREHOUSE_STORAGE.get(), size)));
		}
		WAREHOUSE_STORAGE_ITEMS = Collections.unmodifiableList(storage);
		FLYWHEEL_CONTROLLER_ITEMS = registerTorqueTierItems("flywheel_controller", FLYWHEEL_CONTROLLER);
		List<DeferredItem<BlockItem>> flywheelStorage = new ArrayList<>();
		for (TorqueTier tier : TorqueTier.values()) {
			flywheelStorage.add(ITEM_BLOCKS.register("flywheel_storage_" + tier.getSerializedName(), () -> new ItemBlockFlywheelStorage(FLYWHEEL_STORAGE.get(), tier)));
		}
		FLYWHEEL_STORAGE_ITEMS = Collections.unmodifiableList(flywheelStorage);
		TORQUE_JUNCTION_ITEMS = registerTorqueTierItems("torque_junction", TORQUE_JUNCTION);
		TORQUE_SHAFT_ITEMS = registerTorqueTierItems("torque_shaft", TORQUE_SHAFT);
		TORQUE_DISTRIBUTOR_ITEMS = registerTorqueTierItems("torque_distributor", TORQUE_DISTRIBUTOR);
		STIRLING_GENERATOR_ITEM = ITEM_BLOCKS.register("stirling_generator", () -> new ItemBlockTorqueTile(STIRLING_GENERATOR.get()));
		WATERWHEEL_GENERATOR_ITEM = ITEM_BLOCKS.register("waterwheel_generator", () -> new ItemBlockTorqueTile(WATERWHEEL_GENERATOR.get()));
		HAND_CRANKED_GENERATOR_ITEM = ITEM_BLOCKS.register("hand_cranked_generator", () -> new ItemBlockTorqueTile(HAND_CRANKED_GENERATOR.get()));
		WINDMILL_BLADE_ITEM = ITEM_BLOCKS.register("windmill_blade", () -> new ItemBlockBase(WINDMILL_BLADE.get()));
		WINDMILL_GENERATOR_ITEM = ITEM_BLOCKS.register("windmill_generator", () -> new ItemBlockTorqueTile(WINDMILL_GENERATOR.get()));
		CHUNK_LOADER_SIMPLE_ITEM = ITEM_BLOCKS.register("chunk_loader_simple", () -> new ItemBlockBase(CHUNK_LOADER_SIMPLE.get()));
		CHUNK_LOADER_DELUXE_ITEM = ITEM_BLOCKS.register("chunk_loader_deluxe", () -> new ItemBlockBase(CHUNK_LOADER_DELUXE.get()));
	}

	private static List<DeferredItem<BlockItem>> registerTorqueTierItems(String name, Supplier<? extends Block> block) {
		List<DeferredItem<BlockItem>> items = new ArrayList<>();
		for (TorqueTier tier : TorqueTier.values()) {
			items.add(ITEM_BLOCKS.register(name + "_" + tier.getSerializedName(), () -> new ItemBlockTorqueTile(block.get(), tier)));
		}
		return Collections.unmodifiableList(items);
	}

	/*
	 * the block item of a tiered block (flywheel_controller, flywheel_storage, torque_junction, torque_shaft,
	 * torque_distributor) for the given tier
	 */
	public static Item getTierItem(Block block, TorqueTier tier) {
		List<DeferredItem<BlockItem>> items;
		if (block == FLYWHEEL_CONTROLLER.get()) {
			items = FLYWHEEL_CONTROLLER_ITEMS;
		} else if (block == FLYWHEEL_STORAGE.get()) {
			items = FLYWHEEL_STORAGE_ITEMS;
		} else if (block == TORQUE_JUNCTION.get()) {
			items = TORQUE_JUNCTION_ITEMS;
		} else if (block == TORQUE_SHAFT.get()) {
			items = TORQUE_SHAFT_ITEMS;
		} else if (block == TORQUE_DISTRIBUTOR.get()) {
			items = TORQUE_DISTRIBUTOR_ITEMS;
		} else {
			return block.asItem();
		}
		return items.get(tier.getMeta()).get();
	}

	/* block entity types, ids = 1.12 tile ids */
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkSiteQuarry>> QUARRY_TILE = tile("quarry_tile", WorkSiteQuarry::new, QUARRY);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkSiteTreeFarm>> TREE_FARM_TILE = tile("forestry_tile", WorkSiteTreeFarm::new, TREE_FARM);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkSiteCropFarm>> CROP_FARM_TILE = tile("crop_farm_tile", WorkSiteCropFarm::new, CROP_FARM);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkSiteFruitFarm>> FRUIT_FARM_TILE = tile("fruit_farm_tile", WorkSiteFruitFarm::new, FRUIT_FARM);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkSiteAnimalFarm>> ANIMAL_FARM_TILE = tile("animal_farm_tile", WorkSiteAnimalFarm::new, ANIMAL_FARM);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkSiteFishFarm>> FISH_FARM_TILE = tile("fish_farm_tile", WorkSiteFishFarm::new, FISH_FARM);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWarehouse>> WAREHOUSE_CONTROL_TILE = tile("warehouse_control_tile", TileWarehouse::new, WAREHOUSE_CONTROL);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWarehouseStorageMedium>> WAREHOUSE_STORAGE_MEDIUM_TILE = tile("warehouse_storage_medium_tile", TileWarehouseStorageMedium::new, WAREHOUSE_STORAGE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWarehouseStorage>> WAREHOUSE_STORAGE_SMALL_TILE = tile("warehouse_storage_small_tile", TileWarehouseStorage::new, WAREHOUSE_STORAGE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWarehouseStorageLarge>> WAREHOUSE_STORAGE_LARGE_TILE = tile("warehouse_storage_large_tile", TileWarehouseStorageLarge::new, WAREHOUSE_STORAGE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWarehouseInterface>> WAREHOUSE_INTERFACE_TILE = tile("warehouse_interface_tile", TileWarehouseInterface::new, WAREHOUSE_INTERFACE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWarehouseCraftingStation>> WAREHOUSE_CRAFTING_TILE = tile("warehouse_crafting_tile", TileWarehouseCraftingStation::new, WAREHOUSE_CRAFTING);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWarehouseStockViewer>> WAREHOUSE_STOCK_VIEWER_TILE = tile("warehouse_stock_viewer_tile", TileWarehouseStockViewer::new, WAREHOUSE_STOCK_VIEWER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWarehouseStockLinker>> WAREHOUSE_STOCK_LINKER_TILE = tile("warehouse_stock_linker_tile", TileWarehouseStockLinker::new, WAREHOUSE_STOCK_LINKER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAutoCrafting>> AUTO_CRAFTING_TILE = tile("auto_crafting_tile", TileAutoCrafting::new, AUTO_CRAFTING);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileMailbox>> MAILBOX_TILE = tile("mailbox_tile", TileMailbox::new, MAILBOX);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileFlywheelControllerLight>> FLYWHEEL_LIGHT_TILE = tile("flywheel_light_tile", TileFlywheelControllerLight::new, FLYWHEEL_CONTROLLER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileFlywheelControllerMedium>> FLYWHEEL_MEDIUM_TILE = tile("flywheel_medium_tile", TileFlywheelControllerMedium::new, FLYWHEEL_CONTROLLER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileFlywheelControllerHeavy>> FLYWHEEL_LARGE_TILE = tile("flywheel_large_tile", TileFlywheelControllerHeavy::new, FLYWHEEL_CONTROLLER);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileFlywheelStorage>> FLYWHEEL_STORAGE_TILE = tile("flywheel_storage_tile", TileFlywheelStorage::new, FLYWHEEL_STORAGE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileConduitLight>> TORQUE_JUNCTION_LIGHT_TILE = tile("torque_junction_light_tile", TileConduitLight::new, TORQUE_JUNCTION);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileConduitMedium>> TORQUE_JUNCTION_MEDIUM_TILE = tile("torque_junction_medium_tile", TileConduitMedium::new, TORQUE_JUNCTION);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileConduitHeavy>> TORQUE_JUNCTION_HEAVY_TILE = tile("torque_junction_heavy_tile", TileConduitHeavy::new, TORQUE_JUNCTION);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTorqueShaftLight>> TORQUE_SHAFT_LIGHT_TILE = tile("torque_shaft_light_tile", TileTorqueShaftLight::new, TORQUE_SHAFT);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTorqueShaftMedium>> TORQUE_SHAFT_MEDIUM_TILE = tile("torque_shaft_medium_tile", TileTorqueShaftMedium::new, TORQUE_SHAFT);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTorqueShaftHeavy>> TORQUE_SHAFT_HEAVY_TILE = tile("torque_shaft_heavy_tile", TileTorqueShaftHeavy::new, TORQUE_SHAFT);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileDistributorLight>> TORQUE_DISTRIBUTOR_LIGHT_TILE = tile("torque_distributor_light_tile", TileDistributorLight::new, TORQUE_DISTRIBUTOR);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileDistributorMedium>> TORQUE_DISTRIBUTOR_MEDIUM_TILE = tile("torque_distributor_medium_tile", TileDistributorMedium::new, TORQUE_DISTRIBUTOR);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileDistributorHeavy>> TORQUE_DISTRIBUTOR_HEAVY_TILE = tile("torque_distributor_heavy_tile", TileDistributorHeavy::new, TORQUE_DISTRIBUTOR);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileStirlingGenerator>> STIRLING_GENERATOR_TILE = tile("stirling_generator_tile", TileStirlingGenerator::new, STIRLING_GENERATOR);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWaterwheelGenerator>> WATERWHEEL_GENERATOR_TILE = tile("waterwheel_generator_tile", TileWaterwheelGenerator::new, WATERWHEEL_GENERATOR);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileHandCrankedGenerator>> HAND_CRANKED_GENERATOR_TILE = tile("hand_cranked_generator_tile", TileHandCrankedGenerator::new, HAND_CRANKED_GENERATOR);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWindmillBlade>> WINDMILL_BLADE_TILE = tile("windmill_blade_tile", TileWindmillBlade::new, WINDMILL_BLADE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWindmillController>> WINDMILL_GENERATOR_TILE = tile("windmill_generator_tile", TileWindmillController::new, WINDMILL_GENERATOR);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileChunkLoaderSimple>> CHUNK_LOADER_SIMPLE_TILE = tile("chunk_loader_simple_tile", TileChunkLoaderSimple::new, CHUNK_LOADER_SIMPLE);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileChunkLoaderDeluxe>> CHUNK_LOADER_DELUXE_TILE = tile("chunk_loader_deluxe_tile", TileChunkLoaderDeluxe::new, CHUNK_LOADER_DELUXE);

	private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> tile(String id, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block) {
		return TILES.register(id, () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
	}

	public static void register(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEM_BLOCKS.register(modBus);
		TILES.register(modBus);
		modBus.addListener(AWAutomationBlocks::registerCapabilities);
	}

	/*
	 * 1.12 TileEntity.getCapability(ITEM_HANDLER_CAPABILITY, facing) of: TileWorksiteBoundedInventory (the six
	 * worksites), TileAutoCrafting, TileMailbox, TileStirlingGenerator, TileWarehouseInterface.
	 */
	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		registerItemHandler(event, QUARRY_TILE.get());
		registerItemHandler(event, TREE_FARM_TILE.get());
		registerItemHandler(event, CROP_FARM_TILE.get());
		registerItemHandler(event, FRUIT_FARM_TILE.get());
		registerItemHandler(event, ANIMAL_FARM_TILE.get());
		registerItemHandler(event, FISH_FARM_TILE.get());
		registerItemHandler(event, AUTO_CRAFTING_TILE.get());
		registerItemHandler(event, MAILBOX_TILE.get());
		registerItemHandler(event, STIRLING_GENERATOR_TILE.get());
		registerItemHandler(event, WAREHOUSE_INTERFACE_TILE.get());
	}

	private static <T extends BlockEntity & ISidedItemHandlerTile> void registerItemHandler(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, ISidedItemHandlerTile::getItemHandler);
	}
}
