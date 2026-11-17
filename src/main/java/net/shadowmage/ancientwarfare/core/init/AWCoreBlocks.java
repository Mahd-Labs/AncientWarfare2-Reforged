package net.shadowmage.ancientwarfare.core.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.block.BlockEngineeringStation;
import net.shadowmage.ancientwarfare.core.block.BlockResearchStation;
import net.shadowmage.ancientwarfare.core.item.ItemBlockRotatableMetaTile;
import net.shadowmage.ancientwarfare.core.tile.TileEngineeringStation;
import net.shadowmage.ancientwarfare.core.tile.TileResearchStation;

/*
 * Port notes: @ObjectHolder fields -> DeferredBlock holders (call .get() at use sites). Item blocks and block entity
 * types are registered here as well (1.12: registerItemBlocks / GameRegistry.registerTileEntity).
 */
public class AWCoreBlocks {
	private AWCoreBlocks() {}

	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AncientWarfareCore.MOD_ID);
	private static final DeferredRegister.Items ITEM_BLOCKS = DeferredRegister.createItems(AncientWarfareCore.MOD_ID);
	private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AncientWarfareCore.MOD_ID);

	public static final DeferredBlock<BlockEngineeringStation> ENGINEERING_STATION = BLOCKS.register("engineering_station", BlockEngineeringStation::new);
	public static final DeferredBlock<BlockResearchStation> RESEARCH_STATION = BLOCKS.register("research_station", BlockResearchStation::new);

	public static final DeferredItem<BlockItem> ENGINEERING_STATION_ITEM = ITEM_BLOCKS.register("engineering_station", () -> new ItemBlockRotatableMetaTile(ENGINEERING_STATION.get()));
	public static final DeferredItem<BlockItem> RESEARCH_STATION_ITEM = ITEM_BLOCKS.register("research_station", () -> new ItemBlockRotatableMetaTile(RESEARCH_STATION.get()));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEngineeringStation>> ENGINEERING_STATION_TILE = TILES.register("engineering_station_tile",
			() -> BlockEntityType.Builder.of(TileEngineeringStation::new, ENGINEERING_STATION.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileResearchStation>> RESEARCH_STATION_TILE = TILES.register("research_station_tile",
			() -> BlockEntityType.Builder.of(TileResearchStation::new, RESEARCH_STATION.get()).build(null));

	public static void register(IEventBus modBus) {
		BLOCKS.register(modBus);
		ITEM_BLOCKS.register(modBus);
		TILES.register(modBus);
		modBus.addListener(AWCoreBlocks::registerCapabilities);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		//1.12 TileResearchStation.getCapability: the resource inventory on every side
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RESEARCH_STATION_TILE.get(), (tile, side) -> tile.resourceInventory);
	}
}
