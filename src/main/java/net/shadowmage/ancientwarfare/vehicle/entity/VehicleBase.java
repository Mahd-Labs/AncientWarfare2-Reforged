package net.shadowmage.ancientwarfare.vehicle.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.shadowmage.ancientwarfare.core.owner.IOwnable;
import net.shadowmage.ancientwarfare.core.owner.Owner;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.core.util.MathUtils;
import net.shadowmage.ancientwarfare.core.util.Trig;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFactionSiegeEngineer;
import net.shadowmage.ancientwarfare.npc.entity.vehicle.ITarget;
import net.shadowmage.ancientwarfare.npc.xmod.IVehicle;
import net.shadowmage.ancientwarfare.vehicle.VehicleVarHelpers.DummyVehicleHelper;
import net.shadowmage.ancientwarfare.vehicle.entity.materials.IVehicleMaterial;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleType;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleAmmoHelper;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringHelper;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringVarsHelper;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleMoveHelper;
import net.shadowmage.ancientwarfare.vehicle.helpers.VehicleUpgradeHelper;
import net.shadowmage.ancientwarfare.vehicle.init.AWVehicleEntities;
import net.shadowmage.ancientwarfare.vehicle.inventory.VehicleInventory;
import net.shadowmage.ancientwarfare.vehicle.missiles.IAmmo;
import net.shadowmage.ancientwarfare.vehicle.registry.VehicleRegistry;

import javax.annotation.Nullable;
import java.util.List;

/*
 * Port notes (reduced module: stationary siege weapons, see docs/DETAILS.md, "What is in, and what is not"):
 * - No driving: the pathing package (Navigator, PathWorldAccessEntity, IPathableEntity) and forward movement are
 *   dropped. VehicleMoveHelper is reduced to what a stand needs: gravity and turning in place (1.12 stands are
 *   "drivable" with a forward speed of 0, the left / right keys and the NPC aim AI turn them).
 * - 1.12 synchronised position and rotation with its own PacketVehicleMove and ignored the vanilla packets
 *   (setPositionAndRotationDirect / setVelocity were empty). The port uses the vanilla entity tracker (update
 *   interval 3 like 1.12); lerpTo applies the received values at once.
 * - getControllingPassenger() is NOT overridden: in 1.21 a controlling player makes the client authoritative for the
 *   vehicle's movement. The 1.12 uses of getControllingPassenger() are getFirstPassenger() here.
 * - Entity size comes from the vehicle type: getDimensions() + refreshDimensions() replace setSize().
 * - Dismounting: 1.12 watched player.isSneaking() and moved the rider to a free block (handleDismount). 1.21 dismounts
 *   on sneak by itself; the same block search is in getDismountLocationForPassenger. The 1.12
 *   capabilities.allowFlying toggle while riding (anti fly-kick workaround) is not needed and dropped.
 * - updatePassenger -> positionRider. The 1.12 hacks that changed the rider's height / bounding box for sitting riders
 *   are dropped (the sitting pose comes from shouldRiderSit()).
 * - setDead() dropping the inventories when health <= 0 -> done in hurt() before discard().
 * - Upgrades and armour are dropped (VehicleUpgradeHelper keeps only the damage scaling and stat reset), the storage
 *   inventory and its item handler capability too (every ported type has a storage size of 0).
 * - Synched data FORWARD_INPUT / STRAFE_INPUT were registered but never used in 1.12: dropped.
 * - doBlockCollisions / onInsideBlock (breaking lily pads under boats): dropped with the boats.
 */
public class VehicleBase extends Entity implements IEntityWithComplexSpawn, IMissileHitCallback, IOwnable, IVehicle {

	private static final EntityDataAccessor<Float> VEHICLE_HEALTH = SynchedEntityData.defineId(VehicleBase.class, EntityDataSerializers.FLOAT);

	/**
	 * these are the current max stats.  set from setVehicleType().
	 * these are local cached bases, after application of material factors
	 * should not be altered at all after vehicle is first initialized
	 */
	public float baseForwardSpeed;
	public float baseStrafeSpeed;
	public float basePitchMin;
	public float basePitchMax;
	private float baseTurretRotationMax;
	private float baseLaunchSpeedMax;
	public float baseHealth = 100;
	private float baseAccuracy = 1.f;
	public float baseWeight = 1000;//kg
	private int baseReloadTicks = 100;
	private float baseGenericResist = 0.f;
	private float baseFireResist = 0.f;
	private float baseExplosionResist = 0.f;
	private int hurtInvulTicks = 0;

	/**
	 * local current stats, fully updated and modified from upgrades/etc. should not be altered aside from
	 * upgrades/armor
	 */
	public float currentForwardSpeedMax = 0.42f;
	public float currentStrafeSpeedMax = 2.0f;

