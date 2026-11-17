package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.config.AWCoreStatics;
import net.shadowmage.ancientwarfare.core.interfaces.IWorkSite;
import net.shadowmage.ancientwarfare.core.interfaces.IWorker;
import net.shadowmage.ancientwarfare.core.owner.IOwnable;
import net.shadowmage.ancientwarfare.core.owner.Owner;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.upgrade.WorksiteUpgrade;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBB;
import net.shadowmage.ancientwarfare.structure.template.build.StructureBuilderTicked;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;

public class TileStructureBuilder extends TileUpdatable implements IWorkSite, IOwnable, ITickableTile {
	private static final String BUILDER_TAG = "builder";
	private static final String BB_MIN_TAG = "bbMin";
	private static final String BB_MAX_TAG = "bbMax";
	private Owner owner = Owner.EMPTY;

	private StructureBuilderTicked builder;
	private boolean shouldRemove = false;
	private boolean isStarted = false;
	private int workDelay = 20;

	private double maxEnergyStored;
	private double maxInput;
	private double storedEnergy;
	public StructureBB clientBB;

	public TileStructureBuilder(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.STRUCTURE_BUILDER_TICKED_TILE.get(), pos, state);
	}

	protected TileStructureBuilder(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		maxEnergyStored = AWCoreStatics.energyPerWorkUnit * 3;
		maxInput = AWCoreStatics.energyPerWorkUnit;
	}

	public AABB getRenderBoundingBox() {
		return clientBB != null ? new AABB(Vec3.atLowerCornerOf(clientBB.min), Vec3.atLowerCornerOf(clientBB.max)) : new AABB(getBlockPos());
	}

	@Override
	public Set<WorksiteUpgrade> getUpgrades() {
		return EnumSet.noneOf(WorksiteUpgrade.class);
	}

	@Override
	public Set<WorksiteUpgrade> getValidUpgrades() {
		return EnumSet.noneOf(WorksiteUpgrade.class);
	}//NOOP

	@Override
	public void addUpgrade(WorksiteUpgrade upgrade) {
		//NOOP
	}

	@Override
	public void removeUpgrade(WorksiteUpgrade upgrade) {
		//NOOP
	}

	@Override
	public float getClientOutputRotation(Direction from, float delta) {
		return 0;
	}

	@Override
	public boolean useOutputRotation(Direction from) {
		return false;
	}

	@Override
	public double addTorque(Direction from, double energy) {
		if (canInputTorque(from)) {
			if (energy + getTorqueStored(null) > getMaxTorque(null)) {
				energy = getMaxTorque(null) - getTorqueStored(null);
			}
			if (energy > getMaxTorqueInput(null)) {
				energy = getMaxTorqueInput(null);
			}
			storedEnergy += energy;
			return energy;
		}
		return 0;
	}

	@Override
	public double getMaxTorque(Direction from) {
		return maxEnergyStored;
	}

	@Override
	public double getTorqueStored(Direction from) {
		return storedEnergy;
	}

	@Override
	public double getMaxTorqueInput(Direction from) {
		return maxInput;
	}

	@Override
	public boolean canInputTorque(Direction from) {
		return true;
	}

	@Override
	public void update() {
		if (!hasLevel() || level.isClientSide) {
			return;
		}
		if (shouldRemove || builder == null || builder.invalid || builder.isFinished()) {
			shouldRemove = true;
			level.removeBlock(worldPosition, false);
			return;
		}
		if (builder.getLevel() == null) {
			builder.setWorld(level);
		}
		if (true || true) {
			if (storedEnergy >= AWCoreStatics.energyPerWorkUnit) {
				storedEnergy -= AWCoreStatics.energyPerWorkUnit;
				processWork();
			}
		} else {
			if (workDelay-- <= 0) {
				processWork();
				workDelay = 20;
			}
		}
	}

	private void processWork() {
		isStarted = true;
		builder.tick();
	}

	/*
	 * should be called immediately after the tile-entity is set into the level
	 * from the ItemBlockStructureBuilder item onBlockPlaced code
	 */
	@Override
	public void setOwner(Player player) {
		this.owner = new Owner(player);
	}

	@Override
	public void setOwner(Owner owner) {
		this.owner = owner;
	}

	@Override
	public boolean isOwner(Player player) {
		return owner.isOwnerOrSameTeamOrFriend(player);
	}

	@Override
	public Owner getOwner() {
		return owner;
	}

	/*
	 * should be called immediately after the tile-entity is set into the level
	 * from the ItemBlockStructureBuilder item onBlockPlaced code<br>
	 * the passed in builder must be valid (have a valid structure), and must not
	 * be null
	 */
	public void setBuilder(StructureBuilderTicked builder) {
		this.builder = builder;
	}

	public void onBlockBroken(BlockState state) {
		//noop
	}

	public void onBlockClicked(Player player) {
		if (builder.hasClearedArea()) {
			int pass = builder.getPass() + 1;
			int max = builder.getMaxPasses();
			float percent = builder.getPercentDoneWithPass() * 100.f;
			String perc = String.format("%.2f", percent) + "%";
			player.sendSystemMessage(Component.translatable("guistrings.structure.builder.state", perc, pass, max));
		} else {
			float percent = builder.getPercentDoneClearing() * 100.f;
			String perc = String.format("%.2f", percent) + "%";
			player.sendSystemMessage(Component.translatable("guistrings.structure.builder.clear_state", perc));
		}
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		if (builder == null) {
			return;
		}
		StructureBB bb = builder.getBoundingBox();
		tag.putLong(BB_MIN_TAG, bb.min.asLong());
		tag.putLong(BB_MAX_TAG, bb.max.asLong());
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		if (tag.contains(BB_MIN_TAG) && tag.contains(BB_MAX_TAG)) {
			clientBB = new StructureBB(BlockPos.of(tag.getLong(BB_MIN_TAG)), BlockPos.of(tag.getLong(BB_MAX_TAG)));
		}
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		if (tag.contains(BUILDER_TAG)) {
			builder = new StructureBuilderTicked();
			builder.readFromNBT(tag.getCompound(BUILDER_TAG));
		} else {
			this.shouldRemove = true;
		}
		this.isStarted = tag.getBoolean("started");
		this.storedEnergy = tag.getDouble("storedEnergy");
		owner = Owner.deserializeFromNBT(tag);
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		if (builder != null) {
			CompoundTag builderTag = new CompoundTag();
			builder.writeToNBT(builderTag);
			tag.put(BUILDER_TAG, builderTag);
		}
		tag.putBoolean("started", isStarted);
		tag.putDouble("storedEnergy", storedEnergy);
		owner.serializeToNBT(tag);
	}

	//******************************************WORKSITE************************************************//
	@Override
	public boolean hasWork() {
		return storedEnergy < maxEnergyStored;
	}

	@Override
	public WorkType getWorkType() {
		return WorkType.CRAFTING;
	}

	@Override
	public final net.minecraft.world.scores.Team getTeam() {
		return level.getScoreboard().getPlayersTeam(owner.getName());
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
	public double getMaxTorqueOutput(Direction from) {
		return 0;
	}

	@Override
	public boolean canOutputTorque(Direction towards) {
		return false;
	}

	@Override
	public double drainTorque(Direction from, double energy) {
		return 0;
	}

	public StructureBuilderTicked getBuilder() {
		return builder;
	}
}
