package net.shadowmage.ancientwarfare.structure.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;

import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Port notes: generated from the 1.12 class by tools/structure_gen_sounds.py. @ObjectHolder fields -> DeferredHolder
 * constants (call .get()), one per sound the 1.12 class registered; ids keep their 1.12 path in the "ancientwarfare"
 * namespace. SOUNDS_BY_NAME (registration order) replaces iterating the 1.12 sound registry for this mod id.
 * The 18 TV-clip sounds of the 1.12 class (del_*, myth_*, mythbusters_*) are not registered: dropped for licensing.
 */
@SuppressWarnings("unused")
public class AWStructureSounds {
	private AWStructureSounds() {}

	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, AncientWarfareStructure.MOD_ID);

	public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> SOUNDS_BY_NAME = new LinkedHashMap<>();

	public static final DeferredHolder<SoundEvent, SoundEvent> BARBARIAN_DRUMS_CAUTIOUS = createSoundEvent("barbarian_drums_cautious");
	public static final DeferredHolder<SoundEvent, SoundEvent> BARBARIAN_DRUMS_DISTANT = createSoundEvent("barbarian_drums_distant");
	public static final DeferredHolder<SoundEvent, SoundEvent> BARBARIAN_DRUMS_SLOW_SHORT = createSoundEvent("barbarian_drums_slow_short");
	public static final DeferredHolder<SoundEvent, SoundEvent> BARBARIAN_DRUMS_WARNING = createSoundEvent("barbarian_drums_warning");
	public static final DeferredHolder<SoundEvent, SoundEvent> BARRACKS_AMBIANCE = createSoundEvent("barracks_ambiance");
	public static final DeferredHolder<SoundEvent, SoundEvent> CLOCK_GRANDFATHER_CHIMES = createSoundEvent("clock_grandfather_chimes");
	public static final DeferredHolder<SoundEvent, SoundEvent> COFFIN_OPENS = createSoundEvent("coffin_opens");
	public static final DeferredHolder<SoundEvent, SoundEvent> COIN_STACK_BREAK = createSoundEvent("coin_stack_break");
	public static final DeferredHolder<SoundEvent, SoundEvent> COIN_STACK_INTERACT = createSoundEvent("coin_stack_interact");
	public static final DeferredHolder<SoundEvent, SoundEvent> CREEPY_CHITTERING = createSoundEvent("creepy_chittering");
	public static final DeferredHolder<SoundEvent, SoundEvent> CREEPY_CRYPT_VOICE_1 = createSoundEvent("creepy_crypt_voice_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> CREEPY_CRYPT_VOICE_2 = createSoundEvent("creepy_crypt_voice_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> CREEPY_CRYPT_VOICE_4 = createSoundEvent("creepy_crypt_voice_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> CREEPY_DRONING_1 = createSoundEvent("creepy_droning_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> CREEPY_DRONING_2 = createSoundEvent("creepy_droning_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> CREEPY_EVIL_GIRL = createSoundEvent("creepy_evil_girl");
	public static final DeferredHolder<SoundEvent, SoundEvent> CREEPY_GHOSTLY_BREATH = createSoundEvent("creepy_ghostly_breath");
	public static final DeferredHolder<SoundEvent, SoundEvent> CREEPY_GHOSTLY_WHISPER = createSoundEvent("creepy_ghostly_whisper");
	public static final DeferredHolder<SoundEvent, SoundEvent> CREEPY_KNOCK_KNOCK = createSoundEvent("creepy_knock_knock");
	public static final DeferredHolder<SoundEvent, SoundEvent> CREEPY_SPOOKY_RAVENS = createSoundEvent("creepy_spooky_ravens");
	public static final DeferredHolder<SoundEvent, SoundEvent> CTHULHU = createSoundEvent("cthulhu");
	public static final DeferredHolder<SoundEvent, SoundEvent> CTHULHU_BACKGROUND_1 = createSoundEvent("cthulhu_background_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> CTHULHU_BACKGROUND_2 = createSoundEvent("cthulhu_background_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> CTHULHU_BACKGROUND_3 = createSoundEvent("cthulhu_background_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> DEMONIC_COFFIN_OPENS = createSoundEvent("demonic_coffin_opens");
	public static final DeferredHolder<SoundEvent, SoundEvent> EMPIRE_CHURCH_BELL = createSoundEvent("empire_church_bell");
	public static final DeferredHolder<SoundEvent, SoundEvent> EVIL_AMBIENCE_1 = createSoundEvent("evil_ambience_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> EVIL_AMBIENCE_2 = createSoundEvent("evil_ambience_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> EVIL_DARK_PRAYER = createSoundEvent("evil_dark_prayer");
	public static final DeferredHolder<SoundEvent, SoundEvent> EVIL_DEMONICHAUNTING1 = createSoundEvent("evil_demonichaunting1");
	public static final DeferredHolder<SoundEvent, SoundEvent> EVIL_DEMONICHAUNTING2 = createSoundEvent("evil_demonichaunting2");
	public static final DeferredHolder<SoundEvent, SoundEvent> EVIL_DEMONICHAUNTING3 = createSoundEvent("evil_demonichaunting3");
	public static final DeferredHolder<SoundEvent, SoundEvent> EVIL_DEMONICHAUNTING4 = createSoundEvent("evil_demonichaunting4");
	public static final DeferredHolder<SoundEvent, SoundEvent> EVIL_GETOUT = createSoundEvent("evil_getout");
	public static final DeferredHolder<SoundEvent, SoundEvent> GONG_LARGE = createSoundEvent("gong_large");
	public static final DeferredHolder<SoundEvent, SoundEvent> GONGS_1 = createSoundEvent("gongs_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> GONGS_2 = createSoundEvent("gongs_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> GONGS_3 = createSoundEvent("gongs_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> GONGS_4 = createSoundEvent("gongs_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> GONGS_5 = createSoundEvent("gongs_5");
	public static final DeferredHolder<SoundEvent, SoundEvent> GONGS_6 = createSoundEvent("gongs_6");
	public static final DeferredHolder<SoundEvent, SoundEvent> GRINDSTONE_GENERIC = createSoundEvent("grindstone_generic");
	public static final DeferredHolder<SoundEvent, SoundEvent> GUARDIANS_CHURCH_CHOIR = createSoundEvent("guardians_church_choir");
	public static final DeferredHolder<SoundEvent, SoundEvent> HAUNTED_GRAVEYARD = createSoundEvent("haunted_graveyard");
	public static final DeferredHolder<SoundEvent, SoundEvent> HAUNTED_GRAVEYARD_ENTRY_AMBIENCE = createSoundEvent("haunted_graveyard_entry_ambience");
	public static final DeferredHolder<SoundEvent, SoundEvent> HAUNTED_GRAVEYARD_TREE_RUSTLING_LEAVES = createSoundEvent("haunted_graveyard_tree_rustling_leaves");
	public static final DeferredHolder<SoundEvent, SoundEvent> HAUNTED_GRAVEYARD_WIND_IN_TREES_01 = createSoundEvent("haunted_graveyard_wind_in_trees_01");
	public static final DeferredHolder<SoundEvent, SoundEvent> HAUNTED_GRAVEYARD_WIND_IN_TREES_02 = createSoundEvent("haunted_graveyard_wind_in_trees_02");
	public static final DeferredHolder<SoundEvent, SoundEvent> HAUNTED_GRAVEYARD_WIND_IN_TREES_03 = createSoundEvent("haunted_graveyard_wind_in_trees_03");
	public static final DeferredHolder<SoundEvent, SoundEvent> HAUNTED_GRAVEYARD_WIND_IN_TREES_04 = createSoundEvent("haunted_graveyard_wind_in_trees_04");
	public static final DeferredHolder<SoundEvent, SoundEvent> HORN_1 = createSoundEvent("horn_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> HORN_3 = createSoundEvent("horn_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> HORN_5 = createSoundEvent("horn_5");
	public static final DeferredHolder<SoundEvent, SoundEvent> HORN_6 = createSoundEvent("horn_6");
	public static final DeferredHolder<SoundEvent, SoundEvent> HORN_7 = createSoundEvent("horn_7");
	public static final DeferredHolder<SoundEvent, SoundEvent> HORN_8 = createSoundEvent("horn_8");
	public static final DeferredHolder<SoundEvent, SoundEvent> IRON_GATE = createSoundEvent("iron_gate");
	public static final DeferredHolder<SoundEvent, SoundEvent> IRON_GATE_BREAK = createSoundEvent("iron_gate_break");
	public static final DeferredHolder<SoundEvent, SoundEvent> JUNGLE_DRUMS_1 = createSoundEvent("jungle_drums_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> JUNGLE_DRUMS_2 = createSoundEvent("jungle_drums_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> LOCATION_SIGH_1 = createSoundEvent("location_sigh_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> MAGICAL_APPEARANCE = createSoundEvent("magical_appearance");
	public static final DeferredHolder<SoundEvent, SoundEvent> MARKET_GENERIC = createSoundEvent("market_generic");
	public static final DeferredHolder<SoundEvent, SoundEvent> MARKET_GENERIC_2 = createSoundEvent("market_generic_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> NOGG_CHURCH_BELLS = createSoundEvent("nogg_church_bells");
	public static final DeferredHolder<SoundEvent, SoundEvent> NOGG_CLOCK_TOWER = createSoundEvent("nogg_clock_tower");
	public static final DeferredHolder<SoundEvent, SoundEvent> PIRATE_PUGWASH = createSoundEvent("pirate_pugwash");
	public static final DeferredHolder<SoundEvent, SoundEvent> PRISMARINE_COFFIN_OPENS = createSoundEvent("prismarine_coffin_opens");
	public static final DeferredHolder<SoundEvent, SoundEvent> PROTECTION_FLAG_CLAIM = createSoundEvent("protection_flag_claim");
	public static final DeferredHolder<SoundEvent, SoundEvent> RELIGIOUS_CHURCH_BELLS_12 = createSoundEvent("religious_church_bells_12");
	public static final DeferredHolder<SoundEvent, SoundEvent> SANDSTONE_SARCOPHAGUS_OPENS = createSoundEvent("sandstone_sarcophagus_opens");
	public static final DeferredHolder<SoundEvent, SoundEvent> SARKONID_MUSIC = createSoundEvent("sarkonid_music");
	public static final DeferredHolder<SoundEvent, SoundEvent> STONE_COFFIN_OPENS = createSoundEvent("stone_coffin_opens");
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_AMBIENCE = createSoundEvent("undead_ambience");
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_HAUNTING = createSoundEvent("undead_haunting");
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_SPOOKY = createSoundEvent("undead_spooky");
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_VAMPIRE_EVIL_LAUGH = createSoundEvent("undead_vampire_evil_laugh");
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_WHISPERING_VOICES = createSoundEvent("undead_whispering_voices");
	public static final DeferredHolder<SoundEvent, SoundEvent> URN_BREAK = createSoundEvent("urn_break");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_CASTLE_ORGAN_SHORT = createSoundEvent("vampire_castle_organ_short");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_CRYPT = createSoundEvent("vampire_crypt");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_GHOSTLY_VOICES = createSoundEvent("vampire_ghostly_voices");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_ORGANMUSIC = createSoundEvent("vampire_organmusic");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_WATCHER = createSoundEvent("vampire_watcher");
	public static final DeferredHolder<SoundEvent, SoundEvent> VAMPIRE_WHISPERS = createSoundEvent("vampire_whispers");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_CARPENTER_1 = createSoundEvent("village_carpenter_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_CARPENTER_2 = createSoundEvent("village_carpenter_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_CARPENTER_3 = createSoundEvent("village_carpenter_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_CARPENTER_4 = createSoundEvent("village_carpenter_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_CARPENTER_5 = createSoundEvent("village_carpenter_5");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_FARM_AMBIENCE_3 = createSoundEvent("village_farm_ambience_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_GENERAL_AMBIENCE_1 = createSoundEvent("village_general_ambience_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_GENERAL_AMBIENCE_3 = createSoundEvent("village_general_ambience_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_LUMBERJACK_1 = createSoundEvent("village_lumberjack_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_LUMBERJACK_2 = createSoundEvent("village_lumberjack_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_MARKET_GENERAL_BACKGROUND_1 = createSoundEvent("village_market_general_background_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_PIGEONS_1 = createSoundEvent("village_pigeons_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_PIGEONS_2 = createSoundEvent("village_pigeons_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_PIGEONS_3 = createSoundEvent("village_pigeons_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_TAVERN_AMBIENT_LOOP = createSoundEvent("village_tavern_ambient_loop");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_WATER_MILL_WHEEL_TURNING = createSoundEvent("village_water_mill_wheel_turning");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_WEAVERS_LOOM = createSoundEvent("village_weavers_loom");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_WELL_CRANK = createSoundEvent("village_well_crank");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_WINDMILL = createSoundEvent("village_windmill");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_WINDMILL_INTERIOR = createSoundEvent("village_windmill_interior");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_WINDMILL_SAILS_TURNING_1 = createSoundEvent("village_windmill_sails_turning_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> VILLAGE_WINDMILL_SAILS_TURNING_2 = createSoundEvent("village_windmill_sails_turning_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO = createSoundEvent("volcano");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_1 = createSoundEvent("volcano_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_10 = createSoundEvent("volcano_10");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_2 = createSoundEvent("volcano_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_3 = createSoundEvent("volcano_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_4 = createSoundEvent("volcano_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_5 = createSoundEvent("volcano_5");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_6 = createSoundEvent("volcano_6");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_7 = createSoundEvent("volcano_7");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_8 = createSoundEvent("volcano_8");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_9 = createSoundEvent("volcano_9");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_ERUPTS_1 = createSoundEvent("volcano_erupts_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_ERUPTS_2 = createSoundEvent("volcano_erupts_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_ERUPTS_3 = createSoundEvent("volcano_erupts_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_ERUPTS_4 = createSoundEvent("volcano_erupts_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_EXPLOSION = createSoundEvent("volcano_explosion");
	public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANO_RUMBLE = createSoundEvent("volcano_rumble");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_1 = createSoundEvent("witch_house_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_2_CACKLE = createSoundEvent("witch_house_2_cackle");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_2_HUNGRY = createSoundEvent("witch_house_2_hungry");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_2_LAUGH_1 = createSoundEvent("witch_house_2_laugh_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_2_OOOH = createSoundEvent("witch_house_2_oooh");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_3 = createSoundEvent("witch_house_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_4 = createSoundEvent("witch_house_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_5_1 = createSoundEvent("witch_house_5_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_5_2 = createSoundEvent("witch_house_5_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_5_3 = createSoundEvent("witch_house_5_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_BUBBLING_CAULDRON_1 = createSoundEvent("witch_house_bubbling_cauldron_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_BUBBLING_CAULDRON_2 = createSoundEvent("witch_house_bubbling_cauldron_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_BUBBLING_CAULDRON_3 = createSoundEvent("witch_house_bubbling_cauldron_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_BUBBLING_CAULDRON_4 = createSoundEvent("witch_house_bubbling_cauldron_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_BUGS = createSoundEvent("witch_house_bugs");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_CAT_SCREECHING = createSoundEvent("witch_house_cat_screeching");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_CROW_1 = createSoundEvent("witch_house_crow_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_CROW_2 = createSoundEvent("witch_house_crow_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_MAD_LAUGH = createSoundEvent("witch_house_mad_laugh");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_RATS_SQUEAK_1 = createSoundEvent("witch_house_rats_squeak_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_RATS_SQUEAK_2 = createSoundEvent("witch_house_rats_squeak_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_RATS_SQUEAK_3 = createSoundEvent("witch_house_rats_squeak_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_TOAD_1 = createSoundEvent("witch_house_toad_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCH_HOUSE_TOAD_2 = createSoundEvent("witch_house_toad_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCHBANE_CHURCH_ENTRY = createSoundEvent("witchbane_church_entry");
	public static final DeferredHolder<SoundEvent, SoundEvent> WITCHBANE_NUNS_CHANT = createSoundEvent("witchbane_nuns_chant");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARD_EXPLOSION_2 = createSoundEvent("wizard_explosion_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARD_TOWER_1 = createSoundEvent("wizard_tower_1");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARD_TOWER_2 = createSoundEvent("wizard_tower_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARD_TOWER_3 = createSoundEvent("wizard_tower_3");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARD_TOWER_4 = createSoundEvent("wizard_tower_4");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARD_TOWER_5 = createSoundEvent("wizard_tower_5");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARD_TOWER_6 = createSoundEvent("wizard_tower_6");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARD_TOWER_7 = createSoundEvent("wizard_tower_7");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARD_TOWER_8 = createSoundEvent("wizard_tower_8");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARD_TOWER_9 = createSoundEvent("wizard_tower_9");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARD_TOWER_10 = createSoundEvent("wizard_tower_10");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARD_TOWER_11 = createSoundEvent("wizard_tower_11");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARD_TOWER_12 = createSoundEvent("wizard_tower_12");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARDS_TOWER_13 = createSoundEvent("wizards_tower_13");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARDS_TOWER_14 = createSoundEvent("wizards_tower_14");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARDS_TOWER_15 = createSoundEvent("wizards_tower_15");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARDS_TOWER_16 = createSoundEvent("wizards_tower_16");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARDS_TOWER_17 = createSoundEvent("wizards_tower_17");
	public static final DeferredHolder<SoundEvent, SoundEvent> WIZARDS_TOWER_18 = createSoundEvent("wizards_tower_18");
	public static final DeferredHolder<SoundEvent, SoundEvent> WOODEN_GATE = createSoundEvent("wooden_gate");
	public static final DeferredHolder<SoundEvent, SoundEvent> WOODEN_GATE_BREAK = createSoundEvent("wooden_gate_break");
	public static final DeferredHolder<SoundEvent, SoundEvent> XOLTEC_LONG_DRUM_BEAT = createSoundEvent("xoltec_long_drum_beat");
	public static final DeferredHolder<SoundEvent, SoundEvent> XOLTEC_PYRAMID_DRUMS = createSoundEvent("xoltec_pyramid_drums");
	public static final DeferredHolder<SoundEvent, SoundEvent> XOLTEC_TEMPLE_FLUTE = createSoundEvent("xoltec_temple_flute");
	public static final DeferredHolder<SoundEvent, SoundEvent> XOLTEC_TOWN_DRUMS = createSoundEvent("xoltec_town_drums");
	public static final DeferredHolder<SoundEvent, SoundEvent> XOLTEC_TOWN_FLUTES = createSoundEvent("xoltec_town_flutes");
	public static final DeferredHolder<SoundEvent, SoundEvent> XOLTEC_TOWN_LONG = createSoundEvent("xoltec_town_long");
	public static final DeferredHolder<SoundEvent, SoundEvent> YO_WAY_YO = createSoundEvent("yo_way_yo");
	public static final DeferredHolder<SoundEvent, SoundEvent> ZIMBA_DRUMS_2 = createSoundEvent("zimba_drums_2");
	public static final DeferredHolder<SoundEvent, SoundEvent> ZIMBA_VILLAGE_DANCE = createSoundEvent("zimba_village_dance");
	public static final DeferredHolder<SoundEvent, SoundEvent> EVIL_BOOGIEMANCACKLE = createSoundEvent("evil_boogiemancackle");

	private static DeferredHolder<SoundEvent, SoundEvent> createSoundEvent(String soundName) {
		ResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(AncientWarfareStructure.MOD_ID, soundName);
		DeferredHolder<SoundEvent, SoundEvent> holder = SOUNDS.register(soundName, () -> SoundEvent.createVariableRangeEvent(registryName));
		SOUNDS_BY_NAME.put(soundName, holder);
		return holder;
	}

	public static void register(IEventBus modBus) {
		SOUNDS.register(modBus);
	}
}
