package net.shadowmage.ancientwarfare.vehicle.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.vehicle.NpcSiegeEngineer;
import net.shadowmage.ancientwarfare.npc.init.AWNPCEntities;
import net.shadowmage.ancientwarfare.structure.api.TemplateRuleEntityBase;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplate;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;
import net.shadowmage.ancientwarfare.structure.template.plugin.defaultplugins.entityrules.TemplateRuleVehicle;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.entity.IVehicleType;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleType;
import net.shadowmage.ancientwarfare.vehicle.missiles.IAmmo;
import net.shadowmage.ancientwarfare.vehicle.missiles.MissileBase;
import net.shadowmage.ancientwarfare.vehicle.registry.AmmoRegistry;
import net.shadowmage.ancientwarfare.vehicle.registry.VehicleRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/*
 * Headless checks of the reduced vehicle module (run with ./gradlew runGameTestServer).
 *
 * Shots travel 70 - 170 blocks, far more than a test structure. Every firing test therefore works on its own "lane"
 * of the flat test world next to the world spawn (see Lane): a row of force-loaded chunks with the weapon at its
 * east end firing west (AW yaw 90: vX = -sin(yaw)). The entities are removed when the test ends.
 *
 * Expected ranges are derived from the 1.12 source, not measured in the port (script tools/vehicle_expected_range.py
 * reproduces the numbers):
 *   start    = vehicle position + VehicleBase.getMissileOffset(): turret offset (0, turretVerticalOffset, 0) plus the
 *              missile offset of length |missileForwardsOffset| rotated by turret yaw and pitch
 *   velocity = MissileBase.setMissileParams2: horizontal cos(pitch) * v * 0.05, vertical sin(pitch) * v * 0.05 per tick
 *   flight   = MissileBase.onMovementTick: pos += motion, then motionY -= 9.81 * 0.05 * 0.05 (Ammo.GRAVITY_FACTOR);
 *              impact where the segment pos -> pos + motion crosses the ground (y of the vehicle's feet)
 *   rockets  = burn time (int)(|motion| * 20 * 3) ticks, motion starts at direction * 0.01 and grows by direction *
 *              0.01 every tick, no gravity while burning (AmmoHwachaRocket.BURN_TIME_FACTOR / ACCELERATION_FACTOR)
 * with v = the type's baseMissileVelocityMax and pitch = the default aim (fixed pitch of catapult 20, hwacha 39,
 * trebuchet 70; ballista and cannon are aimed at their maximum pitch 15 by the test). "Range" below is measured from the vehicle
 * position along the firing direction. The accuracy scatter (adjust_missiles_for_accuracy, gaussian) is switched
 * off for these tests. Tolerance: 1 block (float arithmetic of the 1.12 code against the double arithmetic of the
 * derivation, and the 0.05 block step back of a missile that sticks in the ground).
 */
@GameTestHolder(AncientWarfare.MOD_ID)
@PrefixGameTestTemplate(false)
public class VehicleGameTests {
	private static final String TEMPLATE = "npc_empty";
	private static final String OWNER = "AwTestOwner";
	private static final double TOLERANCE = 1.0D;

	/*
	 * The 1.12 missile code keeps positions in floats (setMissileParams(float x, y, z)). If the test world's spawn is
	 * far from the origin a float cannot hold a position exactly any more; the tolerance grows by that step.
	 */
	private static double tolerance(Lane lane) {
		return TOLERANCE + 2 * Math.ulp((float) Math.max(Math.abs(lane.x) + 200, Math.abs(lane.z)));
	}

	private static int nextLane = 0;

	/* ------------------------------------------------ lane ------------------------------------------------ */

	private static class Lane {
		final ServerLevel level;
		final List<ChunkPos> chunks = new ArrayList<>();
		final double x;
		final double y;
		final double z;
		final int lengthChunks;
		final int southChunks;

		/* lengthChunks: how many chunks west of the weapon's chunk are loaded (one chunk wide, to keep the test world small) */
		Lane(GameTestHelper helper, int lengthChunks) {
			this(helper, lengthChunks, 0);
		}

		/* southChunks: additional rows of chunks south of the lane (for a target that is not straight ahead) */
		Lane(GameTestHelper helper, int lengthChunks, int southChunks) {
			this.lengthChunks = lengthChunks;
			this.southChunks = southChunks;
			level = helper.getLevel();
			int row;
			synchronized (VehicleGameTests.class) {
				row = nextLane + southChunks;
				nextLane += 1 + southChunks;
			}
			/*
			 * Lanes are rows of chunks next to the world spawn, north of it (the test grid grows to the south-east of its
			 * origin): the structure world generator leaves the 400 blocks around the spawn alone
			 * (AWStructureStatics.withinProtectionRange), so nothing is generated into a line of fire, and the lanes
			 * add few chunks to the test world. One row per lane; shots do not leave their row.
			 */
			BlockPos spawn = level.getSharedSpawnPos();
			int bx = ((spawn.getX() + 96) >> 4 << 4) + 8;
			int bz = ((spawn.getZ() - 48 - row * 16) >> 4 << 4) + 8;
			ChunkPos base = new ChunkPos(new BlockPos(bx, 0, bz));
			for (int cx = base.x - lengthChunks; cx <= base.x; cx++) {
				for (int cz = base.z; cz <= base.z + southChunks; cz++) {
					level.setChunkForced(cx, cz, true);
					level.getChunk(cx, cz);
					chunks.add(new ChunkPos(cx, cz));
				}
			}
			x = bx + 0.5D;
			z = bz + 0.5D;
			y = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
		}

		void release() {
			AABB all = new AABB(x - lengthChunks * 16 - 32, y - 16, z - 7.9, x + 7.9, y + 200, z + 7.9 + southChunks * 16);
			level.getEntitiesOfClass(VehicleBase.class, all).forEach(e -> e.discard());
			level.getEntitiesOfClass(MissileBase.class, all).forEach(e -> e.discard());
			level.getEntitiesOfClass(NpcBase.class, all).forEach(e -> e.discard());
			level.getEntitiesOfClass(Zombie.class, all).forEach(e -> e.discard());
			/*
			 * The chunks stay force-loaded until the test server stops. Releasing them made neighbouring lanes (the rows
			 * are adjacent) unreliable: now and then a weapon in the next row never started to tick.
			 */
		}

		VehicleBase vehicle(IVehicleType type, int materialLevel) {
			VehicleBase vehicle = VehicleType.getVehicleForType(level, type.getGlobalVehicleType(), materialLevel)
					.orElseThrow(() -> new IllegalStateException("no vehicle for type " + type.getGlobalVehicleType()));
			vehicle.setPos(x, y, z);
			//what ItemSpawner.spawnVehicle does, without the setup time: facing west
			vehicle.setYRot(90);
			vehicle.yRotO = 90;
			vehicle.localTurretDestRot = vehicle.localTurretRotation = vehicle.localTurretRotationHome = 90;
			level.addFreshEntity(vehicle);
			return vehicle;
		}

		List<MissileBase> missiles() {
			return level.getEntitiesOfClass(MissileBase.class, new AABB(x - lengthChunks * 16 - 32, y - 16, z - 7.9, x + 7.9, y + 300, z + 7.9 + southChunks * 16));
		}
	}

	private static void load(VehicleBase vehicle, IAmmo ammo, int count) {
		ItemStack rest = vehicle.inventory.ammoInventory.insertItem(0, new ItemStack(AmmoRegistry.getItemForAmmo(ammo), count), false);
		if (!rest.isEmpty()) {
			throw new IllegalStateException("ammo " + ammo.getRegistryName() + " was not accepted by " + vehicle.vehicleType.getConfigName());
		}
	}

