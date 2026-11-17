package net.shadowmage.ancientwarfare.core.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import net.shadowmage.ancientwarfare.core.crafting.ICraftingRecipe;
import net.shadowmage.ancientwarfare.core.crafting.IngredientCount;
import net.shadowmage.ancientwarfare.core.tile.CraftingRecipeMemory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Port notes: 1.12 names -> 1.21.1: isItemValid -> mayPlace, decrStackSize -> remove, onCrafting(stack, amount) ->
 * onQuickCraft, onCrafting(stack) -> checkTakeAchievements, onTake returns nothing now.
 */
public class SlotResearchCrafting extends Slot {
	private final CraftingContainer craftMatrix;
	private final Player player;
	private int amountCrafted;
	private CraftingRecipeMemory craftingRecipeMemory;

	public SlotResearchCrafting(Player player, CraftingRecipeMemory craftingRecipeMemory, CraftingContainer craftingInventory, Container inventory, int slotIndex, int xPosition, int yPosition) {
		super(inventory, slotIndex, xPosition, yPosition);
		this.player = player;
		this.craftMatrix = craftingInventory;
		this.craftingRecipeMemory = craftingRecipeMemory;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return false;
	}

	@Override
	public ItemStack remove(int amount) {
		if (hasItem()) {
			amountCrafted += Math.min(amount, getItem().getCount());
		}

		return super.remove(amount);
	}

	@Override
	protected void onQuickCraft(ItemStack stack, int amount) {
		amountCrafted += amount;
		checkTakeAchievements(stack);
	}

	@Override
	protected void onSwapCraft(int p_190900_1_) {
		amountCrafted += p_190900_1_;
	}

	@Override
	protected void checkTakeAchievements(ItemStack stack) {
		ResultContainer inventorycraftresult = (ResultContainer) container;
		RecipeHolder<?> irecipe = inventorycraftresult.getRecipeUsed();
		if (amountCrafted > 0) {
			stack.onCraftedBy(player.level(), player, amountCrafted);
			if (irecipe != null) {
				EventHooks.firePlayerCraftingEvent(player, stack, craftMatrix);
			}
		}

		amountCrafted = 0;

		if (irecipe != null && !irecipe.value().isSpecial()) {
			player.awardRecipes(Collections.singleton(irecipe));
			inventorycraftresult.setRecipeUsed(null);
		}
	}

	@Override
	public void onTake(Player thePlayer, ItemStack stack) {
		checkTakeAchievements(stack);
		ICraftingRecipe recipe = craftingRecipeMemory.getRecipe();
		CommonHooks.setCraftingPlayer(thePlayer);
		NonNullList<ItemStack> nonnulllist = recipe.getRemainingItems(craftMatrix);
		CommonHooks.setCraftingPlayer(null);

		List<Integer> usedIngredients = new ArrayList<>();

		for (int i = 0; i < nonnulllist.size(); ++i) {
			ItemStack itemstack = this.craftMatrix.getItem(i);
			ItemStack itemstack1 = nonnulllist.get(i);

			if (!itemstack.isEmpty()) {
				NonNullList<Ingredient> ingredients = recipe.getIngredients();
				ItemStack finalStack = itemstack;
				Ingredient ingredient = ingredients.stream().filter(in -> !usedIngredients.contains(ingredients.indexOf(in)) && in.test(finalStack)).findFirst().orElse(Ingredient.EMPTY);
				usedIngredients.add(ingredients.indexOf(ingredient));

				craftMatrix.removeItem(i, IngredientCount.getCount(ingredient));
				itemstack = this.craftMatrix.getItem(i);
			}

			if (!itemstack1.isEmpty()) {
				if (itemstack.isEmpty()) {
					this.craftMatrix.setItem(i, itemstack1);
				} else if (ItemStack.isSameItemSameComponents(itemstack, itemstack1)) {
					itemstack1.grow(itemstack.getCount());
					this.craftMatrix.setItem(i, itemstack1);
				} else if (!this.player.getInventory().add(itemstack1)) {
					this.player.drop(itemstack1, false);
				}
			}
		}
	}
}
