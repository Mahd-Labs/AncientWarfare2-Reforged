package net.shadowmage.ancientwarfare.vehicle.missiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.shadowmage.ancientwarfare.core.util.Trig;
import net.shadowmage.ancientwarfare.vehicle.entity.IMissileHitCallback;
import net.shadowmage.ancientwarfare.vehicle.init.AWVehicleEntities;
import net.shadowmage.ancientwarfare.vehicle.registry.AmmoRegistry;

import java.util.List;
import java.util.Optional;

/*
 * Port notes:
 * - The flight code is the 1.12 one (own motion fields mX/mY/mZ for rockets, own gravity from the ammo type, own
 *   ray trace against blocks and entity boxes); it does not use the vanilla Projectile class. Motion is the entity's
 *   delta movement, kept in the local doubles motionX/Y/Z during a movement tick to keep the 1.12 arithmetic.
 * - world.rayTraceBlocks(from, to, false, true, false) -> level.clip(COLLIDER, no fluids).
 * - RayTraceResult.entityHit -> the entity hit is kept in a local variable; onImpactWorld gets a BlockHitResult.
 * - checkProximity built a RayTraceResult(hitVec, DOWN, pos) -> BlockHitResult with the same values.
 * - 1.12 incremented ticksExisted itself in onUpdate() in addition to the base class (the counter ran twice as
 *   fast, so the "20 ticks before proximity checks" and the 6000 tick life time were 10 / 3000 real ticks); kept.
 * - Position reaches the client through the vanilla tracker; like 1.12 the client also runs the movement itself and
 *   lerpTo only sets the position.
 */
public class MissileBase extends Entity implements IEntityWithComplexSpawn {

	public IAmmo ammoType = AmmoRegistry.ammoArrow;
	public Entity launcher = null;
	public Entity shooterLiving;
	IMissileHitCallback shooter = null;
	private int rocketBurnTime = 0;

	private boolean inGround = false;
	private boolean hasImpacted = false;
	private BlockPos persistentBlockPos = BlockPos.ZERO;
	private BlockState persistentBlock = Blocks.AIR.defaultBlockState();

	private float mX;
	private float mY;
	private float mZ;

	private double motionX;
	private double motionY;
	private double motionZ;

	public MissileBase(EntityType<? extends MissileBase> type, Level level) {
		super(type, level);
	}

	public MissileBase(Level level) {
		this(AWVehicleEntities.MISSILE.get(), level);
	}

	private void setMissileParams(IAmmo type, float x, float y, float z, float mx, float my, float mz) {
		this.ammoType = type;
		this.setPos(x, y, z);
		this.xo = this.getX();
		this.yo = this.getY();
		this.zo = this.getZ();
		this.motionX = mx;
		this.motionY = my;
		this.motionZ = mz;
		this.mX = mx;
		this.mY = my;
		this.mZ = mz;
		if (this.ammoType.updateAsArrow()) {
			this.onUpdateArrowRotation();
		}
		this.xRotO = this.getXRot();
		this.yRotO = this.getYRot();
		if (this.ammoType.isRocket())//use launch power to determine rocket burn time...
		{
			float temp = Mth.sqrt(mx * mx + my * my + mz * mz);
			this.rocketBurnTime = (int) (temp * 20.f * Trig.ROCKET_BURN_TIME_FACTOR);
			this.mX = (float) (motionX / temp) * Trig.ROCKET_ACCELERATION_FACTOR;
			this.mY = (float) (motionY / temp) * Trig.ROCKET_ACCELERATION_FACTOR;
			this.mZ = (float) (motionZ / temp) * Trig.ROCKET_ACCELERATION_FACTOR;
			this.motionX = mX;
			this.motionY = mY;
			this.motionZ = mZ;
		}
		this.setDeltaMovement(motionX, motionY, motionZ);
	}

