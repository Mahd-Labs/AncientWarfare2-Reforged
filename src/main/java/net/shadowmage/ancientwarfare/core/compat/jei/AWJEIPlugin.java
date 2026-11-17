package net.shadowmage.ancientwarfare.core.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.automation.container.ContainerWarehouseCraftingStation;
import net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteAutoCrafting;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;
import net.shadowmage.ancientwarfare.core.compat.ItemSubtypeKeys;
import net.shadowmage.ancientwarfare.core.container.ContainerEngineeringStation;
import net.shadowmage.ancientwarfare.core.container.ICraftingContainer;
import net.shadowmage.ancientwarfare.core.crafting.AWCraftingManager;
import net.shadowmage.ancientwarfare.core.crafting.ResearchRecipeBase;
import net.shadowmage.ancientwarfare.core.crafting.ShapedResearchRecipe;
import net.shadowmage.ancientwarfare.core.crafting.ShapelessResearchRecipe;
import net.shadowmage.ancientwarfare.core.init.AWCoreBlocks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/*
 * Port notes (1.12 AWJEIPlugin, JEI 4 -> JEI 19): loaded only by JEI (@JeiPlugin); no other class of the mod names a
 * JEI class (the releaseJar task checks it), so the mod runs without JEI.
 * - Subtypes: one interpreter per item of ItemSubtypeKeys (the identifying custom-data keys of each item with data
 *   variants; 1.12: NPC spawner, fire pit, coin, totem part by NBT, vehicle spawner by metadata + NBT).
 * - Recipes: the research recipes (AWCraftingManager.getRecipes, the research-type recipes that currently are research
 *   recipes) in the shaped / shapeless research categories; research-type recipes that the research settings make
 *   plain recipes (1.12 registered those as normal crafting recipes, JEI showed them under crafting) are shown in JEI's
 *   crafting category, as vanilla shaped / shapeless recipes with the same id. Read from the client's recipe manager
 *   (synced by the server) when JEI registers recipes.
 * - Catalysts and transfer handlers: auto crafting worksite, warehouse crafting station and engineering station for
 *   both research categories and for crafting, as 1.12.
 */
@JeiPlugin
public class AWJEIPlugin implements IModPlugin {
	private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(AncientWarfare.MOD_ID, "jei_plugin");

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registration) {
		for (Map.Entry<net.minecraft.world.item.Item, Function<ItemStack, String>> entry : ItemSubtypeKeys.getInterpreters().entrySet()) {
			Function<ItemStack, String> function = entry.getValue();
			registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, entry.getKey(), new ISubtypeInterpreter<ItemStack>() {
				@Nullable
				@Override
				public Object getSubtypeData(ItemStack stack, UidContext context) {
					String key = function.apply(stack);
					return key.isEmpty() ? null : key;
				}

				@Override
				public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
					return function.apply(stack);
				}
			});
		}
		AncientWarfare.LOG.info("JEI: registered {} item subtype interpreters", ItemSubtypeKeys.getInterpreters().size());
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

		registration.addRecipeCategories(new ShapedResearchRecipeCategory(guiHelper));
		registration.addRecipeCategories(new ShapelessResearchRecipeCategory(guiHelper));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		Level level = Minecraft.getInstance().level;
		List<ShapedResearchRecipe> shaped = new ArrayList<>();
		List<ShapelessResearchRecipe> shapeless = new ArrayList<>();
		for (ResearchRecipeBase recipe : AWCraftingManager.getRecipes(level)) {
			if (recipe instanceof ShapedResearchRecipe shapedRecipe) {
				shaped.add(shapedRecipe);
			} else if (recipe instanceof ShapelessResearchRecipe shapelessRecipe) {
				shapeless.add(shapelessRecipe);
			}
		}
		registration.addRecipes(ShapedResearchRecipeCategory.TYPE, shaped);
		registration.addRecipes(ShapelessResearchRecipeCategory.TYPE, shapeless);

		List<RecipeHolder<CraftingRecipe>> regular = new ArrayList<>();
		for (ResearchRecipeBase recipe : AWCraftingManager.getRegularRecipes(level)) {
			CraftingRecipe crafting;
			if (recipe instanceof ShapedResearchRecipe shapedRecipe) {
				crafting = new ShapedRecipe("", CraftingBookCategory.MISC, new ShapedRecipePattern(shapedRecipe.getRecipeWidth(), shapedRecipe.getRecipeHeight(), recipe.getIngredients(), Optional.empty()), recipe.getRecipeOutput());
			} else {
				crafting = new ShapelessRecipe("", CraftingBookCategory.MISC, recipe.getRecipeOutput(), recipe.getIngredients());
			}
			regular.add(new RecipeHolder<>(recipe.getRegistryName(), crafting));
		}
		registration.addRecipes(RecipeTypes.CRAFTING, regular);
		AncientWarfare.LOG.info("JEI: registered {} shaped and {} shapeless research recipes, {} research-type recipes as crafting recipes", shaped.size(), shapeless.size(), regular.size());
	}

	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
		IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
		for (RecipeType<?> type : List.of(ShapedResearchRecipeCategory.TYPE, ShapelessResearchRecipeCategory.TYPE, RecipeTypes.CRAFTING)) {
			register(registration, ContainerWorksiteAutoCrafting.class, type, helper);
			register(registration, ContainerWarehouseCraftingStation.class, type, helper);
			register(registration, ContainerEngineeringStation.class, type, helper);
		}
	}

	private static <C extends AbstractContainerMenu & ICraftingContainer, R> void register(IRecipeTransferRegistration registration, Class<C> containerClass, RecipeType<R> type, IRecipeTransferHandlerHelper helper) {
		registration.addRecipeTransferHandler(new MultiRecipeTransferHandler<>(containerClass, type, helper), type);
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		for (RecipeType<?> type : List.of(ShapedResearchRecipeCategory.TYPE, ShapelessResearchRecipeCategory.TYPE, RecipeTypes.CRAFTING)) {
			registration.addRecipeCatalysts(type, AWAutomationBlocks.AUTO_CRAFTING.get(), AWAutomationBlocks.WAREHOUSE_CRAFTING.get(), AWCoreBlocks.ENGINEERING_STATION.get());
		}
	}
}
