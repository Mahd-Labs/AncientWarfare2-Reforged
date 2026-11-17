package net.shadowmage.ancientwarfare.npc.ai.owned;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.shadowmage.ancientwarfare.core.interfaces.IWorkSite;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.npc.ai.NpcAI;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.entity.NpcWorker;

import java.util.Optional;

public class NpcAIPlayerOwnedWorkRandom extends NpcAI<NpcWorker> {
	private int ticksAtSite = 0;
	//improvement over 1.12 (config improvements.skip_unreachable_worksites), see giveUpIfUnreachable
	private static final int NO_PROGRESS_TICKS = 200;
	private static final int SKIP_TICKS = 1200;
	private double closestDistSq = Double.MAX_VALUE;
	private int ticksWithoutProgress = 0;

	public NpcAIPlayerOwnedWorkRandom(NpcWorker npc) {
		super(npc);
		setMutexBits(ATTACK + MOVE);
	}

	@Override
	public boolean canUse() {
		if (!super.canUse()) {
			return false;
		}
		if (npc.getFoodRemaining() <= 0 || npc.shouldBeAtHome()) {
			return false;
		}
		return npc.ordersStack.isEmpty() && npc.autoWorkTarget != null;
	}

	@Override
	public void start() {
		npc.addAITask(TASK_WORK);
		ticksAtSite = 0;
		closestDistSq = Double.MAX_VALUE;
		ticksWithoutProgress = 0;
	}

	/*
	 * Improvement over 1.12 (not baseline): a worker that has not come at least one block closer to its automatically
	 * chosen worksite for 10 seconds gives it up for a minute, so that NpcAIPlayerOwnedFindWorksite picks another one.
	 * 1.12 keeps walking against the obstacle forever.
	 */
	private boolean giveUpIfUnreachable(BlockPos pos, double dist) {
		if (!AWNPCStatics.improvedSkipUnreachableWorksites) {
			return false;
		}
		if (Math.sqrt(dist) < Math.sqrt(closestDistSq) - 1) {
			closestDistSq = dist;
			ticksWithoutProgress = 0;
			return false;
		}
		if (++ticksWithoutProgress < NO_PROGRESS_TICKS) {
			return false;
		}
		npc.unreachableWorkTarget = pos;
		npc.unreachableWorkTargetUntil = npc.tickCount + SKIP_TICKS;
		npc.autoWorkTarget = null;
		npc.getNavigation().stop();
		return true;
	}

	@Override
	public void updateTask() {
		BlockPos pos = npc.autoWorkTarget;
		double dist = npc.distanceToSqr(pos);
		if (dist > npc.getWorkRangeSq()) {
			if (giveUpIfUnreachable(pos, dist)) {
				return;
			}
			npc.addAITask(TASK_MOVE);
			ticksAtSite = 0;
			moveToPosition(pos, dist);
		} else {
			npc.getNavigation().stop();
			npc.removeAITask(TASK_MOVE);
			workAtSite();
		}
	}

	@Override
	public void stop() {
		npc.removeAITask(TASK_WORK + TASK_MOVE);
	}

	private void workAtSite() {
		ticksAtSite++;
		if (npc.tickCount % 10 == 0) {
			npc.swing(InteractionHand.MAIN_HAND);
		}
		if (ticksAtSite >= AWNPCStatics.npcWorkTicks) {
			ticksAtSite = 0;
			Optional<IWorkSite> te = WorldTools.getTile(npc.level(), npc.autoWorkTarget, IWorkSite.class);
			if (te.isPresent()) {
				IWorkSite site = te.get();
				if (npc.canWorkAt(site.getWorkType()) && site.hasWork()) {
					npc.addExperience(AWNPCStatics.npcXpFromWork);
					site.addEnergyFromWorker(npc);
					return;
				}
			}
			npc.autoWorkTarget = null;
		}
	}

	public void readFromNBT(CompoundTag tag) {
		ticksAtSite = tag.getInt("ticksAtSite");
	}

	public CompoundTag writeToNBT(CompoundTag tag) {
		tag.putInt("ticksAtSite", ticksAtSite);
		return tag;
	}

}
