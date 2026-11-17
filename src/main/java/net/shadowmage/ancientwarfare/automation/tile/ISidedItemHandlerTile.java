package net.shadowmage.ancientwarfare.automation.tile;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/*
 * Port addition: replaces the 1.12 TileEntity.getCapability(ITEM_HANDLER_CAPABILITY, facing) overrides. Tiles that
 * exposed an item handler implement this with the exact 1.12 logic; AWAutomationBlocks registers it as
 * Capabilities.ItemHandler.BLOCK for their block entity types.
 */
public interface ISidedItemHandlerTile {
	@Nullable
	IItemHandler getItemHandler(@Nullable Direction side);
}
