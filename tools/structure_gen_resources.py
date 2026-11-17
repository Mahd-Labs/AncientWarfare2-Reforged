#!/usr/bin/env python3
"""
Generates the vanilla 1.21.1 resources of the structure module from the 1.12 ones:
  - block models: models/block/structure/*.json (copied, texture paths fixed)
  - blockstates: 1.12 Forge format (defaults + per property partial variants + submodels) -> vanilla multipart files
    (one "when" per full state, base model + sub models); only properties the ported block really has are used
    (parsed from the block classes)
  - blocks that were drawn by a TESR only (no 1.12 blockstate file): particle-only model
  - item models: the 1.12 "inventory" variant / custom mesh definition of each block item; items whose look depends
    on the stack NBT get overrides on the item property "ancientwarfare:variant" (registered by
    AncientWarfareStructureClient); models that are made of sub models use the neoforge:composite loader
  - loot tables (blocks that drop themselves), mineable tags (from the 1.12 Material of each block)
usage: AW2_112_SOURCE=<1.12 AW2 checkout> structure_gen_resources.py <port repo>
"""
import itertools
import json
import os
import re
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
JAVA = os.path.join(repo, 'src/main/java/net/shadowmage/ancientwarfare/structure')

VANILLA_TEX = {'log_oak_top': 'oak_log_top', 'log_oak': 'oak_log', 'planks_oak': 'oak_planks', 'planks_spruce': 'spruce_planks',
               'planks_birch': 'birch_planks', 'planks_jungle': 'jungle_planks', 'planks_acacia': 'acacia_planks',
               'planks_big_oak': 'dark_oak_planks', 'fire_layer_0': 'fire_0', 'cobblestone': 'cobblestone',
               'wool_colored_red': 'red_wool', 'stick': 'stick'}
PROP_NAMES = {'FACING': 'facing', 'VISIBLE': 'visible', 'VARIANT': 'variant', 'TOP_BOTTOM_PART': 'part', 'AXIS': 'axis', 'LEGS': 'legs',
              'LIT': 'lit', 'PART': 'part', 'ON_FIRE': 'on_fire', 'INVISIBLE': 'invisible', 'DOUBLE': 'double', 'BOTTOM_PART': 'bottom_part',
              'STACK_SIZE': 'size', 'ROTATION': 'rotation', 'TRANSPARENT': 'transparent', 'LEG_NORTHEAST': 'leg_northeast',
              'LEG_NORTHWEST': 'leg_northwest', 'LEG_SOUTHEAST': 'leg_southeast', 'LEG_SOUTHWEST': 'leg_southwest'}
# particle texture of the blocks without a 1.12 blockstate file (drawn by block entity renderers)
PARTICLE_ONLY = {'gate_proxy': 'minecraft:block/oak_planks', 'sound_block': 'minecraft:block/jukebox_side',
                 'advanced_loot_chest': 'minecraft:block/oak_planks', 'protection_flag': 'minecraft:block/oak_planks',
                 'decorative_flag': 'minecraft:block/oak_planks', 'wooden_coffin': 'minecraft:block/oak_planks',
                 'stone_coffin': 'minecraft:block/stone', 'statue': 'minecraft:block/stone'}
# Textures of the 1.12 mod that are copies (or near-copies) of vanilla 1.12 textures: not shipped; the models use the
# vanilla 1.21.1 texture instead (licensing, release candidate; tools/licensing_replace_vanilla_copies.py --check).
MOJANG_COPIES = {'ancientwarfare:block/structure/builder_bottom': 'minecraft:block/stone_bricks',
                 'ancientwarfare:block/structure/altar_long_cloth_bottom': 'minecraft:block/oak_planks',
                 'ancientwarfare:block/structure/altar_short_cloth_bottom': 'minecraft:block/oak_planks',
                 'ancientwarfare:block/structure/totem_bottom': 'minecraft:block/stone'}
