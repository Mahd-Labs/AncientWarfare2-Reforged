package net.shadowmage.ancientwarfare.structure.block;

import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import static net.shadowmage.ancientwarfare.core.render.property.CoreProperties.FACING;

public class BlockGoldenIdol extends BlockBaseStructure {
	private static final BooleanProperty BOTTOM_PART = BooleanProperty.create("bottom_part");

	public BlockGoldenIdol() {
		super(Material.IRON.props().noOcclusion(), "golden_idol");
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, BOTTOM_PART);
	}

	@Override
	public BlockState getStateForPlacement(Level worldIn, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, LivingEntity placer) {
		return defaultBlockState().setValue(FACING, placer.getDirection()).setValue(BOTTOM_PART, true);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return state.getValue(BOTTOM_PART) ? RenderShape.MODEL : RenderShape.INVISIBLE;
	}

	@Override
	public boolean canPlaceBlockAt(Level world, BlockPos pos) {
		return super.canPlaceBlockAt(world, pos) && super.canPlaceBlockAt(world, pos.above());
	}

	@Override
	public void onBlockPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		worldIn.setBlockAndUpdate(pos.above(), state.setValue(BOTTOM_PART, false));
	}

	@Override
	protected void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean movedByPiston) {
		Boolean bottomPart = state.getValue(BOTTOM_PART);
		BlockPos otherPos = bottomPart ? pos.above() : pos.below();
		if (worldIn.getBlockState(otherPos).getBlock() != this) {
			worldIn.removeBlock(pos, false);
			if (bottomPart) {
				dropBlockAsItem(worldIn, pos, state, 0);
			}
		}
	}

}
