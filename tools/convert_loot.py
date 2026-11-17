#!/usr/bin/env python3
"""
Converts AW2 1.12 loot tables into 1.21.1 datapack loot tables.

usage: convert_loot.py <port repo> <path> [<path> ...] [--drop-unknown]

  <path>   file or folder below assets/ancientwarfare/loot_tables of the 1.12 source, e.g.
           chests/research_note.json, chests/inject, entities.  chests/compat is always skipped.
           -> <repo>/src/main/resources/data/ancientwarfare/loot_table/<same path, lower case>
  --drop-unknown   entries whose item is not registered yet (not a vanilla item and no models/item/<id>.json in
           the port) are left out and listed, instead of being written (a loot table with an unknown item does
           not load at all). Run again without the flag once the owning module is ported.

For every table below chests/inject a NeoForge global loot modifier is written as well
(data/ancientwarfare/loot_modifiers/inject_<name>.json, type neoforge:add_table on minecraft:chests/<name>) and
data/neoforge/loot_modifiers/global_loot_modifiers.json is updated; this replaces the 1.12 LootTableLoadEvent
pool injection.

Supported: item / loot_table / empty entries, weight, quality, set_count, set_data (flattening), set_damage,
set_nbt (AW items -> set_custom_data; vanilla items: display name / lore / colour, Potion, ench,
StoredEnchantments, Unbreakable, AttributeModifiers -> set_name / set_lore / set_components), set_data ranges on
wool / carpet (-> tag entry), enchant_randomly, enchant_with_levels, looting_enchant, random_chance.
Anything else is reported (exit code 1) and has to be converted by hand.
"""
import json
import os
import sys

import aw_legacy as L

repo = None
drop_unknown = False
dropped = []
SRC = os.path.join(L.REF_ASSETS, 'loot_tables')
# vanilla chest loot tables that were renamed since 1.12 (targets of the injected tables)
VANILLA_CHEST_RENAMES = {'village_blacksmith': 'village/village_weaponsmith'}


def number(v):
    if isinstance(v, dict):
        return {'type': 'minecraft:uniform', 'min': v.get('min', 0), 'max': v.get('max', 0)}
    return v


def ns(name):
    return name if ':' in name else 'minecraft:' + name


# ---- 1.12 item NBT (SNBT text in set_nbt) -> 1.21.1 loot functions, for vanilla items

ENCHANTMENTS = {0: 'protection', 1: 'fire_protection', 2: 'feather_falling', 3: 'blast_protection', 4: 'projectile_protection',
                5: 'respiration', 6: 'aqua_affinity', 7: 'thorns', 8: 'depth_strider', 9: 'frost_walker', 10: 'binding_curse',
                16: 'sharpness', 17: 'smite', 18: 'bane_of_arthropods', 19: 'knockback', 20: 'fire_aspect', 21: 'looting',
                22: 'sweeping_edge', 32: 'efficiency', 33: 'silk_touch', 34: 'unbreaking', 35: 'fortune', 48: 'power', 49: 'punch',
                50: 'flame', 51: 'infinity', 61: 'luck_of_the_sea', 62: 'lure', 70: 'mending', 71: 'vanishing_curse'}
ATTRIBUTES = {'generic.movementSpeed': 'generic.movement_speed', 'generic.attackDamage': 'generic.attack_damage',
              'generic.maxHealth': 'generic.max_health', 'generic.attackSpeed': 'generic.attack_speed', 'generic.armor': 'generic.armor',
              'generic.armorToughness': 'generic.armor_toughness', 'generic.knockbackResistance': 'generic.knockback_resistance',
              'generic.luck': 'generic.luck', 'generic.followRange': 'generic.follow_range'}
_modifier_count = [0]


