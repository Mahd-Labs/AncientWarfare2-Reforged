package net.shadowmage.ancientwarfare.vehicle.gui;

import net.minecraft.client.Minecraft;
import net.shadowmage.ancientwarfare.core.container.ContainerBase;
import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase;
import net.shadowmage.ancientwarfare.core.gui.elements.Button;
import net.shadowmage.ancientwarfare.core.gui.elements.Label;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.vehicle.container.ContainerVehicleInventory;
import net.shadowmage.ancientwarfare.vehicle.network.PacketPackCommand;

/*
 * Port notes: the "Upg." and "Armor" labels and the storage rows with their -/+ buttons are gone with the upgrade,
 * armour and storage inventories (see ContainerVehicleInventory). Size: 195 x (playerY + 4 * 18 + 8 + 4), the 1.12
 * getXSize() / getYSize() values.
 */
public class GuiVehicleInventory extends GuiContainerBase<ContainerVehicleInventory> {
	public GuiVehicleInventory(ContainerBase container) {
		super(container, 171 + 16 + 8, ((ContainerVehicleInventory) container).playerY + 4 * 18 + 8 + 4);
		this.shouldCloseOnVanillaKeys = true;
	}

	@Override
	public void initElements() {
		//TODO lang translations
		addGuiElement(new Label(8, getContainer().extrasY - 10, "Ammo"));
		addGuiElement(new Label(8, getContainer().playerY - 10, "Player Inventory"));

		Button stats = new Button(8, 4, 45, 16, "Stats") {
			@Override
			protected void onPressed() {
				if (getContainer().vehicle != null) {
					Minecraft.getInstance().setScreen(new GuiVehicleStats(GuiVehicleInventory.this, getContainer().vehicle));
				}
			}
		};
		addGuiElement(stats);

		Button pack = new Button(8 + 45 + 4, 4, 45, 16, "Pack") {
			@Override
			protected void onPressed() {
				if (getContainer().vehicle != null) {
					NetworkHandler.sendToServer(new PacketPackCommand(getContainer().vehicle));
				}
				closeGui();
			}
		};
		addGuiElement(pack);
	}

	@Override
	public void setupElements() {
	}
}
