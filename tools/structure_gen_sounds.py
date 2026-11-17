#!/usr/bin/env python3
"""
Structure module sounds (run once, results are committed):
  - init/AWStructureSounds.java from the 1.12 class (one DeferredHolder per registered sound, 1.12 field names)
  - merges assets/ancientwarfarestructure/sounds.json into assets/ancientwarfare/sounds.json
    (ancientwarfarestructure:<path> -> ancientwarfare:structure/<path>)
  - 1.12 SoundLoader registered every sounds/auto_load/**.ogg at runtime (client reflection): those get plain
    sounds.json entries "auto_load/<path>" instead
usage: structure_gen_sounds.py <port repo> <1.12 repo>
"""
import json
import os
import re
import sys

repo, ref = sys.argv[1], sys.argv[2]
src = open(os.path.join(ref, 'src/main/java/net/shadowmage/ancientwarfare/structure/init/AWStructureSounds.java'), encoding='utf-8').read()
names = re.findall(r'createSoundEvent\("([^"]+)"\)', src)
# Dropped for licensing (review/LICENSING.md 2b, TV clips): never registered, no sounds.json entry, no file shipped.
REMOVED = {'del_cashinhand', 'del_cowboy', 'del_dontbeaplonker', 'del_dozytwonk', 'del_gonnabecushty', 'del_keepyourmitsoff',
           'del_millionares', 'del_moneyforoldrope', 'del_ohshutupyoutart', 'del_packitin', 'del_plonker', 'del_youdipstick',
           'del_youpranny', 'myth_boldpredictions', 'myth_pithhelmet', 'mythbusters_eyebrow', 'mythbusters_quack',
           'mythbusters_theresyourproblem'}
names = [n for n in names if n not in REMOVED]

out = ['package net.shadowmage.ancientwarfare.structure.init;', '',
       'import net.minecraft.core.registries.Registries;', 'import net.minecraft.resources.ResourceLocation;',
       'import net.minecraft.sounds.SoundEvent;', 'import net.neoforged.bus.api.IEventBus;',
       'import net.neoforged.neoforge.registries.DeferredHolder;', 'import net.neoforged.neoforge.registries.DeferredRegister;',
       'import net.shadowmage.ancientwarfare.structure.AncientWarfareStructure;', '',
       'import java.util.LinkedHashMap;', 'import java.util.Map;', '', '/*',
       ' * Port notes: generated from the 1.12 class by tools/structure_gen_sounds.py. @ObjectHolder fields -> DeferredHolder',
       ' * constants (call .get()), one per sound the 1.12 class registered; ids keep their 1.12 path in the "ancientwarfare"',
       ' * namespace. SOUNDS_BY_NAME (registration order) replaces iterating the 1.12 sound registry for this mod id.',
       ' * The 18 TV-clip sounds of the 1.12 class (del_*, myth_*, mythbusters_*) are not registered: dropped for licensing.',
       ' */', '@SuppressWarnings("unused")', 'public class AWStructureSounds {', '\tprivate AWStructureSounds() {}', '',
       '\tprivate static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, AncientWarfareStructure.MOD_ID);',
       '', '\tpublic static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> SOUNDS_BY_NAME = new LinkedHashMap<>();', '']
for n in names:
    out.append('\tpublic static final DeferredHolder<SoundEvent, SoundEvent> %s = createSoundEvent("%s");' % (re.sub(r'[^A-Za-z0-9]', '_', n).upper(), n))
out += ['', '\tprivate static DeferredHolder<SoundEvent, SoundEvent> createSoundEvent(String soundName) {',
        '\t\tResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(AncientWarfareStructure.MOD_ID, soundName);',
        '\t\tDeferredHolder<SoundEvent, SoundEvent> holder = SOUNDS.register(soundName, () -> SoundEvent.createVariableRangeEvent(registryName));',
        '\t\tSOUNDS_BY_NAME.put(soundName, holder);', '\t\treturn holder;', '\t}', '',
        '\tpublic static void register(IEventBus modBus) {', '\t\tSOUNDS.register(modBus);', '\t}', '}', '']
open(os.path.join(repo, 'src/main/java/net/shadowmage/ancientwarfare/structure/init/AWStructureSounds.java'), 'w', encoding='utf-8').write('\n'.join(out))

assets = os.path.join(repo, 'src/main/resources/assets/ancientwarfare')
target = os.path.join(assets, 'sounds.json')
sounds = json.load(open(target, encoding='utf-8'))
old = json.load(open(os.path.join(ref, 'src/main/resources/assets/ancientwarfarestructure/sounds.json'), encoding='utf-8'))
# Sound files dropped for licensing before the release candidate (tools/licensing_dropped_sounds.txt): their events stay,
# without those files (an event left with no file is an empty list, played silently by core/client/SilentSoundFilter).
dropped = set()
for line in open(os.path.join(repo, 'tools/licensing_dropped_sounds.txt'), encoding='utf-8'):
    if line.strip() and not line.startswith('#'):
        dropped.add(line.split('\t', 1)[0][:-len('.ogg')])
missing = 0
for key, entry in old.items():
    if key in REMOVED:
        continue
    new_sounds = []
    for s in entry['sounds']:
        s = dict(s) if isinstance(s, dict) else {'name': s}
        path = s['name'].split(':', 1)[1]
        if 'structure/' + path in dropped:
            continue
        if not os.path.exists(os.path.join(assets, 'sounds/structure', path + '.ogg')):
            missing += 1
        s['name'] = 'ancientwarfare:structure/' + path
        new_sounds.append(s)
    e = dict(entry)
    e['sounds'] = new_sounds
    sounds[key] = e
auto = 0
base = os.path.join(assets, 'sounds/structure/auto_load')
for root, _, files in os.walk(base):
    for f in sorted(files):
        if f.endswith('.ogg'):
            rel = os.path.relpath(os.path.join(root, f), base).replace(os.sep, '/')[:-4]
            sounds['auto_load/' + rel] = {'sounds': [{'name': 'ancientwarfare:structure/auto_load/' + rel, 'stream': False}]}
            auto += 1
json.dump(sounds, open(target, 'w', encoding='utf-8'), indent=2)
print('sound events', len(names), 'sounds.json entries', len(sounds), 'auto_load', auto, 'missing files', missing)
