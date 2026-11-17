package net.shadowmage.ancientwarfare.automation.tile.torque.multiblock;

import net.minecraft.world.level.block.state.BlockState;

import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import com.google.common.collect.Lists;
import net.shadowmage.ancientwarfare.automation.config.AWAutomationStatics;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.util.BlockFinder;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.Trig;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class TileWindmillBlade extends TileUpdatable implements ITickableTile {
	private final BlockPos pos;

	private static final String IS_CONTROL_TAG = "isControl";
	private static final String CONTROL_POS_TAG = "controlPos";
	private static final String DIRECTION_TAG = "direction";
	private double bladeRpm = 20.d;
	private double bladeRpt = bladeRpm * AWAutomationStatics.rpmToRpt;

	private BlockPos controlPos;

	private boolean isControl = false;//set to true if this is the control block for a setup
	protected float rotation;//used in rendering
	private float lastRotationDiff;

	/*
	 * the raw size of the windmill in blocks tall
	 */
	private int windmillSize = 0;
	public double energy = 0;
	private Direction windmillDirection = Direction.NORTH;

	public TileWindmillBlade(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.WINDMILL_BLADE_TILE.get(), pos, state);
		this.pos = pos;

	}

	public boolean isControl() {
		return isControl;
	}

	public boolean isFormed() {
		return controlPos != null;
	}

	public Direction getDirection() {
		return windmillDirection;
	}

	public int getWindmillSize() {
		return windmillSize;
	}

	public double getEnergy() {
		return energy;
	}

	public void setEnergy(int energy) {
		this.energy = energy;
	}

	@Override
	public void update() {
		if (isControl) {
			if (!level.isClientSide)
				energy = windmillSize * AWAutomationStatics.windmill_per_size_output;
			else
				updateRotation();
		}

	}

	public float getRotation(float delta) {
		return rotation - lastRotationDiff * (1 - delta);
	}

	private void updateRotation() {
		lastRotationDiff = (float) (bladeRpt * Trig.TORADIANS);
		rotation += lastRotationDiff;
		rotation %= Trig.PI * 2;
	}

	public void blockPlaced() {
		if (!level.isClientSide)
			validateSetup();
	}

	public void blockBroken() {
		if (!level.isClientSide) {
			if (isControl) {
				int expand = (windmillSize - 1) / 2;
				if (expand > 0) {
					if (windmillDirection.getAxis() == Direction.Axis.Z) {
						for (int j = -expand; j < expand + 1; j++) {
							for (int i = -expand; i < expand + 1; i++) {
								WorldTools.getTile(level, pos.offset(i, j, 0), TileWindmillBlade.class).ifPresent(t -> t.setController(null));
							}
						}
					} else if (windmillDirection.getAxis() == Direction.Axis.X) {
						for (int j = -expand; j < expand + 1; j++) {
							for (int i = -expand; i < expand + 1; i++) {
								WorldTools.getTile(level, pos.offset(0, j, i), TileWindmillBlade.class).ifPresent(t -> t.setController(null));
							}
						}
					}
				}
				isControl = false;
			} else if (controlPos != null) {
				WorldTools.getTile(level, controlPos, TileWindmillBlade.class).ifPresent(TileWindmillBlade::validateSetup);
				controlPos = null;
			} else
				informNeighborsToValidate();
		}
	}

	private boolean validateSetup() {
		BlockFinder finder = new BlockFinder(level, getBlockState().getBlock(), s -> true, 9 * 9);
		Pair<BlockPos, BlockPos> corners = finder.cross(pos);
		int minX = corners.getLeft().getX();
		int minY = corners.getLeft().getY();
		int minZ = corners.getLeft().getZ();
		int xSize = corners.getRight().getX() - minX + 1;
		int ySize = corners.getRight().getY() - minY + 1;
		int zSize = corners.getRight().getZ() - minZ + 1;

        /*
		 * if y size >= 5
         * and y size <= 17
         * and h%2==1 (is an odd size, 5, 7, 9, etc)
         * and either x or z size == 1 (one needs to be a single block thick)
         * and either x or z == y size (the other needs to be the same size as height)
         * and is full cube (all block spots filled) (will need to modify this check for those sizes with missing corner blocks, create bit mask 2d array to test for proper setup)
         */
		boolean valid = ySize >= 5 && ySize % 2 == 1 && (zSize == 1 || xSize == 1) && (zSize == ySize || xSize == ySize) && finder.box(corners);
		if (valid) {
			/*
			 * calculate the control block coordinates from the min coordinate and sizes
             */
			int controlX;
			int controlY;
			int controlZ;

			int halfSize = (ySize - 1) / 2;

			controlY = minY + halfSize;//should be the center
			if (xSize > 1)//widest on X axis
			{
				controlX = minX + halfSize;//should be the center
				controlZ = minZ;//only 1 z-coordinate
			} else//widest on Z axis
			{
				controlX = minX;//only 1 x-coordinate
				controlZ = minZ + halfSize;//should be the center
			}
			setValidSetup(finder.getPositions(), controlX, controlY, controlZ, xSize, ySize, zSize);
		} else {
			finder.connect(corners.getLeft(), new BlockPos(xSize, ySize, zSize));
			setInvalidSetup(getAllConnectedBlades());
		}
		return valid;
	}

	private List<BlockPos> getAllConnectedBlades() {
		int maxRadius = 20;
		List<BlockPos> connectedBlocks = Lists.newArrayList();
		List<BlockPos> searchedPositions = Lists.newArrayList();
		BlockPos currentPos = pos;

		connectedBlocks.add(currentPos);
		searchedPositions.add(currentPos);

		getConnectedBlades(maxRadius, connectedBlocks, searchedPositions, currentPos);

		return connectedBlocks;
	}

	private void getConnectedBlades(int maxRadius, List<BlockPos> connectedBlocks, List<BlockPos> searchedPositions, BlockPos currentPos) {
		if (Math.sqrt(currentPos.distSqr(pos)) < maxRadius) {
			for (Direction facing : Direction.values()) {
				BlockPos offsetPos = currentPos.relative(facing);
				if (!searchedPositions.contains(offsetPos) && level.getBlockState(offsetPos).getBlock() == AWAutomationBlocks.WINDMILL_BLADE.get()) {
					connectedBlocks.add(offsetPos);
					searchedPositions.add(offsetPos);
					getConnectedBlades(maxRadius, connectedBlocks, searchedPositions, offsetPos);
				}
			}
		}
	}

	private void informNeighborsToValidate() {
		for (Direction d : Direction.values()) {
			WorldTools.getTile(level, pos.relative(d), TileWindmillBlade.class).ifPresent(TileWindmillBlade::validateSetup);
		}
	}

	private void setController(@Nullable BlockPos pos) {
		boolean dirty = false;
		if (pos == null) {
			if (isControl || controlPos != null)
				dirty = true;
			isControl = false;
			controlPos = null;
		} else {
			if (!pos.equals(controlPos))
				dirty = true;
			controlPos = pos;
		}
		if (dirty) {
			markDirty();
			BlockTools.notifyBlockUpdate(this);
		}
	}

	private void setInvalidSetup(List<BlockPos> set) {
		for (BlockPos pos : set) {
			WorldTools.getTile(level, pos, TileWindmillBlade.class).ifPresent(t -> t.setController(null));
		}
	}

	private void setValidSetup(List<BlockPos> set, int cx, int cy, int cz, int xs, int ys, int zs) {
		setController(new BlockPos(cx, cy, cz));
		Optional<TileWindmillBlade> te = WorldTools.getTile(level, controlPos, TileWindmillBlade.class);
		if (te.isPresent()) {
			te.get().setAsController(xs, ys, zs);
			for (BlockPos pos : set) {
				WorldTools.getTile(level, pos, TileWindmillBlade.class).ifPresent(t -> t.setController(controlPos));
			}
		} else
			setController(null);
	}

	private void setAsController(int xSize, int ySize, int zSize) {
		windmillDirection = xSize == 1 ? Direction.WEST : Direction.NORTH;
		windmillSize = ySize;
		this.isControl = true;
		markDirty();
		BlockTools.notifyBlockUpdate(this);
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		tag.putBoolean(IS_CONTROL_TAG, isControl);
		if (controlPos != null) {
			tag.putLong(CONTROL_POS_TAG, controlPos.asLong());
		}
		if (isControl) {
			tag.putInt("size", windmillSize);
			tag.putByte(DIRECTION_TAG, (byte) windmillDirection.ordinal());
		}
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		controlPos = tag.contains(CONTROL_POS_TAG) ? BlockPos.of(tag.getLong(CONTROL_POS_TAG)) : null;
		isControl = tag.getBoolean(IS_CONTROL_TAG);
		if (isControl) {
			windmillSize = tag.getInt("size");
			windmillDirection = Direction.values()[tag.getByte(DIRECTION_TAG)];
		}
		BlockTools.notifyBlockUpdate(this);
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		controlPos = tag.contains(CONTROL_POS_TAG) ? BlockPos.of(tag.getLong(CONTROL_POS_TAG)) : null;
		isControl = tag.getBoolean(IS_CONTROL_TAG);
		if (isControl) {
			windmillSize = tag.getInt("size");
			windmillDirection = Direction.values()[tag.getByte(DIRECTION_TAG)];
		}
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		tag.putBoolean(IS_CONTROL_TAG, isControl);
		if (controlPos != null) {
			tag.putLong(CONTROL_POS_TAG, controlPos.asLong());
		}
		if (isControl) {
			tag.putInt("size", windmillSize);
			tag.putByte(DIRECTION_TAG, (byte) windmillDirection.ordinal());
		}

	}

	public AABB getRenderBoundingBox() {
		int expand = (windmillSize - 1) / 2;
		return new AABB(pos.getX() - expand, pos.getY() - expand, pos.getZ() - expand, pos.getX() + 1 + expand, pos.getY() + 1 + expand, pos.getZ() + 1 + expand);
	}
}
