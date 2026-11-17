package net.shadowmage.ancientwarfare.vehicle.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.shadowmage.ancientwarfare.core.input.InputHandler;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.network.PacketOpenAmmoSelection;
import net.shadowmage.ancientwarfare.vehicle.network.PacketVehicleInput;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/*
 * Client-only. Port notes:
 * - Keys and defaults as 1.12 (left A, right D, aim up R, aim down F, fire SPACE, ammo prev T / next G, turret left Z /
 *   right X, mouse aim toggle C, ammo selection V), in the vehicle key context (active while riding a vehicle with no
 *   screen open). The forward / reverse keys (W / S) are gone with driving; left / right turn a stand in place.
 * - 1.12 sent PacketVehicleInput with forward / turn / power / rotation bytes when one of the held keys was released
 *   and every 20 ticks. Only the turn byte had an effect for stands; the port sends just that (see
 *   PacketVehicleInput), on the same occasions and additionally at once when the turn state changes (1.12 started
 *   turning only with the next 20 tick packet or the next key release).
 * - enableMouseAim was a config field that 1.12 neither saved nor showed ("this probably needs code changes, unused
 *   now"); it is a plain static here, default on, toggled by the mouse aim key.
 * - getPlayerLookTargetClient: world.rayTraceBlocks -> level.clip(COLLIDER, no fluids); RayTracer.getCorrectedHeadVec
 *   (CodeChickenLib) -> player.getEyePosition(); Entity.getCollisionBorderSize -> getPickRadius.
 * - ClientTickEvent END -> ClientTickEvent.Post, KeyInputEvent (key up) -> InputEvent.Key with GLFW_RELEASE.
 */
public class VehicleInputHandler {
	private static final String CATEGORY = "keybind.category.awVehicles";
	public static final KeyMapping LEFT = key(AWVehicleStatics.KEY_VEHICLE_LEFT, GLFW.GLFW_KEY_A);
	public static final KeyMapping RIGHT = key(AWVehicleStatics.KEY_VEHICLE_RIGHT, GLFW.GLFW_KEY_D);
	public static final KeyMapping ASCEND_AIM_UP = key(AWVehicleStatics.KEY_VEHICLE_ASCEND_AIM_UP, GLFW.GLFW_KEY_R);
	public static final KeyMapping DESCEND_AIM_DOWN = key(AWVehicleStatics.KEY_VEHICLE_DESCEND_AIM_DOWN, GLFW.GLFW_KEY_F);
	public static final KeyMapping FIRE = key(AWVehicleStatics.KEY_VEHICLE_FIRE, GLFW.GLFW_KEY_SPACE);
	public static final KeyMapping AMMO_PREV = key(AWVehicleStatics.KEY_VEHICLE_AMMO_PREV, GLFW.GLFW_KEY_T);
	public static final KeyMapping AMMO_NEXT = key(AWVehicleStatics.KEY_VEHICLE_AMMO_NEXT, GLFW.GLFW_KEY_G);
	public static final KeyMapping TURRET_LEFT = key(AWVehicleStatics.KEY_VEHICLE_TURRET_LEFT, GLFW.GLFW_KEY_Z);
	public static final KeyMapping TURRET_RIGHT = key(AWVehicleStatics.KEY_VEHICLE_TURRET_RIGHT, GLFW.GLFW_KEY_X);
	public static final KeyMapping MOUSE_AIM = key(AWVehicleStatics.KEY_VEHICLE_MOUSE_AIM, GLFW.GLFW_KEY_C);
	public static final KeyMapping AMMO_SELECT = key(AWVehicleStatics.KEY_VEHICLE_AMMO_SELECT, GLFW.GLFW_KEY_V);

	public static boolean enableMouseAim = true;

	private static final Set<KeyMapping> releaseableKeys = new HashSet<>();
	private static boolean trackedKeyReleased = false;
	private static byte lastTurnInput = 0;

