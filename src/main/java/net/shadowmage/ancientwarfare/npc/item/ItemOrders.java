package net.shadowmage.ancientwarfare.npc.item;

import net.minecraft.world.item.Item;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.input.InputHandler;
import net.shadowmage.ancientwarfare.core.input.IItemKeyInterface;

import javax.annotation.Nullable;
import java.util.List;

public abstract class ItemOrders extends ItemBaseNPC implements IItemKeyInterface {

	public ItemOrders(String regName) {
		super(regName);
	}

	@Override
		public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		tooltip.add(Component.translatable("guistrings.npc.orders.open_gui"));
		Component key = net.shadowmage.ancientwarfare.core.item.ItemHammer.altItemUse1KeyName.get();
		tooltip.add(Component.translatable("guistrings.npc.orders.add_position", key));
	}

	@Override
	public boolean onKeyActionClient(Player player, ItemStack stack, ItemAltFunction altFunction) {
		return altFunction == ItemAltFunction.ALT_FUNCTION_1;
	}

	public abstract List<BlockPos> getPositionsForRender(ItemStack stack);

	public void addMessage(Player player) {
		player.sendSystemMessage(Component.translatable("guistrings.npc.orders.position_added"));
	}
}
