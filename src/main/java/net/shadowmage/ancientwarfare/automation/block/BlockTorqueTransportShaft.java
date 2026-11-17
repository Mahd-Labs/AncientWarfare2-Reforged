package net.shadowmage.ancientwarfare.automation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueShaftHeavy;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueShaftLight;
import net.shadowmage.ancientwarfare.automation.tile.torque.TileTorqueShaftMedium;

/*
 * Port note: the bounding box follows the FACING block state (1.12: the tile's primary facing).
 */
public class BlockTorqueTransportShaft extends BlockTorqueTransport {

	public BlockTorqueTransportShaft(String regName) {
		super(regName);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		switch (state.getValue(AutomationProperties.TIER)) {
			case LIGHT:
				return new TileTorqueShaftLight(pos, state);
			case MEDIUM:
				return new TileTorqueShaftMedium(pos, state);
			case HEAVY:
				return new TileTorqueShaftHeavy(pos, state);
		}
		return new TileTorqueShaftLight(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		//TODO static AABBs that are used to put together the total
		float min = 0.1875f;
		float max = 0.8125f;
		float x1 = min;
		float y1 = min;
		float z1 = min;
		float x2 = max;
		float y2 = max;
		float z2 = max;
		switch (state.getValue(FACING).getAxis()) {
			case X:
				x1 = 0;
				x2 = 1;
				break;
			case Y:
				y1 = 0;
				y2 = 1;
				break;
			case Z:
				z1 = 0;
				z2 = 1;
				break;
		}
		return Shapes.box(x1, y1, z1, x2, y2, z2);
	}
}
