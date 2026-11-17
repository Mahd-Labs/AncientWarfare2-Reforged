#!/usr/bin/env python3
"""
Generates the vanilla 1.21.1 blockstates / block models / item models, loot tables and mineable tags of the
automation module from the 1.12 Forge-format blockstates (assets/ancientwarfare/blockstates/automation/*.json).

usage: AW2_112_SOURCE=<1.12 AW2 checkout> automation_gen_resources.py <port repo>
The OBJ based torque blocks are listed in OBJ_BLOCKS: their blockstate points to a particle-only model (they are
drawn by block entity renderers), their item models are written by hand (models/item/*.json with the obj loader).
"""
import itertools
import json
import os
import sys

def _source(name, what):
    """a path from the environment (no default: these are inputs from outside this repository)"""
    value = os.environ.get(name)
    if not value:
        raise SystemExit('set %s to %s' % (name, what))
    return os.path.expanduser(value)


REF = os.path.join(_source('AW2_112_SOURCE', 'a checkout of the Ancient Warfare 2 1.12.x sources (github.com/P3pp3rF1y/AncientWarfare2)'), 'src/main/resources/assets/ancientwarfare')
repo = sys.argv[1]
ASSETS = os.path.join(repo, 'src/main/resources/assets/ancientwarfare')
DATA = os.path.join(repo, 'src/main/resources/data')

JSON_BLOCKS = ['quarry', 'tree_farm', 'crop_farm', 'fruit_farm', 'animal_farm', 'fish_farm', 'warehouse_control',
               'warehouse_storage', 'warehouse_interface', 'warehouse_crafting', 'warehouse_stock_viewer',
               'warehouse_stock_linker', 'auto_crafting', 'mailbox', 'chunk_loader_simple', 'chunk_loader_deluxe']
TIERS = ['light', 'medium', 'heavy']
# block -> (particle texture per tier or single)
OBJ_BLOCKS = {
    'flywheel_controller': 'flywheel_controller_%s', 'flywheel_storage': 'flywheel_small_%s',
    'torque_junction': 'torque_junction_%s', 'torque_shaft': 'torque_shaft_%s',
    'torque_distributor': 'torque_distributor_%s', 'stirling_generator': 'stirling_generator',
    'waterwheel_generator': 'waterwheel_generator', 'hand_cranked_generator': 'hand_cranked_generator',
    'windmill_blade': 'windmill_blade_cube', 'windmill_generator': 'windmill_generator',
}
PICKAXE = ['warehouse_interface', 'warehouse_crafting', 'warehouse_stock_viewer', 'warehouse_stock_linker', 'mailbox',
           'chunk_loader_simple', 'chunk_loader_deluxe', 'flywheel_controller', 'flywheel_storage', 'torque_junction',
           'torque_shaft', 'torque_distributor', 'stirling_generator', 'waterwheel_generator',
           'hand_cranked_generator', 'windmill_generator']
AXE = ['quarry', 'tree_farm', 'crop_farm', 'fruit_farm', 'animal_farm', 'fish_farm', 'warehouse_control',
       'warehouse_storage', 'auto_crafting', 'windmill_blade']
UPGRADES = ['bounds_medium', 'bounds_large', 'quarry_medium', 'quarry_large', 'enchanted_tools_1',
            'enchanted_tools_2', 'quality_tools_1', 'quality_tools_2', 'quality_tools_3', 'chunkloader_basic',
            'chunkloader_quarry']


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent='\t')
        f.write('\n')


def tex(t):
    return t.replace(':blocks/', ':block/').replace(':items/', ':item/')


def model_ref(m):
    if m in ('cube', 'cube_bottom_top', 'cube_all'):
        return 'minecraft:block/' + m
    ns, path = m.split(':')
    return ns + ':block/' + path


