package net.shadowmage.ancientwarfare.core.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

/*
 * Port notes: json is { "type": "ancientwarfare:research_shapeless", "research": "<goal>", "ingredients": [...],
 * "result": {"id": ..., "count": n} } (1.12: "ancientwarfare:shapeless_research_recipe").
 */
public class ShapelessResearchRecipe extends ResearchRecipeBase {
	public ShapelessResearchRecipe(String research, NonNullList<Ingredient> ings, ItemStack result) {
		super(research, ings, result);
	}

	@Override
	boolean matchesGrid(int gridWidth, int gridHeight, IntFunction<ItemStack> stacks) {
		NonNullList<Ingredient> ingredients = getIngredients();
		List<Boolean> ingredientsMatched = new ArrayList<>(Collections.nCopies(ingredients.size(), false));

		for (int i = 0; i < gridWidth * gridHeight; ++i) {
			ItemStack itemstack = stacks.apply(i);
			if (itemstack.isEmpty()) {
				continue;
			}

			boolean matched = false;
			for (int j = 0; j < ingredients.size(); j++) {
				if (ingredients.get(j).test(itemstack)) {
					ingredientsMatched.set(j, true);
					matched = true;
					break;
				}
			}
			if (!matched) {
				return false;
			}
		}

		return !ingredientsMatched.contains(false);
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= getIngredients().size();
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AWCraftingManager.RESEARCH_SHAPELESS_SERIALIZER.get();
	}

	public static class Serializer implements RecipeSerializer<ShapelessResearchRecipe> {
		public static final MapCodec<ShapelessResearchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.STRING.fieldOf("research").forGetter(ResearchRecipeBase::getNeededResearch),
				Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(list -> {
					if (list.isEmpty()) {
						return DataResult.error(() -> "No ingredients for shapeless recipe");
					}
					NonNullList<Ingredient> ings = NonNullList.create();
					ings.addAll(list);
					return DataResult.success(ings);
				}, ings -> DataResult.success((List<Ingredient>) ings)).forGetter(ResearchRecipeBase::getIngredients),
				ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ResearchRecipeBase::getRecipeOutput)
		).apply(instance, ShapelessResearchRecipe::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessResearchRecipe> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, ResearchRecipeBase::getNeededResearch,
				Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).map(list -> {
					NonNullList<Ingredient> ings = NonNullList.create();
					ings.addAll(list);
					return ings;
				}, ings -> (List<Ingredient>) ings), ResearchRecipeBase::getIngredients,
				ItemStack.STREAM_CODEC, ResearchRecipeBase::getRecipeOutput,
				ShapelessResearchRecipe::new);

		@Override
		public MapCodec<ShapelessResearchRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ShapelessResearchRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
