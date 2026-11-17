package net.shadowmage.ancientwarfare.structure.util;

import com.mojang.serialization.Dynamic;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;

import java.util.HashMap;
import java.util.Map;

/*
 * Port addition (companion of LegacyBlockStates): item stacks inside the rules of the original (1.12) templates are
 * 1.12 stack NBT ({id:"minecraft:log",Count:4b,Damage:1s,tag:{..}}). Read as they are they lose their count and
 * meta, and every renamed item logs a vanilla error. A stack tag with the 1.12 "Count" key is run through the
 * vanilla data fixers from the 1.12.2 data version (flattening, renames, tag -> components); the five 1.12 Ancient
 * Warfare mod ids are folded into "ancientwarfare" and the meta sub items of this port are mapped
 * (tools/aw_item_meta_map.json). Items that do not exist (1.12 mods, modules that are not ported) give an empty
 * stack, logged at debug.
 */
public class LegacyItemStacks {
	private LegacyItemStacks() {}

	private static final int DATA_VERSION_1_12_2 = 1343;
	private static final Map<String, String[]> META_ITEMS = new HashMap<>();

	static {
		META_ITEMS.put("backpack", new String[] {"backpack_0", "backpack_1", "backpack_2", "backpack_3"});
		META_ITEMS.put("component", new String[] {"component_wooden_gear", "component_iron_gear", "component_steel_gear", "component_wooden_bearings", "component_iron_bearings", "component_steel_bearings", "component_wooden_shaft", "component_iron_shaft", "component_steel_shaft"});
		META_ITEMS.put("torque_junction", new String[] {"torque_junction_light", "torque_junction_medium", "torque_junction_heavy"});
		META_ITEMS.put("torque_shaft", new String[] {"torque_shaft_light", "torque_shaft_medium", "torque_shaft_heavy"});
		META_ITEMS.put("torque_distributor", new String[] {"torque_distributor_light", "torque_distributor_medium", "torque_distributor_heavy"});
		META_ITEMS.put("flywheel_controller", new String[] {"flywheel_controller_light", "flywheel_controller_medium", "flywheel_controller_heavy"});
		META_ITEMS.put("flywheel_storage", new String[] {"flywheel_storage_light", "flywheel_storage_medium", "flywheel_storage_heavy"});
		META_ITEMS.put("warehouse_storage", new String[] {"warehouse_storage_small", "warehouse_storage_medium", "warehouse_storage_large"});
		META_ITEMS.put("worksite_upgrade", new String[] {"worksite_upgrade_bounds_medium", "worksite_upgrade_bounds_large", "worksite_upgrade_quarry_medium", "worksite_upgrade_quarry_large", "worksite_upgrade_enchanted_tools_1", "worksite_upgrade_enchanted_tools_2", "worksite_upgrade_quality_tools_1", "worksite_upgrade_quality_tools_2", "worksite_upgrade_quality_tools_3", "worksite_upgrade_chunkloader_basic", "worksite_upgrade_chunkloader_quarry"});
		META_ITEMS.put("gate_spawner", new String[] {"gate_spawner_0", "gate_spawner_1", null, null, "gate_spawner_4", "gate_spawner_5", null, null, "gate_spawner_8", "gate_spawner_9", null, null, "gate_spawner_12"});
	}

	public static boolean isLegacy(CompoundTag stackTag) {
		return stackTag.contains("Count") && !stackTag.contains("count");
	}

