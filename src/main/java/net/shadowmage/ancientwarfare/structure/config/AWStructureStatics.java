package net.shadowmage.ancientwarfare.structure.config;

import net.minecraft.world.entity.Mob;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.shadowmage.ancientwarfare.core.config.ModConfiguration;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Port notes: file is config/ancientwarfare-structure.toml, same categories, keys and scalar defaults as the 1.12
 * AncientWarfareStructures.cfg.
 * - The 1.12 Material tests (isSkippableMaterial / isValidTargetMaterial) are block tag / block class tests now
 *   (materials are gone): skippable = air, plants, vines, leaves, wood (logs, planks and other wooden blocks that
 *   burn), gourds, cactus; valid target = grass, dirt, stone, snow, ice, sand, water.
 * - The four 1.12 default lists (world gen skippable blocks, world gen target blocks, scanner skipped blocks,
 *   excluded spawner entities, excluded loot tables) were several hundred 1.12 ids of 1.12 mods (BiomesOPlenty,
 *   Natura, Thaumcraft ...). They are NOT carried over: the lists are still read from the config, with an empty
 *   default. The vanilla 1.12 entries of the skippable / target lists are covered by the tag tests above.
 * - The world generation options are kept (and read), world generation itself is not ported yet.
 */
public class AWStructureStatics extends ModConfiguration {

	public static Set<String> lootTableExclusions = new HashSet<>();

	public AWStructureStatics(String mod) {
		super(mod);
	}

	public static float chunkClusterValue = 1f;
	public static String templateExtension = "aws";
	public static String townTemplateExtension = "awt";
	public static boolean enableWorldGen = true;
	public static boolean enableTownGeneration = true;
	public static boolean enableStructureGeneration = true;
	public static boolean loadDefaultPack = true;
	public static float randomGenerationChance = 0.075f;
	public static int spawnProtectionRange = 400;
	public static int townClosestDistance = 90 * 16;
	public static float townGenerationChance = 0.125f;
	public static int townSearchHeightDrop = 12;
	public static float townSearchWaterFraction = 0.25f;
	public static float townSearchRoughFraction = 0.25f;
	public static Set<String> excludedSpawnerEntities = new HashSet<>();
	private static HashSet<String> skippableWorldGenBlocks = new HashSet<>();
	private static HashSet<String> worldGenTargetBlocks = new HashSet<>();
	private static HashSet<String> scannerSkippedBlocks = new HashSet<>();
	public static boolean collectWorldGenStatistics = true;
	//improvements over the 1.12 behaviour (not baseline); each can be switched back to the original behaviour
	public static boolean budgetedPlacement = true;
	public static int placementMsPerTick = 10;
	public static boolean backgroundAttemptChunks = true;

	public static int gateVerticalWoodenMaxHealth = 80;
	public static int gateVerticalIronMaxHealth = 80;
	public static int gateSingleWoodMaxHealth = 80;
	public static int gateSingleIronMaxHealth = 80;
	public static int gateDoubleWoodMaxHealth = 80;
	public static int gateDoubleIronMaxHealth = 80;
	public static int drawbridgeMaxHealth = 80;

	private static final String worldGenCategory = "a_world-gen_settings";
	private static final String villageGenCategory = "b_village-gen_settings";
	private static final String excludedEntitiesCategory = "c_excluded_spawner_entities";
	private static final String worldGenBlocks = "d_world_gen_skippable_blocks";
	private static final String targetBlocks = "e_world_gen_target_blocks";
	private static final String scanSkippedBlocks = "f_scanner_skipped_blocks";
	private static final String excludedLootTables = "g_excluded_loot_tables";

