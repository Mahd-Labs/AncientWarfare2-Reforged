package net.shadowmage.ancientwarfare.npc.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper;
import net.shadowmage.ancientwarfare.core.interfaces.IEntityPacketHandler;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.PacketEntity;
import net.shadowmage.ancientwarfare.core.owner.IOwnable;
import net.shadowmage.ancientwarfare.core.owner.Owner;
import net.shadowmage.ancientwarfare.core.util.EntityTools;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.npc.ai.NpcAIBlockWithShield;
import net.shadowmage.ancientwarfare.npc.ai.NpcNavigator;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.npc.item.ItemCommandBaton;
import net.shadowmage.ancientwarfare.npc.item.ItemNpcSpawner;
import net.shadowmage.ancientwarfare.npc.registry.NpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.npc.skin.NpcSkinSettings;
import net.shadowmage.ancientwarfare.npc.xmod.IVehicle;
import net.shadowmage.ancientwarfare.npc.xmod.NpcXmod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static net.shadowmage.ancientwarfare.npc.config.AWNPCStatics.npcLevelDamageMultiplier;

/*
 * Port notes:
 * - 1.12 slot indices are kept for getItemStackFromSlot(int)/setItemStackToSlot(int, stack): 0 main hand, 1 off hand,
 *   2 feet, 3 legs, 4 chest, 5 head, 6 ORDER_SLOT, 7 UPKEEP_SLOT (1.21 EquipmentSlot.BODY is never used).
 * - vanillaAttackEntityAsMob (a copy of 1.12 EntityMob.attackEntityAsMob) -> Mob.doHurtTarget of 1.21.
 * - ISpecialArmor does not exist any more: armour is the vanilla calculation over getArmorValue() (override kept).
 * - 1.12 overrode updateActiveHand so that a shield kept being used when its stack changed (damage); 1.21
 *   LivingEntity.updatingUsingItem already compares by item, override dropped.
 * - setSize(w, h) -> a size override returned from getDefaultDimensions (sleeping uses 0.2 x 0.2 as 1.12).
 * - getYOffset (-0.35 riding offset) has no 1.21 equivalent (attachment points are defined by the mount).
 * - IPathableEntity (vehicle module) is not implemented, its methods are kept, see TODO-CROSS below.
 */
public abstract class NpcBase extends PathfinderMob implements IEntityWithComplexSpawn, IOwnable, IEntityPacketHandler, Npc {

