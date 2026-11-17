package net.shadowmage.ancientwarfare.npc.entity.vehicle;

import net.minecraft.world.entity.EntityType;
import net.shadowmage.ancientwarfare.npc.xmod.IVehicle;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.npc.ai.RestrictOpenDoorGoal;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIAttackNearest;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIDoor;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIFollowPlayer;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIHurt;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWander;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWatchClosest;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIOwnerHurtByTarget;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIOwnerHurtTarget;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedAlarmResponse;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedCommander;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedFollowCommand;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedGetFood;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedIdleWhenHungry;
import net.shadowmage.ancientwarfare.npc.ai.vehicle.NpcAIAimVehicle;
import net.shadowmage.ancientwarfare.npc.ai.vehicle.NpcAIDismountVehicle;
import net.shadowmage.ancientwarfare.npc.ai.vehicle.NpcAIFindVehicle;
import net.shadowmage.ancientwarfare.npc.ai.vehicle.NpcAIFireVehicle;
import net.shadowmage.ancientwarfare.npc.ai.vehicle.NpcAIMountVehicle;
import net.shadowmage.ancientwarfare.npc.entity.NpcPlayerOwned;
import net.shadowmage.ancientwarfare.npc.npc_command.NpcCommand;

import javax.annotation.Nullable;
import java.util.Optional;

@SuppressWarnings({"squid:MaximumInheritanceDepth", "squid:S2160"})
public class NpcSiegeEngineer extends NpcPlayerOwned implements IVehicleUser {
	private IVehicle vehicle = null;
	private ITarget target = TargetFactory.NONE;

	@Override
	public Optional<ITarget> getVehicleTarget() {
		return target == TargetFactory.NONE ? Optional.empty() : Optional.of(target);
	}

	private void setTarget(BlockPos pos) {
		target = new BlockPosTarget(pos);
	}

	@Override
	public void resetTarget() {
		target = TargetFactory.NONE;
	}

	@Override
	public void setTarget(LivingEntity entity) {
		super.setTarget(entity);
		if (entity != null) {
			//keep the target object while it is the same entity: it measures the target's speed over the last ticks
			//(improvement crew_leads_targets); the target goals call setTarget again and again for the same entity
			if (!(target instanceof EntityTarget current && current.isFor(entity))) {
				target = new EntityTarget(entity);
			}
		} else {
			resetTarget();
		}
	}

	public NpcSiegeEngineer(EntityType<? extends NpcSiegeEngineer> type, Level world) {
		super(type, world);

		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(0, new RestrictOpenDoorGoal(this));
		this.goalSelector.addGoal(0, new NpcAIDoor(this, true));
		this.goalSelector.addGoal(2, new NpcAIFollowPlayer(this));
		this.goalSelector.addGoal(2, new NpcAIPlayerOwnedFollowCommand(this));
		this.goalSelector.addGoal(3, new NpcAIPlayerOwnedAlarmResponse(this));
		this.goalSelector.addGoal(4, new NpcAIPlayerOwnedGetFood(this));
		this.goalSelector.addGoal(5, new NpcAIPlayerOwnedIdleWhenHungry(this));
		this.goalSelector.addGoal(6, new NpcAIDismountVehicle<>(this));
		this.goalSelector.addGoal(6, new NpcAIFindVehicle<>(this));
		this.goalSelector.addGoal(7, new NpcAIMountVehicle<>(this));
		this.goalSelector.addGoal(8, new NpcAIAimVehicle<>(this));
		this.goalSelector.addGoal(9, new NpcAIFireVehicle<>(this));

		this.goalSelector.addGoal(101, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		this.goalSelector.addGoal(102, new NpcAIWander(this) {
			@Override
			public boolean canUse() {
				return !isRidingVehicle() && super.canUse();
			}
		});
		this.goalSelector.addGoal(103, new NpcAIWatchClosest(this, Mob.class, 8.0F));

		targetSelector.addGoal(0, new NpcAIPlayerOwnedCommander(this));
		targetSelector.addGoal(1, new NpcAIOwnerHurtByTarget(this));
		targetSelector.addGoal(2, new NpcAIOwnerHurtTarget(this));
		targetSelector.addGoal(3, new NpcAIHurt(this));
		targetSelector.addGoal(4, new NpcAIAttackNearest(this, this::isHostileTowards));
	}

	/* 1.12 applyEntityAttributes */
	public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
		return net.shadowmage.ancientwarfare.npc.entity.NpcBase.createAttributes().add(Attributes.FOLLOW_RANGE, 120D);
	}

	@Override
	public String getNpcSubType() {
		return "";
	}

	@Override
	public String getNpcType() {
		return "siege_engineer";
	}

	@Override
	public boolean canBeAttackedBy(Entity e) {
		return true;
	}

	@Override
	public Optional<IVehicle> getSiegeVehicle() {
		if (vehicle == null || vehicle.asEntity().isRemoved()) {
			return Optional.empty();
		}
		return Optional.of(vehicle);
	}

	@Override
	public void setSiegeVehicle(IVehicle vehicle) {
		this.vehicle = vehicle;
	}

	@Override
	public void resetVehicle() {
		vehicle = null;
	}

	@Override
	public boolean isRidingVehicle() {
		return getSiegeVehicle().isPresent() && isPassenger();
	}

	@Override
	public boolean canContinueRidingVehicle() {
		return getFoodRemaining() > 0;
	}

	@Override
	public void handlePlayerCommand(NpcCommand.Command cmd) {
		if (cmd.type == NpcCommand.CommandType.ATTACK_AREA) {
			setTarget(cmd.pos);
		} else if (cmd.type == NpcCommand.CommandType.CLEAR_COMMAND) {
			resetTarget();
		} else {
			super.handlePlayerCommand(cmd);
		}
	}

	@Override
	public void tick() {
		super.tick();

		if (tickCount % 20 == 0) {
			checkTargetExistence();
		}
	}

	private void checkTargetExistence() {
		if (target != TargetFactory.NONE && !target.exists(level())) {
			target = TargetFactory.NONE;
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		TargetFactory.serializeNBT(target, tag);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		target = TargetFactory.deserializeFromNBT(tag);
	}
}
