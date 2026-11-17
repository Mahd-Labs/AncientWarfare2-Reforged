package net.shadowmage.ancientwarfare.npc.entity.vehicle;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.lang.ref.WeakReference;

public class EntityTarget implements ITarget {
	private WeakReference<LivingEntity> entity;

	public EntityTarget(LivingEntity entity) {
		this.entity = new WeakReference<>(entity);
	}

	@Override
	public double getX() {
		LivingEntity e = entity.get();
		return e == null ? 0D : e.getX();
	}

	@Override
	public double getY() {
		LivingEntity e = entity.get();
		return e == null ? 0D : e.getY() + e.getEyeHeight();
	}

	@Override
	public double getZ() {
		LivingEntity e = entity.get();
		return e == null ? 0D : e.getZ();
	}

	@Override
	public AABB getBoundigBox() {
		LivingEntity e = entity.get();
		return e == null ? new AABB(0, 0, 0, 0, 0, 0) : e.getBoundingBox();
	}

	@Override
	public boolean exists(Level world) {
		LivingEntity e = entity.get();
		return e != null && !e.isRemoved();
	}

	//measured from positions (a mob's motion vector does not show its walking speed reliably), averaged over 5 ticks
	private net.minecraft.world.phys.Vec3 samplePos = null;
	private long sampleTime = 0;
	private net.minecraft.world.phys.Vec3 velocity = net.minecraft.world.phys.Vec3.ZERO;

	@Override
	public net.minecraft.world.phys.Vec3 getVelocity(long gameTime) {
		LivingEntity e = entity.get();
		if (e == null) {
			return net.minecraft.world.phys.Vec3.ZERO;
		}
		if (samplePos == null || gameTime - sampleTime > 40) {
			samplePos = e.position();
			sampleTime = gameTime;
			velocity = net.minecraft.world.phys.Vec3.ZERO;
		} else if (gameTime - sampleTime >= 5) {
			velocity = e.position().subtract(samplePos).scale(1.0D / (gameTime - sampleTime));
			samplePos = e.position();
			sampleTime = gameTime;
		}
		return velocity;
	}

	public boolean isFor(LivingEntity other) {
		return entity.get() == other;
	}
}
