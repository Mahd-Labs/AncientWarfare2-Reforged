package net.shadowmage.ancientwarfare.structure.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.shadowmage.ancientwarfare.structure.block.BlockBaseStructure;

/*
 * Port addition: 1.12 computed a number of block state values from the neighbours every time the block was looked at
 * (Block.getActualState): fence, pane, iron bar and wall connections, stair corners, fence gates in walls, redstone
 * wire and tripwire connections, snowy grass, the distance of leaves to a log, the vine's top face, the legs / doubles of the Ancient Warfare
 * furniture, and two chests side by side forming a double chest. Those values are stored in the block state since
 * 1.13 and only change when a neighbour changes afterwards. A structure builder places block by block (all at once,
 * budgeted or one block per craftsman's work unit), so a block placed after its neighbours kept the value saved in
 * the 1.12 template (no connection, straight stairs, single chest) where 1.12 showed the connected shape.
 * After each placement the block gets the state its neighbours give it now; blocks placed later update it through
 * the normal neighbour shape updates, so the finished structure shows what 1.12 showed.
 */
public final class LegacyActualStates {
	private LegacyActualStates() {}

	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

	/*
	 * 1.12 kept a double plant's kind in the lower half's metadata; the upper half's metadata says sunflower, its look
	 * came from the lower half (getActualState). An upper half placed on the lower half of another double plant takes
	 * that plant (otherwise the two different plants remove each other at the next shape update).
	 */
	public static BlockState beforePlacement(Level world, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof DoublePlantBlock && state.hasProperty(DoublePlantBlock.HALF) && state.getValue(DoublePlantBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
			BlockState below = world.getBlockState(pos.below());
			if (below.getBlock() instanceof DoublePlantBlock && below.getBlock() != state.getBlock() && below.getValue(DoublePlantBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
				return below.getBlock().withPropertiesOf(state);
			}
		}
		return state;
	}

	public static void afterPlacement(Level world, BlockPos pos, BlockState placed) {
		Block block = placed.getBlock();
		if (block instanceof ChestBlock) {
			joinChest(world, pos, placed);
		} else if (block instanceof VineBlock) {
			//only the top face: VineBlock.updateShape would also drop side faces whose support is not built yet
			BlockPos above = pos.above();
			boolean up = VineBlock.isAcceptableNeighbour(world, above, Direction.DOWN);
			if (placed.getValue(VineBlock.UP) != up) {
				world.setBlock(pos, placed.setValue(VineBlock.UP, up), FLAGS);
			}
		} else if (block instanceof CrossCollisionBlock || block instanceof WallBlock || block instanceof StairBlock || block instanceof FenceGateBlock
				|| block instanceof RedStoneWireBlock || block instanceof TripWireBlock || block instanceof SnowyDirtBlock || block instanceof LeavesBlock || block instanceof BlockBaseStructure) {
			BlockState shaped = Block.updateFromNeighbourShapes(placed, world, pos);
			if (shaped != placed && shaped.getBlock() == block) {
				world.setBlock(pos, shaped, FLAGS);
			}
		}
	}

	/* ChestBlock.getStateForPlacement without a player: a single chest beside a single chest of the same kind and facing */
	private static void joinChest(Level world, BlockPos pos, BlockState placed) {
		if (placed.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			return;
		}
		Direction facing = placed.getValue(ChestBlock.FACING);
		if (!join(world, pos, placed, facing.getClockWise(), ChestType.LEFT)) {
			join(world, pos, placed, facing.getCounterClockWise(), ChestType.RIGHT);
		}
	}

	private static boolean join(Level world, BlockPos pos, BlockState placed, Direction side, ChestType type) {
		BlockPos otherPos = pos.relative(side);
		BlockState other = world.getBlockState(otherPos);
		if (other.getBlock() != placed.getBlock() || other.getValue(ChestBlock.TYPE) != ChestType.SINGLE || other.getValue(ChestBlock.FACING) != placed.getValue(ChestBlock.FACING)) {
			return false;
		}
		world.setBlock(pos, placed.setValue(ChestBlock.TYPE, type), FLAGS);
		world.setBlock(otherPos, other.setValue(ChestBlock.TYPE, type.getOpposite()), FLAGS);
		return true;
	}
}
