package net.shadowmage.ancientwarfare.structure.template.build.validation.properties;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.shadowmage.ancientwarfare.core.util.StringTools;

import java.util.HashSet;
import java.util.Set;

public class StructureValidationPropertyStringSet implements IStructureValidationProperty<Set> {

	private String name;
	private Set<String> defaultValue;

	public StructureValidationPropertyStringSet(String name, Set<String> defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}

	@Override
	public CompoundTag serializeNBT(CompoundTag tag, Set value) {
		ListTag list = new ListTag();
		//noinspection unchecked
		value.forEach(element -> list.add(StringTag.valueOf((String) element)));
		tag.put(getName(), list);
		return tag;
	}

	@Override
	public Set<String> deserializeNBT(CompoundTag tag) {
		ListTag list = tag.getList(getName(), Tag.TAG_STRING);
		Set<String> ret = new HashSet<>();
		for (int i = 0; i < list.size(); i++) {
			ret.add(list.getString(i));
		}
		return ret;
	}

	@Override
	public Set<String> getDefaultValue() {
		return defaultValue;
	}

	@Override
	public String getStringValue(Set value) {
		//noinspection unchecked
		return StringTools.getCSVValueFor(value);
	}

	@Override
	public Set parseValue(String valueString) {
		return StringTools.parseStringSet(valueString);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<Set> getValueClass() {
		return Set.class;
	}
}
