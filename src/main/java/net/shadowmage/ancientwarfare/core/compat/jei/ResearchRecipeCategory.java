package net.shadowmage.ancientwarfare.core.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.placement.VerticalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.crafting.ResearchRecipeBase;
import net.shadowmage.ancientwarfare.core.init.AWCoreItems;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;

import java.util.List;

/*
 * Port notes: 1.12 ResearchRecipeCategory + ResearchRecipeWrapper (JEI 4 split layout and recipe wrapper; JEI 19 has the
 * category do both). Same layout as 1.12: JEI's vanilla crafting layout (116 x 54, 3x3 grid, arrow, output slot), the
 * research the recipe needs drawn at (60, 0) in 0x444444 ("Research disabled" while the research system is off).
 * Icon: the research book (1.12 drew its texture; JEI 19 draws the item).
 */
public abstract class ResearchRecipeCategory<T extends ResearchRecipeBase> extends AbstractRecipeCategory<T> {
	private static final int WIDTH = 116;
	private static final int HEIGHT = 54;
	private final ICraftingGridHelper craftingGridHelper;

	protected ResearchRecipeCategory(RecipeType<T> recipeType, Component title, IGuiHelper guiHelper) {
		super(recipeType, title, guiHelper.createDrawableItemLike(AWCoreItems.RESEARCH_BOOK.get()), WIDTH, HEIGHT);
		craftingGridHelper = guiHelper.createCraftingGridHelper();
	}

	/*
	 * recipe width / height of a shaped recipe, 0 / 0 for a shapeless one
	 */
	protected abstract int getRecipeWidth(T recipe);

	protected abstract int getRecipeHeight(T recipe);

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
		craftingGridHelper.createAndSetIngredients(builder, recipe.getIngredients(), getRecipeWidth(recipe), getRecipeHeight(recipe));
		craftingGridHelper.createAndSetOutputs(builder, List.of(recipe.getRecipeOutput()));
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, T recipe, IFocusGroup focuses) {
		builder.addRecipeArrowWidget().setPosition(61, 0, 55, HEIGHT, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
	}

	@Override
	public void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
		String research = AWCoreStatics.useResearchSystem ? I18n.get(ResearchGoal.getUnlocalizedName(recipe.getNeededResearch())) : "Research disabled";
		guiGraphics.drawString(Minecraft.getInstance().font, research, 60, 0, 0xFF444444, false);
	}

	@Override
	public ResourceLocation getRegistryName(T recipe) {
		return recipe.getRegistryName();
	}
}
