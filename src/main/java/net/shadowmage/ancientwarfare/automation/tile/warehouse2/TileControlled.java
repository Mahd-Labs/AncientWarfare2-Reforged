package net.shadowmage.ancientwarfare.automation.tile.warehouse2;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class TileControlled extends TileUpdatable implements IControlledTile, ITickableTile {

	public TileControlled(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	private static final String CONTROLLER_POSITION_TAG = "controllerPosition";
	private boolean init;
	private TileWarehouseBase controller;
	private BlockPos controllerPosition;

	@Override
	public final void update() {
		if (!init) {
			init = true;
			if (!loadController() && controllerPosition == null) {
				searchForController();
			}
		} else if (controller == null && controllerPosition != null && level.getGameTime() % 20 == 0) {
			//the controller's chunk was not loaded when this tile loaded: the link is kept and tried again
			loadController();
		}
		updateTile();
	}

	private boolean loadController() {
		BlockPos pos = controllerPosition;
		if (pos != null && controller == null) {
			if (!WorldTools.isChunkLoaded(level, pos)) {
				//port fix: a link is never dropped because the other side is not loaded (1.12 cleared the position on the
				//first try); it is only cleared when that chunk is loaded and holds no controller for this tile
				return false;
			}
			controllerPosition = null;
			WorldTools.getTile(level, pos, IControllerTile.class).filter(this::isValidController).ifPresent(t -> t.addControlledTile(this));
		}
		//port fix: when the controller had already taken this tile over (it loaded and scanned first), the saved position
		//was dropped here and the next save lost it
		if (controller != null) {
			controllerPosition = controller.getPosisition();
		}
		return controller != null;
	}

	protected abstract void updateTile();

	public void searchForController() {
		BlockPos min = worldPosition.offset(-16, -4, -16);
		BlockPos max = worldPosition.offset(16, 4, 16);
		for (BlockEntity te : WorldTools.getTileEntitiesInArea(level, min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ())) {
			if (te instanceof IControllerTile && isValidController((IControllerTile) te)) {
				((IControllerTile) te).addControlledTile(this);
				break;
			}
		}
	}

	@Override
	public boolean isValidController(IControllerTile tile) {
		return BlockTools.isPositionWithinBounds(getBlockPos(), tile.getWorkBoundsMin(), tile.getWorkBoundsMax());
	}

	@Override
	public final void setRemoved() {
		/*
		 * Port notes: 1.12 invalidate() ran only when the block was removed. 1.21 calls setRemoved for every block
		 * entity of a chunk that unloads as well (LevelChunk.clearAllBlockEntities), and an exception here keeps the
		 * chunk from being saved. On an unload the tile only leaves the lists of a controller that is still loaded (a
		 * controller cannot use a storage block it cannot reach); the saved controller position is kept, so the tile
		 * finds its controller again when the chunk comes back. Nothing here may throw.
		 */
		TileWarehouseBase oldController = controller;
		controller = null;
		init = false;
		if (oldController != null && !oldController.isRemoved()) {
			try {
				oldController.removeControlledTile(this);
			}
			catch (RuntimeException e) {
				net.shadowmage.ancientwarfare.automation.AncientWarfareAutomation.LOG.error("Warehouse tile at {} could not be detached from its controller at {}: ", worldPosition, oldController.getBlockPos(), e);
			}
		}
		super.setRemoved();
	}

	@Override
	public final void clearRemoved() {
		super.clearRemoved();
		if (controller != null) {
			controller.addControlledTile(this);
		}
	}

	@Override
	public final void setController(@Nullable TileWarehouseBase tile) {
		this.controller = tile;
		this.controllerPosition = tile == null ? null : tile.getPosisition();
		onControllerChanged();
	}

	protected void onControllerChanged() {
	}

	@Override
	public final Optional<TileWarehouseBase> getController() {
		return Optional.ofNullable(controller);
	}

	@Override
	public BlockPos getPosition() {
		return getBlockPos();
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		if (tag.contains(CONTROLLER_POSITION_TAG)) {
			controllerPosition = BlockPos.of(tag.getLong(CONTROLLER_POSITION_TAG));
		}
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		if (controllerPosition != null) {
			tag.putLong(CONTROLLER_POSITION_TAG, controllerPosition.asLong());
		}
	}

	@Override
	public final boolean equals(Object obj) {
		return this == obj
				|| (obj instanceof TileControlled && this.level == ((TileControlled) obj).getLevel() && this.getBlockPos().equals(((TileControlled) obj).getBlockPos()));
	}

	@Override
	public final int hashCode() {
		return this.getBlockPos().hashCode();
	}
}
