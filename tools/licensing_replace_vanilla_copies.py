#!/usr/bin/env python3
"""Replace the pixels of mod PNGs that were copied (exact or recoloured) from vanilla Minecraft textures with
procedurally generated, original pixels. Writes the PNGs in place (keeps size and mode RGBA) and a before/after preview.
Masks: see copymask.py (8x8 rank-signature match, then the whole vanilla texture aligned and colour-fitted).
Generators (all with a fixed seed per file, numpy default_rng):
  bricks  - for stone-brick sources: 8-px courses, mortar row at the bottom of each course, staggered joints every
            16 px, face colour = median of the replaced pixels, mortar = their 15th percentile, new random face noise
  planks  - for planks: 4-px boards, dark seam row, one random butt joint per board, horizontal grain from new noise
  rings   - for a log end: concentric square rings from the centre of the replaced area, two shades, dark rim
  smooth  - for flat / noise textures (stone slab, gold / iron block, sandstone, GUI button and book fills):
            pixels that are lines or edges (more than max(3 robust deviations, 12 levels) from a local low-pass) stay; every other
            copied pixel is replaced by a low-pass of the area (box blur, radius 2, twice; keeps only the shading)
            plus a new random noise field with the robust (median absolute deviation) spread of the removed noise
  head    - npc_default: head and arms redrawn: hair and skin colours = medians of the replaced dark / light pixels,
            a simple hair cap and side locks, new eyes and mouth, new noise
Licensing tool (release candidate). Reads the vanilla client jars from the Gradle caches (1.12.2 from ForgeGradle,
1.21.1 from NeoForm runtime); nothing of them is written into the repository.
usage: licensing_replace_vanilla_copies.py --check <textures dir>        report copied pixels per PNG (skin_pack excluded)
       licensing_replace_vanilla_copies.py <preview-dir> <png>...         replace them in place, write before/after previews
Run on 2026-11-16 for: block/structure/{builder_side,totem_part_irminsul_top,drafting_station,golden_throne},
model/automation/stirling_generator, model/structure/{gate/gate_wood_1,gate/gate_iron_1,coffin_birch,
stone_coffin_sandstone}, entity/npc/npc_default, gui/{guibuttons1,guibuttons2,manual} (all .png).
Limits: finds copies that keep an 8x8 block of a vanilla texture on its grid with its luminance order (exact copies and
colour-mapped recolours); redrawn, rotated, scaled or heavily edited derivatives are not found.
Second method (added 2026-11-17 after the snapshot review): exact aligned 16x16 tiles. Every 16x16 tile of a mod PNG at
a multiple of 16 is compared byte for byte (RGBA) with every 16-aligned 16x16 tile of every vanilla 1.12.2 and 1.21.1
texture; tiles of fewer than 4 colours are ignored (a flat fill with at most two line colours, e.g. the plain
grey GUI panel edge with its black outline and white or dark bevel, is not counted as a copy; on 2026-11-17 such
3-colour tiles matched in gui/guibackgroundlarge (54), gui/guibuttons1 (1) and block/npc/town_hall (192)).
Accepted by the owner on 2026-11-17 as they are (the plain panel style and a two-shade line, not copied artwork);
not to be reopened. It finds low-detail copies (planks) that the 8x8 signature skips
(that needs 8 distinct luminance levels per 8x8 block). Run on 2026-11-17 for model/automation/tile_auto_crafting and
model/structure/gate/gate_iron_1 (two and one tiles of 1.12 blocks/planks_oak)."""
import io, os, sys, zipfile, hashlib, json
import numpy as np
from PIL import Image
from numpy.lib.stride_tricks import sliding_window_view as swv

H = os.path.expanduser('~/.gradle/caches')
JARS = {'1.12.2': H + '/minecraft/net/minecraft/minecraft/1.12.2/minecraft-1.12.2.jar',
        '1.21.1': H + '/neoformruntime/artifacts/minecraft_1.21.1_client.jar'}
T = 8
LW = np.array([0.299, 0.587, 0.114])

def sigs(win):
    a = win[..., 3].reshape(len(win), -1)
    ok = (a == 255).all(1)
    lum = (win[..., :3].astype(np.float32) @ LW).reshape(len(win), -1)
    rl = np.round(lum)
    distinct = 1 + (np.diff(np.sort(rl, 1), axis=1) > 0).sum(1)
    ok &= distinct >= 8
    order = np.argsort(rl, axis=1, kind='stable').astype(np.uint8)
    return [hashlib.md5(order[i].tobytes()).digest()[:10] if ok[i] else None for i in range(len(win))]

