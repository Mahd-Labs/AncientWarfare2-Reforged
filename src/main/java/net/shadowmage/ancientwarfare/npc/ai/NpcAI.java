package net.shadowmage.ancientwarfare.npc.ai;

import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;

import javax.annotation.Nullable;

/*
 * AI template class with utility methods and member access for a non-specific NPC type
 *
 * @author Shadowmage
 */
public abstract class NpcAI<T extends NpcBase> extends Goal {
	/*
	 * used during npc-ai task-rendering to determine how many bits to loop through
	 * of the task bitfield -- needs to be increased if more task types / bits are added
	 */
	public static final int NUMBER_OF_TASKS = 14;
	public static final int TASK_ATTACK = 1;
	public static final int TASK_UPKEEP = 2;
	public static final int TASK_IDLE_HUNGRY = 4;
	public static final int TASK_GO_HOME = 8;
	public static final int TASK_WORK = 16;
	public static final int TASK_PATROL = 32;
	public static final int TASK_GUARD = 64;
	public static final int TASK_FOLLOW = 128;
	public static final int TASK_WANDER = 256;
	public static final int TASK_MOVE = 512;
	public static final int TASK_ALARM = 1024;
	public static final int TASK_FLEE = 2048;
	public static final int TASK_SLEEP = 4096;
	public static final int TASK_RAIN = 8192;

	/*
	 * internal flag used to determine exclusion types
	 */
	public static final int MOVE = 1;
	public static final int ATTACK = 2;
	public static final int SWIM = 4;
	public static final int HUNGRY = 8;

	public static final int MIN_RANGE = 9;

	protected int moveRetryDelay;
	protected double moveSpeed = 1.d;
	private final double maxPFDist;
	private final double maxPFDistSq;

	protected T npc;

	/*
	 * Port notes: 1.12 EntityAIBase mutex bits. The int mask is kept (NpcBase.isAIFlagStopped uses it) and mapped on
	 * the 1.21 goal flags: MOVE(1) -> MOVE, ATTACK(2) -> LOOK, SWIM(4) -> JUMP, HUNGRY(8) -> TARGET, so tasks exclude
	 * each other exactly as in 1.12.
	 */
	private int mutexBits;

	public void setMutexBits(int mutexBits) {
		this.mutexBits = mutexBits;
		setFlags(flagsFromMutexBits(mutexBits));
	}

	public int getMutexBits() {
		return mutexBits;
	}

	public static java.util.EnumSet<Goal.Flag> flagsFromMutexBits(int mutexBits) {
		java.util.EnumSet<Goal.Flag> flags = java.util.EnumSet.noneOf(Goal.Flag.class);
		if ((mutexBits & MOVE) != 0) {
			flags.add(Goal.Flag.MOVE);
		}
		if ((mutexBits & ATTACK) != 0) {
			flags.add(Goal.Flag.LOOK);
		}
		if ((mutexBits & SWIM) != 0) {
			flags.add(Goal.Flag.JUMP);
		}
		if ((mutexBits & HUNGRY) != 0) {
			flags.add(Goal.Flag.TARGET);
		}
		return flags;
	}

	/*
	 * 1.12 ticked every running task every tick
	 */
	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	public NpcAI(T npc) {
		this.npc = npc;
		maxPFDist = npc.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue() * 0.90d;

		maxPFDistSq = maxPFDist * maxPFDist;
	}

	protected final void moveToEntity(Entity target, double sqDist) {
		moveToPosition(target.getX(), target.getBoundingBox().minY, target.getZ(), sqDist);
	}

	protected final void moveToPosition(int x, int y, int z, double sqDist) {
		moveToPosition(x + 0.5d, y, z + 0.5d, sqDist);
	}

	protected final void moveToPosition(BlockPos pos, double sqDist) {
		moveToPosition(pos.getX(), pos.getY(), pos.getZ(), sqDist);
	}

	protected final void forceMoveToPosition(BlockPos pos, double sqDist) {
		moveRetryDelay = 0;
		moveToPosition(pos, sqDist);
	}

	protected final void moveToPosition(double x, double y, double z, double sqDist) {
		moveRetryDelay -= 10;
		if (moveRetryDelay <= 0) {
			if (sqDist > maxPFDistSq) {
				moveLongDistance(x, y, z);
				moveRetryDelay = 30;
			} else {
				setPath(x, y, z);
				moveRetryDelay = 5;
				if (sqDist > 256) {
					moveRetryDelay += 5;
				}
				if (sqDist > 1024) {
					moveRetryDelay += 10;
				}
			}
		}
	}

	/*
	 * Port notes: 1.12 EntityAITasks evaluated shouldExecute() of the tasks that were not running every 3rd
	 * tick, 1.21.1 evaluates canUse() every 2nd tick. Many NPC tasks count or roll dice inside shouldExecute
	 * (recheck delays, "scan every 5 calls", nextInt(120) wander chance), which would run 1.5 times as fast. Start
	 * evaluations are therefore let through only once per 3 tick window of the npc's life time (2 of 3 vanilla
	 * evaluations), which restores the 1.12 rate. Evaluations of a running task (canContinueToUse) are not gated; see
	 * tick() for how they are brought back to once per tick.
	 */
	private boolean continuing = false;
	private int lastEvaluationWindow = -1;
	private int lastContinueCheckTick = -1;

