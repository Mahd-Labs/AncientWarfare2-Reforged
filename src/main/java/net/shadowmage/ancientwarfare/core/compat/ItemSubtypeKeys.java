package net.shadowmage.ancientwarfare.core.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.shadowmage.ancientwarfare.core.init.AWCoreItems;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.npc.init.AWNPCItems;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.vehicle.init.AWVehicleItems;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/*
 * Port addition (JEI-free half of core/compat/jei, 1.12 AWJEIPlugin.registerItemSubtypes): the items whose variants
 * differ only by data (1.12: metadata / NBT, 1.21: the custom data component), with the custom-data keys that tell one
 * variant from another. The JEI plugin registers one subtype interpreter per item from this map; the key strings are
 * plain Java so a GameTest can check them without JEI.
 *
 * 1.12 registered the NPC spawner, fire pit, coin and totem part with useNbtForSubtypes and the vehicle spawner by
 * metadata + NBT. Everything else here is an item that 1.12 flattened by metadata (research note, coin stacks, wood
 * variants, gravestones, ...), which JEI 1.12 told apart by metadata on its own; in 1.21 they differ by custom data.
 * Only the identifying keys are used (an NPC spawner taken from a living NPC keeps its stored NPC data, a vehicle
 * spawner its spawn data, and is still the same subtype).
 */
public final class ItemSubtypeKeys {
	private ItemSubtypeKeys() {}

	private static Map<Item, Function<ItemStack, String>> interpreters;

	/*
	 * item -> function giving the subtype string of a stack of that item ("" for a stack without the data)
	 */
	public static synchronized Map<Item, Function<ItemStack, String>> getInterpreters() {
		if (interpreters == null) {
			Map<Item, Function<ItemStack, String>> map = new LinkedHashMap<>();
			map.put(AWCoreItems.RESEARCH_NOTE.get(), keys("researchName"));
			map.put(AWNPCItems.NPC_SPAWNER.get(), keys("npcType", "npcSubtype", "faction"));
			map.put(AWNPCItems.COIN.get(), keys("metal"));
			AWVehicleItems.SPAWNERS.values().forEach(spawner -> map.put(spawner.get(), nested("spawnData", "level")));
			put(map, AWStructureBlocks.STRUCTURE_BUILDER_TICKED.get(), keys("structureName"));
			put(map, AWStructureBlocks.FIRE_PIT.get(), keys("variant", "lit"));
			put(map, AWStructureBlocks.TOTEM_PART.get(), keys("variant"));
			put(map, AWStructureBlocks.BRAZIER_EMBER.get(), keys("lit"));
			put(map, AWStructureBlocks.BRAZIER_FLAME.get(), keys("lit"));
			//ItemBlockColored: dyeColor, or one of the faction themed tags (free-form, from the faction definitions)
			put(map, AWStructureBlocks.ALTAR_SHORT_CLOTH.get(), ItemSubtypeKeys::wholeCustomData);
			put(map, AWStructureBlocks.ALTAR_LONG_CLOTH.get(), ItemSubtypeKeys::wholeCustomData);
			put(map, AWStructureBlocks.ALTAR_CANDLE.get(), ItemSubtypeKeys::wholeCustomData);
			put(map, AWStructureBlocks.PROTECTION_FLAG.get(), keys("name"));
			put(map, AWStructureBlocks.DECORATIVE_FLAG.get(), keys("name"));
			put(map, AWStructureBlocks.WOODEN_COFFIN.get(), keys("variant"));
			put(map, AWStructureBlocks.STONE_COFFIN.get(), keys("variant"));
			put(map, AWStructureBlocks.STOOL.get(), keys("variant"));
			put(map, AWStructureBlocks.TABLE.get(), keys("variant"));
			put(map, AWStructureBlocks.CHAIR.get(), keys("variant"));
			put(map, AWStructureBlocks.BENCH.get(), keys("variant"));
			put(map, AWStructureBlocks.COIN_STACK_COPPER.get(), keys("size"));
			put(map, AWStructureBlocks.COIN_STACK_SILVER.get(), keys("size"));
			put(map, AWStructureBlocks.COIN_STACK_GOLD.get(), keys("size"));
			put(map, AWStructureBlocks.COIN_STACK_ANCIENT.get(), keys("size"));
			put(map, AWStructureBlocks.GRAVESTONE.get(), keys("variant"));
			interpreters = Collections.unmodifiableMap(map);
		}
		return interpreters;
	}

	/*
	 * the subtype string of the stack, "" when its item has no data variants (or the stack has none of the data)
	 */
	public static String getSubtypeKey(ItemStack stack) {
		Function<ItemStack, String> function = getInterpreters().get(stack.getItem());
		return function == null ? "" : function.apply(stack);
	}

	private static void put(Map<Item, Function<ItemStack, String>> map, Block block, Function<ItemStack, String> function) {
		map.put(block.asItem(), function);
	}

	private static Function<ItemStack, String> keys(String... keys) {
		return stack -> {
			if (!ItemTools.hasTag(stack)) {
				return "";
			}
			return keysOf(ItemTools.getTag(stack), keys);
		};
	}

	private static Function<ItemStack, String> nested(String compoundKey, String... keys) {
		return stack -> {
			if (!ItemTools.hasTag(stack)) {
				return "";
			}
			return keysOf(ItemTools.getTag(stack).getCompound(compoundKey), keys);
		};
	}

	private static String keysOf(CompoundTag tag, String... keys) {
		StringBuilder builder = new StringBuilder();
		for (String key : keys) {
			if (tag.contains(key)) {
				if (builder.length() > 0) {
					builder.append(',');
				}
				builder.append(key).append('=').append(tag.get(key).getAsString());
			}
		}
		return builder.toString();
	}

	private static String wholeCustomData(ItemStack stack) {
		return ItemTools.hasTag(stack) ? ItemTools.getTag(stack).toString() : "";
	}
}
