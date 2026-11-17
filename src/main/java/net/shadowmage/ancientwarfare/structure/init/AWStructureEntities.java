package net.shadowmage.ancientwarfare.structure.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;
import net.shadowmage.ancientwarfare.structure.entity.EntityGate;
import net.shadowmage.ancientwarfare.structure.entity.EntitySeat;

/*
 * Port notes: 1.12 EntityRegistry.registerModEntity in AncientWarfareStructure.preInit:
 *   aw_gate: tracking range 250 blocks, update frequency 200, no velocity updates
 *   seat ("AWSeat"): tracking range 20 blocks, update frequency 10, no velocity updates
 * Tracking ranges are in chunks in 1.21 (250 blocks -> 16, 20 blocks -> 2). The gate keeps the 1.12 default entity
 * size (0.6 x 1.8); its real bounding box comes from the gate type. The seat is 0.01 x 0.01 (1.12 setSize).
 */
public class AWStructureEntities {
	private AWStructureEntities() {}

	private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, AncientWarfareStructure.MOD_ID);

	public static final DeferredHolder<EntityType<?>, EntityType<EntityGate>> GATE = ENTITIES.register("aw_gate",
			() -> EntityType.Builder.<EntityGate>of(EntityGate::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(16).updateInterval(200)
					.setShouldReceiveVelocityUpdates(false).build("aw_gate"));

	public static final DeferredHolder<EntityType<?>, EntityType<EntitySeat>> SEAT = ENTITIES.register("seat",
			() -> EntityType.Builder.<EntitySeat>of(EntitySeat::new, MobCategory.MISC).sized(0.01F, 0.01F).clientTrackingRange(2).updateInterval(10)
					.setShouldReceiveVelocityUpdates(false).build("seat"));

	public static void register(IEventBus modBus) {
		ENTITIES.register(modBus);
	}
}
