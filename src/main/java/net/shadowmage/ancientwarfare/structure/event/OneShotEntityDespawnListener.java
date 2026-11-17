package net.shadowmage.ancientwarfare.structure.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.structure.util.CapabilityRespawnData;
import net.shadowmage.ancientwarfare.structure.util.IRespawnData;
import net.shadowmage.ancientwarfare.structure.util.SpawnerHelper;

/*
 * Port notes: 1.12 IWorldEventListener.onEntityRemoved (added to every world on load) -> EntityLeaveLevelEvent.
 * Deviation: only entities that were discarded (despawned / removed) put their one shot spawner back. 1.12 also did
 * it when the entity left with an unloading chunk; placing a block from a chunk unload is not safe in 1.21.
 */
public class OneShotEntityDespawnListener {
	public static final OneShotEntityDespawnListener INSTANCE = new OneShotEntityDespawnListener();

	private OneShotEntityDespawnListener() {}

	@SubscribeEvent
	public void onEntityRemoved(EntityLeaveLevelEvent evt) {
		Entity entityIn = evt.getEntity();
		if (entityIn.level().isClientSide || entityIn.getRemovalReason() != Entity.RemovalReason.DISCARDED) {
			return;
		}
		if (CapabilityRespawnData.has(entityIn)) {
			IRespawnData respawnData = CapabilityRespawnData.get(entityIn).orElse(null);
			if (respawnData != null && respawnData.canRespawn() && ((LivingEntity) entityIn).getHealth() > 0 && !(entityIn instanceof NpcFaction)) { //NpcFaction is handled separately
				SpawnerHelper.createSpawner(respawnData, entityIn.level());
			}
		}
	}
}
