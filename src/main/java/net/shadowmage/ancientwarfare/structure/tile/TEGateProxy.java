package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TEGateProxy extends TileUpdatable implements ITickableTile {
	private static final String RENDER_TAG = "render";
	private EntityGate owner = null;
	private UUID entityID = null;
	private int clientEntityID = 0;
	private int noParentTicks = 0;
	private boolean render = false;

	public TEGateProxy(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.GATE_PROXY_TILE.get(), pos, state);
	}

	protected TEGateProxy(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public void setOwner(EntityGate gate) {
		owner = gate;
		entityID = owner.getUUID();
		BlockTools.notifyBlockUpdate(this);
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		if (tag.contains("msb") && tag.contains("lsb")) {
			long msb = tag.getLong("msb");
			long lsb = tag.getLong("lsb");
			entityID = new UUID(msb, lsb);
		}
		render = tag.getBoolean(RENDER_TAG);
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		if (entityID != null) {
			tag.putLong("msb", entityID.getMostSignificantBits());
			tag.putLong("lsb", entityID.getLeastSignificantBits());
		}
		tag.putBoolean(RENDER_TAG, render);
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		tag.putBoolean(RENDER_TAG, render);
		tag.putInt("owner", owner != null ? owner.getId() : 0);
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		render = tag.getBoolean(RENDER_TAG);
		clientEntityID = tag.getInt("owner");
	}

	public AABB getRenderBoundingBox() {
		return getGate().map(EntityGate::getRenderBoundingBox).orElse(new AABB(getBlockPos()));
	}

	@Override
	public void update() {
		if (!hasLevel() || (level.isClientSide && (!render || clientEntityID <= 0 || owner != null))) {
			return;
		}
		if (level.isClientSide) {
			Entity entity = level.getEntity(clientEntityID);
			owner = entity instanceof EntityGate ? (EntityGate) entity : null;
			return;
		}

		handleMissingOwner();
	}

	private void handleMissingOwner() {
		if (entityID == null) {
			noParentTicks++;
		} else if (!getOwner().isPresent()) {
			noParentTicks++;

			//Port notes: 1.12 searched level.loadedEntityList
			List<EntityGate> entities = level.getEntitiesOfClass(EntityGate.class, new AABB(worldPosition).inflate(64));
			for (EntityGate ent : entities) {
				if (ent.getUUID().equals(entityID)) {
					setOwner(ent);
					noParentTicks = 0;
					break;
				}
			}
		}
		if (noParentTicks >= 100 || getOwner().map(o -> o.isRemoved()).orElse(false)) {
			owner = null;
			level.removeBlock(worldPosition, false);
		}
	}

		public double getMaxRenderDistanceSquared()
	{
		return 65536.0D;
	}

	public boolean isGateClosed() {
		return getOwner().map(EntityGate::isClosed).orElse(false);
	}

	public Optional<EntityGate> getOwner() {
		return Optional.ofNullable(owner);
	}

	public void setRender() {
		render = true;
		BlockTools.notifyBlockUpdate(this);
	}

	public boolean doesRender() {
		return render;
	}

	public Optional<EntityGate> getGate() {
		return Optional.ofNullable(owner);
	}

	public boolean isOpen() {
		return owner == null || !owner.isClosed();
	}
}
