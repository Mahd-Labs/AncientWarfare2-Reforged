package net.shadowmage.ancientwarfare.structure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.template.build.BudgetedBuild;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBuilder;
import net.shadowmage.ancientwarfare.structure.template.build.StructurePlacementQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Improvement (not baseline): budgeted_placement in ancientwarfare-structure.toml, section improvements.
 * The builds stand high above the test floor (the templates are larger than the test area), each test at its own
 * height, and are removed again.
 */
@GameTestHolder(AncientWarfare.MOD_ID)
@PrefixGameTestTemplate(false)
public class BudgetedPlacementGameTests {
	private static final String TEMPLATE = "automation_empty";
	//a pack template with block entities (spawners, chests, furnace, beds, signs) and entities (gates, a vehicle, an armor stand)
	private static final String PACK_TEMPLATE = "EmpireBorderTower";

	private static void check(GameTestHelper helper, boolean condition, String message) {
		if (!condition) {
			helper.fail(message);
		}
	}

	/*
	 * the same template once with instantConstruction and once budgeted with 1 ms per tick (several ticks): every
	 * block state, every block entity's data (without its position; without the running delay of the advanced spawners,
	 * which count down from the tick they were placed, and the uuid of the gate a gate proxy belongs to, random for every
	 * gate entity) and the entities by type are the same
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 1500)
	public static void improvementBudgetedPlacementMatchesInstantBuild(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = StructureTemplateManager.getTemplate(PACK_TEMPLATE).orElse(null);
		check(helper, template != null, "the pack template " + PACK_TEMPLATE + " is not loaded");
		BlockPos posInstant = helper.absolutePos(new BlockPos(0, 100, 0));
		BlockPos posBudgeted = posInstant.offset(0, 0, 40);
		StructureBuilder instant = new StructureBuilder(level, template, Direction.NORTH, posInstant);
		StructureBuilder budgetedBuilder = new StructureBuilder(level, template, Direction.NORTH, posBudgeted);
		StructureBB bbInstant = instant.getBoundingBox();
		StructureBB bbBudgeted = budgetedBuilder.getBoundingBox();
		BlockPos shift = bbBudgeted.min.subtract(bbInstant.min);

		instant.instantConstruction();
		Snapshot expected = Snapshot.of(level, bbInstant);
		check(helper, expected.blockEntities > 0 && expected.entities.values().stream().mapToInt(Integer::intValue).sum() > 0, "the template built no block entities or no entities: " + expected.blockEntities + " / " + expected.entities);

		BudgetedBuild budgeted = new BudgetedBuild(budgetedBuilder, null, null, false);
		AtomicBoolean compared = new AtomicBoolean();
		helper.onEachTick(() -> {
			if (compared.get()) {
				return;
			}
			if (!budgeted.work(System.nanoTime() + 1000000L, false)) {
				return;
			}
			compared.set(true);
			try {
				check(helper, !budgeted.isFailed(), "the budgeted build failed");
				check(helper, budgeted.getPlacingTicks() >= 3, "the budgeted build took only " + budgeted.getPlacingTicks() + " ticks; the test needs several");
				Snapshot actual = Snapshot.of(level, bbBudgeted);
				for (Map.Entry<BlockPos, String> e : expected.states.entrySet()) {
					String other = actual.states.get(e.getKey());
					check(helper, e.getValue().equals(other), "block at offset " + e.getKey() + ": instant " + e.getValue() + ", budgeted " + other);
				}
				check(helper, expected.states.size() == actual.states.size(), "different number of positions");
				check(helper, expected.blockEntities == actual.blockEntities, "block entities: instant " + expected.blockEntities + ", budgeted " + actual.blockEntities);
				for (Map.Entry<BlockPos, String> e : expected.tileData.entrySet()) {
					String other = actual.tileData.get(e.getKey());
					check(helper, e.getValue().equals(other), "block entity at offset " + e.getKey() + ": instant " + e.getValue() + ", budgeted " + other);
				}
				check(helper, expected.entities.equals(actual.entities), "entities: instant " + expected.entities + ", budgeted " + actual.entities);
				AncientWarfare.LOG.info("improvementBudgetedPlacementMatchesInstantBuild: {} built in {} ticks with 1 ms per tick (longest slice {} ms), {} positions, {} block entities, entities {}", PACK_TEMPLATE, budgeted.getPlacingTicks(), budgeted.getLongestSliceNanos() / 1000000, actual.states.size(), actual.blockEntities, actual.entities);
				helper.succeed();
			}
			finally {
				clear(level, bbInstant);
				clear(level, bbBudgeted);
			}
		});
	}

	/*
	 * the switch off gives the 1.12 path: the queue builds at once (complete in the same tick, the callback has run);
	 * on, the same call only queues the build; the queue completes it on the server tick
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 600)
	public static void improvementBudgetedPlacementSwitchOffBuildsInOneTick(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = StructureTemplateManager.getTemplate("wall_bandit_straight").orElse(null);
		check(helper, template != null, "the pack template wall_bandit_straight is not loaded");
		BlockPos posOff = helper.absolutePos(new BlockPos(0, 150, 2));
		BlockPos posOn = posOff.offset(0, 0, 8);
		StructureBuilder off = new StructureBuilder(level, template, Direction.NORTH, posOff);
		StructureBuilder on = new StructureBuilder(level, template, Direction.NORTH, posOn);
		boolean saved = AWStructureStatics.budgetedPlacement;
		AtomicBoolean offDone = new AtomicBoolean();
		AtomicBoolean onDone = new AtomicBoolean();
		int queuedBefore = StructurePlacementQueue.INSTANCE.size();
		try {
			AWStructureStatics.budgetedPlacement = false;
			StructurePlacementQueue.INSTANCE.build(off, null, () -> offDone.set(true));
			check(helper, offDone.get() && off.isFinished(), "switch off: the build is not complete in the same tick");
			check(helper, StructurePlacementQueue.INSTANCE.size() == queuedBefore, "switch off: the build was queued");
			check(helper, countSolid(level, off.getBoundingBox()) > 0, "switch off: no blocks placed");

			AWStructureStatics.budgetedPlacement = true;
			StructurePlacementQueue.INSTANCE.build(on, null, () -> onDone.set(true));
			check(helper, !onDone.get() && !on.isFinished(), "switch on: the build was complete at once");
			check(helper, countSolid(level, on.getBoundingBox()) == 0, "switch on: blocks placed in the same call");
		}
		finally {
			AWStructureStatics.budgetedPlacement = saved;
		}
		helper.succeedWhen(() -> {
			check(helper, onDone.get(), "switch on: the queued build is not complete yet");
			check(helper, countSolid(level, on.getBoundingBox()) == countSolid(level, off.getBoundingBox()), "switch on and off built different blocks");
			clear(level, off.getBoundingBox());
			clear(level, on.getBoundingBox());
		});
	}

	private static int countSolid(ServerLevel level, StructureBB bb) {
		int n = 0;
		for (BlockPos p : BlockPos.betweenClosed(bb.min, bb.max)) {
			if (!level.getBlockState(p).isAir()) {
				n++;
			}
		}
		return n;
	}

	private static void clear(ServerLevel level, StructureBB bb) {
		level.getEntitiesOfClass(Entity.class, bb.getAABB().inflate(2), e -> !(e instanceof Player)).forEach(Entity::discard);
		for (BlockPos p : BlockPos.betweenClosed(bb.min, bb.max)) {
			level.removeBlockEntity(p);
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
		}
	}

	private static final class Snapshot {
		private final Map<BlockPos, String> states = new TreeMap<>();
		private final Map<BlockPos, String> tileData = new TreeMap<>();
		private final Map<String, Integer> entities = new TreeMap<>();
		private int blockEntities;

		private static Snapshot of(ServerLevel level, StructureBB bb) {
			Snapshot s = new Snapshot();
			for (BlockPos p : BlockPos.betweenClosed(bb.min, bb.max)) {
				BlockPos offset = p.subtract(bb.min).immutable();
				BlockState state = level.getBlockState(p);
				s.states.put(offset, state.toString());
				BlockEntity be = level.getBlockEntity(p);
				if (be != null) {
					s.blockEntities++;
					CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
					//the advanced spawner counts its delay down from the moment it is placed (a running timer, not placed data)
					if (tag.contains("spawnerSettings")) {
						tag.getCompound("spawnerSettings").remove("spawnDelay");
					}
					//a gate proxy holds the uuid of its gate entity, which is random for every placed gate
					tag.remove("msb");
					tag.remove("lsb");
					s.tileData.put(offset, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()) + " " + tag);
				}
			}
			List<Entity> found = new ArrayList<>(level.getEntitiesOfClass(Entity.class, bb.getAABB().inflate(1), e -> !(e instanceof Player)));
			for (Entity e : found) {
				s.entities.merge(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(), 1, Integer::sum);
			}
			return s;
		}
	}
}
