package net.shadowmage.ancientwarfare.automation.tile.torque;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.shadowmage.ancientwarfare.automation.block.BlockTorqueBase;
import net.shadowmage.ancientwarfare.automation.config.AWAutomationStatics;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableTile;
import net.shadowmage.ancientwarfare.core.interfaces.IInteractableTile;
import net.shadowmage.ancientwarfare.core.interfaces.ITorque.ITorqueTile;
import net.shadowmage.ancientwarfare.core.interfaces.ITorque.TorqueCell;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.PacketBlockEvent;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/*
 * Port notes: the RedstoneFlux side (IEnergyReceiver, RF neighbor cache, RFProxy transfer) is dropped. The facing is
 * the block state property BlockTorqueBase.FACING; the orientation field (and its NBT) mirrors it. validate /
 * invalidate -> clearRemoved / setRemoved, receiveClientEvent -> triggerEvent.
 */
public abstract class TileTorqueBase extends TileUpdatable implements ITorqueTile, IInteractableTile, IRotatableTile, ITickableTile {

	public TileTorqueBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		orientation = facingFromState(state, orientation);
	}

	/*
	 * Port addition: the facing lives in the block state (BlockTorqueBase.FACING); the orientation field mirrors it.
	 */
	private static Direction facingFromState(BlockState state, Direction fallback) {
		return state.hasProperty(BlockTorqueBase.FACING) ? state.getValue(BlockTorqueBase.FACING) : fallback;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setBlockState(BlockState state) {
		super.setBlockState(state);
		Direction facing = facingFromState(state, orientation);
		if (facing != orientation) {
			orientation = facing;
			invalidateTorqueCache();
		}
	}


	public static final int DIRECTION_LENGTH = Direction.values().length;
	private static final String ORIENTATION_TAG = "orientation";
	/*
	 * The primary facing direction for this tile.  Default to north for uninitialized tiles (null is not a valid value)
	 */
	protected Direction orientation = Direction.NORTH;

	/*
	 * used by server to limit packet sending<br>
	 * used by client for lerp-ticks for lerping between power states
	 */
	protected int networkUpdateTicks;

	private ITorqueTile[] torqueCache;

	/*
	 * helper vars to be used by tiles during updating, to cache in/out/loss values<br>
	 * IMPORTANT: should NOT be relied upon for calculations, only for use for display purposes<br>
	 * E.G. A tile may choose to -not- update these vars.<br>
	 * However, best effort should be made to update these vars accurately.<br><br>
	 * Generated energy should be counted as 'in'<br>
	 * Any directly output energy should be counted as 'out'<br>
	 * Only direct power loss from transmission efficiency or per-tick loss should be counted for 'loss'
	 */
	protected double torqueIn;
	protected double torqueOut;
	protected double torqueLoss;
	protected double prevEnergy;

	//************************************** NEIGHBOR CACHE UPDATING ***************************************//

	public final ITorqueTile[] getTorqueCache() {
		if (torqueCache == null) {
			buildTorqueCache();
		}
		return torqueCache;
	}

	private void buildTorqueCache() {
		if (!(level != null)) {
			throw new RuntimeException("Attempt to build neighbor cache on null level!!");
		}
		ITorqueTile[] torqueCache = new ITorqueTile[DIRECTION_LENGTH];
		Direction dir;
		for (int i = 0; i < torqueCache.length; i++) {
			dir = Direction.values()[i];
			if (!canOutputTorque(dir) && !canInputTorque(dir)) {
				continue;
			}
			BlockPos offsetPos = worldPosition.relative(dir);
			if (!level.isLoaded(offsetPos)) {
				continue;
			}
			final int index = i;
			WorldTools.getTile(level, offsetPos, ITorqueTile.class).ifPresent(t -> torqueCache[index] = t);
		}
		this.torqueCache = torqueCache;
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		invalidateTorqueCache();
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		invalidateTorqueCache();
	}

	public void onNeighborTileChanged() {
		invalidateTorqueCache();
	}

	private void invalidateTorqueCache() {
		torqueCache = null;
		onNeighborCacheInvalidated();
	}

	protected void onNeighborCacheInvalidated() {

	}

	//************************************** generic stuff ***************************************//

	@Override
	public final void setPrimaryFacing(Direction d) {
		this.orientation = d;
		BlockState state = getBlockState();
		if (level != null && state.hasProperty(BlockTorqueBase.FACING) && state.getValue(BlockTorqueBase.FACING) != d) {
			level.setBlock(worldPosition, state.setValue(BlockTorqueBase.FACING, d), 3);
		}
		this.level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		this.invalidateTorqueCache();
		BlockTools.notifyBlockUpdate(this);
	}

	@Override
	public final Direction getPrimaryFacing() {
		return orientation;
	}

	@Override
	public boolean onBlockClicked(Player player, @Nullable InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.isEmpty()) {
			if (!level.isClientSide) {
				player.sendSystemMessage(Component.translatable("guistrings.automation.torque.values", String.format("%.2f", getTotalTorque()), String.format("%.2f", getTorqueIn()), String.format("%.2f", getTorqueOut()), String.format("%.2f", getTorqueLoss())));
			}
			return true;
		}
		return false;
	}

	//************************************** Utility Methods ***************************************//

	protected void updateRotation() {
		throw new UnsupportedOperationException();
	}

	protected void clientNetworkUpdate() {
		throw new UnsupportedOperationException();
	}

	protected void serverNetworkSynch() {
		throw new UnsupportedOperationException();
	}

	protected abstract void handleClientRotationData(Direction side, int value);

	/*
	 * @return the TOTAL amount stored in the entire tile (not just one side), used by on-right-click functionality
	 */
	protected abstract double getTotalTorque();

	/*
	 * @return the total output of torque for the tick
	 */
	private double getTorqueOut() {
		return torqueOut;
	}

	/*
	 * @return the total input of torque for the tick
	 */
	private double getTorqueIn() {
		return torqueIn;
	}

	/*
	 * @return the total torque lost (destroyed, gone completely) for the tick
	 */
	private double getTorqueLoss() {
		return torqueLoss;
	}

	protected final void serverNetworkUpdate() {
		networkUpdateTicks--;
		if (networkUpdateTicks <= 0) {
			networkUpdateTicks = AWAutomationStatics.energyMinNetworkUpdateFrequency;
			serverNetworkSynch();
		}
	}

	protected void sendSideRotation(Direction side, int value) {
		int valueBits = (value & 0xff);
		sendDataToClient(side.ordinal(), valueBits);
	}

	protected final double transferPowerTo(Direction from) {
		double transferred = 0;
		ITorqueTile[] tc = getTorqueCache();
		if (tc[from.ordinal()] != null) {
			if (tc[from.ordinal()].canInputTorque(from.getOpposite())) {
				return drainTorque(from, tc[from.ordinal()].addTorque(from.getOpposite(), getMaxTorqueOutput(from)));
			}
		}
		return transferred;
	}

	protected final double applyPowerDrain(TorqueCell cell) {
		double e = cell.getEnergy();
		cell.setEnergy(e * cell.getEfficiency());
		return cell.getEnergy() - e;
	}

	protected final void sendDataToClient(int type, int data) {
		PacketBlockEvent pkt = new PacketBlockEvent(worldPosition, getBlockState().getBlock(), (short) type, (short) data);
		NetworkHandler.sendToAllTrackingChunk(level, worldPosition.getX() >> 4, worldPosition.getZ() >> 4, pkt);
	}

	protected final float getRenderRotation(double rotation, double lastRotationDiff, float partialTicks) {
		return (float) (rotation - (lastRotationDiff * (1 - partialTicks)));
	}

	@Override
	public boolean triggerEvent(int a, int b) {
		if (level.isClientSide && a < DIRECTION_LENGTH) {
			networkUpdateTicks = AWAutomationStatics.energyMinNetworkUpdateFrequency;
			handleClientRotationData(Direction.values()[a], b);
		}
		return true;
	}

	//************************************** NBT / DATA PACKET ***************************************//

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		orientation = facingFromState(getBlockState(), Direction.values()[tag.getInt(ORIENTATION_TAG)]);
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		tag.putInt(ORIENTATION_TAG, orientation.ordinal());

	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		tag.putInt(ORIENTATION_TAG, orientation.ordinal());
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		orientation = facingFromState(getBlockState(), Direction.values()[tag.getInt(ORIENTATION_TAG)]);
		this.invalidateTorqueCache(); //TODO is this needed on client??
		BlockTools.notifyBlockUpdate(this);
	}
}
