package net.shadowmage.ancientwarfare.npc.init;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.core.entity.AWEntityRegistry;
import net.shadowmage.ancientwarfare.core.init.AWCoreItems;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;
import net.shadowmage.ancientwarfare.npc.entity.NpcBard;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.NpcCombat;
import net.shadowmage.ancientwarfare.npc.entity.NpcCourier;
import net.shadowmage.ancientwarfare.npc.entity.NpcPriest;
import net.shadowmage.ancientwarfare.npc.entity.NpcTrader;
import net.shadowmage.ancientwarfare.npc.entity.NpcWorker;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionArcher;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionArcherElite;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionBard;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionCivilianFemale;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionCivilianMale;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionLeader;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionLeaderElite;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionMountedArcher;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionMountedSoldier;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionPriest;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionSiegeEngineer;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionSoldier;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionSoldierElite;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionSpellcaster;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionTrader;
import net.shadowmage.ancientwarfare.npc.entity.vehicle.NpcSiegeEngineer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/*
 * Port notes: 1.12 EntityRegistry.registerModEntity through AWEntityRegistry.EntityDeclaration -> a
 * DeferredRegister<EntityType<?>>; the registry paths are the 1.12 entity names (AWEntityRegistry constants, e.g.
 * "aw_npc_worker", "faction.archer") in the "ancientwarfare" namespace. Tracking range 120 blocks -> 8 chunks,
 * update frequency 3, velocity updates on (1.21 default). Size 0.6 x 1.8 (1.12 EntityLiving default); faction NPCs
 * rescale themselves from the registry height. Attributes are registered with EntityAttributeCreationEvent.
 * The faction spellcaster is always the plain NpcFactionSpellcaster (Electroblob's Wizardry integration dropped).
 * NpcFactionDeclaration no longer creates through reflection but through the constructor reference it is given.
 */
public class AWNPCEntities {
	private AWNPCEntities() {}

