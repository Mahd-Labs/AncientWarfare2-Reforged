package net.shadowmage.ancientwarfare.npc.config;

import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.shadowmage.ancientwarfare.core.config.ModConfiguration;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;
import net.shadowmage.ancientwarfare.npc.registry.FactionDefinition;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Port notes:
 * - file config/ancientwarfare-npc.toml (COMMON spec) holds the 1.12 "01_shared_settings" and "02_server_settings"
 *   options with the 1.12 names and defaults.
 * - The 1.12 "03_client_settings" options live in a second, CLIENT spec (config/ancientwarfare-npc-client.toml)
 *   registered from this class. They are plain static booleans now (1.12: forge Property objects read by the client
 *   code with getBoolean()); the options GUI changes them through setClientOption(name, value).
 * - 1.12 had two more files whose keys grew at runtime (AncientWarfareNpcFood.cfg: one int per item id,
 *   AncientWarfareNpcFactionStandings.cfg: "<faction>.starting_faction_standing"). A ModConfigSpec cannot grow keys,
 *   so both are string lists in the COMMON spec: "01_food_settings.food_values" = ["<item id>=<ticks>", ...] and
 *   "01_faction_settings.starting_faction_standings" = ["<faction>=<standing>", ...]; a faction that is not listed
 *   uses the default of the faction registry (what 1.12 wrote into the file as the default).
 * - getFoodValue: ItemFood.getHealAmount / getSaturationModifier -> FoodProperties.nutrition() and
 *   saturation() / (nutrition() * 2) (1.21 stores the resulting saturation, not the modifier).
 * - Fields without a config entry in 1.12 (npcDefaultUpkeepWithdraw, npcAIDebugMode) are
 *   kept as they were.
 */
public class AWNPCStatics extends ModConfiguration {

	/* ********************************************SHARED SETTINGS************************************************ */
	public static int npcActionRange = 3;
	public static boolean repackCreativeOnly = false;
	public static boolean persistOrdersOnDeath = true;

	/* ********************************************SERVER SETTINGS************************************************ */
	public static int maxNpcLevel = 10; // Deprecated
	public static int maxNpcWorkerLevel = 10;
	public static int maxNpcCombatLevel = 10;
	public static int npcXpFromWork = 1;
	public static int npcXpFromTrade = 1;
	public static int npcXpFromAttack = 1;
	public static int npcXpFromKill = 5;
	public static int npcXpFromMoveItem = 1;
	public static int npcWorkTicks = 50;
	public static int npcDefaultUpkeepWithdraw = 6000;//5 minutes
	public static boolean npcAllowUpkeepAnyInventory = true;
	//improvements over the 1.12 behaviour (not baseline); each can be switched back to the original behaviour
	public static boolean improvedPathReuse = true;
	public static boolean improvedSkipUnreachableWorksites = true;
	public static boolean improvedMedicTriage = true;
	public static boolean improvedUpkeepCounting = true;
	public static boolean improvedNpcLevelEditor = true;
	public static int townMaxRange = 100;
	public static int townUpdateFreq = 100; //5 second broadcast frequency
	public static boolean npcAIDebugMode = false;
	public static double archerRange = 60.0;
	public static boolean vanillaEquipmentDropRate = true;
	public static double npcLevelDamageMultiplier = 0.05;//damage bonus per npc level.  @ level 10 they do 2x the damage as at lvl 0

	/* ********************************************CLIENT SETTINGS************************************************ */
	public static boolean loadDefaultSkinPack = true;

	public static boolean renderAI = true;
	public static boolean renderWorkPoints = true;
	public static boolean renderFriendlyNames = false;
	public static boolean renderHostileNames = false;
	public static boolean renderFriendlyHealth = true;
	public static boolean renderHostileHealth = true;
	public static boolean renderTeamColors = true;

