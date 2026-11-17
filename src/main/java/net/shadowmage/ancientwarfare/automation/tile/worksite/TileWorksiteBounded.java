package net.shadowmage.ancientwarfare.automation.tile.worksite;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.automation.chunkloader.AWChunkLoader;
import net.shadowmage.ancientwarfare.core.interfaces.IBoundedSite;
import net.shadowmage.ancientwarfare.core.interfaces.IChunkLoaderTile;
import net.shadowmage.ancientwarfare.core.upgrade.WorksiteUpgrade;
import net.shadowmage.ancientwarfare.core.util.BlockTools;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

/*
 * Port notes:
 * - AWChunkLoader.Ticket -> AWChunkLoader.Ticket. 1.12 handed the saved ticket back through setTicket after a
 *   level load; now the tile re-requests it in onLoad (server) when it has a chunk loader upgrade.
 * - getRenderBoundingBox is a plain method for the (client) renderer; where 1.12 fell back to the TileEntity default
 *   it returns the block's own box.
 */
public abstract class TileWorksiteBounded extends TileWorksiteBase implements IBoundedSite, IChunkLoaderTile {

	private static final String BB_MIN_TAG = "bbMin";
	private static final String BB_MAX_TAG = "bbMax";
	/*
			 * minimum position of the work area bounding box, or a single block position if bbMax is not set
			 * must not be null if this block has a work-area
			 */
	private BlockPos bbMin = BlockPos.ZERO;

	/*
	 * maximum position of the work bounding box.  May be null
	 */
	private BlockPos bbMax = BlockPos.ZERO;

	AWChunkLoader.Ticket chunkTicket = null;

	public TileWorksiteBounded(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public Set<WorksiteUpgrade> getValidUpgrades() {
		return EnumSet.of(WorksiteUpgrade.ENCHANTED_TOOLS_1, WorksiteUpgrade.ENCHANTED_TOOLS_2, WorksiteUpgrade.SIZE_MEDIUM, WorksiteUpgrade.SIZE_LARGE,
				WorksiteUpgrade.TOOL_QUALITY_1, WorksiteUpgrade.TOOL_QUALITY_2, WorksiteUpgrade.TOOL_QUALITY_3, WorksiteUpgrade.BASIC_CHUNK_LOADER);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide && chunkTicket == null && hasChunkLoaderUpgrade()) {
			setupInitialTicket();
		}
	}

	@Override
	public void onBlockBroken(BlockState state) {
		super.onBlockBroken(state);
		if (this.chunkTicket != null) {
			AWChunkLoader.releaseTicket(chunkTicket);
			this.chunkTicket = null;
		}
	}

	@Override
	public void addUpgrade(WorksiteUpgrade upgrade) {
		super.addUpgrade(upgrade);
		if (upgrade == WorksiteUpgrade.BASIC_CHUNK_LOADER || upgrade == WorksiteUpgrade.QUARRY_CHUNK_LOADER) {
			setupInitialTicket();//setup chunkloading for the worksite
		}
	}

	@Override
	public final void removeUpgrade(WorksiteUpgrade upgrade) {
		super.removeUpgrade(upgrade);
		if (upgrade == WorksiteUpgrade.BASIC_CHUNK_LOADER || upgrade == WorksiteUpgrade.QUARRY_CHUNK_LOADER) {
			setTicket(null);//release any existing ticket
		}
	}

	@Override
	public final boolean hasWorkBounds() {
		return !bbMin.equals(BlockPos.ZERO) && !bbMax.equals(BlockPos.ZERO);
	}

	@Override
	public final BlockPos getWorkBoundsMin() {
		return bbMin;
	}

	@Override
	public final BlockPos getWorkBoundsMax() {
		return bbMax;
	}

	@Override
	public final void setBounds(BlockPos min, BlockPos max) {
		setWorkBoundsMin(BlockTools.getMin(min, max));
		setWorkBoundsMax(BlockTools.getMax(min, max));
		onBoundsSet();
	}

	@Override
	public int getBoundsMaxWidth() {
		if (getUpgrades().contains(WorksiteUpgrade.SIZE_LARGE)) {
			return 16;
		} else if (getUpgrades().contains(WorksiteUpgrade.SIZE_MEDIUM)) {
			return 9;
		} else {
			return 5;
		}
	}

	@Override
	public int getBoundsMaxHeight() {
		return 1;
	}

	public final void setTicket(@Nullable AWChunkLoader.Ticket tk) {
		if (chunkTicket != null) {
			AWChunkLoader.releaseTicket(chunkTicket);
			chunkTicket = null;
		}
		this.chunkTicket = tk;
		if (this.chunkTicket == null) {
			return;
		}
		AWChunkLoader.INSTANCE.writeDataToTicket(chunkTicket, worldPosition);
		ChunkPos ccip = new ChunkPos(worldPosition);
		AWChunkLoader.forceChunk(chunkTicket, ccip);
		if (hasWorkBounds()) {
			chunkLoadWorkBounds();
		}
	}