	//no initialisers: assigned from the super constructor (initializeValues)
	private ModConfigSpec.ConfigValue<String> templateExtensionValue;
	private ModConfigSpec.BooleanValue enableStructureGenerationValue;
	private ModConfigSpec.BooleanValue enableTownGenerationValue;
	private ModConfigSpec.BooleanValue loadDefaultPackValue;
	private ModConfigSpec.DoubleValue randomGenerationChanceValue;
	private ModConfigSpec.DoubleValue chunkClusterValueValue;
	private ModConfigSpec.IntValue spawnProtectionRangeValue;
	private ModConfigSpec.BooleanValue enableWorldGenValue;
	private ModConfigSpec.IntValue townClosestDistanceValue;
	private ModConfigSpec.DoubleValue townGenerationChanceValue;
	private ModConfigSpec.IntValue townSearchHeightDropValue;
	private ModConfigSpec.DoubleValue townSearchWaterFractionValue;
	private ModConfigSpec.DoubleValue townSearchRoughFractionValue;
	private ModConfigSpec.IntValue gateVerticalWoodenMaxHealthValue;
	private ModConfigSpec.IntValue gateVerticalIronMaxHealthValue;
	private ModConfigSpec.IntValue gateSingleWoodMaxHealthValue;
	private ModConfigSpec.IntValue gateSingleIronMaxHealthValue;
	private ModConfigSpec.IntValue gateDoubleWoodMaxHealthValue;
	private ModConfigSpec.IntValue gateDoubleIronMaxHealthValue;
	private ModConfigSpec.IntValue drawbridgeMaxHealthValue;
	private ModConfigSpec.BooleanValue collectWorldGenStatisticsValue;
	private ModConfigSpec.BooleanValue budgetedPlacementValue;
	private ModConfigSpec.IntValue placementMsPerTickValue;
	private ModConfigSpec.BooleanValue backgroundAttemptChunksValue;
	private ModConfigSpec.ConfigValue<List<? extends String>> lootTableExclusionsValue;
	private ModConfigSpec.ConfigValue<List<? extends String>> skippableBlocksValue;
	private ModConfigSpec.ConfigValue<List<? extends String>> targetBlocksValue;
	private ModConfigSpec.ConfigValue<List<? extends String>> scannerSkippedBlocksValue;
	private ModConfigSpec.ConfigValue<List<? extends String>> excludedEntitiesValue;

	@Override
	public void initializeCategories() {
		//the category comments are written together with the values (a ModConfigSpec category is created by push)
	}