	/*
	 * 1.12 EntityAIBase.updateTask(); subclasses override this instead of Goal.tick() (see tick() below)
	 */
	public void updateTask() {
	}

	/*
	 * Port notes: 1.12 EntityAITasks.onUpdateTasks checked shouldContinueExecuting() of every running task
	 * on every tick before calling updateTask(). 1.21.1 only does that in GoalSelector.tick(), which Mob.serverAiStep
	 * runs every second tick; on the other ticks tickRunningGoals() calls tick() with no check. The NPC tasks rely on
	 * the 1.12 order (targets, work sites and orders can become invalid between two ticks), so the check is repeated
	 * here whenever the selector has not already done it this tick, and a task that may not continue is stopped
	 * instead of being updated.
	 */
	@Override
	public final void tick() {
		if (lastContinueCheckTick != npc.tickCount && !canContinueToUse()) {
			stopRunning();
			return;
		}
		updateTask();
	}

	private void stopRunning() {
		boolean stopped = false;
		for (net.minecraft.world.entity.ai.goal.WrappedGoal wrapped : npc.goalSelector.getAvailableGoals()) {
			if (wrapped.getGoal() == this && wrapped.isRunning()) {
				wrapped.stop();
				stopped = true;
			}
		}
		for (net.minecraft.world.entity.ai.goal.WrappedGoal wrapped : npc.targetSelector.getAvailableGoals()) {
			if (wrapped.getGoal() == this && wrapped.isRunning()) {
				wrapped.stop();
				stopped = true;
			}
		}
		if (!stopped) {
			stop();
		}
	}

	@Override
	public boolean canUse() {
		if (!continuing) {
			int window = npc.tickCount / 3;
			if (window == lastEvaluationWindow) {
				return false;
			}
			lastEvaluationWindow = window;
		}
		return npc.getIsAIEnabled() && !npc.isAIFlagStopped(getMutexBits());
	}

	@Override
	public final boolean canContinueToUse() {
		lastContinueCheckTick = npc.tickCount;
		continuing = true;
		try {
			return super.canContinueToUse();
		}
		finally {
			continuing = false;
		}
	}

	protected final void moveLongDistance(double x, double y, double z) {
		Vec3 vec = new Vec3(x - npc.getX(), y - npc.getY(), z - npc.getZ());

		//normalize vector to a -1 <-> 1 value range
		double w = Math.sqrt(vec.x * vec.x + vec.y * vec.y + vec.z * vec.z);
		if (w != 0) {
			vec = vec.scale(1d / w);
		}

		//then mult by PF distance to find the proper vector for our PF length
		vec = vec.scale(maxPFDist);

		//finally re-offset by npc position to get an actual target position
		vec = vec.add(npc.getX(), npc.getY(), npc.getZ());

		//move npc towards the calculated partial target
		setPath(vec.x, vec.y, vec.z);
	}

	protected final void returnHome() {
		setPath(npc.getRestrictCenter());
	}

	/*
	 * Improvement over 1.12 (not baseline, config improvements.reuse_paths): the 1.12 retry delays make a walking npc
	 * search a new path on almost every tick. A path that still leads to the requested place is kept instead.
	 */
	private boolean keepsPathTo(BlockPos target) {
		if (npc.getNavigation() instanceof NpcNavigator && ((NpcNavigator) npc.getNavigation()).canKeepPathTo(target)) {
			npc.getNavigation().setSpeedModifier(moveSpeed);
			return true;
		}
		return false;
	}

	protected final void setPath(double x, double y, double z) {
		if (keepsPathTo(BlockPos.containing(x, y, z))) {
			return;
		}
		Path path = trimPath(npc.getNavigation().createPath(x, y, z, 0));
		npc.getNavigation().moveTo(path, moveSpeed);
	}

	protected final void setPath(BlockPos pos) {
		if (keepsPathTo(pos)) {
			return;
		}
		Path path = trimPath(npc.getNavigation().createPath(pos, 0));
		npc.getNavigation().moveTo(path, moveSpeed);
	}

	protected Path trimPath(Path path) {
		if (path != null) {
			int index = path.getNextNodeIndex();
			Node pathpoint = path.getNode(index);
			if (npc.getWalkTargetValue(new BlockPos(pathpoint.x, pathpoint.y, pathpoint.z)) >= 0) {

				for (int i = index + 1; i < path.getNodeCount(); i++) {
					pathpoint = path.getNode(i);
					if (npc.getWalkTargetValue(new BlockPos(pathpoint.x, pathpoint.y, pathpoint.z)) < 0) {
						path.truncateNodes(i - 1);
						break;
					}
				}
			} else {
				Vec3 vec = DefaultRandomPos.getPosAway(npc, MIN_RANGE, MIN_RANGE, new Vec3(npc.getX(), npc.getY(), npc.getZ()));
				if (vec != null) {
					return npc.getNavigation().createPath(vec.x, vec.y, vec.z, 0);
				}
			}
		}
		return path;
	}
}