	private static final String COMBAT_TYPE = "combat";
	private static final String SOLDIER_SUBTYPE = "soldier";
	private static final String COMMANDER_SUBTYPE = "commander";
	private static final String ARCHER_SUBTYPE = "archer";
	private static final String WORKER_TYPE = "worker";
	private static final String MINER_SUBTYPE = "miner";
	private static final String SPELLCASTER_SUBTYPE = "spellcaster";

	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, AncientWarfareNPC.MOD_ID);

	/*
	 * Npc base type -> NpcDeclaration<br>
	 * Used to retrieve declaration for creating entities<br>
	 * NpcDeclaration also stores information pertaining to npc-sub-type basic setup
	 */
	private static final HashMap<String, NpcDeclaration> npcMap = new HashMap<>();

	static {
		addPlayerOwnedNpcs();
		addFaction();
	}

	public static void register(IEventBus modBus) {
		ENTITIES.register(modBus);
		modBus.addListener(AWNPCEntities::registerAttributes);
	}

	private static void registerAttributes(EntityAttributeCreationEvent event) {
		for (NpcDeclaration declaration : npcMap.values()) {
			event.put(declaration.getEntityType(), declaration.attributes.get().build());
		}
	}

	private static void addPlayerOwnedNpcs() {
		NpcDeclaration reg = new NpcDeclaration(NpcCombat::new, AWEntityRegistry.NPC_COMBAT, COMBAT_TYPE, SOLDIER_SUBTYPE);
		reg.addSubTypes(COMMANDER_SUBTYPE, SOLDIER_SUBTYPE, ARCHER_SUBTYPE, "medic", "engineer");
		addNpcRegistration(reg);

		reg = new NpcDeclaration(NpcWorker::new, AWEntityRegistry.NPC_WORKER, WORKER_TYPE, MINER_SUBTYPE);
		reg.addSubTypes(MINER_SUBTYPE, "farmer", "lumberjack", "researcher", "craftsman");
		addNpcRegistration(reg);

		reg = new NpcDeclaration(NpcCourier::new, AWEntityRegistry.NPC_COURIER, "courier");
		addNpcRegistration(reg);

		reg = new NpcDeclaration(NpcTrader::new, AWEntityRegistry.NPC_TRADER, "trader");
		addNpcRegistration(reg);

		reg = new NpcDeclaration(NpcPriest::new, AWEntityRegistry.NPC_PRIEST, "priest");
		addNpcRegistration(reg);

		reg = new NpcDeclaration(NpcBard::new, AWEntityRegistry.NPC_BARD, "bard");
		addNpcRegistration(reg);

		reg = new NpcDeclaration(NpcSiegeEngineer::new, AWEntityRegistry.NPC_SIEGE_ENGINEER, "siege_engineer");
		reg.attributes = NpcSiegeEngineer::createAttributes;
		addNpcRegistration(reg);
	}

	private static void addFaction() {
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionArcher>(NpcFactionArcher::new, NpcFactionArcher::new, AWEntityRegistry.NPC_FACTION_ARCHER, ARCHER_SUBTYPE));
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionSoldier>(NpcFactionSoldier::new, NpcFactionSoldier::new, AWEntityRegistry.NPC_FACTION_SOLDIER, SOLDIER_SUBTYPE));
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionLeader>(NpcFactionLeader::new, NpcFactionLeader::new, AWEntityRegistry.NPC_FACTION_COMMANDER, COMMANDER_SUBTYPE));
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionPriest>(NpcFactionPriest::new, NpcFactionPriest::new, AWEntityRegistry.NPC_FACTION_PRIEST, "priest"));
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionTrader>(NpcFactionTrader::new, NpcFactionTrader::new, AWEntityRegistry.NPC_FACTION_TRADER, "trader"));
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionMountedSoldier>(NpcFactionMountedSoldier::new, NpcFactionMountedSoldier::new, AWEntityRegistry.NPC_FACTION_CAVALRY, SOLDIER_SUBTYPE));
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionMountedArcher>(NpcFactionMountedArcher::new, NpcFactionMountedArcher::new, AWEntityRegistry.NPC_FACTION_MOUNTED_ARCHER, ARCHER_SUBTYPE));
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionCivilianMale>(NpcFactionCivilianMale::new, NpcFactionCivilianMale::new, AWEntityRegistry.NPC_FACTION_CIVILIAN_MALE, "civilian_male"));
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionCivilianFemale>(NpcFactionCivilianFemale::new, NpcFactionCivilianFemale::new, AWEntityRegistry.NPC_FACTION_CIVILIAN_FEMALE, "civilian_female"));
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionArcherElite>(NpcFactionArcherElite::new, NpcFactionArcherElite::new, AWEntityRegistry.NPC_FACTION_ARCHER_ELITE, ARCHER_SUBTYPE));
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionSoldierElite>(NpcFactionSoldierElite::new, NpcFactionSoldierElite::new, AWEntityRegistry.NPC_FACTION_SOLDIER_ELITE, SOLDIER_SUBTYPE));
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionLeaderElite>(NpcFactionLeaderElite::new, NpcFactionLeaderElite::new, AWEntityRegistry.NPC_FACTION_LEADER_ELITE, COMMANDER_SUBTYPE));
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionBard>(NpcFactionBard::new, NpcFactionBard::new, AWEntityRegistry.NPC_FACTION_BARD, "bard"));
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionSiegeEngineer>(NpcFactionSiegeEngineer::new, NpcFactionSiegeEngineer::new, AWEntityRegistry.NPC_FACTION_SIEGE_ENGINEER, "siege_engineer"));

		registerSpellcasterFactionNpc();
	}

	private static void registerSpellcasterFactionNpc() {
		addNpcRegistration(new NpcFactionDeclaration<NpcFactionSpellcaster>(NpcFactionSpellcaster::new, NpcFactionSpellcaster::new, AWEntityRegistry.NPC_FACTION_SPELLCASTER, SPELLCASTER_SUBTYPE));
	}

	/*
	 * has to be called during post-init so that all items/etc are fully initialized
	 */
	public static void loadNpcSubtypeEquipment() {
		addNpcSubtypeEquipment(WORKER_TYPE, "farmer", new ItemStack(Items.IRON_HOE));
		addNpcSubtypeEquipment(WORKER_TYPE, MINER_SUBTYPE, new ItemStack(Items.IRON_PICKAXE));
		addNpcSubtypeEquipment(WORKER_TYPE, "lumberjack", new ItemStack(Items.IRON_AXE));
		addNpcSubtypeEquipment(WORKER_TYPE, "researcher", new ItemStack(AWCoreItems.IRON_QUILL.get()));
		addNpcSubtypeEquipment(WORKER_TYPE, "craftsman", new ItemStack(AWCoreItems.IRON_HAMMER.get()));

		addNpcSubtypeEquipment(COMBAT_TYPE, COMMANDER_SUBTYPE, new ItemStack(AWNPCItems.IRON_COMMAND_BATON.get()));
		addNpcSubtypeEquipment(COMBAT_TYPE, SOLDIER_SUBTYPE, new ItemStack(Items.IRON_SWORD));
		addNpcSubtypeEquipment(COMBAT_TYPE, ARCHER_SUBTYPE, new ItemStack(Items.BOW));
		addNpcSubtypeEquipment(COMBAT_TYPE, "engineer", new ItemStack(AWCoreItems.IRON_HAMMER.get()));
		addNpcSubtypeEquipment(COMBAT_TYPE, "medic", new ItemStack(Items.IRON_AXE));
	}

	private static void addNpcRegistration(NpcDeclaration reg) {
		getNpcMap().put(reg.getNpcType(), reg);
	}

	public static NpcBase createNpc(Level world, String npcType, String npcSubtype, String faction) {
		if (!getNpcMap().containsKey(npcType)) {
			return null;
		}
		NpcDeclaration reg = getNpcMap().get(npcType);
		return reg.createEntity(world, npcSubtype, faction);
	}

	private static void addNpcSubtypeEquipment(String npcType, String npcSubtype, ItemStack equipment) {
		if (!getNpcMap().containsKey(npcType)) {
			throw new IllegalArgumentException("npc type must first be mapped");
		}
		NpcDeclaration reg = getNpcMap().get(npcType);
		reg.addSubtypeEquipment(npcSubtype, equipment);
	}

	public static Map<String, NpcDeclaration> getNpcMap() {
		return npcMap;
	}

	public static NpcDeclaration getNpcDeclaration(String npcType) {
		return npcMap.get(npcType);
	}

	public static EntityType<? extends NpcBase> getEntityType(String npcType) {
		NpcDeclaration declaration = npcMap.get(npcType);
		return declaration == null ? null : declaration.getEntityType();
	}

	public static class NpcDeclaration {

		private final String itemModelVariant;
		private boolean spawnBaseEntity = true;
		private final String npcType;
		private final String entityName;
		private final HashMap<String, ItemStack> spawnEquipment = new HashMap<>();
		private final DeferredHolder<EntityType<?>, ? extends EntityType<? extends NpcBase>> holder;
		Supplier<AttributeSupplier.Builder> attributes = NpcBase::createAttributes;

		public <T extends NpcBase> NpcDeclaration(EntityType.EntityFactory<T> factory, String entityName, String npcType) {
			this(factory, entityName, npcType, npcType.replace(".", "_"));
		}

		public <T extends NpcBase> NpcDeclaration(EntityType.EntityFactory<T> factory, String entityName, String npcType, String itemModelVariant) {
			this.entityName = entityName;
			this.npcType = npcType;
			this.itemModelVariant = itemModelVariant;
			this.holder = ENTITIES.register(entityName, () -> EntityType.Builder.of(factory, MobCategory.MISC)
					.vehicleAttachment(net.minecraft.world.entity.player.Player.DEFAULT_VEHICLE_ATTACHMENT)
					.sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3).build(entityName));
		}

		/* registry path of the entity type (1.12 entity name) */
		public String name() {
			return entityName;
		}

		public EntityType<? extends NpcBase> getEntityType() {
			return holder.get();
		}

		public String getFaction() {
			return "";
		}

		private void addSubTypes(String... subTypes) {
			Arrays.stream(subTypes).forEach(s -> addSubtypeEquipment(s, ItemStack.EMPTY));
		}

		private void addSubtypeEquipment(String type, ItemStack equipment) {
			spawnEquipment.put(type, equipment);
		}

		public NpcBase createEntity(Level world, String subType, String factionName) {
			NpcBase npc = getEntityType().create(world);
			if (npc != null && !subType.isEmpty()) {
				ItemStack stack = spawnEquipment.get(subType);
				if (stack != null && !stack.isEmpty()) {
					npc.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
				}
			}
			return npc;
		}

		public List<String> getItemModelVariants() {
			return new ImmutableList.Builder<String>().addAll(spawnEquipment.keySet()).add(itemModelVariant).build();
		}

		public String getItemModelVariant(String npcSubType) {
			if (npcSubType.isEmpty()) {
				return itemModelVariant;
			}
			return npcSubType;
		}

		public Set<String> getSubTypes() {
			return spawnEquipment.keySet();
		}

		public boolean canSpawnBaseEntity() {
			return spawnBaseEntity;
		}

		public String getNpcType() {
			return npcType;
		}
	}

	public interface FactionNpcFactory<T extends NpcFaction> {
		T create(EntityType<T> type, Level world, String factionName);
	}

	public static class NpcFactionDeclaration<T extends NpcFaction> extends NpcDeclaration {
		private final FactionNpcFactory<T> factionFactory;

		private NpcFactionDeclaration(EntityType.EntityFactory<T> factory, FactionNpcFactory<T> factionFactory, String entityName, String itemModelVariant) {
			super(factory, entityName, entityName, itemModelVariant);
			this.factionFactory = factionFactory;
			this.attributes = NpcFaction::createAttributes;
		}

		@SuppressWarnings("unchecked")
		@Override
		public NpcFaction createEntity(Level world, String subType, String factionName) {
			try {
				return factionFactory.create((EntityType<T>) getEntityType(), world, factionName);
			}
			catch (Exception e) {
				AncientWarfareNPC.LOG.error("Couldn't create entity:" + e.getMessage());
			}
			return null;
		}
	}
}
