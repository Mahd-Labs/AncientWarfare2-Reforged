package net.shadowmage.ancientwarfare.core.entity;

/*
 * Port notes: 1.12 registered entities through EntityRegistry.registerModEntity with an EntityDeclaration.
 * In 1.21.1 every module registers its EntityTypes with a DeferredRegister (see PORTING.md), so registerEntity and
 * EntityDeclaration are gone; this class keeps the shared entity name constants (registry paths may contain dots).
 */
public class AWEntityRegistry {
	private AWEntityRegistry() {}

	/*
	 * NPC Module Registrations
	 */
	public static final String NPC_WORKER = "aw_npc_worker";
	public static final String NPC_COMBAT = "aw_npc_combat";
	public static final String NPC_COURIER = "aw_npc_courier";
	public static final String NPC_TRADER = "aw_npc_trader";
	public static final String NPC_PRIEST = "aw_npc_priest";
	public static final String NPC_BARD = "aw_npc_bard";
	public static final String NPC_SIEGE_ENGINEER = "aw_npc_siege_engineer";

	public static final String NPC_FACTION_ARCHER = "faction.archer";
	public static final String NPC_FACTION_SOLDIER = "faction.soldier";
	public static final String NPC_FACTION_PRIEST = "faction.priest";
	public static final String NPC_FACTION_TRADER = "faction.trader";
	public static final String NPC_FACTION_COMMANDER = "faction.leader";
	public static final String NPC_FACTION_CAVALRY = "faction.cavalry";
	public static final String NPC_FACTION_MOUNTED_ARCHER = "faction.mounted_archer";
	public static final String NPC_FACTION_CIVILIAN_MALE = "faction.civilian.male";
	public static final String NPC_FACTION_ARCHER_ELITE = "faction.archer.elite";
	public static final String NPC_FACTION_SOLDIER_ELITE = "faction.soldier.elite";
	public static final String NPC_FACTION_LEADER_ELITE = "faction.leader.elite";
	public static final String NPC_FACTION_CIVILIAN_FEMALE = "faction.civilian.female";
	public static final String NPC_FACTION_BARD = "faction.bard";
	public static final String NPC_FACTION_SIEGE_ENGINEER = "faction.siege_engineer";
	public static final String NPC_FACTION_SPELLCASTER = "faction.spellcaster";

	/*
	 * Vehicle Module Registrations
	 */
	public static final String VEHICLE = "vehicle";
	public static final String MISSILE = "missile";

	/*
	 * Structure Module Registrations
	 */
	public static final String AW_GATES = "aw_gate";
}
