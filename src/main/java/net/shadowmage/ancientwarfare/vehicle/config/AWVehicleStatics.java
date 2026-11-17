package net.shadowmage.ancientwarfare.vehicle.config;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.shadowmage.ancientwarfare.core.config.ModConfiguration;
import net.shadowmage.ancientwarfare.vehicle.registry.VehicleRegistry;

/*
 * Port notes (generated from the 1.12 class by a one-off script, then edited):
 * - 1.12 used the Forge @Config annotation system (one .cfg with three categories). Port: COMMON spec
 *   config/ancientwarfare-vehicle.toml with "general_settings" and "vehicle_stats_settings", CLIENT spec
 *   config/ancientwarfare-vehicle-client.toml with "client_side_settings". Option names, defaults and ranges are the
 *   1.12 ones; the static holders generalSettings / clientSettings / vehicleStats keep their field names.
 * - adjust_missiles_for_accuracy was listed under the client settings in 1.12 but is read by the server when a
 *   missile is spawned, so it is in the COMMON spec (general_settings).
 * - 1.12 read vehicle health / ammo damage in the constructors of the vehicle and ammo types (config was loaded
 *   before registration). Here the config loads after registration, so loadValues() pushes the values into the
 *   types through VehicleRegistry.applyConfig().
 * - Options of dropped content are not ported: vehicles_tear_up_grass (driving), battering_ram_* , the boat,
 *   battering ram and chest cart health values, and the unused enableMouseAim.
 * - The KEY_VEHICLE_FORWARD / REVERSE key names are dropped with driving (LEFT / RIGHT turn a stand in place).
 */
public class AWVehicleStatics extends ModConfiguration {
	public static final String KEY_VEHICLE_LEFT = "keybind.vehicle.left";
	public static final String KEY_VEHICLE_RIGHT = "keybind.vehicle.right";
	public static final String KEY_VEHICLE_FIRE = "keybind.vehicle.fire";
	public static final String KEY_VEHICLE_ASCEND_AIM_UP = "keybind.vehicle.ascend.aim.up";
	public static final String KEY_VEHICLE_DESCEND_AIM_DOWN = "keybind.vehicle.descend.aim.down";
	public static final String KEY_VEHICLE_AMMO_PREV = "keybind.vehicle.ammo.prev";
	public static final String KEY_VEHICLE_AMMO_NEXT = "keybind.vehicle.ammo.next";
	public static final String KEY_VEHICLE_TURRET_LEFT = "keybind.vehicle.turret.left";
	public static final String KEY_VEHICLE_TURRET_RIGHT = "keybind.vehicle.turret.right";
	public static final String KEY_VEHICLE_MOUSE_AIM = "keybind.vehicle.mouse.aim";
	public static final String KEY_VEHICLE_AMMO_SELECT = "keybind.vehicle.ammo.select";

	public static final String ClientAndServerSideNote = "Affect both client and server. These configs must match for client and server, or strange and probably BAD things WILL happen.";

	public static GeneralSettings generalSettings = new GeneralSettings();
	public static ClientSettings clientSettings = new ClientSettings();
	public static VehicleStats vehicleStats = new VehicleStats();

	public static class GeneralSettings {
		public boolean oversizeAmmoEnabled = true;
		public boolean ownedSoldiersUseAmmo = true;
		public boolean shotsDestroysBlocks = true;
		public boolean blockFires = true;
		public boolean allowFriendlyFire = false;
		public boolean useVehicleSetupTime = true;
		public boolean adjustMissilesForAccuracy = true;
	}

	/* improvements over 1.12 (not baseline); each can be switched back to the original behaviour */
	public static class Improvements {
		public boolean ammoFallback = true;
		public double missileHitMargin = 0.5;
		public boolean missileHitSweep = true;
		public boolean crewLeadsTargets = true;
		public double crewAccuracyPerLevel = 0.067;
		public double crewAccuracyMaxReduction = 0.5;
	}

	public static final Improvements improvements = new Improvements();

	public static class ClientSettings {
		public boolean renderVehiclesInFirstPerson = true;
		public boolean renderVehicleNameplates = true;
		public boolean renderVehicleNameplateHealth = true;
		public boolean renderOverlay = true;
		public boolean renderAdvOverlay = true;
	}

