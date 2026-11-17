package net.shadowmage.ancientwarfare.core.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;

/*
 * Port notes: 1.21.1 stacks have no NBT tag. The 1.12 "tag compound" of AW items lives in the CUSTOM_DATA component.
 * getTag returns a copy - always write changes back with setTag (or use setTagInfo).
 */
public class ItemTools {
	private ItemTools() {}

	/*
	 * 1.12 stack.hasTagCompound()
	 */
	public static boolean hasTag(ItemStack stack) {
		return !stack.isEmpty() && stack.has(DataComponents.CUSTOM_DATA);
	}

	/*
	 * 1.12 stack.getTagCompound(), but never null: returns a copy of the custom data (empty tag if there is none)
	 */
	public static CompoundTag getTag(ItemStack stack) {
		CustomData data = stack.isEmpty() ? null : stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? new CompoundTag() : data.copyTag();
	}

	/*
	 * 1.12 stack.setTagCompound(tag). A null tag removes the custom data. An empty tag is kept (like 1.12).
	 */
	public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
		if (stack.isEmpty()) {
			return;
		}
		if (tag == null) {
			stack.remove(DataComponents.CUSTOM_DATA);
		} else {
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
	}

	/*
	 * 1.12 stack.setTagInfo(key, value)
	 */
	public static void setTagInfo(ItemStack stack, String key, Tag value) {
		CompoundTag tag = getTag(stack);
		tag.put(key, value);
		setTag(stack, tag);
	}

	public static JsonElement serializeToJson(ItemStack stack) {
		JsonObject ret = new JsonObject();
		ret.addProperty("name", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		if (stack.getDamageValue() != 0) {
			ret.addProperty("data", stack.getDamageValue());
		}
		if (stack.getCount() > 1) {
			ret.addProperty("count", stack.getCount());
		}
		if (hasTag(stack)) {
			ret.addProperty("nbt", getTag(stack).toString());
		}
		return ret;
	}
}
