package net.shadowmage.ancientwarfare.core.util;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.shadowmage.ancientwarfare.AncientWarfare;

import java.util.MissingResourceException;

public class RegistryTools {
	public static final ResourceLocation EMPTY_REGISTRY_NAME = ResourceLocation.fromNamespaceAndPath("", "");

	private RegistryTools() {}

	static Block getBlock(String registryName) {
		return getRegistryEntry(registryName, BuiltInRegistries.BLOCK, Blocks.AIR);
	}

	public static Item getItem(String registryName) {
		return getRegistryEntry(registryName, BuiltInRegistries.ITEM, Items.AIR);
	}

	private static <T> T getRegistryEntry(String registryName, Registry<T> registry, T defaultValue) {
		ResourceLocation key = ResourceLocation.parse(registryName);
		if (!registry.containsKey(key)) {
			if (!ModList.get().isLoaded(key.getNamespace())) {
				AncientWarfare.LOG.debug("Mod {} is not loaded. Replacing {} with {}", key::getNamespace, key::toString, () -> String.valueOf(registry.getKey(defaultValue)));
				return defaultValue;
			}
			throw new MissingResourceException("Unable to find entry with registry name \"" + registryName + "\"",
					registry.key().location().toString(), registryName);
		}
		return registry.get(key);
	}

	/*
	 * Port addition: 1.12 new ResourceLocation(text) accepted any text; 1.21 ResourceLocation.parse throws on
	 * characters that are not valid in an id. Ids read from saved data, packets and typed text go through this:
	 * null for null or invalid text, so a bad id is skipped instead of failing a chunk load, a packet or a screen.
	 */
	@javax.annotation.Nullable
	public static ResourceLocation parseOrNull(@javax.annotation.Nullable String text) {
		return text == null ? null : ResourceLocation.tryParse(text);
	}
}
