package net.shadowmage.ancientwarfare.npc.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.init.AWCoreItems;
import net.shadowmage.ancientwarfare.core.interfaces.IWorkSite;
import net.shadowmage.ancientwarfare.core.interfaces.IWorker;
import net.shadowmage.ancientwarfare.core.owner.Owner;
import net.shadowmage.ancientwarfare.core.upgrade.WorksiteUpgrade;
import net.shadowmage.ancientwarfare.npc.ai.NpcAI;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.NpcCombat;
import net.shadowmage.ancientwarfare.npc.entity.NpcCourier;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.entity.NpcPlayerOwned;
import net.shadowmage.ancientwarfare.npc.entity.NpcWorker;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.npc.init.AWNPCEntities;
import net.shadowmage.ancientwarfare.npc.init.AWNPCItems;
import net.shadowmage.ancientwarfare.npc.orders.CombatOrder;
import net.shadowmage.ancientwarfare.npc.orders.RoutingOrder;
import net.shadowmage.ancientwarfare.npc.orders.UpkeepOrder;
import net.shadowmage.ancientwarfare.npc.orders.WorkOrder;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;
import net.shadowmage.ancientwarfare.npc.registry.NpcDefaultsRegistry;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/*
 * Headless checks of the NPC module (run with ./gradlew runGameTestServer). The template is an empty 16x16x16 space;
 * every test lays its own stone floor at y = 1 and works at y = 2.
 */
@GameTestHolder(AncientWarfare.MOD_ID)
@PrefixGameTestTemplate(false)
public class NpcGameTests {
	private static final String TEMPLATE = "npc_empty";
	private static final String OWNER = "AwTestOwner";

