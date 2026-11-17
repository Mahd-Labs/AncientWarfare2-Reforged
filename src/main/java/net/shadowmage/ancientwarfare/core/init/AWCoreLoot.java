package net.shadowmage.ancientwarfare.core.init;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

/*
 * Port notes: everything this class did in 1.12 is data in 1.21.1.
 * - 1.12 walked assets/ancientwarfare/loot_tables/chests and registered every table name with LootTableList.
 *   Loot tables are datapack files now: data/ancientwarfare/loot_table/chests/**.json (converted with
 *   tools/convert_loot.py), loaded by the vanilla loot table loader.
 * - 1.12 injected the pools of loot_tables/chests/inject/<name>.json into the vanilla chest table
 *   "minecraft:chests/<name>" on LootTableLoadEvent. The inject tables are ordinary tables
 *   (data/ancientwarfare/loot_table/chests/inject/<name>.json) that the NeoForge global loot modifier
 *   "neoforge:add_table" (data/ancientwarfare/loot_modifiers/inject_<name>.json, listed in
 *   data/neoforge/loot_modifiers/global_loot_modifiers.json) rolls together with the vanilla table.
 * - The loot tables under chests/compat/<mod> belonged to optional 1.12 mod integrations and are dropped.
 */
public class AWCoreLoot {
	private AWCoreLoot() {}

	public static final String INJECT_FOLDER = "inject";

	/*
	 * name of the loot table holding the pools injected into the vanilla chest table "minecraft:chests/<name>"
	 */
	public static ResourceLocation getInjectTableName(String vanillaChestTableName) {
		return ResourceLocation.fromNamespaceAndPath(AncientWarfareCore.MOD_ID, "chests/" + INJECT_FOLDER + "/" + vanillaChestTableName);
	}

	public static void init() {
		//noop, see the class comment
	}
}