	/* runs body every tick; an exception fails the test; the lane is released when the test is over */
	private static void run(GameTestHelper helper, Lane lane, Runnable body) {
		helper.onEachTick(() -> {
			try {
				body.run();
			}
			catch (RuntimeException e) {
				lane.release();
				throw e;
			}
		});
	}

	private static void done(GameTestHelper helper, Lane lane) {
		lane.release();
		helper.succeed();
	}

	private static void fail(String message) {
		throw new net.minecraft.gametest.framework.GameTestAssertException(message);
	}

	/* where and how a tracked missile ended */
	private static class Impact {
		double range;
		double lateral;
		Vec3 pos;
		boolean stuckInGround;
	}

	/*
	 * Tracks every missile of the lane. A missile that is removed ended on the segment lastPos -> lastPos + lastMotion
	 * (MissileBase removes itself before it moves); the impact point is where that segment crosses the ground. A
	 * missile that stays (bolt, rocket) reports its resting position.
	 */
	private static class Tracker {
		final Lane lane;
		final Map<MissileBase, Vec3[]> flying = new HashMap<>();
		final List<Impact> impacts = new ArrayList<>();
		int seen = 0;

		Tracker(Lane lane) {
			this.lane = lane;
		}

		void tick() {
			for (MissileBase missile : lane.missiles()) {
				if (!flying.containsKey(missile) && !missile.isInGround()) {
					seen++;
				}
				if (missile.isInGround()) {
					if (flying.remove(missile) != null) {
						impacts.add(impact(missile.position(), true));
					}
					continue;
				}
				flying.put(missile, new Vec3[] {missile.position(), missile.getDeltaMovement()});
			}
			flying.entrySet().removeIf(entry -> {
				if (entry.getKey().isRemoved()) {
					Vec3 pos = entry.getValue()[0];
					Vec3 motion = entry.getValue()[1];
					double f = motion.y < 0 ? Math.min(1, Math.max(0, (lane.y - pos.y) / motion.y)) : 0;
					impacts.add(impact(pos.add(motion.scale(f)), false));
					return true;
				}
				return false;
			});
		}

		private Impact impact(Vec3 pos, boolean stuck) {
			Impact impact = new Impact();
			impact.pos = pos;
			impact.range = lane.x - pos.x;
			impact.lateral = pos.z - lane.z;
			impact.stuckInGround = stuck;
			return impact;
		}
	}

	/*
	 * Loads ammo, fires once at the default aim and hands every impact to the check once "shots" missiles have landed.
	 */
	private static void fireTest(GameTestHelper helper, IVehicleType type, IAmmo ammo, int ammoCount, int shots, double expectedRange, Consumer<Shot> check) {
		AWVehicleStatics.generalSettings.adjustMissilesForAccuracy = false;
		Lane lane = new Lane(helper, (int) (expectedRange / 16) + 2);
		VehicleBase vehicle = lane.vehicle(type, 1);
		if (type.canAdjustPitch()) {
			//a new weapon lowers its barrel to the minimum pitch (the 1.12 default of localTurretDestPitch is outside the range)
			vehicle.updateTurretAngles(type.getBasePitchMax(), 90);
		}
		load(vehicle, ammo, ammoCount);
		Tracker tracker = new Tracker(lane);
		int[] state = {0};
		run(helper, lane, () -> {
			if (state[0] == 0) {
				if (vehicle.ammoHelper.getCurrentAmmoType() != ammo) {
					fail("ammo " + ammo.getRegistryName() + " was not selected after loading, current: " + vehicle.ammoHelper.getCurrentAmmoType());
				}
				if (vehicle.ammoHelper.getCurrentAmmoCount() != ammoCount) {
					fail("ammo count after loading is " + vehicle.ammoHelper.getCurrentAmmoCount());
				}
				if (!vehicle.firingHelper.isReadyToFire()) {
					fail("loaded weapon is not ready to fire");
				}
				vehicle.firingHelper.handleFireUpdate();
				state[0] = 1;
				return;
			}
			tracker.tick();
			if (++state[0] > 700 + shots * 6) {
				//say what is missing instead of running into the framework's timeout
				fail(type.getConfigName() + ": no result after " + state[0] + " ticks; vehicle ticks " + vehicle.tickCount + ", removed " + vehicle.isRemoved()
						+ ", in level " + (lane.level.getEntity(vehicle.getId()) == vehicle) + ", entity ticking " + lane.level.isPositionEntityTicking(vehicle.blockPosition())
						+ ", pos " + vehicle.blockPosition() + ", missiles seen "
						+ tracker.seen + ", flying " + tracker.flying.size() + ", impacts " + tracker.impacts.size() + ", ammo left " + vehicle.inventory.ammoInventory.getStackInSlot(0).getCount());
			}
			if (tracker.impacts.size() >= shots && tracker.flying.isEmpty()) {
				if (tracker.seen != shots) {
					fail("expected " + shots + " missiles, saw " + tracker.seen);
				}
				/*
				 * The inventory is the truth. The counter of the ammo helper is one lower after a shot, as in 1.12:
				 * decreaseCurrentAmmo() takes the item out (AmmoStackHandler.onContentsChanged recounts) and then
				 * subtracts the removed item from the counter once more; the next inventory change recounts it.
				 */
				int left = vehicle.inventory.ammoInventory.getStackInSlot(0).getCount();
				if (left != ammoCount - shots) {
					fail("ammo left in the inventory: " + left + ", expected " + (ammoCount - shots));
				}
				if (vehicle.ammoHelper.getCountOf(ammo) != left - 1) {
					fail("ammo counter: " + vehicle.ammoHelper.getCountOf(ammo) + ", expected the 1.12 value " + (left - 1));
				}
				for (Impact impact : tracker.impacts) {
					if (Math.abs(impact.range - expectedRange) > tolerance(lane)) {
						fail(type.getConfigName() + " / " + ammo.getRegistryName().getPath() + ": range " + impact.range + ", expected " + expectedRange + " +- " + tolerance(lane));
					}
				}
				Shot shot = new Shot();
				shot.lane = lane;
				shot.vehicle = vehicle;
				shot.impacts = tracker.impacts;
				check.accept(shot);
				done(helper, lane);
			}
		});
	}

	private static class Shot {
		Lane lane;
		VehicleBase vehicle;
		List<Impact> impacts;

		/* number of ground blocks (the layer the weapon stands on) within r blocks of the impact that are now air */
		int brokenGroundBlocks(Impact impact, int r) {
			return count(impact, r, -1, Blocks.AIR);
		}

