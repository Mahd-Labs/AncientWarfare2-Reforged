package net.shadowmage.ancientwarfare.npc.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.npc.ai.RestrictOpenDoorGoal;
import com.google.common.base.Predicate;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIAttackMeleeLongRange;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIAttackNearest;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIDistressResponse;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIDoor;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIFollowPlayer;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIHurt;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIMedicBase;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIMoveHome;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWander;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWatchClosest;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIOwnerHurtByTarget;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIOwnerHurtTarget;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedAlarmResponse;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedAttackRanged;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedCommander;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedFollowCommand;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedGetFood;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedIdleWhenHungry;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedPatrol;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedRideHorse;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.item.ItemCombatOrder;
import net.shadowmage.ancientwarfare.npc.item.ItemCommandBaton;

import java.util.Collection;

@SuppressWarnings({"squid:MaximumInheritanceDepth", "squid:S2160"})
public class NpcCombat extends NpcPlayerOwned implements RangedAttackMob {
	private static final String PATROL_AI_TAG = "patrolAI";
	private final NpcAIAttackMeleeLongRange meleeAI;
	private final Goal arrowAI;
	private final NpcAIPlayerOwnedPatrol patrolAI;

	private NpcBase distressedTarget;

	@SuppressWarnings("squid:S4738")
	public NpcCombat(EntityType<? extends NpcCombat> type, Level par1World) {
		super(type, par1World);
		meleeAI = new NpcAIAttackMeleeLongRange(this);
		arrowAI = new NpcAIPlayerOwnedAttackRanged(this);
		horseAI = new NpcAIPlayerOwnedRideHorse(this);
		patrolAI = new NpcAIPlayerOwnedPatrol(this);

		//noinspection Guava
		Predicate<Entity> selector = this::isHostileTowards;

		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(0, new RestrictOpenDoorGoal(this));
		goalSelector.addGoal(0, new NpcAIDoor(this, true));
		goalSelector.addGoal(0, horseAI);
		goalSelector.addGoal(1, new NpcAIPlayerOwnedFollowCommand(this));
		goalSelector.addGoal(2, new NpcAIPlayerOwnedGetFood(this));
		goalSelector.addGoal(3, new NpcAIPlayerOwnedIdleWhenHungry(this));

		//3==NpcAIShieldBlock
		goalSelector.addGoal(5, new NpcAIFollowPlayer(this));
		goalSelector.addGoal(6, new NpcAIPlayerOwnedAlarmResponse(this));

		//6--empty....
		//7==combat task, inserted from onweaponinventoryupdated
		goalSelector.addGoal(8, new NpcAIMedicBase<NpcCombat>(this));
		goalSelector.addGoal(8, new NpcAIDistressResponse(this));
		goalSelector.addGoal(9, patrolAI);

		goalSelector.addGoal(10, new NpcAIMoveHome(this, 50F, 5F, 20F, 5F));

		//post-100 -- used by delayed shared tasks (look at random stuff, wander)
		goalSelector.addGoal(101, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		goalSelector.addGoal(102, new NpcAIWander(this));
		goalSelector.addGoal(103, new NpcAIWatchClosest(this, Mob.class, 8.0F));

		targetSelector.addGoal(0, new NpcAIPlayerOwnedCommander(this));
		targetSelector.addGoal(1, new NpcAIOwnerHurtByTarget(this));
		targetSelector.addGoal(2, new NpcAIOwnerHurtTarget(this));
		targetSelector.addGoal(3, new NpcAIHurt(this));
		targetSelector.addGoal(4, new NpcAIAttackNearest(this, selector));

		setCanPickUpLoot(true);
	}

	@Override
	public boolean canHoldItem(ItemStack stack) {
		return getMainHandItem().isEmpty() || getMainHandItem().getItem() == stack.getItem();
	}

	@Override
	public boolean isValidOrdersStack(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof ItemCombatOrder;
	}

	@Override
	public void onOrdersInventoryChanged() {
		patrolAI.onOrdersInventoryChanged();
	}

	@Override
	public void onWeaponInventoryChanged() {
		super.onWeaponInventoryChanged();
		if (!level().isClientSide) {
			goalSelector.removeGoal(arrowAI);
			goalSelector.removeGoal(meleeAI);
			ItemStack stack = getMainHandItem();
			if (isBow(stack.getItem())) {
				goalSelector.addGoal(4, arrowAI);
			} else {
				goalSelector.addGoal(4, meleeAI);
			}
			meleeAI.setAttackReachFromWeapon(getMainHandItem());
		}
	}

	@Override
	protected boolean canAttackFlying() {
		return isBow(getMainHandItem().getItem());
	}

	@Override
	public boolean worksInRain() {
		return true;
	}

	@Override
	public boolean isPassive() {
		return false;
	}

	@Override
	public boolean shouldSleep() {
		return false;
	}

	@Override
	public String getNpcSubType() {
		return getSubtypeFromEquipment();
	}

	private String getSubtypeFromEquipment() {
		ItemStack stack = getMainHandItem();
		if (!stack.isEmpty()) {
			Item item = stack.getItem();
			Collection<String> tools = NpcBase.getToolClasses(stack);
			if (tools.contains("axe")) {
				return "medic";
			} else if (tools.contains("hammer")) {
				return "engineer";
			}
			if (isBow(item)) {
				return "archer";
			} else if (item instanceof ItemCommandBaton) {
				return "commander";
			} else if (item.isEnchantable(stack)) {
				return "soldier";
			}
		}
		return "";
	}

	@Override
	public String getNpcType() {
		return "combat";
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		onWeaponInventoryChanged();
		onOffhandInventoryChanged();
		if (tag.contains(PATROL_AI_TAG)) {
			patrolAI.readFromNBT(tag.getCompound(PATROL_AI_TAG));
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.put(PATROL_AI_TAG, patrolAI.writeToNBT(new CompoundTag()));
	}

	@Override
	public void performRangedAttack(LivingEntity target, float force) {
		// minimum inaccuracy = 3.0f, slowly reaches 0 (or close to it) as the NPC reaches max level
		float inaccuracy = 3.0f - ((float) Math.sqrt(getLevelingStats().getBaseLevel()) / (float) Math.sqrt(AWNPCStatics.maxNpcCombatLevel) * 3.0f);
		RangeAttackHelper.doRangedAttack(this, target, force, inaccuracy);
	}

	public void respondToDistress(NpcBase source) {
		// TODO: Target prioritizing or something...?
		distressedTarget = source;
	}

	public NpcBase getDistressedTarget() {
		return distressedTarget;
	}

	public void clearDistress() {
		distressedTarget = null;
	}
}