class Snbt:
    def __init__(self, text):
        self.t = text
        self.i = 0

    def ws(self):
        while self.i < len(self.t) and self.t[self.i].isspace():
            self.i += 1

    def value(self):
        self.ws()
        c = self.t[self.i]
        if c == '{':
            self.i += 1
            out = {}
            while True:
                self.ws()
                if self.t[self.i] == '}':
                    self.i += 1
                    return out
                key = self.token(':')
                self.ws()
                assert self.t[self.i] == ':', self.t[self.i:self.i + 20]
                self.i += 1
                out[key] = self.value()
                self.ws()
                if self.t[self.i] == ',':
                    self.i += 1
        if c == '[':
            self.i += 1
            out = []
            while True:
                self.ws()
                if self.t[self.i] == ']':
                    self.i += 1
                    return out
                out.append(self.value())
                self.ws()
                if self.t[self.i] == ',':
                    self.i += 1
        if c == '"':
            return self.string()
        tok = self.token(',}]')
        low = tok.lower()
        if low in ('true', 'false'):
            return low == 'true'
        try:
            return int(low[:-1]) if low[-1:] in 'bsl' else float(low[:-1]) if low[-1:] in 'fd' and low[:-1].replace('.', '').replace('-', '').isdigit() else int(low) if low.lstrip('-').isdigit() else float(low)
        except ValueError:
            return tok

    def string(self):
        self.i += 1
        out = ''
        while self.t[self.i] != '"':
            if self.t[self.i] == '\\':
                self.i += 1
            out += self.t[self.i]
            self.i += 1
        self.i += 1
        return out

    def token(self, stop):
        self.ws()
        if self.t[self.i] == '"':
            return self.string()
        j = self.i
        while self.i < len(self.t) and self.t[self.i] not in stop:
            self.i += 1
        return self.t[j:self.i].strip()


def enchant_levels(lst, where):
    levels = {}
    for x in lst:
        eid = x.get('id')
        name = ENCHANTMENTS.get(eid) if isinstance(eid, (int, float)) else str(eid).replace('minecraft:', '')
        if name is None:
            L.problem('unknown 1.12 enchantment id %s (%s)' % (eid, where))
            continue
        levels['minecraft:' + name] = int(x.get('lvl', 1))
    return levels


def vanilla_nbt_functions(tag_text, item, where):
    """1.12 stack NBT of a vanilla item -> list of 1.21.1 loot functions"""
    try:
        tag = Snbt(tag_text).value()
    except (AssertionError, IndexError):
        L.problem('cannot parse set_nbt tag (%s, %s): %s' % (item, where, tag_text[:60]))
        return []
    out = []
    comps = {}
    display = tag.pop('display', {})
    if 'Name' in display:
        out.append({'function': 'minecraft:set_name', 'name': {'text': display.pop('Name'), 'italic': True}, 'target': 'custom_name'})
    if 'Lore' in display:
        out.append({'function': 'minecraft:set_lore', 'lore': [{'text': x, 'italic': True, 'color': 'dark_purple'} for x in display.pop('Lore')], 'mode': 'replace_all'})
    if 'color' in display:
        comps['minecraft:dyed_color'] = {'rgb': int(display.pop('color'))}
    if 'Potion' in tag:
        comps['minecraft:potion_contents'] = {'potion': ns(tag.pop('Potion'))}
    if 'StoredEnchantments' in tag:
        comps['minecraft:stored_enchantments'] = {'levels': enchant_levels(tag.pop('StoredEnchantments'), where)}
    if 'ench' in tag:
        comps['minecraft:enchantments'] = {'levels': enchant_levels(tag.pop('ench'), where)}
    if tag.pop('Unbreakable', False):
        comps['minecraft:unbreakable'] = {}
    if 'AttributeModifiers' in tag:
        mods = []
        for m in tag.pop('AttributeModifiers'):
            attr = ATTRIBUTES.get(m.get('AttributeName'))
            if attr is None:
                L.problem('unknown 1.12 attribute %s (%s, %s)' % (m.get('AttributeName'), item, where))
                continue
            _modifier_count[0] += 1
            mods.append({'type': 'minecraft:' + attr, 'id': 'ancientwarfare:loot_modifier_%d' % _modifier_count[0], 'amount': m.get('Amount', 0),
                         'operation': ['add_value', 'add_multiplied_base', 'add_multiplied_total'][int(m.get('Operation', 0))], 'slot': m.get('Slot', 'any')})
        comps['minecraft:attribute_modifiers'] = {'modifiers': mods, 'show_in_tooltip': True}
    tag.pop('HideFlags', None)
    tag.pop('RepairCost', None)
    if display:
        L.problem('display keys not converted %s (%s, %s)' % (sorted(display), item, where))
    if tag:
        L.problem('set_nbt keys of a vanilla item not converted %s (%s, %s)' % (sorted(tag), item, where))
    if comps:
        out.append({'function': 'minecraft:set_components', 'components': comps})
    return out


