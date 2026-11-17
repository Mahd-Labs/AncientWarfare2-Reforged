package net.shadowmage.ancientwarfare.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.shadowmage.ancientwarfare.AncientWarfare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class NBTHelper {
	private NBTHelper() {}

	/*
	 * Port additions: the only places where ItemStacks are turned into NBT and back (1.12 stack.writeToNBT(tag) /
	 * new ItemStack(tag)). Empty stacks are written as an empty compound and read back as ItemStack.EMPTY.
	 */
	public static CompoundTag writeItemStack(ItemStack stack) {
		return writeItemStack(stack, new CompoundTag());
	}

	public static CompoundTag writeItemStack(ItemStack stack, CompoundTag tag) {
		if (stack.isEmpty()) {
			return tag;
		}
		Tag saved = stack.save(RegistryAccessHelper.get(), tag);
		return saved instanceof CompoundTag ? (CompoundTag) saved : tag;
	}

	public static ItemStack readItemStack(CompoundTag tag) {
		if (tag.isEmpty() || !tag.contains("id")) {
			return ItemStack.EMPTY;
		}
		return ItemStack.parseOptional(RegistryAccessHelper.get(), tag);
	}

	/*
	 * Port additions: 1.12 ItemStackHandler.serializeNBT() / deserializeNBT(tag)
	 */
	public static CompoundTag serializeHandler(ItemStackHandler handler) {
		return handler.serializeNBT(RegistryAccessHelper.get());
	}

	public static void deserializeHandler(ItemStackHandler handler, CompoundTag tag) {
		if (!tag.isEmpty()) {
			handler.deserializeNBT(RegistryAccessHelper.get(), tag);
		}
	}

	public static BlockState getBlockState(CompoundTag blockStateTag) {
		return BlockTools.getBlockState(new Tuple<>(blockStateTag.getString("blockName"), getStateProperties(blockStateTag.getCompound("properties"))),
				Block::defaultBlockState, BlockTools::updateProperty);
	}

	private static Map<String, String> getStateProperties(CompoundTag propertiesTag) {
		Map<String, String> ret = new HashMap<>();
		for (String key : propertiesTag.getAllKeys()) {
			if (propertiesTag.contains(key, Tag.TAG_STRING)) {
				ret.put(key, propertiesTag.getString(key));
			}
		}
		return ret;
	}

	public static CompoundTag getBlockStateTag(BlockState state) {
		CompoundTag ret = new CompoundTag();
		ret.putString("blockName", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
		if (!state.getValues().isEmpty()) {
			ret.put("properties", getStatePropertiesTag(state.getValues()));
		}
		return ret;
	}

	private static CompoundTag getStatePropertiesTag(Map<Property<?>, Comparable<?>> properties) {
		CompoundTag propertiesTag = new CompoundTag();
		for (Map.Entry<Property<?>, Comparable<?>> property : properties.entrySet()) {
			propertiesTag.putString(property.getKey().getName(), serializeValue(property.getKey(), property.getValue()));
		}
		return propertiesTag;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> String serializeValue(Property<T> property, Comparable<?> valueString) {
		return property.getName((T) valueString);
	}

	public static <T> Set<T> getSet(ListTag tagList, Function<Tag, T> getElement) {
		Set<T> ret = new HashSet<>();
		for (Tag tag : tagList) {
			ret.add(getElement.apply(tag));
		}
		return ret;
	}

	public static <T> List<T> getList(ListTag tagList, Function<Tag, T> getElement) {
		ArrayList<T> ret = new ArrayList<>();
		for (Tag tag : tagList) {
			ret.add(getElement.apply(tag));
		}
		return ret;
	}

	public static Set<String> getStringSet(ListTag tagList) {
		return getSet(tagList, Tag::getAsString);
	}

	public static <T> ListTag getTagList(Collection<T> collection, Function<T, Tag> serializeElement) {
		ListTag ret = new ListTag();
		collection.forEach(element -> ret.add(serializeElement.apply(element)));
		return ret;
	}

	public static ListTag getNBTStringList(Collection<String> strings) {
		ListTag ret = new ListTag();
		strings.forEach(str -> ret.add(StringTag.valueOf(str)));
		return ret;
	}

	public static ListTag getNBTUniqueIdList(Collection<UUID> uuids) {
		ListTag ret = new ListTag();
		uuids.forEach(uuid -> ret.add(new NBTBuilder().setUniqueId("uuid", uuid).build()));
		return ret;
	}

	public static Set<UUID> getUniqueIdSet(Tag tag) {
		return getSet(getTagList(tag, Tag.TAG_COMPOUND), element -> ((CompoundTag) element).getUUID("uuid"));
	}

	private static ListTag getTagList(Tag tag, int type) {
		try {
			if (tag != null && tag.getId() == 9) {
				ListTag nbttaglist = (ListTag) tag;
				if (!nbttaglist.isEmpty() && nbttaglist.getElementType() != type) {
					return new ListTag();
				}
				return nbttaglist;
			}
		}
		catch (ClassCastException classcastexception) {
			AncientWarfare.LOG.error("Error casting tag to taglist: {}", tag);
		}
		return new ListTag();
	}

	public static final Collector<Tag, ListTag, ListTag> NBTLIST_COLLECTOR =
			Collector.of(ListTag::new, ListTag::add, (l1, l2) -> {
				l1.addAll(l2);
				return l1;
			});

	public static CompoundTag writeBlockPosToNBT(CompoundTag tag, BlockPos pos) {
		tag.putInt("x", pos.getX());
		tag.putInt("y", pos.getY());
		tag.putInt("z", pos.getZ());
		return tag;
	}

	public static BlockPos readBlockPosFromNBT(CompoundTag tag) {
		return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
	}

	public static <K, V> Map<K, V> getMap(ListTag list, Function<CompoundTag, K> getKey, Function<CompoundTag, V> getValue) {
		Map<K, V> ret = new HashMap<>();
		for (int i = 0; i < list.size(); i++) {
			CompoundTag tag = list.getCompound(i);
			ret.put(getKey.apply(tag), getValue.apply(tag));
		}
		return ret;
	}

	public static <K, V> ListTag mapToCompoundList(Map<K, V> map, BiConsumer<CompoundTag, K> setKeyTag, BiConsumer<CompoundTag, V> setValueTag) {
		ListTag list = new ListTag();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			CompoundTag nbtEntry = new CompoundTag();
			setKeyTag.accept(nbtEntry, entry.getKey());
			setValueTag.accept(nbtEntry, entry.getValue());
			list.add(nbtEntry);
		}
		return list;
	}

	public static void writeSerializablesTo(CompoundTag tag, String key, List<? extends INBTSerializable<?>> elements) {
		ListTag list = new ListTag();
		for (INBTSerializable<?> serializable : elements) {
			list.add(serializable.serializeNBT());
		}
		tag.put(key, list);
	}

	public static <T extends INBTSerializable<CompoundTag>> List<T> deserializeListFrom(CompoundTag tag, String key, Supplier<T> supplier) {
		ListTag tags = tag.getList(key, Tag.TAG_COMPOUND);
		ArrayList<T> list = new ArrayList<>();
		for (int i = 0; i < tags.size(); i++) {
			T element = supplier.get();
			element.deserializeNBT(tags.getCompound(i));
			list.add(element);
		}
		return list;
	}
}