	/* names of the client options, as used by setClientOption / getClientOption (the 1.12 config keys) */
	public static final String RENDER_AI = "render_npc_ai";
	public static final String RENDER_WORK_POINTS = "render_work_points";
	public static final String RENDER_FRIENDLY_NAMES = "render_friendly_nameplates";
	public static final String RENDER_HOSTILE_NAMES = "render_hostile_nameplates";
	public static final String RENDER_FRIENDLY_HEALTH = "render_friendly_health";
	public static final String RENDER_HOSTILE_HEALTH = "render_hostile_health";
	public static final String RENDER_TEAM_COLORS = "render_team_colors";
	public static final String LOAD_DEFAULT_SKIN_PACK = "load_default_skin_pack";

	/* ********************************************FOOD SETTINGS************************************************ */
	private static final String foodSettings = "01_food_settings";
	private static final Map<String, Integer> foodValues = new HashMap<>();
	private static int foodMultiplier = 350;

	static {
		//1.12 defaults, valid until the config file is loaded
		foodValues.put("minecraft:apple", 3000);
		foodValues.put("minecraft:mushroom_stew", 4500);
		foodValues.put("minecraft:rotten_flesh", 0);
		foodValues.put("minecraft:poisonous_potato", 0);
		foodValues.put("minecraft:spider_eye", 0);
	}

	/* ********************************************FACTION STARTING VALUE SETTINGS************************************************ */
	private static final String factionSettings = "01_faction_settings";
	private static final Map<String, Integer> factionStartingStandings = new HashMap<>();

	/*
	 * spec values (no initializers: they are assigned from the super constructor)
	 */
	private ModConfigSpec.IntValue maxNpcLevelValue;
	private ModConfigSpec.IntValue maxNpcWorkerLevelValue;
	private ModConfigSpec.IntValue maxNpcCombatLevelValue;
	private ModConfigSpec.IntValue npcXpFromAttackValue;
	private ModConfigSpec.IntValue npcXpFromKillValue;
	private ModConfigSpec.DoubleValue npcLevelDamageMultiplierValue;
	private ModConfigSpec.IntValue npcXpFromTradeValue;
	private ModConfigSpec.IntValue npcXpFromWorkValue;
	private ModConfigSpec.IntValue npcXpFromMoveItemValue;
	private ModConfigSpec.IntValue npcWorkTicksValue;
	private ModConfigSpec.BooleanValue npcAllowUpkeepAnyInventoryValue;
	private ModConfigSpec.BooleanValue improvedPathReuseValue;
	private ModConfigSpec.BooleanValue improvedSkipUnreachableWorksitesValue;
	private ModConfigSpec.BooleanValue improvedMedicTriageValue;
	private ModConfigSpec.BooleanValue improvedUpkeepCountingValue;
	private ModConfigSpec.BooleanValue improvedNpcLevelEditorValue;
	private ModConfigSpec.IntValue townMaxRangeValue;
	private ModConfigSpec.IntValue townUpdateFreqValue;
	private ModConfigSpec.DoubleValue archerRangeValue;
	private ModConfigSpec.IntValue npcActionRangeValue;
	private ModConfigSpec.BooleanValue vanillaEquipmentDropRateValue;
	private ModConfigSpec.BooleanValue repackCreativeOnlyValue;
	private ModConfigSpec.BooleanValue persistOrdersOnDeathValue;
	private ModConfigSpec.IntValue foodMultiplierValue;
	private ModConfigSpec.ConfigValue<List<? extends String>> foodValuesValue;
	private ModConfigSpec.ConfigValue<List<? extends String>> factionStandingsValue;

	/*
	 * client spec
	 */
	private static final ModConfigSpec CLIENT_SPEC;
	private static final Map<String, ModConfigSpec.BooleanValue> CLIENT_VALUES = new HashMap<>();
	private static final String CLIENT_FILE_NAME = "ancientwarfare-npc-client.toml";

