package net.shadowmage.ancientwarfare.automation.gametest;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.automation.block.BlockTorqueBase;
import net.shadowmage.ancientwarfare.automation.block.BlockWarehouseStorage;
import net.shadowmage.ancientwarfare.automation.block.BlockWorksiteBase;
import net.shadowmage.ancientwarfare.automation.block.TorqueTier;
import net.shadowmage.ancientwarfare.automation.config.AWAutomationStatics;
import net.shadowmage.ancientwarfare.automation.gamedata.MailboxData;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.TileMailbox;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileFlywheelController;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileHandCrankedGenerator;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileStirlingGenerator;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueShaft;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueSidedCell;
import net.shadowmage.ancientwarfare.automation.tile.torque.multiblock.TileFlywheelStorage;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouse;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseCraftingStation;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileWarehouseInterface;
import net.shadowmage.ancientwarfare.automation.tile.warehouse2.WarehouseInterfaceFilter;
import net.shadowmage.ancientwarfare.automation.tile.worksite.TileAutoCrafting;
import net.shadowmage.ancientwarfare.automation.tile.worksite.TileWorksiteBase;
import net.shadowmage.ancientwarfare.automation.tile.worksite.WorkSiteAnimalFarm;
import net.shadowmage.ancientwarfare.automation.tile.worksite.WorkSiteFishFarm;
import net.shadowmage.ancientwarfare.automation.tile.worksite.WorkSiteQuarry;
import net.shadowmage.ancientwarfare.automation.tile.worksite.cropfarm.WorkSiteCropFarm;
import net.shadowmage.ancientwarfare.automation.tile.worksite.treefarm.WorkSiteTreeFarm;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.crafting.AWCraftingManager;
import net.shadowmage.ancientwarfare.core.crafting.ICraftingRecipe;
import net.shadowmage.ancientwarfare.core.gamedata.AWGameData;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Headless checks of the automation module (run with ./gradlew runGameTestServer). The template is an empty
 * 14x14x14 space. Worksites are worked through IWorkSite.addEnergyFromPlayer (the hammer / hand work path; it adds
 * torque exactly like a worker does) unless a test says otherwise.
 */
@GameTestHolder(AncientWarfare.MOD_ID)
@PrefixGameTestTemplate(false)
public class AutomationGameTests {
	private static final String TEMPLATE = "automation_empty";

	private static void check(GameTestHelper helper, boolean condition, String message) {
		if (!condition) {
			helper.fail(message);
		}
	}

	private static int count(IItemHandler handler, Item item) {
		return InventoryTools.getCountOf(handler, s -> s.getItem() == item);
	}

	private static void work(GameTestHelper helper, TileWorksiteBase tile) {
		helper.onEachTick(() -> tile.addEnergyFromPlayer(null));
	}

	private static void fill(GameTestHelper helper, int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
		for (int x = x1; x <= x2; x++) {
			for (int y = y1; y <= y2; y++) {
				for (int z = z1; z <= z2; z++) {
					helper.setBlock(x, y, z, block);
				}
			}
		}
	}

	//************************************** TORQUE ***************************************//

