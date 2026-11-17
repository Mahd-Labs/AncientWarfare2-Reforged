#!/usr/bin/env python3
"""
Converts AW2 1.12 recipe json into 1.21.1 datapack recipes.

usage: convert_recipes.py <port repo> <module folder> [<module folder> ...] [--research-registry] [--manual-registry]

  <module folder>  a folder name below assets/ancientwarfare/recipes and/or .../research_recipes in the 1.12
                   source (the checkout named by AW2_112_SOURCE; MC_SOURCES is needed too, see aw_legacy.py), e.g. core, automation, npc, structure, vehicle.
                   recipes/<module>/**.json           -> <repo>/src/main/resources/data/ancientwarfare/recipe/<module>/**.json
                   research_recipes/<module>/**.json  -> same folder (ids stay "ancientwarfare:<module>/<name>")
  --research-registry  also converts registry/research/*.json (research goals with their ingredient lists) into
                   <repo>/src/main/resources/assets/ancientwarfare/registry/research/
  --manual-registry    also copies registry/manual/** with item ids mapped to the 1.21.1 names

What is converted:
  type        forge:ore_shaped / minecraft:crafting_shaped        -> minecraft:crafting_shaped
              forge:ore_shapeless / minecraft:crafting_shapeless  -> minecraft:crafting_shapeless
              ancientwarfare:research_recipe                      -> ancientwarfare:research_shaped
              ancientwarfare:shapeless_research_recipe            -> ancientwarfare:research_shapeless
  ingredients "#CONSTANT" (from _constants.json), forge:ore_dict -> tag (see ORES in aw_legacy.py), item + data ->
              flattened item (data 32767 -> all variants), minecraft:item_nbt -> neoforge:components (custom_data),
              ancientwarfare:item_count / ore_dict_count (-> tag_count) / item_nbt_relaxed
  result      {"item", "data", "count", "nbt"} -> {"id", "count", "components": {"minecraft:custom_data": nbt}}

Item ids are checked against the vanilla items and the port's models/item folder; everything that could not be
mapped is listed at the end (exit code 1) - add the missing names to aw_legacy.py / aw_item_meta_map.json, or
register the item (and its model) first, then run again.
"""
import json
import os
import sys

import aw_legacy as L

repo = None
constants = {}


def load_constants(kind):
    path = os.path.join(L.REF_ASSETS, kind, '_constants.json')
    out = {}
    if os.path.exists(path):
        for e in json.load(open(path, encoding='utf-8')):
            out[e['name']] = e['ingredient']
    return out


def item_id(obj, where):
    name = L.flatten_item(obj['item'], obj.get('data'))
    L.check_item(repo, name, where)
    return name


def ingredient(obj, where, consts):
    if isinstance(obj, list):
        out = []
        for o in obj:
            c = ingredient(o, where, consts)
            out.extend(c if isinstance(c, list) else [c])
        return out
    if isinstance(obj, str):
        obj = {'item': obj}
    t = obj.get('type', 'minecraft:item')
    if ':' not in t:
        t = 'minecraft:' + t
    if t == 'minecraft:item' and obj.get('item', '').startswith('#'):
        const = consts.get(obj['item'][1:])
        if const is None:
            L.problem('unknown recipe constant %s (%s)' % (obj['item'], where))
            return {'item': 'minecraft:barrier'}
        return ingredient(const, where, consts)
    if t in ('forge:ore_dict', 'minecraft:ore_dict'):
        return L.ore_ingredient(obj['ore'])
    if t == 'minecraft:item':
        if obj.get('data') == 32767:
            variants = L.all_variants(obj['item'])
            for v in variants:
                L.check_item(repo, v, where)
            return [{'item': v} for v in variants] if len(variants) > 1 else {'item': variants[0]}
        return {'item': item_id(obj, where)}
    if t == 'minecraft:item_nbt':
        return {'type': 'neoforge:components', 'items': item_id(obj, where), 'components': L.custom_data(obj.get('nbt', {})), 'strict': False}
    if t == 'ancientwarfare:item_count':
        return {'type': 'ancientwarfare:item_count', 'item': item_id(obj, where), 'count': obj.get('count', 1)}
    if t == 'ancientwarfare:ore_dict_count':
        ore = L.ore_ingredient(obj['ore'])
        if 'tag' in ore:
            return {'type': 'ancientwarfare:tag_count', 'tag': ore['tag'], 'count': obj.get('count', 1)}
        return {'type': 'ancientwarfare:item_count', 'item': ore['item'], 'count': obj.get('count', 1)}
    if t == 'ancientwarfare:item_nbt_relaxed':
        out = {'type': 'ancientwarfare:item_nbt_relaxed', 'item': item_id(obj, where)}
        if 'nbt' in obj:
            out['components'] = L.custom_data(obj['nbt'])
        return out
    L.problem('unsupported ingredient type %s (%s)' % (t, where))
    return {'item': 'minecraft:barrier'}


