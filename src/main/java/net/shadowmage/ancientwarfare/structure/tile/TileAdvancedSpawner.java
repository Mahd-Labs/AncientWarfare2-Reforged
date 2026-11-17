package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.tile.ITickableTile;
import net.shadowmage.ancientwarfare.core.tile.IBlockBreakHandler;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.util.BlockTools;

public class TileAdvancedSpawner extends TileUpdatable implements ITickableTile, IBlockBreakHandler {

	private SpawnerSettings settings = new SpawnerSettings();

	public TileAdvancedSpawner(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.ADVANCED_SPAWNER_TILE.get(), pos, state);
	}

	protected TileAdvancedSpawner(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		settings.setWorld(level, worldPosition);
	}


	@Override
	public void update() {
		if (!hasLevel() || level.isClientSide) {
			return;
		}
		if (!settings.hasLevel()) {
			settings.setWorld(level, worldPosition);
		}
		settings.onUpdate();
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		CompoundTag ntag = new CompoundTag();
		settings.writeToNBT(ntag);
		tag.put("spawnerSettings", ntag);
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		settings.readFromNBT(tag.getCompound("spawnerSettings"));
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		settings.updateSpawnProperties();
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		settings.writeToNBT(tag);
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		settings.readFromNBT(tag);

		BlockTools.notifyBlockUpdate(this);
	}

	public SpawnerSettings getSettings() {
		return settings;
	}

	public void setSettings(SpawnerSettings settings) {
		this.settings = settings;
		this.settings.setWorld(level, worldPosition);
		this.settings.updateSpawnProperties();

		BlockTools.notifyBlockUpdate(this);
	}

	public float getBlockHardness() {
		return settings.blockHardness;
	}

	@Override
	public void onBlockBroken(BlockState state) {
		if (level.isClientSide) {
			return;
		}
		int xp = settings.getXpToDrop();
		while (xp > 0) {
			int j = net.minecraft.world.entity.ExperienceOrb.getExperienceValue(xp);
			xp -= j;
			this.level.addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(this.level, this.worldPosition.getX() + 0.5d, this.worldPosition.getY(), this.worldPosition.getZ() + 0.5d, j));
		}
		//InventoryTools.dropItemsInWorld(level, settings.getInventory(), worldPosition);
	}
}
