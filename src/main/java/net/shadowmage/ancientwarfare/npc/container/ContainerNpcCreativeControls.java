package net.shadowmage.ancientwarfare.npc.container;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.entity.NpcBase;
import net.shadowmage.ancientwarfare.npc.entity.NpcLevelingStats;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.npc.skin.NpcSkinSettings;

public class ContainerNpcCreativeControls extends ContainerNpcBase<NpcBase> implements ISkinSettingsContainer {

	public String ownerName;//allow for editing owner name for player-owned, no effect on faction-owned
	public boolean wander;//temp flag in all npcs
	public boolean hasCustomEquipment;//faction based only
	public int maxHealth;
	public int attackDamage;//faction based only
	public int armorValue;//faction based only
	public NpcSkinSettings skinSettings;

	//improvement (not baseline, config improvements.npc_level_editor): level of the type the npc has now; -1 = not shown (1.12 screen)
	public static final String LEVEL_INFO_TAG = "levelInfo";
	public static final String SET_LEVEL_TAG = "setLevel";
	public int level = -1;
	public int levelExperience;
	public int levelExperienceNeeded = -1;//-1 at the highest level
	public int maxLevel;
	public String levelType = "";
	public int levelToSet = -1;//what the level field of the screen holds
	private CompoundTag pendingLevel;//the data that asks for a level, applied on container close like every other value

	private boolean hasChanged;//if set to true, will set all flags to entity on container close

	public ContainerNpcCreativeControls(Player player, int x, int y, int z) {
		super(player, x);
		ownerName = entity.getOwner().getName();
		skinSettings = entity.getSkinSettings();
		wander = entity.getIsAIEnabled();
		maxHealth = entity.getMaxHealthOverride();
		attackDamage = entity.getAttackDamageOverride();
		armorValue = entity.getArmorValueOverride();
		hasCustomEquipment = entity.getCustomEquipmentOverride();
		if (!player.level().isClientSide && AWNPCStatics.improvedNpcLevelEditor) {
			NpcLevelingStats stats = entity.getLevelingStats();
			level = levelToSet = stats.getLevel();
			levelExperience = stats.getExperience();
			levelExperienceNeeded = stats.getExperienceForNextLevel();
			maxLevel = stats.getMaxLevel();
			levelType = entity.getNpcFullType();
		}
	}

	public void sendChangesToServer() {
		sendDataToServer(changesTag());
	}

	/* what the screen sends when it closes: the 1.12 values, and the level only when the level field was changed */
	public CompoundTag changesTag() {
		CompoundTag tag = serializeContainerData();
		if (level >= 0 && levelToSet != level) {
			tag.putInt(SET_LEVEL_TAG, Math.max(0, Math.min(levelToSet, maxLevel)));
		}
		return tag;
	}

	/*
	 * Server side of the level field. Only for a creative player and only with the option on; anything else changes
	 * nothing. The level is set the way a level-up sets it (NpcLevelingStats.setLevel); an attack damage override
	 * that is set stays in force afterwards.
	 */
	public static boolean applyLevel(Player player, NpcBase npc, CompoundTag tag) {
		if (!AWNPCStatics.improvedNpcLevelEditor || player.level().isClientSide || !player.isCreative() || !tag.contains(SET_LEVEL_TAG, Tag.TAG_INT)) {
			return false;
		}
		npc.getLevelingStats().setLevel(tag.getInt(SET_LEVEL_TAG));
		if (npc.getAttackDamageOverride() >= 0) {
			npc.setAttackDamageOverride(npc.getAttackDamageOverride());
		}
		return true;
	}

	private CompoundTag serializeContainerData() {
		CompoundTag tag = new CompoundTag();
		tag.putString("ownerName", ownerName);
		tag.put("skinSettings", skinSettings.serializeNBT());
		tag.putBoolean("wander", wander);
		tag.putInt("maxHealth", maxHealth);
		tag.putInt("attackDamage", attackDamage);
		tag.putInt("armorValue", armorValue);
		tag.putBoolean("hasCustomEquipment", hasCustomEquipment);
		return tag;
	}

	@Override
	public void sendInitData() {
		CompoundTag tag = serializeContainerData();
		if (level >= 0) {
			CompoundTag info = new CompoundTag();
			info.putInt("level", level);
			info.putInt("xp", levelExperience);
			info.putInt("xpNeeded", levelExperienceNeeded);
			info.putInt("maxLevel", maxLevel);
			info.putString("type", levelType);
			tag.put(LEVEL_INFO_TAG, info);
		}
		sendDataToClient(tag);
	}

	@Override
	public void handlePacketData(CompoundTag tag) {
		if (!player.level().isClientSide && !player.isCreative()) {
			return; //creative controls
		}
		ownerName = tag.getString("ownerName");
		wander = tag.getBoolean("wander");
		hasCustomEquipment = tag.getBoolean("hasCustomEquipment");
		attackDamage = tag.getInt("attackDamage");
		armorValue = tag.getInt("armorValue");
		maxHealth = tag.getInt("maxHealth");
		skinSettings = NpcSkinSettings.deserializeNBT(tag.getCompound("skinSettings"));
		entity.setSkinSettings(skinSettings);
		if (player.level().isClientSide) {
			if (tag.contains(LEVEL_INFO_TAG, Tag.TAG_COMPOUND)) {
				CompoundTag info = tag.getCompound(LEVEL_INFO_TAG);
				level = levelToSet = info.getInt("level");
				levelExperience = info.getInt("xp");
				levelExperienceNeeded = info.getInt("xpNeeded");
				maxLevel = info.getInt("maxLevel");
				levelType = info.getString("type");
			}
		} else if (tag.contains(SET_LEVEL_TAG, Tag.TAG_INT)) {
			pendingLevel = tag;
		}
		hasChanged = true;
		refreshGui();
	}

	@Override
	public void onContainerClosed(Player par1EntityPlayer) {
		if (hasChanged && !player.level().isClientSide) {
			hasChanged = false;
			entity.setOwnerName(ownerName);
			entity.setSkinSettings(skinSettings.minimizeData());
			entity.setAttackDamageOverride(attackDamage);
			entity.setArmorValueOverride(armorValue);
			entity.setIsAIEnabled(wander);
			entity.setMaxHealthOverride(maxHealth);
			entity.setCustomEquipmentOverride(hasCustomEquipment);
			if (pendingLevel != null) {
				applyLevel(player, entity, pendingLevel);
				pendingLevel = null;
			}
		}
		super.onContainerClosed(par1EntityPlayer);
	}

	@Override
	public void handleNpcSkinUpdate() {
		sendDataToServer("skinSettings", skinSettings.serializeNBT());
	}

	@Override
	public NpcSkinSettings getSkinSettings() {
		return skinSettings;
	}

	public boolean isFactionNpc() {
		return entity instanceof NpcFaction;
	}

	@Override
	public void setSkinSettings(NpcSkinSettings skinSettings) {
		this.skinSettings = skinSettings;
	}
}
