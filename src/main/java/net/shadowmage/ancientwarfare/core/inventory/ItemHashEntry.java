package net.shadowmage.ancientwarfare.core.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;

import java.util.stream.Collectors;

/*
 * Lightweight wrapper for an item stack as a hashable object suitable for use as keys in maps.
 * Uses item, item damage, and nbt-tag for hash-code.<br>
 * Immutable.
 *
 * @author Shadowmage
 */
public final class ItemHashEntry {
	private final CompoundTag itemTag;

	private ItemStack cacheStack = ItemStack.EMPTY;
	private String cachedNameAndTooltip = "";

	/*
	 * @param item MUST NOT BE NULL
	 */
	public ItemHashEntry(ItemStack item) {
		ItemStack copy = item.copy();
		copy.setCount(1);
		itemTag = NBTHelper.writeItemStack(copy);
	}

	@Override
	public int hashCode() {
		return itemTag.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ItemHashEntry)) {
			return false;
		}
		return itemTag.equals(((ItemHashEntry) obj).itemTag);
	}

	public ItemStack getItemStack() {
		if (cacheStack.isEmpty()) {
			cacheStack = NBTHelper.readItemStack(itemTag.copy());
		}
		return cacheStack;
	}

	/*
	 * client side in 1.12 (used by the gui item search); the tooltip is built without a player here
	 */
	public String getNameAndTooltip() {
		if (cachedNameAndTooltip.isEmpty()) {
			String stackText = cacheStack.getHoverName().getString().toLowerCase() + " ";
			stackText += cacheStack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL).stream().map(Component::getString).collect(Collectors.joining(" ")).toLowerCase();
			cachedNameAndTooltip = stackText;
		}

		return cachedNameAndTooltip;
	}

	public CompoundTag writeToNBT() {
		return itemTag.copy();
	}

	public static ItemHashEntry readFromNBT(CompoundTag tag) {
		return new ItemHashEntry(NBTHelper.readItemStack(tag));
	}
}