	static {
		ModConfigSpec.Builder client = new ModConfigSpec.Builder();
		client.comment("Client Options\n" + "Affect only client-side operations.  Many of these options can be set from the in-game Options GUI.\n" + "Server admins can ignore these settings.").push(clientOptions);
		CLIENT_VALUES.put(LOAD_DEFAULT_SKIN_PACK, client.comment("Load Default Skin Pack\nDefault=true\n" + "If true, default skin pack will be loaded.\n" + "If false, default skin pack will NOT be loaded -- you will need to supply your own\n" + "skin packs or all npcs will use the default skin.").define(LOAD_DEFAULT_SKIN_PACK, loadDefaultSkinPack));
		CLIENT_VALUES.put(RENDER_AI, client.define(RENDER_AI, true));
		CLIENT_VALUES.put(RENDER_WORK_POINTS, client.define(RENDER_WORK_POINTS, true));
		CLIENT_VALUES.put(RENDER_FRIENDLY_NAMES, client.define(RENDER_FRIENDLY_NAMES, false));
		CLIENT_VALUES.put(RENDER_HOSTILE_NAMES, client.define(RENDER_HOSTILE_NAMES, false));
		CLIENT_VALUES.put(RENDER_FRIENDLY_HEALTH, client.define(RENDER_FRIENDLY_HEALTH, true));
		CLIENT_VALUES.put(RENDER_HOSTILE_HEALTH, client.define(RENDER_HOSTILE_HEALTH, true));
		CLIENT_VALUES.put(RENDER_TEAM_COLORS, client.define(RENDER_TEAM_COLORS, true));
		client.pop();
		CLIENT_SPEC = client.build();
	}

	public AWNPCStatics(String mod) {
		super(mod);
	}

