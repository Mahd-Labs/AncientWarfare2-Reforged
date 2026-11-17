package net.shadowmage.ancientwarfare.vehicle.input;

import net.minecraft.client.Minecraft;
import net.shadowmage.ancientwarfare.core.input.IInputCallback;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

import java.util.function.Consumer;

/* Client-only. Port notes: 1.12 cast the ridden entity without a check (the key context made sure of it). */
public class VehicleCallback implements IInputCallback {
	private final Consumer<VehicleBase> callback;

	public VehicleCallback(Consumer<VehicleBase> callback) {
		this.callback = callback;
	}

	@Override
	public void onKeyPressed() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.getVehicle() instanceof VehicleBase) {
			callback.accept((VehicleBase) mc.player.getVehicle());
		}
	}
}
