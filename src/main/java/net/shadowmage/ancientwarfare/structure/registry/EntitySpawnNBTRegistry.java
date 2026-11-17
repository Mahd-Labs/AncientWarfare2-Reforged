package net.shadowmage.ancientwarfare.structure.registry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.Horse;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import net.shadowmage.ancientwarfare.core.registry.IRegistryDataParser;
import net.shadowmage.ancientwarfare.core.util.parsing.JsonHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

public class EntitySpawnNBTRegistry {
	private EntitySpawnNBTRegistry() {}

	private static final Map<Class, Set<String>> entityNBT = new HashMap<>();

	static {
		//Port notes: 1.12 villager tags Profession / ProfessionName / Career / CareerLevel -> VillagerData
		entityNBT.put(net.minecraft.world.entity.npc.Villager.class, ImmutableSet.of("Offers", "VillagerData"));
		entityNBT.put(Horse.class, Collections.singleton("Variant"));
		entityNBT.put(Mob.class, ImmutableSet.of("HandItems", "HandDropChances", "ArmorItems", "ArmorDropChances", "CustomName"));
		entityNBT.put(Entity.class, Collections.singleton("CustomName"));
	}

	public static CompoundTag getEntitySpawnNBT(Entity entity) {
		CompoundTag ret = new CompoundTag();
		CompoundTag fullEntityNbt = new CompoundTag();
		entity.saveWithoutId(fullEntityNbt);

		for (Map.Entry<Class, Set<String>> entry : entityNBT.entrySet()) {
			if (entry.getKey().isInstance(entity)) {
				for (String tag : entry.getValue()) {
					if (fullEntityNbt.contains(tag)) {
						ret.put(tag, fullEntityNbt.get(tag));
					}
				}
			}
		}
		return ret;
	}

	public static class Parser implements IRegistryDataParser {
		@Override
	public String getName() {
			return "entity_spawn_nbt";
		}

		@Override
	public void parse(JsonObject json) {
			entityNBT.putAll(JsonHelper.mapFromJson(json, "entity_nbt",
					entry -> getClass(entry.getKey()),
					entry -> JsonHelper.setFromJson(entry.getValue(), element -> GsonHelper.convertToString(element, ""))));
		}

		private Class getClass(String className) {
			try {
				return Class.forName(className);
			}
			catch (ClassNotFoundException e) {
				throw new MissingResourceException("Unable to find class for class name: " + className, Class.class.toString(), className);
			}
		}
	}
}
