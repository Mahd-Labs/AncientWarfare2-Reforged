package net.shadowmage.ancientwarfare.vehicle.helpers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.util.INBTSerializable;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.MathUtils;
import net.shadowmage.ancientwarfare.core.util.Trig;
import net.shadowmage.ancientwarfare.npc.entity.vehicle.ITarget;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.missiles.IAmmo;
import net.shadowmage.ancientwarfare.vehicle.missiles.MissileBase;
import net.shadowmage.ancientwarfare.vehicle.network.PacketAimUpdate;
import net.shadowmage.ancientwarfare.vehicle.network.PacketFireUpdate;

import java.util.Optional;
import java.util.Random;

/**
 * handles aiming, firing, updating turret, and client/server comms for input updates
 *
 * @author Shadowmage
 */
public class VehicleFiringHelper implements INBTSerializable<CompoundTag> {

	private static final int TRAJECTORY_ITERATIONS_CLIENT = 20;

	protected static Random rng = new Random();
	/**
	 * these values are updated when the client chooses an aim point, used by overlay rendering gui
	 */
	public float clientHitRange = 0.f;
	public float clientHitPosX = 0.f;
	public float clientHitPosY = 0.f;
	public float clientHitPosZ = 0.f;

	/**
	 * client-side values used by the riding player to check current input vs previous to see if new input packets should be sent...
	 */
	public float clientTurretYaw = 0.f;
	public float clientTurretPitch = 0.f;
	public float clientLaunchSpeed = 0.f;

	/**
	 * used on final launch, to calc final angle from 'approximate' firing arm/turret angle
	 */
	public Vec3 targetPos = null;

	/**
	 * is this vehicle in the process of launching a missile ? (animation, etc)
	 */
	private boolean isFiring = false;

	/**
	 * has started launching...
	 */
	private boolean isLaunching = false;
	/**
	 * if this vehicle isFiring, has it already finished launched, and is in the process of cooling down?
	 */
	private boolean isReloading = false;

	/**
	 * how many ticks until this vehicle is done reloading and can fire again
	 */
	private int reloadingTicks = 0;

	private VehicleBase vehicle;

	public VehicleFiringHelper(VehicleBase vehicle) {
		this.vehicle = vehicle;
	}

	/**
	 * spawns the number of missiles that this vehicle should fire by weight-count
	 * at the given offset from normal missile spawn position (offset is world
	 * coordinates, needs translating prior to being passed in)
	 */
	public void spawnMissilesByWeightCount(float ox, float oy, float oz) {
		int count = getMissileLaunchCount();
		for (int i = 0; i < count; i++) {
			spawnMissile(ox, oy, oz);
		}
	}

	/**
	 * spawn a missile of current missile type, with current firing paramaters, with additional raw x, y, z offsets
	 */
	public void spawnMissile(float ox, float oy, float oz) {
		if (!vehicle.level().isClientSide) {
			IAmmo ammo = vehicle.ammoHelper.getCurrentAmmoType();
			if (ammo == null) {
				return;
			}
			if (vehicle.ammoHelper.getCurrentAmmoCount() > 0) {
				vehicle.ammoHelper.decreaseCurrentAmmo();

				Vec3 off = vehicle.getMissileOffset();
				float x = (float) (vehicle.getX() + off.x + ox);
				float y = (float) (vehicle.getY() + off.y + oy);
				float z = (float) (vehicle.getZ() + off.z + oz);

				int count = ammo.hasSecondaryAmmo() ? ammo.getSecondaryAmmoTypeCount() : 1;
				//      Config.logDebug("type: "+ammo.getDisplayName()+" missile count to fire: "+count + " hasSecondaryAmmo: "+ammo.hasSecondaryAmmo() + " secType: "+ammo.getSecondaryAmmoType());
				MissileBase missile = null;
				float maxPower;
				float yaw;
				float pitch;
				float accuracy;
				float power;
				for (int i = 0; i < count; i++) {
					maxPower = getAdjustedMaxMissileVelocity();
					power = Math.min(vehicle.localLaunchPower, maxPower);
					yaw = vehicle.localTurretRotation;
					pitch = vehicle.localTurretPitch + vehicle.getXRot();
					if (AWVehicleStatics.generalSettings.adjustMissilesForAccuracy) {
						accuracy = getAccuracyAdjusted();
						yaw += (float) rng.nextGaussian() * (1.f - accuracy) * 10.f;
						if (vehicle.canAimPower() && !ammo.isRocket()) {
							power += (float) rng.nextGaussian() * (1.f - accuracy) * 2.5f;
							if (power < 1.f) {
								power = 1.f;
							}
						} else if (vehicle.canAimPitch()) {
							pitch += (float) rng.nextGaussian() * (1.f - accuracy) * 10.f;
						} else if (ammo != null && ammo.isRocket()) {
							power += power / vehicle.currentLaunchSpeedPowerMax;
							pitch += (rng.nextFloat() * 2.f - 1.f) * (1.f - accuracy) * 50.f;
						}
					}
					missile = getMissile2(x, y, z, yaw, pitch, power);
					if (missile != null) {
						vehicle.level().addFreshEntity(missile);
					}
				}
			}
		}
	}

