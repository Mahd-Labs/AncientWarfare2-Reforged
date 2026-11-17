package net.shadowmage.ancientwarfare.vehicle.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.vehicle.AncientWarfareVehicles;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.missiles.MissileBase;

/*
 * Port notes: 1.12 AWEntityRegistry.registerEntity(VehiculeDeclaration): names "aw_vehicle" / "aw_missile"... the
 * 1.12 registry names were ancientwarfarevehicle:vehicle and ancientwarfarevehicle:missile; both paths are taken
 * by nothing else, so the port ids are ancientwarfare:vehicle and ancientwarfare:missile. Tracking range 120 blocks
 * -> 8 chunks, update frequency 3, velocity updates on. The registered size is the catapult stand's; a vehicle
 * reports the size of its type (VehicleBase.getDimensions). Missile size 0.4 x 0.4 (1.12 setSize).
 */
public class AWVehicleEntities {
	private AWVehicleEntities() {}

	private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, AncientWarfareVehicles.MOD_ID);

	public static final DeferredHolder<EntityType<?>, EntityType<VehicleBase>> VEHICLE = ENTITIES.register("vehicle",
			() -> EntityType.Builder.<VehicleBase>of(VehicleBase::new, MobCategory.MISC).sized(2F, 1.7F).clientTrackingRange(8).updateInterval(3)
					.setShouldReceiveVelocityUpdates(true).build("vehicle"));

	public static final DeferredHolder<EntityType<?>, EntityType<MissileBase>> MISSILE = ENTITIES.register("missile",
			() -> EntityType.Builder.<MissileBase>of(MissileBase::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(3)
					.setShouldReceiveVelocityUpdates(true).build("missile"));

	public static void register(IEventBus modBus) {
		ENTITIES.register(modBus);
	}
}