	public static class VehicleStats {
		public int ammoBallistaBoltDamage = 18;
		public int ammoBallistaBoltIronDamage = 30;
		public int ammoBallistaBoltFlameDamage = 16;
		public int ammoBallistaBoltExplosiveDamage = 15;
		public int ammoCannonBall5kgDamage = 10;
		public int ammoCannonBall10kgDamage = 15;
		public int ammoCannonBall15kgDamage = 30;
		public int ammoCannonBall25kgDamage = 45;
		public int ammoCanisterDamage = 8;
		public int ammoHwachaRocketDamage = 6;
		public int ammoHwachaRocketFlameDamage = 5;
		public int ammoHwachaRocketExplosiveDamage = 4;
		public int vehicleBallistaHealth = 100;
		public int vehicleCatapultHealth = 100;
		public int vehicleCannonHealth = 100;
		public int vehicleHwachaHealth = 100;
		public int vehicleTrebuchetHealth = 100;
		public int vehicleGiantTrebuchetHealth = 175;
	}

	private ModConfigSpec.BooleanValue ammoFallbackValue;
	private ModConfigSpec.DoubleValue missileHitMarginValue;
	private ModConfigSpec.BooleanValue missileHitSweepValue;
	private ModConfigSpec.BooleanValue crewLeadsTargetsValue;
	private ModConfigSpec.DoubleValue crewAccuracyPerLevelValue;
	private ModConfigSpec.DoubleValue crewAccuracyMaxReductionValue;
	private ModConfigSpec.BooleanValue oversizeAmmoEnabledValue;
	private ModConfigSpec.BooleanValue ownedSoldiersUseAmmoValue;
	private ModConfigSpec.BooleanValue shotsDestroysBlocksValue;
	private ModConfigSpec.BooleanValue blockFiresValue;
	private ModConfigSpec.BooleanValue allowFriendlyFireValue;
	private ModConfigSpec.BooleanValue useVehicleSetupTimeValue;
	private ModConfigSpec.BooleanValue adjustMissilesForAccuracyValue;
	private ModConfigSpec.IntValue ammoBallistaBoltDamageValue;
	private ModConfigSpec.IntValue ammoBallistaBoltIronDamageValue;
	private ModConfigSpec.IntValue ammoBallistaBoltFlameDamageValue;
	private ModConfigSpec.IntValue ammoBallistaBoltExplosiveDamageValue;
	private ModConfigSpec.IntValue ammoCannonBall5kgDamageValue;
	private ModConfigSpec.IntValue ammoCannonBall10kgDamageValue;
	private ModConfigSpec.IntValue ammoCannonBall15kgDamageValue;
	private ModConfigSpec.IntValue ammoCannonBall25kgDamageValue;
	private ModConfigSpec.IntValue ammoCanisterDamageValue;
	private ModConfigSpec.IntValue ammoHwachaRocketDamageValue;
	private ModConfigSpec.IntValue ammoHwachaRocketFlameDamageValue;
	private ModConfigSpec.IntValue ammoHwachaRocketExplosiveDamageValue;
	private ModConfigSpec.IntValue vehicleBallistaHealthValue;
	private ModConfigSpec.IntValue vehicleCatapultHealthValue;
	private ModConfigSpec.IntValue vehicleCannonHealthValue;
	private ModConfigSpec.IntValue vehicleHwachaHealthValue;
	private ModConfigSpec.IntValue vehicleTrebuchetHealthValue;
	private ModConfigSpec.IntValue vehicleGiantTrebuchetHealthValue;

	private static final ModConfigSpec CLIENT_SPEC;
	private static final ModConfigSpec.BooleanValue renderVehiclesInFirstPersonValue;
	private static final ModConfigSpec.BooleanValue renderVehicleNameplatesValue;
	private static final ModConfigSpec.BooleanValue renderVehicleNameplateHealthValue;
	private static final ModConfigSpec.BooleanValue renderOverlayValue;
	private static final ModConfigSpec.BooleanValue renderAdvOverlayValue;
	private static final String CLIENT_FILE_NAME = "ancientwarfare-vehicle-client.toml";