	MissileBase getMissile2(float x, float y, float z, float yaw, float pitch, float velocity) {
		IAmmo ammo = vehicle.ammoHelper.getCurrentAmmoType();
		if (ammo != null) {
			MissileBase missile = new MissileBase(vehicle.level());
			if (ammo.hasSecondaryAmmo()) {
				ammo = ammo.getSecondaryAmmoType();
			}
			missile.setMissileParams2(ammo, x, y, z, yaw, pitch, velocity);
			missile.setMissileCallback(vehicle);
			missile.setLaunchingEntity(vehicle);
			missile.setShooter(vehicle.getFirstPassenger());
			return missile;
		}
		return null;
	}

	public void onTick() {
		if (this.isReloading) {
			this.vehicle.onReloadUpdate();
			if (this.reloadingTicks <= 0) {
				this.setFinishedReloading();
				this.vehicle.firingVarsHelper.onReloadingFinished();
			}
			this.reloadingTicks--;
		}
		if (this.isFiring) {
			vehicle.onFiringUpdate();
		}
		if (this.isLaunching) {
			vehicle.onLaunchingUpdate();
		}
		if (vehicle.level().isClientSide) {
			if (!vehicle.canAimPitch()) {
				this.clientTurretPitch = vehicle.localTurretPitch;
			}
			if (!vehicle.canAimPower()) {
				this.clientLaunchSpeed = vehicle.localLaunchPower;
			}
			if (!vehicle.canAimRotate()) {
				this.clientTurretYaw = vehicle.getYRot();
			}
		}
		if (!vehicle.canAimPower()) {
			vehicle.localLaunchPower = vehicle.currentLaunchSpeedPowerMax;
		}
		if (vehicle.canAimRotate()) {
			float diff = vehicle.getYRot() - vehicle.yRotO;
			vehicle.localTurretRotation += diff;
			this.clientTurretYaw += diff;
		}
	}

	/**
	 * get how many missiles can be fired at the current missileType and weight
	 * will return at least 1
	 */
	public int getMissileLaunchCount() {
		IAmmo ammo = vehicle.ammoHelper.getCurrentAmmoType();
		int missileCount = 1;
		if (ammo != null) {
			missileCount = (int) (vehicle.vehicleType.getMaxMissileWeight() / ammo.getAmmoWeight());
			if (missileCount < 1) {
				missileCount = 1;
			}
		}
		return missileCount;
	}

	/**
	 * gets the adjusted max missile velocity--adjusted by missile weight percentage of vehicleMaxMissileWeight
	 */
	public float getAdjustedMaxMissileVelocity() {
		float velocity = vehicle.currentLaunchSpeedPowerMax;
		IAmmo ammo = vehicle.ammoHelper.getCurrentAmmoType();
		if (ammo != null) {
			float missileWeight = ammo.getAmmoWeight();
			float maxWeight = vehicle.vehicleType.getMaxMissileWeight();
			if (missileWeight > maxWeight) {
				float totalWeight = missileWeight + maxWeight;
				float temp = maxWeight / totalWeight;
				temp *= 2;
				velocity *= temp;
			}
		}
		//  Config.logDebug("adj velocity: "+velocity);
		return velocity;
	}

