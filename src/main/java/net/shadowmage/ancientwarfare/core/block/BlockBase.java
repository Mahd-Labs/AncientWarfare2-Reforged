package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.tile.IBlockBreakHandler;
import net.shadowmage.ancientwarfare.core.util.WorldTools;

/*
 * Port notes:
 * - 1.12 (Material material, String modID, String regName) -> (BlockBehaviour.Properties properties, String modID,
 *   String regName). Registry names are given by the DeferredRegister, regName is kept for the translation key
 *   ("tile.<regName>.name", the key 1.12 setUnlocalizedName produced). Use baseProperties() to get the 1.12
 *   setHardness(2) default.
 * - breakBlock -> onRemove (only when the block itself changes, like 1.12).
 * - rotateBlock(world, pos, axis) was a Forge hook on Block; it lives here now (used by the hammer).
 */
public abstract class BlockBase extends Block {
	private final String regName;

	public BlockBase(BlockBehaviour.Properties properties, String modID, String regName) {
		super(properties);
		this.regName = regName;
	}

	/*
	 * 1.12 setHardness(2): hardness 2, explosion resistance 2
	 */
	public static BlockBehaviour.Properties baseProperties() {
		return BlockBehaviour.Properties.of().strength(2.0F);
	}

	public String getRegName() {
		return regName;
	}

	@Override
	public String getDescriptionId() {
		return "tile." + regName + ".name";
	}

	@Override
	protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock())) {
			breakBlock(world, pos, state);
		}
		super.onRemove(state, world, pos, newState, movedByPiston);
	}

	/*
	 * 1.12 Block.breakBlock: called before the tile entity is removed, when the block is replaced by another block
	 */
	public void breakBlock(Level world, BlockPos pos, BlockState state) {
		if (state.hasBlockEntity()) {
			WorldTools.getTile(world, pos, IBlockBreakHandler.class).ifPresent(handler -> handler.onBlockBroken(state));
		}
	}

	/*
	 * 1.12 Forge Block.rotateBlock. Default: rotate the block state by 90 degrees around the y axis.
	 */
	public boolean rotateBlock(Level world, BlockPos pos, Direction axis) {
		return rotateVanillaBlock(world, pos);
	}

	public static boolean rotateVanillaBlock(Level world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		BlockState rotated = state.rotate(world, pos, Rotation.CLOCKWISE_90);
		if (rotated != state) {
			world.setBlock(pos, rotated, 3);
			return true;
		}
		return false;
	}

	/*
	 * Port fix (user play test 2026-11-14, NPCs bunching at torque shafts): 1.12 path finding asked Block.isPassable,
	 * which is !material.blocksMovement(): every Ancient Warfare block (rock, wood, iron) was an obstacle whatever
	 * its shape. 1.21 treats any block whose collision shape is not a full cube as walkable space, so NPCs planned
	 * paths through shafts, generators, chairs and the like and then ran into them. Blocks that were passable in 1.12
	 * (flag, fire pit, open gate proxy) override this.
	 */
	@Override
	protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
		return type == net.minecraft.world.level.pathfinder.PathComputationType.WATER && state.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
	}

	/* the 1.21 default, for the blocks that were passable in 1.12 */
	protected static boolean pathfindableByShape(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
		if (type == net.minecraft.world.level.pathfinder.PathComputationType.WATER) {
			return state.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
		}
		return !state.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
	}
}
