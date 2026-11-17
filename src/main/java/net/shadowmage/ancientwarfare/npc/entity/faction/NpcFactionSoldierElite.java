package net.shadowmage.ancientwarfare.npc.entity.faction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class NpcFactionSoldierElite extends NpcFactionSoldier {
	@SuppressWarnings("unused")
	public NpcFactionSoldierElite(EntityType<? extends NpcFactionSoldierElite> type, Level world) {
		super(type, world);
	}

	@SuppressWarnings("unused")
	public NpcFactionSoldierElite(EntityType<? extends NpcFactionSoldierElite> type, Level world, String factionName) {
		super(type, world, factionName);
	}

	@Override
	public String getNpcType() {
		return super.getNpcType() + ".elite";
	}
}
