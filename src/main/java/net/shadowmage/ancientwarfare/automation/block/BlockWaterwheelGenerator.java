package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileWaterwheelGenerator;

/*
 * Port note: VALID_SETUP was a listed property that 1.12 never stored in meta (the renderer filled it from the tile);
 * it is kept as a real property, default false. The tile updates it when the validity of the setup changes.
 */
public class BlockWaterwheelGenerator extends BlockTorqueGenerator {
	public static final BooleanProperty VALID_SETUP = BooleanProperty.create("valid_setup");

	public BlockWaterwheelGenerator(String regName) {
		super(regName);
	}

	@Override
	protected void addProperties(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(VALID_SETUP);
	}

	@Override
	protected BlockState makeDefaultState(BlockState state) {
		return state.setValue(VALID_SETUP, false);
	}

	@Override
	public boolean invertFacing() {
		return true;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileWaterwheelGenerator(pos, state);
	}
}