	/**
	 * get accuracy after adjusting for rider (soldier)
	 */
	private float getAccuracyAdjusted() {
		//improvement over 1.12 (config improvements.crew_accuracy_per_level): 1.12 returns the weapon's accuracy alone
		if (vehicle.getFirstPassenger() instanceof net.shadowmage.ancientwarfare.npc.entity.NpcBase npc) {
			return 1.f - (1.f - vehicle.currentAccuracy) * crewScatterFactor(npc.getLevelingStats().getLevel());
		}
		return vehicle.currentAccuracy;
	}

	/*
	 * The share of the weapon's scatter that is left with an NPC crew member of the given level: every level removes
	 * crew_accuracy_per_level of what is left, down to 1 - crew_accuracy_max_reduction. 1 at level 0 or with the
	 * option at 0 (the 1.12 shot).
	 */
	public static float crewScatterFactor(int level) {
		double perLevel = AWVehicleStatics.improvements.crewAccuracyPerLevel;
		if (level <= 0 || perLevel <= 0) {
			return 1.f;
		}
		double left = Math.pow(1.0 - Math.min(1.0, perLevel), level);
		return (float) Math.max(1.0 - AWVehicleStatics.improvements.crewAccuracyMaxReduction, left);
	}

	/**
	 * if not already firing, this will initiate the launch sequence (phase 1 of 3).
	 * Called by this to start missileLaunch. (triggered from packet)
	 */
	private void initiateLaunchSequence() {
		if (!this.isFiring && !this.isLaunching && this.reloadingTicks <= 0) {
			this.isFiring = true;
			this.isLaunching = false;
			this.isReloading = false;
			vehicle.vehicleType.playFiringSound(vehicle);
		}
	}

	/**
	 * setReloading to finished. private for a reason... (return to phase 0)
	 */
	private void setFinishedReloading() {
		this.isFiring = false;
		this.isReloading = false;
		this.isLaunching = false;
		this.reloadingTicks = 0;
	}

	/**
	 * initiate actual launching of missiles (phase 2 of 3)
	 */
	public void startLaunching() {
		this.isFiring = false;
		this.isLaunching = true;
		this.isReloading = false;
	}

	/**
	 * finish the launching sequence, and begin reloading (phase 3 of 3)
	 */
	public void setFinishedLaunching() {
		this.isFiring = false;
		this.isReloading = true;
		this.isLaunching = false;
		this.reloadingTicks = vehicle.currentReloadTicks;
		vehicle.vehicleType.playReloadSound(vehicle);
		//improvement ammo_fallback: a type that ran out during the firing sequence is replaced now, not in the middle of it
		vehicle.ammoHelper.selectLoadedAmmoIfEmpty();
	}

	/* true from the trigger until the last missile of the sequence has left (a hwacha barrage is 36 launches) */
	public boolean isInFiringSequence() {
		return isFiring || isLaunching;
	}

