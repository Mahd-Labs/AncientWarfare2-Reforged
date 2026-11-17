package net.shadowmage.ancientwarfare.npc.entity.faction;

import com.google.common.primitives.Floats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.shadowmage.ancientwarfare.npc.ai.AIHelper;
import net.shadowmage.ancientwarfare.npc.ai.faction.NpcAIFactionFleeSun;
import net.shadowmage.ancientwarfare.npc.ai.faction.NpcAIFactionRestrictSun;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.NpcPlayerOwned;
import net.shadowmage.ancientwarfare.npc.entity.faction.attributes.AdditionalAttributes;
import net.shadowmage.ancientwarfare.npc.entity.faction.attributes.IAdditionalAttribute;
import net.shadowmage.ancientwarfare.npc.faction.FactionTracker;
import net.shadowmage.ancientwarfare.npc.init.AWNPCSounds;
import net.shadowmage.ancientwarfare.npc.registry.FactionNpcDefault;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;
import net.shadowmage.ancientwarfare.npc.registry.NpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.npc.registry.StandingChanges;
import net.shadowmage.ancientwarfare.npc.xmod.NpcXmod;
import org.apache.commons.lang3.Range;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/*
 * Port notes:
 * - 1.12 had a (World) constructor that left the faction unset (it was always followed by readEntityFromNBT) and a
 *   (World, String faction) constructor. Every 1.21.1 entity must be creatable from its type alone ("/summon" without
 *   NBT, EntityType.create), so the (type, level) constructor applies the name and defaults of a default faction
 *   (getDefaultFactionName(): the first faction name in alphabetical order); reading NBT with a faction name replaces
 *   it exactly as 1.12 did.
 * - despawnEntity -> checkDespawn, canDespawn -> removeWhenFarAway.
 * - EnumCreatureAttribute.UNDEAD is a property of the entity type (tags) in 1.21.1 and cannot depend on the faction;
 *   the per-entity part that can be kept is isInvertedHealAndHarm (healing / harming potions). Smite does not apply.
 * - width / height (1.12 setSize) are the size override of NpcBase; clients get them in the spawn data as before.
 * - the 1.12 poison immunity check compared the faction name with the literal "lizardman|coven" and therefore never
 *   matched; it is left out (no behaviour change).
 */
public abstract class NpcFaction extends NpcBase {
	private static final int DEATH_REVENGE_TICKS = 6000;
	private static final int HIT_REVENGE_TICKS = DEATH_REVENGE_TICKS / 20;
	private static final int REVENGE_LIST_VALIDATION_TICKS = DEATH_REVENGE_TICKS / 100;
	private static final double REVENGE_SET_RANGE = 50D;
	private static final String HEIGHT_TAG = "height";
	/*
	 * Port addition: "awApplyDefaults:1b" in the NBT makes the npc take the equipment / size defaults of its
	 * faction even though the tag carries a height (a tag merged over an existing npc always does). Used by the dev
	 * showcase variants; never written.
	 */
	public static final String APPLY_DEFAULTS_TAG = "awApplyDefaults";
	private static final EntityDataAccessor<String> SOUND = SynchedEntityData.defineId(NpcFaction.class, EntityDataSerializers.STRING);

	protected String factionName;
	private Map<String, Long> revengePlayers = new HashMap<>();

	public NpcFaction(EntityType<? extends NpcFaction> type, Level world) {
		this(type, world, getDefaultFactionName());
	}

	public NpcFaction(EntityType<? extends NpcFaction> type, Level world, String factionName) {
		super(type, world);
		setFactionNameAndDefaults(factionName);
		addAI();
	}

	public static String getDefaultFactionName() {
		return FactionRegistry.getFactionNames().stream().sorted().findFirst().orElse("");
	}

