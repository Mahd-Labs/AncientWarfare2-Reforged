# Changelog

## 2.7.0-reforged.1.0.0 (2026-11-17)

First release (release candidates rc.1 and rc.2 were tested on the owner's instances).
- Fixed: NPCs with a fixed skin from the 1.12 structure templates (for example "Angry Townsman") were drawn with
  the missing-texture pattern; NPCs already in a world are repaired when they load.
- Fixed: faction NPCs (also from structure spawners) floated about 0.6 blocks above their horse's saddle.
- Licensing: 98 sound files of third-party or unclear origin removed (list in tools/licensing_dropped_sounds.txt);
  some NPC sounds now use Minecraft's own sound events, the protection-flag claim and 47 structure ambience sounds are
  silent. No Mojang texture pixels in the mod's own textures. CREDITS.md, AI_DISCLOSURE.md and NOTICE added; the mod
  metadata links https://github.com/Mahd-Labs/AncientWarfare2-Reforged.
- No packet, registry id or save format changed since beta.4: beta worlds load as before.

## 2.7.0-reforged.0.1.0-beta.9 (internal test build, 2026-11-16)

- Fixed: in packs whose data fixers return read-only tags (ATM10), every 1.12 leaves rule failed to convert in beta.8
  (994 "Could not upgrade the 1.12 block state" warnings at start, leaves built as air); 1.12 item conversion had the
  same weakness. Both now work on a copy.

## 2.7.0-reforged.0.1.0-beta.8 (internal test build, 2026-11-16)

- Fixed: structures from the 1.12 templates build as in 1.12: door and chair facings; fence, pane, iron bar, wall,
  corner stair and bench-leg connections and double chests on every build path; leaves, skulls, double plants and the
  plant in flower pots no longer missing; bed and banner colours, banner patterns and sign texts kept; benches along z
  drawn correctly. Structures generated before this build are unchanged.
- Improvement (switchable, `background_attempt_chunks` in `ancientwarfare-structure.toml`): world generation structure
  and town attempts no longer generate missing terrain on the server thread; they wait until Minecraft's chunk workers
  (and, for town searches, two background threads) have prepared it, then run unchanged in the same order. The mod's
  longest world generation tick went from about 4 s to about 0.3 s (ATM10: 0.12 s). No packet, registry id or save
  format changed.

## 2.7.0-reforged.0.1.0-beta.7 (internal test build, 2026-11-15)

- Fix (as in 1.12): ground structures in cold biomes get their ring of snow again; the port had used a stricter snow
  test than the original, so the ring was almost never placed.
- No packet, registry id or save format changed since beta.6.

## 2.7.0-reforged.0.1.0-beta.6 (internal test build, 2026-11-15)

- Budgeted placement (improvement, `budgeted_placement`) now also spreads a town's terrain preparation and a
  world-generation structure's clearing, levelling, filling and snow over several ticks within the same per-tick
  budget. A town's preparation went from one 6.9 s tick to slices of at most about 30 ms, with the same result.
- No packet, registry id or save format changed since beta.5.

## 2.7.0-reforged.0.1.0-beta.5 (internal test build, 2026-11-15)

- Improvement (switchable, `budgeted_placement` in `ancientwarfare-structure.toml`): structures from world
  generation, towns, the creative structure builder and `/awstructure build` are now placed over several ticks within
  `placement_ms_per_tick` (10 ms) instead of freezing the server for seconds; the result is identical.
- Malformed ids in saved data, packets and typed text (ammunition, spawners, NPC skins, teams, songs, recipes,
  structure screens) are skipped or replaced by a default instead of throwing.
- Mailbox sides loaded into a mailbox already in the world (`/data merge`, structure data) refresh what pipes see;
  out-of-range sides are skipped.
- No packet changed since beta.4; server and client should still both be beta.5 for the same behaviour.

## 2.7.0-reforged.0.1.0-beta.4 (internal test build, 2026-11-15)

- JEI support again (optional, JEI 19 for 1.21.1): research recipes in their own shaped / shapeless categories with
  the required research, the mod's data-variant items (NPC spawners, coins, vehicle spawners, research notes,
  structure decorations) listed separately, the engineering station, auto crafting and warehouse crafting station as
  catalysts, and the "+" recipe transfer into them. Server and client must both be beta.4 (a new packet).

## 2.7.0-reforged.0.1.0-beta.3 (internal test build, 2026-11-15)

- Farms recognise crops, seeds and saplings of other mods through their tags (`minecraft:crops`,
  `minecraft:saplings`, `c:seeds`, `minecraft:villager_plantable_seeds`). In 1.12 they were recognised through
  Forge's IPlantable, which no longer exists; until now only crops and saplings built on the vanilla classes worked.

## 2.7.0-reforged.0.1.0-beta.2 (internal test build, 2026-11-15)

- Siege weapons: after getting off a weapon and on again, the aim shown in the overlay is the weapon's real aim
  (it could show a power the server never took).
- NPC crews really lead moving targets now with the `crew_leads_targets` switch on (the speed measurement was reset
  all the time, so the lead stayed zero).
- Big-modpack fixes: holding a command baton no longer makes the client scan the whole world every tick; bad ids in
  config lists and registry files are skipped with a log line instead of stopping the game.

## 2.7.0-reforged.0.1.0-beta.1 (internal test build, 2026-11-15)

First build handed out for testing. Ancient Warfare 2: Reforged is an unofficial port of Ancient Warfare 2
(version 2.7.0 for Minecraft 1.12.2) to Minecraft 1.21.1 / NeoForge 21.1. It is not made or endorsed by the
original authors.

Ported
- Core: research, engineering station, backpack, hammer, manual, the mod's own GUI framework.
- Automation: worksites (crop, tree, fruit, animal, fish farms, quarry, auto crafting), warehouse (control,
  storage, interface, crafting station, stock viewer and linker), torque (generators, shafts, junctions,
  distributors, flywheel, windmill, waterwheel), chunk loaders, mailbox.
- NPCs: workers, combat NPCs, couriers, traders, priests, bards, siege engineers, the faction NPCs, orders
  (work, upkeep, combat, routing, trade), town hall, command baton, factions and standing.
- Structures: scanner, builder, drafting station, gates, advanced spawners, loot chests, decoration blocks,
  the bundled template pack (1484 templates), world generation of structures and towns.
- Siege weapons, stationary only: catapult, ballista, cannon and trebuchet stands (fixed and turret), hwacha,
  large trebuchet, 51 ammunition types, NPC crews.

Not in this build
- Everything that drives or floats (mobile siege weapons, battering ram, boats, chest cart), vehicle upgrades and
  armour. Structures that contained them are generated without them.
- Compatibility code for mods that do not exist for 1.21.1 in the form the original used (the original's optional
  integrations). Templates that need blocks of absent mods are skipped.

Changed on purpose compared with 1.12 (each can be switched back in the config files, see README)
- NPCs: paths are reused, workers give up unreachable worksites, medics heal the worst wounded first, upkeep
  counts only food really taken, NPC level shown and (creative) editable.
- Siege weapons: ammunition fallback, wider hit margin, hit test along the whole flight step, NPC crews lead
  moving targets and shoot more accurately with level.
- Towns: the search for a building site tolerates rougher ground and some water (1.21 terrain is rougher).
- The server no longer trusts the client for opening screens, research and work bounds; chunk tickets are capped
  at 25 chunks as Forge 1.12 did.

Removed
- 18 sound clips taken from television shows, and 19 unused sound files.
