package net.shadowmage.ancientwarfare.structure.init;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.common.util.DeferredSoundType;

/*
 * Port notes: SoundType with registry-held sounds -> DeferredSoundType (the sound events are not registered yet when
 * this class loads).
 */
public class AWStructureSoundTypes {
	private AWStructureSoundTypes() {}

	public static final SoundType URN = new DeferredSoundType(1.0F, 1.0F, AWStructureSounds.URN_BREAK, () -> SoundEvents.STONE_STEP,
			() -> SoundEvents.STONE_PLACE, () -> SoundEvents.STONE_HIT, () -> SoundEvents.STONE_FALL);

	public static final SoundType COINSTACK = new DeferredSoundType(1.0F, 1.3F, AWStructureSounds.COIN_STACK_BREAK, AWStructureSounds.COIN_STACK_INTERACT,
			AWStructureSounds.COIN_STACK_INTERACT, AWStructureSounds.COIN_STACK_INTERACT, AWStructureSounds.COIN_STACK_BREAK);
}
