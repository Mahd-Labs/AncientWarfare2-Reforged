package net.shadowmage.ancientwarfare.npc.entity.faction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class NpcFactionCivilianMale extends NpcFactionCivilian {
	@SuppressWarnings("unused")
	public NpcFactionCivilianMale(EntityType<? extends NpcFactionCivilianMale> type, Level world) {
		super(type, world);
	}

	@SuppressWarnings("unused")
	public NpcFactionCivilianMale(EntityType<? extends NpcFactionCivilianMale> type, Level world, String factionName) {
		super(type, world, factionName);
	}

	@Override
	public String getNpcType() {
		return "civilian.male";
	}
}
