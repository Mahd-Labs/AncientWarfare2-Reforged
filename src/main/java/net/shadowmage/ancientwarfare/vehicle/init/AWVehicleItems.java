package net.shadowmage.ancientwarfare.vehicle.init;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.vehicle.AncientWarfareVehicles;
import net.shadowmage.ancientwarfare.vehicle.entity.IVehicleType;
import net.shadowmage.ancientwarfare.vehicle.item.ItemMisc;
import net.shadowmage.ancientwarfare.vehicle.item.ItemSpawner;
import net.shadowmage.ancientwarfare.vehicle.registry.AmmoRegistry;
import net.shadowmage.ancientwarfare.vehicle.registry.VehicleRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/*
 * Port notes: registry event -> DeferredRegister. Registration order is the 1.12 one (spawner, misc items, ammo).
 * - "spawner" (metadata = vehicle type) is flattened to one item per ported type: spawner_<config name>
 *   (SPAWNERS: type number -> item; SPAWNER = the catapult stand, used as tab icon).
 * - Not registered: mobility_unit (only used by recipes of mobile vehicles), the armour items (ArmorRegistry) and
 *   the upgrade items (UpgradeRegistry) - see docs/DETAILS.md, "What is in, and what is not".
 */
public class AWVehicleItems {
	private AWVehicleItems() {}

	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AncientWarfareVehicles.MOD_ID);
	private static final List<DeferredItem<? extends Item>> MISC_ITEMS = new ArrayList<>();

	public static final Map<Integer, DeferredItem<ItemSpawner>> SPAWNERS;
	public static final DeferredItem<ItemSpawner> SPAWNER;

	static {
		Map<Integer, DeferredItem<ItemSpawner>> spawners = new LinkedHashMap<>();
		for (IVehicleType type : VehicleRegistry.getTypes()) {
			final int typeNum = type.getGlobalVehicleType();
			final String name = "spawner_" + type.getConfigName();
			spawners.put(typeNum, ITEMS.register(name, () -> new ItemSpawner(name, typeNum)));
		}
		SPAWNERS = Collections.unmodifiableMap(spawners);
		SPAWNER = SPAWNERS.get(0);
	}

	public static final DeferredItem<ItemMisc> FLAME_CHARGE = misc("flame_charge", ItemMisc.VehicleItemType.AMMO_MATERIAL);
	public static final DeferredItem<ItemMisc> EXPLOSIVE_CHARGE = misc("explosive_charge", ItemMisc.VehicleItemType.AMMO_MATERIAL);
	public static final DeferredItem<ItemMisc> ROCKET_CHARGE = misc("rocket_charge", ItemMisc.VehicleItemType.AMMO_MATERIAL);
	public static final DeferredItem<ItemMisc> CLUSTER_CHARGE = misc("cluster_charge", ItemMisc.VehicleItemType.AMMO_MATERIAL);
	public static final DeferredItem<ItemMisc> NAPALM_CHARGE = misc("napalm_charge", ItemMisc.VehicleItemType.AMMO_MATERIAL);
	public static final DeferredItem<ItemMisc> CLAY_CASING = misc("clay_casing", ItemMisc.VehicleItemType.AMMO_MATERIAL);
	public static final DeferredItem<ItemMisc> IRON_CASING = misc("iron_casing", ItemMisc.VehicleItemType.AMMO_MATERIAL);
	public static final DeferredItem<ItemMisc> TURRET_COMPONENTS = misc("turret_components", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);
	public static final DeferredItem<ItemMisc> TORSION_UNIT = misc("torsion_unit", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);
	public static final DeferredItem<ItemMisc> COUNTER_WEIGHT_UNIT = misc("counter_weight_unit", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);
	public static final DeferredItem<ItemMisc> POWDER_CASE = misc("powder_case", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);
	public static final DeferredItem<ItemMisc> EQUIPMENT_BAY = misc("equipment_bay", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);
	public static final DeferredItem<ItemMisc> ROUGH_WOOD = misc("rough_wood", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);
	public static final DeferredItem<ItemMisc> TREATED_WOOD = misc("treated_wood", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);
	public static final DeferredItem<ItemMisc> IRONSHOD_WOOD = misc("ironshod_wood", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);
	public static final DeferredItem<ItemMisc> IRON_CORE_WOOD = misc("iron_core_wood", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);
	public static final DeferredItem<ItemMisc> ROUGH_IRON = misc("rough_iron", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);
	public static final DeferredItem<ItemMisc> FINE_IRON = misc("fine_iron", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);
	public static final DeferredItem<ItemMisc> TEMPERED_IRON = misc("tempered_iron", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);
	public static final DeferredItem<ItemMisc> MINOR_ALLOY = misc("minor_alloy", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);
	public static final DeferredItem<ItemMisc> MAJOR_ALLOY = misc("major_alloy", ItemMisc.VehicleItemType.VEHICLE_COMPONENT);

	static {
		AmmoRegistry.registerAmmo(ITEMS);
	}

	private static DeferredItem<ItemMisc> misc(String name, ItemMisc.VehicleItemType type) {
		DeferredItem<ItemMisc> item = register(name, () -> new ItemMisc(name, type));
		MISC_ITEMS.add(item);
		return item;
	}

	private static <T extends Item> DeferredItem<T> register(String name, Supplier<T> supplier) {
		return ITEMS.register(name, supplier);
	}

	/* the misc items in the 1.12 registration order (creative tab) */
	public static List<DeferredItem<? extends Item>> miscItems() {
		return Collections.unmodifiableList(MISC_ITEMS);
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
	}
}