	private static final EntityDataAccessor<Integer> AI_TASKS = SynchedEntityData.defineId(NpcBase.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SHIELD_DISABLED_TICK = SynchedEntityData.defineId(NpcBase.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<BlockPos> BED_POS = SynchedEntityData.defineId(NpcBase.class, EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<Byte> BED_DIRECTION = SynchedEntityData.defineId(NpcBase.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Boolean> IS_SLEEPING = SynchedEntityData.defineId(NpcBase.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> SWINGING_ARMS = SynchedEntityData.defineId(NpcBase.class, EntityDataSerializers.BOOLEAN);
	private static final String SLOT_NUM_TAG = "slotNum";
	private static final String BED_DIRECTION_TAG = "bedDirection";
	private static final String IS_SLEEPING_TAG = "isSleeping";
	private static final String CACHED_BED_POS_TAG = "cachedBedPos";
	private static final String BED_POS_TAG = "bedPos";
	private static final String FOUND_BED_TAG = "foundBed";
	private static final String ORDERS_STACK_TAG = "ordersStack";
	private static final String UPKEEP_STACK_TAG = "upkeepStack";
	private static final String LEVELING_STATS_TAG = "levelingStats";
	private static final String MAX_HEALTH_TAG = "maxHealth";
	private static final String HEALTH_TAG = "health";
	private static final String ATTACK_DAMAGE_OVERRIDE_TAG = "attackDamageOverride";
	private static final String ARMOR_VALUE_OVERRIDE_TAG = "armorValueOverride";
	private static final String AI_ENABLED_TAG = "aiEnabled";
	private static final String HAS_CUSTOM_EQUIPMENT_TAG = "hasCustomEquipment";
	public static final int ORDER_SLOT = 6;
	public static final int UPKEEP_SLOT = 7;
	private static final int EQUIPMENT_SLOTS = 6;
	private static final String ATTACK_TARGET_TAG = "attackTarget";

	public ItemStack ordersStack = ItemStack.EMPTY;
	private NpcAIBlockWithShield shieldBlockAI = null;
	private static final String DO_NOT_PURSUE = "donotpursue";
	private Owner owner = Owner.EMPTY;

	private String followingPlayerName;//set/cleared onInteract from player if player.team==this.team
	private NpcLevelingStats levelingStats;

	private NpcSkinSettings skinSettings = new NpcSkinSettings();

	public ItemStack upkeepStack = ItemStack.EMPTY;

	// used for flee/distress AI
	// this isn't really useful yet, combat NPC's will attack any mob they come in
	// contact with during a distress response
	public Set<Entity> nearbyHostiles = new LinkedHashSet<Entity>();

	private boolean aiEnabled = true;
	public boolean doNotPursue = false; //if the npc should not pursue targets away from its position/route
	private boolean hasCustomEquipment = false; //faction based only

	private int attackDamage = -1;//faction based only
	private int armorValue = -1;//faction based only
	private int maxHealthOverride = -1;

	private BlockPos cachedBedPos;
	private boolean foundBed = false;
	private boolean rainedOn = false;
	private float originalWidth;
	private float originalHeight;
	@Nullable
	private EntityDimensions sizeOverride = null;

	private int stopAIControlFlags = 0;

	public NpcBase(EntityType<? extends NpcBase> type, Level par1World) {
		super(type, par1World);
		levelingStats = new NpcLevelingStats(this);
		setPathfindingMalus(PathType.DOOR_WOOD_CLOSED, 0);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new NpcNavigator(this);
	}

	public void stopAIControlFlag(int flag) {
		stopAIControlFlags |= flag;
	}

	public void startAIControlFlag(int flag) {
		stopAIControlFlags &= ~flag;
	}

	public boolean isAIFlagStopped(int flag) {
		return (stopAIControlFlags & flag) > 0;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(AI_TASKS, 0);
		builder.define(BED_POS, BlockPos.ZERO);
		builder.define(BED_DIRECTION, (byte) Direction.NORTH.ordinal());
		builder.define(IS_SLEEPING, false);
		builder.define(SWINGING_ARMS, false);
		builder.define(SHIELD_DISABLED_TICK, 0);
	}

	/*
	 * 1.12 applyEntityAttributes
	 */
	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE).add(Attributes.ATTACK_SPEED);
	}

	private ItemStack getShieldStack() {
		return getItemInHand(InteractionHand.OFF_HAND);
	}

	public void setShieldStack(ItemStack stack) {
		setItemInHand(InteractionHand.OFF_HAND, stack);
	}

	public int getMaxHealthOverride() {
		return maxHealthOverride;
	}

	public void setMaxHealthOverride(int maxHealthOverride) {
		this.maxHealthOverride = maxHealthOverride;
		if (maxHealthOverride > 0) {
			getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealthOverride);
			if (getHealth() < getMaxHealth()) {
				setHealth(getMaxHealth());
			}
		}
	}

	public void setAttackDamageOverride(int attackDamage) {
		this.attackDamage = attackDamage;
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(attackDamage);
	}

	public void setArmorValueOverride(int armorValue) {
		this.armorValue = armorValue;
	}

	public int getArmorValueOverride() {
		return armorValue;
	}

	public int getAttackDamageOverride() {
		return attackDamage;
	}

	public void setCustomEquipmentOverride(boolean val) {
		hasCustomEquipment = val;
	}

	public boolean getCustomEquipmentOverride() {
		return hasCustomEquipment;
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		float damage = (float) getAttackDamageOverride();

		ItemStack shield = ItemStack.EMPTY;
		if (damage < 0 && !getShieldStack().isEmpty()) {
			shield = getShieldStack().copy();
			shield.forEachModifier(EquipmentSlot.OFFHAND, (attribute, modifier) -> {
				AttributeInstance instance = getAttribute(attribute);
				if (instance != null) {
					instance.removeModifier(modifier.id());
					instance.addTransientModifier(modifier);
				}
			});
		}

		boolean targetHit = super.doHurtTarget(target);

		if (targetHit && target instanceof LivingEntity) {
			ItemStack item = getMainHandItem();
			if (item.getItem().hurtEnemy(item, (LivingEntity) target, this)) {
				item.getItem().postHurtEnemy(item, (LivingEntity) target, this);
			}
		}

		if (!shield.isEmpty()) {
			shield.forEachModifier(EquipmentSlot.OFFHAND, (attribute, modifier) -> {
				AttributeInstance instance = getAttribute(attribute);
				if (instance != null) {
					instance.removeModifier(modifier.id());
				}
			});
			//the off hand item's own modifiers are re-applied by the vanilla equipment change detection
			if (level() instanceof ServerLevel) {
				ItemStack equipped = getShieldStack();
				equipped.forEachModifier(EquipmentSlot.OFFHAND, (attribute, modifier) -> {
					AttributeInstance instance = getAttribute(attribute);
					if (instance != null && !instance.hasModifier(modifier.id())) {
						instance.addTransientModifier(modifier);
					}
				});
			}
		}
		return targetHit;
	}

	/*
	 * Deprecated vanilla armor calculations
	 */
	@Override
	public int getArmorValue() {
		if (getArmorValueOverride() >= 0) {
			return getArmorValueOverride();
		}
		return super.getArmorValue();
	}

	@Override
	public int getMaxFallDistance() {
		return getTarget() == null ? 4 : 4 + (int) (getHealth() / 3);
	}

	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader levelReader) {
		BlockState stateBelow = level().getBlockState(pos.below());
		if (stateBelow.getFluidState().is(FluidTags.LAVA) || stateBelow.is(Blocks.CACTUS))//Avoid cacti and lava when wandering
		{ return -10; } else if (stateBelow.liquid())//Don't try swimming too much
		{ return 0; }
		float level = getLitBlockWeight(pos);//Prefer lit areas
		if (level < 0) { return 0; } else { return level + (stateBelow.isFaceSturdy(level(), pos.below(), Direction.UP) ? 1 : 0); }
	}

	protected float getLitBlockWeight(BlockPos pos) {
		return level().getLightLevelDependentMagicValue(pos);
	}

	public double getDistanceSqFromHome() {
		if (!hasRestriction()) {
			return 0;
		}
		BlockPos home = getRestrictCenter();
		return getDistanceSq(home.getX() + 0.5d, home.getY(), home.getZ() + 0.5d);
	}

	public Optional<BlockPos> getTownHallPosition() {
		return Optional.empty();//NOOP on non-player owned npc
	}

	public void setHomeAreaAtCurrentPosition() {
		restrictTo(new BlockPos(Mth.floor(getX()), Mth.floor(getY()), Mth.floor(getZ())), getHomeRange());
	}

	public int getHomeRange() {
		if (hasRestriction()) {
			return Mth.floor(getRestrictRadius());
		}
		return 5;
	}

	/*
	 * Return true if this NPC should be within his home range.<br>
	 * Should still allow for a combat NPC to attack targets outside his home range.
	 */
	public boolean shouldBeAtHome() {
		if (getTarget() != null || !hasRestriction()) {
			return false;
		}
		if (level().isRainingAt(blockPosition())) {
			setRainedOn(true);
		}
		return shouldSleep() || isWaitingForRainToStop();
	}

	private boolean isWaitingForRainToStop() {
		if (worksInRain() || !level().isRaining()) {
			// rain has stopped, reset
			setRainedOn(false);
			return false;
		}
		return rainedOn;
	}

	public boolean worksInRain() {
		return false;
	}

	public boolean isPassive() {
		return true;
	}

	private void setRainedOn(boolean rainedOn) {
		this.rainedOn = rainedOn;
	}

	public void setIsAIEnabled(boolean val) {
		aiEnabled = val;
	}

	public boolean getIsAIEnabled() {
		return aiEnabled && !AWNPCStatics.npcAIDebugMode;
	}

	@Override
	protected final InteractionResult mobInteract(Player player, InteractionHand hand) {
		return processInteract(player, hand) ? InteractionResult.sidedSuccess(level().isClientSide) : InteractionResult.PASS;
	}

	/*
	 * 1.12 EntityLiving.processInteract (kept under its old name and boolean result; mobInteract maps it)
	 */
	protected boolean processInteract(Player player, InteractionHand hand) {
		return tryCommand(player);
	}

	/*
	 * should be implemented by any npc that wishes to open a GUI on interact<br>
	 * must be called from interact code to actually open the GUI<br>
	 * allows for subtypes/etc to vary the opened GUI without re-implementing the interact logic
	 */
	public void openGUI(Player player) {
		NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_NPC_INVENTORY, getId(), 0, 0);
	}

