package net.shadowmage.ancientwarfare.core.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.input.IItemKeyInterface;
import net.shadowmage.ancientwarfare.core.input.IItemKeyInterface.ItemAltFunction;
import net.shadowmage.ancientwarfare.core.input.IScrollableItem;

import javax.annotation.Nonnull;

public class PacketItemMouseScroll extends PacketBase {
	private boolean scrollUp;

	public PacketItemMouseScroll() {}

	public PacketItemMouseScroll(boolean scrollUp) {
		this.scrollUp = scrollUp;
	}

	@Override
	protected void writeToStream(RegistryFriendlyByteBuf data) {
		data.writeBoolean(scrollUp);
	}

	@Override
	protected void readFromStream(RegistryFriendlyByteBuf data) {
		scrollUp = data.readBoolean();
	}

	@Override
	protected void execute(Player player) {
		ItemStack stack = player.getMainHandItem();
		Item item = stack.getItem();
		if (item instanceof IScrollableItem){
			if (scrollUp) {
				((IScrollableItem) item).onScrollUp(player.level(), player, stack);
			} else {
				((IScrollableItem) item).onScrollDown(player.level(), player, stack);
			}
		}
	}
}
