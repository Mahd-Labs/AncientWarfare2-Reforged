package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.core.NonNullList;
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
import net.shadowmage.ancientwarfare.core.registry.ResearchRegistry;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchTracker;
import net.shadowmage.ancientwarfare.core.util.ItemTools;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ItemResearchNotes extends ItemBaseCore {
	private static final String RESEARCH_NAME_TAG = "researchName";

	/*
	 * client hook: name of the local player (1.12: Minecraft.getMinecraft().player.getName()), null when there is none.
	 * Used by the tooltip to tell whether the research on the note is already known.
	 */
	public static Supplier<String> clientPlayerName = () -> null;

	public ItemResearchNotes() {
		super("research_note");
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		CompoundTag tag = ItemTools.getTag(stack);
		Component researchName = Component.literal("corrupt_item");
		boolean known = false;
		if (tag.contains(RESEARCH_NAME_TAG)) {
			String name = tag.getString(RESEARCH_NAME_TAG);
			String playerName = clientPlayerName.get();
			if (ResearchRegistry.researchExists(name) && playerName != null) {
				researchName = Component.translatable(ResearchGoal.getUnlocalizedName(name));
				known = ResearchTracker.INSTANCE.hasClientPlayerCompleted(playerName, name);
			} else {
				researchName = Component.literal("missing_goal_for_id_corrupt_item");
			}
		}
		tooltip.add(researchName);
		if (known) {
			tooltip.add(Component.translatable("guistrings.research.known_research"));
			tooltip.add(Component.translatable("guistrings.research.click_to_add_progress"));
		} else {
			tooltip.add(Component.translatable("guistrings.research.unknown_research"));
			tooltip.add(Component.translatable("guistrings.research.click_to_learn"));
		}
	}

	/*
	 * 1.12 getSubItems: one note per research goal, sorted by name (creative tab content)
	 */
	public NonNullList<ItemStack> getSubItems() {
		NonNullList<ItemStack> items = NonNullList.create();
		for (ResearchGoal goal : ResearchRegistry.getAllResearchGoals().stream().sorted(Comparator.comparing(ResearchGoal::getName))
				.collect(Collectors.toCollection(LinkedHashSet::new))) {
			items.add(getNote(goal.getName()));
		}
		return items;
	}

	public ItemStack getNote(String researchName) {
		ItemStack stack = new ItemStack(this);
		ItemTools.setTagInfo(stack, RESEARCH_NAME_TAG, StringTag.valueOf(researchName));
		return stack;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		CompoundTag tag = ItemTools.getTag(stack);
		if (!world.isClientSide && tag.contains(RESEARCH_NAME_TAG)) {
			String name = tag.getString(RESEARCH_NAME_TAG);
			String playerName = player.getGameProfile().getName();
			if (ResearchRegistry.researchExists(name)) {
				boolean known = ResearchTracker.INSTANCE.hasPlayerCompleted(player.level(), playerName, name);
				if (!known) {
					if (ResearchTracker.INSTANCE.addResearchFromNotes(player.level(), playerName, name)) {
						player.sendSystemMessage(Component.translatable("guistrings.research.learned_from_item", Component.translatable(name)));
						stack.shrink(1);
					}
				} else {
					if (ResearchTracker.INSTANCE.addProgressFromNotes(player.level(), playerName, name)) {
						player.sendSystemMessage(Component.translatable("guistrings.research.added_progress", Component.translatable(name)));
						stack.shrink(1);
					}
				}
			}
		}
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
	}
}