def result(obj, where):
    out = {'id': item_id(obj, where), 'count': obj.get('count', 1)}
    if 'nbt' in obj:
        out['components'] = L.custom_data(obj['nbt'])
    return out


TYPES = {
    'forge:ore_shaped': 'minecraft:crafting_shaped', 'minecraft:crafting_shaped': 'minecraft:crafting_shaped',
    'forge:ore_shapeless': 'minecraft:crafting_shapeless', 'minecraft:crafting_shapeless': 'minecraft:crafting_shapeless',
    'ancientwarfare:research_recipe': 'ancientwarfare:research_shaped',
    'ancientwarfare:shapeless_research_recipe': 'ancientwarfare:research_shapeless',
}


def convert_recipe(data, where, consts):
    t = data.get('type', '')
    if ':' not in t:
        t = 'minecraft:' + t
    if t not in TYPES:
        L.problem('unsupported recipe type %s (%s)' % (t, where))
        return None
    out = {'type': TYPES[t]}
    if out['type'].startswith('minecraft:'):
        out['category'] = 'misc'
        if 'group' in data:
            out['group'] = data['group']
    else:
        out['research'] = data['research']
    if 'pattern' in data:
        out['pattern'] = data['pattern']
        out['key'] = {k: ingredient(v, where, consts) for k, v in data['key'].items()}
    else:
        out['ingredients'] = [ingredient(i, where, consts) for i in data['ingredients']]
    out['result'] = result(data['result'], where)
    return out


def convert_folder(kind, module, written):
    src = os.path.join(L.REF_ASSETS, kind, module)
    if not os.path.isdir(src):
        return 0
    consts = load_constants(kind)
    n = 0
    for root, _, names in os.walk(src):
        for name in sorted(names):
            if not name.endswith('.json') or name.startswith('_'):
                continue
            path = os.path.join(root, name)
            rel = os.path.relpath(path, os.path.join(L.REF_ASSETS, kind)).replace('\\', '/').lower()
            where = kind + '/' + rel
            out = convert_recipe(json.load(open(path, encoding='utf-8')), where, consts)
            if out is None:
                continue
            if rel in written:
                L.problem('recipe id used twice: %s (%s and %s)' % (rel, written[rel], where))
                continue
            written[rel] = where
            L.write_json(os.path.join(repo, 'src/main/resources/data/ancientwarfare/recipe', rel), out)
            n += 1
    return n


def convert_research_registry():
    src = os.path.join(L.REF_ASSETS, 'registry/research')
    n = 0
    for name in sorted(os.listdir(src)):
        if not name.endswith('.json'):
            continue
        data = json.load(open(os.path.join(src, name), encoding='utf-8'))
        data['resources'] = [ingredient(r, 'registry/research/' + name, {}) for r in data.get('resources', [])]
        L.write_json(os.path.join(repo, 'src/main/resources/assets/ancientwarfare/registry/research', name), data)
        n += 1
    return n


def map_manual_items(node, where):
    if isinstance(node, list):
        return [map_manual_items(n, where) for n in node]
    if isinstance(node, dict):
        if node.get('content_type') == 'item':
            def conv(it):
                if isinstance(it, str):
                    return L.flatten_item(it)
                out = dict(it)
                out['name'] = L.flatten_item(it['name'], it.get('data'))
                out.pop('data', None)
                return out
            out = dict(node)
            if 'items' in out:
                out['items'] = [conv(i) for i in out['items']]
            if 'name' in out:
                out = conv(out)
            return out
        return {k: map_manual_items(v, where) for k, v in node.items()}
    return node


def convert_manual_registry():
    src = os.path.join(L.REF_ASSETS, 'registry/manual')
    n = 0
    for root, _, names in os.walk(src):
        for name in sorted(names):
            path = os.path.join(root, name)
            rel = os.path.relpath(path, src).replace('\\', '/')
            dst = os.path.join(repo, 'src/main/resources/assets/ancientwarfare/registry/manual', rel.lower())
            if name.endswith('.json'):
                L.write_json(dst, map_manual_items(json.loads(open(path, encoding='utf-8-sig').read(), strict=False), 'registry/manual/' + rel))
            else:
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                with open(path, 'rb') as a, open(dst, 'wb') as b:
                    b.write(a.read())
            n += 1
    return n


def main():
    global repo
    L.require_sources(mc=True, aw2=True)
    args = sys.argv[1:]
    if len(args) < 2:
        print(__doc__)
        sys.exit(2)
    repo = args[0]
    written = {}
    for a in args[1:]:
        if a == '--research-registry':
            print('research registry files:', convert_research_registry())
        elif a == '--manual-registry':
            print('manual registry files:', convert_manual_registry())
        else:
            n1 = convert_folder('recipes', a, written)
            n2 = convert_folder('research_recipes', a, written)
            print('%s: %d recipes, %d research recipes' % (a, n1, n2))
    if L.problems:
        print('\nPROBLEMS (%d):' % len(L.problems))
        for p in L.problems:
            print(' - ' + p)
        sys.exit(1)


if __name__ == '__main__':
    main()
