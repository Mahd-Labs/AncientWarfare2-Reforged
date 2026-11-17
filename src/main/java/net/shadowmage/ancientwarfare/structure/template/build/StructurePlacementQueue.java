package net.shadowmage.ancientwarfare.structure.template.build;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/*
 * Improvement (not baseline, budgeted_placement). Two parts:
 * - the time budget of a server tick for budgeted placement (placement_ms_per_tick), shared by this queue and the
 *   structure queue of WorldGenTickHandler (world generation and towns);
 * - the queue of the structure builder item and the awstructure build command, worked off first in first out on
 *   the server tick.
 * With the switch off, build() runs before, instantConstruction, after at once (the 1.12 path). Server stop:
 * AncientWarfareStructure.serverStop calls finishAll before the levels are saved, which completes every queued
 * build in that call; nothing is persisted.
 */
public final class StructurePlacementQueue {
	public static final StructurePlacementQueue INSTANCE = new StructurePlacementQueue();
	private static long remainingNanos;
	//the builds whose area is reserved (see isInBuildArea): all builds of this queue and the world generation build in progress
	private static final java.util.Set<BudgetedBuild> BUILD_AREAS = new java.util.LinkedHashSet<>();

	private final List<BudgetedBuild> builds = new ArrayList<>();

	private StructurePlacementQueue() {
	}

	public static boolean isBudgeted() {
		return AWStructureStatics.budgetedPlacement;
	}

	public static boolean hasBudget() {
		return remainingNanos > 0;
	}

	/* the deadline (System.nanoTime) for a consumer that starts at now */
	public static long deadline(long now) {
		return now + Math.max(0, remainingNanos);
	}

	public static void consume(long nanos) {
		remainingNanos -= nanos;
	}

	static void reserveArea(BudgetedBuild build) {
		BUILD_AREAS.add(build);
	}

	static void releaseArea(BudgetedBuild build) {
		BUILD_AREAS.remove(build);
	}

	/*
	 * true while a build that is queued or in progress covers this chunk (bounding box + border). The world generator
	 * leaves such chunks alone until the build is complete: with the instant path a chunk created by a build only got
	 * its structure / town attempts after the whole build was in place, and they must not see half of it (or the empty
	 * terrain under it).
	 */
	public static boolean isInBuildArea(net.minecraft.world.level.Level level, int chunkX, int chunkZ) {
		for (BudgetedBuild build : BUILD_AREAS) {
			if (build.coversChunk(level, chunkX, chunkZ)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * builds the structure: queued with the switch on, at once (the original path) with it off
	 */
	public void build(StructureBuilder builder, @Nullable Runnable before, @Nullable Runnable after) {
		if (!isBudgeted() || !(builder.getWorld() instanceof ServerLevel)) {
			if (before != null) {
				before.run();
			}
			builder.instantConstruction();
			if (after != null) {
				after.run();
			}
			return;
		}
		BudgetedBuild build = new BudgetedBuild(builder, before, after, true);
		reserveArea(build);
		builds.add(build);
	}

	public boolean isIdle() {
		return builds.isEmpty();
	}

	public int size() {
		return builds.size();
	}

	@SubscribeEvent
	public void serverTickPre(ServerTickEvent.Pre evt) {
		remainingNanos = AWStructureStatics.placementMsPerTick * 1000000L;
	}

	@SubscribeEvent
	public void serverTickPost(ServerTickEvent.Post evt) {
		if (builds.isEmpty()) {
			return;
		}
		if (!isBudgeted()) {
			//switched off while builds were queued: complete them as the original path would have
			finishAll();
			return;
		}
		if (!hasBudget()) {
			return;
		}
		long start = System.nanoTime();
		long deadline = deadline(start);
		while (!builds.isEmpty()) {
			BudgetedBuild build = builds.get(0);
			if (!build.work(deadline, false)) {
				break;
			}
			builds.remove(0);
			if (System.nanoTime() - deadline >= 0) {
				break;
			}
		}
		consume(System.nanoTime() - start);
	}

	/*
	 * completes every queued build now (server stopping)
	 */
	public void finishAll() {
		while (!builds.isEmpty()) {
			builds.remove(0).work(0, true);
		}
	}

	/*
	 * server stopped: the builds hold levels of that server (finishAll has completed them already)
	 */
	public void clear() {
		builds.forEach(BudgetedBuild::abandon);
		builds.clear();
		BUILD_AREAS.clear();
	}
}
