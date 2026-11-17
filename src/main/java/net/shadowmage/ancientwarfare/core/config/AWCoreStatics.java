package net.shadowmage.ancientwarfare.core.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Port notes: file is config/ancientwarfare-core.toml. 1.12 created one boolean per item on demand in the categories
 * "08_recipe_details" and "09_recipe_research_details"; a ModConfigSpec cannot grow keys at runtime, so both
 * categories hold a list of item ids instead (items listed are disabled / need no research; everything else keeps
 * the 1.12 default "true").
 */
public class AWCoreStatics extends ModConfiguration {

	public static final String KEY_ALT_ITEM_USE_1 = "keybind.awCore.alt.item.use.1";
	public static final String KEY_ALT_ITEM_USE_2 = "keybind.awCore.alt.item.use.2";
	public static final String KEY_ALT_ITEM_USE_3 = "keybind.awCore.alt.item.use.3";
	public static final String KEY_ALT_ITEM_USE_4 = "keybind.awCore.alt.item.use.4";
	public static final String KEY_ALT_ITEM_USE_5 = "keybind.awCore.alt.item.use.5";

	public static boolean DEBUG = true;

	public static final String resourcePath = "/assets/ancientwarfare/resources/";
	public static final String utilsExportPath = AWCoreStatics.configPathForFiles + "/export/";

	/*
	 * category names
	 */
	private static final String researchSettings = "06_research";
	private static final String recipeDetailSettings = "08_recipe_details";
	private static final String recipeResearchDetails = "09_recipe_research_details";

	/*
	 * research options
	 */
	public static boolean useResearchSystem = true;
	public static boolean enableResearchResourceUse = true;
	public static double energyPerResearchUnit = 1D;
	public static double researchPerTick = 1;//TODO add to config

	/*
	 * server options
	 */
	public static boolean fireBlockBreakEvents = true;
	public static boolean includeResearchInChests = true;
	public static double energyPerWorkUnit = 50D;

	private static final Set<String> disabledRecipeItems = new HashSet<>();
	private static final Set<String> researchDisabledItems = new HashSet<>();

	private ModConfigSpec.BooleanValue debugValue;
	private ModConfigSpec.BooleanValue fireBlockBreakEventsValue;
	private ModConfigSpec.BooleanValue includeResearchInChestsValue;
	private ModConfigSpec.DoubleValue energyPerWorkUnitValue;
	private ModConfigSpec.DoubleValue energyPerResearchUnitValue;
	private ModConfigSpec.BooleanValue useResearchSystemValue;
	private ModConfigSpec.BooleanValue enableResearchResourceUseValue;
	private ModConfigSpec.ConfigValue<List<? extends String>> disabledRecipeItemsValue;
	private ModConfigSpec.ConfigValue<List<? extends String>> researchDisabledItemsValue;

	public AWCoreStatics(String modid) {
		super(modid);
	}

