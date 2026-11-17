package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler;
import net.shadowmage.ancientwarfare.core.tile.IBlockBreakHandler;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.EntityTools;
import net.shadowmage.ancientwarfare.structure.util.LootHelper;

public class TileGravestone extends TileUpdatable implements ISpecialLootContainer, IBlockBreakHandler, BlockRotationHandler.IRotatableTile {
	private Direction facing = Direction.NORTH;
	private int variant = 1;
	private LootSettings lootSettings = new LootSettings();

	public TileGravestone(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.GRAVESTONE_TILE.get(), pos, state);
	}

	protected TileGravestone(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		//Port notes: the variant is a real block state property now; a block placed without tile data takes it from the state
		if (state.hasProperty(net.shadowmage.ancientwarfare.structure.block.BlockGravestone.VARIANT)) {
			variant = state.getValue(net.shadowmage.ancientwarfare.structure.block.BlockGravestone.VARIANT);
		}
	}

	@Override
	public Direction getPrimaryFacing() {
		return facing;
	}

	@Override
	public void setPrimaryFacing(Direction face) {
		facing = face;
	}

	public void setVariant(int variant) {
		this.variant = variant;
		net.shadowmage.ancientwarfare.structure.block.BlockBaseStructure.refresh(this);
	}

	public int getVariant() {
		return variant;
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
	public void readFromNBT(CompoundTag compound) {
		super.readFromNBT(compound);
		readNBT(compound);
		setChanged();
	}

	private void readNBT(CompoundTag compound) {
		if (compound.contains("variant")) {
			variant = Math.max(1, Math.min(8, compound.getInt("variant")));
		}
		lootSettings = LootSettings.deserializeNBT(compound.getCompound("lootSettings"));
	}

	@Override
	protected void writeUpdateNBT(CompoundTag tag) {
		super.writeUpdateNBT(tag);
		writeNBT(tag);
	}

	@Override
	public void writeToNBT(CompoundTag compound) {
		super.writeToNBT(compound);
		writeNBT(compound);
	}

	private CompoundTag writeNBT(CompoundTag compound) {
		compound.put("lootSettings", lootSettings.serializeNBT());
		compound.putInt("variant", variant);
		return compound;
	}

	@Override
	protected void handleUpdateNBT(CompoundTag tag) {
		readNBT(tag);
		BlockTools.notifyBlockUpdate(this);
	}

	public void activate(Player player) {
		if (6 <= getVariant() && getVariant() <= 8) { // only for runestones: variant 6,7,8
			dropLoot(player);
		}
	}

	private void dropLoot(Player player) {
		if (!level.isClientSide) {
			LootHelper.dropLoot(this, player);
		}
	}
}
