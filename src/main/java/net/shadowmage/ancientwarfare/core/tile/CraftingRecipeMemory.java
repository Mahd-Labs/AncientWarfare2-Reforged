package net.shadowmage.ancientwarfare.core.tile;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.shadowmage.ancientwarfare.core.crafting.AWCraftingManager;
import net.shadowmage.ancientwarfare.core.crafting.ICraftingRecipe;
import net.shadowmage.ancientwarfare.core.crafting.RecipeResourceLocation;
import net.shadowmage.ancientwarfare.core.crafting.wrappers.NoRecipeWrapper;
import net.shadowmage.ancientwarfare.core.item.ItemResearchBook;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;

import javax.annotation.Nullable;
import java.util.List;

public class CraftingRecipeMemory {
	private final BlockEntity tileEntity;
	private ICraftingRecipe recipe = NoRecipeWrapper.INSTANCE;

	public ItemStackHandler bookSlot = new ItemStackHandler(1) {
		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return ItemResearchBook.getResearcherName(stack) != null ? super.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		protected void onContentsChanged(int slot) {
			tileEntity.setChanged();
		}
	};
	public ResultContainer outputSlot = new ResultContainer();

	public CraftingContainer craftMatrix = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
		@Override
		public ItemStack quickMoveStack(Player player, int index) {
			return ItemStack.EMPTY;
		}

		@Override
		public boolean stillValid(Player playerIn) {
			return true;
		}
	}, 3, 3) {
		@Override
		public void setChanged() {
			super.setChanged();
			tileEntity.setChanged();
			updateOutput(this);
		}
	};//the 3x3 recipe template/matrix

	public List<ItemStack> getCraftingStacks() {
		List<ItemStack> ret = NonNullList.create();
		for (int slot = 0; slot < craftMatrix.getContainerSize(); slot++) {
			ret.add(craftMatrix.getItem(slot));
		}
		return ret;
	}

	public CraftingRecipeMemory(BlockEntity tileEntity) {
		this.tileEntity = tileEntity;
	}

	public void dropInventory() {
		InventoryTools.dropItemsInWorld(tileEntity.getLevel(), bookSlot, tileEntity.getBlockPos());
		InventoryTools.dropItemsInWorld(tileEntity.getLevel(), craftMatrix, tileEntity.getBlockPos());
	}

	@Nullable
	public String getCrafterName() {
		return ItemResearchBook.getResearcherName(bookSlot.getStackInSlot(0));
	}

	public void readFromNBT(CompoundTag tag) {
		NBTHelper.deserializeHandler(bookSlot, tag.getCompound("bookSlot"));
		InventoryTools.readInventoryFromNBT(outputSlot, tag.getCompound("outputSlot"));
		InventoryTools.readInventoryFromNBT(craftMatrix, tag.getCompound("craftMatrix"));
		recipe = AWCraftingManager.getRecipe(tileEntity.getLevel(), RecipeResourceLocation.deserialize(tag.getString("recipe")));
	}

	public CompoundTag writeToNBT(CompoundTag tag) {
		tag.put("bookSlot", NBTHelper.serializeHandler(bookSlot));
		tag.put("outputSlot", InventoryTools.writeInventoryToNBT(outputSlot, new CompoundTag()));
		tag.put("craftMatrix", InventoryTools.writeInventoryToNBT(craftMatrix, new CompoundTag()));
		tag.putString("recipe", recipe.getRegistryName().toString());
		return tag;
	}

	public void setRecipe(ICraftingRecipe recipe) {
		this.recipe = recipe;
		updateOutput(craftMatrix);
	}

	private void updateOutput(CraftingContainer craftingMatrix) {
		outputSlot.setItem(0, recipe.getCraftingResult(craftingMatrix));
	}

	public ICraftingRecipe getRecipe() {
		return recipe;
	}

	public ItemStack getCraftingResult(CraftingContainer invCrafting) {
		return recipe.getCraftingResult(invCrafting);
	}

	public NonNullList<ItemStack> getRemainingItems(CraftingContainer invCrafting) {
		return recipe.getRemainingItems(invCrafting);
	}
}
