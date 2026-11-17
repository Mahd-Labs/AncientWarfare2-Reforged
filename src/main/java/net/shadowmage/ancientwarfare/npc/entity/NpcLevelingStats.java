package net.shadowmage.ancientwarfare.npc.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.shadowmage.ancientwarfare.npc.config.AWNPCStatics;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.npc.registry.NpcDefaultsRegistry;

import java.util.HashMap;

public class NpcLevelingStats {

	private final HashMap<String, ExperienceEntry> experienceMap = new HashMap<>();
	private int xp;//'generic' xp, always incremented for all xp-types
	private int level;
	private final NpcBase npc;

	public NpcLevelingStats(NpcBase npc) {
		this.npc = npc;
	}

	public int getExperience() {
		String type = npc.getNpcFullType();
		if (experienceMap.containsKey(type)) {
			return experienceMap.get(type).xp;
		}
		return 0;
	}

	public int getLevel() {
		String type = npc.getNpcFullType();
		if (experienceMap.containsKey(type)) {
			return experienceMap.get(type).level;
		}
		return 0;
	}

	public int getBaseExperience() {
		return xp;
	}

	public int getBaseLevel() {
		return level;
	}

	public void addExperience(int xpGained) {
		if (npc.level().isClientSide) {
			return;
		}
		String type = npc.getNpcFullType();
		if (!experienceMap.containsKey(type)) {
			experienceMap.put(type, new ExperienceEntry());
		}
		ExperienceEntry entry = experienceMap.get(type);
		entry.xp += xpGained;
		while (entry.level < (npc instanceof NpcCombat ? AWNPCStatics.maxNpcCombatLevel : AWNPCStatics.maxNpcWorkerLevel) && entry.xp >= getXPToLevel(entry.level + 1)) {
			entry.xp -= getXPToLevel(entry.level + 1);
			entry.level++;
			onSubLevelGained(entry.level);
		}
		this.xp += xpGained;
		while (level < (npc instanceof NpcCombat ? AWNPCStatics.maxNpcCombatLevel : AWNPCStatics.maxNpcWorkerLevel) && this.xp >= getXPToLevel(level)) {
			this.xp -= getXPToLevel(level);
			onBaseLevelGained(level + 1);
		}
	}

	/* the highest level this npc can reach (config: combat or worker maximum) */
	public int getMaxLevel() {
		return npc instanceof NpcCombat ? AWNPCStatics.maxNpcCombatLevel : AWNPCStatics.maxNpcWorkerLevel;
	}

	/* experience needed for the next level of the current type, -1 at the highest level */
	public int getExperienceForNextLevel() {
		int current = getLevel();
		return current < getMaxLevel() ? getXPToLevel(current + 1) : -1;
	}

	/*
	 * Improvement (not baseline, config improvements.npc_level_editor): sets the level directly. The level of the
	 * current type and the general level both become newLevel (0 .. configured maximum) with no experience towards
	 * the next one, and the two methods a natural level-up runs are called, so health and attack damage follow.
	 */
	public void setLevel(int newLevel) {
		if (npc.level().isClientSide) {
			return;
		}
		newLevel = Math.max(0, Math.min(newLevel, getMaxLevel()));
		ExperienceEntry entry = experienceMap.computeIfAbsent(npc.getNpcFullType(), type -> new ExperienceEntry());
		entry.level = newLevel;
		entry.xp = 0;
		onSubLevelGained(newLevel);
		xp = 0;
		onBaseLevelGained(newLevel);
	}

	private void onBaseLevelGained(int newLevel) {
		level = newLevel;
		if (newLevel <= (npc instanceof NpcCombat ? AWNPCStatics.maxNpcCombatLevel : AWNPCStatics.maxNpcWorkerLevel)) {
			if (npc.getMaxHealthOverride() <= 0) {
				//TODO get rid of instanceof check once player owned attributes are migrated to similar structure to faction npcs
				double health;
				if (npc instanceof NpcFaction) {
					health = NpcDefaultsRegistry.getFactionNpcDefault((NpcFaction) npc).getBaseHealth() + newLevel;
				} else {
					health = NpcDefaultsRegistry.getOwnedNpcDefault((NpcPlayerOwned) npc).getBaseHealth() + newLevel;
				}
				npc.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
			}
			npc.updateDamageFromLevel();
		}
	}

	private void onSubLevelGained(int newLevel) {
		npc.updateDamageFromLevel();
	}

	private int getXPToLevel(int level) {
		return (level + 3) * (level + 3);
	}

	public CompoundTag writeToNBT(CompoundTag tag) {
		tag.putInt("xp", xp);
		tag.putInt("level", level);
		ListTag entryList = new ListTag();
		CompoundTag xpTag;
		for (String key : this.experienceMap.keySet()) {
			xpTag = new CompoundTag();
			xpTag.putString("type", key);
			xpTag.putInt("xp", experienceMap.get(key).xp);
			xpTag.putInt("level", experienceMap.get(key).level);
			entryList.add(xpTag);
		}
		tag.put("entryList", entryList);
		return tag;
	}

	public void readFromNBT(CompoundTag tag) {
		experienceMap.clear();
		xp = tag.getInt("xp");
		level = tag.getInt("level");
		ListTag entryList = tag.getList("entryList", Tag.TAG_COMPOUND);
		CompoundTag xpTag;
		ExperienceEntry entry;
		for (int i = 0; i < entryList.size(); i++) {
			xpTag = entryList.getCompound(i);
			entry = new ExperienceEntry();
			entry.xp = xpTag.getInt("xp");
			entry.level = xpTag.getInt("level");
			experienceMap.put(xpTag.getString("type"), entry);
		}
	}

	private class ExperienceEntry {
		int xp;
		int level;
	}

}