	protected void chunkLoadWorkBounds() {
		int minX = getWorkBoundsMin().getX() >> 4;
		int minZ = getWorkBoundsMin().getZ() >> 4;
		int maxX = getWorkBoundsMax().getX() >> 4;
		int maxZ = getWorkBoundsMax().getZ() >> 4;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				ChunkPos ccip = new ChunkPos(x, z);
				AWChunkLoader.forceChunk(chunkTicket, ccip);
			}
		}
	}

	private void setupInitialTicket() {
		if (chunkTicket != null) {
			AWChunkLoader.releaseTicket(chunkTicket);
		}
		if (hasChunkLoaderUpgrade()) {
			setTicket(AWChunkLoader.requestTicket(level, worldPosition));
		}
	}

	boolean hasChunkLoaderUpgrade() {
		return getUpgrades().contains(WorksiteUpgrade.BASIC_CHUNK_LOADER) || getUpgrades().contains(WorksiteUpgrade.QUARRY_CHUNK_LOADER);
	}

	/*
	 * Used by user-set-blocks tile to set all default harvest-checks to true when bounds are FIRST set
	 */
	protected void onBoundsSet() {

	}

	@Override
	public void onBoundsAdjusted() {
		//implement in subclasses to check target blocks, clear invalid ones
	}

	@Override
	public void onPostBoundsAdjusted() {
		setupInitialTicket();
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	boolean isInBounds(BlockPos pos) {
		return pos.getX() >= bbMin.getX() && pos.getX() <= bbMax.getX() && pos.getZ() >= bbMin.getZ() && pos.getZ() <= bbMax.getZ();
	}

	protected void validateCollection(Collection<BlockPos> blocks) {
		if (!hasWorkBounds()) {
			blocks.clear();
			return;
		}
		Iterator<BlockPos> it = blocks.iterator();
		BlockPos pos;
		while (it.hasNext() && (pos = it.next()) != null) {
			if (!isInBounds(pos)) {
				it.remove();
			}
		}
	}

	@Override
	public final void setWorkBoundsMin(BlockPos min) {
		if (min != bbMin) {
			bbMin = min;
			markDirty();
		}
	}

	@Override
	public final void setWorkBoundsMax(BlockPos max) {
		if (max != bbMax) {
			bbMax = max;
			markDirty();
		}
	}

	public AABB getRenderBoundingBox() {
		if (hasWorkBounds()) {
			BlockPos min = getWorkBoundsMin();
			int minX = Math.min(min.getX(), worldPosition.getX());
			int minY = Math.min(min.getY(), worldPosition.getY());
			int minZ = Math.min(min.getZ(), worldPosition.getZ());
			BlockPos max = getWorkBoundsMax();
			int maxX = Math.max(max.getX() + 1, worldPosition.getX() + 1);
			int maxY = Math.max(max.getY() + 1, worldPosition.getY() + 1);
			int maxZ = Math.max(max.getZ() + 1, worldPosition.getZ() + 1);

			return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
		}
		return new AABB(worldPosition);
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		if (tag.contains(BB_MIN_TAG)) {
			bbMin = BlockPos.of(tag.getLong(BB_MIN_TAG));
		}
		if (tag.contains(BB_MAX_TAG)) {
			bbMax = BlockPos.of(tag.getLong(BB_MAX_TAG));
		}
		if (bbMax == BlockPos.ZERO) {
			setWorkBoundsMax(worldPosition.offset(0, 0, 1));
		}
		if (bbMin == BlockPos.ZERO) {
			setWorkBoundsMin(worldPosition.offset(0, 0, 1));
		}
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		if (bbMin != BlockPos.ZERO) {
			tag.putLong(BB_MIN_TAG, bbMin.asLong());
		}
		if (bbMax != BlockPos.ZERO) {
			tag.putLong(BB_MAX_TAG, bbMax.asLong());
		}
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		if (bbMin != BlockPos.ZERO) {
			tag.putLong(BB_MIN_TAG, bbMin.asLong());
		}
		if (bbMax != BlockPos.ZERO) {
			tag.putLong(BB_MAX_TAG, bbMax.asLong());
		}
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		if (tag.contains(BB_MIN_TAG)) {
			bbMin = BlockPos.of(tag.getLong(BB_MIN_TAG));
		}
		if (tag.contains(BB_MAX_TAG)) {
			bbMax = BlockPos.of(tag.getLong(BB_MAX_TAG));
		}
	}
}
