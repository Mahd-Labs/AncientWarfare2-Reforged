package net.shadowmage.ancientwarfare.structure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureMap;
import net.shadowmage.ancientwarfare.structure.gamedata.TownMap;
import net.shadowmage.ancientwarfare.structure.item.ItemTownBuilder;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.template.WorldGenStructureManager;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.validation.StructureValidationType;
import net.shadowmage.ancientwarfare.structure.template.build.validation.StructureValidator;
import net.shadowmage.ancientwarfare.structure.template.build.validation.properties.StructureValidationProperties;
import net.shadowmage.ancientwarfare.structure.template.scan.TemplateScanner;
import net.shadowmage.ancientwarfare.structure.town.TownBoundingArea;
import net.shadowmage.ancientwarfare.structure.town.TownTemplate;
import net.shadowmage.ancientwarfare.structure.town.TownTemplateManager;
import net.shadowmage.ancientwarfare.structure.town.TownTemplateParser;
import net.shadowmage.ancientwarfare.structure.town.WorldTownGenerator;
import net.shadowmage.ancientwarfare.structure.util.LegacyBiomes;
import net.shadowmage.ancientwarfare.structure.worldgen.Territory;
import net.shadowmage.ancientwarfare.structure.worldgen.TerritoryData;
import net.shadowmage.ancientwarfare.structure.worldgen.TerritoryManager;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldGenTickHandler;
import net.shadowmage.ancientwarfare.structure.worldgen.WorldStructureGenerator;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/*
 * World generation and town checks (run with ./gradlew runGameTestServer). The game test world is flat; the
 * structure / town of a test is generated far away from the grid of test areas (they are larger than a test area
 * and world generation picks its own position inside a chunk), the test area only provides the blocks the test
 * template is scanned from.
 */
@GameTestHolder(AncientWarfare.MOD_ID)
@PrefixGameTestTemplate(false)
public class WorldGenGameTests {
	private static final String TEMPLATE = "automation_empty";

	private static void check(GameTestHelper helper, boolean condition, String message) {
		if (!condition) {
			helper.fail(message);
		}
	}

	/*
	 * a 5 x 4 x 5 hut: stone brick floor, lapis corner posts, glowstone on top of the centre
	 */
	private static StructureTemplate scanHut(GameTestHelper helper, String name) {
		ServerLevel level = helper.getLevel();
		BlockPos min = helper.absolutePos(new BlockPos(2, 2, 2));
		BlockPos max = min.offset(4, 3, 4);
		for (BlockPos p : BlockPos.betweenClosed(min, max)) {
			BlockState state = Blocks.AIR.defaultBlockState();
			boolean corner = (p.getX() == min.getX() || p.getX() == max.getX()) && (p.getZ() == min.getZ() || p.getZ() == max.getZ());
			if (p.getY() == min.getY()) {
				state = Blocks.STONE_BRICKS.defaultBlockState();
			} else if (corner) {
				state = Blocks.LAPIS_BLOCK.defaultBlockState();
			} else if (p.getY() == max.getY() && p.getX() == min.getX() + 2 && p.getZ() == min.getZ() + 2) {
				state = Blocks.GLOWSTONE.defaultBlockState();
			}
			level.setBlock(p, state, 3);
		}
		//key at the middle of the front side, on floor level: offset y = 1, the floor replaces the ground block
		return TemplateScanner.scan(level, Collections.emptySet(), min, max, min.offset(2, 0, 0), 0, name);
	}