	/*
	 * replacement of NBTHelper.readItemStack for stack tags of template rules
	 */
	public static ItemStack read(CompoundTag stackTag) {
		if (stackTag.isEmpty() || !stackTag.contains("id")) {
			return ItemStack.EMPTY;
		}
		CompoundTag tag = isLegacy(stackTag) ? upgrade(stackTag) : stackTag;
		ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
			AncientWarfareStructure.LOG.debug("Item {} of a template does not exist, stack skipped", stackTag.getString("id"));
			return ItemStack.EMPTY;
		}
		try {
			return NBTHelper.readItemStack(tag);
		}
		catch (RuntimeException e) {
			AncientWarfareStructure.LOG.debug("Item stack {} of a template could not be read: {}", stackTag, e.toString());
			return ItemStack.EMPTY;
		}
	}

	/*
	 * Tile entity / entity data of a rule of an original 1.12 template (callers check the origin, this must not run
	 * on data the port wrote): every 1.12 item stack found anywhere inside is upgraded (stacks of items that do not
	 * exist are removed), every 1.12 UUID pair (<key>Most / <key>Least) also gets the 1.21 form (<key> as int array)
	 * that CompoundTag.getUUID reads, plain text custom names become text components, vanilla spawner data gets its
	 * 1.21 shape and the packed block positions Ancient Warfare stores (PACKED_POSITION_KEYS) are re-packed from the
	 * 1.12 bit layout to the 1.21 one.
	 */
	private static final java.util.Set<String> PACKED_POSITION_KEYS = java.util.Set.of("home", "bedPos", "cachedBedPos", "mainBlockPos", "controllerPos",
			"controllerPosition", "controlPos", "bbMin", "bbMax", "pos1", "pos2", "seatPos", "respawnPos", "pos", "position", "targetPos", "townHall", "upkeepPos",
			"upkeepPosition", "withdrawPoint", "depositPoint", "min", "max", "current", "validate", "protectionFlagPos", "warehousePosTag", "lockPos", "clearPos",
			"hangingOffset");
	private static final java.util.Set<String> PACKED_POSITION_LIST_KEYS = java.util.Set.of("pointList", "stockLinkerPositions");

	/* vanilla chest loot tables that were renamed after 1.12 (436 references in the template pack) */
	private static final Map<String, String> RENAMED_LOOT_TABLES = Map.of("minecraft:chests/village_blacksmith", "minecraft:chests/village/village_weaponsmith");

	public static CompoundTag upgradeNbt(CompoundTag data) {
		upgradeCompound(data);
		return data;
	}

	private static boolean isLegacyStack(Tag tag) {
		return tag instanceof CompoundTag && ((CompoundTag) tag).contains("id", Tag.TAG_STRING) && ((CompoundTag) tag).contains("Count", Tag.TAG_ANY_NUMERIC) && !((CompoundTag) tag).contains("count");
	}

	/*
	 * null: the item does not exist
	 */
	private static CompoundTag upgradeStackOrNull(CompoundTag legacy) {
		CompoundTag upgraded = upgrade(legacy);
		ResourceLocation id = ResourceLocation.tryParse(upgraded.getString("id"));
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
			AncientWarfareStructure.LOG.debug("Item {} of a template does not exist, stack skipped", legacy.getString("id"));
			return null;
		}
		return upgraded;
	}

	private static void upgradeCompound(CompoundTag compound) {
		for (String key : new java.util.ArrayList<>(compound.getAllKeys())) {
			Tag child = compound.get(key);
			if (isLegacyStack(child)) {
				CompoundTag upgraded = upgradeStackOrNull((CompoundTag) child);
				if (upgraded == null) {
					compound.remove(key);
				} else {
					compound.put(key, upgraded);
				}
			} else if (key.equals("SpawnData") && child instanceof CompoundTag && ((CompoundTag) child).contains("id", Tag.TAG_STRING) && !((CompoundTag) child).contains("entity")) {
				//vanilla mob spawner, 1.12: SpawnData is the entity, SpawnPotentials are {Entity, Weight}
				upgradeCompound((CompoundTag) child);
				CompoundTag spawnData = new CompoundTag();
				spawnData.put("entity", child);
				compound.put(key, spawnData);
				if (compound.contains("SpawnPotentials", Tag.TAG_LIST)) {
					net.minecraft.nbt.ListTag potentials = new net.minecraft.nbt.ListTag();
					for (Tag potential : compound.getList("SpawnPotentials", Tag.TAG_COMPOUND)) {
						CompoundTag old = (CompoundTag) potential;
						if (!old.contains("Entity", Tag.TAG_COMPOUND)) {
							continue;
						}
						CompoundTag data = new CompoundTag();
						CompoundTag entity = old.getCompound("Entity");
						if (old.contains("Tags", Tag.TAG_LIST)) {
							entity.put("Tags", old.get("Tags"));
						}
						data.put("entity", entity);
						CompoundTag entry = new CompoundTag();
						entry.put("data", data);
						entry.putInt("weight", Math.max(1, old.getInt("Weight")));
						potentials.add(entry);
					}
					compound.put("SpawnPotentials", potentials);
				}
			} else if (child instanceof CompoundTag) {
				upgradeCompound((CompoundTag) child);
			} else if (child instanceof net.minecraft.nbt.ListTag) {
				net.minecraft.nbt.ListTag list = (net.minecraft.nbt.ListTag) child;
				if (PACKED_POSITION_LIST_KEYS.contains(key) && list.getElementType() == Tag.TAG_LONG) {
					for (int i = 0; i < list.size(); i++) {
						list.set(i, net.minecraft.nbt.LongTag.valueOf(LegacyPositions.upgradeLong(((net.minecraft.nbt.LongTag) list.get(i)).getAsLong())));
					}
				} else {
					upgradeList(list);
				}
			} else if (key.equals("CustomName") && child instanceof net.minecraft.nbt.StringTag) {
				//1.12 custom names are plain text, 1.21 reads a text component (json)
				String name = child.getAsString();
				if (!name.isEmpty() && !name.startsWith("{") && !name.startsWith("\"") && !name.startsWith("[")) {
					compound.putString(key, new com.google.gson.JsonPrimitive(name).toString());
				}
			} else if (child instanceof net.minecraft.nbt.StringTag && (key.equals("LootTable") || key.equals("lootTableName")) && RENAMED_LOOT_TABLES.containsKey(child.getAsString())) {
				compound.putString(key, RENAMED_LOOT_TABLES.get(child.getAsString()));
			} else if (child instanceof net.minecraft.nbt.LongTag && PACKED_POSITION_KEYS.contains(key)) {
				compound.putLong(key, LegacyPositions.upgradeLong(((net.minecraft.nbt.LongTag) child).getAsLong()));
			} else if (child instanceof net.minecraft.nbt.LongArrayTag && PACKED_POSITION_LIST_KEYS.contains(key)) {
				long[] values = ((net.minecraft.nbt.LongArrayTag) child).getAsLongArray().clone();
				for (int i = 0; i < values.length; i++) {
					values[i] = LegacyPositions.upgradeLong(values[i]);
				}
				compound.putLongArray(key, values);
			} else if (key.endsWith("Most") && child instanceof net.minecraft.nbt.LongTag) {
				String name = key.substring(0, key.length() - 4);
				if (!name.isEmpty() && compound.contains(name + "Least", Tag.TAG_LONG) && !compound.contains(name)) {
					compound.putUUID(name, new java.util.UUID(compound.getLong(key), compound.getLong(name + "Least")));
				}
			}
		}
	}

	private static void upgradeList(net.minecraft.nbt.ListTag list) {
		for (int i = list.size() - 1; i >= 0; i--) {
			Tag child = list.get(i);
			if (isLegacyStack(child)) {
				CompoundTag upgraded = upgradeStackOrNull((CompoundTag) child);
				if (upgraded == null) {
					list.remove(i);
				} else {
					list.set(i, upgraded);
				}
			} else if (child instanceof CompoundTag) {
				upgradeCompound((CompoundTag) child);
			} else if (child instanceof net.minecraft.nbt.ListTag) {
				upgradeList((net.minecraft.nbt.ListTag) child);
			}
		}
	}

	//the data fixers take about 2 ms per stack; the pack repeats the same stacks (coins, food) thousands of times
	private static final Map<CompoundTag, CompoundTag> UPGRADED = new HashMap<>();

	public static CompoundTag upgrade(CompoundTag legacyStackTag) {
		CompoundTag key = legacyStackTag.copy();
		Tag slot = key.get("Slot");
		key.remove("Slot");
		CompoundTag cached = UPGRADED.get(key);
		if (cached == null) {
			cached = upgradeUncached(key);
			if (UPGRADED.size() < 20000) {
				UPGRADED.put(key, cached);
			}
		}
		CompoundTag ret = cached.copy();
		if (slot != null) {
			ret.put("Slot", slot);
		}
		return ret;
	}

	private static CompoundTag upgradeUncached(CompoundTag legacyStackTag) {
		CompoundTag input = legacyStackTag.copy();
		String name = input.getString("id");
		boolean aw = name.startsWith("ancientwarfare");
		int meta = input.getShort("Damage");
		if (aw) {
			int colon = name.indexOf(':');
			String path = colon > 0 ? name.substring(colon + 1) : name;
			String[] subItems = META_ITEMS.get(path);
			if (subItems != null) {
				if (meta >= 0 && meta < subItems.length && subItems[meta] != null) {
					path = subItems[meta];
				}
				input.remove("Damage");
			}
			input.putString("id", "ancientwarfare:" + path);
		}
		ResourceLocation id = ResourceLocation.tryParse(input.getString("id"));
		if (id == null || !id.getNamespace().equals("minecraft") && !BuiltInRegistries.ITEM.containsKey(id)) {
			return input; //an item of a mod that is not there: nothing to upgrade, the caller drops the stack
		}
		try {
			Dynamic<Tag> fixed = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, input), DATA_VERSION_1_12_2, LegacyBlockStates.currentDataVersion());
			if (fixed.getValue() instanceof CompoundTag) {
				CompoundTag result = (CompoundTag) LegacyBlockStates.mutableCopy(fixed.getValue());
				//enchantments of 1.12 mods (numeric ids the fixers do not know) come out as the key "null"
				CompoundTag components = result.getCompound("components");
				for (String type : new String[] {"minecraft:enchantments", "minecraft:stored_enchantments"}) {
					if (components.contains(type, Tag.TAG_COMPOUND)) {
						components.getCompound(type).getCompound("levels").remove("null");
						if (components.getCompound(type).getCompound("levels").isEmpty()) {
							components.remove(type);
						}
					}
				}
				//a 1.12 written book without an author has no valid 1.21 form without one
				if (components.contains("minecraft:written_book_content", Tag.TAG_COMPOUND) && !components.getCompound("minecraft:written_book_content").contains("author")) {
					components.getCompound("minecraft:written_book_content").putString("author", "");
				}
				return result;
			}
		}
		catch (RuntimeException e) {
			AncientWarfareStructure.LOG.debug("Could not upgrade the 1.12 item stack {}: {}", legacyStackTag, e.toString());
		}
		return input;
	}
}
