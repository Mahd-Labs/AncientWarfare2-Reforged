package net.shadowmage.ancientwarfare.core.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.compat.ItemSubtypeKeys;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.npc.item.ItemCoin;
import net.shadowmage.ancientwarfare.npc.item.ItemNpcSpawner;
import net.shadowmage.ancientwarfare.npc.item.ItemCoin.CoinMetal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * The subtype strings the JEI plugin (core/compat/jei) hands JEI, checked without JEI: every item that is in the mod's
 * creative tabs more than once must have an interpreter, and its stacks must all get different strings (else JEI
 * collapses them and logs "duplicate items were found in '<tab>'"); copies of one variant get the same string.
 */
@GameTestHolder(AncientWarfare.MOD_ID)
@PrefixGameTestTemplate(false)
public class ItemSubtypeGameTests {

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = "core_empty", timeoutTicks = 100)
	public static void creativeTabVariantsHaveDistinctSubtypeKeys(GameTestHelper helper) {
		CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
		List<String> problems = new ArrayList<>();
		int tabs = 0;
		int variantItems = 0;
		for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
			ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
			if (id == null || !id.getNamespace().equals(AncientWarfare.MOD_ID)) {
				continue;
			}
			tabs++;
			Map<Item, List<ItemStack>> byItem = new LinkedHashMap<>();
			for (ItemStack stack : tab.getDisplayItems()) {
				byItem.computeIfAbsent(stack.getItem(), i -> new ArrayList<>()).add(stack);
			}
			for (Map.Entry<Item, List<ItemStack>> entry : byItem.entrySet()) {
				if (entry.getValue().size() < 2) {
					continue;
				}
				variantItems++;
				String itemName = BuiltInRegistries.ITEM.getKey(entry.getKey()).toString();
				if (!ItemSubtypeKeys.getInterpreters().containsKey(entry.getKey())) {
					problems.add(id + ": " + itemName + " has " + entry.getValue().size() + " stacks but no subtype interpreter");
					continue;
				}
				Map<String, ItemStack> seen = new HashMap<>();
				for (ItemStack stack : entry.getValue()) {
					String key = ItemSubtypeKeys.getSubtypeKey(stack);
					if (!key.equals(ItemSubtypeKeys.getSubtypeKey(stack.copy()))) {
						problems.add(itemName + ": a copy gets another key than " + key);
					}
					ItemStack other = seen.put(key, stack);
					if (other != null) {
						problems.add(id + ": " + itemName + " two stacks get the key '" + key + "': " + ItemTools.getTag(other) + " / " + ItemTools.getTag(stack));
					}
				}
			}
		}
		if (tabs != 5) {
			problems.add("expected the 5 tabs of the mod, found " + tabs);
		}

		//identifying data only: extra data on a stack (an NPC picked up as a spawner) keeps its subtype
		ItemStack spawner = ItemNpcSpawner.getStackForNpcType("combat", "archer");
		ItemStack withData = spawner.copy();
		ItemTools.setTagInfo(withData, "npcStoredData", new CompoundTag());
		if (!ItemSubtypeKeys.getSubtypeKey(spawner).equals(ItemSubtypeKeys.getSubtypeKey(withData))) {
			problems.add("stored NPC data changes the NPC spawner subtype");
		}
		Set<String> coinKeys = new HashSet<>();
		for (CoinMetal metal : CoinMetal.values()) {
			coinKeys.add(ItemSubtypeKeys.getSubtypeKey(ItemCoin.getCoinStack(metal, 1)));
			if (!ItemSubtypeKeys.getSubtypeKey(ItemCoin.getCoinStack(metal, 1)).equals(ItemSubtypeKeys.getSubtypeKey(ItemCoin.getCoinStack(metal, 5)))) {
				problems.add("stack size changes the coin subtype");
			}
		}
		if (coinKeys.size() != CoinMetal.values().length) {
			problems.add("coin metals share subtype keys: " + coinKeys);
		}
		AncientWarfare.LOG.info("Item subtypes: {} interpreters, {} items with variants in the {} creative tabs, {} problems", ItemSubtypeKeys.getInterpreters().size(), variantItems, tabs, problems.size());
		if (!problems.isEmpty()) {
			helper.fail(problems.size() + " subtype problems: " + String.join("; ", problems));
		}
		helper.succeed();
	}
}
