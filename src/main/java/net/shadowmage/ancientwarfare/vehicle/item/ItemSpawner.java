package net.shadowmage.ancientwarfare.vehicle.item;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.util.ItemTools;
import net.shadowmage.ancientwarfare.vehicle.AncientWarfareVehicles;
import net.shadowmage.ancientwarfare.vehicle.config.AWVehicleStatics;
import net.shadowmage.ancientwarfare.vehicle.entity.IVehicleType;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.entity.types.VehicleType;

import java.util.List;
import java.util.Optional;

/*
 * Port notes:
 * - Flattened: 1.12 "spawner" with metadata = vehicle type number -> one item per ported type,
 *   "spawner_<config name>" (spawner_catapult_stand, spawner_ballista_stand_turret, ...). The material level stays
 *   in the stack data: custom data {spawnData:{level:n[,health:f]}} as in 1.12. The item model picks the texture by
 *   the item property "ancientwarfare:level" (1.12: custom mesh definition "<config name>_<level>").
 * - A stack without spawnData is level 0 (1.12 logged an error and did nothing); a plain stack from /give or a
 *   recipe book therefore still works.
 * - rayTrace(world, player, true) -> getPlayerPOVHitResult(SOURCE_ONLY) (1.12 flag "useLiquids").
 */
public class ItemSpawner extends ItemBaseVehicle {
	public static final String LEVEL_TAG = "level";
	private static final String HEALTH_TAG = "health";
	public static final String SPAWN_DATA_TAG = "spawnData";

	private final int vehicleTypeNum;

	public ItemSpawner(String regName, int vehicleTypeNum) {
		super(regName, new Item.Properties().stacksTo(1));
		this.vehicleTypeNum = vehicleTypeNum;
	}

	public int getVehicleTypeNum() {
		return vehicleTypeNum;
	}

	public static int getLevel(ItemStack stack) {
		return ItemTools.getTag(stack).getCompound(SPAWN_DATA_TAG).getInt(LEVEL_TAG);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (world.isClientSide) {
			return InteractionResultHolder.success(stack);
		}
		if (stack.isEmpty()) {
			return InteractionResultHolder.fail(stack);
		}
		if (rayTraceAndSpawnVehicle(world, player, hand, stack)) {
			return InteractionResultHolder.fail(stack);
		}
		return InteractionResultHolder.success(stack);
	}

	private boolean rayTraceAndSpawnVehicle(Level world, Player player, InteractionHand hand, ItemStack stack) {
		CompoundTag tag = ItemTools.getTag(stack).getCompound(SPAWN_DATA_TAG);
		int level = tag.getInt(LEVEL_TAG);
		Optional<VehicleBase> v = VehicleType.getVehicleForType(world, vehicleTypeNum, level);
		if (!v.isPresent()) {
			return true;
		}
		VehicleBase vehicle = v.get();
		if (tag.contains(HEALTH_TAG)) {
			vehicle.setHealth(tag.getFloat(HEALTH_TAG));
		}
		BlockHitResult rayTrace = getPlayerPOVHitResult(world, player, ClipContext.Fluid.SOURCE_ONLY);
		if (rayTrace.getType() != HitResult.Type.BLOCK) {
			return true;
		}
		spawnVehicle(world, player, vehicle, rayTrace);
		updateSpawnerStackCount(player, hand, stack);
		return false;
	}

	private void updateSpawnerStackCount(Player player, InteractionHand hand, ItemStack stack) {
		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
			if (stack.getCount() <= 0) {
				player.setItemInHand(hand, ItemStack.EMPTY);
			}
		}
	}

	private void spawnVehicle(Level world, Player player, VehicleBase vehicle, BlockHitResult rayTrace) {
		Vec3 hitVec = rayTrace.getLocation();
		if (rayTrace.getDirection().getAxis().isHorizontal()) {
			Vec3i dirVec = rayTrace.getDirection().getNormal();
			float halfWidth = vehicle.getBbWidth() / 2f;
			hitVec = hitVec.add(dirVec.getX() * halfWidth, 0, dirVec.getZ() * halfWidth);
		}
		vehicle.setPos(hitVec.x, hitVec.y, hitVec.z);
		vehicle.setYRot(-player.getYRot() + 180);
		vehicle.yRotO = vehicle.getYRot();
		vehicle.localTurretDestRot = vehicle.localTurretRotation = vehicle.localTurretRotationHome = vehicle.getYRot();
		if (AWVehicleStatics.generalSettings.useVehicleSetupTime) {
			vehicle.setSetupState(true, 100);
		}
		world.addFreshEntity(vehicle);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
		CompoundTag tag = ItemTools.getTag(stack).getCompound(SPAWN_DATA_TAG);
		int level = tag.getInt(LEVEL_TAG);
		tooltip.add(Component.literal("Material Level: " + level));//TODO additional translations
		if (tag.contains(HEALTH_TAG)) {
			tooltip.add(Component.literal("Vehicle Health: " + tag.getFloat(HEALTH_TAG)));
		}
		IVehicleType type = VehicleType.getVehicleType(vehicleTypeNum);
		if (type == null) {
			return;
		}
		type.getDisplayTooltip().stream().distinct().forEach(line -> tooltip.add(Component.translatable(line)));
	}

	@Override
	public String getDescriptionId() {
		IVehicleType vehicle = VehicleType.getVehicleType(vehicleTypeNum);
		return vehicle == null ? "" : vehicle.getDisplayName() + ".name";
	}

	@Override
	public String getDescriptionId(ItemStack stack) {
		return getDescriptionId();
	}
}
