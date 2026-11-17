package net.shadowmage.ancientwarfare.core.util;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.shadowmage.ancientwarfare.AncientWarfare;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class ItemWrapper {
	public final Item item;
	public final int damage;

	public ItemWrapper(Item item, int damage) {
		this.item = item;
		this.damage = damage;
	}

	public static ArrayList<ItemWrapper> buildList(String listName, String[] input) {
		ArrayList<ItemWrapper> outputList = new ArrayList<>();

		AncientWarfare.LOG.info("Building " + listName + "...");

		for (String itemName : input) {
			itemName = itemName.trim();
			if (!itemName.equals("")) {
				String[] itemId = itemName.split(":");
				if (Array.getLength(itemId) != 2 && Array.getLength(itemId) != 3) {
					AncientWarfare.LOG.warn(" - Invalid item (bad length of " + Array.getLength(itemId) + "): " + itemId);
					continue;
				}
				if (itemId[0] == null || itemId[1] == null) {
					AncientWarfare.LOG.warn(" - Invalid block (parse/format error): " + itemId);
					continue;
				}

				//1.12 new ResourceLocation(..) accepted any text; ResourceLocation.parse throws on characters that are not valid in an id
				ResourceLocation id = ResourceLocation.tryParse(itemId[0] + ":" + itemId[1]);
				Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);

				if (item == null) {
					AncientWarfare.LOG.warn(" - Skipping missing item: " + itemName);
					continue;
				}
				short damage = -1;
				if (Array.getLength(itemId) == 3) {
					try {
						damage = Short.parseShort(itemId[2]);
					}
					catch (NumberFormatException e) {
						AncientWarfare.LOG.warn(" - Damage value invalid : '" + itemId[2] + "', must be a number between 0 and " + Short.MAX_VALUE);
						continue;
					}
				}
				outputList.add(new ItemWrapper(item, damage));
			}
		}

		AncientWarfare.LOG.info("...added " + outputList.size() + " items to " + listName);

		return outputList;
	}
}
