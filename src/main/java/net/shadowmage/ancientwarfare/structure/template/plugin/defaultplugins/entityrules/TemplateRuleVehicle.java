package net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.entityrules;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.api.IStructureBuilder;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleType;

/*
 * Port notes: the vehicle module is reduced to the stationary siege weapons. A rule
 * whose saved vehicle type (entityData.vehType) is not one of the ported types - mobile variants, battering ram,
 * chest cart, boats - places nothing; that is logged at DEBUG only. Everything else is the 1.12 rule (vehicle yaw
 * and turret rotation turn with the structure, in the opposite sense of other entities because the vehicle yaw is
 * mirrored).
 */
public class TemplateRuleVehicle extends TemplateRuleEntity<VehicleBase> {
	public static final String PLUGIN_NAME = "AWVehicle";

	private float turretRotation;

	public TemplateRuleVehicle() {
		super();
	}

	public TemplateRuleVehicle(Level world, VehicleBase entity, int turns, int x, int y, int z) {
		super(world, entity, turns, x, y, z);
		rotation = (entity.getYRot() - 90.f * turns) % 360.f;
		turretRotation = (entity.localTurretDestRot - 90.f * turns) % 360.f;
	}

	@Override
	public void handlePlacement(Level world, int turns, BlockPos pos, IStructureBuilder builder) {
		CompoundTag entityData = getEntityNBT(pos, turns);
		int vehType = entityData == null ? -1 : entityData.getInt("vehType");
		if (VehicleType.getVehicleType(vehType) == null) {
			AncientWarfareStructure.LOG.debug("Vehicle type {} is not part of the reduced vehicle module. Entity skipped during structure creation.", vehType);
			return;
		}
		super.handlePlacement(world, turns, pos, builder);
	}

	@Override
	protected void updateEntityOnPlacement(int turns, BlockPos pos, VehicleBase e) {
		e.absMoveTo(pos.getX() + BlockTools.rotateFloatX(xOffset, zOffset, turns), pos.getY() + yOffset,
				pos.getZ() + BlockTools.rotateFloatZ(xOffset, zOffset, turns), (rotation - 90f * turns) % 360f, 0);

		e.localTurretRotation = (turretRotation - 90f * turns) % 360;
		e.localTurretDestRot = e.localTurretRotation;
	}

	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}

	@Override
	public void writeRuleData(CompoundTag tag) {
		super.writeRuleData(tag);
		tag.putFloat("turretRotation", turretRotation);
	}

	@Override
	public void parseRule(CompoundTag tag) {
		super.parseRule(tag);
		turretRotation = tag.getFloat("turretRotation");
	}
}
