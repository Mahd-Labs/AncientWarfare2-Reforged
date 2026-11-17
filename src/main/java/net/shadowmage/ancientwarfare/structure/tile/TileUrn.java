package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.tile.IBlockBreakHandler;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.util.EntityTools;
import net.shadowmage.ancientwarfare.structure.util.LootHelper;

public class TileUrn extends TileUpdatable implements ISpecialLootContainer, IBlockBreakHandler {
	private LootSettings lootSettings = new LootSettings();

	public TileUrn(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.URN_TILE.get(), pos, state);
	}

	protected TileUrn(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void setLootSettings(LootSettings settings) {
		lootSettings = settings;
	}

	@Override
	public LootSettings getLootSettings() {
		return lootSettings;
	}

	@Override
	public void onBlockBroken(BlockState state) {
		LootHelper.dropLoot(this, EntityTools.findClosestPlayer(level, worldPosition, 100));
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		writeNBT(tag);
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		super.handleUpdateNBT(tag);
		readNBT(tag);
	}

	private void readNBT(CompoundTag tag) {
		lootSettings = LootSettings.deserializeNBT(tag.getCompound("lootSettings"));
	}

	@Override
	public void readFromNBT(CompoundTag compound) {
		super.readFromNBT(compound);
		readNBT(compound);
	}

	@Override
	public void writeToNBT(CompoundTag compound) {
		super.writeToNBT(compound);
		writeNBT(compound);
	}

	private CompoundTag writeNBT(CompoundTag tagCompound) {
		tagCompound.put("lootSettings", lootSettings.serializeNBT());
		return tagCompound;
	}
}
