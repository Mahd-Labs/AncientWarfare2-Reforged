#!/usr/bin/env python3
"""
Shared helpers for converting AW2 1.12 data files (recipes, loot tables, registry json) to 1.21.1 names.
Used by convert_recipes.py and convert_loot.py. Not a command by itself.

 - item ids:  1.12 (id, meta) -> flattened 1.21.1 id   (vanilla: parsed from Mojang's ItemStackTheFlatteningFix
   plus the later renames below; AW: aw_item_meta_map.json next to this file - module agents add their own
   flattened sub items there: {"<1.12 id without module suffix>": {"<meta>": "<new id>", "*": "<id for any meta>"}})
 - mod ids:   ancientwarfare(automation|npc|structure|vehicle): -> ancientwarfare:
 - ore names: ore dictionary name -> {"tag": ...} or {"item": ...}   (ORES below, extend when a name is missing)
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
# Inputs from outside this repository, from the environment (checked when used):
#   AW2_112_SOURCE  a checkout of the Ancient Warfare 2 1.12.x sources (github.com/P3pp3rF1y/AncientWarfare2)
#   MC_SOURCES      decompiled Minecraft 1.21.1 sources with its assets (net/minecraft/..., assets/minecraft/...)
AW2_112_SOURCE = os.path.expanduser(os.environ.get('AW2_112_SOURCE', ''))
MC_SRC = os.path.expanduser(os.environ.get('MC_SOURCES', ''))
REF_ASSETS = os.path.join(AW2_112_SOURCE, 'src/main/resources/assets/ancientwarfare') if AW2_112_SOURCE else ''


def require_sources(mc=False, aw2=False):
    if mc and not MC_SRC:
        raise SystemExit('set MC_SOURCES to decompiled Minecraft 1.21.1 sources with its assets')
    if aw2 and not AW2_112_SOURCE:
        raise SystemExit('set AW2_112_SOURCE to a checkout of the Ancient Warfare 2 1.12.x sources')

# 1.12 ids that were renamed by the 1.13 flattening without a meta split (applied when the flattening fix has no entry)
PRE_113 = {
    'melon_block': 'melon', 'melon': 'melon_slice', 'speckled_melon': 'glistering_melon_slice',
    'fireworks': 'firework_rocket', 'firework_charge': 'firework_star', 'netherbrick': 'nether_brick',
    'nether_brick': 'nether_bricks', 'noteblock': 'note_block', 'golden_rail': 'powered_rail', 'web': 'cobweb',
    'deadbush': 'dead_bush', 'yellow_flower': 'dandelion', 'brick_block': 'bricks', 'mob_spawner': 'spawner',
    'stone_stairs': 'cobblestone_stairs', 'wooden_pressure_plate': 'oak_pressure_plate', 'snow_layer': 'snow',
    'snow': 'snow_block', 'fence': 'oak_fence', 'pumpkin': 'carved_pumpkin', 'lit_pumpkin': 'jack_o_lantern',
    'trapdoor': 'oak_trapdoor', 'fence_gate': 'oak_fence_gate', 'waterlily': 'lily_pad',
    'end_bricks': 'end_stone_bricks', 'wooden_button': 'oak_button', 'quartz_ore': 'nether_quartz_ore',
    'hardened_clay': 'terracotta', 'slime': 'slime_block', 'magma': 'magma_block',
    'red_nether_brick': 'red_nether_bricks', 'wooden_door': 'oak_door', 'boat': 'oak_boat', 'reeds': 'sugar_cane',
    'chorus_fruit_popped': 'popped_chorus_fruit', 'grass': 'grass_block', 'lit_furnace': 'furnace',
    'totem': 'totem_of_undying', 'record_13': 'music_disc_13', 'record_cat': 'music_disc_cat',
    'record_blocks': 'music_disc_blocks', 'record_chirp': 'music_disc_chirp', 'record_far': 'music_disc_far',
    'record_mall': 'music_disc_mall', 'record_mellohi': 'music_disc_mellohi', 'record_stal': 'music_disc_stal',
    'record_strad': 'music_disc_strad', 'record_ward': 'music_disc_ward', 'record_11': 'music_disc_11',
    'record_wait': 'music_disc_wait',
}
# 1.13 ... 1.21 renames (applied once to the flattened id)
POST_113 = {
    'rose_red': 'red_dye', 'dandelion_yellow': 'yellow_dye', 'cactus_green': 'green_dye', 'sign': 'oak_sign',
    'stone_slab': 'smooth_stone_slab', 'grass_path': 'dirt_path', 'grass': 'short_grass', 'scute': 'turtle_scute',
    'zombie_pigman_spawn_egg': 'zombified_piglin_spawn_egg',
}

# ore dictionary name -> ingredient. Tags of the "c" namespace are NeoForge's common tags; the AW ones
# (c:ingots/steel, c:gears/*, c:bearings/*, c:shafts/*, ancientwarfare:food_bundle) are defined by the mod's data.
ORES = {
    'string': {'tag': 'c:strings'}, 'ingotIron': {'tag': 'c:ingots/iron'}, 'leather': {'tag': 'c:leathers'},
    'stickWood': {'tag': 'c:rods/wooden'}, 'plankWood': {'tag': 'minecraft:planks'},
    'blockCactus': {'tag': 'c:crops/cactus'}, 'paper': {'item': 'minecraft:paper'}, 'feather': {'tag': 'c:feathers'},
    'chest': {'tag': 'c:chests'}, 'chestWood': {'tag': 'c:chests/wooden'}, 'gunpowder': {'tag': 'c:gunpowders'},
    'torch': {'item': 'minecraft:torch'}, 'ingotGold': {'tag': 'c:ingots/gold'}, 'dustRedstone': {'tag': 'c:dusts/redstone'},
    'ingotSteel': {'tag': 'c:ingots/steel'}, 'stone': {'tag': 'c:stones'}, 'netherrack': {'tag': 'c:netherracks'},
    'gemDiamond': {'tag': 'c:gems/diamond'}, 'gemEmerald': {'tag': 'c:gems/emerald'}, 'cropWheat': {'tag': 'c:crops/wheat'},
    'sand': {'tag': 'c:sands'}, 'obsidian': {'tag': 'c:obsidians'}, 'logWood': {'tag': 'minecraft:logs'},
    'cobblestone': {'tag': 'c:cobblestones'}, 'workbench': {'tag': 'c:player_workstations/crafting_tables'},
    'paneGlass': {'tag': 'c:glass_panes'}, 'fenceWood': {'tag': 'c:fences/wooden'}, 'enderpearl': {'tag': 'c:ender_pearls'},
    'gearWood': {'tag': 'c:gears/wooden'}, 'gearIron': {'tag': 'c:gears/iron'}, 'gearSteel': {'tag': 'c:gears/steel'},
    'bearingWood': {'tag': 'c:bearings/wooden'}, 'bearingIron': {'tag': 'c:bearings/iron'}, 'bearingSteel': {'tag': 'c:bearings/steel'},
    'shaftWood': {'tag': 'c:shafts/wooden'}, 'shaftIron': {'tag': 'c:shafts/iron'}, 'shaftSteel': {'tag': 'c:shafts/steel'},
    'foodBundle': {'tag': 'ancientwarfare:food_bundle'},
    'dyeBlack': {'tag': 'c:dyes/black'}, 'dyeRed': {'tag': 'c:dyes/red'}, 'dyeGreen': {'tag': 'c:dyes/green'},
    'dyeBrown': {'tag': 'c:dyes/brown'}, 'dyeBlue': {'tag': 'c:dyes/blue'}, 'dyePurple': {'tag': 'c:dyes/purple'},
    'dyeCyan': {'tag': 'c:dyes/cyan'}, 'dyeLightGray': {'tag': 'c:dyes/light_gray'}, 'dyeGray': {'tag': 'c:dyes/gray'},
    'dyePink': {'tag': 'c:dyes/pink'}, 'dyeLime': {'tag': 'c:dyes/lime'}, 'dyeYellow': {'tag': 'c:dyes/yellow'},
    'dyeLightBlue': {'tag': 'c:dyes/light_blue'}, 'dyeMagenta': {'tag': 'c:dyes/magenta'}, 'dyeOrange': {'tag': 'c:dyes/orange'},
    'dyeWhite': {'tag': 'c:dyes/white'},
}

MODULE_NS = re.compile(r'^ancientwarfare(automation|npc|structure|vehicle)?:')

_flat = None
_aw_meta = None
_known = None
problems = []


def problem(msg):
    if msg not in problems:
        problems.append(msg)


def _load_flattening():
    global _flat
    if _flat is None:
        _flat = {}
        require_sources(mc=True)
        path = os.path.join(MC_SRC, 'net/minecraft/util/datafix/fixes/ItemStackTheFlatteningFix.java')
        with open(path, encoding='utf-8') as f:
            for m in re.finditer(r'put\("([^"]+)\.(\d+)", "([^"]+)"\)', f.read()):
                _flat[(m.group(1), int(m.group(2)))] = m.group(3)
    return _flat


def _load_aw_meta():
    global _aw_meta
    if _aw_meta is None:
        path = os.path.join(HERE, 'aw_item_meta_map.json')
        _aw_meta = json.load(open(path, encoding='utf-8')) if os.path.exists(path) else {}
    return _aw_meta


def known_items(repo):
    """ids of all vanilla items plus the items of the port (every item has a models/item/<id>.json file)"""
    global _known
    if _known is None:
        _known = set()
        require_sources(mc=True)
        for ns, d in (('minecraft', os.path.join(MC_SRC, 'assets/minecraft/models/item')),
                      ('ancientwarfare', os.path.join(repo, 'src/main/resources/assets/ancientwarfare/models/item'))):
            if os.path.isdir(d):
                for n in os.listdir(d):
                    if n.endswith('.json'):
                        _known.add(ns + ':' + n[:-5])
        # the reference tree has no client assets (no vanilla item models): fall back to the item registry list
        # (tools/vanilla_items_1_21_1.txt, from the vanilla data generator's reports/registries.json)
        listed = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'vanilla_items_1_21_1.txt')
        if os.path.exists(listed):
            _known.update(x.strip() for x in open(listed, encoding='utf-8') if x.strip())
    return _known


def map_namespace(name):
    if ':' not in name:
        name = 'minecraft:' + name
    return MODULE_NS.sub('ancientwarfare:', name)


def flatten_item(name, meta=None):
    """1.12 item id (+ meta, None/-1/32767 = not given / wildcard) -> 1.21.1 item id"""
    name = map_namespace(name)
    wildcard = meta is None or meta in (-1, 32767)
    m = 0 if wildcard else int(meta)
    if name.startswith('ancientwarfare:'):
        entry = _load_aw_meta().get(name)
        if entry is None:
            if m != 0:
                problem('AW item with meta and no entry in aw_item_meta_map.json: %s meta %s' % (name, m))
            return name
        if str(m) in entry and not (wildcard and '*' in entry):
            return entry[str(m)]
        if '*' in entry:
            return entry['*']
        problem('aw_item_meta_map.json has no meta %s for %s' % (m, name))
        return name
    flat = _load_flattening()
    if (name, m) in flat:
        name = flat[(name, m)]
    elif any(k[0] == name for k in flat):
        problem('no flattening for %s meta %s' % (name, m))
    elif name.startswith('minecraft:') and name[10:] in PRE_113:
        # (items without flattening entry: a meta was the damage value and is ignored)
        name = 'minecraft:' + PRE_113[name[10:]]
    if name.startswith('minecraft:') and name[10:] in POST_113:
        name = 'minecraft:' + POST_113[name[10:]]
    return name


def all_variants(name):
    """for wildcard meta ingredients: every flattened variant of a 1.12 vanilla item id"""
    name = map_namespace(name)
    flat = _load_flattening()
    metas = sorted(k[1] for k in flat if k[0] == name)
    if not metas:
        return [flatten_item(name, 0)]
    out = []
    for m in metas:
        v = flatten_item(name, m)
        if v not in out:
            out.append(v)
    return out


def ore_ingredient(ore):
    if ore not in ORES:
        problem('unknown ore dictionary name: ' + ore)
        return {'tag': 'c:' + ore.lower()}
    return dict(ORES[ore])


def check_item(repo, name, where):
    if name not in known_items(repo):
        problem('unknown item %s (%s)' % (name, where))
        return False
    return True


def custom_data(nbt):
    """1.12 stack nbt (json object or snbt string) -> 1.21.1 components map (AW item tags live in custom_data)"""
    return {'minecraft:custom_data': nbt}


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')