	@Override
	public void initializeValues() {
		config.push(generalOptions);
		debugValue = config.comment("Enable extra debug console output and runtime checks.\n" + "Can degrade performance if left on and lead to large log files.").define("debug_ouput", false);
		config.pop();

		config.push(serverOptions);
		fireBlockBreakEventsValue = config.comment("Fire Block Break Events If set to false, block-break-events will not be posted for _any_ operations\n" + "effectively negating any block-protection mods/mechanims in place on the server.\n" + "If left at true, block-break events will be posted for any automation or vehicles\n" + "which are changing blocks in the world.  Most will use a reference to their owners-name\n" + "for permissions systems.").define("fire_block_break_events", fireBlockBreakEvents);
		includeResearchInChestsValue = config.comment("Include Research In Dungeon Loot Chests\n" + "If set to true, Research Note items will be added to dungeon-chest loot tables.\n" + "If set to false, no research will be added.\n" + "This is the global setting.  Individual research may be toggled in the Research\n" + "section of the config file.").define("include_research_in_chests", includeResearchInChests);
		energyPerWorkUnitValue = config.comment("Energy Per Work Unit\nDefault = 50\n" + "How much Torque energy is generated per worker work tick.\n" + "This is the base number and is further adjusted per worker by worker effectiveness.\n" + "Setting to 0 or below effectively disables  workers.").defineInRange("energy_per_work_unit", energyPerWorkUnit, -Double.MAX_VALUE, Double.MAX_VALUE);
		config.pop();

		config.push(researchSettings);
		energyPerResearchUnitValue = config.comment("Energy Per Research Unit\nDefault = 1\n" + "How much energy is consumed per research tick.\n" + "Research generally ticks every game-tick if being worked at.\n" + "Setting to 0 will eliminate the energy/worker requirements for research.\n" + "Setting to higher than 1 will increase the amount of energy needed for research,\n" + "increasing the amount of time/resources required for all research.").defineInRange("energy_used_per_research_tick", energyPerResearchUnit, -Double.MAX_VALUE, Double.MAX_VALUE);
		useResearchSystemValue = config.comment("If set to false, research system will be disabled and all recipes will be available in normal crafting station.").define("use_research_system", useResearchSystem);
		enableResearchResourceUseValue = config.comment("If set to false, research system will not use resources for research.").define("use_research_resources", enableResearchResourceUse);
		config.pop();

		config.push(recipeDetailSettings);
		disabledRecipeItemsValue = config.comment("Item ids (e.g. \"ancientwarfare:iron_hammer\") whose Ancient Warfare recipe is disabled.").defineListAllowEmpty("disabled_recipes", new ArrayList<>(), () -> "", o -> o instanceof String);
		config.pop();

		config.push(recipeResearchDetails);
		researchDisabledItemsValue = config.comment("Item ids (e.g. \"ancientwarfare:iron_hammer\") whose recipe needs no research.").defineListAllowEmpty("no_research_recipes", new ArrayList<>(), () -> "", o -> o instanceof String);
		config.pop();
	}

	@Override
	public void initializeCategories() {
		config.comment("General Options\n" + "Affect both client and server.  These configs must match for client and server, or\n" + "strange and probably BAD things WILL happen.").push(generalOptions).pop();
		config.comment("Server Options\n" + "Affect only server-side operations.  Will need to be set for dedicated servers, and single\n" + "player (or LAN worlds).  Clients playing on remote servers can ignore these settings.").push(serverOptions).pop();
		config.comment("Research Settings Section\n" + "Affect both client and server.  These configs must match for client and server, or\n" + "strange and probably BAD things WILL happen.").push(researchSettings).pop();
		config.comment("Recipe Detail Settings Section\n" + "Configure recipe enable/disable per item.\n" + "Disabling the recipe effectively disables that item.\n" + "Affect both client and server.  These configs must match for client and server, or\n" + "strange and probably BAD things WILL happen.").push(recipeDetailSettings).pop();
		config.comment("Recipe Research Detail Settings Section\n" + "Configure enable/disable research for specific recipes.\n" + "Disabling the research removes all research requirements for that item.\n" + "Affect both client and server.  These configs must match for client and server, or\n" + "strange and probably BAD things WILL happen.").push(recipeResearchDetails).pop();
	}

	@Override
	protected void loadValues() {
		DEBUG = debugValue.get();
		fireBlockBreakEvents = fireBlockBreakEventsValue.get();
		includeResearchInChests = includeResearchInChestsValue.get();
		energyPerWorkUnit = energyPerWorkUnitValue.get();
		energyPerResearchUnit = energyPerResearchUnitValue.get();
		useResearchSystem = useResearchSystemValue.get();
		enableResearchResourceUse = enableResearchResourceUseValue.get();
		disabledRecipeItems.clear();
		disabledRecipeItems.addAll(disabledRecipeItemsValue.get());
		researchDisabledItems.clear();
		researchDisabledItems.addAll(researchDisabledItemsValue.get());
	}

	public static boolean isItemCraftable(Item item) {
		String name = BuiltInRegistries.ITEM.getKey(item).toString();
		return !disabledRecipeItems.contains(name);
	}

	public static boolean isItemResearcheable(Item item) {
		String name = BuiltInRegistries.ITEM.getKey(item).toString();
		return !researchDisabledItems.contains(name);
	}

	public static void update() {
		AncientWarfareCore.statics.save();
	}
}
