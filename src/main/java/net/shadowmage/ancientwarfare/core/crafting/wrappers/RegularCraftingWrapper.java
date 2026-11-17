package net.shadowmage.ancientwarfare.core.crafting.wrappers;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.shadowmage.ancientwarfare.core.crafting.ICraftingRecipe;
import net.shadowmage.ancientwarfare.core.crafting.RecipeResourceLocation;
import net.shadowmage.ancientwarfare.core.crafting.ShapedResearchRecipe;
import net.shadowmage.ancientwarfare.core.util.RegistryAccessHelper;

import java.util.Optional;

/*
 * Port notes: wraps a RecipeHolder (the registry name is the holder id). Vanilla recipes work on the trimmed
 * CraftingInput; getRemainingItems maps its result back to the slots of the full crafting grid like 1.12.
 */
public class RegularCraftingWrapper implements ICraftingRecipe {
	private final Recipe<CraftingInput> recipe;
	private final RecipeResourceLocation registryName;

	public RegularCraftingWrapper(RecipeHolder<? extends Recipe<CraftingInput>> recipe) {
		this.recipe = recipe.value();
		if (recipe.id() == null) {
			throw new IllegalArgumentException("Null registryName recipes are not allowed here");
		}
		registryName = new RecipeResourceLocation(RecipeResourceLocation.RecipeType.REGULAR, recipe.id());
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return recipe.getIngredients();
	}

	@Override
	public ItemStack getCraftingResult(CraftingContainer inv) {
		return recipe.assemble(inv.asCraftInput(), RegistryAccessHelper.get());
	}

	@Override
	public ItemStack getRecipeOutput() {
		return recipe.getResultItem(RegistryAccessHelper.get());
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingContainer invCrafting) {
		CraftingInput.Positioned positioned = invCrafting.asPositionedCraftInput();
		CraftingInput input = positioned.input();
		NonNullList<ItemStack> remaining = recipe.getRemainingItems(input);
		NonNullList<ItemStack> ret = NonNullList.withSize(invCrafting.getContainerSize(), ItemStack.EMPTY);
		for (int y = 0; y < input.height(); y++) {
			for (int x = 0; x < input.width(); x++) {
				int slot = x + positioned.left() + (y + positioned.top()) * invCrafting.getWidth();
				ret.set(slot, remaining.get(x + y * input.width()));
			}
		}
		return ret;
	}

	@Override
	public RecipeResourceLocation getRegistryName() {
		return registryName;
	}

	@Override
	public Optional<String> getNeededResearch() {
		return Optional.empty();
	}

	@Override
	public int getRecipeWidth() {
		if (recipe instanceof ShapedResearchRecipe) {
			return ((ShapedResearchRecipe) recipe).getRecipeWidth();
		}
		return recipe instanceof ShapedRecipe ? ((ShapedRecipe) recipe).getWidth() : 3;
	}

	@Override
	public int getRecipeHeight() {
		if (recipe instanceof ShapedResearchRecipe) {
			return ((ShapedResearchRecipe) recipe).getRecipeHeight();
		}
		return recipe instanceof ShapedRecipe ? ((ShapedRecipe) recipe).getHeight() : 3;
	}
}
