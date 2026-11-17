package net.shadowmage.ancientwarfare.vehicle.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.shadowmage.ancientwarfare.core.util.Trig;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;

/*
 * Client-only. Port notes: RenderWorldLastEvent -> RenderLevelStageEvent (AFTER_TRANSLUCENT_BLOCKS). 1.12 drew
 * immediate mode GL lines (width 3 for the white facing line, width 4 for the red trajectory) relative to the
 * player position; here the same segments go through the level's buffer source with the vanilla "lines" render
 * type in camera-relative coordinates, so the line width is the vanilla one. Colours and alpha as 1.12 (white
 * 0.6, red 1 / 0.4 / 0.4 at 0.4). The battering ram overlay and the air vehicle branch are not ported.
 */
public class RenderOverlayAdvanced {
	@SubscribeEvent
	public void renderLast(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player != null && AWVehicleStatics.clientSettings.renderAdvOverlay && player.getVehicle() instanceof VehicleBase && mc.screen == null) {
			float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
			Vec3 camera = event.getCamera().getPosition();
			PoseStack poseStack = event.getPoseStack();
			MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
			poseStack.pushPose();
			poseStack.translate(-camera.x, -camera.y, -camera.z);
			renderAdvancedVehicleOverlay((VehicleBase) player.getVehicle(), partialTick, poseStack, buffer.getBuffer(RenderType.lines()));
			poseStack.popPose();
			buffer.endBatch(RenderType.lines());
		}
	}

	private static void renderAdvancedVehicleOverlay(VehicleBase vehicle, float partialTick, PoseStack poseStack, VertexConsumer consumer) {
		Vec3 renderOffset = vehicle.getPosition(partialTick);
		drawStraightLine(vehicle, partialTick, renderOffset, poseStack, consumer);
		boolean rocket = vehicle.ammoHelper.getCurrentAmmoType() != null && vehicle.ammoHelper.getCurrentAmmoType().isRocket();
		drawDynamicPart(vehicle, partialTick, renderOffset, rocket, poseStack, consumer);
	}

	private static void line(PoseStack poseStack, VertexConsumer consumer, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
		PoseStack.Pose pose = poseStack.last();
		float nx = (float) (x2 - x1);
		float ny = (float) (y2 - y1);
		float nz = (float) (z2 - z1);
		float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (length > 0) {
			nx /= length;
			ny /= length;
			nz /= length;
		}
		consumer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
		consumer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
	}

	private static void drawStraightLine(VehicleBase vehicle, float partialTick, Vec3 renderOffset, PoseStack poseStack, VertexConsumer consumer) {
		double x2 = renderOffset.x - 20 * Trig.sinDegrees(vehicle.getYRot() + partialTick * vehicle.moveHelper.getRotationSpeed());
		double z2 = renderOffset.z - 20 * Trig.cosDegrees(vehicle.getYRot() + partialTick * vehicle.moveHelper.getRotationSpeed());
		line(poseStack, consumer, renderOffset.x, renderOffset.y + 0.12d, renderOffset.z, x2, renderOffset.y + 0.12d, z2, 1, 1, 1, 0.6f);
	}

	private static void drawDynamicPart(VehicleBase vehicle, float partialTick, Vec3 renderOffset, boolean rocket, PoseStack poseStack, VertexConsumer consumer) {
		double gravity = 9.81d * 0.05d * 0.05d;
		double speed = vehicle.localLaunchPower * 0.05d;
		double angle = 90 - vehicle.localTurretPitch - vehicle.getXRot();
		double yaw = vehicle.localTurretRotation + partialTick * vehicle.moveHelper.getRotationSpeed();

		double vH = -Trig.sinDegrees((float) angle) * speed;
		Vec3 accelerationVector = new Vec3(Trig.sinDegrees((float) yaw) * vH, Trig.cosDegrees((float) angle) * speed, Trig.cosDegrees((float) yaw) * vH);
		if (rocket) {
			drawRocketFlightPath(vehicle, renderOffset, speed, accelerationVector, gravity, poseStack, consumer);
		} else {
			drawNormalTrajectory(vehicle, renderOffset, gravity, accelerationVector, poseStack, consumer);
		}
	}

	private static void drawRocketFlightPath(VehicleBase vehicle, Vec3 renderOffset, double speed, Vec3 accelerationVector, double gravity, PoseStack poseStack, VertexConsumer consumer) {
		if (speed <= 0) {
			return;
		}
		int rocketBurnTime = (int) (speed * 20.f * Trig.ROCKET_BURN_TIME_FACTOR);

		Vec3 offset = vehicle.getMissileOffset();
		double x2 = renderOffset.x + offset.x;
		double y2 = renderOffset.y + offset.y;
		double z2 = renderOffset.z + offset.z;

		double floorY = renderOffset.y;

		float xAcc = (float) (accelerationVector.x / speed) * Trig.ROCKET_ACCELERATION_FACTOR;
		float yAcc = (float) (accelerationVector.y / speed) * Trig.ROCKET_ACCELERATION_FACTOR;
		float zAcc = (float) (accelerationVector.z / speed) * Trig.ROCKET_ACCELERATION_FACTOR;
		Vec3 adjustedAccelerationVector = new Vec3(xAcc, yAcc, zAcc);
		int steps = 0;
		while (y2 >= floorY && steps++ < 2000) {
			double x1 = x2;
			double y1 = y2;
			double z1 = z2;
			x2 += adjustedAccelerationVector.x;
			z2 += adjustedAccelerationVector.z;
			y2 += adjustedAccelerationVector.y;
			if (rocketBurnTime > 0) {
				rocketBurnTime--;
				adjustedAccelerationVector = adjustedAccelerationVector.add(xAcc, yAcc, zAcc);
			} else {
				adjustedAccelerationVector = adjustedAccelerationVector.add(0, -gravity, 0);
			}
			line(poseStack, consumer, x1, y1, z1, x2, y2, z2, 1.f, 0.4f, 0.4f, 0.4f);
		}
	}

	private static void drawNormalTrajectory(VehicleBase vehicle, Vec3 renderOffset, double gravity, Vec3 accelerationVector, PoseStack poseStack, VertexConsumer consumer) {
		Vec3 offset = vehicle.getMissileOffset();
		double x2 = renderOffset.x + offset.x;
		double y2 = renderOffset.y + offset.y;
		double z2 = renderOffset.z + offset.z;

		double floorY = renderOffset.y;
		Vec3 adjustedAccelerationVector = accelerationVector;
		int steps = 0;
		while (y2 >= floorY && steps++ < 2000) {
			double x1 = x2;
			double y1 = y2;
			double z1 = z2;
			x2 += adjustedAccelerationVector.x;
			z2 += adjustedAccelerationVector.z;
			y2 += adjustedAccelerationVector.y;
			adjustedAccelerationVector = adjustedAccelerationVector.add(0, -gravity, 0);
			line(poseStack, consumer, x1, y1, z1, x2, y2, z2, 1.f, 0.4f, 0.4f, 0.4f);
		}
	}
}
