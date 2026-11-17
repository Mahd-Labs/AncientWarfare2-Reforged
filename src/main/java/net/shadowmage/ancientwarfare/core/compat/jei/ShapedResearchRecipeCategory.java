package net.shadowmage.ancientwarfare.core.compat.jei;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.crafting.ShapedResearchRecipe;

/*
 * Port notes: 1.12 ShapedResearchRecipeCategory (uid "shaped_research_recipe") + ShapedResearchRecipeWrapper; the JEI 19
 * recipe type is "ancientwarfare:shaped_research_recipe".
 */
public class ShapedResearchRecipeCategory extends ResearchRecipeCategory<ShapedResearchRecipe> {
	public static final RecipeType<ShapedResearchRecipe> TYPE = RecipeType.create(AncientWarfare.MOD_ID, "shaped_research_recipe", ShapedResearchRecipe.class);

	public ShapedResearchRecipeCategory(IGuiHelper guiHelper) {
		super(TYPE, Component.translatable("jei.recipe.research_recipe"), guiHelper);
	}

	@Override
	protected int getRecipeWidth(ShapedResearchRecipe recipe) {
		return recipe.getRecipeWidth();
	}

	@Override
	protected int getRecipeHeight(ShapedResearchRecipe recipe) {
		return recipe.getRecipeHeight();
	}
}
