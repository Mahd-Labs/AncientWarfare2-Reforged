package net.shadowmage.ancientwarfare.structure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.render.property.CoreProperties;
import net.shadowmage.ancientwarfare.structure.block.BlockBaseStructure;
import net.shadowmage.ancientwarfare.structure.block.BlockBench;
import net.shadowmage.ancientwarfare.structure.block.BlockChair;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBuilder;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBuilderTicked;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules.TemplateRuleBlockDoors;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules.TemplateRuleBlockTile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*
 * the 1.12 actual states: the original (1.12) templates build what 1.12 built. Expected values are
 * the 1.12.2 reference build (/awstructure build at y 4 on a superflat world, actual states as 1.12 rendered them;
 * the tester's reference logs of door facings, block dumps of the 1.12 reference for the connection totals). Structures are built far from the test grid on a platform like the 1.12 superflat ground.
 */
@GameTestHolder(AncientWarfare.MOD_ID)
@PrefixGameTestTemplate(false)
public class TemplateConversionGameTests {
	private static final String TEMPLATE = "automation_empty";

	private static void check(GameTestHelper helper, boolean condition, String message) {
		if (!condition) {
			helper.fail(message);
		}
	}

	private static Direction rotate(Direction facing, int turns) {
		for (int i = 0; i < turns; i++) {
			facing = facing.getClockWise();
		}
		return facing;
	}

	/*
	 * every upper-half door rule of the shipped 1.12 templates (the half that places the door) places the facing, hinge
	 * and open state saved in the template, turned with the structure
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void everyLegacyDoorRuleKeepsItsSavedFacing(GameTestHelper helper) {
		int[] counts = new int[3];
		List<String> wrong = new ArrayList<>();
		java.util.Set<String> templates = new java.util.HashSet<>();
		int files = LegacyTemplateRules.forEach(e -> {
			if (!(e.rule() instanceof TemplateRuleBlockDoors rule)) {
				return;
			}
			CompoundTag props = e.raw().getCompound("blockState").getCompound("properties");
			BlockState state = rule.getState(0);
			if (!(state.getBlock() instanceof DoorBlock) || !"upper".equals(props.getString("half"))) {
				return;
			}
			counts[0]++;
			templates.add(e.template());
			Direction saved = Direction.byName(props.getString("facing"));
			for (int turns = 0; turns < 4; turns++) {
				BlockState placed = rule.getState(turns);
				boolean ok = placed.getValue(DoorBlock.FACING) == rotate(saved, turns)
						&& placed.getValue(DoorBlock.HINGE).getSerializedName().equals(props.getString("hinge"))
						&& Boolean.toString(placed.getValue(DoorBlock.OPEN)).equals(props.getString("open"));
				if (!ok) {
					counts[1]++;
					if (wrong.size() < 5) {
						wrong.add(e.template() + " rule " + e.number() + " turns " + turns + ": " + placed);
					}
				}
			}
		});
		AncientWarfare.LOG.info("Legacy door rules: {} upper-half rules in {} of {} templates, {} wrong placements (4 turns each)", counts[0], templates.size(), files, counts[1]);
		check(helper, files >= 1484, "only " + files + " template files read");
		check(helper, counts[0] >= 1454, "only " + counts[0] + " upper-half door rules found");
		check(helper, counts[1] == 0, counts[1] + " door placements differ from the saved state, e.g. " + wrong);
		helper.succeed();
	}

	/* every chair rule of the shipped 1.12 templates places the facing saved in the rule (the 1.12 tile facing) */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void everyLegacyChairRuleKeepsItsSavedFacing(GameTestHelper helper) {
		int[] counts = new int[2];
		List<String> wrong = new ArrayList<>();
		LegacyTemplateRules.forEach(e -> {
			if (!(e.rule() instanceof TemplateRuleBlockTile<?> rule) || !(rule.getState(0).getBlock() instanceof BlockChair)) {
				return;
			}
			counts[0]++;
			Direction saved = Direction.byName(e.raw().getString("facing"));
			for (int turns = 0; turns < 4; turns++) {
				if (saved == null || rule.getState(turns).getValue(CoreProperties.FACING) != rotate(saved, turns)) {
					counts[1]++;
					if (wrong.size() < 5) {
						wrong.add(e.template() + " rule " + e.number() + " saved " + saved + " turns " + turns + ": " + rule.getState(turns));
					}
				}
			}
		});
		AncientWarfare.LOG.info("Legacy chair rules: {}, {} wrong placements (4 turns each)", counts[0], counts[1]);
		check(helper, counts[0] >= 5800, "only " + counts[0] + " chair rules found");
		check(helper, counts[1] == 0, counts[1] + " chair placements differ from the saved facing, e.g. " + wrong);
		helper.succeed();
	}

	/* "facing dx dy dz" relative to the build position, 1.12 values (doors: facing half hinge open) */
	private static final String[] WINDMILL_DOORS_112 = {
			"south -10 0 -5 south lower right false", "south -9 0 -5 south lower left false", "south -10 1 -5 south upper right false",
			"south -9 1 -5 south upper left false", "south -2 0 0 south lower right false", "south -2 1 0 south upper right false",
			"south 3 0 2 south lower right false", "south 3 1 2 south upper right false", "south -9 0 5 south lower left false",
			"south -8 0 5 south lower right false", "south -9 1 5 south upper left false", "south -8 1 5 south upper right false",
			"east 2 0 -3 east lower right false", "east 2 1 -3 east upper right false", "east 0 0 2 east lower right false",
			"east 0 1 2 east upper right false", "east 5 0 8 east lower right false", "east 5 1 8 east upper right false",
			"east -5 0 9 east lower left false", "east 5 0 9 east lower left false", "east -5 1 9 east upper left false",
			"east 5 1 9 east upper left false", "east -5 0 10 east lower right false", "east -5 1 10 east upper right false"};
	private static final String[] WINDMILL_CHAIRS_112 = {
			"east -1 0 10 east", "east -1 0 8 east", "east -1 0 9 east", "east -1 1 10 east", "east -1 1 8 east", "east -1 1 9 east",
			"east 2 0 10 west", "east 2 0 8 west", "east 2 0 9 west", "east 2 1 10 west", "east 2 1 8 west", "east 2 1 9 west",
			"south -10 0 -1 south", "south -10 0 2 north", "south -10 1 -1 south", "south -10 1 2 north", "south -8 0 -1 south",
			"south -8 0 2 north", "south -8 1 -1 south", "south -8 1 2 north", "south -9 0 -1 south", "south -9 0 2 north",
			"south -9 1 -1 south", "south -9 1 2 north"};

	/* NoggWindmill2  facing south and east: every door half and chair as 1.12 placed it */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void windmillDoorsAndChairsMatch112(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = StructureTemplateManager.getTemplate("NoggWindmill2").orElse(null);
		check(helper, template != null, "NoggWindmill2 not loaded");
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-6000, 0, 6000);
		Map<String, BlockPos> hits = new TreeMap<>();
		List<StructureBB> boxes = new ArrayList<>();
		for (Direction face : new Direction[] {Direction.SOUTH, Direction.EAST}) {
			BlockPos hit = new BlockPos(base.getX() + (face == Direction.EAST ? 100 : 0), 100, base.getZ());
			hits.put(face.getName(), hit);
			StructureBuilder builder = new StructureBuilder(level, template, face, hit);
			platform(level, builder.getBoundingBox(), hit.getY());
			builder.instantConstruction();
			boxes.add(builder.getBoundingBox());
		}
		List<String> wrong = new ArrayList<>();
		int doors = 0;
		for (String line : WINDMILL_DOORS_112) {
			String[] f = line.split(" ");
			BlockState state = level.getBlockState(hits.get(f[0]).offset(Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3])));
			if (!(state.getBlock() instanceof DoorBlock)) {
				wrong.add(line + " -> " + state);
				continue;
			}
			doors++;
			String actual = state.getValue(DoorBlock.FACING).getName() + " " + state.getValue(DoorBlock.HALF).getSerializedName() + " " + state.getValue(DoorBlock.HINGE).getSerializedName() + " " + state.getValue(DoorBlock.OPEN);
			if (!actual.equals(f[4] + " " + f[5] + " " + f[6] + " " + f[7])) {
				wrong.add(line + " -> " + actual);
			}
		}
		int chairs = 0;
		for (String line : WINDMILL_CHAIRS_112) {
			String[] f = line.split(" ");
			BlockState state = level.getBlockState(hits.get(f[0]).offset(Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3])));
			if (!(state.getBlock() instanceof BlockChair) || !state.getValue(CoreProperties.FACING).getName().equals(f[4])) {
				wrong.add(line + " -> " + state);
			} else {
				chairs++;
			}
		}
		boxes.forEach(bb -> clear(level, bb));
		check(helper, wrong.isEmpty(), "differ from 1.12: " + wrong);
		check(helper, doors == 24 && chairs == 24, doors + " door halves, " + chairs + " chair blocks");
		helper.succeed();
	}

	/*
	 * Panes, fences, walls, corner stairs, Ancient Warfare benches, chests side by side (one double chest in 1.12): built all at once (world generation, command,
	 * creative builder), by the world generation builder (StructureBuilderWorldGen, budgeted placement) and by the
	 * survival builder (StructureBuilderTicked, one rule per craftsman's work unit), every
	 * such block shows the state its neighbours give it (1.12 getActualState), and the totals equal the 1.12 build.
	 * NoggBlacksmithAndStables (corner stairs, fences), SurvivalBuildFarmHouse (a survival template: panes, walls,
	 * fences, benches) and EmpireBlacksmithLarge (a tester's template: panes, iron bars, fences, corner stairs),
	 * facing south and east. Totals are checked for the paths that place every rule (not the survival builder).
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 1200)
	public static void connectionsMatch112OnBothBuildPaths(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		//1.12.2 reference build, both facings alike
		Map<String, Map<String, Integer>> expected = Map.of(
				"EmpireBlacksmithLarge", Map.ofEntries(Map.entry("chest blocks", 5), Map.entry("chest paired", 2), Map.entry("fence blocks", 63), Map.entry("fence sides", 110),
						Map.entry("pane blocks", 24), Map.entry("pane sides", 46), Map.entry("stairs blocks", 195), Map.entry("stairs corner", 3), Map.entry("wall blocks", 1), Map.entry("wall up", 1)),
				"NoggBlacksmithAndStables", Map.ofEntries(Map.entry("chest blocks", 3), Map.entry("chest paired", 2), Map.entry("fence blocks", 266), Map.entry("fence sides", 496), Map.entry("pane blocks", 17), Map.entry("pane sides", 30),
						Map.entry("stairs blocks", 406), Map.entry("stairs corner", 16), Map.entry("wall blocks", 1), Map.entry("wall up", 1)),
				"SurvivalBuildFarmHouse", Map.ofEntries(Map.entry("bench blocks", 4), Map.entry("bench legs both", 4), Map.entry("chest blocks", 3), Map.entry("chest paired", 2), Map.entry("fence blocks", 18), Map.entry("fence sides", 26),
						Map.entry("pane blocks", 12), Map.entry("pane sides", 24), Map.entry("stairs blocks", 69), Map.entry("wall blocks", 21), Map.entry("wall sides", 39), Map.entry("wall up", 5)));
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(6000, 0, 6000);
		List<String> problems = new ArrayList<>();
		int offset = 0;
		for (Map.Entry<String, Map<String, Integer>> entry : new TreeMap<>(expected).entrySet()) {
			StructureTemplate template = StructureTemplateManager.getTemplate(entry.getKey()).orElse(null);
			check(helper, template != null, entry.getKey() + " not loaded");
			for (Direction face : new Direction[] {Direction.SOUTH, Direction.EAST}) {
				for (String path : new String[] {"instant", "world generation", "survival builder"}) {
					boolean ticked = path.equals("survival builder");
					BlockPos hit = new BlockPos(base.getX() + offset, 100, base.getZ());
					offset += 100;
					StructureBB bb;
					if (path.equals("world generation")) {
						//what WorldStructureGenerator queues, run by the budgeted placement (validator steps before and after)
						net.shadowmage.ancientwarfare.structure.template.build.StructureBuilderWorldGen builder = new net.shadowmage.ancientwarfare.structure.template.build.StructureBuilderWorldGen(level, template, face, hit);
						platform(level, builder.getBoundingBox(), hit.getY());
						int calls = 0;
						while (!builder.constructUntil(System.nanoTime(), true) && calls++ < 1000) {
							//unlimited: one call places everything
						}
						bb = builder.getBoundingBox();
					} else if (ticked) {
						StructureBuilderTicked builder = new StructureBuilderTicked(level, template, face, hit);
						platform(level, builder.getBoundingBox(), hit.getY());
						int ticks = 0;
						while (!builder.isFinished() && ticks++ < 2_000_000) {
							builder.tick();
						}
						bb = builder.getBoundingBox();
					} else {
						StructureBuilder builder = new StructureBuilder(level, template, face, hit);
						platform(level, builder.getBoundingBox(), hit.getY());
						builder.instantConstruction();
						bb = builder.getBoundingBox();
					}
					String label = entry.getKey() + " " + face.getName() + " " + path;
					Map<String, Integer> totals = new TreeMap<>();
					for (BlockPos p : BlockPos.betweenClosed(bb.min, bb.max)) {
						BlockState state = level.getBlockState(p);
						String family = family(state);
						if (family == null) {
							continue;
						}
						BlockState neighbours = state.getBlock() instanceof BlockBaseStructure structureBlock ? structureBlock.getActualState(state, level, p) : Block.updateFromNeighbourShapes(state, level, p);
						if (!neighbours.equals(state) && problems.size() < 12) {
							problems.add(label + " " + p.immutable() + " " + state + " but its neighbours give " + neighbours);
						}
						count(totals, family, state);
					}
					if (!ticked && !totals.equals(new TreeMap<>(entry.getValue()))) {
						problems.add(label + " totals " + totals + ", 1.12 " + new TreeMap<>(entry.getValue()));
					}
					AncientWarfare.LOG.info("Connections {}: {}", label, totals);
					clear(level, bb);
				}
			}
		}
		check(helper, problems.isEmpty(), "differ from 1.12: " + problems);
		helper.succeed();
	}

	/*
	 * every leaves rule of the shipped 1.12 templates builds leaves (they were air: the block state fixer leaves the
	 * 1.12 decayable / check_decay properties on the new leaves blocks); persistent unless 1.12 had both set
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void everyLegacyLeavesRuleBuildsLeaves(GameTestHelper helper) {
		int[] counts = new int[2];
		List<String> wrong = new ArrayList<>();
		LegacyTemplateRules.forEach(e -> {
			String name = e.raw().getCompound("blockState").getString("blockName");
			if (!name.equals("minecraft:leaves") && !name.equals("minecraft:leaves2") || !(e.rule() instanceof net.shadowmage.ancientwarfare.structure.api.TemplateRuleBlock rule)) {
				return;
			}
			counts[0]++;
			CompoundTag props = e.raw().getCompound("blockState").getCompound("properties");
			boolean decays = "true".equals(props.getString("decayable")) && "true".equals(props.getString("check_decay"));
			BlockState state = rule.getState(0);
			if (!(state.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) || state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT) == decays) {
				counts[1]++;
				if (wrong.size() < 5) {
					wrong.add(e.template() + " rule " + e.number() + " " + props + " -> " + state);
				}
			}
		});
		AncientWarfare.LOG.info("Legacy leaves rules: {}, {} wrong", counts[0], counts[1]);
		check(helper, counts[0] >= 1100, "only " + counts[0] + " leaves rules found");
		check(helper, counts[1] == 0, counts[1] + " leaves rules wrong, e.g. " + wrong);
		helper.succeed();
	}

	/*
	 * 1.12 showed the gravestone variant held by the tile (getActualState); RakshasaMesaTemple saves gravestones whose
	 * block state says variant 1 and whose tile says 5: every built gravestone shows its tile's variant
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void gravestonesShowTheirTileVariant(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = StructureTemplateManager.getTemplate("RakshasaMesaTemple").orElse(null);
		check(helper, template != null, "RakshasaMesaTemple not loaded");
		BlockPos hit = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX() + 6000, 100, helper.absolutePos(BlockPos.ZERO).getZ() - 6000);
		StructureBuilder builder = new StructureBuilder(level, template, Direction.EAST, hit);
		platform(level, builder.getBoundingBox(), hit.getY());
		builder.instantConstruction();
		StructureBB bb = builder.getBoundingBox();
		int gravestones = 0;
		int notOne = 0;
		List<String> wrong = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(bb.min, bb.max)) {
			if (level.getBlockEntity(p) instanceof net.shadowmage.ancientwarfare.structure.tile.TileGravestone te) {
				gravestones++;
				int stateVariant = level.getBlockState(p).getValue(net.shadowmage.ancientwarfare.structure.block.BlockGravestone.VARIANT);
				notOne += te.getVariant() != 1 ? 1 : 0;
				if (stateVariant != te.getVariant() && wrong.size() < 5) {
					wrong.add(p.immutable() + " state " + stateVariant + " tile " + te.getVariant());
				}
			}
		}
		clear(level, bb);
		check(helper, gravestones > 0 && notOne > 0, gravestones + " gravestones, " + notOne + " with a variant other than 1");
		check(helper, wrong.isEmpty(), "gravestones whose block state differs from the tile: " + wrong);
		helper.succeed();
	}

	private static final String[] SKULLS_112 = {"skeleton", "wither_skeleton", "zombie", "player", "creeper", "dragon"};

	/*
	 * every bed, skull, banner, sign and flower pot rule of the shipped 1.12 templates places what its 1.12 tile showed:
	 * bed colour, skull type / rotation / facing / owner, banner colour and patterns, sign text, the plant in the pot
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void everyLegacyTileBlockRuleKeepsItsLook(GameTestHelper helper) {
		Map<String, Integer> counts = new TreeMap<>();
		List<String> wrong = new ArrayList<>();
		LegacyTemplateRules.forEach(e -> {
			if (!List.of("bed", "vanillaSkull", "banner", "vanillaSign", "vanillaFlowerPot").contains(e.plugin())) {
				return;
			}
			counts.merge(e.plugin(), 1, Integer::sum);
			CompoundTag raw = e.raw();
			CompoundTag props = raw.getCompound("blockState").getCompound("properties");
			CompoundTag te = raw.getCompound("teData");
			if (!(e.rule() instanceof TemplateRuleBlockTile<?> rule)) {
				wrong.add(e.template() + " rule " + e.number() + " not a tile rule");
				return;
			}
			BlockState state = rule.getState(0);
			String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
			CompoundTag tile = rule.tag == null ? new CompoundTag() : rule.tag;
			String expected = switch (e.plugin()) {
				case "bed" -> net.minecraft.world.item.DyeColor.byId(raw.getInt("bedColor")).getName() + "_bed facing=" + props.getString("facing") + " part=" + props.getString("part");
				case "vanillaSkull" -> {
					String type = SKULLS_112[te.getByte("SkullType")];
					String suffix = type.equals("skeleton") || type.equals("wither_skeleton") ? "skull" : "head";
					yield "up".equals(props.getString("facing")) ? type + "_" + suffix + " rotation=" + raw.getInt("skullRotation") + " profile=" + te.contains("Owner")
							: type + "_wall_" + suffix + " facing=" + props.getString("facing") + " profile=" + te.contains("Owner");
				}
				case "banner" -> net.minecraft.world.item.DyeColor.byId(15 - te.getInt("Base")).getName() + (raw.getCompound("blockState").getString("blockName").contains("wall") ? "_wall_banner" : "_banner")
						+ " patterns=" + te.getList("Patterns", 10).size();
				case "vanillaSign" -> "text=" + java.util.stream.IntStream.range(0, 4).mapToObj(i -> raw.getString("signContents" + i).replaceAll("(§r)+$", "")).toList();
				default -> raw.contains("itemName") && !raw.getString("itemName").equals("minecraft:air") ? "potted" : "flower_pot";
			};
			String actual = switch (e.plugin()) {
				case "bed" -> path + " facing=" + state.getValue(net.minecraft.world.level.block.BedBlock.FACING).getName() + " part=" + state.getValue(net.minecraft.world.level.block.BedBlock.PART).getSerializedName();
				case "vanillaSkull" -> path + (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16)
						? " rotation=" + state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16)
						: " facing=" + state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING).getName()) + " profile=" + tile.contains("profile");
				case "banner" -> path + " patterns=" + tile.getList("patterns", 10).size();
				case "vanillaSign" -> "text=" + tile.getCompound("front_text").getList("messages", 8).stream()
						.map(t -> com.google.gson.JsonParser.parseString(t.getAsString()).getAsString()).toList();
				default -> path.startsWith("potted_") ? "potted" : path;
			};
			if (!expected.equals(actual)) {
				counts.merge("wrong " + e.plugin(), 1, Integer::sum);
				if (wrong.size() < 8) {
					wrong.add(e.template() + " rule " + e.number() + " expected " + expected + " got " + actual);
				}
			}
		});
		AncientWarfare.LOG.info("Legacy tile block rules: {}", counts);
		check(helper, counts.getOrDefault("bed", 0) >= 2900 && counts.getOrDefault("vanillaSkull", 0) >= 2900 && counts.getOrDefault("banner", 0) >= 1700
				&& counts.getOrDefault("vanillaSign", 0) >= 1600 && counts.getOrDefault("vanillaFlowerPot", 0) >= 900, "rules found: " + counts);
		check(helper, wrong.isEmpty(), "rules that differ from their 1.12 data " + counts + ": " + wrong);
		helper.succeed();
	}

	/*
	 * OrcVillageSmallRoofedForest (skeleton, wither skeleton and player skulls (no owner), beds) facing east,
	 * SmingolNobleYurtWagon (banners, signs, beds, a flower pot) facing south, EmpireCityGen2MarketStalls (wall signs with
	 * text) facing east and EmpireCityGenHousing3 (standing and wall signs with text) facing west: every block of those rules is built as its rule says, turned
	 * with the structure, with its block entity data (banner patterns, sign text; skull owners: the pack-wide test).
	 * The same builds were compared with the 1.12.2 reference build block by block (skull type / rotation / facing, bed
	 * colour, banner colour / rotation / patterns, sign text): all equal except the lone bed half below
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void legacyTileBlocksBuildWithTheirData(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-6000, 0, -6000);
		Map<String, Integer> counts = new TreeMap<>();
		List<String> wrong = new ArrayList<>();
		int offset = 0;
		Map<String, Direction> builds = Map.of("OrcVillageSmallRoofedForest", Direction.EAST, "SmingolNobleYurtWagon", Direction.SOUTH,
				"EmpireCityGen2MarketStalls", Direction.EAST, "EmpireCityGenHousing3", Direction.WEST);
		for (String name : new TreeMap<>(builds).keySet()) {
			StructureTemplate template = StructureTemplateManager.getTemplate(name).orElse(null);
			check(helper, template != null, name + " not loaded");
			Direction face = builds.get(name);
			BlockPos hit = new BlockPos(base.getX() + offset, 100, base.getZ());
			offset += 100;
			StructureBuilder builder = new StructureBuilder(level, template, face, hit);
			platform(level, builder.getBoundingBox(), hit.getY());
			builder.instantConstruction();
			StructureBB bb = builder.getBoundingBox();
			int turns = (face.get2DDataValue() + 2) % 4;
			net.minecraft.core.Vec3i size = template.getSize();
			for (BlockPos p : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
				var rule = template.getRuleAt(p).orElse(null);
				if (!(rule instanceof net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.blockrules.TemplateRuleLegacyTileBlock legacy)) {
					continue;
				}
				BlockPos world = net.shadowmage.ancientwarfare.core.util.BlockTools.rotateInArea(p, size.getX(), size.getZ(), turns).offset(bb.min);
				BlockState placed = level.getBlockState(world);
				String kind = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(placed.getBlock()).getPath();
				counts.merge(kind, 1, Integer::sum);
				if (placed.isAir() && legacy.getState(turns).getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
					//a bed half without its other half in the template (SmingolNobleYurtWagon): 1.12 kept it, 1.21 removes it as
					//soon as the neighbour where the other half belongs is set
					counts.merge("lone bed half removed", 1, Integer::sum);
					continue;
				}
				if (!placed.equals(legacy.getState(turns))) {
					wrong.add(name + " " + p + " " + placed + " rule says " + legacy.getState(turns));
					continue;
				}
				net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(world);
				if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign && !sign.getFrontText().getMessage(0, false).getString().equals(
						com.google.gson.JsonParser.parseString(legacy.tag.getCompound("front_text").getList("messages", 8).getString(0)).getAsString())) {
					wrong.add(name + " " + p + " sign text " + sign.getFrontText().getMessage(0, false).getString());
				}
				if (be instanceof net.minecraft.world.level.block.entity.BannerBlockEntity banner && banner.getPatterns().layers().size() != legacy.tag.getList("patterns", 10).size()) {
					wrong.add(name + " " + p + " banner patterns " + banner.getPatterns().layers().size());
				}
				if (be instanceof net.minecraft.world.level.block.entity.SkullBlockEntity skull && legacy.tag.contains("profile") && skull.getOwnerProfile() == null) {
					wrong.add(name + " " + p + " player head without its owner");
				}
				if (be instanceof net.minecraft.world.level.block.entity.SkullBlockEntity skull && skull.getOwnerProfile() != null) {
					counts.merge("skull with owner", 1, Integer::sum);
				}
				if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign && java.util.Arrays.stream(sign.getFrontText().getMessages(false)).anyMatch(c -> !c.getString().isEmpty())) {
					counts.merge("sign with text", 1, Integer::sum);
				}
			}
			clear(level, bb);
		}
		AncientWarfare.LOG.info("Legacy tile blocks built: {}", counts);
		check(helper, wrong.isEmpty(), "differ from their rules: " + wrong.subList(0, Math.min(8, wrong.size())));
		check(helper, counts.getOrDefault("sign with text", 0) >= 5 && counts.getOrDefault("light_blue_wall_banner", 0) > 0 && counts.keySet().stream().anyMatch(k -> k.endsWith("_bed") && !k.startsWith("red"))
				&& counts.keySet().stream().anyMatch(k -> k.endsWith("wither_skeleton_skull") || k.endsWith("wither_skeleton_wall_skull")), "built: " + counts);
		helper.succeed();
	}

	/*
	 * 1.12 templates save every upper double plant half as "sunflower" (the kind was in the lower half's metadata).
	 * NoggTownGenHouse13 facing south and NoggTownGenHouse7 facing east: the 1.12.2 build shows 3 lilacs each (6
	 * blocks); without the fix all 12 blocks were gone (sunflower top on lilac bottom, the halves removed each other)
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void doublePlantsKeepTheirKind(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(BlockPos.ZERO).offset(-20000, 0, 6000);
		Map<String, Integer> counts = new TreeMap<>();
		List<String> wrong = new ArrayList<>();
		int offset = 0;
		for (String name : new String[] {"NoggTownGenHouse13", "NoggTownGenHouse7"}) {
			StructureTemplate template = StructureTemplateManager.getTemplate(name).orElse(null);
			check(helper, template != null, name + " not loaded");
			BlockPos hit = new BlockPos(base.getX() + offset, 100, base.getZ());
			offset += 100;
			StructureBuilder builder = new StructureBuilder(level, template, name.endsWith("13") ? Direction.SOUTH : Direction.EAST, hit);
			platform(level, builder.getBoundingBox(), hit.getY());
			builder.instantConstruction();
			StructureBB bb = builder.getBoundingBox();
			for (BlockPos p : BlockPos.betweenClosed(bb.min, bb.max)) {
				BlockState state = level.getBlockState(p);
				if (state.getBlock() instanceof net.minecraft.world.level.block.DoublePlantBlock) {
					counts.merge(name + " " + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath(), 1, Integer::sum);
					BlockPos other = state.getValue(net.minecraft.world.level.block.DoublePlantBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? p.above() : p.below();
					if (level.getBlockState(other).getBlock() != state.getBlock()) {
						wrong.add(name + " " + p.immutable() + " " + state + " other half " + level.getBlockState(other));
					}
				}
			}
			clear(level, bb);
		}
		check(helper, wrong.isEmpty(), "double plants without their other half: " + wrong);
		check(helper, counts.equals(Map.of("NoggTownGenHouse13 lilac", 6, "NoggTownGenHouse7 lilac", 6)), "built " + counts + ", 1.12: 6 lilac blocks each");
		helper.succeed();
	}

	private static String family(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof StairBlock) {
			return "stairs";
		} else if (block instanceof WallBlock) {
			return "wall";
		} else if (block instanceof BlockBench) {
			return "bench";
		} else if (block instanceof net.minecraft.world.level.block.FenceBlock) {
			return "fence";
		} else if (block instanceof CrossCollisionBlock) {
			return "pane";
		} else if (block instanceof net.minecraft.world.level.block.ChestBlock) {
			return "chest";
		}
		return null;
	}

	private static void count(Map<String, Integer> totals, String family, BlockState state) {
		totals.merge(family + " blocks", 1, Integer::sum);
		int sides = 0;
		if (state.getBlock() instanceof CrossCollisionBlock) {
			sides = (state.getValue(CrossCollisionBlock.NORTH) ? 1 : 0) + (state.getValue(CrossCollisionBlock.EAST) ? 1 : 0) + (state.getValue(CrossCollisionBlock.SOUTH) ? 1 : 0) + (state.getValue(CrossCollisionBlock.WEST) ? 1 : 0);
		} else if (state.getBlock() instanceof WallBlock) {
			sides = (state.getValue(WallBlock.NORTH_WALL) != WallSide.NONE ? 1 : 0) + (state.getValue(WallBlock.EAST_WALL) != WallSide.NONE ? 1 : 0) + (state.getValue(WallBlock.SOUTH_WALL) != WallSide.NONE ? 1 : 0) + (state.getValue(WallBlock.WEST_WALL) != WallSide.NONE ? 1 : 0);
			if (state.getValue(WallBlock.UP)) {
				totals.merge("wall up", 1, Integer::sum);
			}
		} else if (state.getBlock() instanceof StairBlock && state.getValue(StairBlock.SHAPE) != StairsShape.STRAIGHT) {
			totals.merge("stairs corner", 1, Integer::sum);
		} else if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock && state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
			totals.merge("chest paired", 1, Integer::sum);
		} else if (state.getBlock() instanceof BlockBench) {
			totals.merge("bench legs " + valueName(state, state.getBlock().getStateDefinition().getProperty("legs")), 1, Integer::sum);
		}
		if (sides > 0) {
			totals.merge(family + " sides", sides, Integer::sum);
		}
	}

	private static <T extends Comparable<T>> String valueName(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property) {
		return property.getName(state.getValue(property));
	}

	/* the 1.12 reference world: superflat, grass at the build height - 1, dirt below */
	private static void platform(ServerLevel level, StructureBB bb, int y) {
		for (int x = bb.min.getX() - 3; x <= bb.max.getX() + 3; x++) {
			for (int z = bb.min.getZ() - 3; z <= bb.max.getZ() + 3; z++) {
				level.setBlock(new BlockPos(x, y - 3, z), Blocks.DIRT.defaultBlockState(), 2);
				level.setBlock(new BlockPos(x, y - 2, z), Blocks.DIRT.defaultBlockState(), 2);
				level.setBlock(new BlockPos(x, y - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
			}
		}
	}

	private static void clear(ServerLevel level, StructureBB bb) {
		for (BlockPos p : BlockPos.betweenClosed(bb.min.offset(-3, -4, -3), bb.max.offset(3, 1, 3))) {
			if (level.getBlockEntity(p) instanceof net.minecraft.world.Container container) {
				container.clearContent();
			}
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
		}
		level.getEntitiesOfClass(Entity.class, bb.getAABB().inflate(4), e -> !(e instanceof Player)).forEach(Entity::discard);
	}

	/*
	 * Packs like ATM10 8.6 return data fixer results backed by immutable maps; the conversion works on a changeable copy
	 * (beta.8 changed the result itself: every 1.12 leaves rule failed there and was built as air)
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 20)
	public static void legacyConversionCopesWithImmutableFixerResults(GameTestHelper helper) {
		CompoundTag inner = new CompoundTag(Map.of("decayable", net.minecraft.nbt.StringTag.valueOf("false"), "variant", net.minecraft.nbt.StringTag.valueOf("jungle"))) {};
		CompoundTag outer = new CompoundTag(Map.of("Properties", inner, "Name", net.minecraft.nbt.StringTag.valueOf("minecraft:jungle_leaves"))) {};
		boolean threw = false;
		try {
			inner.remove("decayable");
		}
		catch (UnsupportedOperationException e) {
			threw = true;
		}
		check(helper, threw, "the immutable test tag accepted a change");
		CompoundTag copy = (CompoundTag) net.shadowmage.ancientwarfare.structure.util.LegacyBlockStates.mutableCopy(outer);
		check(helper, copy.equals(outer), "copy differs: " + copy);
		copy.getCompound("Properties").remove("decayable");
		copy.getCompound("Properties").putString("persistent", "true");
		check(helper, inner.contains("decayable") && !inner.contains("persistent"), "the original changed");
		CompoundTag legacy = new CompoundTag();
		legacy.putString("blockName", "minecraft:leaves");
		CompoundTag legacyProperties = new CompoundTag();
		legacyProperties.putString("check_decay", "false");
		legacyProperties.putString("decayable", "false");
		legacyProperties.putString("variant", "jungle");
		legacy.put("properties", legacyProperties);
		CompoundTag upgraded = net.shadowmage.ancientwarfare.structure.util.LegacyBlockStates.upgrade(legacy);
		check(helper, "minecraft:jungle_leaves".equals(upgraded.getString("blockName")) && "true".equals(upgraded.getCompound("properties").getString("persistent"))
				&& !upgraded.getCompound("properties").contains("decayable"), "leaves upgraded to " + upgraded);
		helper.succeed();
	}
}
