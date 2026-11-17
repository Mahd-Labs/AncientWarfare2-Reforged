package net.shadowmage.ancientwarfare.structure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.build.BudgetedBuild;
import net.shadowmage.ancientwarfare.structure.template.build.ResumableSteps;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBuilderWorldGen;
import net.shadowmage.ancientwarfare.structure.template.build.validation.StructureValidationType;
import net.shadowmage.ancientwarfare.structure.template.build.validation.StructureValidator;
import net.shadowmage.ancientwarfare.structure.template.build.validation.properties.StructureValidationProperties;
import net.shadowmage.ancientwarfare.structure.template.scan.TemplateScanner;
import net.shadowmage.ancientwarfare.structure.town.TownBoundingArea;
import net.shadowmage.ancientwarfare.structure.town.TownGenerator;
import net.shadowmage.ancientwarfare.structure.town.TownTemplate;
import net.shadowmage.ancientwarfare.structure.town.TownTemplateParser;
import net.shadowmage.ancientwarfare.structure.util.BiomeChanger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Each test works in its own quadrant 3000 blocks away (tests of a batch start close to each other; the other far tests
 * of the structure module use the axes at 6000).
 * Improvement (not baseline): budgeted_placement also spreads a world generation structure's validator
 * pre-/postGeneration and a town's terrain preparation over ticks. Each test builds the same uneven terrain twice far
 * from the test area (high above the flat test world), runs the work once at once (the original methods) and once
 * resumable with a small budget per tick, and compares every block state of the affected volume.
 * Not compared: a border smoothing over uneven terrain (Ground validator with a border, town border). The original
 * smooths the points of a HashSet of identity-hashed objects in its iteration order, reading neighbours that may
 * already be smoothed, and draws from world.random, so two instant runs on equal terrain can already differ. The town
 * test keeps the terrain flat around the town so the smoothing has nothing to do.
 */
@GameTestHolder(AncientWarfare.MOD_ID)
@PrefixGameTestTemplate(false)
public class BudgetedPreparationGameTests {
	private static final String TEMPLATE = "automation_empty";
	private static final int BASE_Y = 200;

	private static void check(GameTestHelper helper, boolean condition, String message) {
		if (!condition) {
			helper.fail(message);
		}
	}