WOOD = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak']
TOTEM = ['base', 'mid', 'mid_alt', 'top', 'wings', 'irminsul_base', 'irminsul_mid', 'irminsul_top', 'irminsul_side', 'elephant_left',
         'elephant_right', 'elephant_body', 'xoltec_idol_body_left', 'xoltec_idol_body_right', 'xoltec_idol_head_left', 'xoltec_idol_head_right']
report = []


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent='\t')
        f.write('\n')


def tex(t):
    if ':' not in t:
        t = 'minecraft:' + t
    ns, path = t.split(':', 1)
    if path.startswith('blocks/'):
        path = 'block/' + path[7:]
    elif path.startswith('items/'):
        path = 'item/' + path[6:]
    if ns == 'minecraft':
        kind, name = path.split('/', 1)
        path = kind + '/' + VANILLA_TEX.get(name, name)
    path = path.lower()
    if ns + ':' + path in MOJANG_COPIES:
        return MOJANG_COPIES[ns + ':' + path]
    full = os.path.join(ASSETS, 'textures', path + '.png')
    if ns == 'ancientwarfare' and not os.path.exists(full):
        report.append('missing texture ' + path)
    return ns + ':' + path


def model_ref(m):
    if m in ('builtin/generated', 'minecraft:builtin/generated'):
        return 'minecraft:item/generated'
    if ':' not in m:
        m = 'minecraft:' + m
    ns, path = m.split(':', 1)
    if path.startswith('block/'):
        path = path[6:]
    return ns + ':block/' + path


def copy_models():
    src_dir = os.path.join(REF, 'models/block/structure')
    for n in sorted(os.listdir(src_dir)):
        d = json.load(open(os.path.join(src_dir, n), encoding='utf-8'))
        if 'textures' in d:
            d['textures'] = {k: (v if v.startswith('#') else tex(v)) for k, v in d['textures'].items()}
        if 'parent' in d:
            d['parent'] = model_ref(d['parent'])
        else:
            d['parent'] = 'minecraft:block/block'
        # 1.12 getBlockLayer CUTOUT (per block) -> render type in the model; harmless for opaque textures
        d.setdefault('render_type', 'minecraft:cutout')
        for k in ('credit', '__comment'):
            d.pop(k, None)
        write(os.path.join(ASSETS, 'models/block/structure', n.lower()), d)


def block_properties():
    """registry name -> list of property names, parsed from AWStructureBlocks and the block classes"""
    init = open(os.path.join(JAVA, 'init/AWStructureBlocks.java'), encoding='utf-8').read()
    result = {}
    for m in re.finditer(r'DeferredBlock<(\w+)> \w+ = BLOCKS\.register\("(\w+)"', init):
        cls, name = m.group(1), m.group(2)
        props = []
        c = cls
        while c and c not in ('BlockBaseStructure', 'BlockBase', 'Block', 'ChestBlock'):
            path = None
            for sub in ('block', 'block/altar'):
                cand = os.path.join(JAVA, sub, c + '.java')
                if os.path.exists(cand):
                    path = cand
            if not path:
                break
            t = open(path, encoding='utf-8').read()
            for mm in re.finditer(r'builder\.add\(([^)]*)\)', t):
                for p in mm.group(1).split(','):
                    p = p.strip().split('.')[-1]
                    if p in PROP_NAMES and PROP_NAMES[p] not in props:
                        props.append(PROP_NAMES[p])
            mm = re.search(r'getAdditionalProperties\(\) \{\n\t\treturn Collections\.singletonList\((\w+)\)', t)
            if mm and PROP_NAMES.get(mm.group(1)) not in props:
                props.append(PROP_NAMES[mm.group(1)])
            mm = re.search(r'class ' + c + r'(?:<[^{]*?>)? extends (\w+)', t)
            c = mm.group(1) if mm else None
        if cls == 'BlockAdvancedLootChest':
            props = ['facing', 'type', 'waterlogged']
        result[name] = props
    return result


def first(v):
    return v[0] if isinstance(v, list) else v


