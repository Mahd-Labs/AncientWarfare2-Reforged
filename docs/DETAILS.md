# Ancient Warfare 2: Reforged - details

The longer notes behind [README.md](../README.md).

## Requirements

- Minecraft 1.21.1 and NeoForge 21.1 (tested with NeoForge 21.1.265 and 21.1.266). No other mods are needed.
- The same jar on the server and on every client.
- Mod id `ancientwarfare`. Do not keep an older `ancientwarfare-*.jar` next to it.
- A server that loads the bundled structure pack should have 4 GB of memory or more.

## What is in, and what is not

In: worksites and torque power, the warehouse, NPCs with orders (workers, soldiers, couriers, traders, priests,
bards, siege engineers), factions and their NPCs, research, structure scanning and building, world-generated
structures and towns with the bundled template pack, stationary siege weapons with NPC crews.

Not ported from 1.12: mobile siege weapons, boats, the battering ram, the chest cart, vehicle upgrades and vehicle
armour.

`CHANGELOG.md` lists the changes build by build.

## Config switches

The improvement switches and the other settings are described in [CONFIG.md](CONFIG.md).

## Known issues

- Some sounds of the original are silent or replaced. Sound files of third-party or unclear origin were removed
  (list: `tools/licensing_dropped_sounds.txt`). Some NPC hurt, death and attack sounds now use Minecraft's own
  sounds; the protection-flag claim and many structure ambience sounds (for example in Nogg markets, the Empire
  cathedral and the Cthulhu temple) are silent.
- Some textures differ slightly from the original where pixels copied from Minecraft were redrawn or replaced by
  Minecraft's own textures (builder, altar and totem bottoms, banner poles, a few machine and gate textures).
- Kept from the original on purpose:
  - Siege weapons: with mouse aim on (key C, on by default) the R / F keys seem to do nothing, because the mouse
    sets pitch or power every tick. The ammunition count shown is one lower than what is loaded after a shot, and
    the last round of a stack is fired only after the inventory is touched.
  - The worksite "Sides" screen maps block faces to the worksite's inventories for hoppers and pipes; worksites do
    not push items out by themselves.
  - Warehouse: only the Warehouse Interface exposes items to pipes and other mods, through its own 9-slot buffer
    and its filters. Storage blocks and the controller expose nothing.
  - Torque distributors only take input from the back.
  - Some screens show raw text keys (`guistrings.inventory.left` and similar).
  - Faction cavalry and mounted archers made with `/summon` get no horse unless the command adds `{horseLives:1b}`;
    the ones from structure spawners have their horses.
- Minecraft 1.21 behaviour: a rider's body follows its horse, so an NPC that looks sideways sits turned on its
  horse (in 1.12 the horse turned with the rider).
- Not every screen and animation has been checked by a person yet. Please report anything that looks wrong.

## Using 1.12 structure templates

The bundled template pack loads by default (`load_default_structure_pack` in `ancientwarfare-structure.toml`).
Your own 1.12 templates (`.aws`, town templates `.awt`) go into `config/ancientwarfare/structures/included/`; they
are converted when loaded. Blocks and items of mods that are not installed are skipped; templates that need such
mods are left out (the log says how many). As in the original, a template in that folder that needed fixes is
**rewritten in place** with the fixes, so keep a copy of the originals. Templates scanned in game are saved to
`config/ancientwarfare/structures/export/`, or straight into `included/` when the scanner is set to include them.

## What to put in a problem report

Open an issue at https://github.com/Mahd-Labs/AncientWarfare2-Reforged/issues, not with the original authors. Say
what you did, what you expected and what happened; which build; single player or server; new or existing world;
which other mods. Attach `logs/latest.log` (and `logs/debug.log` if you have it) from the side where it happened,
any file from `crash-reports/`, and a screenshot for anything visual. For a structure or town, add the
coordinates (F3).

## Building from source and the tools/ scripts

Needs a Java 21 JDK. In the source tree:

```
./gradlew build          # compile
./gradlew releaseJar     # the release jar, in build/release/
```

The scripts in `tools/` regenerated or converted some shipped resources from the 1.12 sources; they are not needed
to build. They read the 1.12 Ancient Warfare 2 sources from `AW2_112_SOURCE` and, where needed, decompiled
Minecraft 1.21.1 sources from `MC_SOURCES`.