	/*
	 * if this npc has an alt-control GUI, open it here.<br>
	 * should called from the npc inventory gui.
	 */
	public void openAltGui(Player player) {

	}

	/*
	 * used by the npc inventory gui to determine if it should display the 'alt control gui' button<br>
	 * this setting must return true -on the client- if the button is to be displayed.
	 */
	public boolean hasAltGui() {
		return false;
	}

	protected boolean tryCommand(Player player) {
		boolean baton = !EntityTools.getItemFromEitherHand(player, ItemCommandBaton.class).isEmpty();
		if (!baton) {
			if (!level().isClientSide) {
				if (player.isShiftKeyDown()) {
					if (followingPlayerName != null && followingPlayerName.equals(player.getName().getString())) {
						followingPlayerName = null;
					} else {
						followingPlayerName = player.getName().getString();
					}
				} else {
					openGUI(player);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public final boolean hurt(DamageSource source, float damage) {
		if (isSleeping()) { // prevent suffocation damage (allows bunk beds and such)
			return false;
		}
		if (source.getEntity() != null && !canBeAttackedBy(source.getEntity())) {
			return false;
		}
		if (!level().isClientSide)
			processPreDamageLogic(source, damage);
		return super.hurt(source, damage);
	}

	private void processPreDamageLogic(DamageSource source, float damage) {
		if (shieldBlockAI != null) {
			shieldBlockAI.onPreDamage(source, damage);
		}
	}

	@Override
	public void aiStep() {
		if (isUsingItem() && !isPassenger()) {
			setSpeed(getSpeed() * 0.2f);
			setSprinting(false);
		}
		super.aiStep();
	}

	@Override
	protected void blockUsingShield(LivingEntity attacker) {
		attacker.knockback(0.1F, getX() - attacker.getX(), getZ() - attacker.getZ());

		if (attacker.getMainHandItem().canDisableShield(getUseItem(), this, attacker)) {
			disableShield();
		}
	}

	private void disableShield() {
		//1.12: random.nextFloat() < 1F + efficiency * 0.05F, i.e. always
		setShieldDisabledTick(80);
		stopUsingItem();
		level().broadcastEntityEvent(this, (byte) 30);
	}

	@Override
	protected void hurtCurrentlyUsedShield(float damage) {
		if (damage >= 3.0F && isShield(useItem)) {
			int i = 1 + Mth.floor(damage);
			getShieldStack().hurtAndBreak(i, this, EquipmentSlot.OFFHAND);
			startUsingItem(InteractionHand.OFF_HAND);

			if (getShieldStack().isEmpty()) {  //shield breaks
				setShieldStack(ItemStack.EMPTY);
				playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + level().random.nextFloat() * 0.4F);
			}
		}
	}

	/*
	 * 1.12 Item.isShield(stack, entity)
	 */
	public static boolean isShield(ItemStack stack) {
		return !stack.isEmpty() && stack.canPerformAction(ItemAbilities.SHIELD_BLOCK);
	}

	@Override
	protected void setLevel(Level world) {
		super.setLevel(world);
		if (navigation instanceof NpcNavigator) {
			((NpcNavigator) navigation).onWorldChange();
		}
	}

	@Override
	public void setTarget(@Nullable LivingEntity entity) {
		if (entity != null && !canTarget(entity)) {
			return;
		}
		super.setTarget(entity);

		if (!level().isClientSide && entity != null) {
			updateAttackTargetClient(entity);
		}
	}

	private void updateAttackTargetClient(LivingEntity entity) {
		PacketEntity pkt = new PacketEntity(this);
		pkt.packetData.putInt(ATTACK_TARGET_TAG, entity.getId());
		NetworkHandler.sendToAllTracking(this, pkt);
	}

	@Override
	public final void setLastHurtByMob(@Nullable LivingEntity entity) {
		if (entity != null && !canTarget(entity)) {
			return;
		}
		super.setLastHurtByMob(entity);
	}

	public final ItemStack getItemStackFromSlot(int slot) {
		if (slot == ORDER_SLOT) {
			return ordersStack;
		} else if (slot == UPKEEP_SLOT) {
			return upkeepStack;
		} else if (slot >= 0 && slot < EQUIPMENT_SLOTS) {
			return super.getItemBySlot(EquipmentSlot.values()[slot]);
		}
		return ItemStack.EMPTY;
	}

	/* converter name of getItemStackFromSlot(int) */
	public final ItemStack getItemBySlot(int slot) {
		return getItemStackFromSlot(slot);
	}

	@Override
	public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
		super.setItemSlot(slot, stack);
		if (slot == EquipmentSlot.MAINHAND) {
			onWeaponInventoryChanged();
		}
		if (slot == EquipmentSlot.OFFHAND) {
			onOffhandInventoryChanged();
		}
	}

	public final void setItemStackToSlot(int slot, ItemStack stack) {
		if (slot >= 0 && slot < EQUIPMENT_SLOTS) {
			setItemSlot(EquipmentSlot.values()[slot], stack);
		} else if (slot == UPKEEP_SLOT) {
			upkeepStack = stack;
		} else if (slot == ORDER_SLOT) {
			ordersStack = stack;
			onOrdersInventoryChanged();
		}
	}

	/* converter name of setItemStackToSlot(int, ItemStack) */
	public final void setItemSlot(int slot, ItemStack stack) {
		setItemStackToSlot(slot, stack);
	}

	@Override
	public final boolean killedEntity(ServerLevel world, LivingEntity par1EntityLivingBase) {
		boolean ret = super.killedEntity(world, par1EntityLivingBase);
		addExperience(AWNPCStatics.npcXpFromKill);
		if (par1EntityLivingBase == getTarget()) {
			setTarget(null);
		}
		return ret;
	}

	/*
	 * return the bitfield containing all of the currently executing AI tasks<br>
	 * used by player-owned npcs for rendering ai-tasks
	 */
	public final int getAITasks() {
		return entityData.get(AI_TASKS);
	}

	/*
	 * add a task to the bitfield of currently executing tasks<br>
	 * input should be a ^2, or combination of (e.g. 1+2 or 2+4)<br>
	 */
	public final void addAITask(int task) {
		int tasks = getAITasks();
		int tc = tasks;
		tasks = tasks | task;
		if (tc != tasks) {
			setAITasks(tasks);
		}
	}

	/*
	 * remove a task from the bitfield of currently executing tasks<br>
	 * input should be a ^2, or combination of (e.g. 1+2 or 2+4)<br>
	 */
	public final void removeAITask(int task) {
		int tasks = getAITasks();
		int tc = tasks;
		tasks = tasks & (~task);
		if (tc != tasks) {
			setAITasks(tasks);
		}
	}

	/*
	 * set ai tasks -- only used internally
	 */
	private void setAITasks(int tasks) {
		entityData.set(AI_TASKS, tasks);
	}

	/*
	 * add an amount of experience to this npcs leveling stats<br>
	 * experience is added for base level, and subtype level(if any)
	 */
	public final void addExperience(int amount) {
		getLevelingStats().addExperience(amount);
	}

	/*
	 * implementations should read in any data written during {@link #writeAdditionalItemData(CompoundTag)}
	 */
	public void readAdditionalItemData(CompoundTag tag) {
		ListTag equipmentList = tag.getList("equipment", Tag.TAG_COMPOUND);
		ItemStack stack;
		CompoundTag equipmentTag;
		for (int i = 0; i < equipmentList.size(); i++) {
			equipmentTag = equipmentList.getCompound(i);
			stack = NBTHelper.readItemStack(equipmentTag);
			if (equipmentTag.contains(SLOT_NUM_TAG)) {
				setItemStackToSlot(equipmentTag.getInt(SLOT_NUM_TAG), stack);
			}
		}
		readBaseTags(tag);
	}

	/*
	 * Implementations should write out any persistent entity-data needed to restore entity-state from an item-stack.<br>
	 * This should include inventory, levels, orders, faction / etc
	 */
	public void writeAdditionalItemData(CompoundTag tag) {
		ListTag equipmentList = new ListTag();
		ItemStack stack;
		CompoundTag equipmentTag;
		for (int slot = 0; slot < EQUIPMENT_SLOTS; slot++) {
			stack = getItemStackFromSlot(slot);
			if (stack.isEmpty()) {
				continue;
			}
			equipmentTag = NBTHelper.writeItemStack(stack, new CompoundTag());
			equipmentTag.putInt(SLOT_NUM_TAG, slot);
			equipmentList.add(equipmentTag);
		}
		tag.put("equipment", equipmentList);
		writeBaseTags(tag);
	}

	/*
	 * is the input stack a valid orders-item for this npc?<br>
	 * only used by player-owned NPCs
	 */
	@SuppressWarnings("squid:S1172")
	public boolean isValidOrdersStack(ItemStack stack) {
		return false;
	}

	/*
	 * callback for when orders-stack changes.  implementations should inform any necessary AI tasks of the
	 * change to order-items
	 */
	public void onOrdersInventoryChanged() {
	}

	/*
	 * callback for when weapon slot has been changed.<br>
	 * Implementations should re-set any subtype or inform any AI that need to know when
	 * weapon was changed.
	 */
	public void onWeaponInventoryChanged() {
	}

	/*
	 * callback for when the offhand item slot has been changed.<br>
	 * Implementations should re-set any subtype or inform any AI that need to know when
	 * weapon was changed.
	 */
	public void onOffhandInventoryChanged() {
		if (!level().isClientSide) {
			ItemStack mainhandStack = getMainHandItem();
			ItemStack offhandStack = getOffhandItem();
			if (isShield(offhandStack) && !isBow(mainhandStack.getItem())) {
				shieldBlockAI = initShieldAI();
				if (goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() == shieldBlockAI)) {
					goalSelector.addGoal(3, shieldBlockAI);
				}
			} else {
				shieldBlockAI = null;
			}
		}
	}

	/*
	 * return the NPCs subtype.<br>
	 * this subtype may vary at runtime.
	 */
	public abstract String getNpcSubType();

	/*
	 * return the NPCs type.  This type should be unique for the class of entity,
	 * or at least unique pertaining to the entity registration.
	 */
	public abstract String getNpcType();

	/*
	 * return the full NPC type for this npc<br>
	 * returns npcType if subtype is empty, else npcType.npcSubtype
	 */
	public String getNpcFullType() {
		String type = getNpcType();
		if (type == null || type.isEmpty()) {
			throw new RuntimeException("Type must not be null or empty:");
		}
		String sub = getNpcSubType();
		if (sub == null) {
			throw new RuntimeException("Subtype must not be null...type: " + type);
		}
		if (!sub.isEmpty()) {
			type = type + "." + sub;
		}
		return type;
	}

	@Override
	public Component getName() {
		Component name = Component.translatable("entity.ancientwarfarenpc." + getNpcFullType() + ".name");
		if (hasCustomName()) {
			name = Component.empty().append(name).append(" : " + getCustomName().getString());
		}
		return name;
	}

	public final NpcLevelingStats getLevelingStats() {
		return levelingStats;
	}

	private ItemStack getItemToSpawn() {
		return ItemNpcSpawner.getSpawnerItemForNpc(this);
	}

	public final long getIDForSkin() {
		return getUUID().getLeastSignificantBits();
	}

	@Override
	public final ItemStack getPickedResult(HitResult target) {
		return getItemToSpawn();
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		buffer.writeLong(getUUID().getMostSignificantBits());
		buffer.writeLong(getUUID().getLeastSignificantBits());
		owner.serializeToBuffer(buffer);
		skinSettings.serializeToBuffer(buffer);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		long l1 = buffer.readLong();
		long l2 = buffer.readLong();
		setUUID(new UUID(l1, l2));
		owner = new Owner(buffer);
		skinSettings = NpcSkinSettings.deserializeFromBuffer(buffer);
	}

	@Override
	public void tick() {
		level().getProfiler().push("AWNpcTick");
		updateSwingTime();
		if (getShieldDisabledTick() > 0) {
			decrementShieldDisabledTick();
		}
		if (tickCount % 200 == 0 && getHealth() < getMaxHealth() && isAlive() && (!requiresUpkeep() || getFoodRemaining() > 0)) {
			setHealth(getHealth() + 1);
		}
		super.tick();
		if (!getMainHandItem().isEmpty()) {
			getMainHandItem().inventoryTick(level(), this, 0, true);
		}
		level().getProfiler().pop();
	}

	/*
	 * 1.12 canAttackClass(Class): no flying mobs
	 */
	@Override
	public boolean canAttack(LivingEntity target) {
		return (canAttackFlying() || !(target instanceof FlyingMob)) && super.canAttack(target);
	}

	protected boolean canAttackFlying() {
		return false;
	}

	/*
	 * 1.12 Item.getToolClasses(stack) as far as the NPC code used it ("axe", "pickaxe", "shovel", "hammer", "quill")
	 */
	public static java.util.Collection<String> getToolClasses(ItemStack stack) {
		Set<String> tools = new java.util.HashSet<>();
		if (stack.canPerformAction(ItemAbilities.AXE_DIG)) {
			tools.add("axe");
		}
		if (stack.canPerformAction(ItemAbilities.PICKAXE_DIG)) {
			tools.add("pickaxe");
		}
		if (stack.canPerformAction(ItemAbilities.SHOVEL_DIG)) {
			tools.add("shovel");
		}
		if (stack.getItem() instanceof net.shadowmage.ancientwarfare.core.item.ItemHammer) {
			tools.add("hammer");
		}
		if (stack.getItem() instanceof net.shadowmage.ancientwarfare.core.item.ItemQuill) {
			tools.add("quill");
		}
		return tools;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	final void updateDamageFromLevel() {
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(getLeveledAttack());
	}

	private double getLeveledAttack() {
		double dmg = getBaseAttack();
		int level = getLevelingStats().getLevel();
		return dmg * (1 + level * npcLevelDamageMultiplier);
	}

	private double getBaseAttack() {
		//TODO get rid of this instanceof check once player owned attribs migrated ot the same way
		return this instanceof NpcFaction ? NpcDefaultsRegistry.getFactionNpcDefault((NpcFaction) this).getBaseAttack() :
				NpcDefaultsRegistry.getOwnedNpcDefault((NpcPlayerOwned) this).getBaseAttack();
	}

	public int getFoodRemaining() {
		return 0;//NOOP in non-player owned
	}

	public void setFoodRemaining(int food) {
		//NOOP in non-player owned
	}

	public boolean requiresUpkeep() {
		return this instanceof IKeepFood;//NOOP in non-player owned
	}

	@Override
	public void setOwner(Player player) {
		owner = new Owner(player);
	}

	@Override
	public void setOwner(Owner owner) {
		this.owner = owner;
	}

	public void setOwnerName(String name) {
		owner = new Owner(level(), name);
		if (!level().isClientSide && !name.equals(owner.getName())) {
			PacketEntity pkt = new PacketEntity(this);
			pkt.packetData = owner.serializeToNBT(new CompoundTag());
			NetworkHandler.sendToAllTracking(this, pkt);
		}
	}

	@Override
	public boolean isOwner(Player player) {
		return owner.isOwnerOrSameTeamOrFriend(player);
	}

	@Override
	public Owner getOwner() {
		return owner;
	}

	@Override
	public PlayerTeam getTeam() {
		return level().getScoreboard().getPlayersTeam(owner.getName());
	}

	public boolean hasCommandPermissions(Owner owner) {
		return hasCommandPermissions(owner.getUUID(), owner.getName());
	}

	public boolean hasCommandPermissions(UUID playerId, String playerName) {
		return owner.playerHasCommandPermissions(level(), playerId, playerName);
	}

	/*
	 * 1.12 getExperiencePoints(EntityPlayer attacker)
	 */
	@Override
	protected int getBaseExperienceReward() {
		Player attacker = lastHurtByPlayer;
		if (attacker == null || (isHostileTowards(attacker) && canBeAttackedBy(attacker))) {
			return super.getBaseExperienceReward();
		}
		return 0;
	}

	public abstract boolean isHostileTowards(Entity e);

	public abstract boolean canTarget(Entity e);

	public abstract boolean canBeAttackedBy(Entity e);

	public final LivingEntity getFollowingEntity() {
		if (followingPlayerName == null) {
			return null;
		}
		for (Player player : level().players()) {
			if (followingPlayerName.equals(player.getName().getString())) {
				return player;
			}
		}
		return null;
	}

	public final void setFollowingEntity(LivingEntity entity) {
		if (entity instanceof Player && hasCommandPermissions(entity.getUUID(), entity.getName().getString())) {
			followingPlayerName = entity.getName().getString();
		}
	}

	public final void clearFollowingEntity() {
		followingPlayerName = null;
	}

	public void setDoNotPursue(boolean val) {
		doNotPursue = val;
	}

	public boolean getDoNotPursue() {
		return doNotPursue;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	public final void repackEntity(Player player) {
		// we already hide the button but we need to prevent repack server-side too
		if (AWNPCStatics.repackCreativeOnly && !player.getAbilities().instabuild) {
			return;
		}
		if (!player.level().isClientSide && isAlive()) {
			onRepack();
			ItemStack item = InventoryTools.mergeItemStack(new PlayerInvWrapper(player.getInventory()), getItemToSpawn());
			if (!item.isEmpty()) {
				Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), item);
			}
		}
		discard();
	}

	/*
	 * called when NPC is being repacked into item-form.  Called prior to item being created and prior to entity being set-dead.<br>
	 * Main function is for faction-mounted NPCs to disappear their mounts when repacked.
	 */
	protected void onRepack() {

	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		for (int slot = 0; slot < EQUIPMENT_SLOTS; slot++) {
			setItemSlot(EquipmentSlot.values()[slot], ItemStack.EMPTY);
		}
		super.readAdditionalSaveData(tag);
		if (tag.contains("home")) {
			restrictTo(BlockPos.of(tag.getLong("home")), tag.getInt("homeRange"));
		}
		if (tag.contains(BED_DIRECTION_TAG)) {
			setBedDirection(Direction.values()[tag.getByte(BED_DIRECTION_TAG)]);
		}
		if (tag.contains(IS_SLEEPING_TAG)) {
			setSleeping(tag.getBoolean(IS_SLEEPING_TAG));
		}
		if (tag.contains(CACHED_BED_POS_TAG)) {
			cachedBedPos = BlockPos.of(tag.getLong(CACHED_BED_POS_TAG));
		}
		if (tag.contains(BED_POS_TAG)) {
			setBedPosition(BlockPos.of(tag.getLong(BED_POS_TAG)));
		}
		if (tag.contains(FOUND_BED_TAG)) {
			foundBed = tag.getBoolean(FOUND_BED_TAG);
		}

		originalWidth = tag.getFloat("originalWidth");
		originalHeight = tag.getFloat("originalHeight");
		if (isSleeping()) {
			setSize(0.2F, 0.2F);
		}

		readBaseTags(tag);
		onWeaponInventoryChanged();
		onOffhandInventoryChanged();
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (!hasRestriction()) {
			Optional<BlockPos> position = getTownHallPosition();
			if (position.isPresent()) {
				restrictTo(position.get(), getHomeRange());
			} else {
				setHomeAreaAtCurrentPosition();
			}
		}
		tag.putLong("home", getRestrictCenter().asLong());
		tag.putInt("homeRange", getHomeRange());
		tag.putByte(BED_DIRECTION_TAG, (byte) getBedDirection().ordinal());
		tag.putBoolean(IS_SLEEPING_TAG, isSleeping());
		if (cachedBedPos != null) {
			tag.putLong(CACHED_BED_POS_TAG, cachedBedPos.asLong());
		}
		BlockPos bedPos = getBedPosition();
		tag.putLong(BED_POS_TAG, bedPos.asLong());
		tag.putBoolean(FOUND_BED_TAG, foundBed);
		tag.putFloat("originalWidth", originalWidth);
		tag.putFloat("originalHeight", originalHeight);

		writeBaseTags(tag);
	}

	private void readBaseTags(CompoundTag tag) {
		if (tag.contains(ORDERS_STACK_TAG)) {
			setItemStackToSlot(ORDER_SLOT, NBTHelper.readItemStack(tag.getCompound(ORDERS_STACK_TAG)));
		}
		if (tag.contains(UPKEEP_STACK_TAG)) {
			setItemStackToSlot(UPKEEP_SLOT, NBTHelper.readItemStack(tag.getCompound(UPKEEP_STACK_TAG)));
		}
		if (tag.contains(LEVELING_STATS_TAG)) {
			getLevelingStats().readFromNBT(tag.getCompound(LEVELING_STATS_TAG));
		}
		if (tag.contains(MAX_HEALTH_TAG)) {
			getAttribute(Attributes.MAX_HEALTH).setBaseValue(tag.getFloat(MAX_HEALTH_TAG));
		}
		if (tag.contains(HEALTH_TAG)) {
			setHealth(tag.getFloat(HEALTH_TAG));
		}
		if (tag.contains("name")) {
			setCustomName(Component.literal(tag.getString("name")));
		}
		if (tag.contains("food")) {
			setFoodRemaining(tag.getInt("food"));
		}
		if (tag.contains(ATTACK_DAMAGE_OVERRIDE_TAG)) {
			setAttackDamageOverride(tag.getInt(ATTACK_DAMAGE_OVERRIDE_TAG));
		}
		if (tag.contains(ARMOR_VALUE_OVERRIDE_TAG)) {
			setArmorValueOverride(tag.getInt(ARMOR_VALUE_OVERRIDE_TAG));
		}
		if (tag.contains(AI_ENABLED_TAG)) {
			setIsAIEnabled(tag.getBoolean(AI_ENABLED_TAG));
		}
		if (tag.contains(DO_NOT_PURSUE)) {
			setDoNotPursue(tag.getBoolean(DO_NOT_PURSUE));
		}
		if (tag.contains(HAS_CUSTOM_EQUIPMENT_TAG)) {
			setCustomEquipmentOverride(tag.getBoolean(HAS_CUSTOM_EQUIPMENT_TAG));
		}
		setSkinSettings(NpcSkinSettings.deserializeNBT(tag.getCompound("skinSettings")).minimizeData());
		owner = Owner.deserializeFromNBT(tag);
	}

	private void writeBaseTags(CompoundTag tag) {
		if (!ordersStack.isEmpty()) {
			tag.put(ORDERS_STACK_TAG, NBTHelper.writeItemStack(ordersStack, new CompoundTag()));
		}
		if (!upkeepStack.isEmpty()) {
			tag.put(UPKEEP_STACK_TAG, NBTHelper.writeItemStack(upkeepStack, new CompoundTag()));
		}
		tag.put(LEVELING_STATS_TAG, getLevelingStats().writeToNBT(new CompoundTag()));
		tag.putFloat(MAX_HEALTH_TAG, getMaxHealth());
		tag.putFloat(HEALTH_TAG, getHealth());
		if (hasCustomName()) {
			tag.putString("name", getCustomName().getString());
		}
		tag.putInt("food", getFoodRemaining());
		tag.putInt(ATTACK_DAMAGE_OVERRIDE_TAG, attackDamage);
		tag.putInt(ARMOR_VALUE_OVERRIDE_TAG, armorValue);
		tag.putBoolean(AI_ENABLED_TAG, aiEnabled);
		tag.putBoolean(DO_NOT_PURSUE, doNotPursue);
		tag.putBoolean(HAS_CUSTOM_EQUIPMENT_TAG, hasCustomEquipment);
		tag.put("skinSettings", skinSettings.serializeNBT());
		owner.serializeToNBT(tag);
	}

	public final ResourceLocation getTexture() {
		return skinSettings.getTexture(this);
	}

	@Override
	public void handlePacketData(CompoundTag tag) {
		if (tag.contains("ownerName")) {
			owner = Owner.deserializeFromNBT(tag);
		} else if (tag.contains(NpcSkinSettings.PACKET_TAG_NAME)) {
			skinSettings.handlePacketData(tag);
		} else if (tag.contains(ATTACK_TARGET_TAG)) {
			int entityId = tag.getInt(ATTACK_TARGET_TAG);
			Entity target = level().getEntity(entityId);
			setTarget(target instanceof LivingEntity ? (LivingEntity) target : null);
		}
	}

	/*
	 * 1.12 NpcBase.getDistanceSq(BlockPos): x / z centred, y not
	 */
	public double getDistanceSq(BlockPos pos) {
		return getDistanceSq(pos.getX() + 0.5d, pos.getY(), pos.getZ() + 0.5d);
	}

	public double getDistanceSq(double x, double y, double z) {
		return distanceToSqr(x, y, z);
	}

	/* converter name of getDistanceSq(BlockPos) */
	public double distanceToSqr(BlockPos pos) {
		return getDistanceSq(pos);
	}

	public BlockPos findBed() {
		if (!foundBed) {
			int originX = Mth.floor(getX());
			int originY = Mth.floor(getY());
			int originZ = Mth.floor(getZ());
			int maxSearchRange = 6;
			int minX = originX - maxSearchRange;
			int maxX = originX + maxSearchRange;
			int minY = originY - maxSearchRange;
			int maxY = originY + maxSearchRange;
			int minZ = originZ - maxSearchRange;
			int maxZ = originZ + maxSearchRange;
			List<BlockPos> foundBeds = new ArrayList<>();
			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					for (int z = minZ; z <= maxZ; z++) {
						BlockState state = level().getBlockState(new BlockPos(x, y, z));
						if (isUsableBed(state)) {
							if (state.getValue(BedBlock.PART) != BedPart.FOOT) {
								continue;
							}
							if (!state.getValue(BedBlock.OCCUPIED)) { // occupied check
								foundBeds.add(new BlockPos(x, y, z));
							}
						}
					}
				}
			}

			int closetBedIndex = -1;
			for (int i = 0; i < foundBeds.size(); i++) {
				double bedDistance = distanceSqToCenter(foundBeds.get(i), originX, originY, originZ);
				if ((closetBedIndex == -1) || (bedDistance < distanceSqToCenter(foundBeds.get(closetBedIndex), originX, originY, originZ))) {
					closetBedIndex = i;
				}
			}
			if (closetBedIndex == -1) {
				foundBed = false;
				return null;
			}
			foundBed = true;
			cachedBedPos = foundBeds.get(closetBedIndex);
		}

