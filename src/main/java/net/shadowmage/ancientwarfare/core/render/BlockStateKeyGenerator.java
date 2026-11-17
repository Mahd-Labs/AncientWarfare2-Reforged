package net.shadowmage.ancientwarfare.core.render;

import com.google.common.collect.Maps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;

import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

/*
 * Port note: 1.12 implemented CodeChickenLib IBlockStateKeyGenerator and was registered with
 * ModelBakery.registerBlockKeyGenerator(block, generator). In the port the generator is handed to the BakeryModel
 * of the block, which caches the baked quads by this key. IProperty -> Property, IUnlistedProperty -> ModelProperty.
 * Insertion order of the properties is kept (1.12 used hash order, which only has to be stable per run).
 */
public class BlockStateKeyGenerator {
	private final Map<Property, Function<Object, String>> propertyFormats;
	private final Map<ModelProperty, Function<Object, String>> unlistedPropertyFormats;

	private BlockStateKeyGenerator(Map<Property, Function<Object, String>> propertyFormats, Map<ModelProperty, Function<Object, String>> unlistedPropertyFormats) {
		this.propertyFormats = propertyFormats;
		this.unlistedPropertyFormats = unlistedPropertyFormats;
	}

	@SuppressWarnings("unchecked")
	public String generateKey(IExtendedBlockState state) {
		StringJoiner stringJoiner = new StringJoiner("|");
		stringJoiner.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
		for (Map.Entry<Property, Function<Object, String>> entry : propertyFormats.entrySet()) {
			stringJoiner.add(entry.getValue().apply(state.getValue(entry.getKey())));
		}
		for (Map.Entry<ModelProperty, Function<Object, String>> entry : unlistedPropertyFormats.entrySet()) {
			stringJoiner.add(entry.getValue().apply(state.getValue(entry.getKey())));
		}
		return stringJoiner.toString();
	}

	public static class Builder {
		private Map<Property, Function<Object, String>> propertyFormats = Maps.newLinkedHashMap();
		private Map<ModelProperty, Function<Object, String>> unlistedPropertyFormats = Maps.newLinkedHashMap();

		public Builder addKeyProperties(Property... properties) {
			addKeyProperties(String::valueOf, properties);
			return this;
		}

		public Builder addKeyProperties(Function<Object, String> formatValue, Property... properties) {
			for (Property property : properties) {
				this.propertyFormats.put(property, formatValue);
			}
			return this;
		}

		public Builder addKeyProperties(ModelProperty... properties) {
			addKeyProperties(String::valueOf, properties);
			return this;
		}

		public BlockStateKeyGenerator build() {
			return new BlockStateKeyGenerator(this.propertyFormats, this.unlistedPropertyFormats);
		}

		public Builder addKeyProperties(Function<Object, String> getFormat, ModelProperty... properties) {
			for (ModelProperty property : properties) {
				this.unlistedPropertyFormats.put(property, getFormat);
			}
			return this;
		}
	}
}
