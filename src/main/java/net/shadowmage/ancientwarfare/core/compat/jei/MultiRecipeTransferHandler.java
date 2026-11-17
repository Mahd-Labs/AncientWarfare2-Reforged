package net.shadowmage.ancientwarfare.core.compat.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.container.ICraftingContainer;
import net.shadowmage.ancientwarfare.core.crafting.AWCraftingManager;
import net.shadowmage.ancientwarfare.core.crafting.ICraftingRecipe;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.PacketTransferRecipe;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * Port notes: 1.12 MultiRecipeTransferHandler, same checks and messages: finds the AW recipe matching the shown grid
 * (AWCraftingManager.findMatchingRecipe), refuses without a research book / without the research, when the grid's
 * current items do not fit into the inventories, and marks the missing ingredients; the transfer itself is
 * PacketTransferRecipe (core/network; see there why JEI's own slot transfer does not fit these containers).
 * JEI 19 hands the slot views instead of the 1.12 recipe layout: the 9 input slots of the grid in order (index =
 * crafting matrix index), then the output; missing ingredients are marked by slot view (1.12: by layout index).
 */
public class MultiRecipeTransferHandler<C extends AbstractContainerMenu & ICraftingContainer, R> implements IRecipeTransferHandler<C, R> {
	private final IRecipeTransferHandlerHelper handlerHelper;
	private final Class<C> containerClass;
	private final RecipeType<R> recipeType;

	public MultiRecipeTransferHandler(Class<C> containerClass, RecipeType<R> recipeType, IRecipeTransferHandlerHelper handlerHelper) {
		this.handlerHelper = handlerHelper;
		this.containerClass = containerClass;
		this.recipeType = recipeType;
	}

	@Override
	public Class<? extends C> getContainerClass() {
		return containerClass;
	}

	@Override
	public Optional<MenuType<C>> getMenuType() {
		return Optional.empty();
	}

	@Override
	public RecipeType<R> getRecipeType() {
		return recipeType;
	}

	//JEI 19.70 still calls this from the default transferRecipe(IRecipeTransferContext, boolean)
	@SuppressWarnings("removal")
	@Nullable
	@Override
	public IRecipeTransferError transferRecipe(C container, R jeiRecipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
		List<IRecipeSlotView> outputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT);
		ItemStack result = outputSlots.isEmpty() ? ItemStack.EMPTY : outputSlots.get(0).getDisplayedItemStack().orElse(ItemStack.EMPTY);
		List<IRecipeSlotView> inputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
		NonNullList<ItemStack> inputs = NonNullList.create();
		for (IRecipeSlotView slot : inputSlots) {
			inputs.add(slot.getItemStacks().findFirst().orElse(ItemStack.EMPTY));
		}
		Level level = player.level();
		ICraftingRecipe recipe = AWCraftingManager.findMatchingRecipe(level, inputs, result);
		if (!recipe.isValid()) {
			return handlerHelper.createInternalError();
		}

		if (AWCoreStatics.useResearchSystem && recipe.getNeededResearch().isPresent()) {
			if (container.getCraftingMemoryContainer().getCrafterName() == null) {
				return handlerHelper.createUserErrorWithTooltip(Component.translatable("jei.tooltip.error.recipe.transfer.no.research_book"));
			}

			if (!AWCraftingManager.canPlayerCraft(level, container.getCraftingMemoryContainer().getCrafterName(), recipe.getNeededResearch().get())) {
				return handlerHelper.createUserErrorWithTooltip(Component.translatable("jei.tooltip.error.recipe.transfer.missing.research"));
			}
		}

		List<Slot> craftingSlots = container.getCraftingMemoryContainer().getCraftingMatrixSlots();

		NonNullList<ItemStack> craftingMatrixStacks = NonNullList.create();
		for (Slot slot : craftingSlots) {
			final ItemStack stack = slot.getItem();
			if (!stack.isEmpty()) {
				craftingMatrixStacks.add(stack.copy());
			}
		}

		IItemHandlerModifiable inventories = new CombinedInvWrapper(container.getInventories());
		IItemHandlerModifiable allInventories = new CombinedInvWrapper(new ItemStackHandler(craftingMatrixStacks), inventories);

		// check if we have enough inventory space to put crafting slots into inventories
		if (!InventoryTools.insertItems(inventories, craftingMatrixStacks, true).isEmpty()) {
			return handlerHelper.createUserErrorWithTooltip(Component.translatable("jei.tooltip.error.recipe.transfer.inventory.full"));
		}
		List<Integer> missingItems = getMissingItems(inputs, recipe, allInventories);

		if (!missingItems.isEmpty()) {
			List<IRecipeSlotView> missingSlots = missingItems.stream().filter(i -> i < inputSlots.size()).map(inputSlots::get).collect(Collectors.toList());
			return handlerHelper.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"), missingSlots);
		}

		if (doTransfer) {
			NetworkHandler.sendToServer(new PacketTransferRecipe(recipe));
		}

		return null;
	}

	private List<Integer> getMissingItems(NonNullList<ItemStack> inputs, ICraftingRecipe recipe, IItemHandlerModifiable allInventories) {
		return AWCraftingManager.getRecipeInventoryMatch(recipe, inputs, s -> InventoryTools.hasCountOrMore(allInventories, s), allInventories,
				ArrayList::new, (a, i, s) -> {}, (a, in) -> addMissingItem(a, in, inputs));
	}

	private void addMissingItem(List<Integer> missingItems, Ingredient missingIngredient, NonNullList<ItemStack> inputs) {
		List<ItemStack> matchingStacks = inputs.stream().filter(s -> missingIngredient.test(s) && !missingItems.contains(inputs.indexOf(s))).collect(Collectors.toList());
		if (!matchingStacks.isEmpty()) {
			ItemStack matched = matchingStacks.get(matchingStacks.size() - 1);
			for (int i = inputs.size() - 1; i >= 0; i--) {
				if (!missingItems.contains(i) && inputs.get(i) == matched) {
					missingItems.add(i);
				}
			}
		}
	}
}
