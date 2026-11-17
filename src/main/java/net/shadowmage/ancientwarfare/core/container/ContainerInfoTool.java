package net.shadowmage.ancientwarfare.core.container;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.init.AWCoreItems;
import net.shadowmage.ancientwarfare.core.util.EntityTools;

public class ContainerInfoTool extends ContainerBase {
	private final ItemStack infoTool;

	@SuppressWarnings("unused") //parameters used in reflection
	public ContainerInfoTool(Player player, int x, int y, int z) {
		super(player);
		InteractionHand hand = EntityTools.getHandHoldingItem(player, AWCoreItems.INFO_TOOL.get());
		infoTool = hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
		addPlayerSlots(8);
	}

	public void printItemInfo(int slotId) {
		if (slotId >= 0 && slotId < slots.size()) {
			ItemStack stack = slots.get(slotId).getItem();
			if (!stack.isEmpty()) {
				AWCoreItems.INFO_TOOL.get().printItemInfo(player, infoTool, stack);
			}
		}
	}
}