def convert_blockstate(name):
    src = json.load(open(os.path.join(REF, 'blockstates/automation', name + '.json'), encoding='utf-8'))
    defaults = src.get('defaults', {})
    props = {k: v for k, v in src['variants'].items() if k not in ('normal', 'inventory')}
    keys = sorted(props.keys())
    variants = {}
    models = {}
    first_model = None
    for combo in itertools.product(*[sorted(props[k].keys()) for k in keys]):
        model = defaults.get('model')
        textures = dict(defaults.get('textures', {}))
        y = 0
        for k, v in zip(keys, combo):
            d = props[k][v]
            model = d.get('model', model)
            textures.update(d.get('textures', {}))
            y = d.get('y', y)
        textures = {k: tex(v) for k, v in textures.items()}
        sig = json.dumps([model, textures], sort_keys=True)
        if sig not in models:
            # model name: block name + values of the properties that change the textures/model
            suffix = ''.join('_' + ('inactive' if (k, v) == ('active', 'false') else 'on' if (k, v) == ('lit', 'true') else v)
                             for k, v in zip(keys, combo)
                             if k != 'facing' and (props[k][v].get('textures') or props[k][v].get('model')) and not (k, v) in (('active', 'true'), ('lit', 'false')))
            mname = name + suffix
            if mname in [m for m in models.values()]:
                mname = name + '_' + '_'.join(combo)
            models[sig] = mname
            out = {'parent': model_ref(model)}
            if textures:
                if 'particle' not in textures:
                    textures['particle'] = textures.get('side') or textures.get('north') or next(iter(textures.values()))
                out['textures'] = textures
            write(os.path.join(ASSETS, 'models/block/automation', mname + '.json'), out)
            if first_model is None:
                first_model = mname
        entry = {'model': 'ancientwarfare:block/automation/' + models[sig]}
        if y:
            entry['y'] = y
        variants[','.join('%s=%s' % (k, v) for k, v in zip(keys, combo))] = entry
    write(os.path.join(ASSETS, 'blockstates', name + '.json'), {'variants': variants})
    return variants


def copy_model(name):
    src = json.load(open(os.path.join(REF, 'models/block/automation', name + '.json'), encoding='utf-8'))
    if 'textures' in src:
        src['textures'] = {k: tex(v) for k, v in src['textures'].items()}
    if 'parent' not in src:
        src['parent'] = 'minecraft:block/block'
    src.pop('credit', None)
    write(os.path.join(ASSETS, 'models/block/automation', name + '_base.json' if name != 'sign' else 'models/block/automation/sign.json').replace('models/block/automation/models/block/automation', 'models/block/automation'), src)


def drop_self(item):
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': item}],
                                                 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}


def drop_by_property(block, prop, values):
    entries = []
    for v in values:
        entries.append({'type': 'minecraft:item', 'name': 'ancientwarfare:%s_%s' % (block, v),
                        'conditions': [{'condition': 'minecraft:block_state_property', 'block': 'ancientwarfare:' + block,
                                        'properties': {prop: v}}]})
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': entries}],
                                                 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}