	/**
	 * how many ticks is a reloadCycle, at current upgrade status?
	 * the currentReload status is stored in firingHelper
	 */
	public int currentReloadTicks = 100;
	public float currentTurretPitchMin = 0.f;
	public float currentTurretPitchMax = 90.f;
	public float currentLaunchSpeedPowerMax = 32.321f;
	public float currentGenericResist = 0.f;
	public float currentFireResist = 0.f;
	public float currentExplosionResist = 0.f;
	public float currentWeight = 1000.f;
	public float currentTurretPitchSpeed = 0.f;
	public float currentTurretYawSpeed = 0.f;
	public float currentAccuracy = 1.f;
	public float currentTurretRotationMax = 45.f;

	/**
	 * local variables, may be altered by input/etc...
	 */
	public float localTurretRotationHome = 0.f;
	public float localTurretRotation = 0.f;
	public float localTurretDestRot = 0.f;
	private float localTurretRotInc = 1.f;
	public float localTurretPitch = 45.f;
	public float localTurretDestPitch = 45.f;
	private float localTurretPitchInc = 1.f;
	public float localLaunchPower = 31.321f;

	/**
	 * used to determine if it should allow interaction (setup time on vehicle placement)
	 */
	private boolean isSettingUp = false;

	/**
	 * set client-side when incoming damage is taken
	 */
	public int hitAnimationTicks = 0;

	private NpcBase assignedRider = null;

	/**
	 * complex stat tracking helpers, move, ammo, upgrades, general stats
	 */
	public VehicleAmmoHelper ammoHelper;
	public VehicleUpgradeHelper upgradeHelper;
	public VehicleMoveHelper moveHelper;
	public VehicleFiringHelper firingHelper;
	public VehicleFiringVarsHelper firingVarsHelper;
	public VehicleInventory inventory;
	public IVehicleType vehicleType = VehicleRegistry.CATAPULT_STAND_FIXED;//set to dummy vehicle so it is never null...
	public int vehicleMaterialLevel = 0;//the current material level of this vehicle. should be read/set prior to calling updateBaseStats
	private Owner owner = Owner.EMPTY;

	public VehicleBase(EntityType<? extends VehicleBase> type, Level level) {
		super(type, level);
		this.upgradeHelper = new VehicleUpgradeHelper(this);
		this.moveHelper = new VehicleMoveHelper(this);
		this.ammoHelper = new VehicleAmmoHelper(this);
		this.firingHelper = new VehicleFiringHelper(this);
		this.firingVarsHelper = new DummyVehicleHelper(this);
		this.inventory = new VehicleInventory(this);
		this.setOnGround(false);
		/*
		 * port: a vehicle created without a type (summon command, showcase) is a catapult stand, the 1.12 field default;
		 * 1.12 left such an entity half initialised (no inventories).
		 */
		this.setVehicleType(VehicleRegistry.CATAPULT_STAND_FIXED, 0);
		this.setInitialHealth();
	}

