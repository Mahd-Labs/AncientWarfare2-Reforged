package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileDistributorHeavy;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileDistributorLight;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileDistributorMedium;

public class BlockTorqueDistributor extends BlockTorqueTransportSided {

	public BlockTorqueDistributor(String regName) {
		super(regName);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		switch (state.getValue(AutomationProperties.TIER)) {
			case LIGHT:
				return new TileDistributorLight(pos, state);
			case MEDIUM:
				return new TileDistributorMedium(pos, state);
			case HEAVY:
				return new TileDistributorHeavy(pos, state);
		}
		return new TileDistributorLight(pos, state);
	}
}
