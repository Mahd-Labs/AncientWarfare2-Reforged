package net.shadowmage.ancientwarfare.vehicle.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.shadowmage.ancientwarfare.core.util.Trig;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.missiles.IAmmo;
import net.shadowmage.ancientwarfare.vehicle.registry.AmmoRegistry;

/*
 * Client-only. Port notes: RenderTickEvent END with no screen open -> RenderGuiEvent.Post. Same lines, positions
 * and colours as 1.12 (Gui.drawString draws with shadow). The air vehicle branch (throttle / climb rate) is gone
 * with the movement types.
 */
public class RenderOverlay {
	private void renderVehicleOverlay(GuiGraphics graphics) {
		Minecraft mc = Minecraft.getInstance();
		Font fontRenderer = mc.font;
		VehicleBase vehicle = (VehicleBase) mc.player.getVehicle();
		int white = 0xFFFFFFFF;
		int red = 0xFFFF0000;
		graphics.drawString(fontRenderer, "Range: " + vehicle.firingHelper.clientHitRange, 10, 10, white, true);
		graphics.drawString(fontRenderer, "Pitch: " + vehicle.firingHelper.clientTurretPitch, 10, 20, white, true);
		graphics.drawString(fontRenderer, "Yaw: " + vehicle.firingHelper.clientTurretYaw, 10, 30, white, true);
		graphics.drawString(fontRenderer, "Velocity: " + vehicle.firingHelper.clientLaunchSpeed, 10, 40, white, true);
		IAmmo ammo = vehicle.ammoHelper.getCurrentAmmoType();
		if (ammo != null) {
			int count = vehicle.ammoHelper.getCurrentAmmoCount();
			graphics.drawString(fontRenderer, "Ammo: " + I18n.get(AmmoRegistry.getItemForAmmo(ammo).getDescriptionId()), 10, 50, count > 0 ? white : red, true);
			graphics.drawString(fontRenderer, "Count: " + count, 10, 60, count > 0 ? white : red, true);
		} else {
			graphics.drawString(fontRenderer, "No Ammo Selected", 10, 50, red, true);
		}
		if (AWVehicleStatics.clientSettings.renderAdvOverlay) {
			float velocity = Trig.getVelocity(vehicle.getDeltaMovement().x, 0, vehicle.getDeltaMovement().z);
			graphics.drawString(fontRenderer, "Velocity: " + velocity * 20.f + "m/s  max: " + vehicle.currentForwardSpeedMax * 20, 10, 70, white, true);
			graphics.drawString(fontRenderer, "Yaw Rate: " + vehicle.moveHelper.getRotationSpeed() * 20.f, 10, 80, white, true);
		}
	}

	@SubscribeEvent
	public void tickEnd(RenderGuiEvent.Post event) {
		if (!AWVehicleStatics.clientSettings.renderOverlay) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen == null && mc.player != null && mc.player.getVehicle() instanceof VehicleBase && !mc.options.hideGui) {
			this.renderVehicleOverlay(event.getGuiGraphics());
		}
	}
}
