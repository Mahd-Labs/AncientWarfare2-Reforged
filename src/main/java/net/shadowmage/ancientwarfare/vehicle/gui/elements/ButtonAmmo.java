package net.shadowmage.ancientwarfare.vehicle.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.gui.elements.Button;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.missiles.IAmmo;
import net.shadowmage.ancientwarfare.vehicle.registry.AmmoRegistry;

public class ButtonAmmo extends Button {
	IAmmo ammo;
	ItemStack stack;
	VehicleBase vehicle;

	public ButtonAmmo(int topLeftX, int topLeftY, int width, int height, IAmmo ammo, VehicleBase vehicle) {
		super(topLeftX, topLeftY, width, height, "");
		this.ammo = ammo;
		stack = new ItemStack(AmmoRegistry.getItemForAmmo(ammo));
		this.vehicle = vehicle;
		if (ammo != null) {
			setText(I18n.get("item." + ammo.getRegistryName().getPath() + ".name"));
		}
	}

	@Override
	protected void onPressed() {
		vehicle.ammoHelper.handleClientAmmoSelection(ammo.getRegistryName());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		if (visible) {
			//draw ammo icon on left
			//draw ammo name to the right of that
			//on the far right, ammo qty
			if (this.ammo != null) {
				String quantity = String.valueOf(vehicle.ammoHelper.getCountOf(ammo));
				graphics.renderItem(stack, renderX + 3, renderY + 3);
				int quantityRenderX = renderX + this.width - 10 - Minecraft.getInstance().font.width(quantity);
				graphics.drawString(Minecraft.getInstance().font, quantity, quantityRenderX, renderY + textY, 0xffffffff, true);
			}
		}
	}
}
