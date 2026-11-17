package net.shadowmage.ancientwarfare.structure.template.build.validation.properties;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class StructureValidationPropertyResourceLocation implements IStructureValidationProperty<ResourceLocation> {
	private String name;
	private ResourceLocation defaultValue;

	public StructureValidationPropertyResourceLocation(String name, ResourceLocation defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<ResourceLocation> getValueClass() {
		return ResourceLocation.class;
	}

	@Override
	public CompoundTag serializeNBT(CompoundTag tag, ResourceLocation value) {
		tag.putString(name, value.toString());
		return tag;
	}

	@Override
	public ResourceLocation deserializeNBT(CompoundTag tag) {
		return parseValue(tag.getString(name));
	}

	@Override
	public ResourceLocation getDefaultValue() {
		return defaultValue;
	}

	@Override
	public String getStringValue(ResourceLocation value) {
		return value.toString();
	}

	@Override
	public ResourceLocation parseValue(String valueString) {
		//Port notes: 1.12 ResourceLocation accepted anything; the pack has values like "minecraft:minecraft:" -> default
		ResourceLocation value = ResourceLocation.tryParse(valueString.trim().toLowerCase(java.util.Locale.ROOT));
		return value == null ? defaultValue : value;
	}

}