	/*
	 * 1.12 TileTorqueBase.applyPowerDrain: every tick the stored energy is multiplied by the efficiency factor
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void torqueLossMaths(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, AWAutomationBlocks.TORQUE_SHAFT.get().defaultBlockState().setValue(AutomationProperties.TIER, TorqueTier.LIGHT).setValue(BlockTorqueBase.FACING, Direction.EAST));
		TileTorqueShaft shaft = helper.getBlockEntity(pos);
		check(helper, shaft.getMaxTorque(Direction.EAST) == AWAutomationStatics.low_transfer_max, "light shaft stores " + shaft.getMaxTorque(Direction.EAST));
		check(helper, shaft.getPrimaryFacing() == Direction.EAST, "the shaft faces east like its block state");
		check(helper, shaft.canOutputTorque(Direction.EAST) && shaft.canInputTorque(Direction.WEST) && !shaft.canInputTorque(Direction.EAST), "shaft: in at the back, out at the front");
		double added = shaft.addTorque(Direction.WEST, 20);
		check(helper, added == 20, "20 torque accepted, got " + added);
		double start = shaft.getTorqueStored(Direction.EAST);
		helper.runAfterDelay(10, () -> {
			double expected = start;
			long ticks = 0;
			//the tile ticked 10 times (or 9/11 depending on the tick order of the test and the tile)
			double stored = shaft.getTorqueStored(Direction.EAST);
			boolean match = false;
			for (int i = 0; i <= 12; i++) {
				if (Math.abs(expected - stored) < 1e-12) {
					match = true;
					ticks = i;
					break;
				}
				expected *= AWAutomationStatics.low_efficiency_factor;
			}
			check(helper, match && ticks >= 9 && ticks <= 11, "energy after 10 ticks is 20 * 0.9999^10, got " + stored + " (matched " + ticks + " ticks)");
			helper.succeed();
		});
	}

	/*
	 * stirling generator -> shaft line -> junction -> flywheel controller on top of a flywheel storage block
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void torqueGeneratorConduitFlywheel(GameTestHelper helper) {
		BlockState shaftState = AWAutomationBlocks.TORQUE_SHAFT.get().defaultBlockState().setValue(BlockTorqueBase.FACING, Direction.EAST);
		helper.setBlock(new BlockPos(1, 3, 2), AWAutomationBlocks.STIRLING_GENERATOR.get().defaultBlockState().setValue(BlockTorqueBase.FACING, Direction.EAST));
		helper.setBlock(new BlockPos(2, 3, 2), shaftState);
		helper.setBlock(new BlockPos(3, 3, 2), shaftState);
		helper.setBlock(new BlockPos(4, 3, 2), AWAutomationBlocks.TORQUE_JUNCTION.get().defaultBlockState().setValue(BlockTorqueBase.FACING, Direction.EAST));
		helper.setBlock(new BlockPos(5, 2, 2), AWAutomationBlocks.FLYWHEEL_STORAGE.get());
		helper.setBlock(new BlockPos(5, 3, 2), AWAutomationBlocks.FLYWHEEL_CONTROLLER.get().defaultBlockState().setValue(BlockTorqueBase.FACING, Direction.EAST));
		TileStirlingGenerator generator = helper.getBlockEntity(new BlockPos(1, 3, 2));
		TileTorqueShaft shaft = helper.getBlockEntity(new BlockPos(3, 3, 2));
		TileTorqueSidedCell junction = helper.getBlockEntity(new BlockPos(4, 3, 2));
		TileFlywheelController controller = helper.getBlockEntity(new BlockPos(5, 3, 2));
		TileFlywheelStorage storage = helper.getBlockEntity(new BlockPos(5, 2, 2));
		storage.blockPlaced();
		check(helper, storage.isControl && storage.maxEnergyStored == AWAutomationStatics.low_storage_energy_max, "single light flywheel storage block is its own controller, max " + storage.maxEnergyStored);

		IItemHandler fuel = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(new BlockPos(1, 3, 2)), Direction.UP);
		check(helper, fuel != null, "the stirling generator exposes its fuel slot");
		check(helper, !fuel.insertItem(0, new ItemStack(Items.STONE), true).isEmpty(), "stone is no fuel");
		check(helper, fuel.insertItem(0, new ItemStack(Items.COAL, 2), false).isEmpty(), "coal is fuel");

		helper.runAfterDelay(200, () -> {
			check(helper, generator.getBurnTime() > 0 && generator.getBurnTimeBase() == 1600, "one coal burns for 1600 ticks, base " + generator.getBurnTimeBase());
			check(helper, count(fuel, Items.COAL) == 1, "one coal was consumed");
			check(helper, junction.getConnections()[Direction.WEST.ordinal()] && junction.getConnections()[Direction.EAST.ordinal()], "the junction is connected to the shaft and the flywheel controller: " + java.util.Arrays.toString(junction.getConnections()) + " facing " + junction.getPrimaryFacing() + " cache " + java.util.Arrays.toString(junction.getTorqueCache()));
			double total = generator.getTorqueStored(Direction.EAST) + ((TileTorqueShaft) helper.getBlockEntity(new BlockPos(2, 3, 2))).getTorqueStored(Direction.EAST) + shaft.getTorqueStored(Direction.EAST);
			for (Direction d : Direction.values()) {
				total += junction.getTorqueStored(d);
			}
			total += controller.getTorqueStored(Direction.EAST) + controller.getTorqueStored(Direction.WEST) + storage.storedEnergy;
			//the generator adds stirling_generator_output (1.0) per burn tick; everything else only loses energy
			check(helper, storage.storedEnergy > 50, "energy arrived in the flywheel storage: " + storage.storedEnergy);
			check(helper, total > 150 && total <= 200.0001, "about 200 torque generated in 200 ticks, minus transmission loss; in the network: " + total);
			helper.succeed();
		});
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void handCrankedGeneratorWorkedByHand(GameTestHelper helper) {
		helper.setBlock(new BlockPos(2, 2, 2), AWAutomationBlocks.HAND_CRANKED_GENERATOR.get().defaultBlockState().setValue(BlockTorqueBase.FACING, Direction.EAST));
		helper.setBlock(new BlockPos(3, 2, 2), AWAutomationBlocks.TORQUE_SHAFT.get().defaultBlockState().setValue(BlockTorqueBase.FACING, Direction.EAST));
		TileHandCrankedGenerator crank = helper.getBlockEntity(new BlockPos(2, 2, 2));
		TileTorqueShaft shaft = helper.getBlockEntity(new BlockPos(3, 2, 2));
		check(helper, crank.hasWork(), "an empty hand crank has work");
		crank.addEnergyFromPlayer(null);
		check(helper, Math.abs(crank.getTorqueStored(null) - AWCoreStatics.energyPerWorkUnit) < 1e-9, "one work unit adds energyPerWorkUnit, got " + crank.getTorqueStored(null));
		helper.runAfterDelay(20, () -> {
			check(helper, shaft.getTorqueStored(Direction.EAST) > 1, "the crank feeds the shaft in front of it: " + shaft.getTorqueStored(Direction.EAST));
			helper.succeed();
		});
	}

	//************************************** WORKSITES ***************************************//

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 600)
	public static void cropFarmHarvestsAndReplantsWheat(GameTestHelper helper) {
		fill(helper, 3, 1, 3, 5, 1, 5, Blocks.FARMLAND);
		helper.setBlock(4, 5, 4, Blocks.GLOWSTONE);
		for (int x = 3; x <= 5; x++) {
			for (int z = 3; z <= 5; z++) {
				helper.setBlock(x, 2, z, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
			}
		}
		BlockPos pos = new BlockPos(1, 2, 1);
		helper.setBlock(pos, AWAutomationBlocks.CROP_FARM.get());
		WorkSiteCropFarm farm = helper.getBlockEntity(pos);
		farm.setBounds(helper.absolutePos(new BlockPos(3, 2, 3)), helper.absolutePos(new BlockPos(5, 2, 5)));
		work(helper, farm);
		helper.succeedWhen(() -> {
			int replanted = 0;
			for (int x = 3; x <= 5; x++) {
				for (int z = 3; z <= 5; z++) {
					BlockState state = helper.getBlockState(new BlockPos(x, 2, z));
					if (state.is(Blocks.WHEAT) && state.getValue(CropBlock.AGE) < 7) {
						replanted++;
					}
				}
			}
			check(helper, replanted == 9, "all 9 wheat plants were harvested and replanted, replanted: " + replanted);
			check(helper, count(farm.mainInventory, Items.WHEAT) >= 9, "the wheat is in the farm inventory: " + count(farm.mainInventory, Items.WHEAT));
		});
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void treeFarmChopsAndReplants(GameTestHelper helper) {
		fill(helper, 3, 1, 3, 5, 1, 5, Blocks.DIRT);
		fill(helper, 4, 2, 4, 4, 5, 4, Blocks.OAK_LOG);
		BlockPos pos = new BlockPos(1, 2, 1);
		helper.setBlock(pos, AWAutomationBlocks.TREE_FARM.get());
		WorkSiteTreeFarm farm = helper.getBlockEntity(pos);
		farm.setBounds(helper.absolutePos(new BlockPos(3, 2, 3)), helper.absolutePos(new BlockPos(5, 2, 5)));
		farm.plantableInventory.setStackInSlot(0, new ItemStack(Items.OAK_SAPLING, 1));
		work(helper, farm);
		helper.succeedWhen(() -> {
			for (int y = 2; y <= 5; y++) {
				check(helper, !helper.getBlockState(new BlockPos(4, y, 4)).is(Blocks.OAK_LOG), "the trunk is chopped at y=" + y);
			}
			check(helper, count(farm.mainInventory, Items.OAK_LOG) == 4, "4 logs are in the farm inventory: " + count(farm.mainInventory, Items.OAK_LOG));
			int saplings = 0;
			for (int x = 3; x <= 5; x++) {
				for (int z = 3; z <= 5; z++) {
					if (helper.getBlockState(new BlockPos(x, 2, z)).is(Blocks.OAK_SAPLING)) {
						saplings++;
					}
				}
			}
			check(helper, saplings == 1, "the sapling was planted inside the bounds: " + saplings);
		});
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void quarryDigsLayerByLayer(GameTestHelper helper) {
		fill(helper, 3, 2, 3, 5, 4, 5, Blocks.STONE);
		BlockPos pos = new BlockPos(1, 5, 1);
		helper.setBlock(pos, AWAutomationBlocks.QUARRY.get());
		WorkSiteQuarry quarry = helper.getBlockEntity(pos);
		quarry.setBounds(helper.absolutePos(new BlockPos(3, 5, 3)), helper.absolutePos(new BlockPos(5, 5, 5)));
		check(helper, quarry.getWorkBoundsMax().getY() == helper.absolutePos(pos).getY() - 1, "the quarry bounds start one below the quarry: " + quarry.getWorkBoundsMax());
		work(helper, quarry);
		boolean[] done = new boolean[1];
		helper.onEachTick(() -> {
			if (done[0]) {
				return;
			}
			//the quarry works chunk by chunk and, inside a chunk, layer by layer from the top
			java.util.Map<Long, int[]> perChunk = new java.util.HashMap<>();
			int left = 0;
			for (int x = 3; x <= 5; x++) {
				for (int z = 3; z <= 5; z++) {
					BlockPos abs = helper.absolutePos(new BlockPos(x, 2, z));
					int[] counts = perChunk.computeIfAbsent(new ChunkPos(abs).toLong(), k -> new int[4]);
					counts[3]++;
					for (int y = 2; y <= 4; y++) {
						if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.STONE)) {
							counts[y - 2]++;
							left++;
						}
					}
				}
			}
			for (int[] counts : perChunk.values()) {
				if (counts[2] > 0) {
					check(helper, counts[1] == counts[3] && counts[0] == counts[3], "the top layer is finished before the next one is started (" + counts[2] + "/" + counts[1] + "/" + counts[0] + ")");
				} else if (counts[1] > 0) {
					check(helper, counts[0] == counts[3], "the middle layer is finished before the bottom one is started (" + counts[1] + "/" + counts[0] + ")");
				}
			}
			if (left == 0) {
				done[0] = true;
				check(helper, quarry.getWorkBoundsMin().getY() == helper.getLevel().getMinBuildHeight() + 1, "the quarry bounds reach down to the bottom of the world: " + quarry.getWorkBoundsMin());
				check(helper, count(quarry.mainInventory, Items.COBBLESTONE) >= 27, "the cobblestone is in the quarry inventory: " + count(quarry.mainInventory, Items.COBBLESTONE));
				helper.succeed();
			}
		});
	}

	private static void pen(GameTestHelper helper) {
		fill(helper, 2, 1, 2, 8, 1, 8, Blocks.GRASS_BLOCK);
		for (int i = 2; i <= 8; i++) {
			helper.setBlock(i, 2, 2, Blocks.OAK_FENCE);
			helper.setBlock(i, 2, 8, Blocks.OAK_FENCE);
			helper.setBlock(2, 2, i, Blocks.OAK_FENCE);
			helper.setBlock(8, 2, i, Blocks.OAK_FENCE);
		}
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 1200)
	public static void animalFarmCullsSurplusCows(GameTestHelper helper) {
		pen(helper);
		BlockPos pos = new BlockPos(1, 2, 1);
		helper.setBlock(pos, AWAutomationBlocks.ANIMAL_FARM.get());
		WorkSiteAnimalFarm farm = helper.getBlockEntity(pos);
		farm.setBounds(helper.absolutePos(new BlockPos(3, 2, 3)), helper.absolutePos(new BlockPos(7, 2, 7)));
		for (int i = 0; i < 8; i++) {
			helper.spawnWithNoFreeWill(EntityType.COW, 3.5F + (i % 4), 2, 3.5F + (i / 4) * 2);
		}
		work(helper, farm);
		AABB area = new AABB(helper.absolutePos(new BlockPos(2, 1, 2))).minmax(new AABB(helper.absolutePos(new BlockPos(8, 4, 8))));
		helper.succeedWhen(() -> {
			int cows = helper.getLevel().getEntitiesOfClass(Cow.class, area, Cow::isAlive).size();
			check(helper, cows == farm.maxCowCount, "the farm culls down to maxCowCount (6), alive: " + cows);
			check(helper, count(farm.mainInventory, Items.BEEF) >= 2, "the drops of the culled cows are in the farm inventory, beef: " + count(farm.mainInventory, Items.BEEF));
		});
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 1200)
	public static void animalFarmBreedsCows(GameTestHelper helper) {
		pen(helper);
		BlockPos pos = new BlockPos(1, 2, 1);
		helper.setBlock(pos, AWAutomationBlocks.ANIMAL_FARM.get());
		WorkSiteAnimalFarm farm = helper.getBlockEntity(pos);
		farm.setBounds(helper.absolutePos(new BlockPos(3, 2, 3)), helper.absolutePos(new BlockPos(7, 2, 7)));
		helper.spawn(EntityType.COW, 4, 2, 4);
		helper.spawn(EntityType.COW, 6, 2, 6);
		farm.foodInventory.setStackInSlot(0, new ItemStack(Items.WHEAT, 8));
		work(helper, farm);
		AABB area = new AABB(helper.absolutePos(new BlockPos(2, 1, 2))).minmax(new AABB(helper.absolutePos(new BlockPos(8, 4, 8))));
		helper.succeedWhen(() -> {
			List<Cow> cows = helper.getLevel().getEntitiesOfClass(Cow.class, area, Cow::isAlive);
			check(helper, cows.stream().anyMatch(Cow::isBaby), "a calf was born, cows: " + cows.size());
			check(helper, count(farm.foodInventory, Items.WHEAT) == 6, "breeding one pair used 2 wheat, left: " + count(farm.foodInventory, Items.WHEAT));
		});
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 2400)
	public static void fishFarmCatchesFish(GameTestHelper helper) {
		//a 5x5 basin, 5 deep, below the farm level (glass walls keep the water in)
		fill(helper, 2, 1, 2, 8, 6, 8, Blocks.GLASS);
		fill(helper, 3, 2, 3, 7, 6, 7, Blocks.WATER);
		BlockPos pos = new BlockPos(1, 7, 1);
		helper.setBlock(pos, AWAutomationBlocks.FISH_FARM.get());
		WorkSiteFishFarm farm = helper.getBlockEntity(pos);
		farm.setBounds(helper.absolutePos(new BlockPos(3, 7, 3)), helper.absolutePos(new BlockPos(7, 7, 7)));
		check(helper, farm.getWorkBoundsMax().getY() == helper.absolutePos(pos).getY() - 1 && farm.getWorkBoundsMin().getY() == helper.absolutePos(pos).getY() - 5,
				"the fish farm works the 5 layers below it: " + farm.getWorkBoundsMin() + " " + farm.getWorkBoundsMax());
		work(helper, farm);
		helper.succeedWhen(() -> check(helper, InventoryTools.stream(farm.mainInventory).anyMatch(s -> !s.isEmpty()), "something was fished"));
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 200)
	public static void autoCraftingCraftsRecipe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, AWAutomationBlocks.AUTO_CRAFTING.get());
		TileAutoCrafting tile = helper.getBlockEntity(pos);
		CraftingContainer matrix = tile.craftingRecipeMemory.craftMatrix;
		matrix.setItem(0, new ItemStack(Items.OAK_LOG));
		List<ICraftingRecipe> recipes = AWCraftingManager.findMatchingRecipes(matrix, level, null);
		check(helper, !recipes.isEmpty(), "the planks recipe is found");
		tile.craftingRecipeMemory.setRecipe(recipes.get(0));
		tile.resourceInventory.setStackInSlot(0, new ItemStack(Items.OAK_LOG, 3));
		IItemHandler down = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.DOWN);
		IItemHandler side = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.NORTH);
		check(helper, down == tile.outputInventory && side == tile.resourceInventory, "bottom side = output inventory, other sides = resource inventory");
		work(helper, tile);
		helper.succeedWhen(() -> {
			check(helper, count(tile.outputInventory, Items.OAK_PLANKS) == 12, "3 logs were crafted into 12 planks: " + count(tile.outputInventory, Items.OAK_PLANKS));
			check(helper, count(tile.resourceInventory, Items.OAK_LOG) == 0, "the logs were used");
		});
	}

	/*
	 * tester request: item stacks with components (an enchanted sword) survive saving and loading of a worksite
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void worksiteInventoryKeepsEnchantments(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, AWAutomationBlocks.CROP_FARM.get());
		WorkSiteCropFarm farm = helper.getBlockEntity(pos);
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> sharpness = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
		sword.enchant(sharpness, 4);
		sword.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("GT blade"));
		farm.mainInventory.setStackInSlot(3, sword.copy());
		net.minecraft.nbt.CompoundTag saved = farm.saveWithoutMetadata(level.registryAccess());
		farm.mainInventory.setStackInSlot(3, ItemStack.EMPTY);
		farm.loadWithComponents(saved, level.registryAccess());
		ItemStack loaded = farm.mainInventory.getStackInSlot(3);
		check(helper, loaded.is(Items.DIAMOND_SWORD), "the sword is back after loading: " + loaded);
		check(helper, loaded.getEnchantments().getLevel(sharpness) == 4, "sharpness 4 survived: " + loaded.getEnchantments());
		check(helper, ItemStack.isSameItemSameComponents(sword, loaded), "all components survived");
		helper.succeed();
	}

	//************************************** WAREHOUSE ***************************************//

	private static TileWarehouse warehouse(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 2, 1);
		helper.setBlock(pos, AWAutomationBlocks.WAREHOUSE_CONTROL.get());
		helper.setBlock(new BlockPos(3, 2, 3), AWAutomationBlocks.WAREHOUSE_STORAGE.get().defaultBlockState().setValue(BlockWarehouseStorage.SIZE, BlockWarehouseStorage.Size.SMALL));
		helper.setBlock(new BlockPos(4, 2, 3), AWAutomationBlocks.WAREHOUSE_INTERFACE.get());
		helper.setBlock(new BlockPos(1, 3, 1), AWAutomationBlocks.WAREHOUSE_CRAFTING.get());//the crafting station sits on top of the control block
		TileWarehouse warehouse = helper.getBlockEntity(pos);
		warehouse.setBounds(helper.absolutePos(new BlockPos(3, 2, 3)), helper.absolutePos(new BlockPos(5, 2, 5)));
		return warehouse;
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 600)
	public static void warehouseStoresIndexesAndReturnsItems(GameTestHelper helper) {
		TileWarehouse warehouse = warehouse(helper);
		TileWarehouseInterface iface = helper.getBlockEntity(new BlockPos(4, 2, 3));
		IItemHandler inventory = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(new BlockPos(4, 2, 3)), Direction.UP);
		check(helper, inventory != null && inventory == iface.getItemHandler(null), "the interface exposes its inventory");
		inventory.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
		inventory.insertItem(1, new ItemStack(Items.IRON_INGOT, 10), false);
		work(helper, warehouse);
		boolean[] stored = new boolean[1];
		helper.succeedWhen(() -> {
			if (!stored[0]) {
				check(helper, warehouse.getMaxStorage() > 0, "the storage block registered with the controller, capacity " + warehouse.getMaxStorage());
				check(helper, warehouse.getCountOf(new ItemStack(Items.COBBLESTONE)) == 64 && warehouse.getCountOf(new ItemStack(Items.IRON_INGOT)) == 10,
						"the items moved from the interface into the warehouse: " + warehouse.getCountOf(new ItemStack(Items.COBBLESTONE)) + " / " + warehouse.getCountOf(new ItemStack(Items.IRON_INGOT)));
				check(helper, count(inventory, Items.COBBLESTONE) == 0, "the interface was emptied");
				//now ask for 16 cobblestone through an interface filter
				stored[0] = true;
				WarehouseInterfaceFilter filter = new WarehouseInterfaceFilter();
				filter.setFilterItem(new ItemStack(Items.COBBLESTONE));
				filter.setFilterQuantity(16);
				List<WarehouseInterfaceFilter> filters = new ArrayList<>(Collections.singletonList(filter));
				iface.setFilters(filters);
			}
			check(helper, count(inventory, Items.COBBLESTONE) == 16, "the interface was filled with the 16 requested cobblestone: " + count(inventory, Items.COBBLESTONE));
			check(helper, warehouse.getCountOf(new ItemStack(Items.COBBLESTONE)) == 48, "48 cobblestone stay in the warehouse: " + warehouse.getCountOf(new ItemStack(Items.COBBLESTONE)));
			check(helper, warehouse.getCountOf(new ItemStack(Items.IRON_INGOT)) == 10, "the iron stays in the warehouse");
		});
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 600)
	public static void warehouseCraftingStationCraftsFromStock(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TileWarehouse warehouse = warehouse(helper);
		BlockPos stationPos = new BlockPos(1, 3, 1);
		TileWarehouseCraftingStation station = helper.getBlockEntity(stationPos);
		IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(new BlockPos(4, 2, 3)), Direction.UP);
		inventory.insertItem(0, new ItemStack(Items.OAK_LOG, 5), false);
		work(helper, warehouse);
		boolean[] crafted = new boolean[1];
		helper.succeedWhen(() -> {
			check(helper, warehouse.getCountOf(new ItemStack(Items.OAK_LOG)) == (crafted[0] ? 4 : 5), "the logs are in the warehouse: " + warehouse.getCountOf(new ItemStack(Items.OAK_LOG)));
			if (!crafted[0]) {
				check(helper, station.getWarehouse() == warehouse, "the crafting station found its warehouse");
				//what ContainerWarehouseCraftingStation.handleOnTake does when the result is taken (the container itself can only
				//be created through NetworkHandler.openGui for a real player, so its logic is replayed here)
				CraftingContainer matrix = station.craftingRecipeMemory.craftMatrix;
				matrix.setItem(0, new ItemStack(Items.OAK_LOG));
				List<ICraftingRecipe> recipes = AWCraftingManager.findMatchingRecipes(matrix, level, null);
				check(helper, !recipes.isEmpty(), "the planks recipe is found");
				ICraftingRecipe recipe = recipes.get(0);
				station.craftingRecipeMemory.setRecipe(recipe);
				check(helper, station.craftingRecipeMemory.outputSlot.getItem(0).is(Items.OAK_PLANKS), "the output slot shows planks");
				IItemHandler stock = warehouse.getItemHandler();
				net.minecraft.core.NonNullList<ItemStack> resources = AWCraftingManager.getRecipeInventoryMatch(recipe, station.craftingRecipeMemory.getCraftingStacks(), s -> warehouse.getCountOf(s) >= s.getCount(), stock);
				check(helper, !resources.isEmpty() && resources.get(0).is(Items.OAK_LOG), "the ingredients are found in the warehouse stock: " + resources);
				InventoryTools.removeItems(stock, resources);
				check(helper, warehouse.getCountOf(new ItemStack(Items.OAK_LOG)) == 4, "one log was taken from the warehouse stock: " + warehouse.getCountOf(new ItemStack(Items.OAK_LOG)));
				crafted[0] = true;
			}
		});
	}

	//************************************** MAILBOX ***************************************//

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 200)
	public static void mailboxDeliversWithDelay(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos posA = new BlockPos(2, 2, 2);
		BlockPos posB = new BlockPos(6, 2, 2);
		helper.setBlock(posA, AWAutomationBlocks.MAILBOX.get());
		helper.setBlock(posB, AWAutomationBlocks.MAILBOX.get());
		TileMailbox boxA = helper.getBlockEntity(posA);
		TileMailbox boxB = helper.getBlockEntity(posB);
		String nameA = "gt_a_" + helper.absolutePos(posA).asLong();
		String nameB = "gt_b_" + helper.absolutePos(posB).asLong();
		MailboxData data = AWGameData.INSTANCE.getData(level, MailboxData.class);
		data.addMailbox(null, nameA);
		data.addMailbox(null, nameB);
		boxA.setMailboxName(nameA);
		boxB.setMailboxName(nameB);
		boxA.setTargetName(nameB);
		IItemHandler top = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(posA), Direction.UP);
		IItemHandler bottom = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(posA), Direction.DOWN);
		check(helper, top == boxA.sendInventory && bottom == boxA.receivedInventory, "top side = send inventory, bottom side = received inventory");
		boxA.sendInventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
		//4 blocks apart, mailboxTimePerBlock = 10 -> 40 ticks of travel
		long start = helper.getTick();
		long[] arrived = new long[] {-1};
		helper.onEachTick(() -> {
			if (arrived[0] < 0 && count(boxB.receivedInventory, Items.DIAMOND) == 3) {
				arrived[0] = helper.getTick() - start;
				check(helper, count(boxA.sendInventory, Items.DIAMOND) == 0, "the diamonds left the sender");
				check(helper, arrived[0] >= 4 * AWAutomationStatics.mailboxTimePerBlock && arrived[0] <= 4 * AWAutomationStatics.mailboxTimePerBlock + 5,
						"delivery takes distance * mailboxTimePerBlock = 40 ticks, took " + arrived[0]);
				helper.succeed();
			}
		});
	}

	//************************************** CHUNK LOADER ***************************************//

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void chunkLoaderForcesChunks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, AWAutomationBlocks.CHUNK_LOADER_SIMPLE.get());
		BlockPos abs = helper.absolutePos(pos);
		helper.runAfterDelay(2, () -> {
			ForcedChunksSavedData saved = level.getDataStorage().computeIfAbsent(ForcedChunksSavedData.factory(), "chunks");
			int forced = saved.getBlockForcedChunks().getTickingChunks().values().stream().mapToInt(set -> set.contains(new ChunkPos(abs).toLong()) ? set.size() : 0).sum();
			check(helper, forced == 9, "the simple chunk loader forces the 3x3 chunks around it, forced: " + forced);
			helper.setBlock(pos, Blocks.AIR);
			long left = saved.getBlockForcedChunks().getTickingChunks().values().stream().filter(set -> set.contains(new ChunkPos(abs).toLong())).count();
			check(helper, left == 0, "breaking the block releases the chunks");
			helper.succeed();
		});
	}

	//************************************** COMMANDS FILE ***************************************//

	/*
	 * Runs testing/automation.commands (setblock of every automation block and variant, data get, destroy) through the
	 * command dispatcher; a command that fails or throws fails the test. (The dev server started by Gradle does not
	 * read console input, so the file cannot be fed through boot_test.sh on this setup.)
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void commandsFile(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Path file = Paths.get("..", "testing", "automation.commands");
		if (!Files.exists(file)) {
			AncientWarfare.LOG.warn("testing/automation.commands not found from {}, commandsFile test skipped", Paths.get("").toAbsolutePath());
			helper.succeed();
			return;
		}
		List<String> lines;
		try {
			lines = Files.readAllLines(file);
		}
		catch (Exception e) {
			helper.fail("cannot read " + file + ": " + e);
			return;
		}
		List<String> failed = new ArrayList<>();
		List<String> first = new ArrayList<>();
		List<String> second = new ArrayList<>();
		boolean afterSleep = false;
		for (String line : lines) {
			if (line.startsWith("#sleep")) {
				afterSleep = true;
			} else if (!line.isEmpty() && !line.startsWith("#") && !line.startsWith("give ")) {
				(afterSleep ? second : first).add(line);
			}
		}
		run(level, first, failed);
		helper.runAfterDelay(40, () -> {
			run(level, second, failed);
			check(helper, failed.isEmpty(), failed.size() + " commands failed: " + failed);
			AncientWarfare.LOG.info("automation commands file: {} commands ran", first.size() + second.size());
			helper.succeed();
		});
	}

	private static void run(ServerLevel level, List<String> commands, List<String> failed) {
		for (String command : commands) {
			boolean[] ok = new boolean[1];
			CommandSourceStack source = level.getServer().createCommandSourceStack().withSuppressedOutput().withCallback((success, result) -> ok[0] = success);
			try {
				level.getServer().getCommands().performPrefixedCommand(source, command);
			}
			catch (Exception e) {
				failed.add(command + " -> " + e);
				continue;
			}
			if (!ok[0]) {
				failed.add(command);
			}
		}
	}

	/*
	 * a ticket holds at most 25 forced chunks and drops the oldest beyond that (Forge 1.12
	 * maximumChunksPerTicket; NeoForge has no limit), and work bounds sent by a client are refused when they are
	 * wider than the worksite allows or far away from it.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 100)
	public static void chunkTicketIsCappedAndBoundsAreChecked(GameTestHelper helper) {
		BlockPos owner = helper.absolutePos(new BlockPos(1, 2, 1));
		net.shadowmage.ancientwarfare.automation.chunkloader.AWChunkLoader.Ticket ticket = net.shadowmage.ancientwarfare.automation.chunkloader.AWChunkLoader.requestTicket(helper.getLevel(), owner);
		net.minecraft.world.level.ChunkPos base = new net.minecraft.world.level.ChunkPos(owner);
		try {
			for (int i = 0; i < 40; i++) {
				net.shadowmage.ancientwarfare.automation.chunkloader.AWChunkLoader.forceChunk(ticket, new net.minecraft.world.level.ChunkPos(base.x + i, base.z));
			}
			check(helper, ticket.getChunkList().size() == 25, "the ticket holds " + ticket.getChunkList().size() + " chunks");
			check(helper, !ticket.getChunkList().contains(base) && ticket.getChunkList().contains(new net.minecraft.world.level.ChunkPos(base.x + 39, base.z)), "the oldest chunks were not the ones dropped");
			check(helper, !forcedFor(helper, owner).contains(base.toLong()), "a dropped chunk is still forced");
			check(helper, forcedFor(helper, owner).contains(net.minecraft.world.level.ChunkPos.asLong(base.x + 39, base.z)) && forcedFor(helper, owner).contains(net.minecraft.world.level.ChunkPos.asLong(base.x + 15, base.z)), "the 25 newest chunks are not forced");
			check(helper, !forcedFor(helper, owner).contains(net.minecraft.world.level.ChunkPos.asLong(base.x + 14, base.z)), "chunk 15 of 40 is still forced");
		}
		finally {
			net.shadowmage.ancientwarfare.automation.chunkloader.AWChunkLoader.releaseTicket(ticket);
		}
		check(helper, !forcedFor(helper, owner).contains(net.minecraft.world.level.ChunkPos.asLong(base.x + 39, base.z)), "the released ticket still forces chunks");
		BlockPos site = new BlockPos(100, 64, 100);
		check(helper, net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteBoundsAdjust.acceptsBounds(16, site, new BlockPos(101, 64, 101), new BlockPos(116, 64, 116)), "bounds of the allowed size next to the worksite are refused");
		check(helper, net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteBoundsAdjust.acceptsBounds(64, site, new BlockPos(101, -60, 101), new BlockPos(164, 64, 164)), "quarry bounds down to the bottom of the world are refused");
		check(helper, !net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteBoundsAdjust.acceptsBounds(16, site, new BlockPos(0, 64, 0), new BlockPos(2000, 64, 2000)), "bounds of 2000 x 2000 blocks are accepted");
		check(helper, !net.shadowmage.ancientwarfare.automation.container.ContainerWorksiteBoundsAdjust.acceptsBounds(16, site, new BlockPos(29999000, 64, 100), new BlockPos(29999010, 64, 110)), "bounds far away from the worksite are accepted");
		helper.succeed();
	}

	/*
	 * A warehouse storage block with items survives the unloading of its chunk (1.21 calls setRemoved for
	 * every block entity of an unloading chunk; the storage tile then left its controller as if it had been broken and
	 * a NullPointerException in ItemQuantityMap.removeAll kept the chunk from being saved). The unload is replayed the
	 * way LevelChunk does it - save, onChunkUnloaded, setRemoved - for the storage block alone, for the controller
	 * alone and for both; the reload is a new block entity from the saved data, which is also what a server restart
	 * does. Nothing may throw, the contents stay, and the storage block is attached to its controller again.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 600)
	public static void warehouseStorageSurvivesChunkUnload(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TileWarehouse[] warehouse = {warehouse(helper)};
		BlockPos controlPos = helper.absolutePos(new BlockPos(1, 2, 1));
		BlockPos storagePos = helper.absolutePos(new BlockPos(3, 2, 3));
		IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(new BlockPos(4, 2, 3)), Direction.UP);
		inventory.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
		inventory.insertItem(1, new ItemStack(Items.IRON_INGOT, 10), false);
		work(helper, warehouse[0]);
		//0: wait for the items; 1..3: unload variants, each followed by a reload and a check
		int[] step = {0, 0};
		//an item map without the entries being removed (the state that threw)
		net.shadowmage.ancientwarfare.core.inventory.ItemQuantityMap empty = new net.shadowmage.ancientwarfare.core.inventory.ItemQuantityMap();
		net.shadowmage.ancientwarfare.core.inventory.ItemQuantityMap some = new net.shadowmage.ancientwarfare.core.inventory.ItemQuantityMap();
		some.addCount(new ItemStack(Items.COBBLESTONE), 5);
		empty.removeAll(some);
		helper.onEachTick(() -> {
			step[1]++;
			TileWarehouse control = (TileWarehouse) level.getBlockEntity(controlPos);
			boolean stocked = control != null && control.getCountOf(new ItemStack(Items.COBBLESTONE)) == 64 && control.getCountOf(new ItemStack(Items.IRON_INGOT)) == 10;
			net.minecraft.world.level.block.entity.BlockEntity storage = level.getBlockEntity(storagePos);
			boolean attached = storage instanceof net.shadowmage.ancientwarfare.automation.tile.warehouse2.TileControlled c && c.getController().orElse(null) == control;
			if (step[0] > 3) {
				return;
			}
			if (!(stocked && attached && control.getMaxStorage() > 0)) {
				if (step[1] > 150) {
					helper.fail("step " + step[0] + ": the warehouse does not hold 64 cobblestone and 10 iron in an attached storage block after " + (step[0] == 0 ? "storing" : "the reload")
							+ " (cobblestone " + (control == null ? "no controller" : control.getCountOf(new ItemStack(Items.COBBLESTONE))) + ", attached " + attached + ")");
				}
				return;
			}
			step[0]++;
			step[1] = 0;
			if (step[0] > 3) {
				helper.succeed();
				return;
			}
			boolean unloadStorage = step[0] == 1 || step[0] == 3;
			boolean unloadControl = step[0] == 2 || step[0] == 3;
			List<BlockPos> unload = new ArrayList<>();
			//the controller first in the "both" case: the storage tile then holds a controller that is already gone
			if (unloadControl) {
				unload.add(controlPos);
			}
			if (unloadStorage) {
				unload.add(storagePos);
			}
			List<net.minecraft.nbt.CompoundTag> saved = new ArrayList<>();
			for (BlockPos pos : unload) {
				saved.add(level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()));
			}
			for (BlockPos pos : unload) {
				net.minecraft.world.level.block.entity.BlockEntity tile = level.getBlockEntity(pos);
				tile.onChunkUnloaded();
				tile.setRemoved();
			}
			if (unloadStorage && !unloadControl) {
				check(helper, control.getCountOf(new ItemStack(Items.COBBLESTONE)) == 0, "a controller still counts the items of a storage block whose chunk is unloaded");
			}
			for (int i = 0; i < unload.size(); i++) {
				BlockPos pos = unload.get(i);
				net.minecraft.world.level.block.entity.BlockEntity fresh = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, level.getBlockState(pos), saved.get(i), level.registryAccess());
				check(helper, fresh != null, "the saved block entity at " + pos + " does not load");
				//what a chunk does with the block entities it loads: add and start ticking
				level.getChunkAt(pos).addAndRegisterBlockEntity(fresh);
			}
		});
	}

	/*
	 * Crops, seeds and saplings of other mods: 1.12 recognised them through IPlantable, which 1.21 does not have; the
	 * port recognises the vanilla classes and the tags other mods put their crops in. Every block in minecraft:crops
	 * and minecraft:saplings is farmable / plantable, every block item in c:seeds, minecraft:villager_plantable_seeds
	 * and minecraft:saplings can be planted, and the tree farm takes every sapling item. (Vanilla's tagged blocks also
	 * extend the vanilla classes, so this test cannot fail on the old code; it guards that tagged content of other
	 * mods is accepted.)
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 20)
	public static void taggedCropsSeedsAndSaplingsAreAccepted(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();
		for (net.minecraft.core.Holder<net.minecraft.world.level.block.Block> b : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getTagOrEmpty(net.minecraft.tags.BlockTags.CROPS)) {
			if (!net.shadowmage.ancientwarfare.automation.tile.worksite.TileWorksiteFarm.isPlantableBlock(b.value())) {
				problems.add("crop block " + b.getRegisteredName());
			}
		}
		for (net.minecraft.core.Holder<net.minecraft.world.level.block.Block> b : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getTagOrEmpty(net.minecraft.tags.BlockTags.SAPLINGS)) {
			if (!net.shadowmage.ancientwarfare.automation.tile.worksite.TileWorksiteFarm.isPlantableBlock(b.value())) {
				problems.add("sapling block " + b.getRegisteredName());
			}
		}
		for (net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag : List.of(net.neoforged.neoforge.common.Tags.Items.SEEDS, net.minecraft.tags.ItemTags.VILLAGER_PLANTABLE_SEEDS, net.minecraft.tags.ItemTags.SAPLINGS)) {
			for (net.minecraft.core.Holder<net.minecraft.world.item.Item> i : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
				ItemStack stack = new ItemStack(i.value());
				if (stack.getItem() instanceof net.minecraft.world.item.BlockItem && !net.shadowmage.ancientwarfare.automation.tile.worksite.TileWorksiteFarm.isPlantableItem(stack)) {
					problems.add("seed item " + i.getRegisteredName() + " (" + tag.location() + ")");
				}
			}
		}
		for (net.minecraft.core.Holder<net.minecraft.world.item.Item> i : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(net.minecraft.tags.ItemTags.SAPLINGS)) {
			if (!net.shadowmage.ancientwarfare.automation.registry.TreeFarmRegistry.getSapling(new ItemStack(i.value())).isPresent()) {
				problems.add("sapling item for the tree farm " + i.getRegisteredName());
			}
		}
		check(helper, problems.isEmpty(), "not accepted: " + problems);
		helper.succeed();
	}

	/* every chunk NeoForge keeps forced for block owners in the test level (the mod's chunk tickets) */
	private static it.unimi.dsi.fastutil.longs.LongSet forcedFor(GameTestHelper helper, BlockPos owner) {
		net.minecraft.world.level.ForcedChunksSavedData data = helper.getLevel().getDataStorage().computeIfAbsent(net.minecraft.world.level.ForcedChunksSavedData.factory(), "chunks");
		it.unimi.dsi.fastutil.longs.LongSet all = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
		data.getBlockForcedChunks().getTickingChunks().values().forEach(all::addAll);
		return all;
	}


	/*
	 * Big modpack audit: what a pipe or a polling reader of another mod (AE2 storage bus, Mekanism transporter) does with
	 * an exposed item handler. Simulated calls change nothing, a call never moves more than asked, the slot count is
	 * stable, the returned stacks are not the stored instance.
	 */
	private static void checkHandlerContract(GameTestHelper helper, IItemHandler handler, int slot, ItemStack sample, String name) {
		int slots = handler.getSlots();
		check(helper, slots > slot, name + ": slot count " + slots);
		check(helper, handler.getSlotLimit(slot) > 0, name + ": slot limit " + handler.getSlotLimit(slot));
		check(helper, handler.getStackInSlot(slot).isEmpty(), name + ": the slot starts empty");
		ItemStack offered = sample.copyWithCount(20);
		ItemStack remainder = handler.insertItem(slot, offered, true);
		check(helper, offered.getCount() == 20, name + ": a simulated insert must not change the offered stack");
		check(helper, handler.getStackInSlot(slot).isEmpty(), name + ": a simulated insert must not change the slot");
		check(helper, remainder.isEmpty(), name + ": the simulated insert accepts the stack");
		check(helper, handler.isItemValid(slot, sample), name + ": isItemValid agrees with the insert");
		remainder = handler.insertItem(slot, offered, false);
		check(helper, remainder.isEmpty() && offered.getCount() == 20, name + ": the insert takes all 20 and leaves the offered stack alone");
		check(helper, handler.getStackInSlot(slot).getCount() == 20, name + ": the slot holds the 20 items");
		ItemStack simulated = handler.extractItem(slot, 5, true);
		check(helper, simulated.getCount() == 5 && handler.getStackInSlot(slot).getCount() == 20, name + ": a simulated extract returns 5 and changes nothing");
		check(helper, handler.extractItem(slot, 0, false).isEmpty() && handler.getStackInSlot(slot).getCount() == 20, name + ": extracting 0 returns nothing");
		ItemStack extracted = handler.extractItem(slot, 5, false);
		check(helper, extracted.getCount() == 5 && ItemStack.isSameItemSameComponents(extracted, sample), name + ": the extract returns the 5 items asked for");
		extracted.setCount(1);
		check(helper, handler.getStackInSlot(slot).getCount() == 15, name + ": the returned stack is not the stored one");
		check(helper, handler.extractItem(slot, 64, false).getCount() == 15 && handler.getStackInSlot(slot).isEmpty(), name + ": an extract of more than is there returns the rest");
		check(helper, handler.getSlots() == slots, name + ": the slot count is stable");
	}

	/*
	 * the way AE2's storage bus reads an inventory (ExternalStorageFacade.getAvailableStacks behind a BlockCapabilityCache,
	 * with its "extractable only" filter): every non empty slot counts when one item can be extracted in simulation
	 */
	private static java.util.Map<Item, Integer> pollLikeStorageBus(GameTestHelper helper, BlockPos pos, Direction side, String name) {
		net.neoforged.neoforge.capabilities.BlockCapabilityCache<IItemHandler, Direction> cache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
				Capabilities.ItemHandler.BLOCK, helper.getLevel(), helper.absolutePos(pos), side);
		IItemHandler handler = cache.getCapability();
		check(helper, handler != null, name + ": no item handler on side " + side);
		java.util.Map<Item, Integer> seen = new java.util.HashMap<>();
		for (int i = 0; i < handler.getSlots(); i++) {
			ItemStack stack = handler.getStackInSlot(i);
			if (stack.isEmpty()) {
				continue;
			}
			check(helper, !handler.extractItem(i, 1, true).isEmpty(), name + ": slot " + i + " shows " + stack + " but nothing can be extracted");
			seen.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		return seen;
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void warehouseInterfaceIsReadableByPollingReaders(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, AWAutomationBlocks.WAREHOUSE_INTERFACE.get());
		TileWarehouseInterface iface = helper.getBlockEntity(pos);
		checkHandlerContract(helper, helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.NORTH), 4, new ItemStack(Items.COBBLESTONE), "warehouse interface");
		//what the warehouse puts into the buffer for a filter
		InventoryTools.insertItem(iface.getItemHandler(null), new ItemStack(Items.COBBLESTONE, 20));
		InventoryTools.insertItem(iface.getItemHandler(null), new ItemStack(Items.IRON_INGOT, 5));
		for (Direction side : Direction.values()) {
			java.util.Map<Item, Integer> seen = pollLikeStorageBus(helper, pos, side, "warehouse interface");
			check(helper, seen.getOrDefault(Items.COBBLESTONE, 0) == 20 && seen.getOrDefault(Items.IRON_INGOT, 0) == 5 && seen.size() == 2,
					"a reader on side " + side + " sees the buffer of the interface: " + seen);
		}
		check(helper, helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null) == iface.getItemHandler(null), "the interface answers without a side too (1.12: any facing)");
		helper.succeed();
	}

	/*
	 * 1.12 TileWorksiteBoundedInventory.getCapability: the face decides the inventory through the "Sides" mapping, a
	 * face mapped to nothing has no handler. A reader that cached the answer (BlockCapabilityCache) must see a changed
	 * mapping.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void worksiteFacesFollowTheSideMapping(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos quarryPos = new BlockPos(2, 2, 2);
		helper.setBlock(quarryPos, AWAutomationBlocks.QUARRY.get());
		WorkSiteQuarry quarry = helper.getBlockEntity(quarryPos);
		for (Direction side : Direction.values()) {
			IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(quarryPos), side);
			check(helper, side == Direction.DOWN ? handler == quarry.mainInventory : handler == null, "quarry, default mapping: only the bottom face gives the main inventory, " + side + " gave " + handler);
		}
		check(helper, level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(quarryPos), null) == null, "quarry: no handler without a face (1.12 the same)");
		checkHandlerContract(helper, level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(quarryPos), Direction.DOWN), 0, new ItemStack(Items.COBBLESTONE), "quarry main inventory");
		net.neoforged.neoforge.capabilities.BlockCapabilityCache<IItemHandler, Direction> top = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
				Capabilities.ItemHandler.BLOCK, level, helper.absolutePos(quarryPos), Direction.UP);
		check(helper, top.getCapability() == null, "quarry: nothing on the top face by default");
		quarry.setInventorySideMappings(net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.RelativeSide.TOP, net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.RelativeSide.TOP);
		check(helper, top.getCapability() == quarry.mainInventory, "quarry: a cached reader sees the main inventory on the top face after the mapping changed");

		BlockPos farmPos = new BlockPos(6, 2, 6);
		helper.setBlock(farmPos, AWAutomationBlocks.CROP_FARM.get());
		WorkSiteCropFarm farm = helper.getBlockEntity(farmPos);
		Direction front = net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.RelativeSide.getMCSideToAccess(
				net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.RotationType.FOUR_WAY, farm.getPrimaryFacing(), net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.RelativeSide.FRONT);
		check(helper, front != null && front.getAxis().isHorizontal(), "crop farm: the front is a horizontal face: " + front);
		for (Direction side : Direction.values()) {
			IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(farmPos), side);
			IItemHandler expected = side == Direction.DOWN ? farm.mainInventory : side == Direction.UP ? farm.miscInventory : side == front ? farm.plantableInventory : null;
			check(helper, handler == expected, "crop farm, default mapping (bottom = main, top = bonemeal, front = seeds): face " + side + " gave " + handler);
		}
		checkHandlerContract(helper, farm.mainInventory, 0, new ItemStack(Items.WHEAT), "crop farm main inventory");
		helper.succeed();
	}
}
