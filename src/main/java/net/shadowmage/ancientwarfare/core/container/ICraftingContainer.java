package net.shadowmage.ancientwarfare.core.container;

import net.neoforged.neoforge.items.IItemHandlerModifiable;

public interface ICraftingContainer {
	ContainerCraftingRecipeMemory getCraftingMemoryContainer();

	IItemHandlerModifiable[] getInventories();

	boolean pushCraftingMatrixToInventories();
}