		int count(Impact impact, int r, int dy, net.minecraft.world.level.block.Block block) {
			int count = 0;
			BlockPos center = BlockPos.containing(impact.pos.x, lane.y + dy, impact.pos.z);
			for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, 0, -r), center.offset(r, 0, r))) {
				if (lane.level.getBlockState(pos).is(block)) {
					count++;
				}
			}
			return count;
		}
	}

	private static void expectStraight(Shot shot) {
		for (Impact impact : shot.impacts) {
			if (Math.abs(impact.lateral) > tolerance(shot.lane) - 0.5D) {
				fail("shot left the firing line by " + impact.lateral);
			}
		}
	}

	private static void expectBrokenGround(Shot shot) {
		expectStraight(shot);
		if (shot.brokenGroundBlocks(shot.impacts.get(0), 2) == 0) {
			fail("the shot broke no ground block at its impact point " + shot.impacts.get(0).pos);
		}
	}

	private static void expectStuckInGround(Shot shot) {
		expectStraight(shot);
		for (Impact impact : shot.impacts) {
			if (!impact.stuckInGround || Math.abs(impact.pos.y - shot.lane.y) > 0.5D) {
				fail("missile is not stuck in the ground: " + impact.pos + " stuck=" + impact.stuckInGround);
			}
		}
	}

	/* ------------------------------------------------ weapons ------------------------------------------------ */

	/* 1.12: stone shot 10 kg, v 37, pitch 20, start 0.68 behind / 2.07 above the stand -> 96.0 blocks, 55 ticks */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void catapultStandFiresStoneShot(GameTestHelper helper) {
		fireTest(helper, VehicleRegistry.CATAPULT_STAND_FIXED, AmmoRegistry.ammoStoneShot10, 3, 1, 96.0D, VehicleGameTests::expectBrokenGround);
	}

	/* 1.12: v 32, pitch 20, turret height 0.4 -> 73.6 blocks */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void catapultStandTurretFiresStoneShot(GameTestHelper helper) {
		fireTest(helper, VehicleRegistry.CATAPULT_STAND_TURRET, AmmoRegistry.ammoStoneShot10, 3, 1, 73.6D, VehicleGameTests::expectBrokenGround);
	}

	/* 1.12: bolt, v 45, pitch 15 (max), start 1.125 above the stand -> 109.3 blocks; the bolt is persistent */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void ballistaStandFiresBolt(GameTestHelper helper) {
		fireTest(helper, VehicleRegistry.BALLISTA_STAND_FIXED, AmmoRegistry.ammoBallistaBolt, 3, 1, 109.3D, VehicleGameTests::expectStuckInGround);
	}

	/* 1.12: v 42, pitch 15 -> 95.9 blocks */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void ballistaStandTurretFiresBolt(GameTestHelper helper) {
		fireTest(helper, VehicleRegistry.BALLISTA_STAND_TURRET, AmmoRegistry.ammoBallistaBolt, 3, 1, 95.9D, VehicleGameTests::expectStuckInGround);
	}

	/* 1.12: iron shot 5 kg, v 42, pitch 15, start 0.72 above the stand -> 94.5 blocks (11 ticks fuse before the shot) */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void cannonStandFiresIronShot(GameTestHelper helper) {
		fireTest(helper, VehicleRegistry.CANNON_STAND_FIXED, AmmoRegistry.ammoIronShot5, 3, 1, 94.5D, VehicleGameTests::expectBrokenGround);
	}

	/* 1.12: v 40, pitch 15, start 0.84 above the stand -> 86.4 blocks */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void cannonStandTurretFiresIronShot(GameTestHelper helper) {
		fireTest(helper, VehicleRegistry.CANNON_STAND_TURRET, AmmoRegistry.ammoIronShot5, 3, 1, 86.4D, VehicleGameTests::expectBrokenGround);
	}

	/*
	 * 1.12: 36 rockets, one every 5 ticks, v 42 -> 126 ticks of burn at 0.01 per tick along pitch 39, then ballistic:
	 * 166.1 blocks for a rocket from the launcher's centre. The rockets leave from a 9 x 4 grid (calcMissileOffset:
	 * +- 0.5 sideways, up to 0.375 * sin(39) forward and 0.375 * cos(39) up), which moves the impact by less than the
	 * tolerance. Rockets are persistent.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 2400)
	public static void hwachaFiresRocketBarrage(GameTestHelper helper) {
		fireTest(helper, VehicleRegistry.HWACHA, AmmoRegistry.ammoRocket, 40, 36, 166.1D, shot -> {
			for (Impact impact : shot.impacts) {
				if (!impact.stuckInGround || Math.abs(impact.lateral) > tolerance(shot.lane)) {
					fail("rocket ended at " + impact.pos + " stuck=" + impact.stuckInGround);
				}
			}
		});
	}

	/* 1.12: stone shot 10 kg, v 40, pitch 70, start 7.84 above the stand -> 108.3 blocks, 158 ticks */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 1600)
	public static void trebuchetStandFiresStoneShot(GameTestHelper helper) {
		fireTest(helper, VehicleRegistry.TREBUCHET_STAND_FIXED, AmmoRegistry.ammoStoneShot10, 3, 1, 108.3D, VehicleGameTests::expectBrokenGround);
	}

	/* 1.12: start 8.44 above the stand -> 108.5 blocks */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 1600)
	public static void trebuchetStandTurretFiresStoneShot(GameTestHelper helper) {
		fireTest(helper, VehicleRegistry.TREBUCHET_STAND_TURRET, AmmoRegistry.ammoStoneShot10, 3, 1, 108.5D, VehicleGameTests::expectBrokenGround);
	}

	/*
	 * 1.12: v 50, pitch 70, start 19.6 above the ground -> 171.5 blocks, 200 ticks. maxMissileWeight is 30, so a
	 * 30 kg stone shot is launched alone and at full velocity (getMissileLaunchCount / getAdjustedMaxMissileVelocity).
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 2000)
	public static void trebuchetLargeFiresStoneShot(GameTestHelper helper) {
		fireTest(helper, VehicleRegistry.TREBUCHET_LARGE, AmmoRegistry.ammoStoneShot30, 3, 1, 171.5D, VehicleGameTests::expectBrokenGround);
	}

	/* flame shot: same flight as the stone shot; AmmoFlameShot.igniteBlock puts fire on the ground at the impact */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void catapultFlameShotSetsFire(GameTestHelper helper) {
		fireTest(helper, VehicleRegistry.CATAPULT_STAND_FIXED, AmmoRegistry.ammoFireShot10, 2, 1, 96.0D, shot -> {
			expectStraight(shot);
			if (shot.count(shot.impacts.get(0), 2, 0, Blocks.FIRE) == 0) {
				fail("flame shot left no fire at " + shot.impacts.get(0).pos);
			}
		});
	}

	/*
	 * high explosive shot 10 kg: AmmoExplosiveShot.explode -> explosion of power 7 * 10 / 45 = 1.56 at the impact
	 * (0.2 above the ground), which blows a hole into the grass of the flat world
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void catapultExplosiveShotExplodes(GameTestHelper helper) {
		fireTest(helper, VehicleRegistry.CATAPULT_STAND_FIXED, AmmoRegistry.ammoHE10, 2, 1, 96.0D, shot -> {
			expectStraight(shot);
			int broken = shot.brokenGroundBlocks(shot.impacts.get(0), 2);
			if (broken < 2) {
				fail("explosive shot broke only " + broken + " ground blocks at " + shot.impacts.get(0).pos);
			}
		});
	}

	/* a weapon without ammo does not fire (VehicleFiringHelper.handleFireUpdate / isReadyToFire) */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void unloadedWeaponDoesNotFire(GameTestHelper helper) {
		Lane lane = new Lane(helper, 1);
		VehicleBase vehicle = lane.vehicle(VehicleRegistry.CATAPULT_STAND_FIXED, 0);
		int[] ticks = {0};
		run(helper, lane, () -> {
			if (ticks[0] == 0) {
				if (vehicle.firingHelper.isReadyToFire()) {
					fail("weapon without ammo is ready to fire");
				}
				vehicle.firingHelper.handleFireUpdate();
			}
			if (!lane.missiles().isEmpty()) {
				fail("weapon without ammo fired");
			}
			if (++ticks[0] > 40) {
				done(helper, lane);
			}
		});
	}

	/* ------------------------------------------------ NBT ------------------------------------------------ */

	/* entity NBT round trip of a loaded, aimed weapon (1.12 keys: vehType, matLvl, health, inventory, ammo, tp, tr...) */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void loadedWeaponSurvivesSaveAndLoad(GameTestHelper helper) {
		Lane lane = new Lane(helper, 1);
		VehicleBase vehicle = lane.vehicle(VehicleRegistry.BALLISTA_STAND_TURRET, 3);
		load(vehicle, AmmoRegistry.ammoBallistaBoltFlame, 17);
		vehicle.inventory.ammoInventory.insertItem(1, new ItemStack(AmmoRegistry.getItemForAmmo(AmmoRegistry.ammoBallistaBolt), 5), false);
		vehicle.setHealth(77.5F);
		vehicle.updateTurretAngles(7, 110);
		int[] ticks = {0};
		run(helper, lane, () -> {
			if (++ticks[0] < 5) {
				return;
			}
			CompoundTag tag = new CompoundTag();
			if (!vehicle.save(tag)) {
				fail("vehicle was not saved");
			}
			if (tag.getInt("vehType") != 5 || tag.getInt("matLvl") != 3) {
				fail("saved vehType / matLvl: " + tag.getInt("vehType") + " / " + tag.getInt("matLvl"));
			}
			VehicleBase loaded = (VehicleBase) EntityType.loadEntityRecursive(tag, lane.level, e -> e);
			if (loaded == null) {
				fail("vehicle could not be loaded");
				return;
			}
			if (loaded.vehicleType != VehicleRegistry.BALLISTA_STAND_TURRET || loaded.vehicleMaterialLevel != 3) {
				fail("loaded type " + loaded.vehicleType + " level " + loaded.vehicleMaterialLevel);
			}
			if (Math.abs(loaded.getHealth() - 77.5F) > 0.001F) {
				fail("loaded health " + loaded.getHealth());
			}
			if (loaded.ammoHelper.getCountOf(AmmoRegistry.ammoBallistaBoltFlame) != 17 || loaded.ammoHelper.getCountOf(AmmoRegistry.ammoBallistaBolt) != 5) {
				fail("loaded ammo counts " + loaded.ammoHelper.getCountOf(AmmoRegistry.ammoBallistaBoltFlame) + " / " + loaded.ammoHelper.getCountOf(AmmoRegistry.ammoBallistaBolt));
			}
			if (loaded.ammoHelper.getCurrentAmmoType() != AmmoRegistry.ammoBallistaBoltFlame) {
				fail("loaded ammo selection " + loaded.ammoHelper.getCurrentAmmoType());
			}
			if (loaded.inventory.ammoInventory.getStackInSlot(0).getCount() != 17 || loaded.inventory.ammoInventory.getStackInSlot(1).getCount() != 5) {
				fail("loaded ammo inventory differs");
			}
			if (Math.abs(loaded.localTurretPitch - 7) > 0.001F || Math.abs(loaded.localTurretRotation - 110) > 0.001F || Math.abs(loaded.getYRot() - 90) > 0.001F) {
				fail("loaded aim: pitch " + loaded.localTurretPitch + " turret " + loaded.localTurretRotation + " yaw " + loaded.getYRot());
			}
			if (Math.abs(loaded.getBbWidth() - 1.2F) > 0.001F || Math.abs(loaded.getBbHeight() - 1.4F) > 0.001F) {
				fail("loaded size " + loaded.getBbWidth() + " x " + loaded.getBbHeight());
			}
			if (!loaded.firingHelper.isReadyToFire()) {
				fail("loaded weapon is not ready to fire");
			}
			loaded.discard();
			done(helper, lane);
		});
	}

	/* ------------------------------------------------ NPC crew ------------------------------------------------ */

	/*
	 * An owned siege engineer finds the loaded ballista turret (NpcAIFindVehicle, 30 blocks), walks to it and mounts
	 * (NpcAIMountVehicle), picks the zombie as target, turns the stand / turret and sets the pitch (NpcAIAimVehicle)
	 * and fires (NpcAIFireVehicle) until the zombie is hit by a bolt (18 damage, AmmoBallistaBolt.onImpactEntity).
	 * The zombie stands 24 blocks away, 40 degrees off the weapon's facing, so the engineer has to turn the weapon.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 4800)
	public static void siegeEngineerMansBallistaAndHitsTarget(GameTestHelper helper) {
		AWVehicleStatics.generalSettings.adjustMissilesForAccuracy = false;
		Lane lane = new Lane(helper, 3, 2);
		VehicleBase vehicle = lane.vehicle(VehicleRegistry.BALLISTA_STAND_TURRET, 1);
		load(vehicle, AmmoRegistry.ammoBallistaBolt, 32);

		NpcBase npc = AWNPCEntities.createNpc(lane.level, "siege_engineer", "", "");
		if (!(npc instanceof NpcSiegeEngineer)) {
			lane.release();
			helper.fail("could not create the siege engineer");
			return;
		}
		NpcSiegeEngineer engineer = (NpcSiegeEngineer) npc;
		engineer.setOwnerName(OWNER);
		engineer.setFoodRemaining(6000);
		engineer.moveTo(lane.x + 4, lane.y, lane.z + 3, 0, 0);
		engineer.setHomeAreaAtCurrentPosition();
		lane.level.addFreshEntity(engineer);

		//a husk: a zombie would burn in the daylight of the test world
		Zombie zombie = EntityType.HUSK.create(lane.level);
		double angle = Math.toRadians(40);
		zombie.moveTo(lane.x - Math.cos(angle) * 24, lane.y, lane.z + Math.sin(angle) * 24, 0, 0);
		zombie.setNoAi(true);
		zombie.setPersistenceRequired();
		zombie.setSilent(true);
		lane.level.addFreshEntity(zombie);
		float fullHealth = zombie.getHealth();

		boolean[] mounted = {false};
		run(helper, lane, () -> {
			if (!mounted[0] && engineer.getVehicle() == vehicle) {
				mounted[0] = true;
			}
			if (zombie.getHealth() < fullHealth || !zombie.isAlive()) {
				if (!mounted[0]) {
					fail("the zombie was hurt before the engineer mounted the weapon");
				}
				if (vehicle.ammoHelper.getCountOf(AmmoRegistry.ammoBallistaBolt) >= 32) {
					fail("the zombie was hurt but no bolt was used");
				}
				done(helper, lane);
			}
		});
	}

	/* ------------------------------------------------ structure templates ------------------------------------------------ */

	/*
	 * The template rule "AWVehicle" of a shipped 1.12 template (NoggCityWallsCorner: ballista stand turrets, vehType 5,
	 * material level 4, 200 health) places the weapon with its saved rotation and turret rotation; the same rule with
	 * the type of a vehicle that is not ported (17, chest cart) places nothing.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 800)
	public static void templateRulePlacesWeaponAndSkipsUnportedVehicle(GameTestHelper helper) {
		Lane lane = new Lane(helper, 1);
		BlockPos pos = BlockPos.containing(lane.x, lane.y, lane.z);
		AABB area = new AABB(pos).inflate(4);
		int[] ticks = {0};
		TemplateRuleVehicle[] ruleHolder = {null};
		CompoundTag ruleTag = new CompoundTag();
		//the checks run some ticks after the placements: entities of chunks that were just force-loaded are not visible to queries at once
		run(helper, lane, () -> {
			int tick = ticks[0]++;
			if (tick == 5) {
				StructureTemplate template = StructureTemplateManager.getTemplate("NoggCityWallsCorner").orElse(null);
				if (template == null) {
					fail("the shipped template NoggCityWallsCorner was not loaded");
					return;
				}
				for (TemplateRuleEntityBase entityRule : template.getEntityRules().values()) {
					if (entityRule instanceof TemplateRuleVehicle) {
						ruleHolder[0] = (TemplateRuleVehicle) entityRule;
						break;
					}
				}
				if (ruleHolder[0] == null) {
					fail("NoggCityWallsCorner has no AWVehicle rule (entity rules: " + template.getEntityRules().size() + ")");
					return;
				}
				ruleHolder[0].handlePlacement(lane.level, 0, pos, null);
			} else if (tick > 5 && ruleTag.isEmpty()) {
				if (lane.level.getEntitiesOfClass(VehicleBase.class, area).isEmpty()) {
					//not visible yet (the chunk is still on its way to entity ticking); give it time
					if (tick > 600) {
						fail("the rule placed no vehicle at " + pos);
					}
					return;
				}
				TemplateRuleVehicle rule = ruleHolder[0];
				List<VehicleBase> placed = lane.level.getEntitiesOfClass(VehicleBase.class, area);
				if (placed.size() != 1) {
					fail("the rule placed " + placed.size() + " vehicles at " + pos);
				}
				VehicleBase vehicle = placed.get(0);
				if (vehicle.vehicleType != VehicleRegistry.BALLISTA_STAND_TURRET || vehicle.vehicleMaterialLevel != 4 || Math.abs(vehicle.getHealth() - 200F) > 0.01F) {
					fail("placed vehicle: type " + vehicle.vehicleType + " level " + vehicle.vehicleMaterialLevel + " health " + vehicle.getHealth());
				}
				rule.writeRuleData(ruleTag);
				float yawDiff = net.minecraft.util.Mth.wrapDegrees(vehicle.getYRot() - rule.rotation);
				float turretDiff = net.minecraft.util.Mth.wrapDegrees(vehicle.localTurretDestRot - ruleTag.getFloat("turretRotation"));
				if (Math.abs(yawDiff) > 0.01F || Math.abs(turretDiff) > 0.01F || vehicle.localTurretRotation != vehicle.localTurretDestRot) {
					fail("placed vehicle yaw " + vehicle.getYRot() + " (rule " + rule.rotation + "), turret " + vehicle.localTurretDestRot + " (rule " + ruleTag.getFloat("turretRotation") + ")");
				}
				vehicle.discard();

				CompoundTag chestCartTag = ruleTag.copy();
				chestCartTag.getCompound("entityData").putInt("vehType", 17);
				TemplateRuleVehicle chestCart = new TemplateRuleVehicle();
				chestCart.parseRule(chestCartTag);
				chestCart.handlePlacement(lane.level, 0, pos, null);
				ticks[0] = 10000;
			} else if (tick == 10005) {
				List<VehicleBase> skipped = lane.level.getEntitiesOfClass(VehicleBase.class, area, e -> !e.isRemoved());
				if (!skipped.isEmpty()) {
					fail("a rule for vehicle type 17 placed " + skipped.get(0));
				}
				done(helper, lane);
			}
		});
	}

	/*
	 * Improvement over 1.12 (config improvements.ammo_fallback): the selected ammunition is taken out of the weapon;
	 * the weapon selects the other loaded type by itself. With the switch off it stays on the empty type, as in 1.12.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 200)
	public static void improvementAmmoFallbackSelectsLoadedType(GameTestHelper helper) {
		Lane lane = new Lane(helper, 1);
		boolean saved = AWVehicleStatics.improvements.ammoFallback;
		try {
			for (boolean improved : new boolean[] {false, true}) {
				AWVehicleStatics.improvements.ammoFallback = improved;
				VehicleBase vehicle = lane.vehicle(VehicleRegistry.CATAPULT_STAND_FIXED, 0);
				load(vehicle, AmmoRegistry.ammoStoneShot10, 3);
				vehicle.ammoHelper.updateAmmoCounts();
				if (vehicle.ammoHelper.getCurrentAmmoType() != AmmoRegistry.ammoStoneShot10) {
					fail("the first loaded type was not selected: " + vehicle.ammoHelper.getCurrentAmmoType());
				}
				//second slot (load() fills the first one)
				if (!vehicle.inventory.ammoInventory.insertItem(1, new ItemStack(AmmoRegistry.getItemForAmmo(AmmoRegistry.ammoFireShot10), 5), false).isEmpty()) {
					fail("the catapult did not accept flame shot in its second slot");
				}
				//the player takes the stone shot out again
				for (int slot = 0; slot < vehicle.inventory.ammoInventory.getSlots(); slot++) {
					if (AmmoRegistry.getAmmoForStack(vehicle.inventory.ammoInventory.getStackInSlot(slot)).orElse(null) == AmmoRegistry.ammoStoneShot10) {
						vehicle.inventory.ammoInventory.extractItem(slot, 64, false);
					}
				}
				vehicle.ammoHelper.updateAmmoCounts();
				IAmmo selected = vehicle.ammoHelper.getCurrentAmmoType();
				if (improved && selected != AmmoRegistry.ammoFireShot10) {
					fail("the weapon did not fall back to the loaded flame shot: " + (selected == null ? "none" : selected.getRegistryName()));
				}
				if (!improved && selected != AmmoRegistry.ammoStoneShot10) {
					fail("switch off: the weapon left the empty type (1.12 stays on it): " + (selected == null ? "none" : selected.getRegistryName()));
				}
				if (improved && vehicle.ammoHelper.getCurrentAmmoCount() != 5) {
					fail("the count of the new selection is " + vehicle.ammoHelper.getCurrentAmmoCount());
				}
				vehicle.discard();
			}
		}
		finally {
			AWVehicleStatics.improvements.ammoFallback = saved;
			lane.release();
		}
		helper.succeed();
	}

	/*
	 * Improvement over 1.12 (config improvements.missile_hit_margin, default 0.5; 1.12: 0.3): a bolt that passes 0.4
	 * blocks beside a zombie's bounding box hits it with the default margin and misses with the 1.12 value.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 200)
	public static void improvementMissileHitMarginCatchesGrazingShots(GameTestHelper helper) {
		Lane lane = new Lane(helper, 1);
		double saved = AWVehicleStatics.improvements.missileHitMargin;
		Zombie[] targets = new Zombie[2];
		double[] margins = {0.3, 0.5};
		for (int i = 0; i < 2; i++) {
			//a husk: it does not burn in daylight
			Zombie zombie = new net.minecraft.world.entity.monster.Husk(net.minecraft.world.entity.EntityType.HUSK, lane.level);
			zombie.setNoAi(true);
			zombie.setPos(lane.x - 6 - i * 4, lane.y, lane.z);
			lane.level.addFreshEntity(zombie);
			targets[i] = zombie;
		}
		int[] ticks = {0};
		run(helper, lane, () -> {
			int t = ticks[0]++;
			//one bolt per target, flying west 0.4 blocks south of the target's box (half width 0.3), at chest height
			for (int i = 0; i < 2; i++) {
				if (t == 5 + i * 30) {
					AWVehicleStatics.improvements.missileHitMargin = margins[i];
					MissileBase bolt = new MissileBase(lane.level);
					bolt.setMissileParams2(AmmoRegistry.ammoBallistaBolt, (float) (targets[i].getX() + 2.5), (float) (lane.y + 1.0), (float) (lane.z + 0.3 + 0.4), 90, 0, 20);
					lane.level.addFreshEntity(bolt);
				}
				if (t == 5 + i * 30 + 15) {
					AWVehicleStatics.improvements.missileHitMargin = saved;
				}
			}
			if (t == 80) {
				AWVehicleStatics.improvements.missileHitMargin = saved;
				boolean missed = targets[0].getHealth() >= targets[0].getMaxHealth();
				boolean hit = targets[1].getHealth() < targets[1].getMaxHealth() || !targets[1].isAlive();
				if (!missed) {
					fail("with the 1.12 margin of 0.3 the bolt 0.4 blocks beside the target hit it");
				}
				if (!hit) {
					fail("with the margin of 0.5 the bolt 0.4 blocks beside the target missed it");
				}
				done(helper, lane);
			}
		});
	}

	/*
	 * Improvement over 1.12 (config improvements.crew_accuracy_per_level / crew_accuracy_max_reduction): the level of
	 * an NPC crew member reduces the scatter of the shot. Level 0 and the option at 0 leave the 1.12 scatter; level 10
	 * leaves half of it with the defaults; the cap holds.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 100)
	public static void improvementCrewLevelReducesScatter(GameTestHelper helper) {
		double perLevel = AWVehicleStatics.improvements.crewAccuracyPerLevel;
		double cap = AWVehicleStatics.improvements.crewAccuracyMaxReduction;
		try {
			AWVehicleStatics.improvements.crewAccuracyPerLevel = 0.067;
			AWVehicleStatics.improvements.crewAccuracyMaxReduction = 0.5;
			float level0 = net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringHelper.crewScatterFactor(0);
			float level5 = net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringHelper.crewScatterFactor(5);
			float level10 = net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringHelper.crewScatterFactor(10);
			float level50 = net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringHelper.crewScatterFactor(50);
			if (level0 != 1.f) {
				fail("a level 0 crew member changes the scatter: " + level0);
			}
			if (!(level5 < 0.75f && level5 > 0.66f) || Math.abs(level10 - 0.5f) > 0.01f || level50 != 0.5f) {
				fail("scatter left at level 5 / 10 / 50: " + level5 + " / " + level10 + " / " + level50 + " (expected about 0.71 / 0.5 / 0.5)");
			}
			AWVehicleStatics.improvements.crewAccuracyPerLevel = 0;
			if (net.shadowmage.ancientwarfare.vehicle.helpers.VehicleFiringHelper.crewScatterFactor(10) != 1.f) {
				fail("option at 0: a level 10 crew member still changes the scatter");
			}
		}
		finally {
			AWVehicleStatics.improvements.crewAccuracyPerLevel = perLevel;
			AWVehicleStatics.improvements.crewAccuracyMaxReduction = cap;
		}
		helper.succeed();
	}

	/*
	 * Improvement over 1.12 (config improvements.missile_hit_sweep): a bolt at 2.25 blocks per tick, shot at the
	 * centre of a standing target, hits it at every distance from 12 to 24 blocks (steps of 0.2). Both passes use the
	 * 1.12 margin of 0.3, so only the search differs. With the 1.12 search (entities around the end point of each
	 * step) a target standing 0.6 to 0.75 blocks behind an end point is neither reached in that tick nor found in the
	 * next one; those distances are logged and at least one has to exist, or the test proves nothing.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 4000)
	public static void improvementMissileHitTestCoversTheWholeStep(GameTestHelper helper) {
		Lane lane = new Lane(helper, 2);
		boolean savedSweep = AWVehicleStatics.improvements.missileHitSweep;
		double savedMargin = AWVehicleStatics.improvements.missileHitMargin;
		final int shots = 61;
		final int period = 16;
		List<String> missedOn = new ArrayList<>();
		List<String> missedOff = new ArrayList<>();
		Zombie[] target = new Zombie[1];
		MissileBase[] bolt = new MissileBase[1];
		//shot = which shot is being fired (first all with the whole step, then all with the 1.12 search), phase = tick of it
		int[] state = {0, 0};
		run(helper, lane, () -> {
			int shot = state[0];
			int phase = state[1]++;
			if (shot >= shots * 2) {
				AWVehicleStatics.improvements.missileHitSweep = savedSweep;
				AWVehicleStatics.improvements.missileHitMargin = savedMargin;
				net.shadowmage.ancientwarfare.AncientWarfare.LOG.info("improvementMissileHitTestCoversTheWholeStep: centre shots missed at distances {} with the 1.12 search, {} with the whole step", missedOff, missedOn);
				if (!missedOn.isEmpty()) {
					fail("with the whole step tested, centre shots missed at distances " + missedOn + " (1.12 search: " + missedOff + ")");
				}
				if (missedOff.isEmpty()) {
					fail("the 1.12 search missed no target either, so the test does not show the gap");
				}
				done(helper, lane);
				return;
			}
			boolean improved = shot < shots;
			double distance = 12 + (shot % shots) * 0.2;
			if (phase == 0) {
				Zombie husk = new net.minecraft.world.entity.monster.Husk(net.minecraft.world.entity.EntityType.HUSK, lane.level);
				husk.setNoAi(true);
				husk.setPos(lane.x - distance, lane.y, lane.z);
				lane.level.addFreshEntity(husk);
				target[0] = husk;
			}
			if (phase >= 2 && phase < period - 1) {
				//the options are statics shared with the other tests of the batch: set while this bolt flies
				AWVehicleStatics.improvements.missileHitSweep = improved;
				AWVehicleStatics.improvements.missileHitMargin = 0.3;
			}
			if (phase == 2) {
				//45 = 2.25 blocks per tick; launched level from above head height so that the drop keeps it inside the body
				bolt[0] = new MissileBase(lane.level);
				bolt[0].setMissileParams2(AmmoRegistry.ammoBallistaBolt, (float) lane.x, (float) (lane.y + 1.9), (float) lane.z, 90, 0, 45);
				lane.level.addFreshEntity(bolt[0]);
			}
			if (phase == period - 1) {
				boolean hit = !target[0].isAlive() || target[0].getHealth() < target[0].getMaxHealth();
				//entities of a lane do not always tick right after its chunks were loaded: a bolt that never moved is no shot
				boolean flew = hit || bolt[0].isRemoved() || bolt[0].getX() < lane.x - distance;
				target[0].discard();
				lane.missiles().forEach(MissileBase::discard);
				AWVehicleStatics.improvements.missileHitSweep = savedSweep;
				AWVehicleStatics.improvements.missileHitMargin = savedMargin;
				state[1] = 0;
				if (flew) {
					if (!hit) {
						(improved ? missedOn : missedOff).add(String.format(java.util.Locale.ROOT, "%.1f", distance));
					}
					state[0]++;
				}
			}
		});
	}

	/*
	 * ammo_fallback must not act in the middle of a firing sequence (user report: a hwacha loaded with three rocket
	 * types fired all of them with one trigger pull). One plain rocket and 20 flame rockets: the barrage fires the
	 * plain one and stops, as in 1.12; only then the flame rockets are selected. (A single rocket, because a larger
	 * stack never empties completely during a barrage: the kept 1.12 quirk leaves its last round unfired. Single
	 * leftover rockets are exactly what a player has after a few barrages.)
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 700)
	public static void improvementAmmoFallbackWaitsForTheEndOfABarrage(GameTestHelper helper) {
		Lane lane = new Lane(helper, 11);
		VehicleBase vehicle = lane.vehicle(VehicleRegistry.HWACHA, 0);
		load(vehicle, AmmoRegistry.ammoRocket, 1);
		if (!vehicle.inventory.ammoInventory.insertItem(1, new ItemStack(AmmoRegistry.getItemForAmmo(AmmoRegistry.ammoHwachaRocketFlame), 20), false).isEmpty()) {
			fail("the hwacha did not accept flame rockets in its second slot");
		}
		vehicle.ammoHelper.updateAmmoCounts();
		java.util.Set<MissileBase> seen = new java.util.HashSet<>();
		int[] counts = {0, 0};
		int[] ticks = {0};
		boolean[] started = {false};
		run(helper, lane, () -> {
			int t = ticks[0]++;
			if (!started[0] && vehicle.tickCount > 5 && vehicle.firingHelper.isReadyToFire()) {
				if (vehicle.ammoHelper.getCurrentAmmoType() != AmmoRegistry.ammoRocket) {
					fail("the plain rockets are not selected at the start");
				}
				vehicle.firingHelper.handleFireUpdate();
				started[0] = true;
			}
			for (MissileBase missile : lane.missiles()) {
				if (seen.add(missile)) {
					counts[missile.ammoType == AmmoRegistry.ammoRocket ? 0 : 1]++;
				}
			}
			if (started[0] && !vehicle.firingHelper.isInFiringSequence() && t > 60 || t > 600) {
				if (counts[1] > 0) {
					fail("the barrage went on with another rocket type: " + counts[0] + " plain and " + counts[1] + " other rockets from one trigger pull");
				}
				if (counts[0] != 1) {
					fail("the barrage fired " + counts[0] + " plain rockets, 1 was loaded");
				}
				if (vehicle.ammoHelper.getCurrentAmmoType() != AmmoRegistry.ammoHwachaRocketFlame) {
					fail("after the barrage the flame rockets are not selected");
				}
				done(helper, lane);
			}
		});
	}

	/*
	 * ammo_fallback on a barrage weapon, the case from the user's game: a hwacha with 5 flame rockets selected and 20
	 * plain rockets. The barrage fires 4 flame rockets (the kept 1.12 quirk holds the last round back) and nothing
	 * else; the weapon then reports 0 of the selected type, and once the barrage is over the plain rockets are
	 * selected with their count, so the next trigger pull fires them. With the switch off it stays on the flame
	 * rockets with a count of 0, as in 1.12.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 1500)
	public static void improvementAmmoFallbackAfterABarrage(GameTestHelper helper) {
		Lane lane = new Lane(helper, 11);
		boolean saved = AWVehicleStatics.improvements.ammoFallback;
		VehicleBase[] vehicle = new VehicleBase[1];
		java.util.Set<MissileBase> seen = new java.util.HashSet<>();
		int[] counts = {0, 0};
		//state: 0 = set up, 1 = waiting to fire, 2 = barrage running; pass 0 = switch off, 1 = on
		int[] state = {0, 0, 0};
		run(helper, lane, () -> {
			boolean improved = state[1] == 1;
			int t = state[2]++;
			if (state[0] == 0) {
				seen.clear();
				counts[0] = counts[1] = 0;
				vehicle[0] = lane.vehicle(VehicleRegistry.HWACHA, 0);
				load(vehicle[0], AmmoRegistry.ammoHwachaRocketFlame, 5);
				if (!vehicle[0].inventory.ammoInventory.insertItem(1, new ItemStack(AmmoRegistry.getItemForAmmo(AmmoRegistry.ammoRocket), 20), false).isEmpty()) {
					fail("the hwacha did not accept plain rockets in its second slot");
				}
				vehicle[0].ammoHelper.updateAmmoCounts();
				vehicle[0].ammoHelper.updateSelectedAmmo(AmmoRegistry.ammoHwachaRocketFlame.getRegistryName().toString());
				state[0] = 1;
				state[2] = 0;
				return;
			}
			AWVehicleStatics.improvements.ammoFallback = improved;
			if (state[0] == 1) {
				if (vehicle[0].tickCount > 5 && vehicle[0].firingHelper.isReadyToFire()) {
					vehicle[0].firingHelper.handleFireUpdate();
					state[0] = 2;
					state[2] = 0;
				} else if (t > 400) {
					AWVehicleStatics.improvements.ammoFallback = saved;
					fail("the hwacha never became ready to fire");
				}
				return;
			}
			for (MissileBase missile : lane.missiles()) {
				if (seen.add(missile)) {
					counts[missile.ammoType == AmmoRegistry.ammoHwachaRocketFlame ? 0 : 1]++;
				}
			}
			if ((!vehicle[0].firingHelper.isInFiringSequence() && t > 60) || t > 500) {
				AWVehicleStatics.improvements.ammoFallback = saved;
				IAmmo selected = vehicle[0].ammoHelper.getCurrentAmmoType();
				int count = vehicle[0].ammoHelper.getCurrentAmmoCount();
				String what = (improved ? "switch on: " : "switch off: ") + counts[0] + " flame and " + counts[1] + " other rockets fired, selected "
						+ (selected == null ? "none" : selected.getRegistryName()) + " count " + count;
				if (counts[0] != 4 || counts[1] != 0) {
					fail("the barrage is not the 1.12 one (4 flame rockets, the last one held back, nothing else). " + what);
				}
				if (improved && (selected != AmmoRegistry.ammoRocket || count != 20)) {
					fail("after the barrage the loaded plain rockets are not selected. " + what);
				}
				if (!improved && (selected != AmmoRegistry.ammoHwachaRocketFlame || count != 0)) {
					fail("the 1.12 behaviour is to stay on the empty type. " + what);
				}
				vehicle[0].discard();
				lane.missiles().forEach(MissileBase::discard);
				if (improved) {
					done(helper, lane);
				} else {
					state[0] = 0;
					state[1] = 1;
				}
			}
		});
	}

	/*
	 * Improvement over 1.12 (config improvements.crew_leads_targets): a husk walks across the line of fire of a
	 * ballista turret at 0.15 blocks per tick (3 blocks per second), 20 and 30 blocks away. The weapon is aimed the
	 * way the NPC crew AI aims it (IVehicle.leadTarget, handleSoldierTargetInput, isAimedAt) and fires one bolt
	 * without scatter. Aimed at where the husk will be, the bolt hits; aimed at where it is (1.12), it passes behind.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 3000)
	public static void improvementCrewLeadsMovingTargets(GameTestHelper helper) {
		Lane lane = new Lane(helper, 3, 1);
		boolean savedLead = AWVehicleStatics.improvements.crewLeadsTargets;
		boolean savedScatter = AWVehicleStatics.generalSettings.adjustMissilesForAccuracy;
		final double[] distances = {20, 20, 30, 30};
		final boolean[] leading = {false, true, false, true};
		List<String> results = new ArrayList<>();
		VehicleBase[] vehicle = new VehicleBase[1];
		Zombie[] husk = new Zombie[1];
		net.shadowmage.ancientwarfare.npc.entity.vehicle.ITarget[] target = new net.shadowmage.ancientwarfare.npc.entity.vehicle.ITarget[1];
		//pass, tick of the pass, tick the bolt was fired at (-1: not yet)
		int[] state = {0, 0, -1};
		run(helper, lane, () -> {
			int pass = state[0];
			if (pass >= distances.length) {
				AWVehicleStatics.improvements.crewLeadsTargets = savedLead;
				AWVehicleStatics.generalSettings.adjustMissilesForAccuracy = savedScatter;
				net.shadowmage.ancientwarfare.AncientWarfare.LOG.info("improvementCrewLeadsMovingTargets: {}", results);
				for (int i = 0; i < distances.length; i++) {
					boolean hit = results.get(i).endsWith("hit");
					if (hit != leading[i]) {
						fail("moving target: expected a miss when aiming at the target and a hit when leading it, got " + results);
					}
				}
				done(helper, lane);
				return;
			}
			int t = state[1]++;
			AWVehicleStatics.improvements.crewLeadsTargets = leading[pass];
			AWVehicleStatics.generalSettings.adjustMissilesForAccuracy = false;
			if (t == 0) {
				vehicle[0] = lane.vehicle(VehicleRegistry.BALLISTA_STAND_TURRET, 0);
				load(vehicle[0], AmmoRegistry.ammoBallistaBolt, 10);
				vehicle[0].ammoHelper.updateAmmoCounts();
				husk[0] = new net.minecraft.world.entity.monster.Husk(net.minecraft.world.entity.EntityType.HUSK, lane.level);
				husk[0].setNoAi(true);
				husk[0].setPos(lane.x - distances[pass], lane.y, lane.z - 4);
				lane.level.addFreshEntity(husk[0]);
				target[0] = new net.shadowmage.ancientwarfare.npc.entity.vehicle.EntityTarget(husk[0]);
				state[2] = -1;
				return;
			}
			//the walk starts once the lane ticks (the weapon's tick count moves)
			if (vehicle[0].tickCount < 5) {
				state[1] = 1;
				return;
			}
			husk[0].setPos(husk[0].getX(), husk[0].getY(), husk[0].getZ() + 0.15);
			net.shadowmage.ancientwarfare.npc.entity.vehicle.ITarget aim = vehicle[0].leadTarget(target[0]);
			if (state[2] < 0) {
				vehicle[0].handleSoldierTargetInput(aim.getX(), aim.getY(), aim.getZ());
				if (t > 30 && vehicle[0].isAimedAt(aim) && vehicle[0].firingHelper.isReadyToFire()) {
					vehicle[0].firingHelper.handleFireUpdate();
					state[2] = t;
				} else if (t > 170) {
					AWVehicleStatics.improvements.crewLeadsTargets = savedLead;
					AWVehicleStatics.generalSettings.adjustMissilesForAccuracy = savedScatter;
					fail("pass " + pass + ": the weapon was never aimed at the " + (leading[pass] ? "predicted point" : "target") + " (results so far " + results + ")");
				}
				return;
			}
			if (t > state[2] + 60) {
				boolean hit = !husk[0].isAlive() || husk[0].getHealth() < husk[0].getMaxHealth();
				results.add((int) distances[pass] + " blocks " + (leading[pass] ? "leading" : "1.12 aim") + ": " + (hit ? "hit" : "miss"));
				vehicle[0].discard();
				husk[0].discard();
				lane.missiles().forEach(MissileBase::discard);
				state[0]++;
				state[1] = 0;
			}
		});
	}

	/*
	 * crew_leads_targets with a real siege engineer on a ballista turret (tester #73: the first test drove the calls
	 * without an NPC). A husk walks back and forth across the line of fire 30 blocks away at a zombie's walking speed
	 * (0.115 blocks per tick), healed after every hit. Without scatter, the same time with the option off and on;
	 * the improved crew has to hit with at least half of its bolts and more often than the 1.12 aim.
	 */
	@GameTest(templateNamespace = AncientWarfare.MOD_ID, template = TEMPLATE, timeoutTicks = 6000)
	public static void improvementCrewLeadsWalkingTargetWithRealEngineer(GameTestHelper helper) {
		boolean savedLead = AWVehicleStatics.improvements.crewLeadsTargets;
		boolean savedScatter = AWVehicleStatics.generalSettings.adjustMissilesForAccuracy;
		Lane lane = new Lane(helper, 3, 2);
		final int passTicks = 1600;
		int[] hits = {0, 0};
		int[] shots = {0, 0};
		Object[] state = {null, null, null};// vehicle, engineer, husk
		int[] t = {0};
		java.util.Set<MissileBase> seen = new java.util.HashSet<>();
		run(helper, lane, () -> {
			int tick = t[0]++;
			int pass = tick / passTicks;
			int inPass = tick % passTicks;
			if (pass >= 2) {
				AWVehicleStatics.improvements.crewLeadsTargets = savedLead;
				AWVehicleStatics.generalSettings.adjustMissilesForAccuracy = savedScatter;
				net.shadowmage.ancientwarfare.AncientWarfare.LOG.info("improvementCrewLeadsWalkingTargetWithRealEngineer: 1.12 aim {} hits / {} bolts, leading {} hits / {} bolts", hits[0], shots[0], hits[1], shots[1]);
				//measured: 1.12 aim 0 of 10, leading 9 of 9 (before the fix in NpcSiegeEngineer.setTarget: 1 of 10)
				if (hits[1] * 2 < shots[1] || hits[1] <= hits[0]) {
					fail("a real crew leading a walking target hit " + hits[1] + " of " + shots[1] + " times, aiming at it " + hits[0] + " of " + shots[0]);
				}
				done(helper, lane);
				return;
			}
			AWVehicleStatics.improvements.crewLeadsTargets = pass == 1;
			AWVehicleStatics.generalSettings.adjustMissilesForAccuracy = false;
			if (inPass == 0) {
				if (state[0] != null) {
					((VehicleBase) state[0]).discard();
					((NpcBase) state[1]).discard();
					((Zombie) state[2]).discard();
					lane.missiles().forEach(MissileBase::discard);
				}
				VehicleBase vehicle = lane.vehicle(VehicleRegistry.BALLISTA_STAND_TURRET, 1);
				load(vehicle, AmmoRegistry.ammoBallistaBolt, 64);
				NpcSiegeEngineer engineer = (NpcSiegeEngineer) AWNPCEntities.createNpc(lane.level, "siege_engineer", "", "");
				engineer.setOwnerName(OWNER);
				engineer.setFoodRemaining(6000);
				engineer.moveTo(lane.x + 4, lane.y, lane.z + 3, 0, 0);
				engineer.setHomeAreaAtCurrentPosition();
				lane.level.addFreshEntity(engineer);
				Zombie husk = EntityType.HUSK.create(lane.level);
				husk.moveTo(lane.x - 30, lane.y, lane.z + 4, 0, 0);
				husk.setNoAi(true);
				husk.setPersistenceRequired();
				husk.setSilent(true);
				lane.level.addFreshEntity(husk);
				state[0] = vehicle;
				state[1] = engineer;
				state[2] = husk;
				return;
			}
			Zombie husk = (Zombie) state[2];
			//back and forth between z + 4 and z + 28 at 0.115 blocks per tick
			int period = (int) (24 / 0.115);
			int phase = inPass % (2 * period);
			double z = lane.z + 4 + 0.115 * (phase < period ? phase : 2 * period - phase);
			husk.setPos(husk.getX(), husk.getY(), z);
			if (husk.getHealth() < husk.getMaxHealth() || !husk.isAlive()) {
				hits[pass]++;
				husk.setHealth(husk.getMaxHealth());
			}
			for (MissileBase missile : lane.missiles()) {
				if (seen.add(missile)) {
					shots[pass]++;
				}
			}
		});
	}
}