	private static KeyMapping key(String name, int keyCode) {
		return new KeyMapping(name, VehicleKeyConflictContext.INSTANCE, InputConstants.Type.KEYSYM, keyCode, CATEGORY);
	}

	private VehicleInputHandler() {
	}

	public static void initKeyBindings(RegisterKeyMappingsEvent event) {
		event.register(LEFT);
		event.register(RIGHT);
		event.register(ASCEND_AIM_UP);
		event.register(DESCEND_AIM_DOWN);
		event.register(FIRE);
		event.register(AMMO_PREV);
		event.register(AMMO_NEXT);
		event.register(TURRET_LEFT);
		event.register(TURRET_RIGHT);
		event.register(MOUSE_AIM);
		event.register(AMMO_SELECT);

		initCallbacks();
		initReleaseableKeys();
		NeoForge.EVENT_BUS.addListener(VehicleInputHandler::onKeyInput);
		NeoForge.EVENT_BUS.addListener(VehicleInputHandler::onTickEnd);
	}

	private static void initReleaseableKeys() {
		releaseableKeys.add(LEFT);
		releaseableKeys.add(RIGHT);
		releaseableKeys.add(ASCEND_AIM_UP);
		releaseableKeys.add(DESCEND_AIM_DOWN);
		releaseableKeys.add(TURRET_LEFT);
		releaseableKeys.add(TURRET_RIGHT);
	}

	private static void onKeyInput(InputEvent.Key evt) {
		if (evt.getAction() == GLFW.GLFW_RELEASE) {
			trackReleasedKeys(evt);
		}
	}

	private static void trackReleasedKeys(InputEvent.Key evt) {
		InputConstants.Key key = InputConstants.getKey(evt.getKey(), evt.getScanCode());
		if (releaseableKeys.stream().anyMatch(k -> k.getKey().equals(key))) {
			trackedKeyReleased = true;
		}
	}

	private static void initCallbacks() {
		InputHandler.registerCallBack(MOUSE_AIM, () -> {
			if (MOUSE_AIM.isDown()) {
				enableMouseAim = !enableMouseAim;
			}
		});
		InputHandler.registerCallBack(FIRE, new VehicleCallback(VehicleInputHandler::handleFireAction));
		InputHandler.registerCallBack(ASCEND_AIM_UP, new VehicleCallback(v -> v.firingHelper.handleAimKeyInput(-1, 0)));
		InputHandler.registerCallBack(DESCEND_AIM_DOWN, new VehicleCallback(v -> v.firingHelper.handleAimKeyInput(1, 0)));
		InputHandler.registerCallBack(TURRET_LEFT, new VehicleCallback(v -> v.firingHelper.handleAimKeyInput(0, -1)));
		InputHandler.registerCallBack(TURRET_RIGHT, new VehicleCallback(v -> v.firingHelper.handleAimKeyInput(0, 1)));
		InputHandler.registerCallBack(AMMO_NEXT, new VehicleCallback(v -> v.ammoHelper.setNextAmmo()));
		InputHandler.registerCallBack(AMMO_PREV, new VehicleCallback(v -> v.ammoHelper.setPreviousAmmo()));
		InputHandler.registerCallBack(AMMO_SELECT, new VehicleCallback(VehicleInputHandler::handleAmmoSelectAction));
	}

	private static void handleAmmoSelectAction(VehicleBase vehicle) {
		Minecraft mc = Minecraft.getInstance();
		if (!vehicle.isAmmoLoaded()) {
			mc.player.displayClientMessage(Component.translatable("gui.ancientwarfarevehicles.ammo.no_ammo"), true);
			return;
		}
		if (!vehicle.vehicleType.getValidAmmoTypes().isEmpty()) {
			//port: the server opens the screen (see PacketOpenAmmoSelection)
			NetworkHandler.sendToServer(new PacketOpenAmmoSelection(vehicle));
		}
	}

	private static void handleFireAction(VehicleBase vehicle) {
		if (!vehicle.isAmmoLoaded()) {
			Minecraft.getInstance().player.displayClientMessage(Component.translatable("gui.ancientwarfarevehicles.ammo.no_ammo"), true);
		}
		if (vehicle.isAimable()) {
			vehicle.firingHelper.handleFireInput();
		}
	}