	/*
	 * a scan reuses one rule per block state. In 1.21 BlockState.getProperties() is the collection of
	 * property keys with identity equals, so the 1.12 comparison never matched and every block got its own rule
	 * (a 64 x 32 x 64 scan wrote 129,504 rules). The hut plus a row of stairs has 5 states, air included.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void scanReusesRulesOfEqualBlockStates(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos min = helper.absolutePos(new BlockPos(2, 2, 2));
		StructureTemplate first = scanHut(helper, "gametest_rule_reuse_hut");
		for (int x = 1; x <= 3; x++) {
			level.setBlock(min.offset(x, 1, 1), Blocks.OAK_STAIRS.defaultBlockState(), 3);
		}
		StructureTemplate template = TemplateScanner.scan(level, Collections.emptySet(), min, min.offset(4, 3, 4), min.offset(2, 0, 0), 0, "gametest_rule_reuse");
		int rules = template.getBlockRules().size();
		check(helper, rules >= 4 && rules <= 5, "a scan of 100 blocks with 5 block states wrote " + rules + " rules (hut alone: " + first.getBlockRules().size() + ")");
		for (BlockPos p : BlockPos.betweenClosed(min, min.offset(4, 3, 4))) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
		}
		helper.succeed();
	}

	private static int count(ServerLevel level, StructureBB bb, net.minecraft.world.level.block.Block block) {
		int found = 0;
		for (BlockPos p : BlockPos.betweenClosed(bb.min, bb.max)) {
			if (level.getBlockState(p).is(block)) {
				found++;
			}
		}
		return found;
	}

	/*
	 * the world generator entry point for a chunk (WorldStructureGenerator.generateAt, what a chunk ticket of the
	 * tick handler runs) selects the only registered world gen template, validates the position, registers the
	 * structure in the structure map and the tick handler builds it
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 200)
	public static void worldGeneratorPlacesStructure(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String name = "gametest_worldgen_hut";
		StructureTemplate template = scanHut(helper, name);
		StructureValidator validator = StructureValidationType.GROUND.getValidator();
		validator.setPropertyValue(StructureValidationProperties.WORLD_GEN, true);
		validator.setPropertyValue(StructureValidationProperties.BIOME_WHITE_LIST, false);
		validator.setPropertyValue(StructureValidationProperties.DIMENSION_WHITE_LIST, true);
		validator.setPropertyValue(StructureValidationProperties.DIMENSION_LIST, new int[] {0});
		validator.setPropertyValue(StructureValidationProperties.MIN_GENERATION_HEIGHT, level.getMinBuildHeight());
		validator.setPropertyValue(StructureValidationProperties.CLUSTER_VALUE, 0);
		validator.setPropertyValue(StructureValidationProperties.TERRITORY_NAME, "gametest");
		template.setValidationSettings(validator);

		BlockPos far = helper.absolutePos(BlockPos.ZERO).offset(0, 0, 6000);
		ChunkPos chunk = new ChunkPos(far);
		level.getChunk(chunk.x, chunk.z);
		String biome = LegacyBiomes.getSurfaceBiomeName(level, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ());

		//only the test template is registered while the generator selects, then the real tables come back
		WorldGenStructureManager manager = WorldGenStructureManager.INSTANCE;
		int generatedBefore = WorldGenTickHandler.INSTANCE.getStructuresGenerated();
		try {
			manager.clearCachedTemplates();
			TerritoryManager.clearTerritoryCache();
			manager.loadBiomeList();
			StructureTemplateManager.addTemplate(template);
			check(helper, TerritoryManager.getTerritoryNames().contains("gametest"), "the test template did not register its territory");
			check(helper, TerritoryManager.getTerritoryChanceInBiome("gametest", biome) == 1, "the test territory is not the only territory of biome " + biome);
			WorldStructureGenerator.INSTANCE.generateAt(chunk.x, chunk.z, level);
		}
		finally {
			StructureTemplateManager.removeTemplate(name);
			manager.rebuild();
		}

		Optional<Territory> territory = TerritoryManager.getTerritory(chunk.x, chunk.z, level);
		check(helper, territory.isPresent() && territory.get().getTerritoryName().equals("gametest"), "the chunk has no territory of the test template: " + territory.map(Territory::getTerritoryName).orElse("none"));
		List<StructureEntry> entries = new ArrayList<>(StructureMap.get(level).getEntriesNear(level, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), 1, true, new ArrayList<>()));
		entries.removeIf(e -> !e.getName().equals(name));
		check(helper, entries.size() == 1, "expected one structure map entry for the generated structure, found " + entries.size());
		StructureBB bb = entries.get(0).getBB();

		helper.succeedWhen(() -> {
			check(helper, WorldGenTickHandler.INSTANCE.getStructuresGenerated() > generatedBefore, "the tick handler has not built the structure yet");
			check(helper, count(level, bb, Blocks.STONE_BRICKS) == 25, "floor blocks: " + count(level, bb, Blocks.STONE_BRICKS) + " in " + bb);
			check(helper, count(level, bb, Blocks.LAPIS_BLOCK) == 12, "post blocks: " + count(level, bb, Blocks.LAPIS_BLOCK) + " in " + bb);
			check(helper, count(level, bb, Blocks.GLOWSTONE) == 1, "glowstone blocks: " + count(level, bb, Blocks.GLOWSTONE) + " in " + bb);
		});
	}

	/*
	 * a town template (parsed from the .awt text format) is generated into a flat area through the town builder path
	 * (WorldTownGenerator.generate): town map + structure map entries, levelled ground, roads, houses
	 */
	//timeout: the generation queue is shared with the other tests of the batch; with background_attempt_chunks a structure
	//attempt of another test's new chunks may wait for its chunks at the head of the queue (up to 400 ticks, which pass in a
	//few seconds in the game test server), and the queue must be idle before the test succeeds
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 6000)
	public static void townGeneratesFromTemplate(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String houseName = "gametest_town_hut";
		StructureTemplate house = scanHut(helper, houseName);
		house.setValidationSettings(StructureValidationType.GROUND.getValidator());
		StructureTemplateManager.addTemplate(house);

		List<String> lines = Arrays.asList(
				"header:",
				"name=GameTestTown",
				"minSize=1",
				"maxSize=4",
				"selectionWeight=1",
				"clusterValue=0",
				"townBlockSize=20",
				"townPlotSize=7",
				"wallStyle=0",
				"wallSize=0",
				"exteriorSize=0",
				"exteriorPlotSize=0",
				"interiorEmptyPlotChance=0",
				"randomVillagersPerChunk=0",
				"preventNaturalHostileSpawns=true",
				"biomeWhiteList=false",
				"dimensionWhiteList=true",
				"dimensionList=0",
				":endheader",
				"houseStructures:",
				houseName + ":1",
				":endHouseStructures");
		Optional<TownTemplate> parsed = TownTemplateParser.parseTemplate(lines);
		check(helper, parsed.isPresent(), "the town template was not parsed");
		TownTemplate town = parsed.get();
		town.validateStructureEntries();
		check(helper, town.getHouseStructureEntries().size() == 1, "the town template lost its house entry");

		BlockPos far = helper.absolutePos(BlockPos.ZERO).offset(6000, 0, 0);
		level.getChunk(far.getX() >> 4, far.getZ() >> 4);
		int groundY = WorldStructureGenerator.getTargetY(level, far.getX(), far.getZ(), false);
		BlockPos ground = new BlockPos(far.getX(), groundY, far.getZ());
		TownBoundingArea area = ItemTownBuilder.getTownArea(level, ground, Direction.SOUTH, 3, 3);
		//the town is generated once its chunks are loaded (a few ticks later); the house template stays registered until then
		WorldTownGenerator.INSTANCE.generate(level, area, town);

		StructureBB townBB = new StructureBB(new BlockPos(area.getBlockMinX(), area.getMinY(), area.getBlockMinZ()), new BlockPos(area.getBlockMaxX(), area.getMaxY(), area.getBlockMaxZ()));
		check(helper, net.shadowmage.ancientwarfare.core.gamedata.AWGameData.INSTANCE.getPerWorldData(level, TownMap.class).intersectsWithTown(townBB), "the town is not in the town map");
		check(helper, StructureMap.get(level).getStructureAt(level, new BlockPos(area.getCenterX(), area.getSurfaceY() + 1, area.getCenterZ())).map(e -> e.getName().equals("GameTestTown")).orElse(false),
				"the town is not in the structure map");

		//other tests generate at the same time: the houses are counted by their blocks, not by the tick handler counter
		helper.succeedWhen(() -> {
			check(helper, WorldGenTickHandler.INSTANCE.isIdle(), "town generation is still running");
			int houses = count(level, townBB, Blocks.GLOWSTONE);
			check(helper, houses >= 4, "expected at least 4 houses, found " + houses);
			check(helper, count(level, townBB, Blocks.LAPIS_BLOCK) == houses * 12, "post blocks " + count(level, townBB, Blocks.LAPIS_BLOCK) + ", houses " + houses);
			check(helper, count(level, townBB, Blocks.STONE_BRICKS) == houses * 25, "floor blocks " + count(level, townBB, Blocks.STONE_BRICKS) + ", houses " + houses);
			int roads = count(level, townBB, Blocks.GRAVEL);
			check(helper, roads > 100, "expected the road grid, found " + roads + " gravel blocks");
			//the two main roads cross in the middle of the town, on the surface
			BlockPos centre = new BlockPos(area.getCenterX(), area.getSurfaceY(), area.getCenterZ());
			check(helper, level.getBlockState(centre.offset(-1, 0, -8)).is(Blocks.GRAVEL) || level.getBlockState(centre.offset(0, 0, -8)).is(Blocks.GRAVEL), "no road north of the town centre at " + centre);
			StructureTemplateManager.removeTemplate(houseName);
		});
	}

	/*
	 * the world generation path of a town (what a town ticket of the tick handler runs): WorldTownGenerator
	 * .attemptGeneration finds the templates valid for the biome, searches the largest flat area around the chunk
	 * (TownPlacementValidator, also over chunks that are not generated yet), takes the territory of the chunk, selects
	 * a template that fits and generates it. Towns only start on ground near or above sea level (y 59, as in 1.12),
	 * so the test first lays a 6 x 6 chunk platform at y 62 over the flat test world: the search grows over the
	 * platform (loaded chunks, read block by block) and stops at its edge (chunks that are not generated, height from
	 * the chunk generator); the town is then levelled down to the height of the ground around it.
	 */
	//timeout: the generation queue is shared with the other tests of the batch (packTownGenerates builds a whole town)
	//and has a wall clock budget per tick, so a busy machine needs many more ticks
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 6000)
	public static void townAttemptFindsAreaAndGenerates(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String houseName = "gametest_town_hut_auto";
		StructureTemplate house = scanHut(helper, houseName);
		house.setValidationSettings(StructureValidationType.GROUND.getValidator());
		StructureTemplateManager.addTemplate(house);
		List<String> lines = Arrays.asList("header:", "name=GameTestTownAuto", "minSize=2", "maxSize=3", "selectionWeight=1", "clusterValue=0", "townBlockSize=20", "townPlotSize=7",
				"wallStyle=0", "biomeWhiteList=false", "dimensionWhiteList=true", "dimensionList=0", "territoryName=gametesttown", ":endheader",
				"houseStructures:", houseName + ":1", ":endHouseStructures");
		TownTemplate town = TownTemplateParser.parseTemplate(lines).orElse(null);
		check(helper, town != null, "the town template was not parsed");
		town.validateStructureEntries();

		BlockPos far = helper.absolutePos(BlockPos.ZERO).offset(0, 0, -6000);
		ChunkPos chunk = new ChunkPos(far);
		for (int x = (chunk.x - 3) << 4; x < (chunk.x + 3) << 4; x++) {
			for (int z = (chunk.z - 3) << 4; z < (chunk.z + 3) << 4; z++) {
				level.setBlock(new BlockPos(x, 62, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
			}
		}
		TerritoryData territories = TerritoryData.get(level);
		String territoryId = "gametesttown" + territories.getNextTerritoryId("gametesttown");
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				territories.setOwned(ChunkPos.asLong(chunk.x + x, chunk.z + z), territoryId, "gametesttown");
			}
		}
		TownMap townMap = net.shadowmage.ancientwarfare.core.gamedata.AWGameData.INSTANCE.getPerWorldData(level, TownMap.class);
		check(helper, townMap.getClosestTown(chunk.getMinBlockX(), chunk.getMinBlockZ(), 10000) > 2000, "there already is a town near the test position");
		try {
			TownTemplateManager.INSTANCE.loadTemplate(town);
			WorldTownGenerator.INSTANCE.attemptGeneration(level, chunk.getMinBlockX(), chunk.getMinBlockZ());
		}
		finally {
			TownTemplateManager.INSTANCE.removeTemplate("GameTestTownAuto");
		}
		float distance = townMap.getClosestTown(chunk.getMinBlockX(), chunk.getMinBlockZ(), 10000);
		check(helper, distance < 200, "no town was generated around the chunk (closest town " + distance + " blocks away)");
		//the area grows from the start chunk in all directions and is cut down from its north west corner
		List<StructureEntry> entries = new ArrayList<>(StructureMap.get(level).getEntriesNear(level, chunk.getMinBlockX(), chunk.getMinBlockZ(), 6, true, new ArrayList<>()));
		entries.removeIf(e -> !e.getName().equals("GameTestTownAuto"));
		check(helper, entries.size() == 1, "expected one structure map entry for the town, found " + entries.size());
		StructureBB townBB = entries.get(0).getBB();
		check(helper, townBB.min.getX() == (chunk.x - 3) << 4 && townBB.min.getZ() == (chunk.z - 3) << 4, "the town does not start at the corner of the platform: " + townBB);
		check(helper, townBB.getXSize() == 64 && townBB.getZSize() == 64, "the town area was not cut down to the maximum size of the template (4 x 4 chunks): " + townBB);
		helper.succeedWhen(() -> {
			check(helper, WorldGenTickHandler.INSTANCE.isIdle(), "town generation is still running");
			int houses = count(level, townBB, Blocks.GLOWSTONE);
			check(helper, houses >= 2, "expected at least 2 houses, found " + houses);
			check(helper, count(level, townBB, Blocks.GRAVEL) > 50, "expected roads, found " + count(level, townBB, Blocks.GRAVEL) + " gravel blocks");
			StructureTemplateManager.removeTemplate(houseName);
		});
	}

	/*
	 * every town template of the shipped pack that loaded generates into a flat area without an error
	 * (walls, roads, lamps, the structures the pack provides for it)
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 6000)
	public static void packTownGenerates(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Optional<TownTemplate> template = TownTemplateManager.INSTANCE.getTemplate("NoggUnwalledTown");
		if (!template.isPresent()) {
			template = TownTemplateManager.INSTANCE.getTemplates().stream().findFirst();
		}
		check(helper, template.isPresent(), "no town template of the default pack is loaded");
		TownTemplate town = template.get();

		BlockPos far = helper.absolutePos(BlockPos.ZERO).offset(-6000, 0, 0);
		level.getChunk(far.getX() >> 4, far.getZ() >> 4);
		int groundY = WorldStructureGenerator.getTargetY(level, far.getX(), far.getZ(), false);
		int size = Math.max(town.getMinSize(), 6);
		TownBoundingArea area = ItemTownBuilder.getTownArea(level, new BlockPos(far.getX(), groundY, far.getZ()), Direction.SOUTH, size, size);
		WorldTownGenerator.INSTANCE.generate(level, area, town);
		String info = town.getTownTypeName() + " " + area;
		StructureBB above = new StructureBB(new BlockPos(area.getBlockMinX() + 8, area.getSurfaceY() + 2, area.getBlockMinZ() + 8), new BlockPos(area.getBlockMaxX() - 8, area.getSurfaceY() + 12, area.getBlockMaxZ() - 8));
		helper.succeedWhen(() -> {
			check(helper, WorldGenTickHandler.INSTANCE.isIdle(), "town generation is still running: " + info);
			int blocks = 0;
			for (BlockPos p : BlockPos.betweenClosed(above.min, above.max)) {
				if (!level.getBlockState(p).isAir()) {
					blocks++;
				}
			}
			check(helper, blocks >= 2000, "expected buildings in " + info + ", found " + blocks + " blocks above the town surface");
			AncientWarfare.LOG.info("GameTest packTownGenerates: {} has {} blocks above its surface", info, blocks);
		});
	}

	/*
	 * 1.12 item stacks and uuids inside the rules of the original templates (chest contents, tile data, entity data)
	 * are upgraded: count and meta survive, renamed items are found, the Ancient Warfare 1.12 mod ids are folded
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void legacyTemplateDataUpgrades(GameTestHelper helper) throws Exception {
		net.minecraft.world.item.ItemStack log = net.shadowmage.ancientwarfare.structure.util.LegacyItemStacks.read(net.minecraft.nbt.TagParser.parseTag("{id:\"minecraft:log\",Count:4b,Damage:1s}"));
		check(helper, log.is(net.minecraft.world.item.Items.SPRUCE_LOG) && log.getCount() == 4, "1.12 log:1 x 4 became " + log);
		net.minecraft.world.item.ItemStack sword = net.shadowmage.ancientwarfare.structure.util.LegacyItemStacks.read(net.minecraft.nbt.TagParser.parseTag("{id:\"minecraft:iron_sword\",Count:1b,Damage:20s,tag:{ench:[{id:16s,lvl:3s}]}}"));
		check(helper, sword.is(net.minecraft.world.item.Items.IRON_SWORD) && sword.getDamageValue() == 20 && sword.isEnchanted(), "1.12 damaged enchanted sword became " + sword + " " + sword.getComponents());
		net.minecraft.world.item.ItemStack coin = net.shadowmage.ancientwarfare.structure.util.LegacyItemStacks.read(net.minecraft.nbt.TagParser.parseTag("{id:\"ancientwarfarenpc:coin\",Count:12b,Damage:0s}"));
		check(helper, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(coin.getItem()).toString().equals("ancientwarfare:coin") && coin.getCount() == 12, "1.12 npc coin x 12 became " + coin);
		check(helper, net.shadowmage.ancientwarfare.structure.util.LegacyItemStacks.read(net.minecraft.nbt.TagParser.parseTag("{id:\"ebwizardry:scroll\",Count:1b,Damage:0s}")).isEmpty(), "a stack of a missing mod is not empty");

		CompoundTag chest = net.minecraft.nbt.TagParser.parseTag("{id:\"minecraft:chest\",Items:[{Slot:3b,id:\"minecraft:wool\",Count:5b,Damage:14s},{Slot:4b,id:\"harvestcraft:barleyitem\",Count:2b,Damage:0s}],ownerMost:5L,ownerLeast:7L}");
		net.shadowmage.ancientwarfare.structure.util.LegacyItemStacks.upgradeNbt(chest);
		check(helper, chest.getList("Items", 10).size() == 1, "the stack of the missing mod was not removed: " + chest);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, Blocks.CHEST);
		net.minecraft.world.level.block.entity.ChestBlockEntity tile = helper.getBlockEntity(pos);
		tile.loadWithComponents(chest, helper.getLevel().registryAccess());
		check(helper, tile.getItem(3).is(net.minecraft.world.item.Items.RED_WOOL) && tile.getItem(3).getCount() == 5, "chest slot 3 holds " + tile.getItem(3) + " after loading " + chest);
		check(helper, chest.hasUUID("owner") && chest.getUUID("owner").equals(new java.util.UUID(5, 7)), "the 1.12 uuid pair was not converted: " + chest);
		helper.succeed();
	}

	/*
	 * the entities of an original 1.12 template end up inside the structure. The gate of the shipped
	 * wall_bandit_gate (entity rule position packed with the 1.12 BlockPos layout) stands in the wall.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void legacyTemplateGateIsInsideStructure(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = StructureTemplateManager.getTemplate("wall_bandit_gate").orElse(null);
		check(helper, template != null, "the pack template wall_bandit_gate is not loaded");
		//the wall is 16 x 7 x 3: it is built above the floor of the test area (entities are only found in ticking chunks),
		//one block over the edge on both sides, and removed again
		BlockPos origin = helper.absolutePos(new BlockPos(0, 5, 6));
		StructureBB probe = new StructureBB(origin, Direction.NORTH, template);
		BlockPos pos = origin.offset(origin.getX() - 1 - probe.min.getX(), 0, 0);
		net.shadowmage.ancientwarfare.structure.template.build.StructureBuilder builder = new net.shadowmage.ancientwarfare.structure.template.build.StructureBuilder(level, template, Direction.NORTH, pos);
		builder.instantConstruction();
		StructureBB bb = builder.getBoundingBox();
		List<net.shadowmage.ancientwarfare.structure.entity.EntityGate> gates = level.getEntitiesOfClass(net.shadowmage.ancientwarfare.structure.entity.EntityGate.class, bb.getAABB().inflate(1));
		List<net.shadowmage.ancientwarfare.structure.entity.EntityGate> all = level.getEntitiesOfClass(net.shadowmage.ancientwarfare.structure.entity.EntityGate.class, bb.getAABB().inflate(64, 64, 20000));
		check(helper, gates.size() == 1, "expected the gate inside " + bb + ", found " + gates.size() + " there and " + all.size() + " within 20000 blocks: " + (all.isEmpty() ? "" : all.get(0).blockPosition()));
		BlockPos gatePos = gates.get(0).blockPosition();
		//compared as coordinates, the far edge included: the test area can lie beyond 8 million blocks, where the float
		//maths of the gate rounds a centre of x.5 to the next whole block
		net.shadowmage.ancientwarfare.structure.entity.EntityGate gate = gates.get(0);
		check(helper, gate.getX() >= bb.min.getX() && gate.getX() <= bb.max.getX() + 1 && gate.getZ() >= bb.min.getZ() && gate.getZ() <= bb.max.getZ() + 1, "the gate stands at " + gatePos + " (" + gate.getX() + ", " + gate.getZ() + "), outside " + bb);
		gates.forEach(net.minecraft.world.entity.Entity::discard);
		for (BlockPos p : BlockPos.betweenClosed(bb.min, bb.max)) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
		}
		helper.succeed();
	}

	private static final String[] LEGACY_TEMPLATE = {
			"header:", "version=2.11", "name=gametest_legacy_112", "size=3,2,3", "offset=1,1,0", ":endheader",
			"validation:", "type=ground", "survival=false", "worldGenEnabled=false", ":endvalidation",
			"layer: 0", "3x1|3", ":endlayer",
			"layer: 1", "2,0,3", "2x0|3", ":endlayer",
			"rule:", "plugin=vanillaBlocks", "number=1", "data:", "JSON:{blockState:{blockName:\"minecraft:stone\",properties:{variant:\"stone\"}},buildPass:0}", ":enddata", ":endrule",
			//a chest as the 1.12 scanner writes it (tile data with the items + the inventory copy)
			"rule:", "plugin=inventory", "number=2", "data:",
			"JSON:{blockState:{blockName:\"minecraft:chest\",properties:{facing:\"east\"}},lootLevel:0,teData:{Items:[{Slot:0b,id:\"minecraft:iron_ingot\",Count:5b,Damage:0s},{Slot:1b,id:\"minecraft:wool\",Count:3b,Damage:4s}],id:\"minecraft:chest\",Lock:\"\"},inventoryData:{length:27,inventoryContents:[{slot:0,id:\"minecraft:iron_ingot\",Count:5b,Damage:0s},{slot:1,id:\"minecraft:wool\",Count:3b,Damage:4s}]},legacyFeatures:0b}",
			":enddata", ":endrule",
			//a chest that is filled from the inventory copy only (legacy features, tile data without items)
			"rule:", "plugin=inventory", "number=3", "data:",
			"JSON:{blockState:{blockName:\"minecraft:chest\",properties:{facing:\"east\"}},lootLevel:0,teData:{id:\"minecraft:chest\",Lock:\"\"},inventoryData:{length:27,inventoryContents:[{slot:0,id:\"minecraft:iron_ingot\",Count:5b,Damage:0s},{slot:1,id:\"minecraft:wool\",Count:3b,Damage:4s}]},legacyFeatures:1b}",
			":enddata", ":endrule",
			//a pig at template position (1, 1, 1): 1 << 38 | 1 << 26 | 1 in the 1.12 layout
			"entity:", "plugin=entity", "number=0", "data:",
			"JSON:{yOffset:0.0f,xOffset:0.5f,rotation:0.0f,mobID:\"minecraft:pig\",zOffset:0.5f,position:274945015809L,entityData:{CustomName:\"Lemmy\",Health:10.0f}}",
			":enddata", ":endentity"};

	/*
	 * (also for a plain entity rule) a template in the 1.12 text format with two chests (5 iron ingots,
	 * 3 yellow wool = minecraft:wool with Damage 4) and a pig is parsed and built: both chests hold the right items
	 * with the right counts, the pig stands inside the structure and keeps its name
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void legacyTemplateChestKeepsCountsAndFlattenedItems(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = net.shadowmage.ancientwarfare.structure.template.load.TemplateParser.INSTANCE.parseTemplate("gametest_legacy_112.aws", Arrays.asList(LEGACY_TEMPLATE)).map(r -> r.getData()).orElse(null);
		check(helper, template != null, "the 1.12 format template was not parsed");
		BlockPos pos = helper.absolutePos(new BlockPos(6, 3, 6));
		net.shadowmage.ancientwarfare.structure.template.build.StructureBuilder builder = new net.shadowmage.ancientwarfare.structure.template.build.StructureBuilder(level, template, Direction.NORTH, pos);
		builder.instantConstruction();
		StructureBB bb = builder.getBoundingBox();
		int chests = 0;
		for (BlockPos p : BlockPos.betweenClosed(bb.min, bb.max)) {
			if (level.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
				chests++;
				net.minecraft.world.item.ItemStack iron = chest.getItem(0);
				net.minecraft.world.item.ItemStack wool = chest.getItem(1);
				check(helper, iron.is(net.minecraft.world.item.Items.IRON_INGOT) && iron.getCount() == 5, "chest at " + p + " slot 0: " + iron + ", expected 5 iron ingots");
				check(helper, wool.is(net.minecraft.world.item.Items.YELLOW_WOOL) && wool.getCount() == 3, "chest at " + p + " slot 1: " + wool + ", expected 3 yellow wool");
			}
		}
		check(helper, chests == 2, "expected 2 chests in " + bb + ", found " + chests);
		List<net.minecraft.world.entity.animal.Pig> pigs = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, bb.getAABB().inflate(0.5));
		check(helper, pigs.size() == 1, "expected the pig inside " + bb + ", found " + pigs.size() + " (" + level.getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, bb.getAABB().inflate(64, 64, 20000)).size() + " within 20000 blocks)");
		check(helper, pigs.get(0).hasCustomName() && pigs.get(0).getCustomName().getString().equals("Lemmy"), "the pig lost its 1.12 custom name: " + pigs.get(0).getCustomName());
		pigs.get(0).discard();
		helper.succeed();
	}

	/*
	 * territory data (1.12: world capability, now saved data of the dimension) keeps chunk ownership, territory
	 * names, cluster values and the id counters through a save / load of its file
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void territoryDataSurvivesSaveLoad(GameTestHelper helper) throws Exception {
		ServerLevel level = helper.getLevel();
		TerritoryData data = new TerritoryData("AWTerritoryData");
		check(helper, !data.isDirty(), "new territory data is dirty");
		int id = data.getNextTerritoryId("empire");
		check(helper, id == 1, "first territory id: " + id);
		Random rng = new Random(42);
		HashSet<Long> owned = new HashSet<>();
		for (int i = 0; i < 40; i++) {
			long pos = ChunkPos.asLong(100 + rng.nextInt(12), -300 - rng.nextInt(12));
			owned.add(pos);
			data.setOwned(pos, "empire" + id, "empire");
		}
		long other = ChunkPos.asLong(-5, 7);
		data.setOwned(other, "generic" + data.getNextTerritoryId("generic"), "generic");
		Territory territory = data.getTerritory(owned.iterator().next()).orElseThrow();
		data.setDirty(false);
		territory.addClusterValue(17);
		check(helper, data.isDirty(), "a cluster value change does not mark the territory data dirty");
		int remaining = territory.getRemainingClusterValue();
		BlockPos centre = territory.getTerritoryCenter();

		File file = Files.createTempFile("aw_territory", ".dat").toFile();
		CompoundTag root = new CompoundTag();
		root.put("data", data.save(new CompoundTag(), level.registryAccess()));
		NbtIo.writeCompressed(root, file.toPath());
		CompoundTag read = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
		file.delete();
		TerritoryData loaded = new TerritoryData("AWTerritoryData");
		loaded.readFromNBT(read.getCompound("data"));

		for (long pos : owned) {
			check(helper, loaded.isOwned(pos), "chunk " + new ChunkPos(pos) + " lost its territory");
		}
		check(helper, loaded.isOwned(other) && loaded.getTerritory(other).orElseThrow().getTerritoryName().equals("generic"), "the second territory was not loaded");
		check(helper, !loaded.isOwned(ChunkPos.asLong(0, 0)), "an unowned chunk is owned after loading");
		Territory loadedTerritory = loaded.getTerritory(owned.iterator().next()).orElseThrow();
		check(helper, loadedTerritory.getTerritoryName().equals("empire") && loadedTerritory.equals(territory), "territory name / id changed: " + loadedTerritory.getTerritoryName());
		check(helper, loadedTerritory.getRemainingClusterValue() == remaining, "remaining cluster value " + loadedTerritory.getRemainingClusterValue() + ", expected " + remaining);
		check(helper, loadedTerritory.getTerritoryCenter().equals(centre), "territory centre " + loadedTerritory.getTerritoryCenter() + ", expected " + centre);
		check(helper, !loaded.isDifferentTerritory("empire1", ChunkPos.getX(owned.iterator().next()), ChunkPos.getZ(owned.iterator().next())), "isDifferentTerritory after loading");
		check(helper, loaded.getNextTerritoryId("empire") == 2 && loaded.getNextTerritoryId("generic") == 2, "the territory id counters were not loaded");
		loaded.setDirty(false);
		loadedTerritory.addClusterValue(1);
		check(helper, loaded.isDirty(), "a loaded territory does not mark the data dirty");

		//the instance the dimension hands out is saved data of that dimension
		TerritoryData live = TerritoryData.get(level);
		check(helper, live == TerritoryData.get(level), "the dimension does not keep one territory data instance");
		helper.succeed();
	}

	/*
	 * Port deviation (towns in 1.21 terrain): the town search accepts chunks that lie up to
	 * town_search_height_drop blocks below its start and chunks that are partly deep water. A 6 x 6 chunk platform at
	 * y 70 with one row of chunks 8 blocks lower and a 3 block wide, 12 deep water channel through another row: the
	 * whole platform is one town area with the defaults, and only the 2 chunk wide strip between the two rows with
	 * the 1.12 values (drop 3, no water).
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void townSearchToleratesDropsAndRivers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ChunkPos chunk = new ChunkPos(helper.absolutePos(BlockPos.ZERO).offset(20000, 0, -6000));
		for (int x = (chunk.x - 3) << 4; x < (chunk.x + 3) << 4; x++) {
			for (int z = (chunk.z - 3) << 4; z < (chunk.z + 3) << 4; z++) {
				int cx = x >> 4;
				if (cx == chunk.x - 2 && (x & 15) < 3) {
					for (int y = 50; y <= 69; y++) {
						level.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), 2);
					}
					level.setBlock(new BlockPos(x, 49, z), Blocks.STONE.defaultBlockState(), 2);
				} else {
					level.setBlock(new BlockPos(x, cx == chunk.x + 1 ? 62 : 70, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
				}
			}
		}
		int x = chunk.getMinBlockX() + 8;
		int z = chunk.getMinBlockZ() + 8;
		int drop = net.shadowmage.ancientwarfare.structure.config.AWStructureStatics.townSearchHeightDrop;
		float water = net.shadowmage.ancientwarfare.structure.config.AWStructureStatics.townSearchWaterFraction;
		float rough = net.shadowmage.ancientwarfare.structure.config.AWStructureStatics.townSearchRoughFraction;
		Optional<TownBoundingArea> tolerant;
		Optional<TownBoundingArea> strict;
		try {
			net.shadowmage.ancientwarfare.structure.config.AWStructureStatics.townSearchHeightDrop = 12;
			net.shadowmage.ancientwarfare.structure.config.AWStructureStatics.townSearchRoughFraction = 0;
			net.shadowmage.ancientwarfare.structure.config.AWStructureStatics.townSearchWaterFraction = 0.25f;
			tolerant = net.shadowmage.ancientwarfare.structure.town.TownPlacementValidator.findGenerationPosition(level, x, z);
			net.shadowmage.ancientwarfare.structure.config.AWStructureStatics.townSearchHeightDrop = 3;
			net.shadowmage.ancientwarfare.structure.config.AWStructureStatics.townSearchWaterFraction = 0;
			strict = net.shadowmage.ancientwarfare.structure.town.TownPlacementValidator.findGenerationPosition(level, x, z);
		}
		finally {
			net.shadowmage.ancientwarfare.structure.config.AWStructureStatics.townSearchHeightDrop = drop;
			net.shadowmage.ancientwarfare.structure.config.AWStructureStatics.townSearchWaterFraction = water;
			net.shadowmage.ancientwarfare.structure.config.AWStructureStatics.townSearchRoughFraction = rough;
		}
		check(helper, tolerant.isPresent() && strict.isPresent(), "no town area found on the platform");
		TownBoundingArea a = tolerant.get();
		check(helper, a.getChunkMinX() == chunk.x - 3 && a.getChunkMaxX() == chunk.x + 2 && a.getChunkMinZ() == chunk.z - 3 && a.getChunkMaxZ() == chunk.z + 2,
				"with the defaults the area is not the whole platform: " + a);
		TownBoundingArea b = strict.get();
		check(helper, b.getChunkMinX() == chunk.x - 1 && b.getChunkMaxX() == chunk.x, "with the 1.12 values the area should stop at the lower row and at the water: " + b);
		helper.succeed();
	}

	/*
	 * the chest loot tables the templates refer to (ancientwarfare:chests/*) exist, load and
	 * give items. Every table of the namespace is rolled; the named ones must produce something within 20 rolls.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void structureChestLootTablesGiveItems(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		net.minecraft.server.ReloadableServerRegistries.Holder registries = level.getServer().reloadableRegistries();
		int tables = 0;
		for (net.minecraft.resources.ResourceLocation id : registries.getKeys(net.minecraft.core.registries.Registries.LOOT_TABLE)) {
			if (id.getNamespace().equals(AncientWarfare.MOD_ID) && id.getPath().startsWith("chests/")) {
				tables++;
				rollLoot(level, id, helper.absolutePos(BlockPos.ZERO));
			}
		}
		check(helper, tables >= 130, "expected at least 130 ancientwarfare:chests/ loot tables, found " + tables);
		CompoundTag legacyChest = new CompoundTag();
		legacyChest.putString("LootTable", "minecraft:chests/village_blacksmith");
		String renamed = net.shadowmage.ancientwarfare.structure.util.LegacyItemStacks.upgradeNbt(legacyChest).getString("LootTable");
		check(helper, registries.getKeys(net.minecraft.core.registries.Registries.LOOT_TABLE).contains(net.minecraft.resources.ResourceLocation.parse(renamed)),
				"the 1.12 blacksmith chest table is not renamed to an existing table: " + renamed);
		for (String name : new String[] {"castle_treasury_low_value", "lizardman", "beast_treasure_hoard", "gravestone_medium_value", "potion_harmful", "enchanted_book"}) {
			int items = 0;
			for (int i = 0; i < 20; i++) {
				items += rollLoot(level, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AncientWarfare.MOD_ID, "chests/" + name), helper.absolutePos(BlockPos.ZERO));
			}
			check(helper, items > 0, "loot table chests/" + name + " gave nothing in 20 rolls");
		}
		helper.succeed();
	}

	private static int rollLoot(ServerLevel level, net.minecraft.resources.ResourceLocation id, BlockPos pos) {
		net.minecraft.world.level.storage.loot.LootTable table = level.getServer().reloadableRegistries().getLootTable(
				net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, id));
		net.minecraft.world.level.storage.loot.LootParams params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(pos))
				.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
		//1.12 tables give 2 - 3 potions in one entry; they have to arrive as single potions that kept their contents
		for (int i = 0; i < 20; i++) {
			for (net.minecraft.world.item.ItemStack stack : table.getRandomItems(params)) {
				if (stack.getCount() > stack.getMaxStackSize()) {
					throw new IllegalStateException("loot table " + id + " gives an overstacked item: " + stack);
				}
				if (stack.getItem() instanceof net.minecraft.world.item.PotionItem && !stack.has(net.minecraft.core.component.DataComponents.POTION_CONTENTS)) {
					throw new IllegalStateException("loot table " + id + " gives a potion without contents");
				}
			}
		}
		return table.getRandomItems(params).size();
	}
}
