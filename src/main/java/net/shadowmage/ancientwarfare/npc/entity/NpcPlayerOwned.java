package net.shadowmage.ancientwarfare.npc.entity;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;
import net.shadowmage.ancientwarfare.npc.ai.AIHelper;
import net.shadowmage.ancientwarfare.npc.ai.owned.NpcAIPlayerOwnedRideHorse;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.npc.init.AWNPCSounds;
import net.shadowmage.ancientwarfare.npc.npc_command.NpcCommand.Command;
import net.shadowmage.ancientwarfare.npc.npc_command.NpcCommand.CommandType;
import net.shadowmage.ancientwarfare.npc.orders.UpkeepOrder;
import net.shadowmage.ancientwarfare.npc.registry.NpcDefault;
import net.shadowmage.ancientwarfare.npc.registry.NpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.npc.tile.TileTownHall;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@SuppressWarnings({"squid:MaximumInheritanceDepth", "squid:S2160"})
public abstract class NpcPlayerOwned extends NpcBase implements IKeepFood {
	private static final String COMMAND_TAG = "command";
	private static final String TOWN_HALL_TAG = "townHall";
	private static final String UPKEEP_POS_TAG = "upkeepPos";
	public boolean isAlarmed = false;

	private Command playerIssuedCommand = Command.NONE;
	private int foodValueRemaining = 0;

	NpcAIPlayerOwnedRideHorse horseAI;

	private BlockPos townHallPosition;
	private BlockPos upkeepAutoBlock;

	public NpcPlayerOwned(EntityType<? extends NpcPlayerOwned> type, Level par1World) {
		super(type, par1World);
		NpcDefault npcDefault = NpcDefaultsRegistry.getOwnedNpcDefault(this);
		npcDefault.applyPathSettings((GroundPathNavigation) getNavigation());
		npcDefault.applyAttributes(this);
		java.util.Arrays.fill(armorDropChances, 1.f);
		java.util.Arrays.fill(handDropChances, 1.f);
	}

	@Override
	public int getMaxFallDistance() {
		return super.getMaxFallDistance() - 1;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return isFemale() || getSkinSettings().isAlexModel() ? AWNPCSounds.HUMAN_FEMALE_HURT.get() : AWNPCSounds.HUMAN_HURT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return isFemale() || getSkinSettings().isAlexModel() ? AWNPCSounds.HUMAN_FEMALE_DEATH.get() : AWNPCSounds.HUMAN_HURT.get();
	}

	@Override
	public void die(DamageSource source) {
		if (!level().isClientSide) {
			if (horseAI != null) {
				horseAI.onKilled();
			}
			validateTownHallPosition();
			getTownHall().ifPresent(townHall -> townHall.handleNpcDeath(this, source));
		}
		super.die(source);
	}

	@Override
	protected void onRepack() {
		super.onRepack();
		if (horseAI != null) {
			horseAI.onKilled();
		}
	}

	@Override
	public final int getArmorValueOverride() {
		return -1;
	}

	@Override
	public final int getAttackDamageOverride() {
		return -1;
	}

	private void setTownHallPosition(BlockPos pos) {
		townHallPosition = pos;
	}

	@Override
	public Optional<BlockPos> getTownHallPosition() {
		return Optional.ofNullable(townHallPosition);
	}

	public Optional<TileTownHall> getTownHall() {
		return getTownHallPosition().flatMap(p -> WorldTools.getTile(level(), p, TileTownHall.class));
	}

	public void handleTownHallBroadcast(BlockPos position) {
		validateTownHallPosition();
		Optional<BlockPos> townHallPos = getTownHallPosition();
		if (townHallPos.isPresent()) {
			BlockPos pos = townHallPos.get();
			double curDist = getDistanceSq(pos.getX() + 0.5d, pos.getY(), pos.getZ() + 0.5d);
			double newDist = getDistanceSq(position.getX() + 0.5d, position.getY(), position.getZ() + 0.5d);
			if (newDist < curDist) {
				setTownHallPosition(position);
				if (upkeepAutoBlock == null || upkeepAutoBlock.equals(pos)) {
					upkeepAutoBlock = position;
				}
			}
		} else {
			setTownHallPosition(position);
			if (upkeepAutoBlock == null) {
				upkeepAutoBlock = position;
			}
		}
		// (un)set alarmed status
		isAlarmed = getTownHall().map(t -> t.alarmActive).orElse(false);
	}