	public VehicleBase(Level level) {
		this(AWVehicleEntities.VEHICLE.get(), level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(VEHICLE_HEALTH, 100f);
	}

	@Override
	public ItemStack getPickedResult(HitResult target) {
		return this.vehicleType.getStackForLevel(vehicleMaterialLevel);
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		/* 1.12 getEyeHeight() returned 1.6 ("overriden to help with vision checks for vehicles") */
		return vehicleType == null ? super.getDimensions(pose) : EntityDimensions.scalable(vehicleType.getWidth(), vehicleType.getHeight()).withEyeHeight(1.6F);
	}

	public void setHealth(float health) {
		if (health > this.baseHealth) {
			health = this.baseHealth;
		}
		entityData.set(VEHICLE_HEALTH, health);
	}

	public boolean canTurretTurn() {
		return !MathUtils.epsilonEquals(baseTurretRotationMax, 0);
	}

	public float getHealth() {
		return entityData.get(VEHICLE_HEALTH);
	}

	public void setVehicleType(IVehicleType vehicle, int materialLevel) {
		this.vehicleType = vehicle;
		this.vehicleMaterialLevel = materialLevel;
		VehicleFiringVarsHelper help = vehicle.getFiringVarsHelper(this);
		if (help != null) {
			this.firingVarsHelper = help;
		}
		this.refreshDimensions();
		this.ammoHelper.clearUseableAmmo();
		for (IAmmo ammo : vehicleType.getValidAmmoTypes()) {
			this.ammoHelper.addUseableAmmo(ammo);
		}
		this.inventory.setInventorySizes(vehicle.getAmmoBaySize());
		this.updateBaseStats();
		this.resetCurrentStats();

		if (this.localTurretPitch < currentTurretPitchMin) {
			this.localTurretPitch = currentTurretPitchMin;
		} else if (this.localTurretPitch > currentTurretPitchMax) {
			this.localTurretPitch = currentTurretPitchMax;
		}
		this.localLaunchPower = this.firingHelper.getAdjustedMaxMissileVelocity();
		if (!this.canAimRotate()) {
			this.localTurretRotation = this.getYRot();
		}
	}

	private int setupTicks = 0;

	public void setSetupState(boolean state, int ticks) {
		this.isSettingUp = state;
		if (state) {
			setupTicks = ticks;
		} else {
			setupTicks = 0;
		}
	}

	public void setInitialHealth() {
		this.setHealth(this.baseHealth);
	}

	private void updateBaseStats() {
		IVehicleMaterial material = vehicleType.getMaterialType();
		int level = this.vehicleMaterialLevel;
		baseForwardSpeed = vehicleType.getBaseForwardSpeed() * material.getSpeedForwardFactor(level);
		baseStrafeSpeed = vehicleType.getBaseStrafeSpeed() * material.getSpeedStrafeFactor(level);
		basePitchMin = vehicleType.getBasePitchMin();
		basePitchMax = vehicleType.getBasePitchMax();
		baseTurretRotationMax = vehicleType.getBaseTurretRotationAmount();
		baseLaunchSpeedMax = vehicleType.getBaseMissileVelocityMax();
		baseHealth = vehicleType.getBaseHealth() * material.getHPFactor(level);
		baseAccuracy = vehicleType.getBaseAccuracy() * material.getAccuracyFactor(level);
		baseWeight = vehicleType.getBaseWeight() * material.getWeightFactor(level);
		baseExplosionResist = 0.f;
		baseFireResist = 0.f;
		baseGenericResist = 0.f;
		if (getHealth() > baseHealth) {
			this.setHealth(baseHealth);
		}
	}

	@Override
	protected Entity.MovementEmission getMovementEmission() {
		return Entity.MovementEmission.NONE;
	}

	/**
	 * return an itemStack tagged appropriately for this vehicle
	 */
	private ItemStack getItemForVehicle() {
		ItemStack stack = this.vehicleType.getStackForLevel(vehicleMaterialLevel);
		CompoundTag tag = ItemTools.getTag(stack);
		CompoundTag spawnData = tag.getCompound("spawnData");
		spawnData.putFloat("health", getHealth());
		tag.put("spawnData", spawnData);
		ItemTools.setTag(stack, tag);
		return stack;
	}

	private float getHorizontalMissileOffset() {
		return this.vehicleType.getMissileHorizontalOffset();
	}

	private float getVerticalMissileOffset() {
		return this.vehicleType.getMissileVerticalOffset();
	}

	private float getForwardsMissileOffset() {
		return this.vehicleType.getMissileForwardsOffset();
	}

	public boolean isAimable() {
		return !this.isSettingUp && vehicleType.isCombatEngine();
	}

	public boolean canAimRotate() {
		return !this.isSettingUp && vehicleType.canAdjustYaw();
	}

	public boolean canAimPitch() {
		return !this.isSettingUp && vehicleType.canAdjustPitch();
	}

	public boolean canAimPower() {
		return !this.isSettingUp && vehicleType.canAdjustPower();
	}

	/**
	 * used by inputHelper to determine if it should check movement input keys and send info to server..
	 * (port: "drivable" only means "can be turned in place", see the class comment)
	 */
	@Override
	public boolean isDrivable() {
		return !this.isSettingUp && vehicleType.isDrivable();
	}

	public boolean isMountable() {
		return !this.isSettingUp && vehicleType.isMountable();
	}

	private float getRiderForwardOffset() {
		return vehicleType.getRiderForwardsOffset();
	}

	public float getRiderVerticalOffset() {
		return vehicleType.getRiderVerticalOffest();
	}

	private float getRiderHorizontalOffset() {
		return vehicleType.getRiderHorizontalOffset();
	}

	/**
	 * should return the maximum range allowed in order to hit a point at a given vertical offset
	 * will vary by vehicle type (power/angle/missile offset) and current ammo selection
	 */
	@Override
	public float getEffectiveRange(float verticalOffset) {
		float angle;
		if (currentTurretPitchMin < 45 && currentTurretPitchMax > 45)//if the angle stradles 45, return 45
		{
			angle = 45;
		} else if (currentTurretPitchMin < 45 && currentTurretPitchMax < 45)//else if both are below 45, get the largest
		{
			angle = currentTurretPitchMax;
		} else {
			angle = currentTurretPitchMin;//else get the lowest
		}
		return getEffectiveRange(verticalOffset, angle, firingHelper.getAdjustedMaxMissileVelocity(), 0, ammoHelper.getCurrentAmmoType() != null && ammoHelper.getCurrentAmmoType().isRocket());
	}

	private float getEffectiveRange(float y, float angle, float velocity, int maxIterations, boolean rocket) {
		float motX = Trig.sinDegrees(angle) * velocity * 0.05f;
		float motY = Trig.cosDegrees(angle) * velocity * 0.05f;
		float rocketX = 0;
		float rocketY = 0;
		if (rocket) {
			int rocketBurnTime = (int) (velocity * Trig.ROCKET_BURN_TIME_FACTOR);
			float motX0 = (motX / (velocity * 0.05f)) * Trig.ROCKET_ACCELERATION_FACTOR;
			float motY0 = (motY / (velocity * 0.05f)) * Trig.ROCKET_ACCELERATION_FACTOR;
			motX = motX0;
			motY = motY0;
			while (rocketBurnTime > 0) {
				rocketX += motX;
				rocketY += motY;
				rocketBurnTime--;
				motX += motX0;
				motY += motY0;
			}
			y -= rocketY;
		}
		motX *= 20.f;
		motY *= 20.f;
		float gravity = 9.81f;
		float t = motY / gravity;
		float tQ = Mth.sqrt(((motY * motY) / (gravity * gravity)) - ((2 * y) / gravity));
		float tPlus = t + tQ;
		float tMinus = t - tQ;
		t = tPlus > tMinus ? tPlus : tMinus;
		return (motX * t) + rocketX;
	}

	/**
	 * get a fully translated offset position for missile spawn for the current aim and vehicle params
	 */
	@Override
	public Vec3 getMissileOffset() {
		float x1 = this.vehicleType.getTurretPosX();
		float y1 = this.vehicleType.getTurretPosY();
		float z1 = this.vehicleType.getTurretPosZ();
		float angle = 0;
		float len = 0;
		if (x1 != 0 || z1 != 0) {
			angle = Trig.toDegrees((float) Math.atan2(z1, x1));
			len = Mth.sqrt(x1 * x1 + z1 * z1);
			angle += this.getYRot();
			x1 = Trig.cosDegrees(angle) * len;
			z1 = -Trig.sinDegrees(angle) * len;
		}

		float x = this.getHorizontalMissileOffset();
		float y = this.getVerticalMissileOffset();
		float z = this.getForwardsMissileOffset();
		if (x != 0 || z != 0 || y != 0) {
			angle = Trig.toDegrees((float) Math.atan2(z, x));
			len = Mth.sqrt(x * x + z * z + y * y);
			angle += this.localTurretRotation;
			x = Trig.cosDegrees(angle) * Trig.sinDegrees(localTurretPitch + getXRot()) * len;
			z = -Trig.sinDegrees(angle) * Trig.sinDegrees(localTurretPitch + getXRot()) * len;
			y = Trig.cosDegrees(localTurretPitch + getXRot()) * len;
		}
		x += x1;
		z += z1;
		y += y1;
		return new Vec3(x, y, z);
	}

	/**
	 * called on every tick that the vehicle is 'firing' to update the firing animation and to call
	 * launchMissile when animation has reached launch point
	 */
	public void onFiringUpdate() {
		this.firingVarsHelper.onFiringUpdate();
	}

	/**
	 * called every tick after the vehicle has fired, until reload timer is complete, to update animations
	 */
	public void onReloadUpdate() {
		this.firingVarsHelper.onReloadUpdate();
	}

	/**
	 * called every tick after startLaunching() is called, until setFinishedLaunching() is called...
	 */
	public void onLaunchingUpdate() {
		this.firingVarsHelper.onLaunchingUpdate();
	}

	/**
	 * reset all upgradeable stats back to the base for this vehicle
	 */
	public void resetCurrentStats() {
		this.currentForwardSpeedMax = this.baseForwardSpeed;
		this.currentStrafeSpeedMax = this.baseStrafeSpeed;
		this.currentTurretPitchMin = this.basePitchMin;
		this.currentTurretPitchMax = this.basePitchMax;
		this.currentTurretRotationMax = this.baseTurretRotationMax;
		this.currentReloadTicks = this.baseReloadTicks;
		this.currentLaunchSpeedPowerMax = this.baseLaunchSpeedMax;
		this.currentExplosionResist = this.baseExplosionResist;
		this.currentFireResist = this.baseFireResist;
		this.currentGenericResist = this.baseGenericResist;
		this.currentWeight = this.baseWeight;
		this.currentAccuracy = this.baseAccuracy;
	}

	@Override
	public void tick() {
		super.tick();
		this.updateTurretPitch();
		this.updateTurretRotation();
		this.moveHelper.onUpdate();
		this.firingHelper.onTick();
		this.firingVarsHelper.onTick();
		if (this.hitAnimationTicks > 0) {
			this.hitAnimationTicks--;
		}
		if (this.isSettingUp) {
			this.setupTicks--;
			if (this.setupTicks <= 0) {
				this.isSettingUp = false;
			}
		}
		if (this.hurtInvulTicks > 0) {
			this.hurtInvulTicks--;
		}
		if (this.assignedRider != null) {
			if (!assignedRider.isAlive() || assignedRider.getVehicle() != this || (this.distanceToSqr(assignedRider) > (AWNPCStatics.npcActionRange * AWNPCStatics.npcActionRange))) {
				this.assignedRider = null;
			}
		}
	}

	@Override
	public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
		this.setPos(x, y, z);
		//the yaw is steered by PacketVehicleMove (exact value, eased over 4 ticks as in 1.12) once one arrived
		if (!this.moveHelper.hasYawFromServer()) {
			this.setRot(yRot, xRot);
		}
	}