	private ModConfigSpec.IntValue integer(String name, int def, String comment) {
		return config.comment(comment).defineInRange(name, def, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	private ModConfigSpec.DoubleValue dbl(String name, double def, String comment) {
		return config.comment(comment).defineInRange(name, def, -Double.MAX_VALUE, Double.MAX_VALUE);
	}

	private ModConfigSpec.ConfigValue<List<? extends String>> list(String name, String comment) {
		return config.comment(comment).defineListAllowEmpty(name, new ArrayList<String>(), () -> "", o -> o instanceof String);
	}

	@Override
	public void initializeValues() {
		config.comment("Settings that effect all world-structure-generation.").push(worldGenCategory);
		templateExtensionValue = config.comment("Default=" + templateExtension + "\n" + "The template extension used when looking for and exporting templates.\n" + "Only files matching this extension will be examined.").define("template_extension", "aws");
		enableStructureGenerationValue = config.comment("Default=" + enableStructureGeneration + "\n" + "Enable or disable structure generation entirely.\n" + "If disabled, no structures will be generated.").define("enable_structure_generation", enableStructureGeneration);
		enableTownGenerationValue = config.comment("Default=" + enableTownGeneration + "\n" + "Enable or disable town generation entirely.\n" + "If disabled, no towns will be generated.").define("enable_town_generation", enableTownGeneration);
		loadDefaultPackValue = config.comment("If true the default structure pack will be loaded and enabled for world-gen.").define("load_default_structure_pack", loadDefaultPack);
		randomGenerationChanceValue = dbl("random_generation_chance", randomGenerationChance, "Default=" + randomGenerationChance + "\n" + "Accepts values between 0 and 1.\n" + "Determines the chance that a structure will be attempted to be generated in any given chunk.");
		chunkClusterValueValue = dbl("chunk_cluster_value", chunkClusterValue, "Default=" + chunkClusterValue + "\n" + "Accepts values greater than 0.\n" + "Determines how many cluster value points a chunk can hold.");
		spawnProtectionRangeValue = integer("spawn_protection_radius", spawnProtectionRange, "Default=" + spawnProtectionRange + "\n" + "Determines the area around the central spawn coordinate that will be excluded from random structure generation.");
		townClosestDistanceValue = integer("town_min_distance", townClosestDistance, "Default=" + townClosestDistance + "\n" + "Minimum distance between towns.");
		townGenerationChanceValue = dbl("town_generation_chance", townGenerationChance, "Default=" + townGenerationChance + "\n" + "Accepts values between 0 and 1.\n" + "Determines the chance that a town will be attempted to be generated in any given chunk.");
		townSearchHeightDropValue = integer("town_search_height_drop", townSearchHeightDrop, "Default=" + townSearchHeightDrop + "\n" + "How many blocks a chunk may lie below the spot a town search starts from and still become part of the town area (the area is levelled when the town is built).\n" + "The original mod used 3; modern terrain is rougher, with 3 towns almost never find room.");
		townSearchWaterFractionValue = dbl("town_search_water_fraction", townSearchWaterFraction, "Default=" + townSearchWaterFraction + "\n" + "Accepts values between 0 and 1.\n" + "Share of a chunk that may be deep water (rivers, ponds) and still become part of a town area; the water is filled in when the town is built.\n" + "The original mod used 0.");
		townSearchRoughFractionValue = dbl("town_search_rough_fraction", townSearchRoughFraction, "Default=" + townSearchRoughFraction + "\n" + "Accepts values between 0 and 1.\n" + "Share of the chunks of a town area that may be too low, too high or under water; they are cut down or filled in when the town is built.\n" + "The original mod used 0 (every chunk had to fit). Lower values give fewer towns on flatter ground.");
		gateVerticalWoodenMaxHealthValue = integer("gate_vertical_wooden_max_health", gateVerticalWoodenMaxHealth, "Default=" + gateVerticalWoodenMaxHealth + "\n" + "Determines the max health of vertical wooden gates.");
		gateVerticalIronMaxHealthValue = integer("gate_vertical_iron_max_health", gateVerticalIronMaxHealth, "Default=" + gateVerticalIronMaxHealth + "\n" + "Determines the max health of vertical iron gates.");
		gateSingleWoodMaxHealthValue = integer("gate_single_wood_max_health", gateSingleWoodMaxHealth, "Default=" + gateSingleWoodMaxHealth + "\n" + "Determines the max health of single slide wooden gates.");
		gateSingleIronMaxHealthValue = integer("gate_single_iron_max_health", gateSingleIronMaxHealth, "Default=" + gateSingleIronMaxHealth + "\n" + "Determines the max health of single slide iron gates.");
		gateDoubleWoodMaxHealthValue = integer("gate_double_wood_max_health", gateDoubleWoodMaxHealth, "Default=" + gateDoubleWoodMaxHealth + "\n" + "Determines the max health of double slide wooden gates.");
		gateDoubleIronMaxHealthValue = integer("gate_double_iron_max_health", gateDoubleIronMaxHealth, "Default=" + gateDoubleIronMaxHealth + "\n" + "Determines the max health of double slide iron gates.");
		drawbridgeMaxHealthValue = integer("drawbridge_max_health", drawbridgeMaxHealth, "Default=" + drawbridgeMaxHealth + "\n" + "Determines the max health of drawbridges.");
		collectWorldGenStatisticsValue = config.comment("If true worldgen statistics are collected.").define("collect_worldgen_statistics", collectWorldGenStatistics);
		config.pop();

		config.push("improvements");
		budgetedPlacementValue = config.comment("Budgeted structure placement\nDefault=" + budgetedPlacement + "\n" + "The original mod places a whole structure in one server tick (world generation, towns, the creative structure builder,\n" + "the build command); a large template stops the server for several seconds.\n" + "With this on, those structures are placed over several ticks, at most placement_ms_per_tick milliseconds per tick,\n" + "in the same order and with the same result; so are the terrain preparation of a town (biome, borders, levelling)\n" + "and the clearing / levelling / filling of a world generation structure's validator. Structures appear over a few seconds.\n" + "Set to false for the original behaviour.").define("budgeted_placement", budgetedPlacement);
		placementMsPerTickValue = config.comment("Milliseconds per tick for budgeted placement\nDefault=" + placementMsPerTick + "\n" + "Server tick time spent on placing queued structures per tick, shared by all structures waiting to be built\n" + "(a tick is 50 ms). Only used with budgeted_placement on.").defineInRange("placement_ms_per_tick", placementMsPerTick, 1, 1000);
		backgroundAttemptChunksValue = config.comment("Background chunk preparation for world generation attempts\nDefault=" + backgroundAttemptChunks + "\n" + "The original mod reads the terrain of a structure attempt (spawn height, the border of the structure) and of a town\n" + "search at once, so chunks that do not exist yet are generated on the server thread inside the attempt: single ticks\n" + "of up to several seconds. With this on, an attempt waits (at most 20 s, then it runs as before) until Minecraft's\n" + "chunk workers have generated the terrain it will read, and town searches get the heights of chunks that are not\n" + "generated yet from a background thread; then the unchanged attempt runs, in the same order.\n" + "Set to false for the original behaviour.").define("background_attempt_chunks", backgroundAttemptChunks);
		config.pop();

		config.push(serverOptions);
		enableWorldGenValue = config.comment("Default=" + enableWorldGen + "\n" + "Enable or disable world generation entirely.").define("enable_world_generation", enableWorldGen);
		config.pop();

		config.comment("Settings that effect the generation of vanilla villages.\nCurrently there are no village-generation options.").push(villageGenCategory).pop();
		config.comment("Entities that will not show up in the Mob Spawner Placer entity selection list.\nAdd any mobs here that will crash if spawned via the vanilla mob-spawner (usually complex NBT-defined entities).").push(excludedEntitiesCategory);
		excludedEntitiesValue = list("excluded_entities", "entity ids");
		config.pop();
		config.comment("Blocks that should be skipped/ignored during world gen -- should list all plant blocks/logs/foliage").push(worldGenBlocks);
		skippableBlocksValue = list("skippable_blocks", "block ids");
		config.pop();
		config.comment("List of target blocks that structures and towns can spawn on in addition to materials that are automatically valid.").push(targetBlocks);
		targetBlocksValue = list("target_blocks", "List of blocks that are valid");
		config.pop();
		config.comment("List of blocks that the structure scanner will completely ignore.\nWhenever these blocks are encountered the template will instead be filled with a hard-air rule.").push(scanSkippedBlocks);
		scannerSkippedBlocksValue = list("scanner_skipped_blocks", "Blocks TO be skipped by structure scanner");
		config.pop();
		config.comment("List of loot tables that should be excluded from loot chest placer GUI.\nLoot table names must be specified as fully-qualified name.").push(excludedLootTables);
		lootTableExclusionsValue = list("excluded_loot_tables", "loot table ids");
		config.pop();
	}

	@Override
	protected void loadValues() {
		templateExtension = templateExtensionValue.get();
		enableStructureGeneration = enableStructureGenerationValue.get();
		enableTownGeneration = enableTownGenerationValue.get();
		loadDefaultPack = loadDefaultPackValue.get();
		randomGenerationChance = (float) (double) randomGenerationChanceValue.get();
		chunkClusterValue = (float) (double) chunkClusterValueValue.get();
		spawnProtectionRange = spawnProtectionRangeValue.get();
		enableWorldGen = enableWorldGenValue.get();
		townClosestDistance = townClosestDistanceValue.get();
		townGenerationChance = (float) (double) townGenerationChanceValue.get();
		townSearchHeightDrop = townSearchHeightDropValue.get();
		townSearchWaterFraction = (float) (double) townSearchWaterFractionValue.get();
		townSearchRoughFraction = (float) (double) townSearchRoughFractionValue.get();
		gateVerticalWoodenMaxHealth = gateVerticalWoodenMaxHealthValue.get();
		gateVerticalIronMaxHealth = gateVerticalIronMaxHealthValue.get();
		gateSingleWoodMaxHealth = gateSingleWoodMaxHealthValue.get();
		gateSingleIronMaxHealth = gateSingleIronMaxHealthValue.get();
		gateDoubleWoodMaxHealth = gateDoubleWoodMaxHealthValue.get();
		gateDoubleIronMaxHealth = gateDoubleIronMaxHealthValue.get();
		drawbridgeMaxHealth = drawbridgeMaxHealthValue.get();
		collectWorldGenStatistics = collectWorldGenStatisticsValue.get();
		budgetedPlacement = budgetedPlacementValue.get();
		placementMsPerTick = placementMsPerTickValue.get();
		backgroundAttemptChunks = backgroundAttemptChunksValue.get();
		lootTableExclusions = new HashSet<>(lootTableExclusionsValue.get());
		skippableWorldGenBlocks = new HashSet<>(skippableBlocksValue.get());
		worldGenTargetBlocks = new HashSet<>(targetBlocksValue.get());
		scannerSkippedBlocks = new HashSet<>(scannerSkippedBlocksValue.get());
		excludedSpawnerEntities = new HashSet<>(excludedEntitiesValue.get());
	}

	public static void logSkippableBlocksCoveredByMaterial() {
		skippableWorldGenBlocks.stream().filter(b -> BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.ResourceLocation.tryParse(b))
						.map(block -> isSkippableMaterial(block.defaultBlockState())).orElse(false))
				.forEach(b -> AncientWarfareStructure.LOG.info("Skippable block already covered by material: {}", b));
	}

	public static boolean isValidTargetBlock(BlockState state) {
		//noinspection ConstantConditions
		return isValidTargetMaterial(state) || worldGenTargetBlocks.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
	}

	/* 1.12 Material GRASS, GROUND, ROCK, SNOW, ICE, PACKED_ICE, SAND, WATER */
	public static boolean isValidTargetMaterial(BlockState state) {
		return state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.CLAY) || state.is(Blocks.GRAVEL)
				|| state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER) || state.is(BlockTags.STONE_ORE_REPLACEABLES)
				|| state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || state.is(BlockTags.MINEABLE_WITH_PICKAXE) && state.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO) && !state.is(BlockTags.ICE)
				|| state.is(BlockTags.SNOW) || state.is(BlockTags.ICE) || state.is(BlockTags.SAND) || state.getFluidState().is(FluidTags.WATER) && state.liquid();
	}

	public static boolean isSkippable(BlockState state) {
		//noinspection ConstantConditions
		return isSkippableMaterial(state) || skippableWorldGenBlocks.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
	}

	/* 1.12 Material AIR, PLANTS, VINE, LEAVES, WOOD, GOURD, CACTUS */
	public static boolean isSkippableMaterial(BlockState state) {
		Block block = state.getBlock();
		return state.isAir() || block instanceof BushBlock || state.is(BlockTags.REPLACEABLE_BY_TREES) && !state.liquid() || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.CROPS)
				|| block instanceof VineBlock || state.is(BlockTags.CAVE_VINES) || state.is(BlockTags.LEAVES)
				|| state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.MINEABLE_WITH_AXE) && state.ignitedByLava()
				|| state.is(Blocks.PUMPKIN) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN) || state.is(Blocks.MELON)
				|| block instanceof CactusBlock || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BAMBOO);
	}

	public static boolean shouldSkipScan(Block block) {
		return scannerSkippedBlocks.contains(BuiltInRegistries.BLOCK.getKey(block).toString());
	}

	public static boolean withinProtectionRange(double dist) {
		return dist < spawnProtectionRange * spawnProtectionRange;
	}
}