def merge(base, part):
    out = dict(base)
    part = first(part) or {}
    for k, v in part.items():
        if k == 'textures':
            t = dict(out.get('textures', {}))
            t.update(v)
            out['textures'] = t
        elif k == 'submodel':
            s = dict(out.get('submodel', {}))
            if isinstance(v, dict):
                s.update(v)
            out['submodel'] = s
        else:
            out[k] = v
    return out


made_models = {}


def variant_model(block, model, textures, hint):
    """a model json for (model, texture overrides); returns its resource name"""
    if model is None:
        return None
    ref = model_ref(model)
    if not textures:
        return ref
    textures = {k: tex(v) for k, v in textures.items()}
    sig = json.dumps([ref, textures], sort_keys=True)
    if sig not in made_models:
        base = ref.split('/')[-1]
        name = base + '_' + hint if hint else base + '_tex'
        used = set(made_models.values())
        i = 2
        final = name
        while 'ancientwarfare:block/structure/generated/' + final in used:
            final = '%s_%d' % (name, i)
            i += 1
        made_models[sig] = 'ancientwarfare:block/structure/generated/' + final
        write(os.path.join(ASSETS, 'models/block/structure/generated', final + '.json'), dict({'parent': ref, 'textures': textures}, **({'render_type': 'minecraft:cutout'} if ref.startswith('minecraft:block/') else {})))  # F-014: vanilla parents have no cutout; transparent textures showed their hidden colour
    return made_models[sig]


def applies(block, eff, hint):
    """list of blockstate 'apply' dicts (base model + sub models) for one effective variant"""
    out = []
    tx = eff.get('textures', {})
    base = variant_model(block, eff.get('model'), tx, hint)
    if base:
        e = {'model': base}
        for k in ('x', 'y'):
            if eff.get(k):
                e[k] = eff[k] % 360
        if eff.get('uvlock'):
            e['uvlock'] = True
        out.append(e)
    for sname, sub in sorted(eff.get('submodel', {}).items()):
        sub = first(sub) or {}
        if not sub.get('model'):
            continue
        stx = dict(tx)
        stx.update(sub.get('textures', {}))
        m = variant_model(block, sub['model'], stx, hint)
        e = {'model': m}
        # F-027: Forge's MultiModel composes the variant's rotation into every sub model (1.12 turned the whole bench)
        for k in ('x', 'y'):
            v = (sub.get(k, 0) + eff.get(k, 0)) % 360
            if v:
                e[k] = v
        out.append(e)
    return out


def convert_blockstate(name, props):
    src = json.load(open(os.path.join(REF, 'blockstates/structure', name + '.json'), encoding='utf-8'))
    defaults = src.get('defaults', {})
    variants = src['variants']
    full_keys = [k for k in variants if '=' in k]
    multipart = []
    state_models = {}
    inventory = None
    if 'inventory' in variants:
        inventory = merge(defaults, variants['inventory'])
    if full_keys:
        for key in full_keys:
            eff = merge(defaults, variants[key])
            when = dict(kv.split('=') for kv in key.split(','))
            when = {k: v for k, v in when.items() if k in props}
            hint = '_'.join(v for k, v in sorted(when.items()) if eff.get('textures') != defaults.get('textures'))
            aps = applies(name, eff, hint)
            state_models[key] = aps
            for a in aps:
                multipart.append({'when': when, 'apply': a} if when else {'apply': a})
    else:
        keys = [k for k in variants if k not in ('normal', 'inventory') and isinstance(variants[k], dict)]
        used = [k for k in keys if k in props]
        dropped = [k for k in keys if k not in props]
        if dropped:
            report.append('%s: 1.12 blockstate properties not on the ported block (first value used): %s' % (name, dropped))
        missing = [p for p in props if p not in keys]
        if missing:
            report.append('%s: block properties without 1.12 blockstate entry (ignored by the model): %s' % (name, missing))
        for combo in itertools.product(*[list(variants[k].keys()) for k in used]):
            eff = dict(defaults)
            for k in keys:
                if k in used:
                    eff = merge(eff, variants[k][combo[used.index(k)]])
                else:
                    vals = variants[k]
                    pick = 'north' if 'north' in vals else 'true' if (k == 'visible' and 'true' in vals) else next(iter(vals))
                    eff = merge(eff, vals[pick])
            hint = '_'.join(v for k, v in zip(used, combo) if first(variants[k][v]) and first(variants[k][v]).get('textures'))
            aps = applies(name, eff, hint)
            when = dict(zip(used, combo))
            state_models[','.join('%s=%s' % kv for kv in zip(used, combo))] = aps
            for a in aps:
                multipart.append({'when': when, 'apply': a} if when else {'apply': a})
    if not multipart:
        report.append('%s: no model in any state' % name)
        multipart.append({'apply': {'model': 'minecraft:block/air'}})
    write(os.path.join(ASSETS, 'blockstates', name + '.json'), {'multipart': multipart})
    return state_models, (applies(name, inventory, 'inventory') if inventory else None)