	/*
	 * 1.12 handleDismount: first block around the vehicle with a solid top (or water) and two free blocks above
	 */
	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity rider) {
		float width = getBbWidth();
		int xMin = Mth.floor(this.getX() - width / 2);
		int zMin = Mth.floor(this.getZ() - width / 2);
		int yMin = Mth.floor(getY()) - 2;
		Level world = level();
		for (int y = yMin; y <= yMin + 3; y++) {
			for (int x = xMin; x <= xMin + (int) width; x++) {
				for (int z = zMin; z <= zMin + (int) width; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = world.getBlockState(pos);
					if (state.isFaceSturdy(world, pos, Direction.UP) || state.getFluidState().is(FluidTags.WATER)) {
						if (world.isEmptyBlock(pos.above()) && world.isEmptyBlock(pos.above().above())) {
							return new Vec3(x + 0.5d, y + 1, z + 0.5d);
						}
					}
				}
			}
		}
		return super.getDismountLocationForPassenger(rider);
	}

	private void updateTurretPitch() {
		float prevPitch = this.localTurretPitch;
		if (!Trig.isAngleBetween(localTurretPitch, currentTurretPitchMin, currentTurretPitchMax)) {
			localTurretPitch = currentTurretPitchMin;
		}

		if (!Trig.isAngleBetween(localTurretDestPitch, currentTurretPitchMin, currentTurretPitchMax)) {
			localTurretDestPitch = currentTurretPitchMin;
		}

		if (!canAimPitch()) {
			localTurretDestPitch = localTurretPitch;
		}

		if (!Trig.anglesEqual(localTurretPitch, localTurretDestPitch)) {
			if (Math.abs(Trig.getAngleDiffSigned(localTurretDestPitch, localTurretPitch)) < localTurretPitchInc) {
				localTurretPitch = localTurretDestPitch;
			} else {
				localTurretPitch += Trig.getAngleDiffSigned(localTurretPitch, localTurretDestPitch) > 0 ? localTurretPitchInc : -localTurretPitchInc;
			}
		}
		this.currentTurretPitchSpeed = prevPitch - this.localTurretPitch;
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		if (passenger instanceof NpcFactionSiegeEngineer) {
			currentTurretPitchMin = vehicleType.getBasePitchMin() - 4 * 3;
			currentTurretPitchMax = vehicleType.getBasePitchMax() + 4 * 3;
		}
		/*
		 * (port defect) the client keeps its own aim values (clientTurretPitch / Yaw / LaunchSpeed) and only sends
		 * a change. An aim packet sent in the tick the player got off is refused by the server (sender check of the
		 * port), so after mounting again the overlay showed a value the server never took, and the client did not send
		 * it again because it had not changed. The player's aim starts from the weapon's server-confirmed values.
		 */
		if (level().isClientSide && passenger instanceof Player player && player.isLocalPlayer()) {
			firingHelper.clientLaunchSpeed = localLaunchPower;
			firingHelper.clientTurretPitch = localTurretDestPitch;
			firingHelper.clientTurretYaw = localTurretDestRot;
		}
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (passenger instanceof NpcFactionSiegeEngineer) {
			upgradeHelper.updateUpgradeStats();
		}
	}

	private void updateTurretRotation() {
		float prevYaw = this.localTurretRotation;
		this.localTurretRotationHome = Trig.wrapTo360(this.getYRot());
		if (!canAimRotate()) {
			localTurretRotation = Trig.wrapTo360(this.getYRot());
			localTurretDestRot = localTurretRotation;
		}
		if (Math.abs(localTurretDestRot - localTurretRotation) > localTurretRotInc) {
			while (localTurretRotation < 0) {
				localTurretRotation += 360;
				prevYaw += 360;
			}
			while (localTurretRotation >= 360) {
				localTurretRotation -= 360;
				prevYaw -= 360;
			}
			localTurretDestRot = Trig.wrapTo360(localTurretDestRot);
			float curMod = localTurretRotation;
			float destMod = localTurretDestRot;
			float diff = curMod > destMod ? curMod - destMod : destMod - curMod;
			float turnDir = 0;
			if (curMod > destMod) {
				if (diff < 180) {
					turnDir = -1;
				} else {
					turnDir = 1;
				}
			} else if (curMod < destMod) {
				if (diff < 180) {
					turnDir = 1;
				} else {
					turnDir = -1;
				}
			}
			localTurretRotation += localTurretRotInc * turnDir;
		} else {
			localTurretRotation = localTurretDestRot;
		}
		if (Math.abs(localTurretDestRot - localTurretRotation) < localTurretRotInc) {
			localTurretRotation = localTurretDestRot;
		}
		this.currentTurretYawSpeed = this.localTurretRotation - prevYaw;
		if (this.currentTurretYawSpeed > 180) {
			this.currentTurretYawSpeed -= 360.f;
		}
		if (this.currentTurretYawSpeed < -180) {
			this.currentTurretYawSpeed += 360.f;
		}
	}

	public void updateTurretAngles(float pitch, float rotation) {
		this.localTurretPitch = pitch;
		this.localTurretRotation = rotation;
		this.localTurretDestPitch = this.localTurretPitch;
		this.localTurretDestRot = this.localTurretRotation;
	}

	/**
	 * spits out inventory into world, and packs the vehicle into an item, also spat into the world
	 */
	public void packVehicle() {
		if (!this.level().isClientSide) {
			InventoryTools.dropItemInWorld(level(), getItemForVehicle(), getX(), getY(), getZ());
			InventoryTools.dropItemsInWorld(level(), inventory.ammoInventory, getX(), getY(), getZ());
			this.discard();
		}
	}

	@Override
	public boolean hurt(DamageSource damageSource, float amount) {
		if (!damageSource.is(DamageTypeTags.IS_EXPLOSION)) {
			if (hurtInvulTicks > 0) {
				return false;
			}
			hurtInvulTicks = 10;
		}

		if (this.level().isClientSide) {
			hitAnimationTicks = 20;
			return false;
		}
		if (this.isRemoved() || this.isInvulnerableTo(damageSource)) {
			return false;
		}
		this.markHurt();
		float adjDmg = upgradeHelper.getScaledDamage(damageSource, amount);
		this.setHealth(getHealth() - adjDmg);
		if (getHealth() <= 0) {
			InventoryTools.dropItemsInWorld(level(), inventory.ammoInventory, getX(), getY(), getZ());
			this.discard();
			return false;
		}
		return true;
	}

	@Override
	public void push(Entity entity) {
		if (entity != getFirstPassenger() && !(entity instanceof NpcBase))//skip if it if it is the rider
		{
			double xDiff = entity.getX() - this.getX();
			double zDiff = entity.getZ() - this.getZ();
			double entityDistance = Mth.absMax(xDiff, zDiff);

			if (entityDistance >= 0.009999999776482582D) {
				entityDistance = Math.sqrt(entityDistance);
				xDiff /= entityDistance;
				zDiff /= entityDistance;
				double normalizeToDistance = 1.0D / entityDistance;

				if (normalizeToDistance > 1.0D) {
					normalizeToDistance = 1.0D;
				}

				xDiff *= normalizeToDistance;
				zDiff *= normalizeToDistance;
				xDiff *= 0.05000000074505806D;
				zDiff *= 0.05000000074505806D;
				/* 1.12 entityCollisionReduction = 0.9 */
				xDiff *= 1.0F - 0.9F;
				zDiff *= 1.0F - 0.9F;
				this.push(-xDiff, 0.0D, -zDiff);
				entity.push(xDiff, 0.0D, zDiff);
			}
		}
	}

	public ResourceLocation getTexture() {
		return vehicleType.getTextureForMaterialLevel(vehicleMaterialLevel);
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
		if (!this.hasPassenger(passenger)) {
			return;
		}
		double posX = this.getX();
		double posY = this.getY() + this.getRiderVerticalOffset();
		double posZ = this.getZ();

		float yaw = this.vehicleType.moveRiderWithTurret() ? localTurretRotation : getYRot();
		posX += Trig.sinDegrees(yaw) * -this.getRiderForwardOffset();
		posX += Trig.sinDegrees(yaw + 90) * this.getRiderHorizontalOffset();
		posZ += Trig.cosDegrees(yaw) * -this.getRiderForwardOffset();
		posZ += Trig.cosDegrees(yaw + 90) * this.getRiderHorizontalOffset();
		/* 1.12 passenger.getYOffset(): -0.35 for players and for NpcBase, 0 for everything else */
		double yOffset = passenger instanceof Player || passenger instanceof NpcBase ? -0.35D : 0D;
		callback.accept(passenger, posX, posY + yOffset, posZ);
		if (passenger instanceof NpcBase) {
			passenger.setYRot(180 - localTurretRotation);
			((NpcBase) passenger).setYBodyRot(180 - localTurretRotation);
			((NpcBase) passenger).setYHeadRot(180 - localTurretRotation);
		} else {
			passenger.setYRot(passenger.getYRot() - this.moveHelper.getRotationSpeed());
		}
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (this.isSettingUp) {
			if (!player.level().isClientSide) {
				player.sendSystemMessage(Component.literal("Vehicle is currently being set-up.  It has " + setupTicks + " ticks remaining."));
			}
			return InteractionResult.PASS;
		}
		return this.firingVarsHelper.interact(player) ? InteractionResult.sidedSuccess(level().isClientSide) : InteractionResult.PASS;
	}

	@Override
	public String toString() {
		return String.format("%s::%s @ %.2f, %.2f, %.2f  -- y:%.2f p:%.2f -- m: %.2f, %.2f, %.2f", this.vehicleType.getDisplayName(), this.getId(), this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot(), this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z);
	}

	@Override
	public boolean shouldRiderSit() {
		return this.vehicleType.shouldRiderSit();
	}

	/* 1.12 getCollisionBoundingBox() returned the entity box: other entities collide with a vehicle like with a boat */
	@Override
	public boolean canBeCollidedWith() {
		return true;
	}

	/* 1.12 canBeCollidedWith() */
	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	protected Component getTypeName() {
		return Component.translatable(vehicleType.getDisplayName() + ".name");
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf pb) {
		pb.writeFloat(this.getHealth());
		pb.writeInt(this.vehicleType.getGlobalVehicleType());
		pb.writeInt(this.vehicleMaterialLevel);
		pb.writeNbt(upgradeHelper.serializeNBT());
		pb.writeNbt(ammoHelper.serializeNBT());
		pb.writeNbt(moveHelper.serializeNBT());
		pb.writeNbt(firingHelper.serializeNBT());
		pb.writeNbt(firingVarsHelper.serializeNBT());
		pb.writeFloat(localLaunchPower);
		pb.writeFloat(localTurretPitch);
		pb.writeFloat(localTurretRotation);
		pb.writeFloat(localTurretDestPitch);
		pb.writeFloat(localTurretDestRot);
		owner.serializeToBuffer(pb);
		pb.writeFloat(localTurretRotationHome);
		pb.writeBoolean(this.isSettingUp);
		if (this.isSettingUp) {
			pb.writeInt(this.setupTicks);
		}
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf pb) {
		float health = pb.readFloat();
		IVehicleType type = VehicleType.getVehicleType(pb.readInt());
		this.setVehicleType(type == null ? VehicleRegistry.CATAPULT_STAND_FIXED : type, pb.readInt());
		this.setHealth(health);
		this.upgradeHelper.deserializeNBT(pb.readNbt());
		this.ammoHelper.deserializeNBT(pb.readNbt());
		this.moveHelper.deserializeNBT(pb.readNbt());
		this.firingHelper.deserializeNBT(pb.readNbt());
		this.firingVarsHelper.deserializeNBT(pb.readNbt());
		this.localLaunchPower = pb.readFloat();
		this.localTurretPitch = pb.readFloat();
		this.localTurretRotation = pb.readFloat();
		this.localTurretDestPitch = pb.readFloat();
		this.localTurretDestRot = pb.readFloat();
		this.firingHelper.clientLaunchSpeed = localLaunchPower;
		this.firingHelper.clientTurretPitch = localTurretPitch;
		this.firingHelper.clientTurretYaw = localTurretRotation;
		this.upgradeHelper.updateUpgradeStats();
		owner = new Owner(pb);
		this.localTurretRotationHome = pb.readFloat();
		this.isSettingUp = pb.readBoolean();
		if (this.isSettingUp) {
			this.setupTicks = pb.readInt();
		}
		this.setPos(getX(), getY(), getZ());//this is to reset the bounding box, because the size of the entity changed during vehicleType setup
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		IVehicleType vehType = VehicleType.getVehicleType(tag.getInt("vehType"));
		if (vehType == null) {
			/* port: a type that is not part of the reduced module (or was disabled); 1.12 crashed here */
			vehType = VehicleRegistry.CATAPULT_STAND_FIXED;
		}
		//port: a material level outside the material's range (bad NBT) is clamped; 1.12 logged an error on every stat lookup
		int level = Math.max(0, Math.min(tag.getInt("matLvl"), vehType.getMaterialType().getNumOfLevels() - 1));
		this.setVehicleType(vehType, level);
		this.setHealth(tag.contains("health") ? tag.getFloat("health") : baseHealth);
		this.localTurretRotationHome = tag.getFloat("turHome");
		this.inventory.readFromNBT(tag);
		this.upgradeHelper.deserializeNBT(tag.getCompound("upgrades"));
		this.ammoHelper.deserializeNBT(tag.getCompound("ammo"));
		this.moveHelper.deserializeNBT(tag.getCompound("move"));
		this.firingHelper.deserializeNBT(tag.getCompound("fire"));
		this.firingVarsHelper.deserializeNBT(tag.getCompound("vars"));
		if (tag.contains("lc")) {
			this.localLaunchPower = tag.getFloat("lc");
			this.localTurretPitch = tag.getFloat("tp");
			this.localTurretDestPitch = tag.getFloat("tpd");
			this.localTurretRotation = tag.getFloat("tr");
			this.localTurretDestRot = tag.getFloat("trd");
		}
		this.upgradeHelper.updateUpgrades();
		this.ammoHelper.updateAmmoCounts();
		owner = Owner.deserializeFromNBT(tag);
		this.isSettingUp = tag.getBoolean("setup");
		if (this.isSettingUp) {
			this.setupTicks = tag.getInt("sTick");
		}
		this.setPos(getX(), getY(), getZ());//this is to reset the bounding box, because the size of the entity changed during vehicleType setup
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("vehType", this.vehicleType.getGlobalVehicleType());
		tag.putInt("matLvl", this.vehicleMaterialLevel);
		tag.putFloat("health", this.getHealth());
		tag.putFloat("turHome", this.localTurretRotationHome);
		this.inventory.writeToNBT(tag);//yah..I wrote this one a long time ago, is why it is different.....
		tag.put("upgrades", this.upgradeHelper.serializeNBT());
		tag.put("ammo", this.ammoHelper.serializeNBT());
		tag.put("move", this.moveHelper.serializeNBT());
		tag.put("fire", this.firingHelper.serializeNBT());
		tag.put("vars", this.firingVarsHelper.serializeNBT());
		tag.putFloat("lc", localLaunchPower);
		tag.putFloat("tp", localTurretPitch);
		tag.putFloat("tpd", localTurretDestPitch);
		tag.putFloat("tr", localTurretRotation);
		tag.putFloat("trd", localTurretDestRot);
		owner.serializeToNBT(tag);
		tag.putBoolean("setup", this.isSettingUp);
		if (this.isSettingUp) {
			tag.putInt("sTick", this.setupTicks);
		}
	}

	/**
	 * missile callback methods...
	 */
	@Override
	public void onMissileImpact(Level world, double x, double y, double z) {
		if (getVehicle() instanceof IMissileHitCallback) {
			((IMissileHitCallback) getVehicle()).onMissileImpact(world, x, y, z);
		}
	}

	@Override
	public void onMissileImpactEntity(Level world, Entity entity) {
		if (getVehicle() instanceof IMissileHitCallback) {
			((IMissileHitCallback) getVehicle()).onMissileImpactEntity(world, entity);
		}
	}

	@Override
	public void setOwner(Player player) {
		owner = new Owner(player);
	}

	@Override
	public void setOwner(Owner owner) {
		this.owner = owner;
	}

	@Override
	public Owner getOwner() {
		return owner;
	}

	@Override
	public boolean isOwner(Player player) {
		return owner.isOwnerOrSameTeamOrFriend(player);
	}

	public boolean isAmmoLoaded() {
		return vehicleType.getValidAmmoTypes().stream().anyMatch(a -> ammoHelper.getCountOf(a) > 0);
	}

	/* ---------------------------------------- npc.xmod.IVehicle ---------------------------------------- */

	@Override
	public Entity asEntity() {
		return this;
	}

	@Override
	public boolean canSoldiersPilot() {
		return vehicleType.canSoldiersPilot();
	}

	@Override
	public String getConfigName() {
		return vehicleType.getConfigName();
	}

	@Override
	public boolean canAdjustYaw() {
		return vehicleType.canAdjustYaw();
	}

	@Override
	public float getBaseTurretRotationAmount() {
		return vehicleType.getBaseTurretRotationAmount();
	}

	@Override
	public float getLocalTurretRotationHome() {
		return localTurretRotationHome;
	}

	@Override
	public float getCurrentTurretRotationMax() {
		return currentTurretRotationMax;
	}

	@Override
	public boolean isAimedAt(ITarget target) {
		return firingHelper.isAimedAt(target);
	}

	@Override
	public ITarget leadTarget(ITarget target) {
		return firingHelper.leadTarget(target);
	}

	@Override
	public float getAimYaw(ITarget target) {
		return firingHelper.getAimYaw(target);
	}

	@Override
	public boolean isReadyToFire() {
		return firingHelper.isReadyToFire();
	}

	@Override
	public void handleFireUpdate() {
		firingHelper.handleFireUpdate();
	}

	@Override
	public void handleSoldierTargetInput(double x, double y, double z) {
		firingHelper.handleSoldierTargetInput(x, y, z);
	}

	@Override
	public void setStrafeInput(byte input) {
		moveHelper.setStrafeInput(input);
	}

	/* forward movement is not ported: the input is ignored */
	@Override
	public void setForwardInput(byte input) {
	}

	@Override
	public void stopMotion() {
		moveHelper.stopMotion();
	}

	/* the vehicle path finder is not ported */
	@Override
	public void forcePath(List<?> path) {
	}
}
