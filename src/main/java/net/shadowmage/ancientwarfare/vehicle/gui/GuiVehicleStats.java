package net.shadowmage.ancientwarfare.vehicle.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase;
import net.shadowmage.ancientwarfare.core.gui.elements.Label;
import net.shadowmage.ancientwarfare.core.util.Trig;
import net.shadowmage.ancientwarfare.vehicle.container.ContainerVehicleInventory;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

/*
 * Port notes: 1.12 used a client-made DummyContainer. A 1.21 container screen needs a menu the server knows, so the
 * stats screen reuses the container of the inventory screen it was opened from and hides its slots while it is
 * shown (removeSlots / addSlots), like the other raw sub screens of the port (GuiSelectFromList). The screen was
 * also registered under GUI_VEHICLE_STATS in 1.12 but never opened through it.
 */
public class GuiVehicleStats extends GuiContainerBase<ContainerVehicleInventory> {
	private VehicleBase vehicle;
	private GuiVehicleInventory parent;

	public GuiVehicleStats(GuiVehicleInventory parent, VehicleBase vehicle) {
		super(parent.getContainer(), 256, 240);
		this.vehicle = vehicle;
		this.parent = parent;
		this.shouldCloseOnVanillaKeys = true;
		getContainer().removeSlots();
	}

	@Override
	public void initElements() {
		//TODO lang translations
		addGuiElement(new Label(10, 4, "Vehicle Type: " + I18n.get(vehicle.vehicleType.getDisplayName() + ".name")));
		addGuiElement(new Label(10, 14, "Material Level: " + vehicle.vehicleMaterialLevel));
		addGuiElement(new Label(10, 24, "Health: " + vehicle.getHealth() + "/" + vehicle.baseHealth));
		addGuiElement(new Label(10, 34, "Weight: " + vehicle.currentWeight + "/" + vehicle.baseWeight));
		addGuiElement(new Label(10, 44, "Speed: " + (Trig.getVelocity(vehicle.getDeltaMovement().x, vehicle.getDeltaMovement().y, vehicle.getDeltaMovement().z) * 20) + "/" + (vehicle.currentForwardSpeedMax * 20)));
		addGuiElement(new Label(10, 54, "Missile Velocity: " + vehicle.localLaunchPower + "/" + vehicle.currentLaunchSpeedPowerMax));
		addGuiElement(new Label(10, 64, "Resists: F: " + vehicle.currentFireResist + " E: " + vehicle.currentExplosionResist + " G: " + vehicle.currentGenericResist));
		addGuiElement(new Label(10, 74, "Mountable: " + vehicle.isMountable()));
		addGuiElement(new Label(10, 84, "Drivable: " + vehicle.isDrivable()));
		addGuiElement(new Label(10, 94, "Combat Vehicle: " + vehicle.isAimable()));
		addGuiElement(new Label(10, 104, "Rider Sits: " + vehicle.shouldRiderSit()));
		addGuiElement(new Label(10, 114, "Rider On Turret: " + vehicle.vehicleType.moveRiderWithTurret()));
		addGuiElement(new Label(10, 124, "Adjustable Yaw: " + vehicle.canAimRotate()));
		addGuiElement(new Label(10, 134, "Adjustable Pitch: " + vehicle.canAimPitch()));
		addGuiElement(new Label(10, 144, "Adjustable Power: " + vehicle.canAimPower()));
		addGuiElement(new Label(10, 154, "Pitch Min: " + vehicle.currentTurretPitchMin));
		addGuiElement(new Label(10, 164, "Pitch Max: " + vehicle.currentTurretPitchMax));
		addGuiElement(new Label(10, 174, "Yaw Min: " + (vehicle.localTurretRotationHome - vehicle.currentTurretRotationMax)));
		addGuiElement(new Label(10, 184, "Yaw Max: " + (vehicle.localTurretRotationHome + vehicle.currentTurretRotationMax)));
		addGuiElement(new Label(10, 204, "TeamNum: " + (vehicle.getTeam() == null ? "" : vehicle.getTeam().getName())));
	}

	@Override
	public void setupElements() {
	}

	@Override
	protected boolean onGuiCloseRequested() {
		getContainer().addSlots();
		getContainer().setGui(parent);
		Minecraft.getInstance().setScreen(parent);
		return false;
	}
}
