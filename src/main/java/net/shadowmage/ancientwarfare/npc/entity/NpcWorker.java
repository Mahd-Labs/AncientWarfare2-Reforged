package net.shadowmage.ancientwarfare.npc.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.npc.ai.RestrictOpenDoorGoal;
import net.shadowmage.ancientwarfare.core.interfaces.IWorkSite;
import net.shadowmage.ancientwarfare.core.interfaces.IWorkSite.WorkType;
import net.shadowmage.ancientwarfare.core.interfaces.IWorker;
import net.shadowmage.ancientwarfare.core.item.ItemHammer;
import net.shadowmage.ancientwarfare.core.item.ItemQuill;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIDoor;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIFleeHostiles;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIFollowPlayer;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIMoveHome;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIWander;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedAlarmResponse;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedFindWorksite;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedFollowCommand;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedGetFood;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedIdleWhenHungry;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedRideHorse;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedWork;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedWorkRandom;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.item.ItemWorkOrder;
import net.shadowmage.ancientwarfare.npc.orders.WorkOrder;

import javax.annotation.Nonnull;
import java.util.Collection;

public class NpcWorker extends NpcPlayerOwned implements IWorker {

	public BlockPos autoWorkTarget;
	//improvement over 1.12 (config improvements.skip_unreachable_worksites): a worksite given up as unreachable, not saved
	public BlockPos unreachableWorkTarget;
	public int unreachableWorkTargetUntil;
	private NpcAIPlayerOwnedWork workAI;
	private NpcAIPlayerOwnedWorkRandom workRandomAI;

	public NpcWorker(EntityType<? extends NpcWorker> type, Level par1World) {
		super(type, par1World);
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(0, new RestrictOpenDoorGoal(this));
		this.goalSelector.addGoal(0, new NpcAIDoor(this, true));
		this.goalSelector.addGoal(0, (horseAI = new NpcAIPlayerOwnedRideHorse(this)));
		this.goalSelector.addGoal(2, new NpcAIFollowPlayer(this));
		this.goalSelector.addGoal(2, new NpcAIPlayerOwnedFollowCommand(this));
		this.goalSelector.addGoal(3, new NpcAIFleeHostiles(this));
		this.goalSelector.addGoal(3, new NpcAIPlayerOwnedAlarmResponse(this));
		this.goalSelector.addGoal(4, new NpcAIPlayerOwnedGetFood(this));
		this.goalSelector.addGoal(5, new NpcAIPlayerOwnedIdleWhenHungry(this));
		this.goalSelector.addGoal(6, (workAI = new NpcAIPlayerOwnedWork(this)));
		this.goalSelector.addGoal(7, (workRandomAI = new NpcAIPlayerOwnedWorkRandom(this)));
		this.goalSelector.addGoal(8, new NpcAIMoveHome(this, 50F, 3F, 30F, 3F));

		//post-100 -- used by delayed shared tasks (look at random stuff, wander)
		this.goalSelector.addGoal(101, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		this.goalSelector.addGoal(102, new NpcAIWander(this));
		this.goalSelector.addGoal(103, new LookAtPlayerGoal(this, Mob.class, 8.0F));

		this.targetSelector.addGoal(0, new NpcAIPlayerOwnedFindWorksite(this));
	}

	@Override
	public String getNpcSubType() {
		WorkType type = getWorkTypeFromEquipment();
		switch (type) {
			case CRAFTING:
				return "craftsman";
			case FARMING:
				return "farmer";
			case FORESTRY:
				return "lumberjack";
			case MINING:
				return "miner";
			case RESEARCH:
				return "researcher";
			case NONE:
			default:
				return "";
		}
	}

	public void handleWorksiteBroadcast(IWorkSite site, BlockPos pos) {

	}

	@Override
	public String getNpcType() {
		return "worker";
	}

	@Override
	public float getWorkEffectiveness(WorkType type) {
		if (canWorkAt(type)) {
			float effectiveness = 1.f + this.getLevelingStats().getLevel() * 0.05F;

			Item item = getMainHandItem().getItem();
			//Port notes: 1.12 ItemTool and ItemHoe branches; both are TieredItem in 1.21 (same efficiency numbers)
			if (item instanceof net.minecraft.world.item.TieredItem) {
				effectiveness += ((net.minecraft.world.item.TieredItem) item).getTier().getSpeed() * 0.05f;
			} else if (item instanceof ItemHammer) {
				effectiveness += ((ItemHammer) item).getMaterial().getSpeed() * 0.05f;
			} else if (item instanceof ItemQuill) {
				effectiveness += ((ItemQuill) item).getMaterial().getSpeed() * 0.05f;
			}
			return effectiveness;
		}
		return 0.F;
	}

	@Override
	public boolean shouldSleep() {
		WorkOrder order = WorkOrder.getWorkOrder(ordersStack);
		if (order == null || !order.isNightShift()) {
			return super.shouldSleep();
		}

		return WorldTools.isDaytimeInDimension(level());
	}

	@Override
	public double getWorkRangeSq() {
		return AWNPCStatics.npcActionRange * AWNPCStatics.npcActionRange;
	}

	@Override
	public boolean canWorkAt(WorkType type) {
		return type == getWorkTypeFromEquipment();
	}

	@Override
	public boolean isValidOrdersStack(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof ItemWorkOrder;
	}

	protected WorkType getWorkTypeFromEquipment() {
		ItemStack stack = getMainHandItem();
		if (!stack.isEmpty()) {
			if (stack.getItem() instanceof HoeItem) {
				return WorkType.FARMING;
			} else {
				Collection<String> tools = NpcBase.getToolClasses(stack);
				if (tools.contains("axe")) {
					return WorkType.FORESTRY;
				} else if (tools.contains("pickaxe")) {
					return WorkType.MINING;
				} else if (tools.contains("hammer")) {
					return WorkType.CRAFTING;
				} else if (tools.contains("quill")) {
					return WorkType.RESEARCH;
				}
			}
		}
		return WorkType.NONE;
	}

	@Override
	public void onOrdersInventoryChanged() {
		this.workAI.onOrdersChanged();
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("workAI")) {
			workAI.readFromNBT(tag.getCompound("workAI"));
		}
		if (tag.contains("workRandomAI")) {
			workRandomAI.readFromNBT(tag.getCompound("workRandomAI"));
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.put("workAI", workAI.writeToNBT(new CompoundTag()));
		tag.put("workRandomAI", workRandomAI.writeToNBT(new CompoundTag()));
	}

}
