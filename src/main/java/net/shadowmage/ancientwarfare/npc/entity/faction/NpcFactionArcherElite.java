package net.shadowmage.ancientwarfare.npc.entity.faction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class NpcFactionArcherElite extends NpcFactionArcher {
	@SuppressWarnings("unused")
	public NpcFactionArcherElite(EntityType<? extends NpcFactionArcherElite> type, Level world) {
		super(type, world);
	}

	@SuppressWarnings("unused")
	public NpcFactionArcherElite(EntityType<? extends NpcFactionArcherElite> type, Level world, String factionName) {
		super(type, world, factionName);
	}

	@Override
	public String getNpcType() {
		return super.getNpcType() + ".elite";
	}
}