def load_index():
    idx = {}; imgs = {}
    for ver, j in JARS.items():
        z = zipfile.ZipFile(j)
        for e in z.namelist():
            if not (e.endswith('.png') and e.startswith('assets/minecraft/textures/')): continue
            try: a = np.asarray(Image.open(io.BytesIO(z.read(e))).convert('RGBA'))
            except Exception: continue
            if a.shape[0] < T or a.shape[1] < T: continue
            key = ver + ':' + e[len('assets/minecraft/textures/'):]
            g = swv(a, (T, T, 4))[::T, ::T, 0]; gw = g.shape[1]
            for k, s in enumerate(sigs(g.reshape(-1, T, T, 4))):
                if s and s not in idx:
                    ty, tx = divmod(k, gw); idx[s] = (key, ty * T, tx * T); imgs[key] = a
    return idx, imgs

TILE = 16

def load_tiles():
    tiles = {}
    for ver, j in JARS.items():
        z = zipfile.ZipFile(j)
        for e in z.namelist():
            if not (e.endswith('.png') and e.startswith('assets/minecraft/textures/')): continue
            try: a = np.asarray(Image.open(io.BytesIO(z.read(e))).convert('RGBA'))
            except Exception: continue
            key = ver + ':' + e[len('assets/minecraft/textures/'):]
            for y in range(0, a.shape[0] - TILE + 1, TILE):
                for x in range(0, a.shape[1] - TILE + 1, TILE):
                    t = np.ascontiguousarray(a[y:y+TILE, x:x+TILE])
                    if len(np.unique(t.reshape(-1, 4), axis=0)) < 4: continue
                    tiles.setdefault(t.tobytes(), key)
    return tiles

def tile_masks(a, tiles):
    a8 = np.clip(np.round(a), 0, 255).astype(np.uint8); h, w = a8.shape[:2]
    out = {}
    for y in range(0, h - TILE + 1, TILE):
        for x in range(0, w - TILE + 1, TILE):
            key = tiles.get(np.ascontiguousarray(a8[y:y+TILE, x:x+TILE]).tobytes())
            if key:
                m = out.setdefault(key, np.zeros((h, w), bool)); m[y:y+TILE, x:x+TILE] = True
    return out

def masks_for(a, idx, imgs, tiles=None):
    out = masks_for_8(a, idx, imgs)
    for key, m in (tile_masks(a, tiles) if tiles else {}).items():
        out[key] = out[key] | m if key in out else m
    return out

def masks_for_8(a, idx, imgs):
    h, w = a.shape[:2]
    win = swv(a.astype(np.uint8), (T, T, 4))[:, :, 0]; ww = win.shape[1]
    out = {}
    for k, s in enumerate(sigs(win.reshape(-1, T, T, 4))):
        if not s or s not in idx: continue
        y, x = divmod(k, ww); key, ty, tx = idx[s]; v = imgs[key].astype(np.float64)
        mw = a[y:y+T, x:x+T, :3].reshape(-1, 3); vw = v[ty:ty+T, tx:tx+T, :3].reshape(-1, 3)
        coef = [np.linalg.lstsq(np.stack([vw[:, c], np.ones(len(vw))], 1), mw[:, c], rcond=None)[0] for c in range(3)]
        oy, ox = y - ty, x - tx
        y0, y1 = max(0, oy), min(h, oy + v.shape[0]); x0, x1 = max(0, ox), min(w, ox + v.shape[1])
        vs = v[y0-oy:y1-oy, x0-ox:x1-ox]; ms = a[y0:y1, x0:x1]
        mapped = np.stack([vs[..., c] * coef[c][0] + coef[c][1] for c in range(3)], -1)
        close = (np.abs(ms[..., :3] - mapped) <= 6).all(-1) & (ms[..., 3] > 0) & (vs[..., 3] > 0)
        m = out.setdefault(key, np.zeros((h, w), bool)); m[y0:y1, x0:x1] |= close
    return out

def kind_of(key):
    n = key.split(':', 1)[1]
    if 'stonebrick' in n: return 'bricks'
    if 'planks' in n: return 'planks'
    if 'log' in n: return 'rings'
    if 'steve' in n: return 'head'
    return 'smooth'

