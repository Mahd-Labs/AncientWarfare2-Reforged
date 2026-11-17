package net.shadowmage.ancientwarfare.core.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/*
 * Port notes: the 1.12 ore dictionary ingredient with a count. Ore names are item tags now
 * ("type": "ancientwarfare:tag_count", "tag": "c:rods/wooden", "count": n); see IngredientCount for the general notes.
 */
public class IngredientOreCount implements ICustomIngredient, IIngredientCount {
	public static final MapCodec<IngredientOreCount> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(i -> i.tag),
			Codec.INT.optionalFieldOf("count", 1).forGetter(i -> i.count)
	).apply(instance, IngredientOreCount::new));

	private final TagKey<Item> tag;
	private final int count;

	public IngredientOreCount(String ore, int count) {
		this(TagKey.create(Registries.ITEM, ResourceLocation.parse(ore)), count);
	}

	public IngredientOreCount(TagKey<Item> tag, int count) {
		this.tag = tag;
		this.count = count;
	}

	public ItemStack[] getMatchingStacks() {
		//not cached: tags are reloaded with the datapacks
		List<ItemStack> matchingStacks = new ArrayList<>();
		for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
			matchingStacks.add(new ItemStack(holder, count));
		}
		return matchingStacks.toArray(new ItemStack[0]);
	}

	@Override
	public Stream<ItemStack> getItems() {
		return Stream.of(getMatchingStacks());
	}

	@Override
	public boolean test(ItemStack input) {
		return input != null && input.is(tag) && input.getCount() >= count;
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public IngredientType<?> getType() {
		return AWCraftingManager.TAG_COUNT.get();
	}

	@Override
	public int getCount() {
		return count;
	}
}