	private static void check(GameTestHelper helper, boolean condition, String message) {
		if (!condition) {
			helper.fail(message);
		}
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new GameTestAssertException(message);
		}
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static <T extends Entity> T place(GameTestHelper helper, T entity, int x, int z) {
		Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(x, 2, z)));
		entity.moveTo(pos.x, pos.y, pos.z, 0, 0);
		if (entity instanceof NpcBase) {
			//what ItemNpcSpawner does; without a home the 1.12 attack range formula measures from the world origin
			((NpcBase) entity).setHomeAreaAtCurrentPosition();
		}
		helper.getLevel().addFreshEntity(entity);
		return entity;
	}

	@SuppressWarnings("unchecked")
	private static <T extends NpcBase> T owned(GameTestHelper helper, String type, String subType, int x, int z) {
		NpcBase npc = AWNPCEntities.createNpc(helper.getLevel(), type, subType, "");
		if (npc == null) {
			throw new GameTestAssertException("could not create npc " + type);
		}
		npc.setOwnerName(OWNER);
		npc.setFoodRemaining(6000);
		return (T) place(helper, npc, x, z);
	}

	private static ChestBlockEntity chest(GameTestHelper helper, int x, int z) {
		BlockPos pos = new BlockPos(x, 2, z);
		helper.setBlock(pos, Blocks.CHEST);
		return (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
	}

	/* every registered npc type can be created from its entity type alone (what "/summon" does) */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void everyNpcTypeCanBeCreated(GameTestHelper helper) {
		floor(helper);
		int i = 0;
		for (AWNPCEntities.NpcDeclaration declaration : AWNPCEntities.getNpcMap().values()) {
			NpcBase npc = declaration.getEntityType().create(helper.getLevel());
			check(helper, npc != null, "entity of " + declaration.name() + " is created");
			place(helper, npc, 1 + (i % 7) * 2, 1 + (i / 7) * 2);
			check(helper, npc.getNpcFullType() != null && !npc.getNpcFullType().isEmpty(), declaration.name() + " has a full type");
			if (npc instanceof NpcFaction) {
				check(helper, FactionRegistry.getFactionNames().contains(((NpcFaction) npc).getFaction()), declaration.name() + " has a registered default faction");
			}
			CompoundTag tag = new CompoundTag();
			check(helper, npc.saveWithoutId(tag).contains("levelingStats"), declaration.name() + " saves its data");
			i++;
		}
		check(helper, i == 22, "22 npc entity types are registered, found " + i);
		helper.succeed();
	}

	/* experience raises the level and with it the attack damage (combat) */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void experienceRaisesLevelAndDamage(GameTestHelper helper) {
		floor(helper);
		NpcCombat npc = owned(helper, "combat", "soldier", 4, 4);
		check(helper, npc.getMainHandItem().is(Items.IRON_SWORD), "soldier sub type spawns with an iron sword");
		int levelBefore = npc.getLevelingStats().getLevel();
		double damageBefore = npc.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
		npc.addExperience(100000);
		int levelAfter = npc.getLevelingStats().getLevel();
		double damageAfter = npc.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
		check(helper, levelBefore == 0, "a new npc is level 0");
		check(helper, levelAfter > levelBefore, "experience raises the level (now " + levelAfter + ")");
		check(helper, damageAfter > damageBefore, "attack damage grows with the level: " + damageBefore + " -> " + damageAfter);
		helper.succeed();
	}

	/* inventory, orders, level and owner survive writing to and reading from NBT */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void npcSurvivesSaveAndLoad(GameTestHelper helper) {
		floor(helper);
		NpcCombat npc = owned(helper, "combat", "soldier", 4, 4);
		//an enchanted weapon: enchantments are registry backed components, they only survive with a real registry access
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 3);
		npc.setItemSlot(EquipmentSlot.MAINHAND, sword);
		ItemStack orders = new ItemStack(AWNPCItems.COMBAT_ORDER.get());
		CombatOrder order = CombatOrder.getCombatOrder(orders);
		order.addPatrolPoint(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)));
		order.addPatrolPoint(helper.getLevel(), helper.absolutePos(new BlockPos(8, 2, 8)));
		order.write(orders);
		npc.setItemStackToSlot(NpcBase.ORDER_SLOT, orders);
		npc.setItemStackToSlot(NpcBase.UPKEEP_SLOT, new ItemStack(AWNPCItems.UPKEEP_ORDER.get()));
		npc.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		npc.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(AWNPCItems.WOODEN_SHIELD.get()));
		npc.addExperience(100000);
		npc.setFoodRemaining(1234);
		int level = npc.getLevelingStats().getLevel();

		CompoundTag tag = new CompoundTag();
		check(helper, npc.save(tag), "npc can be saved");
		Entity loadedEntity = EntityType.loadEntityRecursive(tag, helper.getLevel(), e -> e);
		check(helper, loadedEntity instanceof NpcCombat, "saved npc loads as NpcCombat");
		NpcCombat loaded = (NpcCombat) loadedEntity;
		check(helper, loaded.getMainHandItem().is(Items.IRON_SWORD), "main hand item is kept");
		check(helper, net.shadowmage.ancientwarfare.npc.entity.RangeAttackHelper.getEnchantmentLevel(loaded, net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, loaded.getMainHandItem()) == 3, "the enchantment of the weapon is kept");
		//the same through the item form (NBTHelper item stack round trip inside the spawner item)
		NpcBase respawned = net.shadowmage.ancientwarfare.npc.item.ItemNpcSpawner.createNpcFromItem(helper.getLevel(), net.shadowmage.ancientwarfare.npc.item.ItemNpcSpawner.getSpawnerItemForNpc(npc));
		check(helper, respawned instanceof NpcCombat && net.shadowmage.ancientwarfare.npc.entity.RangeAttackHelper.getEnchantmentLevel(respawned, net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, respawned.getMainHandItem()) == 3, "the enchantment survives repacking into a spawner item");
		check(helper, respawned.getLevelingStats().getLevel() == level, "the level survives repacking into a spawner item");
		check(helper, loaded.getOffhandItem().is(AWNPCItems.WOODEN_SHIELD.get()), "shield is kept");
		check(helper, loaded.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "helmet is kept");
		check(helper, loaded.ordersStack.is(AWNPCItems.COMBAT_ORDER.get()), "orders item is kept");
		check(helper, CombatOrder.getCombatOrder(loaded.ordersStack).size() == 2, "the orders keep their two patrol points: " + CombatOrder.getCombatOrder(loaded.ordersStack).size() + " before " + CombatOrder.getCombatOrder(npc.ordersStack).size() + " tag " + tag.get("ordersStack"));
		check(helper, loaded.upkeepStack.is(AWNPCItems.UPKEEP_ORDER.get()), "upkeep order is kept");
		check(helper, loaded.getLevelingStats().getLevel() == level && level > 0, "level is kept (" + level + ")");
		check(helper, OWNER.equals(loaded.getOwner().getName()), "owner is kept");
		check(helper, loaded.getFoodRemaining() == 1234, "food value is kept");
		check(helper, "soldier".equals(loaded.getNpcSubType()), "sub type follows the weapon");
		helper.succeed();
	}

	/* two soldiers of factions that are hostile towards each other fight */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 600)
	public static void hostileFactionSoldiersFight(GameTestHelper helper) {
		floor(helper);
		String factionA = null;
		String factionB = null;
		for (String a : FactionRegistry.getFactionNames()) {
			for (String b : FactionRegistry.getFactionNames()) {
				if (!a.equals(b) && FactionRegistry.getFaction(a).isHostileTowards(b) && FactionRegistry.getFaction(b).isHostileTowards(a)
						&& NpcDefaultsRegistry.getFactionNpcDefault(a, "soldier").isEnabled() && NpcDefaultsRegistry.getFactionNpcDefault(b, "soldier").isEnabled()) {
					factionA = a;
					factionB = b;
					break;
				}
			}
			if (factionA != null) {
				break;
			}
		}
		check(helper, factionA != null, "two mutually hostile factions exist");
		NpcBase first = place(helper, AWNPCEntities.createNpc(helper.getLevel(), "faction.soldier", "", factionA), 5, 7);
		NpcBase second = place(helper, AWNPCEntities.createNpc(helper.getLevel(), "faction.soldier", "", factionB), 10, 7);
		check(helper, first.isHostileTowards(second) && second.isHostileTowards(first), factionA + " and " + factionB + " soldiers are hostile");
		helper.succeedWhen(() -> {
			require(first.getTarget() == second || second.getTarget() == first, "a soldier targets the other");
			require(!first.isAlive() || !second.isAlive() || first.getHealth() < first.getMaxHealth() || second.getHealth() < second.getMaxHealth(), "a soldier was damaged: dist " + first.distanceTo(second) + " targets " + first.getTarget() + " / " + second.getTarget() + " dmg " + first.getAttributeValue(Attributes.ATTACK_DAMAGE) + " tasks " + first.getAITasks() + " alive " + first.isAlive() + second.isAlive());
		});
	}

	/* an owned combat npc attacks a hostile mob */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 600)
	public static void ownedSoldierAttacksZombie(GameTestHelper helper) {
		floor(helper);
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				helper.setBlock(new BlockPos(x, 6, z), Blocks.STONE); //roof: no sunlight on the zombie
			}
		}
		NpcCombat npc = owned(helper, "combat", "soldier", 4, 7);
		Zombie zombie = place(helper, EntityType.ZOMBIE.create(helper.getLevel()), 10, 7);
		zombie.setNoAi(true);
		check(helper, npc.isHostileTowards(zombie), "owned npcs are hostile towards zombies");
		helper.succeedWhen(() -> {
			require(npc.getTarget() == zombie || !zombie.isAlive(), "the soldier targets the zombie");
			require(!zombie.isAlive() || zombie.getHealth() < zombie.getMaxHealth(), "the zombie was damaged");
		});
	}

	/* an owned combat npc with a combat order walks its patrol route */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void ownedSoldierPatrols(GameTestHelper helper) {
		floor(helper);
		NpcCombat npc = owned(helper, "combat", "soldier", 2, 2);
		BlockPos far = helper.absolutePos(new BlockPos(12, 2, 12));
		ItemStack orders = new ItemStack(AWNPCItems.COMBAT_ORDER.get());
		CombatOrder order = CombatOrder.getCombatOrder(orders);
		order.addPatrolPoint(helper.getLevel(), far);
		order.addPatrolPoint(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)));
		order.write(orders);
		npc.setItemStackToSlot(NpcBase.ORDER_SLOT, orders);
		helper.succeedWhen(() -> require(npc.getDistanceSq(far) < 9, "the soldier reaches the far patrol point, distance^2 " + npc.getDistanceSq(far)));
	}

	/* a worker with a work order walks to the work site and works there */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void workerWorksAtWorksite(GameTestHelper helper) {
		floor(helper);
		BlockPos sitePos = new BlockPos(12, 2, 12);
		helper.setBlock(sitePos, Blocks.BARREL);
		BlockPos absSite = helper.absolutePos(sitePos);
		ServerLevel level = helper.getLevel();
		TestWorkSite site = new TestWorkSite(absSite, level.getBlockState(absSite));
		level.setBlockEntity(site);

		NpcWorker npc = owned(helper, "worker", "miner", 2, 2);
		check(helper, npc.getMainHandItem().is(Items.IRON_PICKAXE), "miner sub type spawns with an iron pickaxe");
		check(helper, npc.canWorkAt(IWorkSite.WorkType.MINING), "a miner can work at mining sites");
		ItemStack orders = new ItemStack(AWNPCItems.WORK_ORDER.get());
		WorkOrder order = WorkOrder.getWorkOrder(orders);
		check(helper, order.addWorkPosition(level, absSite), "work position is added");
		order.write(orders);
		npc.setItemStackToSlot(NpcBase.ORDER_SLOT, orders);
		helper.succeedWhen(() -> {
			require(npc.getDistanceSq(absSite) < 25, "the worker walks to the site, distance^2 " + npc.getDistanceSq(absSite));
			require(site.workDone > 0, "the worker added energy to the site");
		});
	}

	/* a courier moves the content of one chest to another */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 1200)
	public static void courierExecutesRoutingOrder(GameTestHelper helper) {
		floor(helper);
		ChestBlockEntity source = chest(helper, 2, 12);
		ChestBlockEntity target = chest(helper, 12, 12);
		source.setItem(0, new ItemStack(Items.COBBLESTONE, 32));

		NpcCourier npc = owned(helper, "courier", "", 7, 4);
		npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(AWCoreItems.BACKPACK.get()));
		check(helper, npc.backpackInventory != null, "a courier holding a backpack has a backpack inventory");

		ItemStack orders = new ItemStack(AWNPCItems.ROUTING_ORDER.get());
		RoutingOrder order = RoutingOrder.getRoutingOrder(orders);
		order.addRoutePoint(Direction.UP, source.getBlockPos());
		order.addRoutePoint(Direction.UP, target.getBlockPos());
		setRouteType(order, 0, RoutingOrder.RouteType.WITHDRAW_ALL_EXCEPT);
		setRouteType(order, 1, RoutingOrder.RouteType.DEPOSIT_ALL_EXCEPT);
		check(helper, order.getEntries().get(0).getRouteType() == RoutingOrder.RouteType.WITHDRAW_ALL_EXCEPT, "first point withdraws");
		check(helper, order.getEntries().get(1).getRouteType() == RoutingOrder.RouteType.DEPOSIT_ALL_EXCEPT, "second point deposits");
		order.write(orders);
		npc.setItemStackToSlot(NpcBase.ORDER_SLOT, orders);
		helper.succeedWhen(() -> require(target.countItem(Items.COBBLESTONE) == 32 && source.countItem(Items.COBBLESTONE) == 0,
				"the cobblestone arrives in the target chest, there: " + target.countItem(Items.COBBLESTONE)));
	}

	private static void setRouteType(RoutingOrder order, int index, RoutingOrder.RouteType type) {
		for (int i = 0; i < 20 && order.getEntries().get(index).getRouteType() != type; i++) {
			order.changeRouteType(index, false);
		}
	}

	/* a hungry npc with an upkeep order walks to the chest and eats */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void hungryNpcWithdrawsUpkeep(GameTestHelper helper) {
		floor(helper);
		ChestBlockEntity larder = chest(helper, 12, 12);
		larder.setItem(0, new ItemStack(Items.BREAD, 16));

		NpcWorker npc = owned(helper, "worker", "miner", 2, 2);
		npc.setFoodRemaining(0);
		ItemStack upkeep = new ItemStack(AWNPCItems.UPKEEP_ORDER.get());
		UpkeepOrder order = UpkeepOrder.getUpkeepOrder(upkeep).orElse(null);
		check(helper, order != null, "upkeep order reads from its item");
		check(helper, order.addUpkeepPosition(helper.getLevel(), larder.getBlockPos()), "a chest is a valid upkeep position");
		order.write(upkeep);
		npc.setItemStackToSlot(NpcBase.UPKEEP_SLOT, upkeep);
		check(helper, npc.getUpkeepPoint().isPresent(), "the npc knows its upkeep point");
		helper.succeedWhen(() -> {
			require(npc.getFoodRemaining() > 0, "the npc has eaten, food " + npc.getFoodRemaining());
			require(larder.countItem(Items.BREAD) < 16, "bread was taken from the chest");
		});
	}

	/* a civilian npc flees from a hostile mob */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void workerFleesFromZombie(GameTestHelper helper) {
		floor(helper);
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				helper.setBlock(new BlockPos(x, 6, z), Blocks.STONE);
			}
		}
		NpcWorker npc = owned(helper, "worker", "miner", 5, 7);
		//an npc with a home flees towards it (1.12 NpcAIFleeHostiles), so the home is put away from the zombie
		npc.restrictTo(helper.absolutePos(new BlockPos(1, 2, 7)), 2);
		Zombie zombie = place(helper, EntityType.ZOMBIE.create(helper.getLevel()), 8, 7);
		zombie.setNoAi(true);
		double startDistance = npc.distanceToSqr(zombie);
		helper.succeedWhen(() -> {
			require((npc.getAITasks() & NpcAI.TASK_FLEE) != 0 || npc.distanceToSqr(zombie) > startDistance + 16, "the worker flees, tasks " + npc.getAITasks());
			require(npc.distanceToSqr(zombie) > startDistance, "the worker moved away from the zombie");
		});
	}

	/* the time of day is a property of the whole test level: the night test runs in its own batch */
	@net.minecraft.gametest.framework.BeforeBatch(batch = "npcNight")
	public static void beforeNight(ServerLevel level) {
		level.setDayTime(18000);
	}

	@net.minecraft.gametest.framework.AfterBatch(batch = "npcNight")
	public static void afterNight(ServerLevel level) {
		level.setDayTime(6000);
	}

	/* an npc outside its home range walks back home at night */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800, batch = "npcNight")
	public static void npcReturnsHomeAtNight(GameTestHelper helper) {
		floor(helper);
		NpcWorker npc = owned(helper, "worker", "miner", 13, 13);
		BlockPos home = helper.absolutePos(new BlockPos(3, 2, 3));
		npc.restrictTo(home, 3);

		helper.succeedWhen(() -> require(npc.getDistanceSq(home) <= 16, "the npc walks home, distance^2 " + npc.getDistanceSq(home)));
	}

	/*
	 * Minimal work site (the automation module is not part of this checkout): always has work, counts the energy
	 * workers add. Placed as the block entity of a barrel.
	 */
	public static class TestWorkSite extends BarrelBlockEntity implements IWorkSite {
		public double workDone = 0;
		private Owner owner = Owner.EMPTY;

		public TestWorkSite(BlockPos pos, BlockState state) {
			super(pos, state);
		}

		@Override
		public boolean hasWork() {
			return true;
		}

		@Override
		public void addEnergyFromWorker(IWorker worker) {
			workDone += worker.getWorkEffectiveness(getWorkType());
		}

		@Override
		public void addEnergyFromPlayer(Player player) {
			workDone += 1;
		}

		@Override
		public WorkType getWorkType() {
			return WorkType.MINING;
		}

		@Nullable
		@Override
		public Team getTeam() {
			return null;
		}

		@Override
		public Set<WorksiteUpgrade> getUpgrades() {
			return Collections.emptySet();
		}

		@Override
		public Set<WorksiteUpgrade> getValidUpgrades() {
			return Collections.emptySet();
		}

		@Override
		public void addUpgrade(WorksiteUpgrade upgrade) {
		}

		@Override
		public void removeUpgrade(WorksiteUpgrade upgrade) {
		}

		@Override
		public double getMaxTorque(@Nullable Direction from) {
			return 1000;
		}

		@Override
		public double getTorqueStored(@Nullable Direction from) {
			return workDone;
		}

		@Override
		public double addTorque(@Nullable Direction from, double energy) {
			workDone += energy;
			return energy;
		}

		@Override
		public double drainTorque(Direction from, double energy) {
			return 0;
		}

		@Override
		public double getMaxTorqueOutput(Direction from) {
			return 0;
		}

		@Override
		public double getMaxTorqueInput(@Nullable Direction from) {
			return 1000;
		}

		@Override
		public boolean canOutputTorque(Direction from) {
			return false;
		}

		@Override
		public boolean canInputTorque(Direction from) {
			return true;
		}

		@Override
		public boolean useOutputRotation(@Nullable Direction from) {
			return false;
		}

		@Override
		public float getClientOutputRotation(Direction from, float delta) {
			return 0;
		}

		@Override
		public void onBlockBroken(BlockState state) {
		}

		@Override
		public void setOwner(Player player) {
			owner = new Owner(player);
		}

		@Override
		public void setOwner(Owner owner) {
			this.owner = owner;
		}

		@Override
		public Owner getOwner() {
			return owner;
		}

		@Override
		public boolean isOwner(Player player) {
			return owner.isOwnerOrSameTeamOrFriend(player);
		}
	}

	/* 1.12 ids / stack NBT in the shipped faction trade lists become real 1.21.1 stacks */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void factionTradeListsUseComponents(GameTestHelper helper) {
		java.util.Map<String, net.shadowmage.ancientwarfare.npc.registry.FactionTradeListTemplate> defaults = net.shadowmage.ancientwarfare.npc.registry.FactionTradeListRegistry.getDefaults();
		if (defaults.isEmpty()) {
			helper.fail("no faction trade lists loaded");
		}
		StringBuilder all = new StringBuilder();
		int trades = 0;
		for (net.shadowmage.ancientwarfare.npc.registry.FactionTradeListTemplate template : defaults.values()) {
			trades += template.getTrades().size();
			all.append(template.toTradeList().serializeNBT());
		}
		String data = all.toString();
		if (trades < 100) {
			helper.fail("only " + trades + " faction trades loaded");
		}
		if (!data.contains("_spawn_egg")) {
			helper.fail("no spawn egg outputs in the trade lists");
		}
		if (!data.contains("minecraft:enchantments")) {
			helper.fail("no enchanted outputs in the trade lists");
		}
		if (!data.contains("minecraft:custom_name") || !data.contains("minecraft:potion_contents")) {
			helper.fail("custom names / potions were not translated");
		}
		if (!data.contains("minecraft:dyed_color") || !data.contains("minecraft:banner_patterns")) {
			helper.fail("leather dye colour / banner patterns were not translated");
		}
		if (data.contains(net.shadowmage.ancientwarfare.core.util.parsing.LegacyStackNbt.LEGACY_ENCH_KEY) || data.contains("ench:[")) {
			helper.fail("untranslated legacy enchantment data left in the trade lists");
		}
		helper.succeed();
	}

	/*
	 * a running npc task must not be updated on a tick where its continue condition fails
	 * (1.12 checked the condition every tick; vanilla 1.21.1 only every second tick). The condition is dropped on an
	 * odd and on an even game tick so that one of the two falls on a tick without a vanilla check.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void runningTaskIsNotUpdatedAfterItsConditionFails(GameTestHelper helper) {
		floor(helper);
		boolean[] valid = {true};
		int[] updates = {0};
		int[] updatesWhileInvalid = {0};
		NpcBase npc = owned(helper, "worker", "farmer", 4, 4);
		npc.goalSelector.addGoal(0, new net.shadowmage.ancientwarfare.npc.ai.NpcAI<NpcBase>(npc) {
			@Override
			public boolean canUse() {
				return valid[0];
			}

			@Override
			public void updateTask() {
				updates[0]++;
				if (!valid[0]) {
					updatesWhileInvalid[0]++;
				}
			}
		});
		helper.runAtTickTime(21, () -> valid[0] = false);
		helper.runAtTickTime(25, () -> valid[0] = true);
		helper.runAtTickTime(34, () -> valid[0] = false);
		helper.runAtTickTime(45, () -> {
			check(helper, updates[0] > 10, "test task never ran (" + updates[0] + " updates)");
			check(helper, updatesWhileInvalid[0] == 0, "task was updated " + updatesWhileInvalid[0] + " time(s) after its condition failed");
			helper.succeed();
		});
	}

	/*
	 * Improvement over 1.12 (config improvements.reuse_paths): two workers walk to a work site each, one with the
	 * original behaviour and one that keeps its path. Both must arrive and work; the improved one must have searched
	 * far fewer paths.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 600)
	public static void improvementWalkingNpcKeepsItsPath(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		NpcWorker[] npcs = new NpcWorker[2];
		TestWorkSite[] sites = new TestWorkSite[2];
		for (int i = 0; i < 2; i++) {
			BlockPos sitePos = new BlockPos(14, 2, 3 + i * 9);
			helper.setBlock(sitePos, Blocks.BARREL);
			BlockPos absSite = helper.absolutePos(sitePos);
			sites[i] = new TestWorkSite(absSite, level.getBlockState(absSite));
			level.setBlockEntity(sites[i]);
			npcs[i] = owned(helper, "worker", "miner", 1, 3 + i * 9);
			((net.shadowmage.ancientwarfare.npc.ai.NpcNavigator) npcs[i].getNavigation()).reusePaths = i == 1;
			ItemStack orders = new ItemStack(AWNPCItems.WORK_ORDER.get());
			WorkOrder order = WorkOrder.getWorkOrder(orders);
			check(helper, order.addWorkPosition(level, absSite), "work position is added");
			order.write(orders);
			npcs[i].setItemStackToSlot(NpcBase.ORDER_SLOT, orders);
		}
		helper.succeedWhen(() -> {
			require(sites[0].workDone > 0 && sites[1].workDone > 0, "both workers reached their site and worked");
			int original = ((net.shadowmage.ancientwarfare.npc.ai.NpcNavigator) npcs[0].getNavigation()).pathsCalculated;
			int improved = ((net.shadowmage.ancientwarfare.npc.ai.NpcNavigator) npcs[1].getNavigation()).pathsCalculated;
			check(helper, improved * 4 < original, "the path keeping worker searched " + improved + " paths, the original one " + original);
		});
	}

	/*
	 * Improvement over 1.12 (config improvements.skip_unreachable_worksites): the closest worksite sits on a pillar
	 * out of reach, a second one stands on the ground a little further away. The worker gives the first one up and
	 * works at the second (1.12: it stands below the pillar forever).
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 1200)
	public static void improvementWorkerSkipsUnreachableWorksite(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		TestWorkSite[] sites = new TestWorkSite[2];
		BlockPos[] positions = {new BlockPos(4, 11, 8), new BlockPos(13, 2, 8)};
		for (int y = 2; y < 11; y++) {
			helper.setBlock(new BlockPos(4, y, 8), Blocks.STONE);
		}
		for (int i = 0; i < 2; i++) {
			helper.setBlock(positions[i], Blocks.BARREL);
			BlockPos abs = helper.absolutePos(positions[i]);
			sites[i] = new TestWorkSite(abs, level.getBlockState(abs));
			level.setBlockEntity(sites[i]);
		}
		NpcWorker npc = owned(helper, "worker", "miner", 2, 8);
		helper.succeedWhen(() -> {
			require(sites[1].workDone > 0, "the worker reached the second worksite and worked (work target " + npc.autoWorkTarget + ")");
			check(helper, sites[0].workDone == 0, "the worksite on the pillar cannot have been worked");
		});
	}

	/*
	 * Improvement over 1.12 (config improvements.medic_heals_worst_first): a lightly wounded soldier stands next to
	 * the medic, a badly wounded one further away. The badly wounded one is healed first (1.12: the nearest one).
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 400)
	public static void improvementMedicHealsWorstWoundedFirst(GameTestHelper helper) {
		floor(helper);
		NpcCombat near = owned(helper, "combat", "soldier", 10, 8);
		NpcCombat far = owned(helper, "combat", "soldier", 3, 8);
		near.setNoAi(true);
		far.setNoAi(true);
		float nearHealth = near.getMaxHealth() * 0.75f;
		float farHealth = far.getMaxHealth() * 0.2f;
		near.setHealth(nearHealth);
		far.setHealth(farHealth);
		NpcCombat medic = owned(helper, "combat", "medic", 8, 8);
		check(helper, "medic".equals(medic.getNpcSubType()), "the medic sub type was created");
		helper.succeedWhen(() -> {
			require(far.getHealth() > farHealth, "the badly wounded soldier was healed");
			check(helper, near.getHealth() <= nearHealth, "the lightly wounded soldier next to the medic was healed first (" + near.getHealth() + ")");
		});
	}

	/*
	 * Improvement over 1.12 (config improvements.npc_level_editor): the creative controls screen sets the level of an
	 * npc. The data the screen sends goes to the server container of a creative player; level, attack damage and health
	 * follow. The same data from a survival player, or with the switch off, changes nothing.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 100)
	public static void improvementCreativeLevelEditor(GameTestHelper helper) {
		floor(helper);
		NpcCombat npc = owned(helper, "combat", "soldier", 4, 4);
		npc.setNoAi(true);
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setPos(npc.getX() + 1, npc.getY(), npc.getZ());
		Player survival = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		boolean saved = AWNPCStatics.improvedNpcLevelEditor;
		try {
			AWNPCStatics.improvedNpcLevelEditor = true;
			check(helper, player.isCreative() && !survival.isCreative(), "one creative and one survival player");
			double damage = npc.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
			double health = npc.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
			check(helper, npc.getLevelingStats().getLevel() == 0 && damage > 0, "a new soldier is level 0 and has attack damage");

			//the data the screen sends when its level field holds 4
			net.shadowmage.ancientwarfare.npc.container.ContainerNpcCreativeControls container = openCreativeControls(player, npc);
			check(helper, container.level == 0 && container.maxLevel == AWNPCStatics.maxNpcCombatLevel && "combat.soldier".equals(container.levelType), "the container knows level, maximum and type");
			check(helper, !container.changesTag().contains("setLevel"), "an untouched level field sends no level");
			container.levelToSet = 4;
			CompoundTag tag = container.changesTag();
			check(helper, tag.getInt("setLevel") == 4, "the changed level field sends the level");

			//survival: the container of a survival player, and the application itself
			container.removed(player);
			container = openCreativeControls(survival, npc);
			container.handlePacketData(tag.copy());
			container.removed(survival);
			check(helper, !net.shadowmage.ancientwarfare.npc.container.ContainerNpcCreativeControls.applyLevel(survival, npc, tag), "a survival player cannot apply a level");
			check(helper, npc.getLevelingStats().getLevel() == 0, "data of a survival player changes no level");
			check(helper, npc.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() == damage && npc.getAttribute(Attributes.MAX_HEALTH).getBaseValue() == health, "data of a survival player changes no attribute");

			//switch off: the server ignores the level, and sends none
			AWNPCStatics.improvedNpcLevelEditor = false;
			container = openCreativeControls(player, npc);
			check(helper, container.level == -1, "switch off: the container has no level for the screen");
			container.handlePacketData(tag.copy());
			container.removed(player);
			check(helper, npc.getLevelingStats().getLevel() == 0 && npc.getAttribute(Attributes.MAX_HEALTH).getBaseValue() == health, "switch off: level and health stay");
			AWNPCStatics.improvedNpcLevelEditor = true;

			//creative, switch on
			container = openCreativeControls(player, npc);
			container.handlePacketData(tag.copy());
			check(helper, npc.getLevelingStats().getLevel() == 0, "the level is applied when the screen closes, like the other values");
			container.removed(player);
			check(helper, npc.getLevelingStats().getLevel() == 4 && npc.getLevelingStats().getBaseLevel() == 4, "the level is 4 (" + npc.getLevelingStats().getLevel() + ")");
			check(helper, npc.getLevelingStats().getExperience() == 0, "no experience towards the next level");
			double leveled = damage * (1 + 4 * AWNPCStatics.npcLevelDamageMultiplier);
			double now = npc.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
			check(helper, Math.abs(now - leveled) < 1.0e-6, "attack damage is that of level 4: " + now + ", expected " + leveled);
			check(helper, npc.getAttribute(Attributes.MAX_HEALTH).getBaseValue() == health + 4, "health grew by 4");
			check(helper, openCreativeControls(player, npc).level == 4, "a newly opened screen gets level 4");

			//limits: above the maximum, and back down to 0
			tag.putInt("setLevel", 9999);
			container = openCreativeControls(player, npc);
			container.handlePacketData(tag.copy());
			container.removed(player);
			check(helper, npc.getLevelingStats().getLevel() == AWNPCStatics.maxNpcCombatLevel, "the level stops at the configured maximum");
			tag.putInt("setLevel", -3);
			container = openCreativeControls(player, npc);
			container.handlePacketData(tag.copy());
			container.removed(player);
			check(helper, npc.getLevelingStats().getLevel() == 0, "the level does not go below 0");
			check(helper, Math.abs(npc.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() - damage) < 1.0e-6 && npc.getAttribute(Attributes.MAX_HEALTH).getBaseValue() == health, "level 0 gives the first damage and health again");
		} finally {
			AWNPCStatics.improvedNpcLevelEditor = saved;
			npc.discard();
			helper.getLevel().getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	private static net.shadowmage.ancientwarfare.npc.container.ContainerNpcCreativeControls openCreativeControls(Player player, NpcBase npc) {
		net.shadowmage.ancientwarfare.core.container.ContainerBase container = net.shadowmage.ancientwarfare.core.network.NetworkHandler.createContainerForTest(net.shadowmage.ancientwarfare.core.network.NetworkHandler.GUI_NPC_CREATIVE, player, npc.getId(), 0, 0);
		if (!(container instanceof net.shadowmage.ancientwarfare.npc.container.ContainerNpcCreativeControls)) {
			throw new GameTestAssertException("the creative controls container was not created");
		}
		return (net.shadowmage.ancientwarfare.npc.container.ContainerNpcCreativeControls) container;
	}

	/*
	 * Improvement over 1.12 (config improvements.upkeep_counts_only_taken_food): one bread in a town hall style
	 * inventory (ItemStackHandler) gives the food value of one bread, and the rest of a full larder stays in it.
	 * With the switch off the 1.12 counting error is back: one bread fills the whole food bar.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 100)
	public static void improvementUpkeepCountsOnlyTakenFood(GameTestHelper helper) {
		floor(helper);
		NpcPlayerOwned npc = owned(helper, "worker", "", 8, 8);
		npc.setNoAi(true);
		int bread = net.shadowmage.ancientwarfare.npc.AncientWarfareNPC.statics.getFoodValue(new ItemStack(net.minecraft.world.item.Items.BREAD));
		check(helper, bread > 0 && bread < npc.getUpkeepAmount(), "one bread is less than an upkeep ration (" + bread + " of " + npc.getUpkeepAmount() + ")");
		boolean saved = AWNPCStatics.improvedUpkeepCounting;
		try {
			AWNPCStatics.improvedUpkeepCounting = true;
			net.neoforged.neoforge.items.ItemStackHandler larder = new net.neoforged.neoforge.items.ItemStackHandler(3);
			larder.setStackInSlot(1, new ItemStack(net.minecraft.world.item.Items.BREAD));
			npc.setFoodRemaining(0);
			check(helper, !npc.withdrawFood(larder), "one bread does not fill the npc");
			check(helper, npc.getFoodRemaining() == bread, "one bread gives the food value of one bread (" + npc.getFoodRemaining() + ")");
			check(helper, larder.getStackInSlot(1).isEmpty(), "the bread was taken");

			larder.setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.BREAD, 64));
			larder.setStackInSlot(2, new ItemStack(net.minecraft.world.item.Items.BREAD, 64));
			npc.setFoodRemaining(0);
			check(helper, npc.withdrawFood(larder), "a full larder fills the npc");
			int taken = 128 - larder.getStackInSlot(0).getCount() - larder.getStackInSlot(2).getCount();
			check(helper, taken == (npc.getUpkeepAmount() + bread - 1) / bread, "exactly the needed bread was taken (" + taken + ")");
			check(helper, npc.getFoodRemaining() == taken * bread, "the food bar matches the bread taken");

			AWNPCStatics.improvedUpkeepCounting = false;
			larder = new net.neoforged.neoforge.items.ItemStackHandler(3);
			larder.setStackInSlot(1, new ItemStack(net.minecraft.world.item.Items.BREAD));
			npc.setFoodRemaining(0);
			check(helper, npc.withdrawFood(larder), "switch off: 1.12 counting, one bread fills the npc");
		} finally {
			AWNPCStatics.improvedUpkeepCounting = saved;
		}
		helper.succeed();
	}

	/*
	 * Torque shafts are obstacles for NPC path finding, as in 1.12 (Block.isPassable of a rock block), although their
	 * collision shape is thin. A row of shafts lies on a ledge between the NPC and its target, one block above the
	 * lower ground behind it; the path has to go around the row, not through a shaft. (1.21 alone would call the
	 * shafts open space; NPCs then walked into them and bunched up there.)
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 100)
	public static void npcPathGoesAroundTorqueShafts(GameTestHelper helper) {
		floor(helper);
		//upper level: one more layer of stone for z <= 8, so z = 9.. is one block down
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z <= 8; z++) {
				helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
			}
		}
		net.minecraft.world.level.block.Block shaft = net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks.TORQUE_SHAFT.get();
		for (int x = 3; x <= 12; x++) {
			helper.setBlock(new BlockPos(x, 3, 8), shaft);
		}
		for (net.minecraft.world.level.block.Block block : new net.minecraft.world.level.block.Block[] {shaft,
				net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks.TORQUE_JUNCTION.get(), net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks.TORQUE_DISTRIBUTOR.get(),
				net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks.HAND_CRANKED_GENERATOR.get(), net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks.FLYWHEEL_CONTROLLER.get(),
				net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks.FLYWHEEL_STORAGE.get()}) {
			check(helper, !block.defaultBlockState().isPathfindable(net.minecraft.world.level.pathfinder.PathComputationType.LAND), block + " counts as open space for path finding");
		}
		NpcBase npc = AWNPCEntities.createNpc(helper.getLevel(), "worker", "", "");
		npc.setOwnerName(OWNER);
		npc.setFoodRemaining(6000);
		BlockPos start = helper.absolutePos(new BlockPos(8, 3, 4));
		npc.moveTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0, 0);
		helper.getLevel().addFreshEntity(npc);
		BlockPos target = helper.absolutePos(new BlockPos(8, 2, 12));
		//the path is asked for once the NPC stands on the ground (a path needs that)
		helper.runAfterDelay(10, () -> {
			net.minecraft.world.level.pathfinder.Path path = npc.getNavigation().createPath(target, 1);
			check(helper, path != null && path.getNodeCount() > 1, "no path from the ledge (npc at " + npc.blockPosition() + ", on ground " + npc.onGround() + ")");
			BlockPos end = path.getEndNode().asBlockPos();
			check(helper, end.distManhattan(target) <= 2, "the path ends at " + end + ", not at the target " + target + " behind the shafts");
			for (int i = 0; i < path.getNodeCount(); i++) {
				BlockPos node = path.getNode(i).asBlockPos();
				check(helper, !helper.getLevel().getBlockState(node).is(shaft) && !helper.getLevel().getBlockState(node.above()).is(shaft), "the path goes through a torque shaft at " + node);
			}
			npc.discard();
			helper.succeed();
		});
	}


	/*
	 * Big modpack audit: 1.12 TileTownHall.getCapability gave the 27 slot inventory on every face and without a face
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE)
	public static void townHallExposesItsInventory(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, net.shadowmage.ancientwarfare.npc.init.AWNPCBlocks.TOWN_HALL.get());
		net.shadowmage.ancientwarfare.npc.tile.TileTownHall townHall = helper.getBlockEntity(pos);
		for (Direction side : Direction.values()) {
			check(helper, helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), side) == townHall.getInventory(), "town hall inventory on face " + side);
		}
		net.neoforged.neoforge.items.IItemHandler handler = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
		check(helper, handler == townHall.getInventory() && handler.getSlots() == 27, "town hall inventory without a face, 27 slots");
		check(helper, handler.insertItem(3, new ItemStack(Items.BREAD, 7), true).isEmpty() && handler.getStackInSlot(3).isEmpty(), "a simulated insert changes nothing");
		check(helper, handler.insertItem(3, new ItemStack(Items.BREAD, 7), false).isEmpty() && handler.extractItem(3, 2, true).getCount() == 2 && handler.getStackInSlot(3).getCount() == 7, "insert, then a simulated extract of 2 leaves 7");
		check(helper, handler.extractItem(3, 64, false).getCount() == 7, "an extract of more than is there gives the 7");
		helper.succeed();
	}

	/*
	 * Mounted NPCs sit where 1.12 put them (getYOffset -0.35 for every NPC = the 1.21 player vehicle attachment 0.6).
	 * Faction NPCs always carry a size override from the faction table; the override lost the attachment and they sat
	 * 0.6 above the saddle. Checked for a faction cavalry with a size override, an owned combat NPC, and the cavalry
	 * after a save and reload of the horse with its passenger (what a world reload or a structure's saved horse does).
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 200)
	public static void mountedNpcsSitOnTheSaddle(GameTestHelper helper) {
		floor(helper);
		String faction = null;
		for (String f : FactionRegistry.getFactionNames()) {
			if (NpcDefaultsRegistry.getFactionNpcDefault(f, "cavalry").isEnabled()) {
				faction = f;
				break;
			}
		}
		check(helper, faction != null, "a faction has cavalry");
		NpcBase cavalry = place(helper, AWNPCEntities.createNpc(helper.getLevel(), "faction.cavalry", "", faction), 3, 3);
		NpcBase owned = owned(helper, "combat", "soldier", 12, 12);
		net.minecraft.world.entity.animal.horse.Horse horseA = place(helper, EntityType.HORSE.create(helper.getLevel()), 3, 3);
		net.minecraft.world.entity.animal.horse.Horse horseB = place(helper, EntityType.HORSE.create(helper.getLevel()), 12, 12);
		//tamed like the horses of structures and of NpcAIFactionRideHorse (an untamed horse's RunAroundLikeCrazyGoal throws off any rider)
		horseA.setTamed(true);
		horseB.setTamed(true);
		check(helper, cavalry.startRiding(horseA, true) && owned.startRiding(horseB, true), "both npcs mount");
		float height = cavalry.getBbHeight();
		check(helper, cavalry.getVehicleAttachmentPoint(horseA).y == Player.DEFAULT_VEHICLE_ATTACHMENT.y, "faction npc with the size override (height " + height + ") has the 0.6 vehicle attachment, got " + cavalry.getVehicleAttachmentPoint(horseA).y);
		check(helper, owned.getVehicleAttachmentPoint(horseB).y == Player.DEFAULT_VEHICLE_ATTACHMENT.y, "owned npc has the 0.6 vehicle attachment");
		helper.runAfterDelay(5, () -> {
			requireSeated(cavalry, horseA, "faction cavalry");
			requireSeated(owned, horseB, "owned combat npc");
			//save the horse with its rider and load both again
			CompoundTag tag = new CompoundTag();
			check(helper, horseA.save(tag) && tag.contains("Passengers"), "the horse saves its rider");
			cavalry.discard();
			horseA.discard();
			Entity loaded = EntityType.loadEntityRecursive(tag, helper.getLevel(), e -> e);
			check(helper, loaded != null && helper.getLevel().tryAddFreshEntityWithPassengers(loaded), "horse and rider load again");
			check(helper, loaded.getFirstPassenger() instanceof NpcFaction, "the loaded horse carries the faction npc");
			NpcBase reloaded = (NpcBase) loaded.getFirstPassenger();
			check(helper, Math.abs(reloaded.getBbHeight() - height) < 1e-4, "reloaded size " + reloaded.getBbHeight() + " = saved " + height);
			helper.runAfterDelay(5, () -> {
				requireSeated(reloaded, loaded, "reloaded faction cavalry");
				helper.succeed();
			});
		});
	}

	private static void requireSeated(NpcBase npc, Entity horse, String what) {
		require(npc.getVehicle() == horse, what + " still rides its horse");
		double expected = horse.getPassengerRidingPosition(npc).y - Player.DEFAULT_VEHICLE_ATTACHMENT.y;
		require(Math.abs(npc.getY() - expected) < 1e-3, what + " sits at y " + npc.getY() + ", expected " + expected + " (saddle point - 0.6)");
	}
}
