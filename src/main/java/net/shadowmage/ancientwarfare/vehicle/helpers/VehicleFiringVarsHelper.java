package net.shadowmage.ancientwarfare.vehicle.helpers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.core.util.INBTSerializable;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

public abstract class VehicleFiringVarsHelper implements INBTSerializable<CompoundTag> {

	protected VehicleBase vehicle;

	public VehicleFiringVarsHelper(VehicleBase vehicle) {
		this.vehicle = vehicle;
	}

	/**
	 * called on every tick that the vehicle is 'firing' to update the firing animation and to call
	 * launchMissile when animation has reached launch point
	 */
	public abstract void onFiringUpdate();

	/**
	 * called every tick after the vehicle has fired, until reload timer is complete, to update animations
	 */
	public abstract void onReloadUpdate();

	/**
	 * called every tick after startLaunching() is called, until setFinishedLaunching() is called...
	 */
	public abstract void onLaunchingUpdate();

	public abstract void onReloadingFinished();

	public void onTick() {
	}

	public boolean interact(Player player) {
		if (player.level().isClientSide) {
			return true;
		}
		if (!player.isShiftKeyDown() && !vehicle.isVehicle()) {
			player.startRiding(vehicle);
			return true;
		} else if (player.isShiftKeyDown()) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_VEHICLE_INVENTORY, vehicle.getId());
		} else if (vehicle.isVehicle() && vehicle.getPassengers().get(0) instanceof NpcBase) {
			NpcBase npc = (NpcBase) vehicle.getPassengers().get(0);
			npc.stopRiding();
		}
		return true;
	}

	public abstract float getVar1();

	public abstract float getVar2();

	public abstract float getVar3();

	public abstract float getVar4();

	public abstract float getVar5();

	public abstract float getVar6();

	public abstract float getVar7();

	public abstract float getVar8();

}