def item_model_for(aps):
    """item model json for a list of apply entries (1 entry: parent; several: neoforge composite)"""
    if not aps:
        return {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'minecraft:item/barrier'}}
    if len(aps) == 1 and not aps[0].get('y') and not aps[0].get('x'):
        return {'parent': aps[0]['model']}
    children = {}
    for i, a in enumerate(aps):
        child = {'parent': a['model']}
        if a.get('y') or a.get('x'):
            child['transform'] = {'rotation': [{'y': -a.get('y', 0)}, {'x': -a.get('x', 0)}], 'origin': 'center'}
        children['part%d' % i] = child
    return {'parent': 'minecraft:block/block', 'loader': 'neoforge:composite', 'children': children, 'textures': {'particle': 'minecraft:block/oak_planks'}}


def pick(state_models, **wanted):
    best = None
    for key, aps in state_models.items():
        kv = dict(x.split('=') for x in key.split(',')) if key else {}
        if all(kv.get(k, v) == v for k, v in wanted.items()):
            score = sum(1 for k, v in (('facing', 'north'), ('visible', 'true'), ('part', 'bottom'), ('lit', 'false'), ('axis', 'x'), ('legs', 'both'),
                                         ('leg_northeast', 'true'), ('leg_northwest', 'true'), ('leg_southeast', 'true'), ('leg_southwest', 'true'),
                                         ('bottom_part', 'true'), ('double', 'false'), ('transparent', 'false'), ('on_fire', 'false'), ('variant', 'oak'))
                        if kv.get(k, v) == v)
            if best is None or score > best[0]:
                best = (score, aps)
    return best[1] if best else None


def item_with_overrides(name, models):
    """models: list of item model dicts, index = value of the ancientwarfare:variant item property"""
    base = dict(models[0])
    overrides = []
    for i, m in enumerate(models):
        if i == 0:
            continue
        write(os.path.join(ASSETS, 'models/item', '%s_variant_%d.json' % (name, i)), m)
        overrides.append({'predicate': {'ancientwarfare:variant': i}, 'model': 'ancientwarfare:item/%s_variant_%d' % (name, i)})
    if overrides:
        base['overrides'] = overrides
    write(os.path.join(ASSETS, 'models/item', name + '.json'), base)