	/*
	 * Ground validator (no border, leveling 10): preGeneration clears 60 layers above the structure (trees on the hills
	 * included) and fills the dips under it; postGeneration sprinkles snow (snowy biome). Worked with no time budget at
	 * all (each call does the minimum: 32 positions, one column, 63 block positions).
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 2400)
	public static void improvementBudgetedGroundValidatorMatchesInstant(GameTestHelper helper) {
		StructureValidator validator = StructureValidationType.GROUND.getValidator();
		validator.setPropertyValue(StructureValidationProperties.BORDER_SIZE, 0);
		validator.setPropertyValue(StructureValidationProperties.MAX_LEVELING, 10);
		validator.setPropertyValue(StructureValidationProperties.MAX_FILL, 10);
		validator.setPropertyValue(StructureValidationProperties.PRESERVE_BLOCKS, false);
		runValidatorComparison(helper, "gametest_budget_ground", validator, 3000, 3000, true, 2);
	}

	/*
	 * Harbor validator (border 6, leveling 10, fill 0): preGeneration levels the border ring column by column (clearing
	 * the hills and trees, a top block on each column)
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 2400)
	public static void improvementBudgetedBorderValidatorMatchesInstant(GameTestHelper helper) {
		StructureValidator validator = StructureValidationType.HARBOR.getValidator();
		validator.setPropertyValue(StructureValidationProperties.BORDER_SIZE, 6);
		validator.setPropertyValue(StructureValidationProperties.MAX_LEVELING, 10);
		validator.setPropertyValue(StructureValidationProperties.MAX_FILL, 0);
		validator.setPropertyValue(StructureValidationProperties.PRESERVE_BLOCKS, false);
		runValidatorComparison(helper, "gametest_budget_border", validator, -3000, 3000, false, 0);
	}

	/*
	 * Fidelity (not an improvement): the Ground validator's postGeneration decides on the snow ring by temperature only, as
	 * 1.12 did (world.canSnowAt(origin.up(), false)). The block above the key is usually part of the structure, so the
	 * full snow test (air, sturdy block below, light) was nearly always false on the port. Cold biome with a solid block
	 * above the key: snow; warm biome: none.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 200)
	public static void groundValidatorSnowRingUsesTemperatureOnly(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int cold = snowRing(helper, level, -3000, -3000, "minecraft:snowy_plains");
		int warm = snowRing(helper, level, -3000, -2900, "minecraft:plains");
		check(helper, cold > 0, "no snow ring in a cold biome with a block above the key");
		check(helper, warm == 0, "snow in a warm biome: " + warm);
		helper.succeed();
	}

	private static int snowRing(GameTestHelper helper, ServerLevel level, int farX, int farZ, String biome) {
		BlockPos far = helper.absolutePos(BlockPos.ZERO).offset(farX, 0, farZ);
		BlockPos origin = new BlockPos(far.getX(), BASE_Y, far.getZ());
		BlockPos from = origin.offset(-8, -3, -8);
		BlockPos to = origin.offset(8, 8, 8);
		reserveForTest(level, from, to);
		loadChunks(level, from, to);
		for (BlockPos p : BlockPos.betweenClosed(from, to)) {
			level.setBlock(p, p.getY() < BASE_Y - 1 ? Blocks.DIRT.defaultBlockState() : p.getY() == BASE_Y - 1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
		}
		setBiome(helper, level, from, to, biome);
		level.setBlock(origin, Blocks.STONE_BRICKS.defaultBlockState(), 2 | 16);
		level.setBlock(origin.above(), Blocks.STONE_BRICKS.defaultBlockState(), 2 | 16);
		StructureValidator validator = StructureValidationType.GROUND.getValidator();
		validator.setPropertyValue(StructureValidationProperties.BORDER_SIZE, 4);
		StructureBB bb = new StructureBB(origin.offset(-1, 0, -1), origin.offset(1, 4, 1));
		validator.postGeneration(level, origin, bb, null);
		return countBlock(level, from, to, Blocks.SNOW.defaultBlockState());
	}

	private static void runValidatorComparison(GameTestHelper helper, String name, StructureValidator validator, int farX, int farZ, boolean snowy, int minAfterCalls) {
		ServerLevel level = helper.getLevel();
		BlockPos far = helper.absolutePos(BlockPos.ZERO).offset(farX, 0, farZ);
		far = new BlockPos(far.getX(), BASE_Y, far.getZ());
		StructureTemplate template = scanHouse(level, far.offset(0, 80, 0), name);
		template.setValidationSettings(validator);
		int border = validator.getBorderSize();
		int margin = border + 12;

		BlockPos keyA = far;
		BlockPos keyB = far.offset(0, 0, 96);
		StructureBuilderWorldGen probe = new StructureBuilderWorldGen(level, template, Direction.NORTH, keyA);
		int dy = BASE_Y - probe.getBoundingBox().min.getY();
		keyA = keyA.offset(0, dy, 0);
		keyB = keyB.offset(0, dy, 0);
		StructureBuilderWorldGen instant = new StructureBuilderWorldGen(level, template, Direction.NORTH, keyA);
		StructureBuilderWorldGen budgetedBuilder = new StructureBuilderWorldGen(level, template, Direction.NORTH, keyB);
		BlockPos originB = keyB;
		StructureBB bbA = instant.getBoundingBox();
		StructureBB bbB = budgetedBuilder.getBoundingBox();
		check(helper, bbA.min.getY() == BASE_Y && bbB.min.getY() == BASE_Y, "bounding boxes not at the base height: " + bbA + " / " + bbB);

		BlockPos fromA = new BlockPos(bbA.min.getX() - margin, BASE_Y - 14, bbA.min.getZ() - margin);
		BlockPos toA = new BlockPos(bbA.max.getX() + margin, bbA.max.getY() + 64, bbA.max.getZ() + margin);
		BlockPos shift = bbB.min.subtract(bbA.min);
		reserveForTest(level, fromA, toA);
		reserveForTest(level, fromA.offset(shift), toA.offset(shift));
		buildTerrain(level, fromA, toA, bbA.min);
		buildTerrain(level, fromA.offset(shift), toA.offset(shift), bbB.min);
		if (snowy) {
			setBiome(helper, level, fromA, toA, "minecraft:snowy_plains");
			setBiome(helper, level, fromA.offset(shift), toA.offset(shift), "minecraft:snowy_plains");
		}
		check(helper, equalVolumes(level, fromA, toA, shift, new ArrayList<>()), "the two terrain copies differ before the builds");

		forceChunks(level, fromA, toA, true);
		forceChunks(level, fromA.offset(shift), toA.offset(shift), true);
		instant.instantConstruction();

		BudgetedBuild budgeted = new BudgetedBuild(budgetedBuilder, null, null, false);
		AtomicBoolean compared = new AtomicBoolean();
		AtomicInteger calls = new AtomicInteger();
		helper.onEachTick(() -> {
			if (compared.get()) {
				return;
			}
			calls.incrementAndGet();
			//no budget: every call works the minimum before it checks the deadline
			if (!budgeted.work(System.nanoTime(), false)) {
				return;
			}
			compared.set(true);
			forceChunks(level, fromA, toA, false);
			forceChunks(level, fromA.offset(shift), toA.offset(shift), false);
			check(helper, !budgeted.isFailed(), "the budgeted build failed");
			check(helper, budgetedBuilder.getBeforeCalls() >= 10, "preGeneration took only " + budgetedBuilder.getBeforeCalls() + " calls; the test needs it split");
			List<String> diffs = new ArrayList<>();
			boolean equal = equalVolumes(level, fromA, toA, shift, diffs);
			check(helper, equal, diffs.size() + "+ positions differ (instant / budgeted at offset): " + diffs);
			BlockPos above = originB.above();
			check(helper, budgetedBuilder.getAfterCalls() >= minAfterCalls, "postGeneration took " + budgetedBuilder.getAfterCalls() + " calls, expected at least " + minAfterCalls
					+ " (origin " + originB + " " + level.getBlockState(above) + ", biome " + level.getBiome(above) + ", snow " + level.getBiome(above).value().shouldSnow(level, above) + ")");
			int changed = countChanged(level, fromA, toA, bbA.min);
			int entitiesA = countEntities(level, fromA, toA);
			int entitiesB = countEntities(level, fromA.offset(shift), toA.offset(shift));
			check(helper, entitiesA == entitiesB, "entities: instant " + entitiesA + ", budgeted " + entitiesB);
			int snow = countBlock(level, fromA, toA, Blocks.SNOW.defaultBlockState());
			check(helper, !snowy || snow > 0, "no snow was sprinkled (postGeneration did nothing)");
			AncientWarfare.LOG.info("{}: identical after {} calls (preGeneration {} calls, longest {} ms; postGeneration {} calls), {} positions compared, {} snow layers",
					name, calls.get(), budgetedBuilder.getBeforeCalls(), budgetedBuilder.getLongestBeforeNanos() / 1000000, budgetedBuilder.getAfterCalls(), volume(fromA, toA), snow);
			check(helper, changed > 0, "nothing changed");
			helper.succeed();
		});
	}

	/*
	 * the terrain preparation of a town (biome replacement, clearing of the area up to 50 above it with its trees, the
	 * border smoothing, levelling) at once (the original calls) and as resumable steps with 1 ms per tick
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 3000)
	public static void improvementBudgetedTownPreparationMatchesInstant(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> lines = Arrays.asList("header:", "name=GameTestBudgetTown", "minSize=1", "maxSize=4", "selectionWeight=1", "clusterValue=0", "townBlockSize=20", "townPlotSize=7",
				"wallStyle=0", "wallSize=0", "exteriorSize=0", "exteriorPlotSize=0", "interiorEmptyPlotChance=0", "randomVillagersPerChunk=0", "biomeReplacement=minecraft:ice_spikes",
				"biomeWhiteList=false", "dimensionWhiteList=true", "dimensionList=0", ":endheader");
		TownTemplate town = TownTemplateParser.parseTemplate(lines).orElse(null);
		check(helper, town != null, "the town template was not parsed");
		check(helper, town.getBiomeReplacement().isPresent(), "the town template has no biome replacement");

		BlockPos far = helper.absolutePos(BlockPos.ZERO).offset(3000, 0, -3000);
		int cx = far.getX() >> 4;
		int cz = far.getZ() >> 4;
		TownBoundingArea areaA = new TownBoundingArea(cx, cz, cx + 3, cz + 3, BASE_Y - 3, BASE_Y + 37);
		TownBoundingArea areaB = new TownBoundingArea(cx + 8, cz, cx + 11, cz + 3, BASE_Y - 3, BASE_Y + 37);
		TownBoundingArea areaC = new TownBoundingArea(cx, cz + 8, cx + 3, cz + 11, BASE_Y - 3, BASE_Y + 37);
		BlockPos shift = new BlockPos(128, 0, 0);
		BlockPos shiftC = new BlockPos(0, 0, 128);
		TownGenerator genA = new TownGenerator(level, areaA, town);
		TownGenerator genB = new TownGenerator(level, areaB, town);
		TownGenerator genC = new TownGenerator(level, areaC, town);
		StructureBB maxA = genA.getMaximalBounds();
		StructureBB extA = genA.getExteriorBounds();
		check(helper, genA.getMaximalBounds().min.offset(shift).equals(genB.getMaximalBounds().min), "the two town areas are not congruent");
		int surfaceY = extA.min.getY() - 1;

		BlockPos fromA = new BlockPos(maxA.min.getX() - 8, surfaceY - 12, maxA.min.getZ() - 8);
		BlockPos toA = new BlockPos(maxA.max.getX() + 8, extA.max.getY() + 54, maxA.max.getZ() + 8);
		reserveForTest(level, fromA, toA);
		reserveForTest(level, fromA.offset(shift), toA.offset(shift));
		reserveForTest(level, fromA.offset(shiftC), toA.offset(shiftC));
		buildTownTerrain(level, fromA, toA, extA, surfaceY);
		buildTownTerrain(level, fromA.offset(shift), toA.offset(shift), genB.getExteriorBounds(), surfaceY);
		buildTownTerrain(level, fromA.offset(shiftC), toA.offset(shiftC), genC.getExteriorBounds(), surfaceY);
		//the whole volume is a snowy biome first: the replacement (another snowy biome) then draws no snow line through the
		//border, where the fuzzy biome lookup (position hashed) would differ between the two copies
		setBiome(helper, level, fromA, toA, "minecraft:snowy_plains");
		setBiome(helper, level, fromA.offset(shift), toA.offset(shift), "minecraft:snowy_plains");
		setBiome(helper, level, fromA.offset(shiftC), toA.offset(shiftC), "minecraft:snowy_plains");
		check(helper, equalVolumes(level, fromA, toA, shift, new ArrayList<>()), "the two terrain copies differ before the preparation");

		//the steps run over many ticks: the chunks of both copies stay loaded (in the game the town's chunk ticket does that)
		forceChunks(level, fromA, toA, true);
		forceChunks(level, fromA.offset(shift), toA.offset(shift), true);
		genA.prepareTerrain();
		//control: the original twice on equal terrain gives the same result (otherwise the comparison below means nothing)
		genC.prepareTerrain();
		List<String> controlDiffs = new ArrayList<>();
		check(helper, equalVolumes(level, fromA, toA, shiftC, controlDiffs), "the original preparation is not deterministic on this terrain: " + controlDiffs);
		ResumableSteps steps = genB.prepareTerrainSteps();
		AtomicBoolean compared = new AtomicBoolean();
		AtomicInteger calls = new AtomicInteger();
		long[] longest = new long[1];
		helper.onEachTick(() -> {
			if (compared.get()) {
				return;
			}
			calls.incrementAndGet();
			long start = System.nanoTime();
			boolean done = steps.run(start + 1000000L, false);
			longest[0] = Math.max(longest[0], System.nanoTime() - start);
			if (!done) {
				return;
			}
			compared.set(true);
			forceChunks(level, fromA, toA, false);
			forceChunks(level, fromA.offset(shift), toA.offset(shift), false);
			check(helper, calls.get() >= 3, "the preparation took only " + calls.get() + " calls at 1 ms; the test needs several");
			List<String> diffs = new ArrayList<>();
			boolean equal = equalVolumes(level, fromA, toA, shift, diffs);
			check(helper, equal, diffs.size() + "+ positions differ (instant / budgeted at offset): " + diffs + (level.isRaining() ? " (raining)" : ""));
			int biomesChanged = 0;
			for (int x = fromA.getX(); x <= toA.getX(); x += 4) {
				for (int z = fromA.getZ(); z <= toA.getZ(); z += 4) {
					BlockPos p = new BlockPos(x, surfaceY, z);
					var a = level.getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(x), net.minecraft.core.QuartPos.fromBlock(surfaceY), net.minecraft.core.QuartPos.fromBlock(z));
					var b = level.getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(x + shift.getX()), net.minecraft.core.QuartPos.fromBlock(surfaceY), net.minecraft.core.QuartPos.fromBlock(z + shift.getZ()));
					check(helper, a.equals(b), "biome at " + p + ": instant " + a + ", budgeted " + b);
					if (a.is(net.minecraft.world.level.biome.Biomes.ICE_SPIKES)) {
						biomesChanged++;
					}
				}
			}
			check(helper, biomesChanged > 0, "the biome was not replaced");
			int logs = countBlock(level, fromA, toA, Blocks.OAK_LOG.defaultBlockState());
			check(helper, logs == 0, logs + " logs of the trees in the town area are left");
			AncientWarfare.LOG.info("improvementBudgetedTownPreparationMatchesInstant: identical after {} calls at 1 ms per tick ({} units, longest call {} ms), {} positions compared, {} biome samples replaced",
					calls.get(), steps.getUnits(), longest[0] / 1000000, volume(fromA, toA), biomesChanged);
			helper.succeed();
		});
	}

	/* a 9 x 7 x 9 house (floor with a porch, walls with a door, roof, a chest and bookshelves; no light sources), scanned and removed */
	private static StructureTemplate scanHouse(ServerLevel level, BlockPos min, String name) {
		BlockPos max = min.offset(8, 6, 8);
		loadChunks(level, min, max);
		for (BlockPos p : BlockPos.betweenClosed(min, max)) {
			int x = p.getX() - min.getX();
			int y = p.getY() - min.getY();
			int z = p.getZ() - min.getZ();
			//z = 0 is an open porch (the key stands on it, so the snow test of postGeneration sees open sky)
			boolean wall = z >= 1 && (x == 0 || x == 8 || z == 1 || z == 8);
			BlockState state = Blocks.AIR.defaultBlockState();
			if (y == 0) {
				state = Blocks.STONE_BRICKS.defaultBlockState();
			} else if (y == 1 && z == 0 && x == 4) {
				//a step at the key: the snow test of the Ground validator's postGeneration looks at the block above the key
				state = Blocks.STONE_BRICKS.defaultBlockState();
			} else if (y == 6 && z >= 1) {
				state = Blocks.OAK_PLANKS.defaultBlockState();
			} else if (wall && !(z == 1 && x == 4 && y <= 2)) {
				state = Blocks.COBBLESTONE.defaultBlockState();
			} else if (y == 1 && x == 2 && z == 6) {
				state = Blocks.CHEST.defaultBlockState();
			} else if (x == 7 && z >= 2 && z <= 6 && y <= 3) {
				state = Blocks.BOOKSHELF.defaultBlockState();
			}
			level.setBlock(p, state, 2 | 16);
		}
		StructureTemplate template = TemplateScanner.scan(level, Collections.emptySet(), min, max, min.offset(4, 0, 0), 0, name);
		for (BlockPos p : BlockPos.betweenClosed(min, max)) {
			level.removeBlockEntity(p);
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
		}
		return template;
	}

