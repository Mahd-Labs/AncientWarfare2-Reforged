package net.shadowmage.ancientwarfare.structure.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.shadowmage.ancientwarfare.structure.block.BlockSeat;
import net.shadowmage.ancientwarfare.structure.init.AWStructureEntities;
import net.shadowmage.ancientwarfare.structure.util.RotationLimit;

import java.util.Optional;

/*
 * Port notes: setSize(0.01, 0.01) is on the entity type. onEntityUpdate -> baseTick. The passenger attachment point
 * reproduces the 1.12 riding height (mounted offset 0.75 * height, player y offset -0.35; a 1.21 player sits 0.6
 * below the attachment point).
 */
@SuppressWarnings("squid:S2160") //no need to override equals as entityId comparison works for this as well
public class EntitySeat extends Entity implements IEntityWithComplexSpawn {
	private BlockPos seatPos = BlockPos.ZERO;

	public EntitySeat(EntityType<? extends EntitySeat> type, Level world) {
		super(type, world);
	}

	public EntitySeat(Level world, Vec3 position, BlockPos seatPos) {
		super(AWStructureEntities.SEAT.get(), world);
		this.seatPos = seatPos;
		setPos(position.x, position.y, position.z);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		//noop
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
		return new Vec3(0, 0.01 * 0.75 - 0.35 + 0.6, 0);
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		getRotationLimit().ifPresent(rotationLimit -> {
			if (!rotationLimit.isWithinLimit(passenger.getYRot())) {
				passenger.setYRot(rotationLimit.getMidPoint());
				passenger.yRotO = rotationLimit.getMidPoint();
			}
		});
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!level().isClientSide && (!isVehicle() || level().isEmptyBlock(seatPos))) {
			discard();
			level().updateNeighbourForOutputSignal(blockPosition(), level().getBlockState(blockPosition()).getBlock());
			return;
		}

		if (!level().isClientSide || !isVehicle()) {
			return;
		}

		restrictPlayerRotation();
	}

	private void restrictPlayerRotation() {
		getRotationLimit().ifPresent(rotationLimit -> {
					if (!(getPassengers().get(0) instanceof Player)) {
						return;
					}

					Player passenger = (Player) getPassengers().get(0);
					if (!rotationLimit.isWithinLimit(passenger.getYRot())) {
						float rotation = rotationLimit.restrictToLimit(passenger.getYRot());
						passenger.yRotO = rotation;
						passenger.setYRot(rotation);
					}
				}
		);
	}

	private Optional<RotationLimit> getRotationLimit() {
		BlockState state = level().getBlockState(seatPos);
		Block block = state.getBlock();
		if (!(block instanceof BlockSeat)) {
			return Optional.empty();
		}

		BlockSeat seat = (BlockSeat) block;
		RotationLimit rotationLimit = seat.getRotationLimit(level(), seatPos, state);
		return Optional.of(rotationLimit);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag compound) {
		seatPos = BlockPos.of(compound.getLong("seatPos"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag compound) {
		compound.putLong("seatPos", seatPos.asLong());
	}

	public BlockPos getSeatPos() {
		return seatPos;
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		buffer.writeLong(seatPos.asLong());
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
		seatPos = BlockPos.of(additionalData.readLong());
	}
}
