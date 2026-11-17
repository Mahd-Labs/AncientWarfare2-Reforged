package net.shadowmage.ancientwarfare.npc.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/*
 * Port notes: the 1.12 custom IRarity (dark purple name) has no 1.21.1 counterpart without an enum extension; the
 * item name is styled dark purple instead. Durability = material uses - 100 as in 1.12.
 */
public class ItemMacuahuitl extends SwordItem {
	private final String regName;

	public ItemMacuahuitl(Tier material, String registryName) {
		super(ItemBaseNPC.tierWithUses(material, material.getUses() - 100), new Item.Properties().attributes(SwordItem.createAttributes(material, 3, -2.4F)));
		this.regName = registryName;
	}

	@Override
	public String getDescriptionId() {
		return "item." + regName + ".name";
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(getDescriptionId(stack)).withStyle(ChatFormatting.DARK_PURPLE);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		tooltip.add(Component.translatable("item.macuahuitl.tooltip"));
	}

	@Override
	public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
		return repair.getItem() == Items.OBSIDIAN || super.isValidRepairItem(toRepair, repair);
	}
}