	private static int height(int dx, int dz) {
		return (int) Math.round(2.5 * Math.sin(dx * 0.45) + 2.5 * Math.cos(dz * 0.35) + (dx + dz) * 0.08);
	}

	private static int townHeight(int dx, int dz) {
		return (int) Math.round(4 * Math.sin(dx * 0.3) + 3 * Math.cos(dz * 0.25));
	}

	/* uneven terrain around base (stone, dirt, grass), with oak trees at fixed places, over air */
	private static void buildTerrain(ServerLevel level, BlockPos from, BlockPos to, BlockPos base) {
		loadChunks(level, from, to);
		for (int x = from.getX(); x <= to.getX(); x++) {
			for (int z = from.getZ(); z <= to.getZ(); z++) {
				int dx = x - base.getX();
				int dz = z - base.getZ();
				int top = BASE_Y + height(dx, dz);
				for (int y = from.getY(); y <= to.getY(); y++) {
					BlockState state = y > top ? Blocks.AIR.defaultBlockState() : y == top ? Blocks.GRASS_BLOCK.defaultBlockState() : y >= top - 2 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
					level.setBlock(new BlockPos(x, y, z), state, 2 | 16);
				}
			}
		}
		for (int[] t : new int[][] {{-8, -6}, {3, 4}, {12, 2}, {-5, 12}, {14, 14}, {6, -9}}) {
			int x = base.getX() + t[0];
			int z = base.getZ() + t[1];
			if (x < from.getX() + 3 || x > to.getX() - 3 || z < from.getZ() + 3 || z > to.getZ() - 3) {
				continue;
			}
			tree(level, new BlockPos(x, BASE_Y + height(t[0], t[1]) + 1, z));
		}
	}