	public void setMissileParams2(IAmmo ammo, float x, float y, float z, float yaw, float angle, float velocity) {
		float vX = -Trig.sinDegrees(yaw) * Trig.cosDegrees(angle) * velocity * 0.05f;
		float vY = Trig.sinDegrees(angle) * velocity * 0.05f;
		float vZ = -Trig.cosDegrees(yaw) * Trig.cosDegrees(angle) * velocity * 0.05f;
		this.setMissileParams(ammo, x, y, z, vX, vY, vZ);
	}

	public void setShooter(Entity shooter) {
		this.shooterLiving = shooter;
	}

	public void setLaunchingEntity(Entity ent) {
		this.launcher = ent;
	}

	public void setMissileCallback(IMissileHitCallback shooter) {
		this.shooter = shooter;
	}

	public boolean hasImpacted() {
		return hasImpacted;
	}

	public boolean isInGround() {
		return inGround;
	}

	public void onImpactEntity(Entity ent, float x, float y, float z) {
		if (Ammo.shouldEffectEntity(level(), ent, this)) {
			this.ammoType.onImpactEntity(level(), ent, x, y, z, this);
			if (this.shooter != null) {
				this.shooter.onMissileImpactEntity(level(), ent);
			}
		}
	}

	public void onImpactWorld(BlockHitResult hit) {
		this.ammoType.onImpactWorld(level(), hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ(), this, hit);
		if (this.shooter != null) {
			this.shooter.onMissileImpact(level(), hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ());
		}
	}

