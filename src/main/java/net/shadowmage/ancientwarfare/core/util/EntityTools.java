package net.shadowmage.ancientwarfare.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

public class EntityTools {
	private static final String FACTION_NAME_TAG = "factionName";

	private EntityTools() {
	}

	/*
	 * Port addition: 1.12 called NpcFaction.setFactionNameAndDefaults directly from spawnEntity. The npc module
	 * installs that call here (invoked for every spawned entity whose nbt has "factionName"; the handler must check
	 * the entity type itself) so that core does not depend on the npc module.
	 */
	private static BiConsumer<Entity, String> factionNameSetter = (entity, factionName) -> { };

	public static void setFactionNameSetter(BiConsumer<Entity, String> setter) {
		factionNameSetter = setter;
	}

	@Nullable
	public static InteractionHand getHandHoldingItem(LivingEntity entity, Item item) {
		if (entity.getMainHandItem().getItem() == item) {
			return InteractionHand.MAIN_HAND;
		} else if (entity.getOffhandItem().getItem() == item) {
			return InteractionHand.OFF_HAND;
		}
		return null;
	}

	/*
	 * Port addition: flattened sub items (backpack_0..3) share an item class, so containers look the hand up by class
	 */
	@Nullable
	public static InteractionHand getHandHoldingItem(LivingEntity entity, Class<? extends Item> itemClass) {
		if (itemClass.isInstance(entity.getMainHandItem().getItem())) {
			return InteractionHand.MAIN_HAND;
		} else if (itemClass.isInstance(entity.getOffhandItem().getItem())) {
			return InteractionHand.OFF_HAND;
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public static ItemStack getItemFromEitherHand(Player player, Class... itemClasses) {
		for (Class itemClass : itemClasses) {
			if (itemClass.isInstance(player.getMainHandItem().getItem())) {
				return player.getMainHandItem();
			} else if (itemClass.isInstance(player.getOffhandItem().getItem())) {
				return player.getOffhandItem();
			}
		}
		return ItemStack.EMPTY;
	}

	public static String getUnlocName(String resourceLocation) {
		ResourceLocation id = RegistryTools.parseOrNull(resourceLocation);
		return id == null ? resourceLocation : getUnlocName(id);
	}

	/*
	 * Port note: returns the entity type's description id (1.12 built "entity.<modid.>name.name" by hand)
	 */
	public static String getUnlocName(ResourceLocation registryName) {
		return BuiltInRegistries.ENTITY_TYPE.get(registryName).getDescriptionId();
	}

	public static <T extends Entity> List<T> getEntitiesWithinBounds(Level world, Class<T> clazz, BlockPos p1, BlockPos p2) {
		AABB bb = new AABB(Vec3.atLowerCornerOf(p1), Vec3.atLowerCornerOf(p2.offset(1, 1, 1)));
		return world.getEntitiesOfClass(clazz, bb);
	}

	public static void spawnEntity(Level world, ResourceLocation entity, CompoundTag entityNBT, BlockPos pos, String... tags) {
		Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entity);
		if (type.isEmpty()) {
			return;
		}
		Entity e = type.get().create(world);
		if (e == null) {
			return;
		}
		e.moveTo(pos.getX() + 0.5d, pos.getY(), pos.getZ() + 0.5d, world.random.nextFloat() * 360, 0);
		if (e instanceof Mob) {
			if (world instanceof ServerLevel) {
				((Mob) e).finalizeSpawn((ServerLevel) world, world.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.STRUCTURE, null);
			}
			((Mob) e).spawnAnim();
		}

		setDataFromTag(e, entityNBT); //some data needs to be set before spawning entity in the world (like factionName)
		if (tags.length > 0) {
			e.getTags().addAll(Arrays.asList(tags));
		}
		world.addFreshEntity(e);
		setDataFromTag(e, entityNBT); //and some data needs to be set after onInitialSpawn fires for entity]
	}

	private static void setDataFromTag(Entity e, CompoundTag entityNBT) {
		CompoundTag temp = new CompoundTag();
		if (entityNBT.contains(FACTION_NAME_TAG)) {
			factionNameSetter.accept(e, entityNBT.getString(FACTION_NAME_TAG));
		}
		e.saveWithoutId(temp);
		Set<String> keys = entityNBT.getAllKeys();
		for (String key : keys) {
			temp.put(key, entityNBT.get(key).copy());
		}
		e.load(temp);
	}

	@Nullable
	public static Player findClosestPlayer(Level world, BlockPos pos, int maxSqDistance) {
		return world.players().stream().filter(player -> player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < maxSqDistance).findFirst().orElse(null);
	}
}
