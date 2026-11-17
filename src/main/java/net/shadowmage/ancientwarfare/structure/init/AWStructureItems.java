package net.shadowmage.ancientwarfare.structure.init;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockColored;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockInfo;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockStructureBuilder;
import net.shadowmage.ancientwarfare.structure.item.ItemBlockTotemPart;
import net.shadowmage.ancientwarfare.structure.item.ItemConstructionTool;
import net.shadowmage.ancientwarfare.structure.item.ItemConstructionToolLakes;
import net.shadowmage.ancientwarfare.structure.item.ItemGateSpawner;
import net.shadowmage.ancientwarfare.structure.item.ItemLootChestPlacer;
import net.shadowmage.ancientwarfare.structure.item.ItemSpawnerPlacer;
import net.shadowmage.ancientwarfare.structure.item.ItemStructureBuilder;
import net.shadowmage.ancientwarfare.structure.item.ItemStructureBuilderWorldGen;
import net.shadowmage.ancientwarfare.structure.item.ItemStructureScanner;
import net.shadowmage.ancientwarfare.structure.item.ItemTownBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Port notes: @ObjectHolder fields -> DeferredItem holders (call .get()). The 1.12 class only had constants for some
 * items; every registered item has one here. The block item constants of the 1.12 class point at the holders
 * registered in AWStructureBlocks.
 */
public class AWStructureItems {
	private AWStructureItems() {}

	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AncientWarfareStructure.MOD_ID);

	/* 1.12 registration order */
	public static final DeferredItem<ItemStructureScanner> STRUCTURE_SCANNER = ITEMS.register("structure_scanner", () -> new ItemStructureScanner("structure_scanner"));
	public static final DeferredItem<ItemStructureBuilder> STRUCTURE_BUILDER = ITEMS.register("structure_builder", () -> new ItemStructureBuilder("structure_builder"));
	public static final DeferredItem<ItemStructureBuilderWorldGen> STRUCTURE_BUILDER_WORLD_GEN = ITEMS.register("structure_builder_world_gen", () -> new ItemStructureBuilderWorldGen("structure_builder_world_gen"));
	public static final DeferredItem<ItemTownBuilder> TOWN_BUILDER = ITEMS.register("town_builder", () -> new ItemTownBuilder("town_builder"));
	public static final DeferredItem<ItemSpawnerPlacer> SPAWNER_PLACER = ITEMS.register("spawner_placer", () -> new ItemSpawnerPlacer("spawner_placer"));
	/*
	 * Flattened: 1.12 gate_spawner with the gate type as meta -> gate_spawner_<gate id> (ids 0, 1, 4, 5, 8, 9, 12).
	 * GATE_SPAWNER is the meta 0 item (basic wooden gate).
	 */
	public static final Map<Integer, DeferredItem<ItemGateSpawner>> GATE_SPAWNERS = new LinkedHashMap<>();
	public static final DeferredItem<ItemGateSpawner> GATE_SPAWNER;

	static {
		for (int id : new int[] {0, 1, 4, 5, 8, 9, 12}) {
			GATE_SPAWNERS.put(id, ITEMS.register("gate_spawner_" + id, () -> new ItemGateSpawner("gate_spawner", id)));
		}
		GATE_SPAWNER = GATE_SPAWNERS.get(0);
	}

	public static ItemGateSpawner getGateSpawner(int gateId) {
		DeferredItem<ItemGateSpawner> item = GATE_SPAWNERS.get(gateId);
		return (item == null ? GATE_SPAWNER : item).get();
	}

	public static final DeferredItem<ItemConstructionTool> CONSTRUCTION_TOOL = ITEMS.register("construction_tool", () -> new ItemConstructionTool("construction_tool"));
	public static final DeferredItem<ItemConstructionToolLakes> CONSTRUCTION_TOOL_LAKES = ITEMS.register("construction_tool_lakes", () -> new ItemConstructionToolLakes("construction_tool_lakes"));
	public static final DeferredItem<ItemBlockInfo> BLOCK_INFO_CLICKER = ITEMS.register("block_info_clicker", () -> new ItemBlockInfo("block_info_clicker"));
	public static final DeferredItem<ItemLootChestPlacer> LOOT_CHEST_PLACER = ITEMS.register("loot_chest_placer", ItemLootChestPlacer::new);

	public static final DeferredItem<ItemBlockTotemPart> TOTEM_PART = AWStructureBlocks.TOTEM_PART_ITEM;
	public static final DeferredItem<ItemBlockStructureBuilder> STRUCTURE_BUILDER_TICKED = AWStructureBlocks.STRUCTURE_BUILDER_TICKED_ITEM;
	public static final DeferredItem<ItemBlockColored> ALTAR_CANDLE = AWStructureBlocks.ALTAR_CANDLE_ITEM;
	public static final DeferredItem<ItemBlockColored> ALTAR_SHORT_CLOTH = AWStructureBlocks.ALTAR_SHORT_CLOTH_ITEM;
	public static final DeferredItem<ItemBlockColored> ALTAR_LONG_CLOTH = AWStructureBlocks.ALTAR_LONG_CLOTH_ITEM;

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
	}
}
