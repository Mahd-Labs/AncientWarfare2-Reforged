package net.shadowmage.ancientwarfare.core.util.parsing;

import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.core.util.RegistryAccessHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
 * Port addition. The JSON registries shipped with the mod (and user overrides) describe
 * vanilla stacks with 1.12 item ids and 1.12 stack NBT. 1.21.1 keeps that meaning in data components, so the
 * vanilla parts are translated here when a stack is parsed:
 *   minecraft:spawn_egg + EntityTag.id      -> minecraft:<mob>_spawn_egg
 *   ench / StoredEnchantments (numeric ids) -> minecraft:enchantments / minecraft:stored_enchantments
 *   display.Name / display.Lore             -> minecraft:custom_name / minecraft:lore
 *   Potion                                  -> minecraft:potion_contents
 *   RepairCost, Unbreakable                 -> their components
 * Everything else (AW's own item data, other mods' data) stays in custom data as before.
 * Enchantments are a datapack registry: if no registry access exists yet (registries are first parsed during
 * mod setup) the list is kept under LEGACY_ENCH_KEY in custom data and the owning registry is parsed again once a
 * server is starting (see AncientWarfareCore).
 */
public final class LegacyStackNbt {
	private LegacyStackNbt() {}

	public static final String LEGACY_ENCH_KEY = "awLegacyEnch";

	private static final Map<Integer, String> ENCHANTMENT_IDS = new HashMap<>();

	static {
		String[] names = {"0:protection", "1:fire_protection", "2:feather_falling", "3:blast_protection", "4:projectile_protection", "5:respiration", "6:aqua_affinity", "7:thorns", "8:depth_strider", "9:frost_walker", "10:binding_curse", "16:sharpness", "17:smite", "18:bane_of_arthropods", "19:knockback", "20:fire_aspect", "21:looting", "22:sweeping_edge", "32:efficiency", "33:silk_touch", "34:unbreaking", "35:fortune", "48:power", "49:punch", "50:flame", "51:infinity", "61:luck_of_the_sea", "62:lure", "70:mending", "71:vanishing_curse"};
		for (String n : names) {
			String[] parts = n.split(":");
			ENCHANTMENT_IDS.put(Integer.parseInt(parts[0]), parts[1]);
		}
	}

	/*
	 * 1.12 item id (+ its nbt) -> 1.21.1 item id, for vanilla items whose identity moved out of NBT
	 */
	public static String mapItemName(String registryName, JsonObject stackJson) {
		if (!registryName.equals("minecraft:spawn_egg")) {
			return registryName;
		}
		CompoundTag tag = parse(stackJson);
		if (tag != null && tag.contains("EntityTag", Tag.TAG_COMPOUND)) {
			ResourceLocation entity = ResourceLocation.tryParse(tag.getCompound("EntityTag").getString("id"));
			if (entity != null) {
				ResourceLocation egg = ResourceLocation.fromNamespaceAndPath(entity.getNamespace(), entity.getPath() + "_spawn_egg");
				if (BuiltInRegistries.ITEM.containsKey(egg)) {
					return egg.toString();
				}
			}
		}
		AncientWarfare.LOG.warn("No spawn egg item for legacy stack {}, using pig spawn egg", stackJson);
		return "minecraft:pig_spawn_egg";
	}

	private static CompoundTag parse(JsonObject stackJson) {
		if (!GsonHelper.isValidNode(stackJson, "nbt")) {
			return null;
		}
		try {
			return TagParser.parseTag(GsonHelper.getAsString(stackJson, "nbt"));
		}
		catch (CommandSyntaxException e) {
			return null;
		}
	}

