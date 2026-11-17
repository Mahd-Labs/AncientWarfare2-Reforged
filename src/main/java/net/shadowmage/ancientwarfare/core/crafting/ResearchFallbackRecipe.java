package net.shadowmage.ancientwarfare.core.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/*
 * Port addition. 1.12 registered a research recipe as a plain Forge crafting recipe when it needed no research
 * (research system disabled in the config, item configured as not researched, no counted ingredients). Recipes are
 * data in 1.21.1 and cannot change their type with the config, so this one special crafting recipe
 * (data/ancientwarfare/recipe/research_fallback.json) stands in for all of them: it matches whenever one of those
 * recipes matches the crafting grid and gives that recipe's result.
 */
public class ResearchFallbackRecipe extends CustomRecipe {
	public ResearchFallbackRecipe(CraftingBookCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return AWCraftingManager.findRegularResearchRecipe(input, level) != null;
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ResearchRecipeBase recipe = AWCraftingManager.findRegularResearchRecipe(input, null);
		return recipe == null ? ItemStack.EMPTY : recipe.getCraftingResult();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AWCraftingManager.RESEARCH_FALLBACK_SERIALIZER.get();
	}
}