	public void handleFireUpdate() {
		if (reloadingTicks <= 0 || vehicle.level().isClientSide) {

			boolean shouldFire = vehicle.ammoHelper.getCurrentAmmoCount() > 0 || vehicle.ammoHelper.doesntUseAmmo();
			if (shouldFire) {

				if (!vehicle.level().isClientSide) {
					NetworkHandler.sendToAllTracking(vehicle, new PacketFireUpdate(vehicle));
				}
				this.initiateLaunchSequence();
			}
		}
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public void updateAim(Optional<Float> pitch, Optional<Float> yaw, Optional<Float> power) {
		boolean sendReply = false;
		if (pitch.isPresent()) {
			sendReply = true;
			vehicle.localTurretDestPitch = pitch.get();
		}
		if (yaw.isPresent()) {
			sendReply = true;
			vehicle.localTurretDestRot = yaw.get();
		}
		if (power.isPresent()) {
			sendReply = true;
			vehicle.localLaunchPower = power.get();
		}
		if (!vehicle.level().isClientSide && sendReply) {
			NetworkHandler.sendToAllTracking(vehicle, new PacketAimUpdate(vehicle, pitch, yaw, power));
		}
	}

	public void handleFireInput() {
		if (isReadyToFire()) {
			NetworkHandler.sendToServer(new PacketFireUpdate(vehicle));
		}
	}

	public boolean isReadyToFire() {
		return !isFiring && !isLaunching && !isReloading && hasAmmo();
	}

	private boolean hasAmmo() {
		return vehicle.ammoHelper.doesntUseAmmo() || (vehicle.ammoHelper.getCurrentAmmoCount() > 0 && vehicle.ammoHelper.getCurrentAmmoType() != null);
	}

	public void handleAimKeyInput(float pitch, float yaw) {
		boolean pitchUpdated = false;
		boolean powerUpdated = false;
		boolean yawUpdated = false;
		if (vehicle.canAimPitch()) {
			float pitchTest = this.clientTurretPitch + pitch;
			if (pitchTest < vehicle.currentTurretPitchMin) {
				pitchTest = vehicle.currentTurretPitchMin;
			} else if (pitchTest > vehicle.currentTurretPitchMax) {
				pitchTest = vehicle.currentTurretPitchMax;
			}
			if (!MathUtils.epsilonEquals(pitchTest, this.clientTurretPitch)) {
				pitchUpdated = true;
				this.clientTurretPitch = pitchTest;
			}
		} else if (vehicle.canAimPower()) {
			float powerTest = clientLaunchSpeed + pitch;
			if (powerTest < 0) {
				powerTest = 0;
			} else if (powerTest > getAdjustedMaxMissileVelocity()) {
				powerTest = getAdjustedMaxMissileVelocity();
			}
			if (!MathUtils.epsilonEquals(clientLaunchSpeed, powerTest)) {
				powerUpdated = true;
				this.clientLaunchSpeed = powerTest;
			}
		}
		if (vehicle.canAimRotate()) {
			yawUpdated = true;
			this.clientTurretYaw += yaw;
		}

		if (powerUpdated || pitchUpdated || yawUpdated) {
			Optional<Float> turretPitch = pitchUpdated ? Optional.of(clientTurretPitch) : Optional.empty();
			Optional<Float> turretYaw = yawUpdated ? Optional.of(clientTurretYaw) : Optional.empty();
			Optional<Float> power = powerUpdated ? Optional.of(clientLaunchSpeed) : Optional.empty();

			NetworkHandler.sendToServer(new PacketAimUpdate(vehicle, turretPitch, turretYaw, power));
		}
	}

	/**
	 * CLIENT SIDE--used client side to update client desired pitch and yaw and send these to server/other clients...
	 */
	public void handleAimInput(Vec3 target) {
		boolean updated = false;
		boolean updatePitch = false;
		boolean updatePower = false;
		boolean updateYaw = false;
		Vec3 offset = vehicle.getMissileOffset();
		float x = (float) (vehicle.getX() + offset.x);
		float y = (float) (vehicle.getY() + offset.y);
		float z = (float) (vehicle.getZ() + offset.z);
		float tx = (float) (target.x - x);
		float ty = (float) (target.y - y);
		float tz = (float) (target.z - z);
		float range = Mth.sqrt(tx * tx + tz * tz);
		if (vehicle.canAimPitch()) {
			Tuple<Float, Float> angles = Trig.getLaunchAngleToHit(tx, ty, tz, vehicle.localLaunchPower);
			if (angles.getA().isNaN() || angles.getB().isNaN()) {
			} else if (Trig.isAngleBetween(angles.getB(), vehicle.currentTurretPitchMin, vehicle.currentTurretPitchMax)) {
				if (!MathUtils.epsilonEquals(this.clientTurretPitch, angles.getB())) {
					this.clientTurretPitch = angles.getB();
					updated = true;
					updatePitch = true;
				}
			} else if (Trig.isAngleBetween(angles.getA(), vehicle.currentTurretPitchMin, vehicle.currentTurretPitchMax)) {
				if (!MathUtils.epsilonEquals(clientTurretPitch, angles.getA())) {
					this.clientTurretPitch = angles.getA();
					updated = true;
					updatePitch = true;
				}
			}
		} else if (vehicle.canAimPower()) {
			float power = Trig.iterativeSpeedFinder(tx, ty, tz, vehicle.localTurretPitch + vehicle.getXRot(), TRAJECTORY_ITERATIONS_CLIENT,
					(vehicle.ammoHelper.getCurrentAmmoType() != null && vehicle.ammoHelper.getCurrentAmmoType().isRocket()));
			if (!MathUtils.epsilonEquals(clientLaunchSpeed, power) && power < getAdjustedMaxMissileVelocity()) {
				this.clientLaunchSpeed = power;
				updated = true;
				updatePower = true;
			}
		}
		if (vehicle.canAimRotate()) {
			float yaw = getAimYaw(target.x, target.z);
			if (!MathUtils.epsilonEquals(yaw, clientTurretYaw) && (vehicle.currentTurretRotationMax >= 180 || Trig
					.isAngleBetween(yaw, vehicle.localTurretRotationHome - vehicle.currentTurretRotationMax,
							vehicle.localTurretRotationHome + vehicle.currentTurretRotationMax))) {
				if (Math.abs(yaw - clientTurretYaw) > 0.25f) {
					this.clientTurretYaw = yaw;
					updated = true;
					updateYaw = true;
				}
			}
		}

		if (updated) {
			this.clientHitRange = range;
			Optional<Float> turretPitch = updatePitch ? Optional.of(clientTurretPitch) : Optional.empty();
			Optional<Float> turretYaw = updateYaw ? Optional.of(clientTurretYaw) : Optional.empty();
			Optional<Float> power = updatePower ? Optional.of(clientLaunchSpeed) : Optional.empty();
			NetworkHandler.sendToServer(new PacketAimUpdate(vehicle, turretPitch, turretYaw, power));
		}
	}

	public boolean isAtTarget() {
		return isAtTarget(0.35f);
	}

	private boolean isAtTarget(float range) {
		float yaw = Trig.wrapTo360(vehicle.localTurretRotation);
		float dest = Trig.wrapTo360(vehicle.localTurretDestRot);

		return MathUtils.epsilonEquals(vehicle.localTurretDestPitch, vehicle.localTurretPitch) && Math.abs(yaw - dest) < range;
	}

	public boolean isNearTarget() {
		return isAtTarget(5f);
	}

	public void handleSoldierTargetInput(double targetX, double targetY, double targetZ) {
		boolean updated = false;
		boolean updatePitch = false;
		boolean updatePower = false;
		boolean updateYaw = false;
		Vec3 offset = vehicle.getMissileOffset();
		float x = (float) (vehicle.getX() + offset.x);
		float y = (float) (vehicle.getY() + offset.y);
		float z = (float) (vehicle.getZ() + offset.z);
		float tx = (float) (targetX - x);
		float ty = (float) (targetY - y);
		float tz = (float) (targetZ - z);
		if (vehicle.canAimPitch()) {
			Tuple<Float, Float> angles = Trig.getLaunchAngleToHit(tx, ty, tz, vehicle.localLaunchPower);
			if (angles.getA().isNaN() || angles.getB().isNaN()) {
			} else if (Trig.isAngleBetween(angles.getB(), vehicle.currentTurretPitchMin, vehicle.currentTurretPitchMax)) {
				if (!MathUtils.epsilonEquals(vehicle.localTurretDestPitch, angles.getB())) {
					vehicle.localTurretDestPitch = angles.getB();
					updated = true;
					updatePitch = true;
				}
			} else if (Trig.isAngleBetween(angles.getA(), vehicle.currentTurretPitchMin, vehicle.currentTurretPitchMax)) {
				if (!MathUtils.epsilonEquals(vehicle.localTurretDestPitch, angles.getA())) {
					vehicle.localTurretDestPitch = angles.getA();
					updated = true;
					updatePitch = true;
				}
			}
		} else if (vehicle.canAimPower()) {
			float power = Trig.iterativeSpeedFinder(tx, ty, tz, vehicle.localTurretPitch + vehicle.getXRot(), TRAJECTORY_ITERATIONS_CLIENT,
					(vehicle.ammoHelper.getCurrentAmmoType() != null && vehicle.ammoHelper.getCurrentAmmoType().isRocket()));
			if (!MathUtils.epsilonEquals(vehicle.localLaunchPower, power) && power < getAdjustedMaxMissileVelocity()) {
				this.vehicle.localLaunchPower = power;
				updated = true;
				updatePower = true;
			}
		}
		if (vehicle.canAimRotate()) {
			float yaw = getAimYaw(targetX, targetZ);
			if (!MathUtils.epsilonEquals(yaw, vehicle.localTurretDestRot) && (vehicle.currentTurretRotationMax >= 180 || Trig
					.isAngleBetween(yaw, vehicle.localTurretRotationHome - vehicle.currentTurretRotationMax,
							vehicle.localTurretRotationHome + vehicle.currentTurretRotationMax))) {
				this.vehicle.localTurretDestRot = yaw;
				updated = true;
				updateYaw = true;
			}
		}
		if (updated && !vehicle.level().isClientSide) {
			Optional<Float> turretPitch = updatePitch ? Optional.of(vehicle.localTurretDestPitch) : Optional.empty();
			Optional<Float> turretYaw = updateYaw ? Optional.of(vehicle.localTurretDestRot) : Optional.empty();
			Optional<Float> power = updatePower ? Optional.of(vehicle.localLaunchPower) : Optional.empty();
			NetworkHandler.sendToAllTracking(vehicle, new PacketAimUpdate(vehicle, turretPitch, turretYaw, power));
		}
	}

	private float getAimYaw(double targetX, double targetZ) {
		Vec3 offset = vehicle.getMissileOffset();
		float vecX = (float) (vehicle.getX() + (vehicle.canTurretTurn() ? offset.x : 0) - targetX);
		float vecZ = (float) (vehicle.getZ() + (vehicle.canTurretTurn() ? offset.z : 0) - targetZ);
		//noinspection SuspiciousNameCombination
		return Trig.wrapTo360(Trig.toDegrees((float) Math.atan2(vecX, vecZ)));
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("rt", reloadingTicks);
		tag.putBoolean("f", this.isFiring);
		tag.putBoolean("r", this.isReloading);
		tag.putBoolean("l", this.isLaunching);
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		this.reloadingTicks = tag.getInt("rt");
		this.isFiring = tag.getBoolean("f");
		this.isReloading = tag.getBoolean("r");
		this.isLaunching = tag.getBoolean("l");
	}

	private static final float NEGLIGIBLE_ANGLE_DIFFERENCE = 0.35f;

	public boolean isAimedAt(ITarget target) {
		return isPitchPointedAt(target) && isYawPointedAt(target) && isPowerSetToPointAt(target);
	}

	private boolean isYawPointedAt(ITarget target) {
		float minYaw = getAimYaw(target.getBoundigBox().minX, target.getBoundigBox().minZ);
		float maxYaw = getAimYaw(target.getBoundigBox().maxX, target.getBoundigBox().maxZ);

		if (minYaw > maxYaw) {
			float temp = minYaw;
			minYaw = maxYaw;
			maxYaw = temp;
		}

		float vehicleRotation = vehicle.canAimRotate() ? vehicle.localTurretRotation : vehicle.getYRot();

		return Trig.isAngleBetween(vehicleRotation, minYaw, maxYaw);
	}

	private boolean isPitchPointedAt(ITarget target) {
		if (!vehicle.canAimPitch()) {
			return true;
		}

		Vec3 offset = vehicle.getMissileOffset();
		float targetX = (float) target.getBoundigBox().minX - (float) (vehicle.getX() + offset.x);
		float targetY = (float) target.getBoundigBox().minY - (float) (vehicle.getY() + offset.y);
		float targetZ = (float) target.getBoundigBox().minZ - (float) (vehicle.getZ() + offset.z);

		Tuple<Float, Float> anglesMin = Trig.getLaunchAngleToHit(targetX, targetY, targetZ, vehicle.localLaunchPower);
		//noinspection SimplifiableIfStatement
		if (Math.abs(anglesMin.getB() - vehicle.localTurretPitch) < NEGLIGIBLE_ANGLE_DIFFERENCE || Math
				.abs(anglesMin.getA() - vehicle.localTurretPitch) < NEGLIGIBLE_ANGLE_DIFFERENCE) {
			return true;
		}

		targetX = (float) target.getBoundigBox().maxX - (float) (vehicle.getX() + offset.x);
		targetY = (float) target.getBoundigBox().maxY - (float) (vehicle.getY() + offset.y);
		targetZ = (float) target.getBoundigBox().maxZ - (float) (vehicle.getZ() + offset.z);

		Tuple<Float, Float> anglesMax = Trig.getLaunchAngleToHit(targetX, targetY, targetZ, vehicle.localLaunchPower);
		//noinspection SimplifiableIfStatement
		if (Math.abs(anglesMax.getB() - vehicle.localTurretPitch) < NEGLIGIBLE_ANGLE_DIFFERENCE || Math
				.abs(anglesMax.getA() - vehicle.localTurretPitch) < NEGLIGIBLE_ANGLE_DIFFERENCE) {
			return true;
		}

		return Trig.isAngleBetween(vehicle.localTurretPitch, anglesMin.getA(), anglesMax.getA())
				|| Trig.isAngleBetween(vehicle.localTurretPitch, anglesMin.getB(), anglesMax.getB());
	}

	private boolean isPowerSetToPointAt(ITarget target) {
		if (!vehicle.canAimPower()) {
			return true;
		}
		Vec3 offset = vehicle.getMissileOffset();
		float x = (float) (vehicle.getX() + offset.x);
		float y = (float) (vehicle.getY() + offset.y);
		float z = (float) (vehicle.getZ() + offset.z);

		float minDistY;
		float maxDistY;
		if (Math.abs(target.getBoundigBox().minY - y) < Math.abs(target.getBoundigBox().maxY - y)) {
			minDistY = (float) (target.getBoundigBox().minY - y);
			maxDistY = (float) (target.getBoundigBox().maxY - y);
		} else {
			minDistY = (float) (target.getBoundigBox().maxY - y);
			maxDistY = (float) (target.getBoundigBox().minY - y);
		}

		float powerMin = getPowerFor((float) target.getBoundigBox().minX - x, minDistY, (float) target.getBoundigBox().minZ - z);
		float powerMax = getPowerFor((float) target.getBoundigBox().maxX - x, maxDistY, (float) target.getBoundigBox().maxZ - z);

		if (powerMin > powerMax) {
			float temp = powerMin;
			powerMin = powerMax;
			powerMax = temp;
		}

		return vehicle.localLaunchPower >= powerMin && vehicle.localLaunchPower <= powerMax;
	}

	private float getPowerFor(float distX, float distY, float distZ) {
		return Trig.iterativeSpeedFinder(distX, distY, distZ, vehicle.localTurretPitch + vehicle.getXRot(),
						TRAJECTORY_ITERATIONS_CLIENT, (vehicle.ammoHelper.getCurrentAmmoType() != null && vehicle.ammoHelper.getCurrentAmmoType().isRocket()));
	}

	public float getAimYaw(ITarget target) {
		return getAimYaw(target.getX(), target.getZ());
	}

	/* improvement crew_leads_targets: how far ahead of a target a crew aims at most, and how often the estimate is refined */
	public static final double MAX_LEAD_BLOCKS = 12.0D;
	private static final int LEAD_ITERATIONS = 3;

	/*
	 * Improvement over 1.12 (config improvements.crew_leads_targets): 1.12 aims at where the target is. Here the
	 * target's velocity (measured from its positions) times the flight time of the selected ammunition gives the
	 * point to aim at; the flight time depends on the distance to that point, so it is refined three times. The lead
	 * is capped at 12 blocks, and the target itself is returned when the predicted point is out of range.
	 */
	public ITarget leadTarget(ITarget target) {
		if (!AWVehicleStatics.improvements.crewLeadsTargets || target == null) {
			return target;
		}
		Vec3 velocity = target.getVelocity(vehicle.level().getGameTime());
		if (velocity.lengthSqr() < 1.0E-6D) {
			return target;
		}
		Vec3 offset = vehicle.getMissileOffset();
		Vec3 from = new Vec3(vehicle.getX() + offset.x, vehicle.getY() + offset.y, vehicle.getZ() + offset.z);
		Vec3 now = new Vec3(target.getX(), target.getY(), target.getZ());
		Vec3 lead = Vec3.ZERO;
		for (int i = 0; i < LEAD_ITERATIONS; i++) {
			Vec3 aim = now.add(lead);
			double horizontal = Math.sqrt((aim.x - from.x) * (aim.x - from.x) + (aim.z - from.z) * (aim.z - from.z));
			lead = velocity.scale(getFlightTicks(horizontal));
			if (lead.length() > MAX_LEAD_BLOCKS) {
				lead = lead.normalize().scale(MAX_LEAD_BLOCKS);
			}
		}
		Vec3 aim = now.add(lead);
		double distance = from.distanceTo(aim);
		if (vehicle.getEffectiveRange((float) (aim.y - vehicle.getY())) < distance) {
			return target;
		}
		return new LeadTarget(target, lead);
	}

	/*
	 * Ticks a missile of the selected ammunition needs for a horizontal distance at the current aim: constant
	 * horizontal speed cos(pitch) * power * 0.05 per tick (MissileBase has no drag); a rocket first accelerates for
	 * its burn time (the model of Trig.iterativeSpeedFinder / getEffectiveRange).
	 */
	public float getFlightTicks(double horizontalDistance) {
		IAmmo ammo = vehicle.ammoHelper.getCurrentAmmoType();
		float pitch = vehicle.canAimPitch() ? vehicle.localTurretDestPitch : vehicle.localTurretPitch + vehicle.getXRot();
		float power = Math.max(1.f, Math.min(vehicle.localLaunchPower, getAdjustedMaxMissileVelocity()));
		float cos = Math.max(0.05f, Trig.cosDegrees(pitch));
		if (ammo != null && ammo.isRocket()) {
			int burn = (int) (power * Trig.ROCKET_BURN_TIME_FACTOR);
			float acceleration = cos * Trig.ROCKET_ACCELERATION_FACTOR;
			float speed = acceleration;
			float travelled = 0;
			int ticks = 0;
			while (burn > 0 && travelled < horizontalDistance) {
				travelled += speed;
				speed += acceleration;
				burn--;
				ticks++;
			}
			return travelled >= horizontalDistance ? ticks : ticks + (float) ((horizontalDistance - travelled) / Math.max(0.05f, speed));
		}
		return (float) (horizontalDistance / (cos * power * 0.05f));
	}

	/* a target shifted to the point where it is expected when the missile arrives */
	private static final class LeadTarget implements ITarget {
		private final ITarget target;
		private final Vec3 lead;

		private LeadTarget(ITarget target, Vec3 lead) {
			this.target = target;
			this.lead = lead;
		}

		@Override
		public double getX() {
			return target.getX() + lead.x;
		}

		@Override
		public double getY() {
			return target.getY() + lead.y;
		}

		@Override
		public double getZ() {
			return target.getZ() + lead.z;
		}

		@Override
		public net.minecraft.world.phys.AABB getBoundigBox() {
			return target.getBoundigBox().move(lead);
		}

		@Override
		public boolean exists(net.minecraft.world.level.Level world) {
			return target.exists(world);
		}

		@Override
		public Vec3 getVelocity(long gameTime) {
			return target.getVelocity(gameTime);
		}
	}
}