	private void validateTownHallPosition() {
		Optional<BlockPos> townHallPos = getTownHallPosition();
		if (!townHallPos.isPresent()) {
			return;
		}
		BlockPos pos = townHallPos.get();
		if (!level().isLoaded(pos)) {
			return;
		}//cannot validate, unloaded...assume good
		Optional<TileTownHall> te = WorldTools.getTile(level(), pos, TileTownHall.class);
		if (te.isPresent() && hasCommandPermissions(te.get().getOwner())) {
			return;
		}
		setTownHallPosition(null);
	}

	/*
	 * Returns the currently following player-issues command, or null if none
	 */
	public Command getCurrentCommand() {
		return playerIssuedCommand;
	}

	/*
	 * input path from command baton - default implementation for player-owned NPC is to set current command==input command and then let AI do the rest
	 */
	public void handlePlayerCommand(Command cmd) {
		if (cmd.type == CommandType.ATTACK) {
			handleAttackCommand(cmd);
			return;
		}
		setPlayerCommand(cmd);
	}

	private void handleAttackCommand(Command cmd) {
		Entity e = cmd.getEntityTarget(level());
		if (e instanceof LivingEntity) {
			LivingEntity elb = (LivingEntity) e;
			if (canTarget(elb))//only attacked allowed targets
			{
				setTarget(elb);
			}
		}
		setPlayerCommand(Command.NONE);
	}

	public void setPlayerCommand(Command cmd) {
		this.playerIssuedCommand = cmd;
	}

	@Override
	public boolean isHostileTowards(Entity entityTarget) {
		if ((entityTarget instanceof NpcPlayerOwned) || (entityTarget instanceof Player)) {
			return !getOwner().isOwnerOrSameTeamOrFriend(entityTarget);
		} else if (entityTarget instanceof NpcFaction) {
			return ((NpcFaction) entityTarget).isHostileTowards(this); // hostility is based on faction standing
		}
		return NpcDefaultsRegistry.getOwnedNpcDefault(this).isTarget(entityTarget) || AIHelper.isAdditionalEntityToTarget(entityTarget);
	}

	@Override
	public boolean canTarget(Entity e) {
		// don't let npcs target their own teams npcs/players
		return !getOwner().isOwnerOrSameTeamOrFriend(e) && e instanceof LivingEntity;
	}

	@Override
	public boolean canBeAttackedBy(Entity e) {
		return !getOwner().isOwnerOrSameTeamOrFriend(e);
	}

	@Override
	public int getFoodRemaining() {
		return foodValueRemaining;
	}

	@Override
	public void setFoodRemaining(int food) {
		this.foodValueRemaining = food;
	}

	@Override
	public Optional<BlockPos> getUpkeepPoint() {
		return UpkeepOrder.getUpkeepOrder(upkeepStack).map(UpkeepOrder::getUpkeepPosition).orElse(Optional.ofNullable(upkeepAutoBlock));
	}

	@Override
	public void setUpkeepAutoPosition(BlockPos pos) {
		upkeepAutoBlock = pos;
	}

	@Override
	public Direction getUpkeepBlockSide() {
		return UpkeepOrder.getUpkeepOrder(upkeepStack).map(UpkeepOrder::getUpkeepBlockSide).orElse(Direction.DOWN);
	}

	@Override
	public ResourceKey<Level> getUpkeepDimensionId() {
		return UpkeepOrder.getUpkeepOrder(upkeepStack).map(UpkeepOrder::getUpkeepDimension).orElse(level().dimension());
	}

	@Override
	public int getUpkeepAmount() {
		return UpkeepOrder.getUpkeepOrder(upkeepStack).map(UpkeepOrder::getUpkeepAmount).orElse(AWNPCStatics.npcDefaultUpkeepWithdraw);
	}

