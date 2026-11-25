package net.shadowmage.ancientwarfare.automation.tile.worksite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.AncientWarfare;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/*
 * GitHub issue 4 on 1.0.0: a farm worksite calls into other mods' block code when it scans, fertilizes,
 * harvests, picks and plants (isValidBonemealTarget, performBonemeal, getDrops, placement, ...). An exception thrown by
 * that code used to escape the block entity tick and take the server down. Every such call goes through run(): an
 * exception is caught, that block position is skipped for SKIP_TICKS, and the problem is logged once per block type
 * (with the stack trace) for the whole game session, so a farm full of a broken crop does not flood the log.
 * Only RuntimeExceptions are caught; Errors (out of memory, stack overflow) still end the tick as before.
 * Not saved: after a reload the block is tried again (and skipped again if it still throws).
 */
public final class ForeignBlockGuard {
	public static final int SKIP_TICKS = 20 * 60 * 5;
	private static final Set<String> LOGGED_BLOCKS = ConcurrentHashMap.newKeySet();

	private final TileWorksiteBase worksite;
	private final Map<BlockPos, Long> skipUntil = new HashMap<>();

	public ForeignBlockGuard(TileWorksiteBase worksite) {
		this.worksite = worksite;
	}

	public boolean isSkipped(Level level, BlockPos pos) {
		Long until = skipUntil.get(pos);
		if (until == null) {
			return false;
		}
		if (level.getGameTime() >= until) {
			skipUntil.remove(pos);
			return false;
		}
		return true;
	}

	/* runs a call into block code for pos; false when the position is skipped or the call threw */
	public boolean run(Level level, BlockPos pos, String doing, BooleanSupplier call) {
		if (isSkipped(level, pos)) {
			return false;
		}
		try {
			return call.getAsBoolean();
		}
		catch (RuntimeException e) {
			failed(level, pos, doing, e);
			return false;
		}
	}

	/*
	 * runs a call into an entity's code (shearing, breeding, culling a possibly modded animal); an exception is caught
	 * and logged once per entity type; the entity is not remembered (the worksite's next scan finds it again, and
	 * the work lists already dropped it)
	 */
	public boolean runEntity(net.minecraft.world.entity.Entity entity, String doing, BooleanSupplier call) {
		try {
			return call.getAsBoolean();
		}
		catch (RuntimeException e) {
			String typeId;
			try {
				typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
			}
			catch (RuntimeException ignored) {
				typeId = "unknown entity";
			}
			if (LOGGED_BLOCKS.add("entity " + typeId)) {
				AncientWarfare.LOG.warn("Worksite {} at {}: the entity {} threw an exception while {}; it was skipped. "
								+ "This is a problem in the entity's own mod, reported here once per entity type.",
						worksite.getClass().getSimpleName(), worksite.getBlockPos().toShortString(), typeId, doing, e);
			}
			return false;
		}
	}

	/* runs a call into loot or other shared code that has no block or entity of its own (fishing loot); logged once per kind */
	public boolean runOther(String kind, String doing, BooleanSupplier call) {
		try {
			return call.getAsBoolean();
		}
		catch (RuntimeException e) {
			if (LOGGED_BLOCKS.add("other " + kind)) {
				AncientWarfare.LOG.warn("Worksite {} at {}: {} threw an exception while {}; that attempt was skipped. "
								+ "Most likely a problem in another mod's data or code, reported here once.",
						worksite.getClass().getSimpleName(), worksite.getBlockPos().toShortString(), kind, doing, e);
			}
			return false;
		}
	}

	/* TileWorksiteBase.update's catch-all: logged once per worksite type */
	static void worksiteFailed(TileWorksiteBase worksite, RuntimeException e) {
		if (LOGGED_BLOCKS.add("worksite " + worksite.getClass().getName())) {
			AncientWarfare.LOG.error("Worksite {} at {} threw an exception in its work tick; it rests {} ticks and tries again. "
							+ "Reported once per worksite type; please report this with the lines below.",
					worksite.getClass().getSimpleName(), worksite.getBlockPos().toShortString(), SKIP_TICKS, e);
		}
	}

	public int skippedCount() {
		return skipUntil.size();
	}

	private void failed(Level level, BlockPos pos, String doing, RuntimeException e) {
		BlockPos key = pos.immutable();
		skipUntil.put(key, level.getGameTime() + SKIP_TICKS);
		String blockId;
		try {
			blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(key).getBlock()).toString();
		}
		catch (RuntimeException ignored) {
			blockId = "unknown block";
		}
		if (LOGGED_BLOCKS.add(blockId)) {
			AncientWarfare.LOG.warn("Worksite {} at {}: the block {} at {} threw an exception while {}; that block is skipped for {} ticks. "
							+ "This is a problem in the block's own mod, reported here once per block type.",
					worksite.getClass().getSimpleName(), worksite.getBlockPos().toShortString(), blockId, key.toShortString(), doing, SKIP_TICKS, e);
		}
	}
}
