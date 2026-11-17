package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/*
 * Port notes: 1.12 EntityList.createEntityByIDFromName / EntityRegistry lookups.
 */
public class EntityHelper {
	private EntityHelper() {}

	private static final java.util.Map<String, String> RENAMED = new java.util.HashMap<>();

	static {
		RENAMED.put("zombie_pigman", "zombified_piglin");
		RENAMED.put("evocation_illager", "evoker");
		RENAMED.put("vindication_illager", "vindicator");
		RENAMED.put("illusion_illager", "illusioner");
		RENAMED.put("villager_golem", "iron_golem");
		RENAMED.put("snowman", "snow_golem");
		RENAMED.put("ender_crystal", "end_crystal");
		RENAMED.put("evocation_fangs", "evoker_fangs");
		RENAMED.put("xp_orb", "experience_orb");
		RENAMED.put("xp_bottle", "experience_bottle");
		RENAMED.put("fireworks_rocket", "firework_rocket");
		RENAMED.put("commandblock_minecart", "command_block_minecart");
	}

	/*
	 * Port addition: entity ids of original (1.12) templates and spawner settings. The five 1.12 Ancient Warfare mod
	 * ids are folded into "ancientwarfare" (the entity names are unchanged), 1.12 vanilla ids that were renamed are
	 * mapped. Ids that exist are returned as they are.
	 */
	@Nullable
	public static ResourceLocation upgradeId(@Nullable ResourceLocation id) {
		if (id == null || BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
			return id;
		}
		if (id.getNamespace().startsWith("ancientwarfare")) {
			return ResourceLocation.fromNamespaceAndPath("ancientwarfare", id.getPath());
		}
		if (id.getNamespace().equals("minecraft") && RENAMED.containsKey(id.getPath())) {
			return ResourceLocation.withDefaultNamespace(RENAMED.get(id.getPath()));
		}
		return id;
	}

	@Nullable
	public static Entity createEntity(@Nullable ResourceLocation id, Level world) {
		if (id == null) {
			return null;
		}
		id = upgradeId(id);
		return BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(type -> type.create(world)).orElse(null);
	}

	public static ResourceLocation getId(Entity entity) {
		return EntityType.getKey(entity.getType());
	}

	public static boolean exists(@Nullable ResourceLocation id) {
		return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(upgradeId(id));
	}
}
