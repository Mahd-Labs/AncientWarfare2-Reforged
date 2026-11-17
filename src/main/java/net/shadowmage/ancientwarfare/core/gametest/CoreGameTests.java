package net.shadowmage.ancientwarfare.core.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.block.BlockResearchStation;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.crafting.AWCraftingManager;
import net.shadowmage.ancientwarfare.core.crafting.ICraftingRecipe;
import net.shadowmage.ancientwarfare.core.init.AWCoreBlocks;
import net.shadowmage.ancientwarfare.core.init.AWCoreItems;
import net.shadowmage.ancientwarfare.core.inventory.ItemQuantityMap;
import net.shadowmage.ancientwarfare.core.item.ItemResearchBook;
import net.shadowmage.ancientwarfare.core.owner.Owner;
import net.shadowmage.ancientwarfare.core.registry.ResearchRegistry;
import net.shadowmage.ancientwarfare.core.research.ResearchTracker;
import net.shadowmage.ancientwarfare.core.tile.TileEngineeringStation;
import net.shadowmage.ancientwarfare.core.tile.TileResearchStation;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.core.util.NBTBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
 * Headless checks of the core module (run with ./gradlew runGameTestServer). The template is an empty 3x3x3 space.
 */
@GameTestHolder(AncientWarfare.MOD_ID)
@PrefixGameTestTemplate(false)
public class CoreGameTests {
	private static final String TEMPLATE = "core_empty";

	private static void check(GameTestHelper helper, boolean condition, String message) {
		if (!condition) {
			helper.fail(message);
		}
	}