	public static AttributeSupplier.Builder createAttributes() {
		return NpcBase.createAttributes();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SOUND, "none");
	}

	private final Map<IAdditionalAttribute<?>, Object> additionalAttributes = new HashMap<>();
	private boolean canDespawn = false;

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return canDespawn;
	}

	private void setDeathRevengePlayer(String playerName) {
		revengePlayers.put(playerName, level().getGameTime() + DEATH_REVENGE_TICKS);
	}

	@Override
	public void checkDespawn() {
		boolean deadPreDespawn = isRemoved();

		super.checkDespawn();

		if (!deadPreDespawn && isRemoved()) {
			// TODO-CROSS(structure): NpcFaction.despawnEntity - CapabilityRespawnData.RESPAWN_DATA_CAPABILITY + SpawnerHelper.createSpawner(respawnData, level())
			NpcXmod.structure().createRespawnSpawner(this, level());
		}
	}

	public void setCanDespawn() {
		canDespawn = true;
	}

	private void addAI() {
		goalSelector.addGoal(2, new NpcAIFactionRestrictSun(this));
		goalSelector.addGoal(3, new NpcAIFactionFleeSun(this, 1.0D));
	}

	public void setAdditionalAttribute(IAdditionalAttribute<?> attribute, Object value) {
		additionalAttributes.put(attribute, value);
	}

	public <T> Optional<T> getAdditionalAttributeValue(IAdditionalAttribute<T> attribute) {
		return Optional.ofNullable(attribute.getValueClass().cast(additionalAttributes.get(attribute)));
	}

	public boolean burnsInSun() {
		return getAdditionalAttributeValue(AdditionalAttributes.BURNS_IN_SUN).orElse(false);
	}

	public boolean isUndead() {
		return getAdditionalAttributeValue(AdditionalAttributes.UNDEAD).orElse(false);
	}

	@Override
	public void aiStep() {
		doSunBurn();
		super.aiStep();
	}

	private void doSunBurn() {
		if (burnsInSun() && level().isDay() && !level().isClientSide) {
			float brightness = getLightLevelDependentMagicValue();
			BlockPos blockpos = getVehicle() instanceof Boat ? (BlockPos.containing(getX(), (double) Math.round(getY()), getZ())).above() : BlockPos.containing(getX(), Math.round(getY()), getZ());

			if (brightness > 0.5F && random.nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F && level().canSeeSky(blockpos)) {
				ItemStack helmet = getItemBySlot(EquipmentSlot.HEAD);
				if (helmet.isEmpty()) {
					igniteForSeconds(8);
				} else {
					damageHelmet(helmet);
				}
			}
		}
	}

	private void damageHelmet(ItemStack helmet) {
		if (helmet.isDamageableItem()) {
			helmet.setDamageValue(helmet.getDamageValue() + random.nextInt(2));

			if (helmet.getDamageValue() >= helmet.getMaxDamage()) {
				onEquippedItemBroken(helmet.getItem(), EquipmentSlot.HEAD);
				setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			}
		}
	}

	/*
	 * 1.12 getCreatureAttribute() == UNDEAD, as far as it is a per entity property in 1.21.1
	 */
	@Override
	public boolean isInvertedHealAndHarm() {
		return isUndead() || super.isInvertedHealAndHarm();
	}

	@Override
	public boolean canBeAffected(MobEffectInstance potioneffectIn) {
		return super.canBeAffected(potioneffectIn);
	}

	public void setFactionNameAndDefaults(String factionName) {
		this.factionName = factionName;
		FactionNpcDefault npcDefault = NpcDefaultsRegistry.getFactionNpcDefault(this);
		applyFactionNpcSettings(npcDefault);
		// do not apply the default equipment if the hasCustomEquipment tag was set to true
		// Port notes: server only - the client is told the equipment, and the initial equipment packet lists only the
		// non-empty slots, so anything applied here on the client would stay in slots that are empty on the server
		if (!getCustomEquipmentOverride() && !level().isClientSide) {
			npcDefault.applyEquipment(this);
		}

		if (AWNPCStatics.vanillaEquipmentDropRate) {
			// necessary to reset this to the default values as many old structures had NPCs scanned with 1.0f drop rates
			Arrays.fill(armorDropChances, 0.085f);
			Arrays.fill(handDropChances, 0.085f);
		} else {
			// makes faction NPCs drop all their items otherwise use the default vanilla drop rate
			Arrays.fill(armorDropChances, 1.f);
			Arrays.fill(handDropChances, 1.f);
		}

		Range<Float> heightRange = npcDefault.getHeightRange();
		float newHeight = heightRange.getMinimum() + level().random.nextFloat() * (heightRange.getMaximum() - heightRange.getMinimum());
		float newWidth = (newHeight / 1.8f) * 0.6f * npcDefault.getThinness();
		setSize(newWidth, newHeight);
	}

	@Override
	protected void setSize(float width, float height) {
		super.setSize(Floats.constrainToRange(width, 0.1f, 10f), Floats.constrainToRange(height, 0.1f, 40f));
	}

	private void applyFactionNpcSettings(FactionNpcDefault npcDefault) {
		npcDefault.applyAttributes(this);
		npcDefault.applyAdditionalAttributes(this);
		xpReward = npcDefault.getExperienceDrop();
		npcDefault.applyPathSettings((GroundPathNavigation) getNavigation());
		String sound = getAdditionalAttributeValue(AdditionalAttributes.ENTITY_SOUND).isPresent() ? getAdditionalAttributeValue(AdditionalAttributes.ENTITY_SOUND).get() : "none";
		setSound(sound);
	}

	@Override
	public int getMaxFallDistance() {
		int i = super.getMaxFallDistance();
		if (i > 4) {
			i += level().getDifficulty().getId() * getMaxHealth() / 5;
		}
		if (i >= getHealth()) {
			return (int) getHealth();
		}
		return i;
	}

	@Override
	protected boolean tryCommand(Player player) {
		return player.getAbilities().instabuild && super.tryCommand(player);
	}

	@Override
	public boolean hasCommandPermissions(UUID playerUuid, String playerName) {
		return false;
	}

	@Override
	public Component getName() {
		Component name = Component.translatable("entity.ancientwarfarenpc." + getNpcFullType() + ".name");
		if (hasCustomName()) {
			name = Component.literal(getCustomName().getString());
		}
		return name;
	}

	@Override
	protected float getLitBlockWeight(BlockPos pos) {
		return burnsInSun() ? 1F - level().getLightLevelDependentMagicValue(pos) : super.getLitBlockWeight(pos);
	}

	@Override
	public String getNpcFullType() {
		return factionName + "." + super.getNpcFullType();
	}

	@Override
	public boolean isHostileTowards(Entity e) {
		if (e instanceof Player || e instanceof NpcPlayerOwned) {
			String playerName = e instanceof Player ? e.getName().getString() : ((NpcBase) e).getOwner().getName();
			UUID playerUUID = e instanceof Player ? e.getUUID() : ((NpcBase) e).getOwner().getUUID();
			return revengePlayers.containsKey(playerName) || FactionTracker.INSTANCE.isHostileToPlayer(level(), playerUUID, playerName, getFaction());
		} else if (e instanceof NpcFaction) {
			NpcFaction npc = (NpcFaction) e;
			return !npc.getFaction().equals(factionName) && FactionRegistry.getFaction(getFaction()).isHostileTowards(npc.getFaction());
		} else {
			return FactionRegistry.getFaction(factionName).isTarget(e) || AIHelper.isAdditionalEntityToTarget(e);
		}
	}

	/*
	 * 1.12 onEntityUpdate
	 */
	@Override
	public void baseTick() {
		super.baseTick();
		if (level().getDayTime() % REVENGE_LIST_VALIDATION_TICKS == 0) {
			Iterator<Map.Entry<String, Long>> it = revengePlayers.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Long> playerRevengeTime = it.next();
				if (level().getGameTime() > playerRevengeTime.getValue()) {
					it.remove();
					setTarget(null);
					setLastHurtByMob(null);
				}
			}
		}

	}

	private String getSound() {
		return entityData.get(SOUND);
	}

	public void setSound(String sound) {
		entityData.set(SOUND, sound);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return getSound().equals("none") ? SoundEvents.GENERIC_HURT : AWNPCSounds.getSoundEventFromString(getSound() + "_hurt");
	}

	@Override
	protected SoundEvent getDeathSound() {
		return getSound().equals("none") ? SoundEvents.GENERIC_DEATH : AWNPCSounds.getSoundEventFromString(getSound() + "_death");
	}

	/* 1.12 playAttackSound (renamed: Mob.playAttackSound exists in 1.21.1 and is called on every successful hit) */
	private void playFactionAttackSound() {
		// don't play any sound if there is no specific attack sound
		if (!getSound().equals("none")) {
			SoundEvent sound = AWNPCSounds.getSoundEventFromString(getSound() + "_attack");
			if (sound != null) {
				playSound(sound, 1.0F, 1.2F / (random.nextFloat() * 0.2F + 0.9F));
			}
		}
	}

	/*
	 * 1.12 damageEntity
	 */
	@Override
	protected void actuallyHurt(DamageSource damageSrc, float damageAmount) {
		super.actuallyHurt(damageSrc, damageAmount);

		if (damageSrc.getMsgId().equals("player") && damageSrc.getEntity() != null) {
			revengePlayers.put(damageSrc.getEntity().getName().getString(), level().getGameTime() + HIT_REVENGE_TICKS);
		} else if ((damageSrc.getMsgId().equals("mob") && damageSrc.getEntity() instanceof NpcBase)) {
			revengePlayers.put(((NpcBase) damageSrc.getEntity()).getOwner().getName(), level().getGameTime() + HIT_REVENGE_TICKS);
		}
	}

	@Override
	public boolean canTarget(Entity e) {
		if (e instanceof NpcFaction) {
			return !((NpcFaction) e).getFaction().equals(getFaction());
		}
		return e instanceof LivingEntity;
	}

	@Override
	public boolean canBeAttackedBy(Entity e) {
		//can only be attacked by other factions, not your own...disable friendly fire
		return !(e instanceof NpcFaction) || !getFaction().equals(((NpcFaction) e).getFaction());
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		if ((Math.random() < 0.2)) {
			playFactionAttackSound();
		}
		return super.doHurtTarget(target);
	}

	@Override
	public void die(DamageSource damageSource) {
		super.die(damageSource);
		if (level().isClientSide) {
			return;
		}
		if (damageSource.getEntity() instanceof Player || damageSource.getEntity() instanceof NpcPlayerOwned) {
			String playerName = damageSource.getEntity() instanceof Player ? damageSource.getEntity().getName().getString() :
					((NpcBase) damageSource.getEntity()).getOwner().getName();
			FactionTracker.INSTANCE.adjustStandingFor(level(), playerName, getFaction(), FactionRegistry.getFaction(getFaction()).getStandingSettings().getStandingChange(StandingChanges.KILL));

			setDeathRevengePlayer(playerName);
			level().getEntitiesOfClass(NpcFaction.class, new AABB(blockPosition()).inflate(REVENGE_SET_RANGE))
					.forEach(factionNpc -> {
						if (factionNpc.getFaction().equals(getFaction())) {
							factionNpc.setDeathRevengePlayer(playerName);
						}
					});
		}
	}

	@Override
	protected ResourceKey<LootTable> getDefaultLootTable() {
		ResourceLocation lootTable = NpcDefaultsRegistry.getFactionNpcDefault(this).getLootTable();
		return lootTable == null ? super.getDefaultLootTable() : ResourceKey.create(Registries.LOOT_TABLE, lootTable);
	}

	@Override
	public String getNpcSubType() {
		return "";
	}

	public String getFaction() {
		return factionName;
	}

	@Override
	public PlayerTeam getTeam() {
		return null;
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeUtf(factionName);
		buffer.writeFloat(getBbHeight());
		buffer.writeFloat(getBbWidth());
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		super.readSpawnData(buffer);
		factionName = buffer.readUtf(20);
		float height = buffer.readFloat();
		float width = buffer.readFloat();
		setSize(width, height);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (!tag.contains("factionName") || !FactionRegistry.getFactionNames().contains(tag.getString("factionName"))) {
			//no (or an unknown) faction: e.g. "/summon" without NBT - keep the default faction of the constructor and
			//apply its defaults again (NpcBase cleared the equipment before reading), as 1.12 did for tags without height
			setFactionNameAndDefaults(factionName);
			return;
		}
		factionName = tag.getString("factionName");
		applyFactionNpcSettings(NpcDefaultsRegistry.getFactionNpcDefault(this));
		canDespawn = tag.getBoolean("canDespawn");
		revengePlayers = NBTHelper.getMap(tag.getList("revengePlayers", Tag.TAG_COMPOUND),
				t -> t.getString("playerName"), t -> t.getLong("time"));
		if (tag.contains(HEIGHT_TAG) && !tag.getBoolean(APPLY_DEFAULTS_TAG)) {
			setSize(tag.getFloat("width"), tag.getFloat(HEIGHT_TAG));
		} else {
			setFactionNameAndDefaults(factionName);
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (factionName != null) {
			tag.putString("factionName", factionName);
		}
		tag.putBoolean("canDespawn", canDespawn);
		tag.put("revengePlayers", NBTHelper.mapToCompoundList(revengePlayers,
				(t, playerName) -> t.putString("playerName", playerName), (t, time) -> t.putLong("time", time)));
		tag.putFloat(HEIGHT_TAG, getBbHeight());
		tag.putFloat("width", getBbWidth());
	}

	@Override
	public float getRenderSizeModifier() {
		return getBbHeight() / 1.8f;
	}

	public float getWidthModifier() {
		return getBbWidth() / 0.6f;
	}
}
