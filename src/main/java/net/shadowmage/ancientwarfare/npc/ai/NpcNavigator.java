package net.shadowmage.ancientwarfare.npc.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;

import javax.annotation.Nullable;
import java.util.Set;

/*
 * Port notes: 1.12 PathNavigateGround subclass -> GroundPathNavigation.
 * - the NpcWalkNodeProcessor is installed through createPathFinder
 * - paths are searched for the mount when the npc rides a Mob (1.12 pathToXYZ / pathToEntity with mountOrEntity()),
 *   by overriding the one createPath method every 1.21 path request ends in; the search region offsets (0 / 8 blocks
 *   for positions, 1 / 16 for entities) are the vanilla ones and match the 1.12 values
 * - setPath / clearPath / onUpdateNavigation -> moveTo(Path, speed) / stop() / tick(), forwarding to the mount
 * - setCanSwim / setEnterDoors -> setCanFloat / setCanPassDoors act on the node evaluator directly in 1.21
 * - 1.12 isSafeToStandAt (direct-line shortcut check) is not ported: 1.21 GroundPathNavigation.canMoveDirectly uses
 *   a different algorithm with no such hook
 * - onWorldChange: a 1.21 navigation's level is final (entities are re-created when changing dimension): no-op
 */
public class NpcNavigator extends GroundPathNavigation {
	private final Mob entity;

	public NpcNavigator(Mob living) {
		super(living, living.level());
		entity = living;
	}

	@Override
	protected PathFinder createPathFinder(int maxVisitedNodes) {
		this.nodeEvaluator = new NpcWalkNodeProcessor();
		mountPathFinder = new PathFinder(this.nodeEvaluator, maxVisitedNodes);
		return mountPathFinder;
	}

	//the PathFinder of PathNavigation is private, this is the same instance
	private PathFinder mountPathFinder;

	public void onWorldChange() {
		//noop, see port notes
	}

	@Nullable
	@Override
	protected Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange) {
		pathsCalculated++;
		if (!hasMount()) {
			return super.createPath(targets, regionOffset, offsetUpward, accuracy, followRange);
		}
		if (targets.isEmpty() || mob.getY() < (double) level.getMinBuildHeight() || !canUpdatePath()) {
			return null;
		}
		level.getProfiler().push("pathfind");
		BlockPos blockpos = offsetUpward ? mob.blockPosition().above() : mob.blockPosition();
		int i = (int) (followRange + (float) regionOffset);
		PathNavigationRegion region = new PathNavigationRegion(level, blockpos.offset(-i, -i, -i), blockpos.offset(i, i, i));
		Path path = mountPathFinder.findPath(region, mountOrEntity(), targets, followRange, accuracy, 1.0F);
		level.getProfiler().pop();
		return path;
	}

	/*
	 * Improvement over 1.12 (config improvements.reuse_paths, see NpcAI.setPath): bookkeeping for keeping a path.
	 * reusePaths: null = follow the config (tests set it per npc); pathsCalculated counts path searches.
	 */
	public Boolean reusePaths = null;
	public int pathsCalculated = 0;
	private int lastPathTick = Integer.MIN_VALUE / 2;

	public boolean canKeepPathTo(BlockPos target) {
		if (!(reusePaths != null ? reusePaths : net.shadowmage.ancientwarfare.npc.config.AWNPCStatics.improvedPathReuse)) {
			return false;
		}
		Path current = getPath();
		return current != null && !current.isDone() && !isStuck() && entity.tickCount - lastPathTick <= 40 && current.getTarget().distManhattan(target) <= 1;
	}

	@Override
	public boolean moveTo(@Nullable Path path, double speed) {
		if (path != null) {
			lastPathTick = entity.tickCount;
		}
		if (hasMount()) {
			//noinspection ConstantConditions
			((Mob) entity.getVehicle()).getNavigation().moveTo(path, speed);
		}
		return super.moveTo(path, speed);
	}

	@Override
	public void stop() {
		if (hasMount()) {
			//noinspection ConstantConditions
			((Mob) entity.getVehicle()).getNavigation().stop();
		}
		super.stop();
	}

	@Override
	public void tick() {
		super.tick();
		if (!isDone() && hasMount()) {
			//noinspection ConstantConditions
			((Mob) entity.getVehicle()).getNavigation().tick();
		}
	}

	private boolean hasMount() {
		return entity != null && entity.getVehicle() instanceof Mob;
	}

	private Mob mountOrEntity() {
		//noinspection ConstantConditions
		return hasMount() ? (Mob) entity.getVehicle() : entity;
	}

	/*
	 * Whether pathing can be done
	 */
	@Override
	protected boolean canUpdatePath() {
		return super.canUpdatePath() || hasMount();
	}

	@Override
	public String toString() {
		String result;
		if (isDone()) {
			result = "No Path " + (getPath() != null ? getPath().getNodeCount() : "");
		} else {
			result = "Path to " + getPath().getNode(getPath().getNextNodeIndex()).toString();
		}
		if (hasMount() && !((Mob) entity.getVehicle()).getNavigation().isDone()) {
			Path path = ((Mob) entity.getVehicle()).getNavigation().getPath();
			result += "AND Mount path to " + path.getNode(path.getNextNodeIndex()).toString();
		}
		return result;
	}
}
