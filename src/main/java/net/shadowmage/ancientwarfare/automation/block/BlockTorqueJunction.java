package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileConduitHeavy;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileConduitLight;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileConduitMedium;

public class BlockTorqueJunction extends BlockTorqueTransportSided {
	public BlockTorqueJunction(String regName) {
		super(regName);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		switch (state.getValue(AutomationProperties.TIER)) {
			case LIGHT:
				return new TileConduitLight(pos, state);
			case MEDIUM:
				return new TileConduitMedium(pos, state);
			case HEAVY:
				return new TileConduitHeavy(pos, state);
		}
		return new TileConduitLight(pos, state);
	}
}
