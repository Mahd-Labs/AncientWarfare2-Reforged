package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;

import java.util.List;

/*
 * Port notes: the four 1.12 meta sub items are flattened into backpack_0 .. backpack_3 (AWCoreItems.BACKPACKS);
 * getSize() is the 1.12 item damage (0-3). The item handler capability is registered in AWCoreItems.
 */
public class ItemBackpack extends ItemBaseCore {
	private final int size;

	public ItemBackpack(int size) {
		super("backpack", new Item.Properties().stacksTo(1));
		this.size = size;
	}

	/*
	 * the 1.12 meta value: 0-3, the backpack has (size + 1) * 9 slots
	 */
	public int getSize() {
		return size;
	}

	public static int getSize(ItemStack stack) {
		return stack.getItem() instanceof ItemBackpack ? ((ItemBackpack) stack.getItem()).getSize() : 0;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		tooltip.add(Component.translatable("guistrings.core.backpack.size", ((size + 1) * 9)));
		tooltip.add(Component.translatable("guistrings.core.backpack.click_to_open"));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		if (!world.isClientSide)
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_BACKPACK, 0, 0, 0);
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
	}

	@Override
	public String getDescriptionId() {
		return "item.backpack." + size + ".name";
	}

	@Override
	public String getDescriptionId(ItemStack par1ItemStack) {
		return getDescriptionId();
	}
}