	private static final float MAX_RANGE = 140;

	@Nullable
	private static Vec3 getPlayerLookTargetClient(Player player, Entity excludedEntity) {
		Vec3 playerEyesPos = player.getEyePosition();
		Vec3 lookVector = player.getViewVector(0);
		Vec3 endVector = playerEyesPos.add(lookVector.x * MAX_RANGE, lookVector.y * MAX_RANGE, lookVector.z * MAX_RANGE);
		BlockHitResult blockHit = player.level().clip(new ClipContext(playerEyesPos, endVector, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 hitVec = blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getLocation();

		Optional<Tuple<Double, Entity>> closestEntityFound = getClosestCollidedEntity(excludedEntity, playerEyesPos, lookVector, endVector);
		if (closestEntityFound.isPresent() && (hitVec == null || closestEntityFound.get().getA() < hitVec.distanceTo(playerEyesPos))) {
			Entity hitEntity = closestEntityFound.get().getB();
			hitVec = new Vec3(hitEntity.getX(), hitEntity.getY() + hitEntity.getBbHeight() * 0.65d, hitEntity.getZ());
		}
		return hitVec;
	}

	private static Optional<Tuple<Double, Entity>> getClosestCollidedEntity(Entity excludedEntity, Vec3 playerEyesPos, Vec3 lookVector, Vec3 endVector) {
		Minecraft mc = Minecraft.getInstance();
		Entity viewEntity = mc.getCameraEntity();
		if (viewEntity == null || mc.level == null) {
			return Optional.empty();
		}
		List<Entity> possibleHitEntities = mc.level.getEntities(viewEntity,
				viewEntity.getBoundingBox().expandTowards(lookVector.x * MAX_RANGE, lookVector.y * MAX_RANGE, lookVector.z * MAX_RANGE).inflate(1, 1, 1));
		return possibleHitEntities.stream().filter(e -> e != excludedEntity && e.isPickable())
				.map(e -> new Tuple<>(getDistanceToCollidedEntity(e, playerEyesPos, endVector), e)).filter(t -> t.getA() < Double.MAX_VALUE)
				.min(Comparator.comparing(Tuple::getA));
	}

	private static double getDistanceToCollidedEntity(Entity entity, Vec3 startVector, Vec3 endVector) {
		float borderSize = entity.getPickRadius();
		AABB entBB = entity.getBoundingBox().inflate(borderSize, borderSize, borderSize);
		return entBB.clip(startVector, endVector).map(startVector::distanceTo).orElse(Double.MAX_VALUE);
	}

	private static void onTickEnd(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.getVehicle() instanceof VehicleBase) {
			VehicleBase vehicle = (VehicleBase) mc.player.getVehicle();
			handleTickInput(vehicle);
			if (enableMouseAim) {
				handleMouseAimUpdate(vehicle);
			}
		} else {
			lastTurnInput = 0;
		}
	}

	private static void handleMouseAimUpdate(VehicleBase vehicle) {
		if (vehicle.tickCount % 5 == 0) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 pos = getPlayerLookTargetClient(mc.player, vehicle);
		if (pos != null) {
			vehicle.firingHelper.handleAimInput(pos);
		}
	}

	private static void handleTickInput(VehicleBase vehicle) {
		byte turnInput = 0;
		if (vehicle.isDrivable()) {
			if (RIGHT.isDown() && !LEFT.isDown()) {
				turnInput = 1;
			} else if (LEFT.isDown() && !RIGHT.isDown()) {
				turnInput = -1;
			}
		}
		if (trackedKeyReleased || vehicle.tickCount % 20 == 0 || turnInput != lastTurnInput) {
			trackedKeyReleased = false;
			lastTurnInput = turnInput;
			NetworkHandler.sendToServer(new PacketVehicleInput(vehicle, turnInput));
		}
	}
}