	private static ItemStack boundBook(String researcher) {
		ItemStack book = new ItemStack(AWCoreItems.RESEARCH_BOOK.get());
		ItemTools.setTagInfo(book, "researcherName", StringTag.valueOf(researcher));
		return book;
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void inventoryToolsMergeRemove(GameTestHelper helper) {
		ItemStackHandler handler = new ItemStackHandler(3);
		check(helper, InventoryTools.insertItem(handler, new ItemStack(Items.STICK, 70)).isEmpty(), "70 sticks fit into 3 slots");
		check(helper, handler.getStackInSlot(0).getCount() == 64 && handler.getStackInSlot(1).getCount() == 6, "sticks fill slot 0 then slot 1");
		check(helper, InventoryTools.getCountOf(handler, new ItemStack(Items.STICK)) == 70, "getCountOf counts both slots");
		check(helper, InventoryTools.canInventoryHold(handler, new ItemStack(Items.STICK, 64 + 58)), "exactly the free space can be held");
		check(helper, !InventoryTools.canInventoryHold(handler, new ItemStack(Items.STICK, 64 + 59)), "one more than the free space can not be held");
		check(helper, handler.getStackInSlot(2).isEmpty(), "simulated insert does not change the inventory");

		ItemStack toMerge = new ItemStack(Items.STICK, 10);
		ItemStack merged = InventoryTools.mergeItemStack(handler, toMerge);
		check(helper, merged.isEmpty() || toMerge.isEmpty(), "mergeItemStack merges everything");
		check(helper, handler.getStackInSlot(1).getCount() == 16, "mergeItemStack tops up the partial slot");

		ItemStack removed = InventoryTools.removeItems(handler, new ItemStack(Items.STICK), 70);
		check(helper, removed.getCount() == 64, "removeItems is capped at the max stack size, got " + removed.getCount());
		check(helper, InventoryTools.getCountOf(handler, new ItemStack(Items.STICK)) == 16, "16 sticks are left");

		ItemStackHandler target = new ItemStackHandler(1);
		check(helper, InventoryTools.transferItems(handler, target, new ItemStack(Items.STICK), 10) == 10, "transferItems moves 10");
		check(helper, target.getStackInSlot(0).getCount() == 10 && InventoryTools.getCountOf(handler, new ItemStack(Items.STICK)) == 6, "10 moved, 6 left");

		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		ItemStack damagedSword = new ItemStack(Items.IRON_SWORD);
		damagedSword.setDamageValue(5);
		check(helper, !InventoryTools.doItemStacksMatch(sword, damagedSword), "damage matters for doItemStacksMatch");
		check(helper, InventoryTools.doItemStacksMatch(sword, damagedSword, true, false), "damage can be ignored");
		check(helper, InventoryTools.doItemStacksMatchRelaxed(sword, damagedSword), "relaxed match ignores damage");
		ItemStack note = AWCoreItems.RESEARCH_NOTE.get().getNote("invention");
		ItemStack plainNote = new ItemStack(AWCoreItems.RESEARCH_NOTE.get());
		check(helper, !InventoryTools.doItemStacksMatch(plainNote, note), "a stack without tag does not match one with a tag");
		check(helper, InventoryTools.doItemStacksMatch(plainNote, note, false, true), "the tag can be ignored");
		check(helper, InventoryTools.doItemStacksMatch(note, AWCoreItems.RESEARCH_NOTE.get().getNote("invention")), "equal tags match");
		check(helper, !InventoryTools.doItemStacksMatchRelaxed(note, AWCoreItems.RESEARCH_NOTE.get().getNote("engineering")), "different tags do not match");

		NonNullList<ItemStack> list = NonNullList.create();
		list.add(new ItemStack(Items.COBBLESTONE, 40));
		list.add(new ItemStack(Items.COBBLESTONE, 40));
		list.add(note.copy());
		NonNullList<ItemStack> compact = InventoryTools.compactStackList(list);
		int cobble = compact.stream().filter(s -> s.is(Items.COBBLESTONE)).mapToInt(ItemStack::getCount).sum();
		check(helper, compact.size() == 3 && cobble == 80, "compactStackList gives 64 + 16 cobblestone and the note");

		ItemQuantityMap map = new ItemQuantityMap();
		map.addCount(note, 3);
		map.addCount(new ItemStack(Items.STICK), 5);
		ItemQuantityMap read = new ItemQuantityMap();
		read.readFromNBT(map.writeToNBT(new CompoundTag()));
		check(helper, read.getCount(note) == 3 && read.getCount(new ItemStack(Items.STICK)) == 5, "ItemQuantityMap survives nbt (stack with custom data included)");

		SimpleContainer container = new SimpleContainer(3);
		container.setItem(1, note.copy());
		SimpleContainer container2 = new SimpleContainer(3);
		InventoryTools.readInventoryFromNBT(container2, InventoryTools.writeInventoryToNBT(container, new CompoundTag()));
		check(helper, InventoryTools.doItemStacksMatch(container2.getItem(1), note) && container2.getItem(0).isEmpty(), "inventory nbt round trip keeps slot and tag");
		helper.succeed();
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void ownerAndTeamChecks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		UUID alice = UUID.nameUUIDFromBytes("GTAlice".getBytes());
		UUID bob = UUID.nameUUIDFromBytes("GTBob".getBytes());
		UUID carol = UUID.nameUUIDFromBytes("GTCarol".getBytes());
		Owner owner = Owner.deserializeFromNBT(new NBTBuilder().setString("ownerName", "GTAlice").setUniqueId("ownerId", alice).build());
		check(helper, owner != Owner.EMPTY && owner.getName().equals("GTAlice") && owner.getUUID().equals(alice), "owner is read from nbt");
		check(helper, Owner.deserializeFromNBT(owner.serializeToNBT(new CompoundTag())).getUUID().equals(alice), "owner nbt round trip");
		check(helper, Owner.deserializeFromNBT(new CompoundTag()) == Owner.EMPTY, "no owner tag gives the empty owner");

		check(helper, owner.isOwnerOrSameTeamOrFriend(level, alice, "GTAlice"), "the owner is friendly to itself");
		check(helper, !owner.isOwnerOrSameTeamOrFriend(level, bob, "GTBob"), "a stranger is not friendly");
		Scoreboard scoreboard = level.getScoreboard();
		PlayerTeam team = scoreboard.getPlayerTeam("gt_core_team");
		if (team == null) {
			team = scoreboard.addPlayerTeam("gt_core_team");
		}
		scoreboard.addPlayerToTeam("GTAlice", team);
		scoreboard.addPlayerToTeam("GTBob", team);
		check(helper, owner.isOwnerOrSameTeamOrFriend(level, bob, "GTBob"), "a scoreboard team mate is friendly");
		check(helper, owner.playerHasCommandPermissions(level, bob, "GTBob"), "a scoreboard team mate has command permissions");
		check(helper, !owner.isOwnerOrSameTeamOrFriend(level, carol, "GTCarol"), "a player outside the team is not friendly");
		check(helper, !Owner.EMPTY.playerHasCommandPermissions(level, bob, "GTBob"), "the empty owner gives no permissions");

		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, AWCoreBlocks.RESEARCH_STATION.get());
		TileResearchStation tile = helper.getBlockEntity(pos);
		tile.setOwner(owner);
		CompoundTag saved = tile.saveWithoutMetadata(level.registryAccess());
		check(helper, saved.getString("ownerName").equals("GTAlice"), "the owned tile saves its owner");
		check(helper, tile.getTeam() == team, "the work site reports the owner's scoreboard team");
		scoreboard.removePlayerTeam(team);
		helper.succeed();
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void researchStationResearchesQueuedGoal(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String researcher = "GTResearcher";
		check(helper, ResearchRegistry.researchExists("invention") && ResearchRegistry.getAllResearchGoals().size() == 29, "29 research goals are loaded from the registry files (1.12 had 30; naval_warfare went with the boats)");

		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, AWCoreBlocks.RESEARCH_STATION.get());
		TileResearchStation tile = helper.getBlockEntity(pos);
		check(helper, tile.bookInventory.insertItem(0, new ItemStack(AWCoreItems.RESEARCH_BOOK.get()), false).getCount() == 1, "an unbound book is rejected");
		check(helper, tile.bookInventory.insertItem(0, boundBook(researcher), false).isEmpty(), "a bound book is accepted");
		check(helper, helper.getBlockState(pos).getValue(BlockResearchStation.HAS_BOOK), "the block state shows the book");
		check(helper, researcher.equals(tile.getCrafterName()), "the station knows its researcher");

		IItemHandler resources = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
		check(helper, resources == tile.resourceInventory, "the item handler capability is the resource inventory");
		InventoryTools.insertItem(resources, new ItemStack(Items.STICK, 12));
		InventoryTools.insertItem(resources, new ItemStack(Items.STRING, 3));
		InventoryTools.insertItem(resources, new ItemStack(Items.CHARCOAL, 4));
		InventoryTools.insertItem(resources, new ItemStack(Items.TORCH, 5));

		//create the research entry of the researcher (normally done at login), then queue the first goal
		ResearchTracker.INSTANCE.addResearch(level, researcher, "invention");
		ResearchTracker.INSTANCE.clearResearch(level, researcher);
		check(helper, ResearchTracker.INSTANCE.getResearchableGoals(level, researcher).contains("invention"), "invention can be researched");
		check(helper, !ResearchTracker.INSTANCE.getResearchableGoals(level, researcher).contains("engineering"), "engineering needs invention first");
		ResearchTracker.INSTANCE.addQueuedGoal(level, researcher, "invention");
		for (int i = 0; i < 5; i++) {
			tile.addEnergyFromPlayer(null);
		}
		check(helper, tile.getTorqueStored(null) == 250, "5 player work units are 250 energy");

		helper.runAfterDelay(5, () -> {
			check(helper, ResearchTracker.INSTANCE.getCurrentGoal(level, researcher).equals(Optional.of("invention")), "the queued goal was started");
			check(helper, InventoryTools.getCountOf(resources, s -> !s.isEmpty()) == 1, "the research resources were used (one spare torch left), left: " + InventoryTools.getItems(resources));
		});
		helper.succeedWhen(() -> {
			check(helper, ResearchTracker.INSTANCE.hasPlayerCompleted(level, researcher, "invention"), "invention not finished, progress " + ResearchTracker.INSTANCE.getProgress(level, researcher));
			check(helper, tile.getTorqueStored(null) <= 51, "200 research ticks used 200 energy, left " + tile.getTorqueStored(null));
		});
	}

	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void engineeringStationResearchRecipe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String crafter = "GTCrafter";
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, AWCoreBlocks.ENGINEERING_STATION.get());
		TileEngineeringStation tile = helper.getBlockEntity(pos);
		CraftingContainer matrix = tile.craftingRecipeMemory.craftMatrix;
		//iron hammer: MSM / _S_ / _S_ , needs the research "engineering"
		matrix.setItem(0, new ItemStack(Items.IRON_INGOT));
		matrix.setItem(1, new ItemStack(Items.STICK));
		matrix.setItem(2, new ItemStack(Items.IRON_INGOT));
		matrix.setItem(4, new ItemStack(Items.STICK));
		matrix.setItem(7, new ItemStack(Items.STICK));

		check(helper, AWCraftingManager.getRecipes(level).size() >= 15, "the 15 core research recipes are loaded, got " + AWCraftingManager.getRecipes(level).size());
		check(helper, AWCraftingManager.findMatchingRecipes(matrix, level, null).isEmpty(), "no recipe without a research book");
		tile.craftingRecipeMemory.bookSlot.setStackInSlot(0, boundBook(crafter));
		check(helper, crafter.equals(ItemResearchBook.getResearcherName(tile.craftingRecipeMemory.bookSlot.getStackInSlot(0))), "book is bound");
		check(helper, AWCraftingManager.findMatchingRecipes(matrix, level, tile.craftingRecipeMemory.getCrafterName()).isEmpty(), "no recipe without the research");

		ResearchTracker.INSTANCE.addResearch(level, crafter, "engineering");
		List<ICraftingRecipe> recipes = AWCraftingManager.findMatchingRecipes(matrix, level, tile.craftingRecipeMemory.getCrafterName());
		check(helper, recipes.size() == 1, "one recipe with the research, got " + recipes.size());
		ICraftingRecipe recipe = recipes.get(0);
		check(helper, recipe.getRegistryName().toString().equals("research|ancientwarfare:core/iron_hammer"), "recipe id is " + recipe.getRegistryName());
		check(helper, recipe.getNeededResearch().equals(Optional.of("engineering")), "the recipe needs engineering");
		tile.craftingRecipeMemory.setRecipe(recipe);
		check(helper, tile.craftingRecipeMemory.outputSlot.getItem(0).is(AWCoreItems.IRON_HAMMER.get()), "the output slot shows the iron hammer");

		//the recipe memory survives saving and loading
		CompoundTag saved = tile.saveWithoutMetadata(level.registryAccess());
		tile.craftingRecipeMemory.setRecipe(AWCraftingManager.getRecipe(level, net.shadowmage.ancientwarfare.core.crafting.RecipeResourceLocation.NO_RECIPE_REGISTRY_NAME));
		tile.loadWithComponents(saved, level.registryAccess());
		check(helper, tile.craftingRecipeMemory.getRecipe().getRegistryName().equals(recipe.getRegistryName()), "the selected recipe is restored from nbt");

		//ingredients are found in the storage slots (what the station uses to refill the matrix)
		tile.extraSlots.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 2));
		tile.extraSlots.setStackInSlot(1, new ItemStack(Items.STICK, 2));
		NonNullList<ItemStack> match = AWCraftingManager.getRecipeInventoryMatch(recipe, tile.extraSlots);
		check(helper, match.isEmpty(), "2 sticks are not enough for the 3 needed");
		tile.extraSlots.setStackInSlot(1, new ItemStack(Items.STICK, 3));
		match = AWCraftingManager.getRecipeInventoryMatch(recipe, tile.extraSlots);
		check(helper, match.stream().filter(s -> !s.isEmpty()).count() == 5, "all 5 ingredients are matched from the storage slots");

		//a research recipe is no normal crafting recipe ...
		CraftingInput input = matrix.asCraftInput();
		Optional<RecipeHolder<CraftingRecipe>> vanilla = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
		check(helper, vanilla.isEmpty(), "the research recipe is not craftable in a normal crafting grid");
		//... unless the research system is switched off in the config
		boolean useResearchSystem = AWCoreStatics.useResearchSystem;
		AWCoreStatics.useResearchSystem = false;
		try {
			vanilla = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
			check(helper, vanilla.isPresent() && vanilla.get().value().assemble(input, level.registryAccess()).is(AWCoreItems.IRON_HAMMER.get()), "without research system the recipe works in a normal crafting grid");
			recipes = AWCraftingManager.findMatchingRecipes(matrix, level, null);
			check(helper, recipes.size() == 1 && recipes.get(0).getRegistryName().toString().equals("regular|ancientwarfare:core/iron_hammer"), "without research system the station lists it as a regular recipe");
		}
		finally {
			AWCoreStatics.useResearchSystem = useResearchSystem;
		}
		helper.succeed();
	}


	/*
	 * Big modpack audit: the item list of a config value (automation "animal_farm_pickups") with entries of mods that are
	 * not installed, malformed entries and characters that are not valid in an id. 1.12 skipped them with a log line;
	 * ResourceLocation.parse throws for the last kind in 1.21.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void configItemListSkipsBadEntries(GameTestHelper helper) {
		java.util.List<net.shadowmage.ancientwarfare.core.util.ItemWrapper> list = net.shadowmage.ancientwarfare.core.util.ItemWrapper.buildList("game test list", new String[] {
				"minecraft:egg", "notinstalledmod:golden_feather", "Minecraft:Bad Name", "minecraft:feather:abc", "one:two:three:four", "nocolon", "", "  ", "minecraft:wool:3", "minecraft:not_an_item"});
		check(helper, list.size() == 1 && list.get(0).item == Items.EGG, "only the valid entry of an installed mod is kept: " + list.size());
		helper.succeed();
	}

	/*
	 * Ids read from saved data, packets and typed text: 1.12 accepted any text, 1.21 ResourceLocation.parse throws on
	 * invalid characters. A bad id is now skipped (or a default used) instead of throwing during a chunk load, a
	 * packet or in a screen.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 20)
	public static void badIdsInSavedDataDoNotThrow(GameTestHelper helper) {
		String bad = "Some Mod:Bad Name!";
		check(helper, net.shadowmage.ancientwarfare.core.util.RegistryTools.parseOrNull(bad) == null, "an invalid id is parsed");
		check(helper, net.shadowmage.ancientwarfare.core.crafting.RecipeResourceLocation.deserialize("research|" + bad) != null, "a bad recipe id from a packet throws or gives null");
		check(helper, bad.equals(net.shadowmage.ancientwarfare.core.util.EntityTools.getUnlocName(bad)), "a bad entity id gives no name");
		net.minecraft.nbt.CompoundTag loot = new net.minecraft.nbt.CompoundTag();
		loot.putString("lootTableName", bad);
		loot.putString("entity", bad);
		net.shadowmage.ancientwarfare.structure.tile.LootSettings.deserializeNBT(loot);
		net.minecraft.nbt.CompoundTag team = new net.minecraft.nbt.CompoundTag();
		team.putString("teamName", "Red Team+");
		check(helper, net.shadowmage.ancientwarfare.npc.gamedata.Team.deserializeNBT(team) != null, "a team with an old name does not load");
		helper.succeed();
	}
}