def main():
    copy_models()
    props = block_properties()
    all_state_models = {}
    inventories = {}
    for name in sorted(props):
        if os.path.exists(os.path.join(REF, 'blockstates/structure', name + '.json')):
            all_state_models[name], inventories[name] = convert_blockstate(name, props[name])
        else:
            particle = PARTICLE_ONLY.get(name)
            if particle is None:
                report.append('%s: no 1.12 blockstate and no particle texture configured' % name)
                particle = 'minecraft:block/stone'
            write(os.path.join(ASSETS, 'models/block/structure', name + '_particle.json'), {'textures': {'particle': particle}})
            # the sound block is drawn as the block it is disguised as (1.12 default: a jukebox); the disguise renderer is
            # not ported, so it always shows the jukebox model
            model = 'minecraft:block/jukebox' if name == 'sound_block' else 'ancientwarfare:block/structure/%s_particle' % name
            write(os.path.join(ASSETS, 'blockstates', name + '.json'), {'variants': {'': {'model': model}}})
    # ---- item models of the blocks
    for name in sorted(props):
        sm = all_state_models.get(name)
        target = os.path.join(ASSETS, 'models/item', name + '.json')
        if name in ('stool', 'table', 'chair', 'bench'):
            item_with_overrides(name, [item_model_for(pick(sm, variant=w)) for w in WOOD])
        elif name == 'fire_pit':
            item_with_overrides(name, [item_model_for(pick(sm, variant=v, lit=l)) for v in ('default', 'log', 'square') for l in ('false', 'true')])
        elif name in ('brazier_ember', 'brazier_flame'):
            item_with_overrides(name, [item_model_for(pick(sm, lit=l)) for l in ('false', 'true')])
        elif name.startswith('coin_stack_'):
            item_with_overrides(name, [item_model_for(pick(sm, size=str(s))) for s in (8, 8, 16, 24, 32, 40, 48, 56, 64)])
        elif name == 'gravestone':
            item_with_overrides(name, [item_model_for(pick(sm, variant=str(v), facing='west')) for v in (8, 1, 2, 3, 4, 5, 6, 7, 8)])
        elif name == 'totem_part':
            item_with_overrides(name, [item_model_for(pick(sm, variant=v, visible='true', facing='west')) for v in TOTEM])  # 1.12 mesh definition: facing=west (F-012)
        elif sm is None:
            if name == 'sound_block':
                write(target, {'parent': 'minecraft:block/jukebox'})
            elif name in ('wooden_coffin', 'stone_coffin', 'protection_flag', 'decorative_flag', 'advanced_loot_chest', 'gate_proxy'):
                # hand written (not generated): builtin/entity models whose "display" block is the 1.12 IItemRenderer TRANSFORMS
                # map (render/StructureItemRenderer draws them); gate_proxy is an empty model (1.12 DummyBakedModel)
                pass
            elif name not in ('statue', 'golden_throne'):
                # drawn by a block entity / item renderer in 1.12: placeholder cube until the item renderer is ported
                write(target, {'parent': 'minecraft:block/cube_all', 'textures': {'all': PARTICLE_ONLY.get(name, 'minecraft:block/stone')}})
                report.append('%s: item has no model (1.12 item renderer), placeholder cube' % name)
        else:
            # 1.12 registered these items with the blockstate variant "normal" (no rotation = the facing=south entry) (F-012)
            normal = name in ('wooden_throne', 'tribal_chair', 'scissor_seat', 'drafting_station')
            aps = inventories.get(name) or (pick(sm, facing='south') if normal else None) or pick(sm)
            write(target, item_model_for(aps))
    # ---- plain items
    item_dir = os.path.join(REF, 'models/item/structure')
    for n in sorted(os.listdir(item_dir)):
        d = json.load(open(os.path.join(item_dir, n), encoding='utf-8'))
        if 'textures' in d:
            d['textures'] = {k: (v if v.startswith('#') else tex(v)) for k, v in d['textures'].items()}
        if 'parent' in d:
            p = d['parent']
            if p.startswith('ancientwarfare:') and 'structure/' in p:
                d['parent'] = model_ref(p)
            elif ':' not in p and not p.startswith('item/') and not p.startswith('block/') and not p.startswith('builtin'):
                d['parent'] = 'minecraft:' + p
        target = os.path.join(ASSETS, 'models/item', n.lower())
        if n[:-5] in props and os.path.exists(target) and n[:-5] not in ('statue', 'golden_throne'):
            continue
        write(target, d)
    # gate spawner: one item per gate type (1.12 blockstates/structure/gate_spawner.json, "variant" = gate name)
    gs = json.load(open(os.path.join(REF, 'blockstates/structure/gate_spawner.json'), encoding='utf-8'))
    gate_names = {0: 'gate_wood_basic', 1: 'gate_iron_basic', 4: 'gate_wood_single', 5: 'gate_iron_single', 8: 'gate_wood_double', 9: 'gate_iron_double', 12: 'gate_drawbridge'}
    gvars = {k.lower(): v for k, v in gs['variants'].get('variant', {}).items()}
    rest = [k for k in gvars if k not in gate_names.values()]
    if 'gate_drawbridge' not in gvars and len(rest) == 1:
        gate_names[12] = rest[0]
    for gid, gname in gate_names.items():
        eff = merge(gs.get('defaults', {}), gvars.get(gname.lower(), {}))
        textures = {k: tex(v) for k, v in eff.get('textures', {}).items()}
        parent = eff.get('model', 'builtin/generated')
        if parent in ('builtin/generated', 'minecraft:builtin/generated'):
            parent = 'minecraft:item/generated'
        if not textures:
            report.append('gate spawner %d (%s): no texture found in the 1.12 blockstate' % (gid, gname))
        write(os.path.join(ASSETS, 'models/item', 'gate_spawner_%d.json' % gid), {'parent': parent, 'textures': textures})
    # ---- loot tables: blocks whose class (or super class) does not build its drops in code
    custom_drop = set()
    no_loot = set()
    for sub in ('block', 'block/altar'):
        for n in os.listdir(os.path.join(JAVA, sub)):
            if not n.endswith('.java'):
                continue
            t = open(os.path.join(JAVA, sub, n), encoding='utf-8').read()
            if re.search(r'public void getDrops\(NonNullList<ItemStack>', t) and n != 'BlockBaseStructure.java':
                custom_drop.add(n[:-5])
            if '.noLootTable()' in t:
                no_loot.add(n[:-5])
    init = open(os.path.join(JAVA, 'init/AWStructureBlocks.java'), encoding='utf-8').read()
    classes = {m.group(2): m.group(1) for m in re.finditer(r'DeferredBlock<(\w+)> \w+ = BLOCKS\.register\("(\w+)"', init)}
    parents = {'BlockWoodenCoffin': 'BlockCoffin', 'BlockStoneCoffin': 'BlockCoffin', 'BlockProtectionFlag': 'BlockFlag', 'BlockDecorativeFlag': 'BlockFlag'}
    tags = {'axe': [], 'pickaxe': [], 'shovel': []}
    for name, cls in sorted(classes.items()):
        t = open(os.path.join(JAVA, 'block/altar' if os.path.exists(os.path.join(JAVA, 'block/altar', cls + '.java')) else 'block', cls + '.java'), encoding='utf-8').read()
        m = re.search(r'Material\.(\w+)\.props\(\)', t)
        material = m.group(1) if m else {'BlockAdvancedLootChest': 'WOOD'}.get(cls)
        if name == 'loot_basket':
            material = 'WOOD'
        tool = {'WOOD': 'axe', 'ROCK': 'pickaxe', 'IRON': 'pickaxe', 'CLAY': 'shovel', 'GROUND': 'shovel', 'GRASS': 'shovel'}.get(material)
        if tool:
            tags[tool].append('ancientwarfare:' + name)
        else:
            report.append('%s: no material found for the mineable tag' % name)
        lt = os.path.join(DATA, 'ancientwarfare/loot_table/blocks', name + '.json')
        if cls in custom_drop or parents.get(cls) in custom_drop or cls in no_loot:
            if os.path.exists(lt):
                os.remove(lt)
            continue
        drop = 'minecraft:chest' if name == 'advanced_loot_chest' else 'ancientwarfare:' + name
        write(lt, {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': drop}],
                                                         'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    for tool, blocks in tags.items():
        p = os.path.join(DATA, 'minecraft/tags/block/mineable', tool + '.json')
        cur = json.load(open(p, encoding='utf-8')) if os.path.exists(p) else {'replace': False, 'values': []}
        for b in blocks:
            if b not in cur['values']:
                cur['values'].append(b)
        write(p, cur)
    print('blocks', len(props), 'generated models', len(made_models))
    for r in sorted(set(report)):
        print('  ' + r)


main()
