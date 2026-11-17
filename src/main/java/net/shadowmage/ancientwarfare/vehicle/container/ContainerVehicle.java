package net.shadowmage.ancientwarfare.vehicle.container;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.core.container.ContainerBase;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

public class ContainerVehicle extends ContainerBase {
	public VehicleBase vehicle;

	public ContainerVehicle(Player player, int entityId, int dummy1, int dummy2) {
		super(player);
		Entity entity = player.level().getEntity(entityId);
		this.vehicle = entity instanceof VehicleBase ? (VehicleBase) entity : null;
	}

	@Override
	public boolean canInteractWith(Player player) {
		return super.canInteractWith(player) && vehicle != null && vehicle.isAlive();
	}
}
