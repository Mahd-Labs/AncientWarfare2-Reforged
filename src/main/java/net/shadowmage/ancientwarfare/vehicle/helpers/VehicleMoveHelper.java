package net.shadowmage.ancientwarfare.vehicle.helpers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.MoverType;
import net.shadowmage.ancientwarfare.core.util.INBTSerializable;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

/*
 * Port notes: reduced to what a stationary weapon needs (see docs/DETAILS.md, "What is in, and what is not"). Kept from 1.12:
 * gravity (applyGroundMotion), turning in place from the turn input (applyTurnInput, used by the left / right keys
 * and by the NPC aim AI), stopMotion / clearInputFromDismount and the NBT keys. Dropped: forward motion, water and
 * air movement, the navigator hook, tearUpGrass, and the client side interpolation of PacketVehicleMove (position
 * and yaw reach the client through the vanilla entity tracker; rotationSpeed on the client is the yaw change of the
 * last tick, which is what 1.12 computed from the packet values).
 */
public class VehicleMoveHelper implements INBTSerializable<CompoundTag> {
	byte forwardInput;
	byte turnInput;
	byte powerInput;
	byte rotationInput;

	protected float turnMotion = 0.f;
	protected float rotationDrag = 0.94f;
	protected float rotationStop = 0.2f;
	protected float rotationSpeed = 0.f;

	protected VehicleBase vehicle;

	public VehicleMoveHelper(VehicleBase vehicle) {
		this.vehicle = vehicle;
	}

	public float getRotationSpeed() {
		return this.rotationSpeed;
	}

	public void setStrafeInput(byte in) {
		this.turnInput = in;
	}

	public void handleInputData(byte forwardInput, byte turnInput, byte powerInput, byte rotationInput) {
		this.forwardInput = forwardInput;
		this.turnInput = turnInput;
		this.powerInput = powerInput;
		this.rotationInput = rotationInput;
	}

	public void onUpdate() {
		if (vehicle.level().isClientSide) {
			onUpdateClient();
		} else {
			if (this.vehicle.getFirstPassenger() == null) {
				this.clearInputFromDismount();
			}
			onUpdateServer();
		}
	}

	/* 1.12 VEHICLE_MOVE_UPDATE_FREQUENCY: the client eases towards a received yaw over this many ticks + 1 */
	private static final int VEHICLE_MOVE_UPDATE_FREQUENCY = 3;
	private float destYaw;
	private int rotationTicks = 0;
	private boolean yawFromServer = false;
	private float lastSentYaw = Float.NaN;

	/* client: the exact yaw from PacketVehicleMove (1.12 updateMoveData, yaw only) */
	public void updateMoveData(float yaw) {
		this.rotationTicks = VEHICLE_MOVE_UPDATE_FREQUENCY + 1;
		this.destYaw = yaw;
		this.yawFromServer = true;
	}

	/* client: true once PacketVehicleMove steers the yaw; the entity tracker's rounded yaw is ignored from then on */
	public boolean hasYawFromServer() {
		return yawFromServer;
	}

	protected void onUpdateClient() {
		//1.12 onUpdateClient, rotation part
		rotationSpeed = 0;
		if (this.rotationTicks > 0) {
			rotationSpeed = (this.destYaw - vehicle.getYRot()) / rotationTicks;
			vehicle.setYRot(vehicle.getYRot() + this.rotationSpeed);
			this.rotationTicks--;
		}
	}

	protected void onUpdateServer() {
		this.applyGroundMotion();
		vehicle.setDeltaMovement(0, vehicle.getDeltaMovement().y, 0);
		this.vehicle.move(MoverType.SELF, vehicle.getDeltaMovement());
		if (vehicle.onGround()) {
			vehicle.setDeltaMovement(0, 0, 0);
		}
		//1.12 sent PacketVehicleMove on every tick with a rider or a change; here: whenever the yaw changed, and once a
		//second while someone rides (a client that started tracking later gets the exact value soon)
		float yaw = vehicle.getYRot();
		if (yaw != lastSentYaw || (vehicle.getFirstPassenger() != null && vehicle.tickCount % 20 == 0)) {
			lastSentYaw = yaw;
			net.shadowmage.ancientwarfare.core.network.NetworkHandler.sendToAllTracking(vehicle, new net.shadowmage.ancientwarfare.vehicle.network.PacketVehicleMove(vehicle, yaw));
		}
	}

	protected void applyGroundMotion() {
		this.applyTurnInput(0.05f);
		vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(0, -9.81 * 0.05f * 0.05f, 0));
	}

	protected void applyTurnInput(float inputFactor) {
		float weightAdjust = 1.f;
		if (vehicle.currentWeight > vehicle.baseWeight) {
			weightAdjust = vehicle.baseWeight / vehicle.currentWeight;
		}
		if (turnInput != 0) {
			boolean reverse = (turnInput == -1 && turnMotion > 0) || (turnInput == 1 && turnMotion < 0);
			float maxSpeed = vehicle.currentStrafeSpeedMax * weightAdjust;
			float percent = 1 - (Math.abs(turnMotion) / maxSpeed);
			percent = reverse ? 1.f : percent;
			float changeFactor = percent * (turnInput * 2) * inputFactor;
			if (reverse) {
				changeFactor *= 2;
			}
			turnMotion += changeFactor;
			if (turnMotion > maxSpeed) {
				turnMotion = maxSpeed;
			}
			if (turnMotion < -maxSpeed) {
				turnMotion = -maxSpeed;
			}
		} else {
			turnMotion *= rotationDrag;
		}
		if (Math.abs(turnMotion) < rotationStop && turnInput == 0) {
			turnMotion = 0.f;
		}
		this.vehicle.setYRot(this.vehicle.getYRot() - this.turnMotion);
	}

	public void stopMotion() {
		this.clearInputFromDismount();
		vehicle.setDeltaMovement(0, 0, 0);
		turnMotion = 0;
	}

	public void clearInputFromDismount() {
		this.forwardInput = 0;
		this.turnInput = 0;
		this.powerInput = 0;
		this.rotationInput = 0;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putByte("fi", forwardInput);
		tag.putByte("si", turnInput);
		tag.putByte("pi", powerInput);
		tag.putByte("ri", rotationInput);
		tag.putFloat("tr", 0);
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		this.forwardInput = tag.getByte("fi");
		this.turnInput = tag.getByte("si");
		this.powerInput = tag.getByte("pi");
		this.rotationInput = tag.getByte("ri");
	}
}
