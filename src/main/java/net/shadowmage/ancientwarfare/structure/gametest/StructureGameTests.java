package net.shadowmage.ancientwarfare.structure.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.owner.Owner;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.xmod.IStructureHooks;
import net.shadowmage.ancientwarfare.npc.xmod.NpcXmod;
import net.shadowmage.ancientwarfare.structure.block.BlockBaseStructure;
import net.shadowmage.ancientwarfare.structure.block.BlockCoffin;
import net.shadowmage.ancientwarfare.structure.block.BlockStake;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry;
import net.shadowmage.ancientwarfare.structure.gamedata.StructureMap;
import net.shadowmage.ancientwarfare.structure.gates.types.Gate;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBuilder;
import net.shadowmage.ancientwarfare.structure.template.load.TemplateParser;
import net.shadowmage.ancientwarfare.structure.template.save.TemplateExporter;
import net.shadowmage.ancientwarfare.structure.template.scan.TemplateScanner;
import net.shadowmage.ancientwarfare.structure.tile.LootSettings;
import net.shadowmage.ancientwarfare.structure.tile.SpawnerSettings;
import net.shadowmage.ancientwarfare.structure.tile.TileAdvancedLootChest;
import net.shadowmage.ancientwarfare.structure.tile.TileAdvancedSpawner;
import net.shadowmage.ancientwarfare.structure.tile.TileDraftingStation;
import net.shadowmage.ancientwarfare.structure.tile.TileLootBasket;
import net.shadowmage.ancientwarfare.structure.tile.TileSoundBlock;
import net.shadowmage.ancientwarfare.structure.tile.TileStake;
import net.shadowmage.ancientwarfare.structure.tile.TileStatue;
import net.shadowmage.ancientwarfare.structure.tile.TileWoodenCoffin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/*
 * Headless checks of the structure module (run with ./gradlew runGameTestServer). The template is the empty
 * 14x14x14 space the automation tests use.
 */
@GameTestHolder(AncientWarfare.MOD_ID)
@PrefixGameTestTemplate(false)
public class StructureGameTests {
	private static final String TEMPLATE = "automation_empty";

	private static void check(GameTestHelper helper, boolean condition, String message) {
		if (!condition) {
			helper.fail(message);
		}
	}

	private static LootSettings loot(String table) {
		LootSettings settings = new LootSettings();
		settings.setHasLoot(true);
		settings.setLootTableName(ResourceLocation.parse(table));
		settings.setLootRolls(3);
		return settings;
	}

