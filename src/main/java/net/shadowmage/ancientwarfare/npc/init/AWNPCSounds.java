package net.shadowmage.ancientwarfare.npc.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.npc.AncientWarfareNPC;

import java.util.HashMap;
import java.util.Map;

/*
 * Port notes: generated from the 1.12 class by tools/npc_gen_sounds.py. @ObjectHolder fields -> DeferredHolder
 * constants (one per registered sound, call .get()); ids keep their 1.12 path in the "ancientwarfare" namespace.
 * npcSoundEvents maps the sound name to its holder; getSoundEventFromString still returns the SoundEvent (or null).
 */
@SuppressWarnings("unused")
public class AWNPCSounds {
	private AWNPCSounds() {}

	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, AncientWarfareNPC.MOD_ID);

	public static Map<String, DeferredHolder<SoundEvent, SoundEvent>> npcSoundEvents = new HashMap<>();

	public static final DeferredHolder<SoundEvent, SoundEvent> BARD_TUNE1 = createSoundEvent("bard.tune1");
	public static final DeferredHolder<SoundEvent, SoundEvent> BARD_TUNE2 = createSoundEvent("bard.tune2");
	public static final DeferredHolder<SoundEvent, SoundEvent> BARD_TUNE3 = createSoundEvent("bard.tune3");
	public static final DeferredHolder<SoundEvent, SoundEvent> BARD_TUNE4 = createSoundEvent("bard.tune4");
	public static final DeferredHolder<SoundEvent, SoundEvent> BARD_TUNE5 = createSoundEvent("bard.tune5");
	public static final DeferredHolder<SoundEvent, SoundEvent> BARD_TUNE6 = createSoundEvent("bard.tune6");
	public static final DeferredHolder<SoundEvent, SoundEvent> TELEPORT_IN = createSoundEvent("teleport_in");
	public static final DeferredHolder<SoundEvent, SoundEvent> TELEPORT_OUT = createSoundEvent("teleport_out");
	public static final DeferredHolder<SoundEvent, SoundEvent> BARBARIAN_ATTACK = createSoundEvent("barbarian_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> BARBARIAN_HURT = createSoundEvent("barbarian_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> BEAST_ATTACK = createSoundEvent("beast_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> BEAST_DEATH = createSoundEvent("beast_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> BEAST_HURT = createSoundEvent("beast_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> BRIGAND_ATTACK = createSoundEvent("brigand_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> BRIGAND_DEATH = createSoundEvent("brigand_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> BRIGAND_HURT = createSoundEvent("brigand_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_DRYAD_DEATH = createSoundEvent("coven_dryad_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_DRYAD_HURT = createSoundEvent("coven_dryad_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_FAMILIAR_ATTACK = createSoundEvent("coven_familiar_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_FAMILIAR_DEATH = createSoundEvent("coven_familiar_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_FAMILIAR_HURT = createSoundEvent("coven_familiar_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_PUPPET_ATTACK = createSoundEvent("coven_puppet_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_PUPPET_DEATH = createSoundEvent("coven_puppet_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_PUPPET_HURT = createSoundEvent("coven_puppet_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_SATYR_DEATH = createSoundEvent("coven_satyr_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_SATYR_HURT = createSoundEvent("coven_satyr_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_SCARECROW_ATTACK = createSoundEvent("coven_scarecrow_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_SCARECROW_DEATH = createSoundEvent("coven_scarecrow_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_SCARECROW_HURT = createSoundEvent("coven_scarecrow_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_WITCH_ATTACK = createSoundEvent("coven_witch_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_WITCH_DEATH = createSoundEvent("coven_witch_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> COVEN_WITCH_HURT = createSoundEvent("coven_witch_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> DWARF_ATTACK = createSoundEvent("dwarf_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> DWARF_DEATH = createSoundEvent("dwarf_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> DWARF_HURT = createSoundEvent("dwarf_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> ELF_ATTACK = createSoundEvent("elf_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> ELF_DEATH = createSoundEvent("elf_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> ELF_HURT = createSoundEvent("elf_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> ENT_ATTACK = createSoundEvent("ent_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> ENT_DEATH = createSoundEvent("ent_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> ENT_HURT = createSoundEvent("ent_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> GARGOYLE_ATTACK = createSoundEvent("gargoyle_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> GARGOYLE_DEATH = createSoundEvent("gargoyle_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> GARGOYLE_HURT = createSoundEvent("gargoyle_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> GIANT_ATTACK = createSoundEvent("giant_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> GIANT_DEATH = createSoundEvent("giant_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> GIANT_HURT = createSoundEvent("giant_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> GNOME_ATTACK = createSoundEvent("gnome_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> GNOME_DEATH = createSoundEvent("gnome_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> GNOME_HURT = createSoundEvent("gnome_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> GREMLIN_ATTACK = createSoundEvent("gremlin_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> GREMLIN_DEATH = createSoundEvent("gremlin_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> GREMLIN_HURT = createSoundEvent("gremlin_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> HOBBIT_ATTACK = createSoundEvent("hobbit_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> HOBBIT_DEATH = createSoundEvent("hobbit_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> HOBBIT_HURT = createSoundEvent("hobbit_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> HUMAN_ATTACK = createSoundEvent("human_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> HUMAN_DEATH = createSoundEvent("human_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> HUMAN_FEMALE_ATTACK = createSoundEvent("human_female_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> HUMAN_FEMALE_DEATH = createSoundEvent("human_female_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> HUMAN_FEMALE_HURT = createSoundEvent("human_female_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> HUMAN_HURT = createSoundEvent("human_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> ISHTARI_ANUBITE_ATTACK = createSoundEvent("ishtari_anubite_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> ISHTARI_ANUBITE_DEATH = createSoundEvent("ishtari_anubite_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> ISHTARI_ANUBITE_HURT = createSoundEvent("ishtari_anubite_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> ISHTARI_MUMMY_ATTACK = createSoundEvent("ishtari_mummy_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> ISHTARI_MUMMY_DEATH = createSoundEvent("ishtari_mummy_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> ISHTARI_MUMMY_HURT = createSoundEvent("ishtari_mummy_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> ISHTARI_PHAROAH_ATTACK = createSoundEvent("ishtari_pharoah_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> ISHTARI_PHAROAH_DEATH = createSoundEvent("ishtari_pharoah_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> ISHTARI_PHAROAH_HURT = createSoundEvent("ishtari_pharoah_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> KLOWN_ATTACK = createSoundEvent("klown_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> KLOWN_DEATH = createSoundEvent("klown_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> KLOWN_HURT = createSoundEvent("klown_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> KOBOLD_ATTACK = createSoundEvent("kobold_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> KOBOLD_DEATH = createSoundEvent("kobold_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> KOBOLD_HURT = createSoundEvent("kobold_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> KONG_APE_ATTACK = createSoundEvent("kong_ape_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> KONG_APE_DEATH = createSoundEvent("kong_ape_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> KONG_APE_HURT = createSoundEvent("kong_ape_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> LIZARDMAN_AMBIENT = createSoundEvent("lizardman_ambient");
	public static final DeferredHolder<SoundEvent, SoundEvent> LIZARDMAN_ATTACK = createSoundEvent("lizardman_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> LIZARDMAN_DEATH = createSoundEvent("lizardman_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> LIZARDMAN_HURT = createSoundEvent("lizardman_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> MALICE_ATTACK = createSoundEvent("malice_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> MALICE_DEATH = createSoundEvent("malice_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> MALICE_HURT = createSoundEvent("malice_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> MONSTER_ATTACK = createSoundEvent("monster_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> MONSTER_DEATH = createSoundEvent("monster_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> MONSTER_HURT = createSoundEvent("monster_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> NORSKA_ATTACK = createSoundEvent("norska_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> NORSKA_DEATH = createSoundEvent("norska_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> NORSKA_HURT = createSoundEvent("norska_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> ORC_AMBIENT = createSoundEvent("orc_ambient");
	public static final DeferredHolder<SoundEvent, SoundEvent> ORC_ATTACK = createSoundEvent("orc_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> ORC_DEATH = createSoundEvent("orc_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> ORC_HURT = createSoundEvent("orc_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> ORC_URUK_ATTACK = createSoundEvent("orc_uruk_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> ORC_URUK_DEATH = createSoundEvent("orc_uruk_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> ORC_URUK_HURT = createSoundEvent("orc_uruk_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> PIRATE_AMBIENT = createSoundEvent("pirate_ambient");
	public static final DeferredHolder<SoundEvent, SoundEvent> PIRATE_ATTACK = createSoundEvent("pirate_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> PIRATE_DEATH = createSoundEvent("pirate_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> PIRATE_HURT = createSoundEvent("pirate_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> SKELETON_DEATH = createSoundEvent("skeleton_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> SKELETON_HURT = createSoundEvent("skeleton_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_ATTACK = createSoundEvent("vampire_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_BOSS_ATTACK = createSoundEvent("vampire_boss_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_BOSS_DEATH = createSoundEvent("vampire_boss_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_BOSS_HURT = createSoundEvent("vampire_boss_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_BRIDE_ATTACK = createSoundEvent("vampire_bride_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_BRIDE_DEATH = createSoundEvent("vampire_bride_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_BRIDE_HURT = createSoundEvent("vampire_bride_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_DEATH = createSoundEvent("vampire_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_HURT = createSoundEvent("vampire_hurt");
	public static final DeferredHolder<SoundEvent, SoundEvent> ZOMBIE_AMBIENT = createSoundEvent("zombie_ambient");
	public static final DeferredHolder<SoundEvent, SoundEvent> ZOMBIE_ATTACK = createSoundEvent("zombie_attack");
	public static final DeferredHolder<SoundEvent, SoundEvent> ZOMBIE_DEATH = createSoundEvent("zombie_death");
	public static final DeferredHolder<SoundEvent, SoundEvent> ZOMBIE_HURT = createSoundEvent("zombie_hurt");

	public static void register(IEventBus modBus) {
		SOUNDS.register(modBus);
		addAlternativeSoundReferences();
	}

	private static DeferredHolder<SoundEvent, SoundEvent> createSoundEvent(String soundName) {
		ResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(AncientWarfareNPC.MOD_ID, soundName);
		DeferredHolder<SoundEvent, SoundEvent> soundEvent = SOUNDS.register(soundName, () -> SoundEvent.createVariableRangeEvent(registryName));
		npcSoundEvents.put(soundName, soundEvent);
		return soundEvent;
	}

	public static boolean isValidSound(String sound) {
		return npcSoundEvents.containsKey(sound);
	}

	public static void addAlternativeSoundReferences() {
		npcSoundEvents.put("barbarian_death", HUMAN_DEATH);
	}

	public static SoundEvent getSoundEventFromString(String name) {
		name = name.toLowerCase();
		DeferredHolder<SoundEvent, SoundEvent> holder = npcSoundEvents.get(name);
		return holder == null ? null : holder.get();
	}
}
