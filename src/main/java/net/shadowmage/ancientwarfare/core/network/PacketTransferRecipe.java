package net.shadowmage.ancientwarfare.core.network;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.container.ICraftingContainer;
import net.shadowmage.ancientwarfare.core.crafting.AWCraftingManager;
import net.shadowmage.ancientwarfare.core.crafting.ICraftingRecipe;
import net.shadowmage.ancientwarfare.core.crafting.RecipeResourceLocation;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/*
 * client -> server: JEI "+" button on the engineering station, auto crafting worksite or warehouse crafting station
 * (sent by core/compat/jei/MultiRecipeTransferHandler). Moves what is in the crafting grid back into the container's
 * inventories, takes the recipe's ingredients from them into the grid and selects that recipe in the recipe memory.
 *
 * Port notes: 1.12 core/compat/jei/PacketTransferRecipe. It lives outside the jei package because both sides must
 * know it whether or not JEI is installed (a client with JEI on a server without it); it references no JEI class.
 * JEI 19's own transfer packets cannot do this: the warehouse crafting station takes the items from the warehouse,
 * which has no slots in the container, and the recipe memory must select the transferred recipe (several recipes can
 * match one grid). Server-side checks (port addition, packet rules): the sender has one of those containers open and
 * may still use it, the recipe exists and is enabled, and with the research system on the research book in the
 * station has the research the recipe needs (what the client-side handler checks before sending).
 */
public class PacketTransferRecipe extends PacketBase {
	private static final int MAX_NAME_LENGTH = 1024;
	private RecipeResourceLocation recipeName = RecipeResourceLocation.NO_RECIPE_REGISTRY_NAME;

	public PacketTransferRecipe() {}

	public PacketTransferRecipe(ICraftingRecipe recipe) {
		this(recipe.getRegistryName());
	}

	public PacketTransferRecipe(RecipeResourceLocation recipeName) {
		this.recipeName = recipeName;
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		data.writeUtf(recipeName.toString(), MAX_NAME_LENGTH);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) throws IOException {
		String name = data.readUtf(MAX_NAME_LENGTH);
		try {
			recipeName = RecipeResourceLocation.deserialize(name);
		}
		catch (RuntimeException e) {
			recipeName = RecipeResourceLocation.NO_RECIPE_REGISTRY_NAME;
		}
	}

	@Override
	protected void execute(Player player) {
		if (player.level().isClientSide || !(player.containerMenu instanceof ICraftingContainer craftingContainer) || !player.containerMenu.stillValid(player)) {
			return;
		}
		ICraftingRecipe recipe = AWCraftingManager.getRecipe(player.level(), recipeName);
		if (!recipe.isValid()) {
			return;
		}
		Optional<String> research = recipe.getNeededResearch();
		if (AWCoreStatics.useResearchSystem && research.isPresent()) {
			String crafter = craftingContainer.getCraftingMemoryContainer().getCrafterName();
			if (crafter == null || !AWCraftingManager.canPlayerCraft(player.level(), crafter, research.get())) {
				return;
			}
		}

		if (craftingContainer.pushCraftingMatrixToInventories()) {
			IItemHandler handler = new CombinedInvWrapper(craftingContainer.getInventories());

			NonNullList<ItemStack> resources = AWCraftingManager.getRecipeInventoryMatch(recipe, handler);

			if (resources.isEmpty()) {
				return;
			}

			List<Slot> slots = craftingContainer.getCraftingMemoryContainer().getCraftingMatrixSlots();

			for (int x = 0; x < recipe.getRecipeWidth(); x++) {
				for (int y = 0; y < recipe.getRecipeHeight(); y++) {
					int slotIndex = y * 3 + x;
					int ingredientIndex = y * recipe.getRecipeWidth() + x;
					Slot slot = slots.get(slotIndex);
					slot.set(resources.get(ingredientIndex));
				}
			}
			craftingContainer.getCraftingMemoryContainer().setRecipe(recipe, true);
			craftingContainer.getCraftingMemoryContainer().setUpdatePending();
			craftingContainer.getCraftingMemoryContainer().updateClients();

			InventoryTools.removeItems(handler, resources);
		}
	}
}
