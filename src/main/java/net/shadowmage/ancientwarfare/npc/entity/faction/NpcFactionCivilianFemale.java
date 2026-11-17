package net.shadowmage.ancientwarfare.npc.entity.faction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class NpcFactionCivilianFemale extends NpcFactionCivilian {
	@SuppressWarnings("unused")
	public NpcFactionCivilianFemale(EntityType<? extends NpcFactionCivilianFemale> type, Level world) {
		super(type, world);
	}

	@SuppressWarnings("unused")
	public NpcFactionCivilianFemale(EntityType<? extends NpcFactionCivilianFemale> type, Level world, String factionName) {
		super(type, world, factionName);
	}

	@Override
	public String getNpcType() {
		return "civilian.female";
	}

	@Override
	public boolean isFemale() {
		return true;
	}
}
