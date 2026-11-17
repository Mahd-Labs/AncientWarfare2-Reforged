package net.shadowmage.ancientwarfare.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/*
 * Port notes: RayTraceResult -> HitResult (EntityHitResult / BlockHitResult). Methods still return null when nothing
 * is hit (vanilla returns a MISS result now).
 */
public class RayTraceUtils {
	@Nullable
	public static HitResult getPlayerTarget(Player player, float range, float border) {
		HashSet<Entity> excluded = new HashSet<>();
		excluded.add(player);
		if (player.getVehicle() != null) {
			excluded.add(player.getVehicle());
		}
		float yOffset = player.getEyeHeight();
		Vec3 look = player.getLookAngle();
		look = look.scale(range);
		look = look.add(player.getX(), player.getY() + yOffset, player.getZ());
		return tracePath(player.level(), player.getX(), player.getY() + yOffset, player.getZ(), look.x, look.y, look.z, border, excluded);
	}

	@Nullable
	public static HitResult tracePathWithYawPitch(Level world, float x, float y, float z, float yaw, float pitch, float range, float borderSize, HashSet<Entity> excluded) {
		float tx = x + (Trig.sinDegrees(yaw + 180) * range * Trig.cosDegrees(pitch));
		float ty = (-Trig.sinDegrees(pitch) * range) + y;
		float tz = z + (Trig.cosDegrees(yaw) * range * Trig.cosDegrees(pitch));
		return tracePath(world, x, y, z, tx, ty, tz, borderSize, excluded);
	}

	@Nullable
	public static HitResult tracePath(Level world, double x, double y, double z, double tx, double ty, double tz, float borderSize, HashSet<Entity> excluded) {
		double minX = x < tx ? x : tx;
		double minY = y < ty ? y : ty;
		double minZ = z < tz ? z : tz;
		double maxX = x > tx ? x : tx;
		double maxY = y > ty ? y : ty;
		double maxZ = z > tz ? z : tz;
		AABB bb = new AABB(minX, minY, minZ, maxX, maxY, maxZ).expandTowards(borderSize, borderSize, borderSize);
		List<Entity> allEntities = world.getEntities((Entity) null, bb);
		Entity closestHitEntity = null;
		float closestHit = Float.POSITIVE_INFINITY;
		float currentHit;
		Optional<Vec3> intercept;
		Vec3 startVec = new Vec3(x, y, z);
		Vec3 endVec = new Vec3(tx, ty, tz);
		for (Entity ent : allEntities) {
			if (ent.isPickable() && !excluded.contains(ent)) {
				AABB entityBb = ent.getBoundingBox();
				float entBorder = ent.getPickRadius();
				intercept = entityBb.expandTowards(entBorder, entBorder, entBorder).clip(startVec, endVec);
				if (intercept.isPresent()) {
					currentHit = (float) intercept.get().distanceTo(startVec);
					if (currentHit < closestHit || currentHit == 0) {
						closestHit = currentHit;
						closestHitEntity = ent;
					}
				}
			}
		}
		if (closestHitEntity != null) {
			return new EntityHitResult(closestHitEntity);
		}
		startVec = new Vec3(x, y, z);
		endVec = new Vec3(tx, ty, tz);
		BlockHitResult blockHit = world.clip(new ClipContext(startVec, endVec, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty()));
		return blockHit.getType() == HitResult.Type.MISS ? null : blockHit;
	}

	@Nullable
	public static BlockHitResult raytraceMultiAABB(List<AABB> aabbs, BlockPos pos, Vec3 start, Vec3 end) {
		List<BlockHitResult> list = new ArrayList<>();

		for (AABB axisalignedbb : aabbs) {
			list.add(rayTraceAABBIntercept(pos, start, end, axisalignedbb));
		}

		BlockHitResult ret = null;
		double closestHit = Integer.MAX_VALUE;

		for (BlockHitResult raytraceresult : list) {
			if (raytraceresult != null) {
				double distance = raytraceresult.getLocation().distanceToSqr(start);

				if (distance < closestHit) {
					ret = raytraceresult;
					closestHit = distance;
				}
			}
		}

		return ret;
	}

	@Nullable
	public static <T> T raytraceMultiAABB(List<AABB> aabbs, BlockPos pos, Vec3 start, Vec3 end, Function2<BlockHitResult, AABB, T> getValue) {
		List<BlockHitResult> list = new ArrayList<>();

		for (AABB axisalignedbb : aabbs) {
			list.add(rayTraceAABBIntercept(pos, start, end, axisalignedbb));
		}

		T ret = null;
		double closestHit = Integer.MAX_VALUE;

		for (int i = 0; i < list.size(); i++) {
			BlockHitResult raytraceresult = list.get(i);
			if (raytraceresult != null) {
				double distance = raytraceresult.getLocation().distanceToSqr(start);

				if (distance < closestHit) {
					ret = getValue.apply(raytraceresult, aabbs.get(i));
					closestHit = distance;
				}
			}
		}

		return ret;
	}

	@Nullable
	private static BlockHitResult rayTraceAABBIntercept(BlockPos pos, Vec3 start, Vec3 end, AABB boundingBox) {
		return AABB.clip(Collections.singletonList(boundingBox), start, end, pos);
	}

	/*
	 * Port note: 1.12 used CodeChickenLib RayTracer.getStartVec / getEndVec (eye position and eye position + look *
	 * block reach).
	 */
	public static AABB getSelectedBoundingBox(List<AABB> aabbs, BlockPos pos, Player player) {
		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(player.getLookAngle().scale(player.blockInteractionRange()));
		AABB axisAlignedBB = raytraceMultiAABB(aabbs, pos, start, end, (rtr, aabb) -> aabb);
		if (axisAlignedBB == null) {
			axisAlignedBB = aabbs.get(0);
		}
		return axisAlignedBB.move(pos);
	}
}
