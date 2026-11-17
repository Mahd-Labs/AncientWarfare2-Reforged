package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.shadowmage.ancientwarfare.structure.init.AWStructureBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.google.common.collect.ImmutableSet;
import net.shadowmage.ancientwarfare.core.block.BlockRotationHandler;
import net.shadowmage.ancientwarfare.core.render.property.CoreProperties;
import net.shadowmage.ancientwarfare.structure.util.BlockStateProperties;

import java.util.Set;

public class TileChair extends TileMulti implements BlockRotationHandler.IRotatableTile {
	private Direction facing = Direction.NORTH;

	public TileChair(BlockPos pos, BlockState state) {
		this(AWStructureBlocks.CHAIR_TILE.get(), pos, state);
	}

	protected TileChair(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected void readNBT(CompoundTag compound) {
		super.readNBT(compound);
		facing = Direction.byName(compound.getString("facing"));
	}

	@Override
	protected void writeNBT(CompoundTag compound) {
		super.writeNBT(compound);
		compound.putString("facing", facing.getName());
	}

	@Override
	public Set<BlockPos> getAdditionalPositions(BlockState state) {
		return ImmutableSet.of(worldPosition.above());
	}

	@Override
	public Direction getPrimaryFacing() {
		return facing;
	}

	@Override
	public void setPrimaryFacing(Direction face) {
		facing = face;
	}

	public boolean shouldRefresh(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
		return oldState.getBlock() != newState.getBlock()
				|| oldState.getValue(BlockStateProperties.VARIANT) != newState.getValue(BlockStateProperties.VARIANT)
				|| !oldState.getValue(CoreProperties.VISIBLE).equals(newState.getValue(CoreProperties.VISIBLE));
	}
}
