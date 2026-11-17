package net.shadowmage.ancientwarfare.vehicle.registry;

import net.shadowmage.ancientwarfare.vehicle.missiles.IAmmo;

public class VehicleAmmoEntry {

	public IAmmo baseAmmoType;
	public int ammoCount;

	public VehicleAmmoEntry(IAmmo ammo) {
		this.baseAmmoType = ammo;
	}

}
