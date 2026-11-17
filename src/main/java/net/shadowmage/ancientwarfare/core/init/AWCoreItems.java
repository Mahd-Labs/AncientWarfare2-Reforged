package net.shadowmage.ancientwarfare.core.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.inventory.ItemHandlerBackpack;
import net.shadowmage.ancientwarfare.core.item.ItemBackpack;
import net.shadowmage.ancientwarfare.core.item.ItemBaseCore;
import net.shadowmage.ancientwarfare.core.item.ItemComponent;
import net.shadowmage.ancientwarfare.core.item.ItemHammer;
import net.shadowmage.ancientwarfare.core.item.ItemInfoTool;
import net.shadowmage.ancientwarfare.core.item.ItemManual;
import net.shadowmage.ancientwarfare.core.item.ItemQuill;
import net.shadowmage.ancientwarfare.core.item.ItemResearchBook;
import net.shadowmage.ancientwarfare.core.item.ItemResearchNotes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Port notes: @ObjectHolder fields -> DeferredItem holders (call .get() at use sites).
 * Flattened sub items: backpack meta 0-3 -> backpack_0 .. backpack_3 (BACKPACKS, BACKPACK = backpack_0),
 * component meta 0-8 -> component_<name> (COMPONENTS, index = 1.12 meta).
 * Ore dictionary registrations (ingotSteel, gear*, bearing*, shaft*) are item tags in data/c/tags/item.
 */
@SuppressWarnings("WeakerAccess")
public class AWCoreItems {
	private AWCoreItems() {}

	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AncientWarfareCore.MOD_ID);

	public static final DeferredItem<ItemInfoTool> INFO_TOOL = ITEMS.register("info_tool", ItemInfoTool::new);
	public static final DeferredItem<ItemResearchBook> RESEARCH_BOOK = ITEMS.register("research_book", ItemResearchBook::new);
	public static final DeferredItem<ItemResearchNotes> RESEARCH_NOTE = ITEMS.register("research_note", ItemResearchNotes::new);

	public static final List<DeferredItem<ItemBackpack>> BACKPACKS;
	public static final DeferredItem<ItemBackpack> BACKPACK;

	public static final DeferredItem<ItemQuill> WOODEN_QUILL;
	public static final DeferredItem<ItemQuill> STONE_QUILL;
	public static final DeferredItem<ItemQuill> IRON_QUILL;
	public static final DeferredItem<ItemQuill> GOLD_QUILL;
	public static final DeferredItem<ItemQuill> DIAMOND_QUILL;
	public static final DeferredItem<ItemHammer> WOODEN_HAMMER;
	public static final DeferredItem<ItemHammer> STONE_HAMMER;
	public static final DeferredItem<ItemHammer> IRON_HAMMER;
	public static final DeferredItem<ItemHammer> GOLD_HAMMER;
	public static final DeferredItem<ItemHammer> DIAMOND_HAMMER;
	public static final DeferredItem<ItemManual> MANUAL;
	public static final List<DeferredItem<ItemComponent>> COMPONENTS;
	public static final DeferredItem<Item> STEEL_INGOT;

	static {
		List<DeferredItem<ItemBackpack>> backpacks = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			final int size = i;
			backpacks.add(ITEMS.register("backpack_" + i, () -> new ItemBackpack(size)));
		}
		BACKPACKS = Collections.unmodifiableList(backpacks);
		BACKPACK = BACKPACKS.get(0);

		WOODEN_QUILL = ITEMS.register("wooden_quill", () -> new ItemQuill("wooden_quill", Tiers.WOOD));
		STONE_QUILL = ITEMS.register("stone_quill", () -> new ItemQuill("stone_quill", Tiers.STONE));
		IRON_QUILL = ITEMS.register("iron_quill", () -> new ItemQuill("iron_quill", Tiers.IRON));
		GOLD_QUILL = ITEMS.register("gold_quill", () -> new ItemQuill("gold_quill", Tiers.GOLD));
		DIAMOND_QUILL = ITEMS.register("diamond_quill", () -> new ItemQuill("diamond_quill", Tiers.DIAMOND));
		WOODEN_HAMMER = ITEMS.register("wooden_hammer", () -> new ItemHammer("wooden_hammer", Tiers.WOOD));
		STONE_HAMMER = ITEMS.register("stone_hammer", () -> new ItemHammer("stone_hammer", Tiers.STONE));
		IRON_HAMMER = ITEMS.register("iron_hammer", () -> new ItemHammer("iron_hammer", Tiers.IRON));
		GOLD_HAMMER = ITEMS.register("gold_hammer", () -> new ItemHammer("gold_hammer", Tiers.GOLD));
		DIAMOND_HAMMER = ITEMS.register("diamond_hammer", () -> new ItemHammer("diamond_hammer", Tiers.DIAMOND));
		MANUAL = ITEMS.register("manual", ItemManual::new);

		List<DeferredItem<ItemComponent>> components = new ArrayList<>();
		for (int i = 0; i < ItemComponent.SUB_NAMES.length; i++) {
			final int num = i;
			components.add(ITEMS.register("component_" + ItemComponent.SUB_NAMES[i], () -> new ItemComponent(num)));
		}
		COMPONENTS = Collections.unmodifiableList(components);

		STEEL_INGOT = ITEMS.register("steel_ingot", () -> new ItemBaseCore("steel_ingot"));
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
		modBus.addListener(AWCoreItems::registerCapabilities);
	}

	/*
	 * 1.12 load(): registered steel ingot in the ore dictionary - a tag now, nothing left to do
	 */
	public static void load() {
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		//1.12 ItemBackpack.initCapabilities
		event.registerItem(Capabilities.ItemHandler.ITEM, (stack, context) -> new ItemHandlerBackpack(stack), BACKPACKS.get(0).get(), BACKPACKS.get(1).get(), BACKPACKS.get(2).get(), BACKPACKS.get(3).get());
	}
}