def conditions(conds, where):
    out = []
    for c in conds:
        t = ns(c['condition'])
        if t == 'minecraft:random_chance':
            out.append({'condition': t, 'chance': c['chance']})
        else:
            L.problem('unsupported loot condition %s (%s)' % (t, where))
    return out


def entry(e, where):
    t = ns(e['type'])
    out = {'type': t}
    if 'weight' in e:
        out['weight'] = e['weight']
    if e.get('quality'):
        out['quality'] = e['quality']
    if 'conditions' in e:
        out['conditions'] = conditions(e['conditions'], where)
    if t == 'minecraft:empty':
        return out
    if t == 'minecraft:loot_table':
        out['value'] = L.map_namespace(e['name']).lower()
        return out
    if t != 'minecraft:item':
        L.problem('unsupported loot entry type %s (%s)' % (t, where))
        return None
    meta = None
    random_colour = None
    functions = []
    for f in e.get('functions', []):
        fn = ns(f['function'])
        nf = {'function': fn}
        if 'conditions' in f:
            nf['conditions'] = conditions(f['conditions'], where)
        if fn == 'minecraft:set_data':
            if isinstance(f['data'], dict) and L.map_namespace(e['name']) in ('minecraft:wool', 'minecraft:carpet'):
                random_colour = 'minecraft:wool' if e['name'].endswith('wool') else 'minecraft:wool_carpets'
            elif isinstance(f['data'], dict):
                L.problem('set_data with a range (random sub item) has to be converted by hand (%s, %s)' % (e['name'], where))
            else:
                meta = int(f['data'])
            continue
        if fn == 'minecraft:set_count':
            nf['count'] = number(f['count'])
        elif fn == 'minecraft:set_damage':
            nf['damage'] = number(f['damage'])
        elif fn == 'minecraft:set_nbt':
            if not L.map_namespace(e['name']).startswith('ancientwarfare:'):
                for vf in vanilla_nbt_functions(f['tag'], e['name'], where):
                    if 'conditions' in nf:
                        vf['conditions'] = nf['conditions']
                    functions.append(vf)
                continue
            nf['function'] = 'minecraft:set_custom_data'
            nf['tag'] = f['tag']
        elif fn == 'minecraft:set_attributes':
            mods = []
            for m in f.get('modifiers', []):
                attr = ATTRIBUTES.get(m.get('attribute'), m.get('attribute'))
                _modifier_count[0] += 1
                slot = m.get('slot', 'any')
                mods.append({'id': 'ancientwarfare:loot_modifier_%d' % _modifier_count[0], 'attribute': 'minecraft:' + attr, 'amount': number(m.get('amount', 0)),
                             'operation': {'addition': 'add_value', 'multiply_base': 'add_multiplied_base', 'multiply_total': 'add_multiplied_total'}[m.get('operation', 'addition')],
                             'slot': slot})
            nf['modifiers'] = mods
            nf['replace'] = False
        elif fn == 'minecraft:enchant_randomly':
            if 'enchantments' in f:
                nf['options'] = [ns(x) for x in f['enchantments']]
        elif fn == 'minecraft:enchant_with_levels':
            nf['levels'] = number(f['levels'])
            if not f.get('treasure', False):
                nf['options'] = '#minecraft:on_random_loot'
        elif fn == 'minecraft:looting_enchant':
            nf = {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting', 'count': number(f['count'])}
            if 'limit' in f:
                nf['limit'] = f['limit']
        else:
            L.problem('unsupported loot function %s (%s)' % (fn, where))
            continue
        functions.append(nf)
    # the count goes last: 1.21 refuses to put components on a stack above its maximum size (1.12 tables give 2 - 3
    # potions in one entry); the loot table splits an overstacked result into single stacks afterwards
    functions.sort(key=lambda x: x['function'] == 'minecraft:set_count')
    if random_colour:
        # 1.12 picked a random colour through a meta range: any item of the tag, the weight spread over the 16 colours
        out['type'] = 'minecraft:tag'
        out['name'] = random_colour
        out['expand'] = True
        out['weight'] = max(1, round(out.get('weight', 1) / 16))
        if functions:
            out['functions'] = functions
        return out
    name = L.flatten_item(e['name'], meta)
    if name not in L.known_items(repo):
        if drop_unknown:
            dropped.append('%s: %s' % (where, name))
            return None
        L.problem('unknown item %s (%s)' % (name, where))
    out['name'] = name
    if functions:
        out['functions'] = functions
    return out


def convert_table(data, rel):
    kind = 'minecraft:entity' if rel.startswith('entities/') else 'minecraft:chest'
    pools = []
    for p in data.get('pools', []):
        np = {'rolls': number(p.get('rolls', 1))}
        if 'bonus_rolls' in p:
            np['bonus_rolls'] = number(p['bonus_rolls'])
        if 'conditions' in p:
            np['conditions'] = conditions(p['conditions'], rel)
        np['entries'] = [x for x in (entry(e, rel) for e in p.get('entries', [])) if x is not None]
        pools.append(np)
    return {'type': kind, 'pools': pools}


def convert_file(path):
    rel = os.path.relpath(path, SRC).replace('\\', '/').lower()
    if rel.startswith('chests/compat/'):
        return 0
    data = json.load(open(path, encoding='utf-8-sig'))
    L.write_json(os.path.join(repo, 'src/main/resources/data/ancientwarfare/loot_table', rel), convert_table(data, rel))
    if rel.startswith('chests/inject/'):
        name = rel[len('chests/inject/'):-len('.json')]
        L.write_json(os.path.join(repo, 'src/main/resources/data/ancientwarfare/loot_modifiers', 'inject_' + name.replace('/', '_') + '.json'), {
            'type': 'neoforge:add_table',
            'conditions': [{'condition': 'neoforge:loot_table_id', 'loot_table_id': 'minecraft:chests/' + VANILLA_CHEST_RENAMES.get(name, name)}],
            'table': 'ancientwarfare:chests/inject/' + name,
        })
        glm = os.path.join(repo, 'src/main/resources/data/neoforge/loot_modifiers/global_loot_modifiers.json')
        cur = json.load(open(glm, encoding='utf-8')) if os.path.exists(glm) else {'replace': False, 'entries': []}
        ident = 'ancientwarfare:inject_' + name.replace('/', '_')
        if ident not in cur['entries']:
            cur['entries'].append(ident)
            cur['entries'].sort()
        L.write_json(glm, cur)
    return 1


def main():
    global repo, drop_unknown
    args = sys.argv[1:]
    if len(args) < 2:
        print(__doc__)
        sys.exit(2)
    repo = args[0]
    n = 0
    drop_unknown = '--drop-unknown' in args
    for a in args[1:]:
        if a.startswith('--'):
            continue
        p = os.path.join(SRC, a)
        if os.path.isfile(p):
            n += convert_file(p)
        else:
            for root, _, names in os.walk(p):
                for name in sorted(names):
                    if name.endswith('.json'):
                        n += convert_file(os.path.join(root, name))
    print('loot tables written:', n)
    if dropped:
        print('\nDROPPED entries with items that are not registered yet (%d):' % len(dropped))
        for d in dropped:
            print(' - ' + d)
    if L.problems:
        print('\nPROBLEMS (%d):' % len(L.problems))
        for p in L.problems:
            print(' - ' + p)
        sys.exit(1)


if __name__ == '__main__':
    main()
