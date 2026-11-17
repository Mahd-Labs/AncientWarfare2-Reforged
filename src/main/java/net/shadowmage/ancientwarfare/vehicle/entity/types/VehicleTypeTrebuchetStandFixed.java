package net.shadowmage.ancientwarfare.vehicle.entity.types;

public class VehicleTypeTrebuchetStandFixed extends VehicleTypeTrebuchet {

	/**
	 * @param typeNum
	 */
	public VehicleTypeTrebuchetStandFixed(int typeNum) {
		super(typeNum);
		this.configName = "trebuchet_stand";
		this.displayName = "item.vehicleSpawner.13";
		this.displayTooltip.add("item.vehicleSpawner.tooltip.weight");
		this.displayTooltip.add("item.vehicleSpawner.tooltip.fixed");
		this.displayTooltip.add("item.vehicleSpawner.tooltip.noturret");
		this.riderForwardsOffset = 1.425f;
		this.riderVerticalOffset = 0.35f;
		this.armorBaySize = 4;
		this.upgradeBaySize = 4;
	}
}
