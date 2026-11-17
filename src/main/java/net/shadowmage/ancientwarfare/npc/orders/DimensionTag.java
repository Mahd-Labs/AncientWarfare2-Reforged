package net.shadowmage.ancientwarfare.npc.orders;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/*
 * Port addition: 1.12 orders stored the dimension as an int ("dim"). Dimensions are ResourceKey<Level> now and are
 * written as the dimension id string under the same key. Old style ints are still accepted when reading
 * (0 = overworld, -1 = nether, 1 = end, anything else = overworld).
 */
public final class DimensionTag {
	private DimensionTag() {}

	public static void write(CompoundTag tag, String key, ResourceKey<Level> dimension) {
		tag.putString(key, (dimension == null ? Level.OVERWORLD : dimension).location().toString());
	}

	public static ResourceKey<Level> read(CompoundTag tag, String key) {
		if (tag.contains(key, Tag.TAG_STRING)) {
			ResourceLocation id = ResourceLocation.tryParse(tag.getString(key));
			return id == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, id);
		}
		return fromLegacyId(tag.getInt(key));
	}

	public static ResourceKey<Level> fromLegacyId(int id) {
		if (id == -1) {
			return Level.NETHER;
		}
		if (id == 1) {
			return Level.END;
		}
		return Level.OVERWORLD;
	}
}
