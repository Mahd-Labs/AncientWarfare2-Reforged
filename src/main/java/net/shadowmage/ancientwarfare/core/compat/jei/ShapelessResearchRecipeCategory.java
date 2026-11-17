package net.shadowmage.ancientwarfare.core.compat.jei;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.crafting.ShapelessResearchRecipe;

/*
 * Port notes: 1.12 ShapelessResearchRecipeCategory (uid "shapeless_research_recipe"); the JEI 19 recipe type is
 * "ancientwarfare:shapeless_research_recipe".
 */
public class ShapelessResearchRecipeCategory extends ResearchRecipeCategory<ShapelessResearchRecipe> {
	public static final RecipeType<ShapelessResearchRecipe> TYPE = RecipeType.create(AncientWarfare.MOD_ID, "shapeless_research_recipe", ShapelessResearchRecipe.class);

	public ShapelessResearchRecipeCategory(IGuiHelper guiHelper) {
		super(TYPE, Component.translatable("jei.recipe.shapeless_research_recipe"), guiHelper);
	}

	@Override
	protected int getRecipeWidth(ShapelessResearchRecipe recipe) {
		return 0;
	}

	@Override
	protected int getRecipeHeight(ShapelessResearchRecipe recipe) {
		return 0;
	}
}
