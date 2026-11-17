package net.shadowmage.ancientwarfare.core.util.parsing;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.util.ItemTools;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/*
 * Port note: "meta" is the stack's damage value (1.12 sub types are separate items now), the tag compound is the
 * stack's custom data (ItemTools.getTag).
 */
public class ItemStackMatcher implements Predicate<ItemStack> {
	private final Item item;
	private final Predicate<Integer> metaMatches;
	@Nullable
	private final CompoundTag tagCompound;
	private boolean ignoreNbt;

	private ItemStackMatcher(Item item, Predicate<Integer> metaMatches, @Nullable CompoundTag tagCompound, boolean ignoreNbt) {
		this.item = item;
		this.metaMatches = metaMatches;
		this.tagCompound = tagCompound;
		this.ignoreNbt = ignoreNbt;
	}

	@Override
	@SuppressWarnings("squid:S2259")
	public boolean test(ItemStack input) {
		return input.getItem() == item && metaMatches.test(input.getDamageValue()) && (ignoreNbt || tagCompound == null && !ItemTools.hasTag(input) || tagCompound != null && ItemTools.hasTag(input) && tagCompound.equals(ItemTools.getTag(input)));
	}

	public static class Builder {
		private Item item;
		private int meta = -1;
		private CompoundTag tagCompound = null;
		private boolean ignoreNbt = false;

		public Builder(Item item) {
			this.item = item;
		}

		public Builder setMeta(int meta) {
			this.meta = meta;
			return this;
		}

		public Builder setTagCompound(@Nullable CompoundTag tagCompound) {
			this.tagCompound = tagCompound;
			return this;
		}

		Builder setIgnoreNbt(boolean ignoreNbt) {
			this.ignoreNbt = ignoreNbt;
			return this;
		}

		public ItemStackMatcher build() {
			return new ItemStackMatcher(item, meta == -1 ? i -> true : i -> i == meta, tagCompound, ignoreNbt);
		}
	}
}
