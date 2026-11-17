package net.shadowmage.ancientwarfare.core.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.Team;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.shadowmage.ancientwarfare.core.block.BlockResearchStation;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler.IRotatableTile;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.init.AWCoreBlocks;
import net.shadowmage.ancientwarfare.core.interfaces.IInteractableTile;
import net.shadowmage.ancientwarfare.core.interfaces.ITorque.ITorqueTile;
import net.shadowmage.ancientwarfare.core.interfaces.IWorkSite;
import net.shadowmage.ancientwarfare.core.interfaces.IWorker;
import net.shadowmage.ancientwarfare.core.item.ItemResearchBook;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.registry.ResearchRegistry;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchTracker;
import net.shadowmage.ancientwarfare.core.upgrade.WorksiteUpgrade;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/*
 * Port notes: the item handler capability (resourceInventory on every side) is registered in AWCoreBlocks;
 * shouldRefresh is gone (block entities survive state changes of their block in 1.21.1).
 */
public class TileResearchStation extends TileOwned implements IWorkSite, ITorqueTile, IInteractableTile, IRotatableTile, ITickableTile {

	public static final String ORIENTATION_TAG = "orientation";
	private Direction orientation = Direction.NORTH;//default for old blocks

	public final ItemStackHandler bookInventory = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			if (level != null) {
				BlockState iblockstate = level.getBlockState(worldPosition);
				if (iblockstate.getBlock() instanceof BlockResearchStation) {
					level.setBlock(worldPosition, iblockstate.setValue(BlockResearchStation.HAS_BOOK, hasBook()), 3);
				}
			}
			markDirty();
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return ItemResearchBook.getResearcherName(stack) != null ? super.insertItem(slot, stack, simulate) : stack;
		}
	};
	public final ItemStackHandler resourceInventory = new ItemStackHandler(9) {
		@Override
		protected void onContentsChanged(int slot) {
			markDirty();
		}
	};

	private int startCheckDelay = 0;
	private int startCheckDelayMax = 40;

	public boolean useAdjacentInventory;
	public Direction inventoryDirection = Direction.NORTH;
	public Direction inventorySide = Direction.NORTH;

	private double maxEnergyStored = 1600;
	private double maxInput = 100;
	private double storedEnergy;

	public TileResearchStation(BlockPos pos, BlockState state) {
		super(AWCoreBlocks.RESEARCH_STATION_TILE.get(), pos, state);
	}

	@Override
	public void onBlockBroken(BlockState state) {
		InventoryTools.dropItemsInWorld(level, bookInventory, worldPosition);
		InventoryTools.dropItemsInWorld(level, resourceInventory, worldPosition);
	}

	public boolean hasBook() {
		return (!bookInventory.getStackInSlot(0).isEmpty());
	}

	@Override
	public Set<WorksiteUpgrade> getUpgrades() {
		return EnumSet.noneOf(WorksiteUpgrade.class);
	}//NOOP

	@Override
	public Set<WorksiteUpgrade> getValidUpgrades() {
		return EnumSet.noneOf(WorksiteUpgrade.class);
	}//NOOP

	@Override
	public void addUpgrade(WorksiteUpgrade upgrade) {
	}//NOOP

	@Override
	public void removeUpgrade(WorksiteUpgrade upgrade) {
	}//NOOP

	@Override
	public float getClientOutputRotation(Direction from, float delta) {
		return 0;
	}

	@Override
	public boolean useOutputRotation(@Nullable Direction from) {
		return false;
	}

	@Override
	public double getMaxTorqueOutput(Direction from) {
		return 0;
	}

	@Override
	public boolean canOutputTorque(Direction towards) {
		return false;
	}

	@Override
	public double addTorque(@Nullable Direction from, double energy) {
		if (canInputTorque(from)) {
			if (energy + getTorqueStored(from) > getMaxTorque(from)) {
				energy = getMaxTorque(from) - getTorqueStored(from);
			}
			if (energy > getMaxTorqueInput(from)) {
				energy = getMaxTorqueInput(from);
			}
			storedEnergy += energy;
			return energy;
		}
		return 0;
	}

	@Override
	public double getMaxTorque(@Nullable Direction from) {
		return maxEnergyStored;
	}

	@Override
	public double getTorqueStored(@Nullable Direction from) {
		return storedEnergy;
	}

	@Override
	public double getMaxTorqueInput(@Nullable Direction from) {
		return maxInput;
	}

	@Override
	public boolean canInputTorque(Direction from) {
		return true;
	}

	public String getCrafterName() {
		return ItemResearchBook.getResearcherName(bookInventory.getStackInSlot(0));
	}

	@Override
	public void update() {
		if (!hasLevel() || level.isClientSide) {
			return;
		}
		String name = getCrafterName();
		if (name == null) {
			return;
		}
		Optional<String> goal = ResearchTracker.INSTANCE.getCurrentGoal(level, name);
		boolean started = goal.isPresent();
		if (started && storedEnergy >= AWCoreStatics.energyPerResearchUnit) {
			workTick(name, goal.get(), 1);
		} else if (!started) {
			startCheckDelay--;
			if (startCheckDelay <= 0) {
				tryStartNextResearch(name);
			}
		}
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		tag.putInt(ORIENTATION_TAG, orientation.ordinal());
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		orientation = Direction.values()[tag.getInt(ORIENTATION_TAG)];
		BlockTools.notifyBlockUpdate(this);
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		NBTHelper.deserializeHandler(bookInventory, tag.getCompound("bookInventory"));
		NBTHelper.deserializeHandler(resourceInventory, tag.getCompound("resourceInventory"));
		this.useAdjacentInventory = tag.getBoolean("useAdjacentInventory");
		this.storedEnergy = tag.getDouble("storedEnergy");
		if (tag.contains(ORIENTATION_TAG)) {
			setPrimaryFacing(Direction.values()[tag.getInt(ORIENTATION_TAG)]);
		}
		this.inventoryDirection = Direction.values()[tag.getInt("inventoryDirection")];
		this.inventorySide = Direction.values()[tag.getInt("inventorySide")];
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		tag.put("bookInventory", NBTHelper.serializeHandler(bookInventory));
		tag.put("resourceInventory", NBTHelper.serializeHandler(resourceInventory));
		tag.putBoolean("useAdjacentInventory", useAdjacentInventory);
		tag.putDouble("storedEnergy", storedEnergy);
		tag.putInt(ORIENTATION_TAG, orientation.ordinal());
		tag.putInt("inventoryDirection", inventoryDirection.ordinal());
		tag.putInt("inventorySide", inventorySide.ordinal());
	}

	@Override
	public boolean hasWork() {
		return storedEnergy < maxEnergyStored;
	}

	private void workTick(String name, String goal, int tickCount) {
		ResearchGoal g1 = ResearchRegistry.getResearch(goal);
		int progress = ResearchTracker.INSTANCE.getProgress(level, name);
		progress += tickCount;
		if (progress >= g1.getTotalResearchTime()) {
			ResearchTracker.INSTANCE.finishResearch(level, getCrafterName(), goal);
			tryStartNextResearch(name);
		} else {
			ResearchTracker.INSTANCE.setProgress(level, name, progress);
		}
		storedEnergy -= AWCoreStatics.energyPerResearchUnit;
	}

	private void tryStartNextResearch(String name) {
		List<String> queue = ResearchTracker.INSTANCE.getResearchQueueFor(level, name);
		if (!queue.isEmpty()) {
			String goalName = queue.get(0);
			ResearchGoal goalInstance = ResearchRegistry.getResearch(goalName);
			if (goalInstance == null) {
				return;
			}
			if (goalInstance.tryStart(resourceInventory)) {
				ResearchTracker.INSTANCE.startResearch(level, name, goalName);
			} else if (useAdjacentInventory) {
				boolean started = WorldTools.getItemHandlerFromTile(level, worldPosition.relative(inventoryDirection), inventorySide).map(goalInstance::tryStart)
						.orElse(false);
				if (started) {
					ResearchTracker.INSTANCE.startResearch(level, name, goalName);
				}
			}
		}
		startCheckDelay = startCheckDelayMax;
	}

	@Override
	public WorkType getWorkType() {
		return WorkType.RESEARCH;
	}

	@Override
	public final Team getTeam() {
		return level.getScoreboard().getPlayersTeam(getOwner().getName());
	}

	@Override
	public void addEnergyFromWorker(IWorker worker) {
		storedEnergy += AWCoreStatics.energyPerWorkUnit * worker.getWorkEffectiveness(getWorkType());
		if (storedEnergy > getMaxTorque(null)) {
			storedEnergy = getMaxTorque(null);
		}
	}

	@Override
	public void addEnergyFromPlayer(Player player) {
		storedEnergy += AWCoreStatics.energyPerWorkUnit;
		if (storedEnergy > getMaxTorque(null)) {
			storedEnergy = getMaxTorque(null);
		}
	}

	@Override
	public boolean onBlockClicked(Player player, InteractionHand hand) {
		if (!player.level().isClientSide) {
			NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_RESEARCH_STATION, worldPosition);
		}
		return true;
	}

	@Override
	public Direction getPrimaryFacing() {
		return orientation;
	}

	@Override
	public void setPrimaryFacing(Direction face) {
		this.orientation = face;
	}

	@Override
	public double drainTorque(Direction from, double energy) {
		return 0;
	}
}
