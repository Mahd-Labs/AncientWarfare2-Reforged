#!/usr/bin/env python3
"""
Generates the item models of the (reduced) vehicle module from the 1.12 Forge blockstate files
assets/ancientwarfare/blockstates/vehicle/{ammo,misc,spawner}.json.

usage: AW2_112_SOURCE=<1.12 AW2 checkout> vehicle_gen_resources.py <port repo>

  ammo.json / misc.json  variant <registry path> -> models/item/<registry path>.json (item/generated, 1.12 texture)
  spawner.json           variant <config name>_<level> -> models/item/vehicle/spawner/<config name>_<level>.json;
                         models/item/spawner_<config name>.json is level 0 with overrides on the item property
                         "ancientwarfare:level" (registered in AncientWarfareVehiclesClient)
Only items of the ported vehicle types are written (SPAWNERS / MISC_SKIP below).
"""
import json
import os
import sys

def _source(name, what):
    """a path from the environment (no default: these are inputs from outside this repository)"""
    value = os.environ.get(name)
    if not value:
        raise SystemExit('set %s to %s' % (name, what))
    return os.path.expanduser(value)


REF = os.path.join(_source('AW2_112_SOURCE', 'a checkout of the Ancient Warfare 2 1.12.x sources (github.com/P3pp3rF1y/AncientWarfare2)'),
                   'src/main/resources/assets/ancientwarfare/blockstates/vehicle')
# config names of the ported vehicle types (VehicleRegistry)
SPAWNERS = ['catapult_stand', 'catapult_stand_turret', 'ballista_stand', 'ballista_stand_turret', 'cannon_stand',
            'cannon_stand_turret', 'hwacha', 'trebuchet_stand', 'trebuchet_stand_turret', 'trebuchet_giant']
MISC_SKIP = ['mobility_unit']


def tex(t):
    return t.replace(':items/', ':item/')


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def main():
    repo = sys.argv[1]
    models = os.path.join(repo, 'src/main/resources/assets/ancientwarfare/models/item')
    count = 0
    for name in ('ammo', 'misc'):
        variants = json.load(open(os.path.join(REF, name + '.json')))['variants']['variant']
        for variant, data in variants.items():
            if variant in MISC_SKIP:
                continue
            write(os.path.join(models, variant + '.json'), {'parent': 'item/generated', 'textures': {'layer0': tex(data['textures']['layer0'])}})
            count += 1
    variants = json.load(open(os.path.join(REF, 'spawner.json')))['variants']['variant']
    for config in SPAWNERS:
        levels = sorted(int(v[len(config) + 1:]) for v in variants if v.startswith(config + '_') and v[len(config) + 1:].isdigit())
        overrides = []
        for level in levels:
            layer = tex(variants['%s_%d' % (config, level)]['textures']['layer0'])
            write(os.path.join(models, 'vehicle/spawner', '%s_%d.json' % (config, level)), {'parent': 'item/generated', 'textures': {'layer0': layer}})
            if level > 0:
                overrides.append({'predicate': {'ancientwarfare:level': level}, 'model': 'ancientwarfare:item/vehicle/spawner/%s_%d' % (config, level)})
        base = tex(variants['%s_0' % config]['textures']['layer0'])
        write(os.path.join(models, 'spawner_%s.json' % config), {'parent': 'item/generated', 'textures': {'layer0': base}, 'overrides': overrides})
        count += 1
    print('wrote %d item models' % count)


if __name__ == '__main__':
    main()