def components(mask):
    lab = np.zeros(mask.shape, int); n = 0
    for y, x in zip(*np.nonzero(mask)):
        if lab[y, x]: continue
        n += 1; st = [(y, x)]; lab[y, x] = n
        while st:
            cy, cx = st.pop()
            for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                ny, nx = cy + dy, cx + dx
                if 0 <= ny < mask.shape[0] and 0 <= nx < mask.shape[1] and mask[ny, nx] and not lab[ny, nx]:
                    lab[ny, nx] = n; st.append((ny, nx))
    return [lab == i for i in range(1, n + 1)]

def blur(a, m, r=2):
    acc = np.where(m[..., None], a, 0.0); wt = m.astype(np.float64)
    for _ in range(2):
        pa = np.pad(acc, ((r, r), (r, r), (0, 0))); pw = np.pad(wt, r)
        s = np.zeros_like(acc); sw = np.zeros_like(wt)
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                s += pa[r+dy:r+dy+acc.shape[0], r+dx:r+dx+acc.shape[1]]
                sw += pw[r+dy:r+dy+wt.shape[0], r+dx:r+dx+wt.shape[1]]
        acc = s / np.maximum(sw[..., None], 1e-9) * m[..., None]; wt = m.astype(np.float64)
    return acc

def gen(kind, a, m, rng):
    out = a.copy()
    px = a[m][:, :3]
    if kind == 'smooth':
        # pass 1: classify lines / edges (bevels, outlines, slab borders) as outliers from a local low-pass
        base = blur(a[..., :3], m)
        resid = (a[..., :3] - base)[m]
        if len(resid) == 0:
            return out
        sd = 1.4826 * np.median(np.abs(resid - np.median(resid, 0)), 0) + 0.5
        dev = np.abs(a[..., :3] - base).max(-1)
        noise_px = m & (dev <= max(3 * sd.max(), 12))
        # pass 2: low-pass of the noise pixels only; lines and edges are kept as they are (generic shapes)
        base2 = blur(a[..., :3], noise_px)
        resid2 = (a[..., :3] - base2)[noise_px]
        sd2 = np.maximum(1.4826 * np.median(np.abs(resid2 - np.median(resid2, 0)), 0), 1.5)
        noise = rng.normal(0, 1, a.shape[:2] + (1,)) * sd2  # one new noise field for all channels, like a grain
        out[..., :3] = np.where(noise_px[..., None], base2 + noise, a[..., :3])
        return out
    ys, xs = np.nonzero(m); y0, x0 = ys.min(), xs.min()
    lum = px @ LW
    face = np.median(px[lum >= np.percentile(lum, 35)], 0)
    dark = np.percentile(px, 12, axis=0)
    sd = px[lum >= np.percentile(lum, 35)].std(0).mean() * 0.8 + 1
    for y, x in zip(ys, xs):
        ly, lx = y - y0, x - x0
        if kind == 'bricks':
            course = ly // 8
            joint = (lx + (8 if course % 2 else 0)) % 16 == 15
            if ly % 8 == 7 or joint: c = dark + rng.normal(0, sd * 0.4)
            elif ly % 8 == 0: c = face * 1.08 + rng.normal(0, sd * 0.6)
            else: c = face + rng.normal(0, sd)
        elif kind == 'planks':
            board = ly // 4
            jx = (board * 7 + 5) % 16
            if ly % 4 == 3 or lx % 16 == jx: c = dark + rng.normal(0, sd * 0.4)
            else: c = face * (1 + 0.06 * np.sin(lx * 0.9 + board * 2.1)) + rng.normal(0, sd * 0.7)
        elif kind == 'rings':
            cy, cx = (ys.min() + ys.max()) / 2, (xs.min() + xs.max()) / 2
            d = int(max(abs(y - cy), abs(x - cx)))
            edge = d >= int(max(ys.max() - cy, xs.max() - cx))
            if edge: c = dark + rng.normal(0, sd * 0.4)
            elif (d // 2) % 2: c = face * 0.88 + rng.normal(0, sd * 0.5)
            else: c = face * 1.05 + rng.normal(0, sd * 0.5)
        else:
            c = face
        out[y, x, :3] = c
    return out

def head(a, m, rng):
    """npc_default (64x64 skin layout): redraw the masked head and arm pixels."""
    out = a.copy()
    px = a[m][:, :3]; lum = px @ LW
    hair = np.median(px[lum < np.percentile(lum, 40)], 0)
    skin = np.median(px[lum >= np.percentile(lum, 60)], 0)
    hn = px[lum < np.percentile(lum, 40)].std(0).mean() * 0.6 + 1
    sn = px[lum >= np.percentile(lum, 60)].std(0).mean() * 0.6 + 1
    def put(y, x, c, n):
        if m[y, x]: out[y, x, :3] = c + rng.normal(0, n)
    for y, x in zip(*np.nonzero(m)):
        if y < 16 and x < 32:          # head: top, bottom, 4 sides
            ly, lx = y % 8, x % 8
            if y < 8:                    # top (x 8-15) hair, bottom (x 16-23) skin
                put(y, x, hair if x < 16 else skin, hn if x < 16 else sn)
            elif 24 <= x < 32:           # back: hair
                put(y, x, hair, hn)
            else:                        # right / front / left
                is_hair = ly < 2 or (ly < 3 and (lx in (0, 7) or not (8 <= x < 16)))
                put(y, x, hair if is_hair else skin, hn if is_hair else sn)
        else:                            # arms / neck: skin
            put(y, x, skin, sn)
    # face features on the front (x 8-15, y 8-15): eyes on row 12, mouth on row 14
    for x, c in ((9, (235, 235, 235)), (10, (60, 60, 140)), (13, (60, 60, 140)), (14, (235, 235, 235))):
        if m[12, x]: out[12, x, :3] = c
    for x in (11, 12):
        if m[14, x]: out[14, x, :3] = skin * 0.7
    return out

def check(root):
    idx, imgs = load_index(); tiles = load_tiles()
    found = 0; tile_found = 0
    for d, _, fs in os.walk(root):
        if 'skin_pack' in d: continue
        for f in sorted(fs):
            if not f.endswith('.png'): continue
            p = os.path.join(d, f)
            try: a = np.asarray(Image.open(p).convert('RGBA')).astype(np.float64)
            except Exception: continue
            if a.shape[0] < T or a.shape[1] < T: continue
            ms = masks_for(a, idx, imgs, tiles)
            n = int(np.any(list(ms.values()), axis=0).sum()) if ms else 0
            tm = tile_masks(a, tiles)
            if tm:
                tile_found += 1
            if n:
                found += 1
                print(n, os.path.relpath(p, root), sorted(ms), '(aligned 16x16 tiles: %d)' % sum(int(m.sum()) // (TILE * TILE) for m in tm.values()))
    print('files with copied pixels:', found, '(of them with exact aligned 16x16 tiles: %d)' % tile_found)
    return found

def main():
    if sys.argv[1] == '--check':
        sys.exit(1 if check(sys.argv[2]) else 0)
    pdir = sys.argv[1]; os.makedirs(pdir, exist_ok=True)
    idx, imgs = load_index(); tiles = load_tiles()
    report = {}
    for p in sys.argv[2:]:
        im = Image.open(p); a = np.asarray(im.convert('RGBA')).astype(np.float64)
        ms = masks_for(a, idx, imgs, tiles)
        seed = int(hashlib.sha1(p.encode()).hexdigest()[:8], 16)
        rng = np.random.default_rng(seed)
        out = a.copy(); done = {}
        for key, mask in sorted(ms.items()):
            mask &= ~np.any([done[k] for k in done], axis=0) if done else mask
            kind = kind_of(key)
            if kind == 'head':
                out = head(out, mask, rng)
            elif kind == 'smooth':
                out = gen('smooth', out, mask, rng)
            else:
                for comp in components(mask):
                    out = gen(kind, out, comp, rng)
            done[key] = mask
            report.setdefault(p, []).append({'source': key, 'pixels': int(mask.sum()), 'generator': kind})
        out[..., :3] = np.clip(np.round(out[..., :3]), 0, 255)
        res = Image.fromarray(out.astype(np.uint8), 'RGBA')
        res.save(p, optimize=True)
        h, w = a.shape[:2]; S = max(1, 512 // max(h, w))
        pv = Image.new('RGBA', (w * 2 * S + 4, h * S), (255, 0, 255, 255))
        pv.paste(Image.fromarray(a.astype(np.uint8)).resize((w * S, h * S), Image.NEAREST), (0, 0))
        pv.paste(res.resize((w * S, h * S), Image.NEAREST), (w * S + 4, 0))
        pv.save(os.path.join(pdir, p.replace('/', '__') + '.after.png'))
    print(json.dumps(report, indent=1))

if __name__ == '__main__':
    main()