	static {
		ModConfigSpec.Builder client = new ModConfigSpec.Builder();
		client.comment("Affect only client-side operations. Server admins can ignore these settings.").push("client_side_settings");
		renderVehiclesInFirstPersonValue = client.comment("Determines whether a vehicle should be rendered in first person view while a player rides it.").define("render_vehicles_in_first_person", true);
		renderVehicleNameplatesValue = client.comment("Renders the name plates above the vehicles").define("render_vehicle_nameplates", true);
		renderVehicleNameplateHealthValue = client.comment("Determines whether to render the vehicle health in the name plate or not, requires render_vehicle_nameplates.").define("render_vehicle_nameplate_health", true);
		renderOverlayValue = client.comment("Determines whether to show the vehicle info overlay while driving a vehicle.").define("render_overlay", true);
		renderAdvOverlayValue = client.comment("Determines whether to show the advanced vehicle info overlay while driving a vehicle.").define("render_adv_overlay", true);
		client.pop();
		CLIENT_SPEC = client.build();
	}

	public AWVehicleStatics(String mod) {
		super(mod);
	}

	@Override
	public void register(IEventBus modBus, ModContainer container) {
		super.register(modBus, container);
		container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, CLIENT_FILE_NAME);
		modBus.addListener((ModConfigEvent.Loading evt) -> onClientConfigEvent(evt));
		modBus.addListener((ModConfigEvent.Reloading evt) -> onClientConfigEvent(evt));
	}

	private static void onClientConfigEvent(ModConfigEvent evt) {
		if (evt.getConfig().getSpec() == CLIENT_SPEC) {
			clientSettings.renderVehiclesInFirstPerson = renderVehiclesInFirstPersonValue.get();
			clientSettings.renderVehicleNameplates = renderVehicleNameplatesValue.get();
			clientSettings.renderVehicleNameplateHealth = renderVehicleNameplateHealthValue.get();
			clientSettings.renderOverlay = renderOverlayValue.get();
			clientSettings.renderAdvOverlay = renderAdvOverlayValue.get();
		}
	}

	@Override
	public void initializeCategories() {
	}

	@Override
	public void initializeValues() {
		config.comment(ClientAndServerSideNote).push("general_settings");
		oversizeAmmoEnabledValue = config.comment("Determines whether over sized ammunition (30kg, 45kg) can be fired from the regular sized vehicles (e.g. not the Giant Trebuchet)").define("oversize_ammo_enabled", true);
		ownedSoldiersUseAmmoValue = config.comment("Makes PLAYER OWNED soldiers require ammo for firing vehicles").define("owned_soldiers_use_ammo", true);
		shotsDestroysBlocksValue = config.comment("Determines whether vehicle shots can break blocks or not. Does NOT affect the Battering Ram's block breaking").define("shots_destroys_blocks", true);
		blockFiresValue = config.comment("Determines whether the fire and explosive shots can light nearby blocks on fire").define("block_fires", true);
		allowFriendlyFireValue = config.comment("Determines whether vehicle shots can hurt friendly (player owned or allied) NPCs and friendly players").define("allow_friendly_fire", false);
		useVehicleSetupTimeValue = config.comment("Makes vehicles require a setup time after they are placed").define("use_vehicle_setup_time", true);
		adjustMissilesForAccuracyValue = config.define("adjust_missiles_for_accuracy", true);
		config.pop();

		config.comment("Improvements over the original mod. Each one can be switched back to the 1.12 behaviour.").push("improvements");
		ammoFallbackValue = config.comment("Ammunition fallback\nDefault=true\n" + "The original mod leaves a weapon on its selected ammunition type when that type runs out or is taken out of the weapon:\n" + "it reports no ammunition until the player switches by key, and an NPC crew stops firing.\n" + "With this on, the weapon selects the next loaded type by itself. Set to false for the original behaviour.").define("ammo_fallback", true);
		missileHitMarginValue = config.comment("Missile hit margin\nDefault=0.5\n" + "How far (in blocks) a siege missile may pass outside an entity's bounding box and still hit it.\n" + "The original mod uses 0.3, which makes shots that graze a target miss. Set to 0.3 for the original behaviour.\n" + "Block impacts are not affected.").defineInRange("missile_hit_margin", 0.5, 0.0, 2.0);
		missileHitSweepValue = config.comment("Missile hit test along the whole step\nDefault=true\n" + "The original mod looks for entities only around the point a missile reaches at the end of a tick. A fast missile\n" + "(a bolt moves 2.25 blocks per tick) can pass a target standing at certain distances without ever testing it.\n" + "With this on, every entity along the whole step of the tick is tested. Set to false for the original behaviour.").define("missile_hit_sweep", true);
		crewLeadsTargetsValue = config.comment("NPC crews lead moving targets\nDefault=true\n" + "In the original mod an NPC crew aims at the place where its target is now, so it misses anything that walks.\n" + "With this on, the crew aims at the place where the target will be when the missile arrives (flight time of the\n" + "selected ammunition at the current aim, at most 12 blocks ahead). Set to false for the original behaviour.").define("crew_leads_targets", true);
		crewAccuracyPerLevelValue = config.comment("Crew accuracy per level\nDefault=0.067\n" + "In the original mod the level of an NPC firing a siege weapon has no effect on where the shot goes.\n" + "With this, every level of the NPC removes this share of the remaining scatter (0.067: half the scatter at level 10).\n" + "A level 0 NPC and a player shoot exactly as in the original. Set to 0 for the original behaviour.").defineInRange("crew_accuracy_per_level", 0.067, 0.0, 1.0);
		crewAccuracyMaxReductionValue = config.comment("Crew accuracy cap\nDefault=0.5\n" + "The largest share of the scatter a crew member's level can remove.").defineInRange("crew_accuracy_max_reduction", 0.5, 0.0, 1.0);
		config.pop();
		config.push("vehicle_stats_settings");
		ammoBallistaBoltDamageValue = config.comment("Ballista Bolt damage").defineInRange("ammo_ballista_bolt_damage", 18, 6, 80);
		ammoBallistaBoltIronDamageValue = config.comment("Ballista Iron Bolt damage").defineInRange("ammo_ballista_bolt_iron_damage", 30, 6, 80);
		ammoBallistaBoltFlameDamageValue = config.comment("Ballista Flame Bolt damage").defineInRange("ammo_ballista_bolt_flame_damage", 16, 6, 80);
		ammoBallistaBoltExplosiveDamageValue = config.comment("Ballista Explosive Bolt damage").defineInRange("ammo_ballista_bolt_explosive_damage", 15, 6, 80);
		ammoCannonBall5kgDamageValue = config.comment("Cannon Ball 5kg Damage").defineInRange("ammo_cannon_ball_5kg_damage", 10, 6, 80);
		ammoCannonBall10kgDamageValue = config.comment("Cannon Ball 10kg Damage").defineInRange("ammo_cannon_ball_10kg_damage", 15, 6, 80);
		ammoCannonBall15kgDamageValue = config.comment("Cannon Ball 15kg Damage").defineInRange("ammo_cannon_ball_15kg_damage", 30, 6, 80);
		ammoCannonBall25kgDamageValue = config.comment("Cannon Ball 25kg Damage").defineInRange("ammo_cannon_ball_25kg_damage", 45, 6, 80);
		ammoCanisterDamageValue = config.comment("Canister Damage (Cannon ammo)").defineInRange("ammo_canister_damage", 8, 6, 80);
		ammoHwachaRocketDamageValue = config.comment("Hwacha Rocket Damage").defineInRange("ammo_hwacha_rocket_damage", 6, 1, 80);
		ammoHwachaRocketFlameDamageValue = config.comment("Hwacha Rocket Flame Damage").defineInRange("ammo_hwacha_rocket_flame_damage", 5, 1, 80);
		ammoHwachaRocketExplosiveDamageValue = config.comment("Hwacha Rocket Explosive Damage").defineInRange("ammo_hwacha_rocket_explosive_damage", 4, 1, 80);
		vehicleBallistaHealthValue = config.comment("Determines the health of the Ballista vehicles (mobile fixed, stand turret and stand fixed)").defineInRange("vehicle_ballista_health", 100, 50, 500);
		vehicleCatapultHealthValue = config.comment("Determines the health of the Catapult vehicles (mobile fixed, stand turret and stand fixed)").defineInRange("vehicle_catapult_health", 100, 50, 500);
		vehicleCannonHealthValue = config.comment("Determines the health of the Cannon vehicles (mobile fixed, stand turret and stand fixed). There is no Boat Cannon in the mod.").defineInRange("vehicle_cannon_health", 100, 50, 500);
		vehicleHwachaHealthValue = config.comment("Determines the health of the Hwacha vehicle").defineInRange("vehicle_hwacha_health", 100, 50, 500);
		vehicleTrebuchetHealthValue = config.comment("Determines the health of the Trebuchet vehicles (mobile fixed, stand turret and stand fixed)").defineInRange("vehicle_trebuchet_health", 100, 50, 500);
		vehicleGiantTrebuchetHealthValue = config.comment("Determines the health of the Giant Trebuchet vehicle").defineInRange("vehicle_giant_trebuchet_health", 175, 50, 500);
		config.pop();
	}

	@Override
	protected void loadValues() {
		improvements.ammoFallback = ammoFallbackValue.get();
		improvements.missileHitMargin = missileHitMarginValue.get();
		improvements.missileHitSweep = missileHitSweepValue.get();
		improvements.crewLeadsTargets = crewLeadsTargetsValue.get();
		improvements.crewAccuracyPerLevel = crewAccuracyPerLevelValue.get();
		improvements.crewAccuracyMaxReduction = crewAccuracyMaxReductionValue.get();
		generalSettings.oversizeAmmoEnabled = oversizeAmmoEnabledValue.get();
		generalSettings.ownedSoldiersUseAmmo = ownedSoldiersUseAmmoValue.get();
		generalSettings.shotsDestroysBlocks = shotsDestroysBlocksValue.get();
		generalSettings.blockFires = blockFiresValue.get();
		generalSettings.allowFriendlyFire = allowFriendlyFireValue.get();
		generalSettings.useVehicleSetupTime = useVehicleSetupTimeValue.get();
		generalSettings.adjustMissilesForAccuracy = adjustMissilesForAccuracyValue.get();
		vehicleStats.ammoBallistaBoltDamage = ammoBallistaBoltDamageValue.get();
		vehicleStats.ammoBallistaBoltIronDamage = ammoBallistaBoltIronDamageValue.get();
		vehicleStats.ammoBallistaBoltFlameDamage = ammoBallistaBoltFlameDamageValue.get();
		vehicleStats.ammoBallistaBoltExplosiveDamage = ammoBallistaBoltExplosiveDamageValue.get();
		vehicleStats.ammoCannonBall5kgDamage = ammoCannonBall5kgDamageValue.get();
		vehicleStats.ammoCannonBall10kgDamage = ammoCannonBall10kgDamageValue.get();
		vehicleStats.ammoCannonBall15kgDamage = ammoCannonBall15kgDamageValue.get();
		vehicleStats.ammoCannonBall25kgDamage = ammoCannonBall25kgDamageValue.get();
		vehicleStats.ammoCanisterDamage = ammoCanisterDamageValue.get();
		vehicleStats.ammoHwachaRocketDamage = ammoHwachaRocketDamageValue.get();
		vehicleStats.ammoHwachaRocketFlameDamage = ammoHwachaRocketFlameDamageValue.get();
		vehicleStats.ammoHwachaRocketExplosiveDamage = ammoHwachaRocketExplosiveDamageValue.get();
		vehicleStats.vehicleBallistaHealth = vehicleBallistaHealthValue.get();
		vehicleStats.vehicleCatapultHealth = vehicleCatapultHealthValue.get();
		vehicleStats.vehicleCannonHealth = vehicleCannonHealthValue.get();
		vehicleStats.vehicleHwachaHealth = vehicleHwachaHealthValue.get();
		vehicleStats.vehicleTrebuchetHealth = vehicleTrebuchetHealthValue.get();
		vehicleStats.vehicleGiantTrebuchetHealth = vehicleGiantTrebuchetHealthValue.get();
		VehicleRegistry.applyConfig();
	}
}
