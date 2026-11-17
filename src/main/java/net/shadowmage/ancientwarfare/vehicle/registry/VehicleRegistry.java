package net.shadowmage.ancientwarfare.vehicle.registry;

import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.entity.IVehicleType;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleType;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleTypeBallistaStand;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleTypeBallistaStandTurret;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleTypeCannonStandFixed;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleTypeCannonStandTurret;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleTypeCatapultStandFixed;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleTypeCatapultStandTurret;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleTypeHwacha;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleTypeTrebuchetLarge;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleTypeTrebuchetStandFixed;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleTypeTrebuchetStandTurret;

import java.util.ArrayList;
import java.util.List;

/*
 * Port notes: reduced module, only the stationary siege weapons are registered. The type numbers are the 1.12 ones
 * (they are stored in entity NBT "vehType", in structure templates and decide the spawner item). Not registered
 * here: 2, 3 catapult mobile; 6, 7 ballista mobile; 8 battering ram; 11 cannon mobile; 15 trebuchet
 * mobile; 17 chest cart; 18-20 boats.
 * 1.12 registerVehicles() ran in the item registry event after the config was loaded; applyConfig() is its
 * replacement and runs every time the config (re)loads.
 */
public class VehicleRegistry {
	public static final IVehicleType CATAPULT_STAND_FIXED = new VehicleTypeCatapultStandFixed(0);
	public static final IVehicleType CATAPULT_STAND_TURRET = new VehicleTypeCatapultStandTurret(1);
	public static final IVehicleType BALLISTA_STAND_FIXED = new VehicleTypeBallistaStand(4);
	public static final IVehicleType BALLISTA_STAND_TURRET = new VehicleTypeBallistaStandTurret(5);
	public static final IVehicleType CANNON_STAND_FIXED = new VehicleTypeCannonStandFixed(9);
	public static final IVehicleType CANNON_STAND_TURRET = new VehicleTypeCannonStandTurret(10);
	public static final IVehicleType HWACHA = new VehicleTypeHwacha(12);
	public static final IVehicleType TREBUCHET_STAND_FIXED = new VehicleTypeTrebuchetStandFixed(13);
	public static final IVehicleType TREBUCHET_STAND_TURRET = new VehicleTypeTrebuchetStandTurret(14);
	public static final IVehicleType TREBUCHET_LARGE = new VehicleTypeTrebuchetLarge(16);

	private VehicleRegistry() {
	}

	/* the registered types in type number order */
	public static List<IVehicleType> getTypes() {
		List<IVehicleType> types = new ArrayList<>();
		for (IVehicleType vehicle : VehicleType.vehicleTypes) {
			if (vehicle != null) {
				types.add(vehicle);
			}
		}
		return types;
	}

	public static void applyConfig() {
		AWVehicleStatics.VehicleStats stats = AWVehicleStatics.vehicleStats;
		CATAPULT_STAND_FIXED.setBaseHealth(stats.vehicleCatapultHealth);
		CATAPULT_STAND_TURRET.setBaseHealth(stats.vehicleCatapultHealth);
		BALLISTA_STAND_FIXED.setBaseHealth(stats.vehicleBallistaHealth);
		BALLISTA_STAND_TURRET.setBaseHealth(stats.vehicleBallistaHealth);
		CANNON_STAND_FIXED.setBaseHealth(stats.vehicleCannonHealth);
		CANNON_STAND_TURRET.setBaseHealth(stats.vehicleCannonHealth);
		HWACHA.setBaseHealth(stats.vehicleHwachaHealth);
		TREBUCHET_STAND_FIXED.setBaseHealth(stats.vehicleTrebuchetHealth);
		TREBUCHET_STAND_TURRET.setBaseHealth(stats.vehicleTrebuchetHealth);
		TREBUCHET_LARGE.setBaseHealth(stats.vehicleGiantTrebuchetHealth);
		AmmoRegistry.applyConfig();
		for (IVehicleType vehicle : VehicleType.vehicleTypes) {
			if (vehicle instanceof VehicleType) {
				((VehicleType) vehicle).applyOversizeAmmoConfig(AWVehicleStatics.generalSettings.oversizeAmmoEnabled);
			}
		}
	}
}
