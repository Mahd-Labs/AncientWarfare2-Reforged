package net.shadowmage.ancientwarfare.npc.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/*
 * Port notes: the 1.12 class was a copy of vanilla WalkNodeProcessor.findPathOptions/getSafePoint (needed only to
 * reach the private methods) with one real change: fence gates are treated like wooden doors (open -> DOOR_OPEN,
 * closed -> DOOR_WOOD_CLOSED) and the processor can always open and enter doors. In 1.21.1 that is the single
 * override of getPathType; the copied neighbour search is the vanilla one again.
 */
public class NpcWalkNodeProcessor extends WalkNodeEvaluator {

	public NpcWalkNodeProcessor() {
		setCanOpenDoors(true);
		setCanPassDoors(true);
	}

	@Override
	public PathType getPathType(PathfindingContext context, int x, int y, int z) {
		BlockState iblockstate = context.getBlockState(new BlockPos(x, y, z));

		if (iblockstate.getBlock() instanceof FenceGateBlock) {
			return iblockstate.getValue(FenceGateBlock.OPEN) ? PathType.DOOR_OPEN : PathType.DOOR_WOOD_CLOSED;
		}

		return super.getPathType(context, x, y, z);
	}
}
