package net.shadowmage.ancientwarfare.vehicle.helpers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.shadowmage.ancientwarfare.core.util.INBTSerializable;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

/*
 * Port notes: vehicle upgrades and armour are not ported (user decision, see docs/DETAILS.md, "What is in, and what is not"). What is
 * left of the 1.12 class is what VehicleBase needs: the stat reset (updateUpgradeStats, called when a faction siege
 * engineer leaves the weapon), the damage scaling by the (always 0) resist values and the NBT shape ("upgrades" /
 * "armors" lists, written empty, ignored on read) so saved vehicles keep the 1.12 layout.
 * getScaledDamage: 1.12 compared against DamageType.explosiveMissile / fireMissile instances and
 * DamageSource.isExplosion() / isFireDamage(); the port uses the damage type tags is_explosion / is_fire.
 */
public class VehicleUpgradeHelper implements INBTSerializable<CompoundTag> {
	private VehicleBase vehicle;

	public VehicleUpgradeHelper(VehicleBase vehicle) {
		this.vehicle = vehicle;
	}

	public void updateUpgrades() {
		if (vehicle.level().isClientSide) {
			return;
		}
		this.updateUpgradeStats();
	}

	public void updateUpgradeStats() {
		vehicle.resetCurrentStats();
	}

	public float getScaledDamage(DamageSource src, float amt) {
		if (src.is(DamageTypeTags.IS_EXPLOSION)) {
			return amt * (1 - (vehicle.currentExplosionResist * 0.01f));
		} else if (src.is(DamageTypeTags.IS_FIRE)) {
			return amt * (1 - (vehicle.currentFireResist * 0.01f));
		}
		return amt * (1 - (vehicle.currentGenericResist * 0.01f));
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.put("upgrades", new ListTag());
		tag.put("armors", new ListTag());
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
	}
}