	/* flat ground at surfaceY around the town; hills and dips in the middle of the exterior area, trees */
	private static void buildTownTerrain(ServerLevel level, BlockPos from, BlockPos to, StructureBB exterior, int surfaceY) {
		loadChunks(level, from, to);
		for (int x = from.getX(); x <= to.getX(); x++) {
			for (int z = from.getZ(); z <= to.getZ(); z++) {
				//flat within 12 blocks of the exterior edge too: the smoothing reads reference points inside the area
				boolean inside = x >= exterior.min.getX() + 12 && x <= exterior.max.getX() - 12 && z >= exterior.min.getZ() + 12 && z <= exterior.max.getZ() - 12;
				int dx = x - exterior.min.getX();
				int dz = z - exterior.min.getZ();
				int top = surfaceY + (inside ? townHeight(dx, dz) : 0);
				for (int y = from.getY(); y <= to.getY(); y++) {
					BlockState state = y > top ? Blocks.AIR.defaultBlockState() : y == top ? Blocks.GRASS_BLOCK.defaultBlockState() : y >= top - 2 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
					level.setBlock(new BlockPos(x, y, z), state, 2 | 16);
				}
			}
		}
		for (int[] t : new int[][] {{6, 6}, {20, 9}, {33, 30}, {12, 38}, {40, 14}, {25, 25}}) {
			int x = exterior.min.getX() + t[0];
			int z = exterior.min.getZ() + t[1];
			tree(level, new BlockPos(x, surfaceY + townHeight(t[0], t[1]) + 1, z));
		}
	}

