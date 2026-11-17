package net.shadowmage.ancientwarfare.vehicle.gui;

import net.shadowmage.ancientwarfare.core.container.ContainerBase;
import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase;
import net.shadowmage.ancientwarfare.core.gui.elements.Button;
import net.shadowmage.ancientwarfare.core.gui.elements.CompositeScrolled;
import net.shadowmage.ancientwarfare.vehicle.container.ContainerVehicle;
import net.shadowmage.ancientwarfare.vehicle.gui.elements.ButtonAmmo;
import net.shadowmage.ancientwarfare.vehicle.missiles.IAmmo;

import java.util.List;
import java.util.stream.Collectors;

/* Port notes: getXSize() / getYSize() overrides -> the size is passed to the base constructor (256 x 240). */
public class GuiVehicleAmmoSelection extends GuiContainerBase<ContainerVehicle> {
	private CompositeScrolled area;

	public GuiVehicleAmmoSelection(ContainerBase container) {
		super(container, 256, 240);
		this.shouldCloseOnVanillaKeys = true;
	}

	@Override
	public void initElements() {
		//TODO lang translations
		area = new CompositeScrolled(this, 10, 40, imageWidth - 20, imageHeight - 40 - 10);
		addGuiElement(area);
		Button done = new Button(imageWidth - 35 - 5, 5, 35, 12, "Done") {
			@Override
			protected void onPressed() {
				closeGui();
			}
		};
		addGuiElement(done);
	}

	@Override
	public void setupElements() {
		area.clearElements();
		if (getContainer().vehicle == null) {
			return;
		}
		List<IAmmo> ammos = getContainer().vehicle.vehicleType.getValidAmmoTypes().stream().filter(a -> getContainer().vehicle.ammoHelper.getCountOf(a) > 0)
				.collect(Collectors.toList());
		int currentY = 10;
		for (int i = 0; i < ammos.size(); i++) {
			ButtonAmmo ammo = new ButtonAmmo(5, currentY, imageWidth - 20 - 20 - 5, 22, ammos.get(i), getContainer().vehicle) {
				@Override
				protected void onPressed() {
					super.onPressed();
					closeGui();
				}
			};
			area.addGuiElement(ammo);
			currentY += 25;
		}
		area.setAreaSize(currentY);
	}
}
