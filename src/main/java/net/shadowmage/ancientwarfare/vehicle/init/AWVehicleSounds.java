package net.shadowmage.ancientwarfare.vehicle.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.vehicle.AncientWarfareVehicles;

/*
 * Port notes: @ObjectHolder fields -> DeferredHolder constants (call .get()); ids keep their 1.12 path in the
 * "ancientwarfare" namespace. Not ported: battering_ram_* (battering ram dropped) and vehicle_moving (driving
 * dropped; it was registered but never played in 1.12).
 */
public class AWVehicleSounds {
	private AWVehicleSounds() {
	}

	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, AncientWarfareVehicles.MOD_ID);

	public static final DeferredHolder<SoundEvent, SoundEvent> BALLISTA_BOLT_HIT_ENTITY = createSoundEvent("ballista_bolt_hit_entity");
	public static final DeferredHolder<SoundEvent, SoundEvent> BALLISTA_BOLT_HIT_GROUND = createSoundEvent("ballista_bolt_hit_ground");
	public static final DeferredHolder<SoundEvent, SoundEvent> BALLISTA_LAUNCH = createSoundEvent("ballista_launch");
	public static final DeferredHolder<SoundEvent, SoundEvent> BALLISTA_RELOAD = createSoundEvent("ballista_reload");
	public static final DeferredHolder<SoundEvent, SoundEvent> CATAPULT_LAUNCH = createSoundEvent("catapult_launch");
	public static final DeferredHolder<SoundEvent, SoundEvent> CATAPULT_RELOAD = createSoundEvent("catapult_reload");
	public static final DeferredHolder<SoundEvent, SoundEvent> GIANT_TREBUCHET_LAUNCH = createSoundEvent("giant_trebuchet_launch");
	public static final DeferredHolder<SoundEvent, SoundEvent> TREBUCHET_LAUNCH = createSoundEvent("trebuchet_launch");
	public static final DeferredHolder<SoundEvent, SoundEvent> TREBUCHET_RELOAD = createSoundEvent("trebuchet_reload");

	private static DeferredHolder<SoundEvent, SoundEvent> createSoundEvent(String soundName) {
		ResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(AncientWarfareVehicles.MOD_ID, soundName);
		return SOUNDS.register(soundName, () -> SoundEvent.createVariableRangeEvent(registryName));
	}

	public static void register(IEventBus modBus) {
		SOUNDS.register(modBus);
	}
}