	/*
	 * every state of every structure block: place it (as /setblock does, nothing configured), tick the block entity,
	 * random tick the block, ask for its shapes and drops, remove it
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void everyBlockStateTicks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos rel = new BlockPos(6, 2, 6);
		BlockPos pos = helper.absolutePos(rel);
		int states = 0;
		for (Block block : BuiltInRegistries.BLOCK) {
			String cls = block.getClass().getName();
			if (!cls.startsWith("net.shadowmage.ancientwarfare.structure.")) {
				continue;
			}
			for (BlockState state : block.getStateDefinition().getPossibleStates()) {
				level.setBlock(pos, state, 3);
				BlockState placed = level.getBlockState(pos);
				BlockEntity be = level.getBlockEntity(pos);
				for (int i = 0; i < 3; i++) {
					if (be instanceof ITickableTile && !be.isRemoved()) {
						((ITickableTile) be).update();
					}
				}
				if (placed.getBlock() == block) {
					placed.randomTick(level, pos, level.random);
					placed.getShape(level, pos, CollisionContext.empty());
					placed.getCollisionShape(level, pos, CollisionContext.empty());
					Block.getDrops(placed, level, pos, level.getBlockEntity(pos));
					if (be != null && !be.isRemoved()) {
						CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
						be.loadWithComponents(tag, level.registryAccess());
					}
				}
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 5, 4))) {
					if (!level.getBlockState(p).isAir() && level.getBlockState(p).getBlock() instanceof BlockBaseStructure) {
						level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
					}
				}
				states++;
			}
		}
		level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8)).forEach(Entity::discard);
		check(helper, states > 300, "only " + states + " structure block states found");
		helper.succeed();
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void lootBasketAndChestGiveLoot(GameTestHelper helper) {
		BlockPos basketPos = new BlockPos(2, 2, 2);
		BlockPos chestPos = new BlockPos(6, 2, 2);
		helper.setBlock(basketPos, AWStructureBlocks.LOOT_BASKET.get());
		helper.setBlock(chestPos, AWStructureBlocks.ADVANCED_LOOT_CHEST.get());
		TileLootBasket basket = helper.getBlockEntity(basketPos);
		TileAdvancedLootChest chest = helper.getBlockEntity(chestPos);
		basket.setLootSettings(loot("minecraft:chests/simple_dungeon"));
		chest.setLootSettings(loot("minecraft:chests/simple_dungeon"));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		check(helper, basket.fillWithLootAndCheckIfGoodToOpen(player), "basket not good to open");
		check(helper, chest.fillWithLootAndCheckIfGoodToOpen(player), "chest not good to open");
		check(helper, !InventoryTools.getItems(new InvWrapper(basket)).isEmpty(), "loot basket stayed empty");
		check(helper, !InventoryTools.getItems(new InvWrapper(chest)).isEmpty(), "advanced loot chest stayed empty");
		check(helper, !basket.getLootSettings().hasLoot(), "basket loot settings still flag loot");
		//settings survive save / load
		chest.setLootSettings(loot("minecraft:chests/village/village_weaponsmith"));
		CompoundTag tag = chest.saveWithFullMetadata(helper.getLevel().registryAccess());
		helper.setBlock(chestPos.above(2), AWStructureBlocks.ADVANCED_LOOT_CHEST.get());
		TileAdvancedLootChest copy = helper.getBlockEntity(chestPos.above(2));
		copy.loadWithComponents(tag, helper.getLevel().registryAccess());
		check(helper, copy.getLootSettings().getLootTableName().map(rl -> rl.getPath().equals("chests/village/village_weaponsmith")).orElse(false), "loot table name lost in save / load");
		helper.succeed();
	}

	private static void spawner(GameTestHelper helper, BlockPos pos, String entityId) {
		helper.setBlock(pos, AWStructureBlocks.ADVANCED_SPAWNER.get());
		TileAdvancedSpawner tile = helper.getBlockEntity(pos);
		SpawnerSettings settings = new SpawnerSettings();
		CompoundTag tag = new CompoundTag();
		SpawnerSettings.getDefaultSettings().writeToNBT(tag);
		settings.readFromNBT(tag);
		settings.getSpawnGroups().clear();
		SpawnerSettings.EntitySpawnGroup group = new SpawnerSettings.EntitySpawnGroup(settings);
		SpawnerSettings.EntitySpawnSettings entity = new SpawnerSettings.EntitySpawnSettings(group);
		entity.setEntityToSpawn(ResourceLocation.parse(entityId));
		entity.setSpawnCountMin(1);
		entity.setSpawnCountMax(1);
		entity.setSpawnLimitTotal(1);
		group.addSpawnSetting(entity);
		settings.addSpawnGroup(group);
		settings.setSpawnDelay(5);
		settings.setMinDelay(5);
		settings.setMaxDelay(5);
		tile.setSettings(settings);
		//1.12 rule: a spawner only runs with a (non creative) player inside its player range. A GameTest has no real
		//player (a mock server player can not receive the mod packets), so the spawn group is run directly.
		try {
			java.lang.reflect.Method spawn = SpawnerSettings.EntitySpawnGroup.class.getDeclaredMethod("spawnEntities", net.minecraft.world.level.Level.class, BlockPos.class, int.class, int.class, int.class);
			spawn.setAccessible(true);
			spawn.invoke(group, helper.getLevel(), helper.absolutePos(pos), 0, 0, 2);
		}
		catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 200)
	public static void advancedSpawnerSpawnsZombieAndNpc(GameTestHelper helper) {
		spawner(helper, new BlockPos(3, 2, 3), "minecraft:zombie");
		spawner(helper, new BlockPos(10, 2, 10), "ancientwarfare:aw_npc_worker");
		helper.succeedWhen(() -> {
			AABB box = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).inflate(16);
			check(helper, !helper.getLevel().getEntitiesOfClass(Zombie.class, box).isEmpty(), "no zombie spawned yet");
			check(helper, !helper.getLevel().getEntitiesOfClass(NpcBase.class, box).isEmpty(), "no npc spawned yet");
		});
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void structureMapThroughNpcHook(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos min = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockPos max = helper.absolutePos(new BlockPos(9, 6, 9));
		BlockPos flagPos = helper.absolutePos(new BlockPos(5, 2, 5));
		StructureEntry entry = new StructureEntry(new StructureBB(min, max), "gametest_structure", 1, min.getX() >> 4, min.getZ() >> 4);
		StructureMap.get(level).setGeneratedAt(level, min.getX(), min.getZ(), entry, false);
		IStructureHooks hooks = NpcXmod.structure();
		check(helper, hooks != IStructureHooks.NONE, "structure hooks are not installed");
		IStructureHooks.IStructureEntry found = hooks.getStructureAt(level, flagPos).orElse(null);
		check(helper, found != null, "structure not found through the npc hook");
		check(helper, !found.hasProtectionFlag(level), "flag reported before it was placed");
		helper.setBlock(new BlockPos(5, 2, 5), AWStructureBlocks.PROTECTION_FLAG.get());
		check(helper, StructureMap.get(level).getStructureAt(level, flagPos).map(e -> flagPos.equals(e.getProtectionFlagPos())).orElse(false), "flag position not recorded in the structure entry");
		check(helper, found.hasProtectionFlag(level), "placed protection flag not seen through the hook");
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		check(helper, found.flagProtectsAgainst(level, player), "an unowned flag should protect against any player");
		check(helper, hooks.isAdvancedSpawner(AWStructureBlocks.ADVANCED_SPAWNER.get()), "isAdvancedSpawner");
		check(helper, hooks.getCoinStackBlock("gold") == AWStructureBlocks.COIN_STACK_GOLD.get(), "coin stack block");
		check(helper, hooks.getCoinStackInteractSound() != null, "coin stack sound");
		helper.setBlock(new BlockPos(7, 2, 7), AWStructureBlocks.URN.get());
		check(helper, hooks.isSpecialLootContainer(helper.getBlockEntity(new BlockPos(7, 2, 7))), "urn is a special loot container");
		helper.succeed();
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void gateOpensAndCloses(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos p1 = helper.absolutePos(new BlockPos(3, 2, 6));
		BlockPos p2 = helper.absolutePos(new BlockPos(7, 4, 6));
		EntityGate gate = Gate.constructGate(level, p1, p2, Gate.getGateByID(0), Direction.SOUTH, Owner.EMPTY).orElse(null);
		check(helper, gate != null, "gate was not constructed");
		level.addFreshEntity(gate);
		boolean[] opened = new boolean[1];
		helper.runAfterDelay(5, () -> {
			check(helper, level.getBlockState(p1).getBlock() == AWStructureBlocks.GATE_PROXY.get(), "closed gate has no proxy block at " + p1 + ": " + level.getBlockState(p1));
			gate.activateGate();
		});
		helper.succeedWhen(() -> {
			if (!opened[0]) {
				check(helper, gate.edgePosition >= gate.edgeMax && gate.getOpeningStatus() == 0, "gate not open yet: " + gate.edgePosition + " / " + gate.edgeMax);
				opened[0] = true;
				gate.activateGate();
			}
			check(helper, gate.isClosed(), "gate not closed again yet: " + gate.edgePosition);
			check(helper, level.getBlockState(p1).getBlock() == AWStructureBlocks.GATE_PROXY.get(), "no proxy block after closing");
		});
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 100)
	public static void soundBlockTicksAndKeepsDisguise(GameTestHelper helper) {
		BlockPos pos = new BlockPos(4, 2, 4);
		helper.setBlock(pos, AWStructureBlocks.SOUND_BLOCK.get());
		TileSoundBlock tile = helper.getBlockEntity(pos);
		tile.setDisguiseState(new ItemStack(Blocks.STONE_BRICKS));
		check(helper, tile.getDisguiseState() != null && tile.getDisguiseState().is(Blocks.STONE_BRICKS), "disguise not set from a full block item");
		CompoundTag tag = tile.saveWithFullMetadata(helper.getLevel().registryAccess());
		helper.setBlock(pos.above(3), AWStructureBlocks.SOUND_BLOCK.get());
		TileSoundBlock copy = helper.getBlockEntity(pos.above(3));
		copy.loadWithComponents(tag, helper.getLevel().registryAccess());
		check(helper, copy.getDisguiseState() != null && copy.getDisguiseState().is(Blocks.STONE_BRICKS), "disguise lost in save / load");
		tile.turnOffByProtectionFlag();
		helper.runAfterDelay(40, helper::succeed);
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 300)
	public static void coffinOpensAndDropsLoot(GameTestHelper helper) {
		BlockPos pos = new BlockPos(6, 2, 6);
		helper.setBlock(pos, AWStructureBlocks.WOODEN_COFFIN.get());
		TileWoodenCoffin coffin = helper.getBlockEntity(pos);
		coffin.setLootSettings(loot("minecraft:chests/simple_dungeon"));
		helper.makeMockPlayer(GameType.SURVIVAL);
		coffin.open();
		helper.succeedWhen(() -> {
			check(helper, coffin.getLidAngle() > 14.9F, "coffin lid not open yet: " + coffin.getLidAngle());
			AABB box = new AABB(helper.absolutePos(pos)).inflate(4);
			check(helper, !helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).isEmpty(), "open coffin dropped no loot");
		});
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void statueAndStakeKeepTheirData(GameTestHelper helper) {
		BlockPos statuePos = new BlockPos(3, 2, 3);
		BlockPos stakePos = new BlockPos(8, 2, 8);
		helper.setBlock(statuePos, AWStructureBlocks.STATUE.get());
		helper.setBlock(stakePos, AWStructureBlocks.STAKE.get().defaultBlockState().setValue(net.shadowmage.ancientwarfare.structure.util.StructureProperties.TOP_BOTTOM_PART, net.shadowmage.ancientwarfare.structure.util.TopBottomPart.BOTTOM));
		TileStatue statue = helper.getBlockEntity(statuePos);
		TileStake stake = helper.getBlockEntity(stakePos);
		CompoundTag statueTag = statue.saveWithFullMetadata(helper.getLevel().registryAccess());
		statue.loadWithComponents(statueTag, helper.getLevel().registryAccess());
		check(helper, statue.getPrimaryFacing() != null, "statue has no facing");
		CompoundTag stakeTag = stake.saveWithFullMetadata(helper.getLevel().registryAccess());
		stake.loadWithComponents(stakeTag, helper.getLevel().registryAccess());
		check(helper, !helper.getBlockState(stakePos).getValue(BlockStake.ON_FIRE), "a fresh stake burns");
		check(helper, BlockCoffin.CoffinDirection.fromYaw(0) != null, "coffin direction");
		helper.succeed();
	}

	/*
	 * scan a small build into a template, export it to the original text format, parse that file again, build it
	 * somewhere else (rotated by a quarter turn and back) and compare the blocks
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void scanExportParseBuild(GameTestHelper helper) throws Exception {
		ServerLevel level = helper.getLevel();
		BlockPos min = helper.absolutePos(new BlockPos(1, 2, 1));
		BlockPos max = min.offset(3, 2, 3);
		for (BlockPos p : BlockPos.betweenClosed(min, max)) {
			level.setBlock(p, p.getY() == min.getY() ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
		}
		level.setBlock(min.offset(0, 1, 0), Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.EAST), 3);
		level.setBlock(min.offset(3, 1, 3), Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X), 3);
		level.setBlock(min.offset(1, 1, 2), AWStructureBlocks.STOOL.get().defaultBlockState(), 3);
		level.setBlock(min.offset(2, 2, 1), Blocks.GLOWSTONE.defaultBlockState(), 3);

		//the build key of a scan sits one block above the template origin (TemplateScanner): key = min.below() -> offset 0
		StructureTemplate scanned = TemplateScanner.scan(level, Collections.emptySet(), min, max, min.below(), 0, "gametest_scan");
		File dir = Files.createTempDirectory("aw_template_test").toFile();
		check(helper, TemplateExporter.exportTo(scanned, dir), "template export failed");
		File[] files = dir.listFiles();
		check(helper, files != null && files.length == 1, "expected one exported template file");
		List<String> lines = Files.readAllLines(files[0].toPath(), StandardCharsets.ISO_8859_1).stream().filter(l -> !l.startsWith("#")).collect(Collectors.toList());
		StructureTemplate parsed = TemplateParser.INSTANCE.parseTemplate(files[0].getPath(), lines).map(r -> r.getData()).orElse(null);
		check(helper, parsed != null, "exported template could not be parsed again");

		BlockPos target = helper.absolutePos(new BlockPos(8, 2, 8));
		//turns = (face + 2) % 4: building towards north is the unrotated template
		StructureBuilder builder = new StructureBuilder(level, parsed, Direction.NORTH, target);
		String info = " [size " + parsed.getSize() + " offset " + parsed.getOffset() + " bb " + builder.getBoundingBox() + " rule0 parsed " + parsed.getRuleAt(net.minecraft.core.Vec3i.ZERO).isPresent() + " scanned " + scanned.getRuleAt(net.minecraft.core.Vec3i.ZERO).isPresent() + " target " + target + "]";
		builder.instantConstruction();
		for (BlockPos p : BlockPos.betweenClosed(min, max)) {
			BlockPos q = target.offset(p.getX() - min.getX(), p.getY() - min.getY(), p.getZ() - min.getZ());
			BlockState expected = level.getBlockState(p);
			BlockState actual = level.getBlockState(q);
			check(helper, expected.equals(actual), "block mismatch at offset " + p.subtract(min) + ": scanned " + expected + ", built " + actual + info);
		}
		helper.succeed();
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void enchantedItemSurvivesTileSaveLoad(GameTestHelper helper) {
		BlockPos pos = new BlockPos(4, 2, 4);
		helper.setBlock(pos, AWStructureBlocks.DRAFTING_STATION.get());
		TileDraftingStation station = helper.getBlockEntity(pos);
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 4);
		station.inputSlots.setStackInSlot(3, sword.copy());
		CompoundTag tag = station.saveWithFullMetadata(helper.getLevel().registryAccess());
		helper.setBlock(pos.above(2), AWStructureBlocks.DRAFTING_STATION.get());
		TileDraftingStation copy = helper.getBlockEntity(pos.above(2));
		copy.loadWithComponents(tag, helper.getLevel().registryAccess());
		check(helper, ItemStack.matches(sword, copy.inputSlots.getStackInSlot(3)), "enchanted sword changed in save / load: " + copy.inputSlots.getStackInSlot(3));
		helper.succeed();
	}

	/*
	 * an original 1.12 template file (shipped in the default pack under assets/ancientwarfare/template) is loaded at
	 * startup, its 1.12 block states are upgraded and it builds real blocks
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void originalTemplateBuilds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager.getTemplate("wall_bandit_straight").orElse(null);
		check(helper, template != null, "the original sample template wall_bandit_straight was not loaded");
		BlockPos target = helper.absolutePos(new BlockPos(1, 3, 6));
		StructureBuilder builder = new StructureBuilder(level, template, Direction.NORTH, target);
		builder.instantConstruction();
		StructureBB bb = builder.getBoundingBox();
		int grass = 0;
		int stairs = 0;
		int other = 0;
		for (BlockPos p : BlockPos.betweenClosed(bb.min, bb.max)) {
			BlockState state = level.getBlockState(p);
			if (state.is(Blocks.GRASS_BLOCK)) {
				grass++;
			} else if (state.getBlock() instanceof net.minecraft.world.level.block.StairBlock) {
				stairs++;
			} else if (!state.isAir()) {
				other++;
			}
		}
		for (BlockPos p : BlockPos.betweenClosed(bb.min, bb.max)) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
		}
		level.getEntitiesOfClass(Entity.class, bb.getAABB().inflate(2), e -> !(e instanceof Player)).forEach(Entity::discard);
		check(helper, grass > 0, "no grass block built from the 1.12 state minecraft:grass[snowy=false] (stairs " + stairs + ", other " + other + ")");
		check(helper, stairs > 0, "no stairs built (grass " + grass + ", other " + other + ")");
		check(helper, other > 0, "nothing else built (grass " + grass + ", stairs " + stairs + ")");
		helper.succeed();
	}


	/*
	 * Big modpack audit: 1.12 loot chest and loot basket were TileEntityChest subclasses with the vanilla item handler
	 * capability on every face. An empty chest without loot settings is a plain 27 slot inventory for a pipe.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void lootChestAndBasketExposeTheirInventory(GameTestHelper helper) {
		BlockPos[] positions = {new BlockPos(2, 2, 2), new BlockPos(6, 2, 2)};
		helper.setBlock(positions[0], AWStructureBlocks.ADVANCED_LOOT_CHEST.get());
		helper.setBlock(positions[1], AWStructureBlocks.LOOT_BASKET.get());
		for (BlockPos pos : positions) {
			for (Direction side : new Direction[] {Direction.UP, Direction.DOWN, Direction.NORTH, null}) {
				net.neoforged.neoforge.items.IItemHandler handler = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), side);
				check(helper, handler != null && handler.getSlots() == 27, "27 slot item handler at " + pos + " face " + side);
				check(helper, handler.insertItem(5, new ItemStack(Items.GOLD_INGOT, 4), true).isEmpty() && handler.getStackInSlot(5).isEmpty(), "a simulated insert changes nothing");
				check(helper, handler.insertItem(5, new ItemStack(Items.GOLD_INGOT, 4), false).isEmpty() && handler.extractItem(5, 1, true).getCount() == 1 && handler.getStackInSlot(5).getCount() == 4, "insert, then a simulated extract leaves 4");
				check(helper, handler.extractItem(5, 64, false).getCount() == 4 && handler.getStackInSlot(5).isEmpty(), "an extract of more than is there gives the 4");
			}
		}
		helper.succeed();
	}
}
