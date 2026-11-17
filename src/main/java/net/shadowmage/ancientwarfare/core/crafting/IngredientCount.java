package net.shadowmage.ancientwarfare.core.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.stream.Stream;

/*
 * Port notes: 1.12 subclassed Ingredient; in 1.21.1 Ingredient is final and this is a NeoForge custom ingredient
 * ("type": "ancientwarfare:item_count", "item": "<id>", "count": n). Use toVanilla() to get the Ingredient and
 * IngredientCount.getCount(ingredient) instead of the 1.12 "ingredient instanceof IIngredientCount" check.
 * Like the 1.12 item ingredient it matches the item only (no component comparison).
 */
public class IngredientCount implements ICustomIngredient, IIngredientCount {
	public static final MapCodec<IngredientCount> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(i -> i.item),
			Codec.INT.optionalFieldOf("count", 1).forGetter(i -> i.matchCount)
	).apply(instance, IngredientCount::new));

	private final Item item;
	private final int matchCount;
	private final int count;
	private ItemStack[] array = null;

	public IngredientCount(ItemStack stack) {
		this(stack.getItem(), stack.getCount());
	}

	public IngredientCount(Item item, int count) {
		this.item = item;
		this.matchCount = count;
		this.count = count;
	}

	/*
	 * 1.12 used an anonymous subclass that matched stack.getCount() items but reported another count
	 */
	public IngredientCount(ItemStack stack, int reportedCount) {
		this.item = stack.getItem();
		this.matchCount = stack.getCount();
		this.count = reportedCount;
	}

	/*
	 * count of a (possibly counted) ingredient, 1 for every other ingredient
	 */
	public static int getCount(Ingredient ingredient) {
		ICustomIngredient custom = ingredient.getCustomIngredient();
		return custom instanceof IIngredientCount ? ((IIngredientCount) custom).getCount() : 1;
	}

	public ItemStack[] getMatchingStacks() {
		if (array == null) {
			array = new ItemStack[] {new ItemStack(item, matchCount)};
		}
		return array;
	}

	@Override
	public Stream<ItemStack> getItems() {
		return Stream.of(getMatchingStacks());
	}

	@Override
	public boolean test(ItemStack input) {
		return input != null && input.getItem() == item && input.getCount() >= matchCount;
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public IngredientType<?> getType() {
		return AWCraftingManager.ITEM_COUNT.get();
	}

	@Override
	public int getCount() {
		return count;
	}
}
