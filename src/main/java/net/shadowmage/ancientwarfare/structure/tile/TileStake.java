package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.util.BlockTools;

import java.util.Optional;

public class TileStake extends TileUpdatable {
	private final EntityStatueInfo entityStatueInfo = new EntityStatueInfo();
	private boolean burns = true;

	public TileStake(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.STAKE_TILE.get(), pos, state);
	}

	protected TileStake(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public Optional<Entity> getRenderEntity() {
		return entityStatueInfo.getRenderEntity(level);
	}

	public boolean isEntityOnFire() {
		return entityStatueInfo.isEntityOnFire();
	}

	public boolean burns() {
		return burns;
	}

	@Override
	public void readFromNBT(CompoundTag compound) {
		super.readFromNBT(compound);
		readNBT(compound);
		setChanged();
	}

	private void readNBT(CompoundTag compound) {
		entityStatueInfo.deserializeNBT(compound);
		burns = compound.getBoolean("burns");
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		writeNBT(tag);
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		readNBT(tag);
		BlockTools.notifyBlockUpdate(this);
	}

	@Override
	public void writeToNBT(CompoundTag compound) {
		super.writeToNBT(compound);
		writeNBT(compound);
	}

	private CompoundTag writeNBT(CompoundTag compound) {
		compound = entityStatueInfo.serializeNBT(compound);
		compound.putBoolean("burns", burns);
		return compound;
	}

	public void resetEntityName() {
		entityStatueInfo.resetEntityName();
		setChanged();
	}

	public ResourceLocation getEntityName() {
		return entityStatueInfo.getEntityName();
	}

	public void setEntityName(ResourceLocation entityName) {
		entityStatueInfo.setEntityName(entityName);
		setChanged();
	}

	public void setEntityOnFire(boolean entityOnFire) {
		entityStatueInfo.setEntityOnFire(entityOnFire);
		setChanged();
	}

	public void setBurns(boolean burns) {
		this.burns = burns;
		setChanged();
	}

	public AABB getRenderBoundingBox() {
		return new AABB(Vec3.atLowerCornerOf(worldPosition), Vec3.atLowerCornerOf(worldPosition.offset(0, 3, 0)));
	}
}
