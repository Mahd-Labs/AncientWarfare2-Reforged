package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

/*
 * Port addition (licensing, release candidate): sound files of third-party origin were removed, but their sound events
 * stay registered and keep their sounds.json entries (an empty "sounds" list), so worlds, templates and sound blocks that
 * name them still load. Vanilla would log "Unable to play empty soundEvent" once per such event when one is played; this
 * client-side filter drops an "ancientwarfare" sound whose event has no sound files left before the sound engine sees it,
 * so it is silent without a log line. Events that still have files, and every other mod's sounds, are not touched.
 * List of the removed files: tools/licensing_dropped_sounds.txt.
 */
public final class SilentSoundFilter {
	private SilentSoundFilter() {}

	public static void onPlaySound(PlaySoundEvent event) {
		SoundInstance sound = event.getSound();
		if (sound == null) {
			return;
		}
		ResourceLocation location = sound.getLocation();
		if (!AncientWarfareCore.MOD_ID.equals(location.getNamespace())) {
			return;
		}
		WeighedSoundEvents events = Minecraft.getInstance().getSoundManager().getSoundEvent(location);
		if (events != null && events.getWeight() == 0) {
			event.setSound(null);
		}
	}
}