	@Override
	protected boolean tryCommand(Player player) {
		return hasCommandPermissions(player.getUUID(), player.getName().getString()) && super.tryCommand(player);
	}

	@Override
	protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel serverLevel, DamageSource source, boolean wasRecentlyHit) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot == EquipmentSlot.BODY) {
				continue;
			}
			ItemStack itemstack = this.getItemBySlot(slot);
			if (!itemstack.isEmpty()) {
				this.spawnAtLocation(itemstack);
			}
			setItemSlot(slot, ItemStack.EMPTY);
		}
		if (!AWNPCStatics.persistOrdersOnDeath) {
			if (!ordersStack.isEmpty()) {
				spawnAtLocation(ordersStack);
			}
			if (!upkeepStack.isEmpty()) {
				spawnAtLocation(upkeepStack);
			}
			ordersStack = ItemStack.EMPTY;
			upkeepStack = ItemStack.EMPTY;
		}
	}

	public boolean withdrawFood(IItemHandler handler) {
		int amount = getUpkeepAmount() - getFoodRemaining();
		if (amount <= 0) {
			return true;
		}
		ItemStack stack;
		int val;
		int eaten = 0;
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			stack = handler.getStackInSlot(slot);
			val = AncientWarfareNPC.statics.getFoodValue(stack);
			if (val <= 0) {
				continue;
			}
			while (eaten < amount && !stack.isEmpty()) {
				/*
				 * Improvement over 1.12 (config improvements.upkeep_counts_only_taken_food): 1.12 tests the stack it read
				 * before extracting. An ItemStackHandler (town hall, most mod inventories) replaces its stack on extraction,
				 * so that test never fails and one food item fills the whole food bar. Counted here: only what was removed.
				 */
				ItemStack taken = handler.extractItem(slot, 1, false);
				if (AWNPCStatics.improvedUpkeepCounting && taken.isEmpty()) {
					break;
				}
				eaten += val;
			}
		}
		setFoodRemaining(getFoodRemaining() + eaten);
		return getFoodRemaining() >= getUpkeepAmount();
	}

	@Override
	protected boolean processInteract(Player player, InteractionHand hand) {
		if (getFoodRemaining() < getUpkeepAmount()) {
			int value = AncientWarfareNPC.statics.getFoodValue(player.getItemInHand(hand));
			if (value > 0) {
				if (!level().isClientSide) {
					player.getItemInHand(hand).shrink(1);
				}
				foodValueRemaining += value;
				return true;
			}
		}
		return super.processInteract(player, hand);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (foodValueRemaining > 0 && !isSleeping()) {
			foodValueRemaining--;
		}
	}

	@Override
	public Entity changeDimension(net.minecraft.world.level.portal.DimensionTransition dimensionIn) {
		this.townHallPosition = null;
		this.upkeepAutoBlock = null;
		return super.changeDimension(dimensionIn);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		foodValueRemaining = tag.getInt("foodValue");
		if (tag.contains(COMMAND_TAG)) {
			playerIssuedCommand = new Command(tag.getCompound(COMMAND_TAG));
		}
		if (tag.contains(TOWN_HALL_TAG)) {
			townHallPosition = BlockPos.of(tag.getLong(TOWN_HALL_TAG));
		}
		if (tag.contains(UPKEEP_POS_TAG)) {
			upkeepAutoBlock = BlockPos.of(tag.getLong(UPKEEP_POS_TAG));
		}
		onOrdersInventoryChanged();
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("foodValue", foodValueRemaining);
		if (playerIssuedCommand != Command.NONE) {
			tag.put(COMMAND_TAG, playerIssuedCommand.writeToNBT(new CompoundTag()));
		}
		if (townHallPosition != null) {
			tag.putLong(TOWN_HALL_TAG, townHallPosition.asLong());
		}
		if (upkeepAutoBlock != null) {
			tag.putLong(UPKEEP_POS_TAG, upkeepAutoBlock.asLong());
		}
	}

}
