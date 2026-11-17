package net.shadowmage.ancientwarfare.npc.entity.faction.attributes;

import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;

import java.util.Optional;

/*
 * Port notes: 1.12 parsed a Java class name ("net.minecraft.entity.passive.EntityHorse"). Entity classes are not a
 * stable identifier in 1.21.1, the registry json holds entity type ids ("minecraft:horse") instead and the attribute
 * value is the EntityType (the 1.12 base class check "is an EntityLiving" is done where the entity is created).
 */
@SuppressWarnings("rawtypes")
public class ClassAttribute extends BaseAttribute<EntityType> {
	@SuppressWarnings({"unused", "FieldCanBeLocal"})
	private Class baseClass;

	public ClassAttribute(String name, Class baseClass) {
		super(name);
		this.baseClass = baseClass;
	}

	@Override
	public Class<EntityType> getValueClass() {
		return EntityType.class;
	}

	@Override
	public Optional<EntityType> parseValue(String value) {
		ResourceLocation id = ResourceLocation.tryParse(value);
		if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
			AncientWarfareNPC.LOG.error("Horse entity class was not found for: " + value);
			return Optional.empty();
		}
		return Optional.of(BuiltInRegistries.ENTITY_TYPE.get(id));
	}
}