	/*
	 * applies a 1.12 stack tag: vanilla meaning goes to components, the rest to custom data
	 */
	public static void apply(ItemStack stack, CompoundTag legacy) {
		CompoundTag tag = legacy.copy();
		boolean egg = stack.getItem() instanceof net.minecraft.world.item.SpawnEggItem;
		if (egg && tag.contains("EntityTag", Tag.TAG_COMPOUND)) {
			CompoundTag entityTag = tag.getCompound("EntityTag");
			tag.remove("EntityTag");
			if (entityTag.size() > 1) {
				stack.set(DataComponents.ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(entityTag));
			}
		}
		if (tag.contains("display", Tag.TAG_COMPOUND)) {
			CompoundTag display = tag.getCompound("display");
			if (display.contains("Name", Tag.TAG_STRING)) {
				stack.set(DataComponents.CUSTOM_NAME, Component.literal(display.getString("Name")));
				display.remove("Name");
			}
			if (display.contains("color", Tag.TAG_ANY_NUMERIC)) {
				stack.set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(display.getInt("color"), true));
				display.remove("color");
			}
			if (display.contains("Lore", Tag.TAG_LIST)) {
				ListTag lore = display.getList("Lore", Tag.TAG_STRING);
				List<Component> lines = new ArrayList<>();
				for (int i = 0; i < lore.size(); i++) {
					lines.add(Component.literal(lore.getString(i)));
				}
				stack.set(DataComponents.LORE, new ItemLore(lines));
				display.remove("Lore");
			}
			if (display.isEmpty()) {
				tag.remove("display");
			}
		}
		if (tag.contains("BlockEntityTag", Tag.TAG_COMPOUND) && tag.getCompound("BlockEntityTag").contains("Patterns", Tag.TAG_LIST)) {
			if (applyBannerPatterns(stack, tag.getCompound("BlockEntityTag").getList("Patterns", Tag.TAG_COMPOUND))) {
				tag.getCompound("BlockEntityTag").remove("Patterns");
				tag.getCompound("BlockEntityTag").remove("Base");
				if (tag.getCompound("BlockEntityTag").isEmpty()) {
					tag.remove("BlockEntityTag");
				}
			}
		}
		if (tag.contains("Potion", Tag.TAG_STRING)) {
			ResourceLocation id = ResourceLocation.tryParse(tag.getString("Potion"));
			Optional<Holder.Reference<Potion>> potion = id == null ? Optional.empty() : BuiltInRegistries.POTION.getHolder(id);
			if (potion.isPresent()) {
				stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion.get()));
				tag.remove("Potion");
			}
		}
		if (tag.contains("RepairCost", Tag.TAG_ANY_NUMERIC)) {
			int cost = tag.getInt("RepairCost");
			if (cost > 0) {
				stack.set(DataComponents.REPAIR_COST, cost);
			}
			tag.remove("RepairCost");
		}
		if (tag.contains("Unbreakable", Tag.TAG_ANY_NUMERIC)) {
			if (tag.getBoolean("Unbreakable")) {
				stack.set(DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(true));
			}
			tag.remove("Unbreakable");
		}
		for (String key : new String[] {"ench", "StoredEnchantments"}) {
			if (tag.contains(key, Tag.TAG_LIST)) {
				ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
				tag.remove(key);
				if (!applyEnchantments(stack, list, key.equals("StoredEnchantments") || stack.is(Items.ENCHANTED_BOOK))) {
					CompoundTag pending = new CompoundTag();
					pending.put(key, list);
					tag.put(LEGACY_ENCH_KEY, pending);
				}
			}
		}
		if (!tag.isEmpty()) {
			ItemTools.setTag(stack, tag);
		}
	}

	/* 1.12 banner pattern codes -> 1.21.1 pattern ids; 1.12 colours are dye damage values (15 = white ... 0 = black) */
	private static final Map<String, String> BANNER_PATTERNS = new HashMap<>();

	static {
		String[] codes = {"b:base", "bl:square_bottom_left", "br:square_bottom_right", "tl:square_top_left", "tr:square_top_right", "bs:stripe_bottom", "ts:stripe_top", "ls:stripe_left", "rs:stripe_right", "cs:stripe_center", "ms:stripe_middle", "drs:stripe_downright", "dls:stripe_downleft", "ss:small_stripes", "cr:cross", "sc:straight_cross", "bt:triangle_bottom", "tt:triangle_top", "bts:triangles_bottom", "tts:triangles_top", "ld:diagonal_left", "rd:diagonal_up_right", "lud:diagonal_up_left", "rud:diagonal_right", "mc:circle", "mr:rhombus", "vh:half_vertical", "hh:half_horizontal", "vhr:half_vertical_right", "hhb:half_horizontal_bottom", "bo:border", "cbo:curly_border", "gra:gradient", "gru:gradient_up", "bri:bricks", "cre:creeper", "sku:skull", "flo:flower", "moj:mojang"};
		for (String c : codes) {
			String[] parts = c.split(":");
			BANNER_PATTERNS.put(parts[0], parts[1]);
		}
	}

	private static boolean applyBannerPatterns(ItemStack stack, ListTag patterns) {
		Optional<HolderLookup.RegistryLookup<net.minecraft.world.level.block.entity.BannerPattern>> lookup = RegistryAccessHelper.get().lookup(Registries.BANNER_PATTERN);
		if (lookup.isEmpty()) {
			return false;
		}
		net.minecraft.world.level.block.entity.BannerPatternLayers.Builder builder = new net.minecraft.world.level.block.entity.BannerPatternLayers.Builder();
		for (int i = 0; i < patterns.size(); i++) {
			CompoundTag entry = patterns.getCompound(i);
			String name = BANNER_PATTERNS.get(entry.getString("Pattern"));
			if (name == null) {
				continue;
			}
			Optional<Holder.Reference<net.minecraft.world.level.block.entity.BannerPattern>> holder = lookup.get().get(ResourceKey.create(Registries.BANNER_PATTERN, ResourceLocation.withDefaultNamespace(name)));
			if (holder.isPresent()) {
				builder.add(holder.get(), net.minecraft.world.item.DyeColor.byId(15 - (entry.getInt("Color") & 15)));
			}
		}
		stack.set(DataComponents.BANNER_PATTERNS, builder.build());
		return true;
	}

	private static boolean applyEnchantments(ItemStack stack, ListTag list, boolean stored) {
		HolderLookup.Provider provider = RegistryAccessHelper.get();
		Optional<HolderLookup.RegistryLookup<Enchantment>> lookup = provider.lookup(Registries.ENCHANTMENT);
		if (lookup.isEmpty()) {
			return false;
		}
		ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			String name = entry.contains("id", Tag.TAG_STRING) ? entry.getString("id") : ENCHANTMENT_IDS.get((int) entry.getShort("id"));
			ResourceLocation id = name == null ? null : ResourceLocation.tryParse(name);
			if (id == null) {
				AncientWarfare.LOG.warn("Unknown legacy enchantment {} on {}", entry, stack);
				continue;
			}
			Optional<Holder.Reference<Enchantment>> holder = lookup.get().get(ResourceKey.create(Registries.ENCHANTMENT, id));
			if (holder.isPresent()) {
				enchantments.set(holder.get(), Math.max(1, entry.getShort("lvl")));
			} else {
				AncientWarfare.LOG.warn("Legacy enchantment {} does not exist in 1.21.1 (on {})", id, stack);
			}
		}
		stack.set(stored ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS, enchantments.toImmutable());
		return true;
	}
}
