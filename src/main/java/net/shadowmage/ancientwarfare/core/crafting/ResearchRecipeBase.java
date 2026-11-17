package net.shadowmage.ancientwarfare.core.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.registry.ResearchRegistry;
import net.shadowmage.ancientwarfare.core.util.RegistryTools;

import javax.annotation.Nonnull;
import java.util.function.IntFunction;

/*
 * Port notes: a real 1.21.1 recipe (RecipeType "ancientwarfare:research") instead of an entry of the 1.12 research
 * recipe registry. The registry name is the id of the RecipeHolder; AWCraftingManager copies it into the recipe when
 * it hands recipes out, so getRegistryName() keeps working. getCraftingRecipe() (the Forge recipe a research recipe
 * was turned into when it needed no research) is replaced by ResearchFallbackRecipe.
 */
public abstract class ResearchRecipeBase implements Recipe<CraftingInput> {
	private String neededResearch;
	private ItemStack output;
	private NonNullList<Ingredient> input;
	private ResourceLocation registryName = RegistryTools.EMPTY_REGISTRY_NAME;

	ResearchRecipeBase(String research, NonNullList<Ingredient> input, ItemStack output) {
		addResearch(research);
		this.input = input;
		this.output = output;
	}

	public String getNeededResearch() {
		return neededResearch;
	}

	public ItemStack getCraftingResult() {
		return output.copy();
	}

	public ItemStack getRecipeOutput() {
		return output;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return input;
	}

	public ResourceLocation getRegistryName() {
		return registryName;
	}

	void setRegistryName(ResourceLocation registryName) {
		this.registryName = registryName;
	}

	private void addResearch(String name) {
		if (!ResearchRegistry.researchExists(name)) {
			throw new IllegalArgumentException("COULD NOT LOCATE RESEARCH GOAL FOR NAME: " + name);
		}
		neededResearch = name;
	}

	/*
	 * the 1.12 matches(InventoryCrafting, World): matches against the full crafting grid
	 */
	boolean matches(@Nonnull CraftingContainer inv, @Nonnull Level world) {
		return matchesGrid(inv.getWidth(), inv.getHeight(), inv::getItem);
	}

	/*
	 * @param stacks stack at index x + y * gridWidth
	 */
	abstract boolean matchesGrid(int gridWidth, int gridHeight, IntFunction<ItemStack> stacks);

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return matchesGrid(input.width(), input.height(), input::getItem);
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		return getCraftingResult();
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return output;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeType<?> getType() {
		return AWCraftingManager.RESEARCH_RECIPE_TYPE.get();
	}
}
