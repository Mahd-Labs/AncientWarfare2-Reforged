#!/usr/bin/env python3
"""One-off generator for the core module's blockstates, models, tags and block loot tables.
usage: gen_core_resources.py <port repo>"""
import json
import os
import sys

import aw_legacy as L

repo = sys.argv[1]
RES = os.path.join(repo, 'src/main/resources')
ASSETS = os.path.join(RES, 'assets/ancientwarfare')
DATA = os.path.join(RES, 'data')


def fix_textures(node):
    if isinstance(node, dict):
        return {k: fix_textures(v) for k, v in node.items()}
    if isinstance(node, list):
        return [fix_textures(v) for v in node]
    if isinstance(node, str):
        return node.replace('ancientwarfare:blocks/', 'ancientwarfare:block/').replace('ancientwarfare:items/', 'ancientwarfare:item/')
    return node


def check_texture(tex):
    p = os.path.join(ASSETS, 'textures', tex.split(':', 1)[1] + '.png')
    if not os.path.exists(p):
        print('MISSING TEXTURE', tex)


# block models
for name in ('engineering_station', 'research_station', 'research_station_has_book'):
    model = fix_textures(json.load(open(os.path.join(L.REF_ASSETS, 'models/block/core', name + '.json'), encoding='utf-8')))
    model.setdefault('parent', 'minecraft:block/block')
    for t in model.get('textures', {}).values():
        if not t.startswith('#'):
            check_texture(t)
    L.write_json(os.path.join(ASSETS, 'models/block/core', name + '.json'), model)

# blockstates (1.12: south = model as is, west y90, north y180, east y270)
ROT = {'south': 0, 'west': 90, 'north': 180, 'east': 270}


def variant(model, y):
    v = {'model': 'ancientwarfare:block/core/' + model}
    if y:
        v['y'] = y
    return v


L.write_json(os.path.join(ASSETS, 'blockstates/engineering_station.json'),
             {'variants': {'facing=' + f: variant('engineering_station', y) for f, y in ROT.items()}})
variants = {}
for f, y in ROT.items():
    variants['facing=%s,has_book=false' % f] = variant('research_station', y)
    variants['facing=%s,has_book=true' % f] = variant('research_station_has_book', y)
L.write_json(os.path.join(ASSETS, 'blockstates/research_station.json'), {'variants': variants})

# item models
for name in ('engineering_station', 'research_station'):
    L.write_json(os.path.join(ASSETS, 'models/item', name + '.json'), {'parent': 'ancientwarfare:block/core/' + name})
src = os.path.join(L.REF_ASSETS, 'models/item/core')
for f in sorted(os.listdir(src)):
    model = fix_textures(json.load(open(os.path.join(src, f), encoding='utf-8')))
    for t in model.get('textures', {}).values():
        check_texture(t)
    names = ['backpack_%d' % i for i in range(4)] if f == 'backpack.json' else [f[:-5]]
    for n in names:
        L.write_json(os.path.join(ASSETS, 'models/item', n + '.json'), model)
COMPONENTS = ['wooden_gear', 'iron_gear', 'steel_gear', 'wooden_bearings', 'iron_bearings', 'steel_bearings', 'wooden_shaft', 'iron_shaft', 'steel_shaft']
for c in COMPONENTS:
    tex = 'ancientwarfare:item/automation/' + c
    check_texture(tex)
    L.write_json(os.path.join(ASSETS, 'models/item', 'component_' + c + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': tex}})


# tags (1.12 ore dictionary registrations of AWCoreItems)
def tag(path, values):
    L.write_json(os.path.join(DATA, path + '.json'), {'values': values})


tag('c/tags/item/ingots/steel', ['ancientwarfare:steel_ingot'])
tag('c/tags/item/ingots', ['#c:ingots/steel'])
for kind, sub in (('gears', 'gear'), ('bearings', 'bearings'), ('shafts', 'shaft')):
    for mat, prefix in (('wooden', 'wooden'), ('iron', 'iron'), ('steel', 'steel')):
        tag('c/tags/item/%s/%s' % (kind, mat), ['ancientwarfare:component_%s_%s' % (prefix, sub)])
    tag('c/tags/item/' + kind, ['#c:%s/%s' % (kind, m) for m in ('wooden', 'iron', 'steel')])
tag('minecraft/tags/block/mineable/axe', ['ancientwarfare:engineering_station', 'ancientwarfare:research_station'])

# block drops (1.12 blocks dropped themselves without any data)
for name in ('engineering_station', 'research_station'):
    L.write_json(os.path.join(DATA, 'ancientwarfare/loot_table/blocks', name + '.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'ancientwarfare:' + name}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})

# the stand-in crafting recipe for research recipes that need no research (see ResearchFallbackRecipe)
L.write_json(os.path.join(DATA, 'ancientwarfare/recipe/research_fallback.json'), {'type': 'ancientwarfare:research_fallback', 'category': 'misc'})
print('core resources written')
