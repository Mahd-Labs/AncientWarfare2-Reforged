# Ancient Warfare 2: Reforged - configuration

## Where the files are

All files are in the game's `config/` folder. They are created with their defaults the first time the game starts,
and every key has a comment in the file that says what it does and what its default is.

| File | What it covers |
|---|---|
| `ancientwarfare-core.toml` | research system, recipes |
| `ancientwarfare-automation.toml` | torque power, worksites, mailboxes |
| `ancientwarfare-npc.toml` | NPC levels, food, upkeep, combat, factions |
| `ancientwarfare-structure.toml` | world generation of structures and towns, gates, the template pack |
| `ancientwarfare-vehicle.toml` | siege weapons and their ammunition |
| `ancientwarfare-npc-client.toml` | client only: NPC skins, name plates, health bars |
| `ancientwarfare-vehicle-client.toml` | client only: vehicle name plates and overlay |

The first five are common configs: the server and every client read their own copy, nothing is sent from the server.
Gameplay is decided by the server's copy. Some sections (for example `01_shared_settings` and the vehicle
`general_settings`) say in their comments that client and server must match, so give players the same five files as
the server. The two `-client` files only matter on each player's own computer and can differ.

The mod reads the values again when a file is saved while the game runs. Settings that are used once at start-up,
such as `load_default_structure_pack` and `template_extension`, need a restart; when in doubt, restart.

## Improvements over the original

Where this port deliberately behaves differently from Ancient Warfare 2 for 1.12, a switch restores the original.
To get the 1.12 behaviour back for all of them, set every key below to the value in the "Original" column. The keys
are in the section `[improvements]`, except the three `town_search_*` keys, which are in `[a_world-gen_settings]`.

### `ancientwarfare-npc.toml`

| Key | Default | Original | What it does |
|---|---|---|---|
| `reuse_paths` | true | false | an NPC keeps its path while it still leads to the same place, instead of recalculating it almost every tick |
| `skip_unreachable_worksites` | true | false | a worker that gets no closer to a worksite for 10 seconds tries another one for a minute |
| `medic_heals_worst_first` | true | false | medics treat the most injured NPC first |
| `upkeep_counts_only_taken_food` | true | false | fixes the original's counting error where one food item taken from a town hall filled the whole food bar |
| `npc_level_editor` | true | false | the NPC inventory screen shows the level; the creative controls screen shows and edits level and experience |

### `ancientwarfare-vehicle.toml`

| Key | Default | Original | What it does |
|---|---|---|---|
| `ammo_fallback` | true | false | a siege weapon uses other loaded ammunition when the chosen type runs out |
| `missile_hit_margin` | 0.5 | 0.3 | how far (blocks) a missile may pass outside a target and still hit it |
| `missile_hit_sweep` | true | false | missiles test every entity along their whole step per tick |
| `crew_leads_targets` | true | false | NPC crews aim ahead of moving targets |
| `crew_accuracy_per_level` | 0.067 | 0 | each crew level removes this share of the scatter |
| `crew_accuracy_max_reduction` | 0.5 | - | the most scatter levels can remove |

### `ancientwarfare-structure.toml`

| Key | Default | Original | What it does |
|---|---|---|---|
| `budgeted_placement` | true | false | large structures are placed over several ticks instead of in one |
| `placement_ms_per_tick` | 10 | - | time per tick for that |
| `background_attempt_chunks` | true | false | world-generation attempts wait for their terrain instead of generating it on the server thread |
| `town_search_height_drop` | 12 | 3 | how many blocks a chunk may lie below the town search start and still join the town area |
| `town_search_water_fraction` | 0.25 | 0 | share of a chunk that may be deep water |
| `town_search_rough_fraction` | 0.25 | 0 | share of a town's chunks that may be too low, too high or under water (levelled or filled when built) |

## Other settings players often change

`ancientwarfare-structure.toml`, section `[a_world-gen_settings]`:
- `enable_world_generation`, `enable_structure_generation`, `enable_town_generation`: turn all world generation,
  single structures or towns off.
- `random_generation_chance` (0.075): chance per chunk that a structure is attempted.
- `town_generation_chance` (0.125) and `town_min_distance` (1440): chance per chunk that a town is attempted, and the
  minimum distance between towns.
- `spawn_protection_radius` (400): no random structures this close to the world spawn.
- `chunk_cluster_value` (1.0): how many cluster value points a chunk can hold.
- `load_default_structure_pack` (true): load the bundled templates.

`ancientwarfare-npc.toml`:
- `archer_attack_range` (60.0): attack range of archers; mounted archers use half.
- `allow_upkeep_any_inventory` (true): false allows only Town Halls as upkeep locations.
- `npc_vanilla_equipment_drop_rate` (true): false makes faction NPCs always drop all their equipment.
- `npc_repack_creative_only` (false): true allows repacking NPCs only in creative mode.

`ancientwarfare-vehicle.toml`:
- `shots_destroys_blocks` (true), `block_fires` (true): whether siege shots break blocks and start fires.
- `allow_friendly_fire` (false): whether shots can hurt friendly NPCs and players.

`ancientwarfare-core.toml`:
- `use_research_system` (true): false disables research; all recipes work in the normal crafting station.

Client (`ancientwarfare-npc-client.toml`, `ancientwarfare-vehicle-client.toml`):
- `render_friendly_nameplates`, `render_hostile_nameplates` (false): name plates above NPCs.
- `render_vehicle_nameplates` (true), `render_overlay` (true): vehicle name plates and the driving overlay.
- `load_default_skin_pack` (true): the bundled NPC skins.

Everything else is described by the comments in the files themselves.
