package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
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
import net.shadowmage.ancientwarfare.core.util.ItemTools;

import javax.annotation.Nullable;
import java.util.List;

public class ItemResearchBook extends ItemBaseCore {

	public ItemResearchBook() {
		super("research_book", new Item.Properties().stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		String name = getResearcherName(stack);
		if (name == null) {
			tooltip.add(Component.translatable("guistrings.research.researcher_name").append(": ").append(Component.translatable("guistrings.research.no_researcher")));
			tooltip.add(Component.translatable("guistrings.research.right_click_to_bind"));
		} else {
			tooltip.add(Component.translatable("guistrings.research.researcher_name").append(": " + name));
			tooltip.add(Component.translatable("guistrings.research.right_click_to_view"));
		}
	}

	@Nullable
	public static String getResearcherName(ItemStack stack) {
		if (!stack.isEmpty() && stack.getItem() instanceof ItemResearchBook && ItemTools.hasTag(stack)) {
			CompoundTag tag = ItemTools.getTag(stack);
			if (tag.contains("researcherName")) {
				return tag.getString("researcherName");
			}
		}
		return null;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!world.isClientSide) {
			if (!ItemTools.hasTag(stack) || !ItemTools.getTag(stack).contains("researcherName")) {
				ItemTools.setTagInfo(stack, "researcherName", StringTag.valueOf(player.getGameProfile().getName()));
				player.sendSystemMessage(Component.translatable("guistrings.research.book_bound"));
			} else {
				NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_RESEARCH_BOOK, 0, 0, 0);
			}
		}
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
	}
}