	@Override
	public void register(IEventBus modBus, ModContainer container) {
		super.register(modBus, container);
		container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, CLIENT_FILE_NAME);
		modBus.addListener((ModConfigEvent.Loading evt) -> onClientConfigEvent(evt));
		modBus.addListener((ModConfigEvent.Reloading evt) -> onClientConfigEvent(evt));
	}

	private static void onClientConfigEvent(ModConfigEvent evt) {
		if (evt.getConfig().getSpec() == CLIENT_SPEC) {
			loadClientValues();
		}
	}

	private static void loadClientValues() {
		loadDefaultSkinPack = CLIENT_VALUES.get(LOAD_DEFAULT_SKIN_PACK).get();
		renderAI = CLIENT_VALUES.get(RENDER_AI).get();
		renderWorkPoints = CLIENT_VALUES.get(RENDER_WORK_POINTS).get();
		renderFriendlyNames = CLIENT_VALUES.get(RENDER_FRIENDLY_NAMES).get();
		renderHostileNames = CLIENT_VALUES.get(RENDER_HOSTILE_NAMES).get();
		renderFriendlyHealth = CLIENT_VALUES.get(RENDER_FRIENDLY_HEALTH).get();
		renderHostileHealth = CLIENT_VALUES.get(RENDER_HOSTILE_HEALTH).get();
		renderTeamColors = CLIENT_VALUES.get(RENDER_TEAM_COLORS).get();
	}

	public static ModConfigSpec getClientConfig() {
		return CLIENT_SPEC;
	}

	/*
	 * Port addition for the options GUI (1.12: forge config GUI editing the Property objects): sets and saves one of
	 * the client options (names: the RENDER_... / LOAD_DEFAULT_SKIN_PACK constants). Client side only.
	 */
	public static void setClientOption(String name, boolean value) {
		ModConfigSpec.BooleanValue configValue = CLIENT_VALUES.get(name);
		if (configValue == null) {
			AncientWarfareNPC.LOG.error("Unknown npc client option {}", name);
			return;
		}
		configValue.set(value);
		configValue.save();
		loadClientValues();
	}

	public static boolean getClientOption(String name) {
		ModConfigSpec.BooleanValue configValue = CLIENT_VALUES.get(name);
		return configValue != null && configValue.get();
	}

	@Override
	public void initializeCategories() {
		config.comment("General Options\n" + "Affect both client and server.  These configs must match for client and server, or\n" + "strange and probably BAD things WILL happen.").push(generalOptions).pop();

		config.comment("Server Options\n" + "Affect only server-side operations.  Will need to be set for dedicated servers, and single\n" + "player (or LAN worlds).  Clients playing on remote servers can ignore these settings.").push(serverOptions).pop();

		config.comment("Food Value Options\n" + "The value specified is the number of ticks that the item will feed the NPC for.\n" + "Add a new line for each item. The item type is not checked, and the default multiplier is not applied.\n" + "0 or under will make the item unusable as a food.\n" + "Affect only server-side operations.  Will need to be set for dedicated servers, and single\n" + "player (or LAN worlds).  Clients playing on remote servers can ignore these settings.").push(foodSettings).pop();

		config.comment("Faction Options\n" + "Set starting faction values, and alter the amount of standing gained/lost from player actions.\n" + "Affect only server-side operations.  Will need to be set for dedicated servers, and single\n" + "player (or LAN worlds).  Clients playing on remote servers can ignore these settings.").push(factionSettings).pop();
	}

	@Override
	public void initializeValues() {
		config.push(foodSettings);
		foodMultiplierValue = config.comment("Food items which don't have a custom duration time set will have their nourishing amount multiplied by this number, to get the number of ticks feeding the npc.").defineInRange("food_multiplier", 350, 0, Integer.MAX_VALUE / 10);
		foodValuesValue = config.comment("Food tick durations as \"<item id>=<ticks>\". Default food multiplier included.\n" + "Rotten flesh, poisonous potato and spider eye are rejected foods by default.")
				.defineListAllowEmpty("food_values", Arrays.asList("minecraft:apple=3000", "minecraft:mushroom_stew=4500", "minecraft:rotten_flesh=0", "minecraft:poisonous_potato=0", "minecraft:spider_eye=0"), () -> "", AWNPCStatics::isKeyIntPair);
		config.pop();

		config.push(factionSettings);
		factionStandingsValue = config.comment("Default faction standings for new players joining a game as \"<faction>=<standing>\".\n" + "Less than 0 will be hostile, greater than or equal to zero will be neutral/friendly.\n" + "Players will need to trade with faction-owned traders to improve their standing to become friendly.\n" + "Factions that are not listed use the default of the faction registry.")
				.defineListAllowEmpty("starting_faction_standings", List.of(), () -> "", AWNPCStatics::isKeyIntPair);
		config.pop();

		config.push(serverOptions);
		maxNpcLevelValue = config.comment("[DEPRECATED] Max NPC Level\nDefault=" + maxNpcLevel + "\n" + "How high can NPCs level up?  Npcs gain more health, attack damage, and overall\n" + "improved stats with each level.  Levels can go very high, but higher values may\n" + "result in overpowered NPCs once leveled up.").defineInRange("npc_max_level", maxNpcLevel, Integer.MIN_VALUE, Integer.MAX_VALUE);

		maxNpcWorkerLevelValue = config.comment("Max Worker NPC Level\nDefault=" + maxNpcWorkerLevel + "\n" + "How high Worker can NPCs level up?  Npcs gain more health and overall\n" + "improved stats with each level.  Levels can go very high, but higher values may\n" + "result in overpowered NPCs once leveled up.").defineInRange("npc_worker_max_level", maxNpcWorkerLevel, Integer.MIN_VALUE, Integer.MAX_VALUE);

		maxNpcCombatLevelValue = config.comment("Max Combat NPC Level\nDefault=" + maxNpcCombatLevel + "\n" + "How high Combat can NPCs level up?  Npcs gain more health, attack damage, and overall\n" + "improved stats with each level.  Levels can go very high, but higher values may\n" + "result in overpowered NPCs once leveled up.").defineInRange("npc_combat_max_level", maxNpcCombatLevel, Integer.MIN_VALUE, Integer.MAX_VALUE);

		npcXpFromAttackValue = config.comment("XP Per Attack\nDefault=" + npcXpFromAttack + "\n" + "How much xp should an NPC gain each time they damage but do not kill an enemy?\n" + "Higher values will result in faster npc leveling.\n" + "Applies to both player-owned and faction-based NPCs.").defineInRange("npc_xp_per_attack", npcXpFromAttack, Integer.MIN_VALUE, Integer.MAX_VALUE);

		npcXpFromKillValue = config.comment("XP Per Kill\nDefault=" + npcXpFromKill + "\n" + "How much xp should an NPC gain each time they kill an enemy?\n" + "Higher values will result in faster npc leveling.\n" + "Applies to both player-owned and faction-based NPCs.").defineInRange("npc_xp_per_kill", npcXpFromKill, Integer.MIN_VALUE, Integer.MAX_VALUE);

		npcLevelDamageMultiplierValue = config.comment("NPC damage bonus per npc level\nDefault=" + npcLevelDamageMultiplier + "\n" + "By default at level 10 they do 2x the damage as at lvl 0.\n" + "Higher values will result in more damage.\n" + "Applies to player-owned and faction-based NPCs.").defineInRange("npc_level_damage_multiplier", npcLevelDamageMultiplier, -Double.MAX_VALUE, Double.MAX_VALUE);

		npcXpFromTradeValue = config.comment("XP Per Trade\nDefault=" + npcXpFromTrade + "\n" + "How much xp should an NPC gain each time successfully traded with?\n" + "Higher values will result in faster npc leveling and unlock more trade recipes.\n" + "Applies to both player-owned and faction-based NPCs.").defineInRange("npc_xp_per_trade", npcXpFromTrade, Integer.MIN_VALUE, Integer.MAX_VALUE);

		npcXpFromWorkValue = config.comment("XP Per Work\nDefault=" + npcXpFromWork + "\n" + "How much xp should an NPC gain each time it works at a worksite?\n" + "Higher values will result in faster npc leveling.\n" + "Applies to player-owned NPCs only.").defineInRange("npc_xp_per_work", npcXpFromWork, Integer.MIN_VALUE, Integer.MAX_VALUE);

		npcXpFromMoveItemValue = config.comment("XP Per Courier\nDefault=" + npcXpFromMoveItem + "\n" + "How much xp should an NPC gain each time it moves an item?\n" + "Higher values will result in faster npc leveling.\n" + "Applies to player-owned NPCs only.").defineInRange("npc_xp_per_item_moved", npcXpFromMoveItem, Integer.MIN_VALUE, Integer.MAX_VALUE);

		npcWorkTicksValue = config.comment("Time Between Work Ticks\nDefault=" + npcWorkTicks + "\n" + "How many game ticks should pass between workers' processing work at a work-site.\n" + "Lower values result in more work output, higher values result in less work output.").defineInRange("npc_work_ticks", npcWorkTicks, Integer.MIN_VALUE, Integer.MAX_VALUE);

		npcAllowUpkeepAnyInventoryValue = config.comment("Allow NPC upkeep location at any inventory\nDefault=" + npcAllowUpkeepAnyInventory + "\n" + "By default, the Upkeep Order slip can be used to assign upkeep locations to any valid inventory block.\n" + "If set to false, only Town Hall blocks will be allowed as valid upkeep locations.").define("allow_upkeep_any_inventory", npcAllowUpkeepAnyInventory);

		townMaxRangeValue = config.comment("Town Hall Max Activation Range\nDefault=" + townMaxRange + "\n" + "How many blocks can a Town Hall be away from an NPC, while still detecting their death for possible resurrection.\n" + "This is a maximum, for server efficiency sake. Lower individual values can be setup from each block interaction GUI.").defineInRange("town_hall_max_range", townMaxRange, Integer.MIN_VALUE, Integer.MAX_VALUE);

		townUpdateFreqValue = config.comment("Default=" + townUpdateFreq + "\n" + "How many game ticks should pass between Town Hall updates." + "This affect how an NPC can change its selected Town Hall by moving to different places.\n" + "Lower values will make an NPC change its Town Hall faster, but is more costly for a server.\n").defineInRange("town_hall_ticks", townUpdateFreq, Integer.MIN_VALUE, Integer.MAX_VALUE);

		archerRangeValue = config.comment("Archer attack range\nDefault=" + archerRange + "\n" + "Attack range of all archers, except mounted archers who are half of this value.").defineInRange("archer_attack_range", archerRange, -Double.MAX_VALUE, Double.MAX_VALUE);
		config.pop();

		config.push("improvements");
		improvedPathReuseValue = config.comment("Reuse paths\nDefault=true\n" + "The original mod calculates a new path on almost every tick while an NPC walks somewhere.\n" + "With this on, an NPC keeps its path while it still leads to the same place (and is at most 2 seconds old, and the NPC is not stuck).\n" + "Set to false for the original behaviour.").define("reuse_paths", improvedPathReuse);
		improvedSkipUnreachableWorksitesValue = config.comment("Skip unreachable worksites\nDefault=true\n" + "The original mod lets a worker without a work order walk towards the closest worksite forever, even if it cannot get there.\n" + "With this on, a worker that has not come closer for 10 seconds gives that worksite up for a minute and looks for another one.\n" + "Set to false for the original behaviour.").define("skip_unreachable_worksites", improvedSkipUnreachableWorksites);
		improvedMedicTriageValue = config.comment("Medics heal the worst wounded first\nDefault=true\n" + "The original mod makes medics and priests heal the nearest injured NPC or player, however light the wound.\n" + "With this on, they pick the one with the least health left (relative to its maximum) among those in range.\n" + "Set to false for the original behaviour.").define("medic_heals_worst_first", improvedMedicTriage);
		improvedUpkeepCountingValue = config.comment("Upkeep counts only food that was really taken\nDefault=true\n" + "The original mod has a counting error: when an NPC takes its upkeep from a town hall or another mod inventory,\n" + "one single food item fills its whole food bar, because it keeps counting after the slot is empty.\n" + "(Vanilla chests were counted correctly.) With this on, an NPC only gets the food value of the items it removes.\n" + "Set to false for the original behaviour.").define("upkeep_counts_only_taken_food", improvedUpkeepCounting);
		improvedNpcLevelEditorValue = config.comment("NPC level in the screens\nDefault=true\n" + "The original mod shows the level of an NPC nowhere and has no way to change it.\n" + "With this on, the NPC inventory screen shows the level, and the creative controls screen shows level and experience\n" + "and has a field to set the level (creative mode only, 0 to the maximum level of the NPC).\n" + "Set to false for the original screens; the server then ignores a level sent by a client.").define("npc_level_editor", improvedNpcLevelEditor);
		config.pop();

		config.push(generalOptions);
		npcActionRangeValue = config.comment("Action Range\nDefault=" + npcActionRange + "\n" + "The range in blocks that an NPC can perform an action on something. The player has an action\n" + "range of 5. Only affects workers, no effect on the attack range of combat units nor medics.\n" + "Minimum value of 3 unless you want NPC's to bug-out and get stuck at random.").defineInRange("npc_action_range", npcActionRange, Integer.MIN_VALUE, Integer.MAX_VALUE);

		vanillaEquipmentDropRateValue = config.comment("Vanilla equipment drop rate for faction NPCs\nMakes Faction NPCs drop their items by the Vanilla Equipment Drop rate (8,5%)\nDefault=" + vanillaEquipmentDropRate + "\n" + "If set to false, the NPCs will ALWAYS drop ALL equipment they have (100%) rate").define("npc_vanilla_equipment_drop_rate", vanillaEquipmentDropRate);

		repackCreativeOnlyValue = config.comment("Repack only available for Creative players?\nDefault=" + repackCreativeOnly + "\n" + "If true, the 'Repack' option for NPC's will be unavailable outside of Creative mode.").define("npc_repack_creative_only", repackCreativeOnly);

		persistOrdersOnDeathValue = config.comment("NPC's will keep orders items on death?\nDefault=" + persistOrdersOnDeath + "\n" + "If true, an NPC who dies and manages to notify a nearby town hall will keep their orders items on their body. So if/when a priest resurrects them, they will have the orders items on them still. If there is no Town Hall nearby to catch the death however, the will drop on the ground as normal.").define("npc_death_keep_orders_items", persistOrdersOnDeath);
		config.pop();
	}

	private static boolean isKeyIntPair(Object o) {
		if (!(o instanceof String)) {
			return false;
		}
		String s = (String) o;
		int index = s.lastIndexOf('=');
		if (index <= 0) {
			return false;
		}
		try {
			Integer.parseInt(s.substring(index + 1).trim());
			return true;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	private static void readKeyIntPairs(List<? extends String> list, Map<String, Integer> map) {
		map.clear();
		for (String entry : list) {
			int index = entry.lastIndexOf('=');
			if (index <= 0) {
				continue;
			}
			try {
				map.put(entry.substring(0, index).trim(), Integer.parseInt(entry.substring(index + 1).trim()));
			}
			catch (NumberFormatException e) {
				AncientWarfareNPC.LOG.error("Invalid npc config entry {}", entry);
			}
		}
	}

	@Override
	protected void loadValues() {
		loadFoodValues();

		maxNpcLevel = maxNpcLevelValue.get();
		maxNpcWorkerLevel = maxNpcWorkerLevelValue.get();
		maxNpcCombatLevel = maxNpcCombatLevelValue.get();
		npcXpFromAttack = npcXpFromAttackValue.get();
		npcXpFromKill = npcXpFromKillValue.get();
		npcLevelDamageMultiplier = npcLevelDamageMultiplierValue.get();
		npcXpFromTrade = npcXpFromTradeValue.get();
		npcXpFromWork = npcXpFromWorkValue.get();
		npcXpFromMoveItem = npcXpFromMoveItemValue.get();
		npcWorkTicks = npcWorkTicksValue.get();
		npcAllowUpkeepAnyInventory = npcAllowUpkeepAnyInventoryValue.get();
		improvedPathReuse = improvedPathReuseValue.get();
		improvedSkipUnreachableWorksites = improvedSkipUnreachableWorksitesValue.get();
		improvedMedicTriage = improvedMedicTriageValue.get();
		improvedUpkeepCounting = improvedUpkeepCountingValue.get();
		improvedNpcLevelEditor = improvedNpcLevelEditorValue.get();
		townMaxRange = townMaxRangeValue.get();
		townUpdateFreq = townUpdateFreqValue.get();
		archerRange = archerRangeValue.get();
		npcActionRange = npcActionRangeValue.get();
		vanillaEquipmentDropRate = vanillaEquipmentDropRateValue.get();
		repackCreativeOnly = repackCreativeOnlyValue.get();
		persistOrdersOnDeath = persistOrdersOnDeathValue.get();

		readKeyIntPairs(factionStandingsValue.get(), factionStartingStandings);
	}

	public int getPlayerDefaultStanding(String factionName) {
		FactionDefinition.StandingSettings standingSettings = FactionRegistry.getFaction(factionName).getStandingSettings();
		if (!standingSettings.canPlayerStandingChange()) {
			return standingSettings.getPlayerDefaultStanding();
		}
		Integer configured = factionStartingStandings.get(factionName);
		return configured != null ? configured : standingSettings.getPlayerDefaultStanding();
	}

	private void loadFoodValues() {
		foodMultiplier = foodMultiplierValue.get();
		readKeyIntPairs(foodValuesValue.get(), foodValues);
	}

	/*
	 * returns the food value for a single size stack of the input item stack
	 */
	public int getFoodValue(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		if (foodValues.containsKey(name)) {
			return foodValues.get(name);
		}
		FoodProperties food = stack.getFoodProperties(null);
		if (food != null) {
			int healAmount = food.nutrition();
			float saturationModifier = healAmount == 0 ? 0 : food.saturation() / (healAmount * 2F);
			return (healAmount + healAmount * (int) (saturationModifier * 2F)) * foodMultiplier;
		}
		return 0;
	}

	@Override
	public void save() {
		super.save();
	}
}