	@Override
	public boolean displayFireAnimation() {
		return this.ammoType.isFlaming();
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void push(Entity entity) {
		//NOOP
	}

	@Override
	protected Entity.MovementEmission getMovementEmission() {
		return Entity.MovementEmission.NONE;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public void tick() {
		this.tickCount++;
		super.tick();
		this.onMovementTick();
		if (!this.level().isClientSide) {
			if (this.tickCount > 6000)//5 min timer max for missiles...
			{
				this.discard();
			}
		}
	}

	private void checkProximity() {
		if (this.motionY > 0) {
			return;//don't bother checking when travelling upwards, wait until the downward swing...
		}
		//check ground.
		int groundDiff = 0;
		int x = (int) getX();
		int y = (int) getY();
		int z = (int) getZ();
		boolean impacted = false;
		if (ammoType.groundProximity() > 0) {
			while (groundDiff <= ammoType.groundProximity()) {
				groundDiff++;
				if (!level().isEmptyBlock(new BlockPos(x, y - groundDiff, z))) {
					this.onImpactWorld(new BlockHitResult(new Vec3(x, y, z), Direction.DOWN, new BlockPos(x, y, z), false));
					impacted = true;
					break;
				}
			}
		}
		//check entities if not detonated by ground
		if (!impacted && ammoType.entityProximity() > 0) {
			float entProx = ammoType.entityProximity();
			float foundDist = 0;
			List<Entity> entities = level().getEntities(this, new AABB(getX() - entProx, getY() - entProx, getZ() - entProx, getX() + entProx, getY() + entProx, getZ() + entProx));
			for (Entity ent : entities) {
				if (ent != null && ent.getClass() != MissileBase.class)//don't collide with missiles
				{
					foundDist = this.distanceTo(ent);
					if (foundDist < entProx) {
						this.onImpactEntity(ent, (float) getX(), (float) getY(), (float) getZ());
						break;
					}
				}
			}
		}
	}

	private void onMovementTick() {
		Vec3 delta = getDeltaMovement();
		motionX = delta.x;
		motionY = delta.y;
		motionZ = delta.z;
		if (this.inGround && persistentBlock != level().getBlockState(persistentBlockPos)) {
			this.motionX = 0;
			this.motionY = 0;
			this.motionZ = 0;
			this.inGround = false;
		}
		if (!this.inGround) {
			double posX = getX();
			double posY = getY();
			double posZ = getZ();
			Vec3 positionVector = new Vec3(posX, posY, posZ);
			Vec3 moveVector = new Vec3(posX + this.motionX, posY + this.motionY, posZ + this.motionZ);
			BlockHitResult blockHit = level().clip(new ClipContext(positionVector, moveVector, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
			if (blockHit.getType() == HitResult.Type.MISS) {
				blockHit = null;
			}
			Entity hitEntity = null;
			boolean testEntities = true;
			if (this.level().isClientSide) {
				testEntities = false;
			}
			if (testEntities) {
				//1.12: only the surroundings of the end point of this tick's step. Improvement (config
				//improvements.missile_hit_sweep): the whole step, so a fast missile cannot pass a target untested
				AABB searchBox = net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics.improvements.missileHitSweep
						? this.getBoundingBox().expandTowards(this.motionX, this.motionY, this.motionZ).inflate(1.0D, 1.0D, 1.0D)
						: this.getBoundingBox().move(this.motionX, this.motionY, this.motionZ).inflate(1.0D, 1.0D, 1.0D);
				List<Entity> nearbyEntities = this.level().getEntities(this, searchBox);
				double closestHit = 0.0D;
				float borderSize;
				for (Entity curEnt : nearbyEntities) {
					if (curEnt.isPickable()) {
						if (this.launcher != null) {
							if (curEnt == this.launcher || curEnt == this.launcher.getFirstPassenger() || curEnt == this.shooterLiving || curEnt == this.shooter) {
								continue;
							}
						}
						//improvement over 1.12 (config improvements.missile_hit_margin): 1.12 uses a fixed 0.3
						borderSize = (float) net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics.improvements.missileHitMargin;
						AABB var12 = curEnt.getBoundingBox().inflate(borderSize, borderSize, borderSize);
						Optional<Vec3> checkHit = var12.clip(positionVector, moveVector);
						if (checkHit.isPresent()) {
							double hitDistance = positionVector.distanceTo(checkHit.get());
							if (hitDistance < closestHit || closestHit == 0.0D) {
								hitEntity = curEnt;
								closestHit = hitDistance;
							}
						}
					}
				}
			}

			if (hitEntity != null) {
				this.onImpactEntity(hitEntity, (float) posX, (float) posY, (float) posZ);
				this.hasImpacted = true;
				if (!this.ammoType.isPenetrating() && !this.level().isClientSide) {
					this.discard();
				} else if (this.ammoType.isPenetrating()) {
					this.motionX *= 0.65f;
					this.motionY *= 0.65f;
					this.motionZ *= 0.65f;
				}
			} else if (blockHit != null) {
				this.onImpactWorld(blockHit);
				this.hasImpacted = true;
				if (!this.ammoType.isPenetrating()) {
					this.motionX = (float) (blockHit.getLocation().x - posX);
					this.motionY = (float) (blockHit.getLocation().y - posY);
					this.motionZ = (float) (blockHit.getLocation().z - posZ);
					float var20 = Mth.sqrt((float) (this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ));
					if (var20 > 0) {
						posX -= this.motionX / (double) var20 * 0.05000000074505806D;
						posY -= this.motionY / (double) var20 * 0.05000000074505806D;
						posZ -= this.motionZ / (double) var20 * 0.05000000074505806D;
					}
					this.inGround = true;
					if (!this.ammoType.isPersistent() && !this.level().isClientSide) {
						this.discard();
					} else if (this.ammoType.isPersistent()) {
						persistentBlockPos = blockHit.getBlockPos();
						persistentBlock = level().getBlockState(persistentBlockPos);
					}
				} else {
					this.motionX *= 0.65f;
					this.motionY *= 0.65f;
					this.motionZ *= 0.65f;
				}
			}
			if (this.ammoType.isProximityAmmo() && this.tickCount > 20) {
				this.setPosRaw(posX, posY, posZ);
				checkProximity();
				if (this.isRemoved()) {
					return;
				}
			}
			posX += this.motionX;
			posY += this.motionY;
			posZ += this.motionZ;
			if (this.ammoType.isRocket() && this.rocketBurnTime > 0)//if it is a rocket, accellerate if still burning
			{
				this.rocketBurnTime--;
				this.motionX += mX;
				this.motionY += mY;
				this.motionZ += mZ;
				if (this.level().isClientSide) {
					this.level().addParticle(ParticleTypes.SMOKE, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
				}
			} else {
				this.motionY -= (double) this.ammoType.getGravityFactor();
			}
			this.setPos(posX, posY, posZ);
			this.setDeltaMovement(motionX, motionY, motionZ);
			if (this.ammoType.updateAsArrow()) {
				this.onUpdateArrowRotation();
			}
		} else {
			this.setDeltaMovement(motionX, motionY, motionZ);
		}
	}

	private void onUpdateArrowRotation() {
		double motionSpeed = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
		this.setYRot(Trig.toDegrees((float) Math.atan2(this.motionX, this.motionZ)) - 90);
		this.setXRot(Trig.toDegrees((float) Math.atan2(this.motionY, motionSpeed)) - 90);
		while (this.getXRot() - this.xRotO < -180.0F) {
			this.xRotO -= 360.0F;
		}
		while (this.getXRot() - this.xRotO >= 180.0F) {
			this.xRotO += 360.0F;
		}
		while (this.getYRot() - this.yRotO < -180.0F) {
			this.yRotO -= 360.0F;
		}
		while (this.getYRot() - this.yRotO >= 180.0F) {
			this.yRotO += 360.0F;
		}
	}

	@Override
	public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
		this.setPos(x, y, z);
	}

	public ResourceLocation getTexture() {
		return ammoType.getModelTexture();
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		ResourceLocation ammoId = net.shadowmage.ancientwarfare.core.util.RegistryTools.parseOrNull(tag.getString("ammoRegistryName"));
		this.ammoType = ammoId == null ? null : AmmoRegistry.getAmmo(ammoId);
		this.inGround = tag.getBoolean("inGround");
		persistentBlockPos = BlockPos.of(tag.getLong("persistentBlockPos"));
		persistentBlock = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("persistentBlock"));
		this.tickCount = tag.getInt("ticks");
		this.mX = tag.getFloat("mX");
		this.mY = tag.getFloat("mY");
		this.mZ = tag.getFloat("mZ");
		if (this.ammoType == null) {
			this.ammoType = AmmoRegistry.ammoArrow;
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putString("ammoRegistryName", ammoType.getRegistryName().toString());
		tag.putBoolean("inGround", this.inGround);
		tag.putLong("persistentBlockPos", persistentBlockPos.asLong());
		tag.put("persistentBlock", NbtUtils.writeBlockState(persistentBlock));
		tag.putInt("ticks", this.tickCount);
		tag.putFloat("mX", this.mX);
		tag.putFloat("mY", this.mY);
		tag.putFloat("mZ", this.mZ);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		//NOOP
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf pb) {
		pb.writeUtf(ammoType.getRegistryName().toString());
		pb.writeFloat(getYRot());
		pb.writeFloat(getXRot());
		pb.writeBoolean(inGround);
		pb.writeLong(persistentBlockPos.asLong());
		pb.writeInt(Block.getId(persistentBlock));
		pb.writeInt(rocketBurnTime);
		pb.writeBoolean(this.launcher != null);
		if (this.launcher != null) {
			pb.writeInt(this.launcher.getId());
		}
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf pb) {
		ResourceLocation ammoId = net.shadowmage.ancientwarfare.core.util.RegistryTools.parseOrNull(pb.readUtf(64));
		ammoType = ammoId == null ? null : AmmoRegistry.getAmmo(ammoId);
		if (ammoType == null) {
			ammoType = AmmoRegistry.ammoArrow;
		}
		float yaw = pb.readFloat();
		float pitch = pb.readFloat();
		this.setYRot(yaw);
		this.setXRot(pitch);
		this.yRotO = yaw;
		this.xRotO = pitch;
		this.inGround = pb.readBoolean();
		persistentBlockPos = BlockPos.of(pb.readLong());
		persistentBlock = Block.stateById(pb.readInt());
		this.rocketBurnTime = pb.readInt();
		boolean hasLauncher = pb.readBoolean();
		if (hasLauncher) {
			launcher = level().getEntity(pb.readInt());
		}
	}
}