def main():
    # custom element models (referenced by the converted blockstates under their 1.12 names)
    for n in ('auto_crafting', 'sign', 'warehouse_stock_linker'):
        src = json.load(open(os.path.join(REF, 'models/block/automation', n + '.json'), encoding='utf-8'))
        if 'textures' in src:
            src['textures'] = {k: tex(v) for k, v in src['textures'].items()}
        src.setdefault('parent', 'minecraft:block/block')
        src.pop('credit', None)
        write(os.path.join(ASSETS, 'models/block/automation', n + '_template.json'), src)
    item_parent = {}
    for b in JSON_BLOCKS:
        variants = convert_blockstate(b)
        item_parent[b] = variants
    # the converted models whose parent is one of the templates
    for root, _, names in os.walk(os.path.join(ASSETS, 'models/block/automation')):
        for n in names:
            p = os.path.join(root, n)
            d = json.load(open(p, encoding='utf-8'))
            if d.get('parent') in ('ancientwarfare:block/automation/auto_crafting', 'ancientwarfare:block/automation/sign',
                                   'ancientwarfare:block/automation/warehouse_stock_linker') and not n.endswith('_template.json'):
                d['parent'] += '_template'
                write(p, d)
    # item models of the json blocks
    def default_variant(variants, wanted):
        for key, entry in variants.items():
            if all(w in key.split(',') for w in wanted):
                return entry['model']
        return next(iter(variants.values()))['model']
    for b in JSON_BLOCKS:
        v = item_parent[b]
        if b == 'warehouse_storage':
            for size in ('small', 'medium', 'large'):
                write(os.path.join(ASSETS, 'models/item', 'warehouse_storage_%s.json' % size), {'parent': default_variant(v, ['size=' + size])})
        else:
            wanted = [w for w in ('facing=north', 'active=true', 'lit=false') if any(w in k.split(',') for k in v)]
            write(os.path.join(ASSETS, 'models/item', b + '.json'), {'parent': default_variant(v, wanted)})
    for u in UPGRADES:
        write(os.path.join(ASSETS, 'models/item', 'worksite_upgrade_%s.json' % u),
              {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'ancientwarfare:item/automation/upgrade_' + u}})
    # obj blocks: particle only block models, drawn by block entity renderers
    for b, t in OBJ_BLOCKS.items():
        if '%s' in t:
            for tier in TIERS:
                write(os.path.join(ASSETS, 'models/block/automation', '%s_%s.json' % (b, tier)),
                      {'textures': {'particle': 'ancientwarfare:model/automation/' + t % tier}})
        else:
            write(os.path.join(ASSETS, 'models/block/automation', b + '.json'), {'textures': {'particle': 'ancientwarfare:model/automation/' + t}})
    facings6 = ['north', 'south', 'west', 'east', 'up', 'down']
    for b, t in OBJ_BLOCKS.items():
        variants = {}
        if b == 'windmill_blade':
            variants[''] = {'model': 'ancientwarfare:block/automation/windmill_blade'}
        elif b == 'flywheel_storage':
            for tier in TIERS:
                variants['tier=' + tier] = {'model': 'ancientwarfare:block/automation/%s_%s' % (b, tier)}
        elif '%s' in t:
            for tier in TIERS:
                variants['tier=' + tier] = {'model': 'ancientwarfare:block/automation/%s_%s' % (b, tier)}
        else:
            variants[''] = {'model': 'ancientwarfare:block/automation/' + b}
        write(os.path.join(ASSETS, 'blockstates', b + '.json'), {'variants': variants})
    # loot tables
    loot = os.path.join(DATA, 'ancientwarfare/loot_table/blocks')
    for b in JSON_BLOCKS + list(OBJ_BLOCKS.keys()):
        if b == 'warehouse_storage':
            table = drop_by_property(b, 'size', ['small', 'medium', 'large'])
        elif b in OBJ_BLOCKS and '%s' in OBJ_BLOCKS[b]:
            table = drop_by_property(b, 'tier', TIERS)
        else:
            table = drop_self('ancientwarfare:' + b)
        write(os.path.join(loot, b + '.json'), table)
    # mineable tags (merge with what is there)
    for tool, blocks in (('pickaxe', PICKAXE), ('axe', AXE)):
        p = os.path.join(DATA, 'minecraft/tags/block/mineable', tool + '.json')
        cur = json.load(open(p, encoding='utf-8')) if os.path.exists(p) else {'replace': False, 'values': []}
        for b in blocks:
            if 'ancientwarfare:' + b not in cur['values']:
                cur['values'].append('ancientwarfare:' + b)
        write(p, cur)
    print('done')


main()


def obj_item_models():
    """placeholder item models of the OBJ blocks (replaced by the BakeryModel at bake time; needed so that the item
    model location exists and tools/convert_recipes.py knows the item ids)"""
    for b, t in OBJ_BLOCKS.items():
        if '%s' in t:
            for tier in TIERS:
                write(os.path.join(ASSETS, 'models/item', '%s_%s.json' % (b, tier)), {'parent': 'ancientwarfare:block/automation/%s_%s' % (b, tier)})
        else:
            write(os.path.join(ASSETS, 'models/item', b + '.json'), {'parent': 'ancientwarfare:block/automation/' + b})


obj_item_models()
