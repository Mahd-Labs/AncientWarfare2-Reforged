package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.gamedata.WorldData;
import net.shadowmage.ancientwarfare.core.init.AWCoreItems;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;

import java.util.List;

/*
 * Port note: 1.12 registered the item instance on the event bus from its constructor; AncientWarfareCore.init adds
 * handlePlayerFirstAWCraft as a listener instead.
 */
public class ItemManual extends ItemBaseCore {
	public ItemManual() {
		super("manual", new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		if (!world.isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_MANUAL);
		}
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		tooltip.add(Component.translatable("item.manual.tooltip"));
	}

	public static void handlePlayerFirstAWCraft(PlayerEvent.ItemCraftedEvent evt) {
		Player player = evt.getEntity();
		if (player.level().isClientSide) {
			return;
		}
		Item item = evt.getCrafting().getItem();
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
		if (item != AWCoreItems.MANUAL.get() && registryName.getNamespace().startsWith("ancientwarfare")) {
			WorldData data = AWGameData.INSTANCE.getPerWorldData(player.level(), WorldData.class);
			if (!data.wasPlayerGivenManual(player)) {
				ItemEntity manualDrop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), new ItemStack(AWCoreItems.MANUAL.get()));
				manualDrop.setNoPickUpDelay();
				player.level().addFreshEntity(manualDrop);
				data.addPlayerThatWasGivenManual(player);
			}
		}
	}
}
