package net.shadowmage.ancientwarfare.npc.entity.faction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class NpcFactionLeaderElite extends NpcFactionLeader {
	@SuppressWarnings("unused")
	public NpcFactionLeaderElite(EntityType<? extends NpcFactionLeaderElite> type, Level world) {
		super(type, world);
	}

	@SuppressWarnings("unused")
	public NpcFactionLeaderElite(EntityType<? extends NpcFactionLeaderElite> type, Level world, String factionName) {
		super(type, world, factionName);
	}

	@Override
	public String getNpcType() {
		return super.getNpcType() + ".elite";
	}
}