	private static void tree(ServerLevel level, BlockPos bottom) {
		for (int dy = 0; dy < 5; dy++) {
			level.setBlock(bottom.above(dy), Blocks.OAK_LOG.defaultBlockState(), 2 | 16);
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 3; dy <= 5; dy++) {
					BlockPos p = bottom.offset(dx, dy, dz);
					if ((dx != 0 || dz != 0 || dy == 5) && (dy < 5 || Math.abs(dx) <= 1 && Math.abs(dz) <= 1)) {
						level.setBlock(p, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 2 | 16);
					}
				}
			}
		}
	}

	private static void setBiome(GameTestHelper helper, ServerLevel level, BlockPos from, BlockPos to, String biome) {
		BiomeChanger changer = BiomeChanger.create(level, ResourceLocation.parse(biome)).orElse(null);
		check(helper, changer != null, "biome " + biome + " not found");
		for (BlockPos p : BlockPos.betweenClosed(new BlockPos(from.getX(), from.getY(), from.getZ()), new BlockPos(to.getX(), from.getY(), to.getZ()))) {
			changer.addColumn(p);
		}
		changer.apply();
	}

	/*
	 * The terrain is built in chunks that are new to the game, so the mod's world generator makes its structure and town
	 * attempts there on the next tick and found the test platform a valid place (seen: PMMOBSShriekerCircle and
	 * BrigandHighwayMan built at y 201 into one copy while the test ran, levelling and smoothing it). The area plus 32
	 * blocks (structure borders) is entered in the town map before that tick: structure attempts reject a bounding box
	 * that intersects a town, town attempts keep their distance to towns.
	 */
	private static void reserveForTest(ServerLevel level, BlockPos from, BlockPos to) {
		StructureBB bb = new StructureBB(new BlockPos(from.getX() - 32, level.getMinBuildHeight(), from.getZ() - 32), new BlockPos(to.getX() + 32, level.getMaxBuildHeight() - 1, to.getZ() + 32));
		net.shadowmage.ancientwarfare.core.gamedata.AWGameData.INSTANCE.getPerWorldData(level, net.shadowmage.ancientwarfare.structure.gamedata.TownMap.class)
				.setGenerated(new net.shadowmage.ancientwarfare.structure.gamedata.TownEntry(bb, false));
	}

	private static void forceChunks(ServerLevel level, BlockPos from, BlockPos to, boolean force) {
		for (int x = from.getX() >> 4; x <= to.getX() >> 4; x++) {
			for (int z = from.getZ() >> 4; z <= to.getZ() >> 4; z++) {
				level.setChunkForced(x, z, force);
			}
		}
	}

	private static void loadChunks(ServerLevel level, BlockPos from, BlockPos to) {
		for (int x = from.getX() >> 4; x <= to.getX() >> 4; x++) {
			for (int z = from.getZ() >> 4; z <= to.getZ() >> 4; z++) {
				level.getChunk(x, z);
			}
		}
	}

	private static long volume(BlockPos from, BlockPos to) {
		return (long) (to.getX() - from.getX() + 1) * (to.getY() - from.getY() + 1) * (to.getZ() - from.getZ() + 1);
	}

	private static boolean equalVolumes(ServerLevel level, BlockPos from, BlockPos to, BlockPos shift, List<String> diffs) {
		boolean equal = true;
		for (BlockPos p : BlockPos.betweenClosed(from, to)) {
			BlockState a = level.getBlockState(p);
			BlockState b = level.getBlockState(p.offset(shift));
			if (a != b) {
				equal = false;
				if (diffs.size() < 24) {
					diffs.add(p.subtract(from) + " " + a + " / " + b);
				}
			}
		}
		return equal;
	}

	/* positions that are no longer the terrain (or air) that buildTerrain put there; only to make sure the build did something */
	private static int countChanged(ServerLevel level, BlockPos from, BlockPos to, BlockPos base) {
		int changed = 0;
		for (BlockPos p : BlockPos.betweenClosed(from, to)) {
			BlockState s = level.getBlockState(p);
			if (s.is(Blocks.STONE_BRICKS) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.SNOW)) {
				changed++;
			}
		}
		return changed;
	}

	private static int countBlock(ServerLevel level, BlockPos from, BlockPos to, BlockState state) {
		int n = 0;
		for (BlockPos p : BlockPos.betweenClosed(from, to)) {
			if (level.getBlockState(p).getBlock() == state.getBlock()) {
				n++;
			}
		}
		return n;
	}

	private static int countEntities(ServerLevel level, BlockPos from, BlockPos to) {
		return level.getEntitiesOfClass(Entity.class, new net.minecraft.world.phys.AABB(from.getX(), from.getY(), from.getZ(), to.getX() + 1, to.getY() + 1, to.getZ() + 1), e -> !(e instanceof Player)).size();
	}
}
