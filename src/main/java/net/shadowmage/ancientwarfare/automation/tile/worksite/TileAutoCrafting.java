package net.shadowmage.ancientwarfare.automation.tile.worksite;

import net.neoforged.neoforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.automation.tile.ISidedItemHandlerTile;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.minecraft.core.BlockPos;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.shadowmage.ancientwarfare.core.crafting.AWCraftingManager;
import net.shadowmage.ancientwarfare.core.interfaces.IWorkSite;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.tile.CraftingRecipeMemory;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class TileAutoCrafting extends TileWorksiteBase implements ISidedItemHandlerTile {

	public TileAutoCrafting(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.AUTO_CRAFTING_TILE.get(), pos, state);
	}

	public CraftingRecipeMemory craftingRecipeMemory = new CraftingRecipeMemory(this);
	public ItemStackHandler outputInventory = new ItemStackHandler(9) {
		@Override
		protected void onContentsChanged(int slot) {
			markDirty();
		}
	};
	public ItemStackHandler resourceInventory = new ItemStackHandler(18) {
		@Override
		protected void onContentsChanged(int slot) {
			markDirty();
		}
	};

	private boolean canHoldLastCheck = false;

	@Override
	public void onBlockBroken(BlockState state) {
		craftingRecipeMemory.dropInventory();
		InventoryTools.dropItemsInWorld(level, outputInventory, worldPosition);
		InventoryTools.dropItemsInWorld(level, resourceInventory, worldPosition);
		super.onBlockBroken(state);
	}

	public boolean tryCraftItem() {
		if (canHold()) {
			NonNullList<ItemStack> reusableStacks = AWCraftingManager.getReusableStacks(craftingRecipeMemory.getRecipe(), craftingRecipeMemory.craftMatrix);
			NonNullList<ItemStack> resources = InventoryTools.removeItems(AWCraftingManager.getRecipeInventoryMatch(craftingRecipeMemory.getRecipe(), craftingRecipeMemory.getCraftingStacks(),
					new CombinedInvWrapper(resourceInventory, new ItemStackHandler(reusableStacks))), reusableStacks);
			if (!resources.isEmpty()) {
				craftItem(resources, reusableStacks);
				return true;
			}
		}
		return false;
	}

	private void craftItem(NonNullList<ItemStack> resources, NonNullList<ItemStack> reusableStacks) {
		CraftingContainer invCrafting = AWCraftingManager.fillCraftingMatrixFromInventory(resources);
		ItemStack result = craftingRecipeMemory.getCraftingResult(invCrafting);
		InventoryTools.removeItems(resourceInventory, resources);
		NonNullList<ItemStack> remainingItems = InventoryTools.removeItems(craftingRecipeMemory.getRemainingItems(invCrafting), reusableStacks);

		for (ItemStack stack : remainingItems) {
			if (stack.isEmpty()) {
				continue;
			}

			if (!InventoryTools.removeItem(resources, is -> ItemStack.matches(stack, is), stack.getCount(), true).isEmpty()) {
				InventoryTools.insertOrDropItem(resourceInventory, stack, level, worldPosition);
			} else {
				InventoryTools.insertOrDropItem(outputInventory, stack, level, worldPosition);
			}
		}

		InventoryTools.insertOrDropItem(outputInventory, result, level, worldPosition);
	}

	@Override
	public WorkType getWorkType() {
		return WorkType.CRAFTING;
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		craftingRecipeMemory.readFromNBT(tag);
		NBTHelper.deserializeHandler(resourceInventory, tag.getCompound("resourceInventory"));
		NBTHelper.deserializeHandler(outputInventory, tag.getCompound("outputInventory"));
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		craftingRecipeMemory.writeToNBT(tag);
		tag.put("resourceInventory", NBTHelper.serializeHandler(resourceInventory));
		tag.put("outputInventory", NBTHelper.serializeHandler(outputInventory));
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		craftingRecipeMemory.writeToNBT(tag);
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		craftingRecipeMemory.readFromNBT(tag);
	}

	/* ***********************************INVENTORY METHODS*********************************************** */
	@Override
	public boolean onBlockClicked(Player player, @Nullable InteractionHand hand) {
		if (!player.level().isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WORKSITE_AUTO_CRAFT, worldPosition);
		}
		return true;
	}

	private static final CraftAction CRAFT_ACTION = new CraftAction();

	private static class CraftAction implements IWorksiteAction {
		@Override
		public double getEnergyConsumed(double efficiencyBonusFactor) {
			return IWorkSite.WorksiteImplementation.getEnergyPerActivation(efficiencyBonusFactor);
		}
	}

	@Override
	protected Optional<IWorksiteAction> getNextAction() {
		return canHoldLastCheck && !craftingRecipeMemory.getRecipe().getRecipeOutput().isEmpty() ? Optional.of(CRAFT_ACTION) : Optional.empty();
	}

	@Override
	protected boolean processAction(IWorksiteAction action) {
		return tryCraftItem();
	}

	@Override
	protected void updateWorksite() {
		canHoldLastCheck = canHold();
	}

	private boolean canHold() {
		ItemStack test = craftingRecipeMemory.getRecipe().getRecipeOutput();
		return !test.isEmpty() && InventoryTools.canInventoryHold(outputInventory, test);
	}

	@Nullable
	@Override
	public IItemHandler getItemHandler(@Nullable Direction facing) {
		if (facing != null) {
			if (facing == Direction.DOWN) {
				return outputInventory;
			} else {
				return resourceInventory;
			}
		}

		return null;
	}
}