		BlockState state = level().getBlockState(cachedBedPos);
		if (isUsableBed(state)) {
			return cachedBedPos;
		} else {
			foundBed = false;
			return null; // try again in a while
		}
	}

	/* 1.12 Vec3i.distanceSqToCenter */
	private static double distanceSqToCenter(BlockPos pos, double x, double y, double z) {
		double d0 = pos.getX() + 0.5D - x;
		double d1 = pos.getY() + 0.5D - y;
		double d2 = pos.getZ() + 0.5D - z;
		return d0 * d0 + d1 * d1 + d2 * d2;
	}

	public boolean lieDown(BlockPos pos) {
		if (!foundBed) {
			return false;
		}
		if (level().isLoaded(pos) && isUsableBed(level().getBlockState(pos))) {
			BlockState state = level().getBlockState(pos);
			if (state.getValue(BedBlock.OCCUPIED)) {
				// occupied check
				foundBed = false;
				return false;
			}
			state = state.setValue(BedBlock.OCCUPIED, true);
			level().setBlockAndUpdate(pos, state);
			setBedPosition(pos);
			setBedDirection(state.getValue(BedBlock.FACING));
			setSleeping(true);
			originalHeight = getBbHeight();
			originalWidth = getBbWidth();
			setSize(0.2F, 0.2F);
			setPositionToBed();
			return true;
		}
		return false;
	}

	public void wakeUp() {
		setSleeping(false);
		BlockPos bedPos = getBedPosition();
		// set vacant
		BlockState bedState = level().getBlockState(bedPos);
		if (isUsableBed(bedState)) {
			level().setBlock(bedPos, bedState.setValue(BedBlock.OCCUPIED, false), 4);
		}

		// Try placing the NPC to an empty spot next to the bed. We don't want them standing on top of the bed, chance for suffocation
		Direction bedDirection = getBedDirection();

		setSize(originalWidth, originalHeight);

		if (tryMovingToBedside(bedPos.relative(bedDirection.getClockWise()))) {
			return;
		}
		if (tryMovingToBedside(bedPos.relative(bedDirection.getCounterClockWise()))) {
			return;
		}

		BlockPos offsetPos = bedPos.relative(bedDirection.getOpposite());
		if (tryMovingToBedside(offsetPos)) {
			return;
		}
		if (tryMovingToBedside(offsetPos.relative(bedDirection.getClockWise()))) {
			return;
		}
		tryMovingToBedside(offsetPos.relative(bedDirection.getCounterClockWise()));
	}

	private boolean tryMovingToBedside(BlockPos posToMove) {
		if (level().getBlockState(posToMove).blocksMotion()) {
			return false;
		}
		if (level().getBlockState(posToMove.above()).blocksMotion()) {
			return false;
		}
		if (!level().getBlockState(posToMove.below()).blocksMotion()) {
			return false;
		}
		aiEnabled = false;
		setPos(posToMove.getX() + 0.5, posToMove.getY() + 0.5, posToMove.getZ() + 0.5);
		aiEnabled = true;
		return true;
	}

	/*
	 * 1.12 Entity.setSize(width, height); a value of 0 (never slept) restores the type's size. The override keeps the
	 * type's riding offset (1.12 getYOffset -0.35 for every NPC size = the 1.21 player vehicle attachment): without it
	 * faction NPCs, which always have a size from the faction table, sat 0.6 blocks above the saddle.
	 */
	protected void setSize(float width, float height) {
		sizeOverride = width > 0 && height > 0 ? EntityDimensions.scalable(width, height)
				.withAttachments(net.minecraft.world.entity.EntityAttachments.builder().attach(net.minecraft.world.entity.EntityAttachment.VEHICLE, Player.DEFAULT_VEHICLE_ATTACHMENT)) : null;
		refreshDimensions();
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		if (sizeOverride != null) {
			return sizeOverride;
		}
		return super.getDefaultDimensions(pose);
	}

	/*
	 * 1.12 had this only on faction NPCs (render scale from the registry height); 1 = normal
	 */
	public float getRenderSizeModifier() {
		return 1.0F;
	}

	private BlockPos getBedPosition() {
		return entityData.get(BED_POS);
	}

	private void setBedPosition(BlockPos pos) {
		entityData.set(BED_POS, pos);
	}

	public void setPositionToBed() {
		float xOffset = 0.5F;
		float yOffset = 0.6F;
		float zOffset = 0.5F;

		setPos(cachedBedPos.getX() + xOffset, cachedBedPos.getY() + yOffset, cachedBedPos.getZ() + zOffset);
	}

	@Override
	public void travel(Vec3 travelVector) {
		if (isSleeping()) {
			jumping = false;
			xxa = 0.0F;
			zza = 0.0F;
			super.travel(Vec3.ZERO);
		} else {
			super.travel(travelVector);
		}
	}

	public boolean isBedCacheValid() {
		return (isUsableBed(level().getBlockState(cachedBedPos)));
	}

	@Override
	public void playerTouch(Player player) {
		if (!isSleeping()) {
			super.playerTouch(player);
		}
	}

	@Override
	public boolean isPushable() {
		return (!isSleeping());
	}

	@Override
	protected void doPush(Entity entity) {
		if (!isSleeping()) {
			super.doPush(entity);
		}
	}

	private void setSleeping(boolean isSleeping) {
		entityData.set(IS_SLEEPING, isSleeping);
	}

	@Override
	public boolean isSleeping() {
		return entityData == null ? false : entityData.get(IS_SLEEPING);
	}

	private void setBedDirection(Direction direction) {
		entityData.set(BED_DIRECTION, (byte) direction.ordinal());
	}

	@Override
	public Direction getBedOrientation() {
		return getBedDirection();
	}

	public Direction getBedDirection() {
		return Direction.values()[entityData.get(BED_DIRECTION)];
	}

	public boolean shouldSleep() {
		return !WorldTools.isDaytimeInDimension(level());
	}

	/*
	 * a bed of another mod (sleeping bag, hammock, bunk) can extend BedBlock with
	 * its own state definition; reading PART, OCCUPIED or FACING from it threw IllegalArgumentException in the NPC's
	 * tick. Such a block is not a bed for NPCs.
	 */
	static boolean isUsableBed(BlockState state) {
		return state.getBlock() instanceof BedBlock && state.hasProperty(BedBlock.PART) && state.hasProperty(BedBlock.OCCUPIED) && state.hasProperty(BedBlock.FACING);
	}

	// Only used by the renderer
	public float getBedOrientationInDegrees() {
		BlockPos bedLocation = getBedPosition();
		BlockState state = bedLocation == null ? null : level().getBlockState(bedLocation);
		if (state != null && state.isBed(level(), bedLocation, this)) {
			Direction enumfacing;
			try {
				enumfacing = state.getBedDirection(level(), bedLocation);
			}
			catch (RuntimeException e) {
				//a mod block that says it is a bed but has no FACING; drawn facing south
				return 0.0F;
			}
			if (enumfacing == null) {
				return 0.0F;
			}

			switch (enumfacing) {
				case SOUTH:
					return 90.0F;
				case WEST:
					return 0.0F;
				case NORTH:
					return 270.0F;
				case EAST:
					return 180.0F;
				default:
			}
		}

		return 0.0F;
	}

	public boolean isSwingingArms() {
		return entityData.get(SWINGING_ARMS);
	}

	public void setSwingingArms(boolean swingingArms) {
		entityData.set(SWINGING_ARMS, swingingArms);
	}

	/* 1.12 direct access to EntityLivingBase.activeItemStackUseCount (NpcAIBlockWithShield) */
	public void setUseItemRemaining(int ticks) {
		useItemRemaining = ticks;
	}

	public int getShieldDisabledTick() {
		return entityData.get(SHIELD_DISABLED_TICK);
	}

	public void setShieldDisabledTick(int count) {
		entityData.set(SHIELD_DISABLED_TICK, count);
	}

	public void decrementShieldDisabledTick() {
		entityData.set(SHIELD_DISABLED_TICK, (entityData.get(SHIELD_DISABLED_TICK)) - 1);
	}
	//TODO refactor vehicle stuff out - perhaps capability??

	@Nullable
	private IVehicle getRidingVehicle() {
		// TODO-CROSS(vehicle): NpcBase.getRidingVehicle (getRidingEntity() instanceof VehicleBase)
		return NpcXmod.vehicle().asVehicle(getVehicle());
	}

	// TODO-CROSS(vehicle): IPathableEntity - the methods below implemented vehicle.entity.IPathableEntity in 1.12
	public void setPath(List<?> path) {
		IVehicle vehicle = getRidingVehicle();
		if (vehicle != null) {
			// TODO-CROSS(vehicle): VehicleBase.nav.forcePath(List<Node>)
			vehicle.forcePath(path);
		} else {
			//TODO implement forcePath for NPC
			//nav.forcePath(path);
		}
	}

	public void setMoveTo(double x, double y, double z, float moveSpeed) {
		getMoveControl().setWantedPosition(x, y, z, moveSpeed);
	}

	public float getDefaultMoveSpeed() {
		return 1f;
	}

	public boolean isPathableEntityOnLadder() {
		return onClimbable();
	}

	public Entity getEntity() {
		return this;
	}

	public void onStuckDetected() {
		//noop
	}

	public boolean isFemale() {
		return false;
	}

	public NpcSkinSettings getSkinSettings() {
		return skinSettings;
	}

	public void setSkinSettings(NpcSkinSettings skinSettings) {
		this.skinSettings = skinSettings;
		this.skinSettings.onNpcSet(this);
	}

	public boolean isBow(Item item) {
		return item instanceof BowItem;
	}

	private NpcAIBlockWithShield initShieldAI() {
		if (shieldBlockAI == null) {
			shieldBlockAI = new NpcAIBlockWithShield(this);
		}
		return shieldBlockAI;
	}
}
