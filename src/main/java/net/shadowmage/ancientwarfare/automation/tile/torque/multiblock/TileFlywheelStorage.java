package net.shadowmage.ancientwarfare.automation.tile.torque.multiblock;

import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.init.AWAutomationBlocks;

import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.config.AWAutomationStatics;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.PacketBlockEvent;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.util.BlockFinder;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.Trig;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class TileFlywheelStorage extends TileUpdatable implements ITickableTile {
	private final BlockPos pos;

	public TileFlywheelStorage(BlockPos pos, BlockState state) {
		super(AWAutomationBlocks.FLYWHEEL_STORAGE_TILE.get(), pos, state);
		this.pos = pos;
	}


	private static final String CONTROLLER_POS_TAG = "controllerPos";
	private static final String IS_CONTROL_TAG = "isControl";
	private static final String SET_WIDTH_TAG = "setWidth";
	private static final String SET_HEIGHT_TAG = "setHeight";
	public BlockPos controllerPos;
	public boolean isControl = false;//set to true if this is the control block for a setup
	public int setWidth;
	public int setHeight;
	private int setCube;//validation params, only 'valid' in the control block.  used by rendering
	public double storedEnergy;
	public double maxEnergyStored;
	private double maxRpm = 100;
	public double torqueLoss;
	public double rotation;//used in rendering
	public double lastRotationDiff;
	private int clientEnergy;
	private int clientDestEnergy;
	private int networkUpdateTicks = 0;

	private boolean setupChecked = false;

	@Override
	public void update() {
		if (!setupChecked) {
			setupChecked = true;
			//Port addition: a storage block that was never set up (placed without a placer, e.g. /setblock) validates itself
			//on its first tick; 1.12 only did this from onBlockPlacedBy and left such a block without controller (invisible)
			if (!level.isClientSide && controllerPos == null && !isControl) {
				validateSetup();
			}
		}
		if (isControl) {
			if (level.isClientSide) {
				clientNetworkUpdate();
			} else {
				serverNetworkUpdate();
				applyPowerLoss();
			}
		}
	}

	protected void clientNetworkUpdate() {
		updateRotation();
		if (networkUpdateTicks > 0) {
			int diff = clientDestEnergy - clientEnergy;
			clientEnergy += diff / networkUpdateTicks;
			networkUpdateTicks--;
		}
	}

	protected void applyPowerLoss() {
		double eff = 1.d - getEfficiency();
		eff *= 0.1d;
		torqueLoss = storedEnergy * eff;
		storedEnergy -= torqueLoss;
	}

	/* 1.12 block metadata: the tier of the storage block */
	private int getTierMeta() {
		BlockState state = getBlockState();
		return state.hasProperty(AutomationProperties.TIER) ? state.getValue(AutomationProperties.TIER).getMeta() : 0;
	}

	protected double getEfficiency() {
		int meta = getTierMeta();
		switch (meta) {
			case 0:
				return AWAutomationStatics.low_efficiency_factor;
			case 1:
				return AWAutomationStatics.med_efficiency_factor;
			case 2:
				return AWAutomationStatics.high_efficiency_factor;
			default:
				return AWAutomationStatics.low_efficiency_factor;
		}
	}

	private void serverNetworkUpdate() {
		if (!AWAutomationStatics.enable_energy_network_updates) {
			return;
		}
		networkUpdateTicks--;
		if (networkUpdateTicks <= 0) {
			double percentStored = storedEnergy / maxEnergyStored;
			int total = (int) (percentStored * 100.d);
			if (total != clientEnergy) {
				clientEnergy = total;
				sendDataToClient(1, clientEnergy);
			}
			networkUpdateTicks = AWAutomationStatics.energyMinNetworkUpdateFrequency;
		}
	}

	protected final void sendDataToClient(int type, int data) {
		PacketBlockEvent pkt = new PacketBlockEvent(pos, getBlockState().getBlock(), (short) type, (short) data);
		NetworkHandler.sendToAllTrackingChunk(level, pos.getX() >> 4, pos.getZ() >> 4, pkt);
	}

	@Override
	public boolean triggerEvent(int a, int b) {
		if (level.isClientSide && a == 1) {
			clientDestEnergy = b;
			networkUpdateTicks = AWAutomationStatics.energyMinNetworkUpdateFrequency;
		}
		return true;
	}

	private void updateRotation() {
		double rpm = (double) clientEnergy * 0.01d * this.maxRpm;
		lastRotationDiff = -(rpm * AWAutomationStatics.rpmToRpt) * Trig.TORADIANS;
		rotation += lastRotationDiff;
		rotation %= Trig.PI * 2;
	}

	public void blockBroken() {
		informNeighborsToValidate();
	}

	public final void blockPlaced() {
		validateSetup();
	}

	private void setController(@Nullable BlockPos controllerPos) {
		this.controllerPos = controllerPos;
		isControl = (controllerPos == null || controllerPos.equals(pos));
		if (controllerPos == null) {
			setWidth = 1;
			setHeight = 1;
			setCube = 1;
		}

		markDirty();
		BlockTools.notifyBlockUpdate(this);
	}

	private boolean validateSetup() {
		BlockFinder finder = new BlockFinder(level, getBlockState().getBlock(), s -> s.getValue(AutomationProperties.TIER).getMeta() == getTierMeta(), 30);
		Pair<BlockPos, BlockPos> corners = finder.cross(pos);
		int minX = corners.getLeft().getX();
		int minY = corners.getLeft().getY();
		int minZ = corners.getLeft().getZ();
		int w = corners.getRight().getX() - minX + 1;
		int h = corners.getRight().getY() - minY + 1;
		int l = corners.getRight().getZ() - minZ + 1;
		boolean valid = w == l && (w == 1 || w == 3) && finder.box(corners);
		if (valid) {
			int cx = w == 1 ? minX : minX + 1;
			int cz = l == 1 ? minZ : minZ + 1;
			setValidSetup(finder.getPositions(), cx, minY, cz, w, h, getTierMeta());
		} else {
			setInvalidSetup(finder.getAllConnectedBlocks(pos));
		}
		return valid;
	}

	private void setValidSetup(List<BlockPos> set, int cx, int cy, int cz, int size, int height, int type) {
		controllerPos = new BlockPos(cx, cy, cz);
		Optional<TileFlywheelStorage> te = WorldTools.getTile(level, controllerPos, TileFlywheelStorage.class);
		if (te.isPresent()) {
			te.get().setAsController(size, height, type);
			for (BlockPos pos : set) {
				WorldTools.getTile(level, pos, TileFlywheelStorage.class).ifPresent(t -> t.setController(controllerPos));
			}
		} else {
			controllerPos = null;
		}
	}

	private void setAsController(int size, int height, int type) {
		this.isControl = true;
		this.setWidth = size;
		this.setHeight = height;
		this.setCube = size * size * height;
		double energyPerBlockForType = 1600;
		switch (type) {
			case 0:
				energyPerBlockForType = AWAutomationStatics.low_storage_energy_max;
				break;
			case 1:
				energyPerBlockForType = AWAutomationStatics.med_storage_energy_max;
				break;
			case 2:
				energyPerBlockForType = AWAutomationStatics.high_storage_energy_max;
				break;
		}
		this.maxEnergyStored = (double) setCube * energyPerBlockForType;
		markDirty();
		BlockTools.notifyBlockUpdate(this);
	}

	private void setInvalidSetup(List<BlockPos> set) {
		setController(null);
		for (BlockPos pos : set) {
			WorldTools.getTile(level, pos, TileFlywheelStorage.class).ifPresent(t -> t.setController(null));
		}
	}

	private void informNeighborsToValidate() {
		for (Direction d : Direction.values()) {
			WorldTools.getTile(level, pos.relative(d), TileFlywheelStorage.class).ifPresent(TileFlywheelStorage::validateSetup);
		}
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		if (controllerPos != null) {
			tag.putLong(CONTROLLER_POS_TAG, controllerPos.asLong());
		}
		if (isControl) {
			tag.putBoolean(IS_CONTROL_TAG, true);
			tag.putInt(SET_WIDTH_TAG, setWidth);
			tag.putInt(SET_HEIGHT_TAG, setHeight);
			tag.putInt("clientEnergy", clientEnergy);
		}
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		controllerPos = tag.contains(CONTROLLER_POS_TAG) ? BlockPos.of(tag.getLong(CONTROLLER_POS_TAG)) : null;
		isControl = tag.getBoolean(IS_CONTROL_TAG);
		if (isControl) {
			setHeight = tag.getInt(SET_HEIGHT_TAG);
			setWidth = tag.getInt(SET_WIDTH_TAG);
			setCube = setWidth * setWidth * setHeight;
			clientEnergy = tag.getInt("clientEnergy");
		}
		BlockTools.notifyBlockUpdate(this);
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		controllerPos = tag.contains(CONTROLLER_POS_TAG) ? BlockPos.of(tag.getLong(CONTROLLER_POS_TAG)) : null;
		isControl = tag.getBoolean(IS_CONTROL_TAG);
		if (isControl) {
			maxEnergyStored = tag.getDouble("maxEnergy");
			setHeight = tag.getInt(SET_HEIGHT_TAG);
			setWidth = tag.getInt(SET_WIDTH_TAG);
			setCube = setWidth * setWidth * setHeight;
		}
		storedEnergy = tag.getDouble("storedEnergy");
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		if (controllerPos != null) {
			tag.putLong(CONTROLLER_POS_TAG, controllerPos.asLong());
		}
		if (isControl) {
			tag.putBoolean(IS_CONTROL_TAG, true);
			tag.putDouble("maxEnergy", maxEnergyStored);
			tag.putInt(SET_WIDTH_TAG, setWidth);
			tag.putInt(SET_HEIGHT_TAG, setHeight);
		}
		tag.putDouble("storedEnergy", storedEnergy);
	}

	public AABB getRenderBoundingBox() {
		return new AABB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 2, pos.getY() + setHeight, pos.getZ() + 2);
	}

}
