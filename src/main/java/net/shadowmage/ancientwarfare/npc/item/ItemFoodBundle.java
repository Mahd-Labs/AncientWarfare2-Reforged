package net.shadowmage.ancientwarfare.npc.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

/*
 * Port notes: 1.12 ItemFood(15, 1, true) -> food component (nutrition 15, saturation modifier 1). The "wolf food"
 * flag has no 1.21.1 equivalent on the item (wolf food is the item tag minecraft:wolf_food).
 */
public class ItemFoodBundle extends Item {
	public ItemFoodBundle() {
		super(new Item.Properties().food(new FoodProperties.Builder().nutrition(15).saturationModifier(1F).build()));
	}

	@Override
	public String getDescriptionId() {
		return "item.food_bundle.name";
	}
}
