"""Generates every texture, 3D model (Blockbench-compatible JSON), sound and data file for Matrix Glass."""
import json, os, random, math
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import soundfile as sf

R = "src/main/resources"
A = f"{R}/assets/matrixglass"
D = f"{R}/data/matrixglass"
rng = np.random.default_rng(7); random.seed(7)

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f: json.dump(obj, f, indent=2, ensure_ascii=False)
def png(path, im):
    os.makedirs(os.path.dirname(path), exist_ok=True); im.save(path)

# ------------------------------------------------------------------ TEXTURES
GREEN = (0, 255, 65)
def noise_img(size, base, amt, alpha=255):
    a = np.array(base, float)[None, None, :] + rng.normal(0, amt, (size, size, 3))
    a = np.clip(a, 0, 255).astype(np.uint8)
    al = np.full((size, size, 1), alpha, np.uint8)
    return Image.fromarray(np.concatenate([a, al], 2), "RGBA").copy()

# black glass with diagonal specular streaks
g = noise_img(64, (6, 11, 9), 3, 232); px = g.load()
for y in range(64):
    for x in range(64):
        d = abs(((x + y) % 40) - 20)
        r_, g_, b_, a_ = px[x, y]
        if d < 2: px[x, y] = (min(255, r_ + 40), min(255, g_ + 70), min(255, b_ + 55), 255)
        elif d < 4: px[x, y] = (min(255, r_ + 12), min(255, g_ + 24), min(255, b_ + 18), 240)
dr = ImageDraw.Draw(g); dr.rectangle([0, 0, 63, 63], outline=(0, 120, 40, 255))
png(f"{A}/textures/block/monolith_glass.png", g)

t = noise_img(64, (16, 20, 18), 4); dr = ImageDraw.Draw(t)
dr.rectangle([0, 0, 63, 63], outline=(0, 200, 55, 255)); dr.rectangle([3, 3, 60, 60], outline=(10, 70, 25, 255))
for i in range(6, 60, 12): dr.line([6, i, 57, i], fill=(8, 35, 14, 255))
png(f"{A}/textures/block/monolith_trim.png", t)

# animated falling code strip: 16 frames of 32x32
FR, S, C = 16, 32, 4
cells = {(c, r): rng.random((3, 3)) > 0.45 for c in range(8) for r in range(8)}
colspd = [random.choice([1, 2]) for _ in range(8)]; coloff = [random.randrange(8) for _ in range(8)]
strip = Image.new("RGBA", (S, S * FR), (0, 0, 0, 0)); sp = strip.load()
for f in range(FR):
    for c in range(8):
        head = (coloff[c] + colspd[c] * f) % 8
        for r in range(8):
            d = (head - r) % 8
            if d >= 6: continue
            b = 1 - d / 6
            col = (200, 255, 215, 255) if d == 0 else (0, int(60 + 195 * b), int(20 + 45 * b), 255)
            for i in range(3):
                for j in range(3):
                    if cells[(c, r)][i, j]: sp[c * C + j, f * S + r * C + i] = col
png(f"{A}/textures/block/monolith_code.png", strip)
w(f"{A}/textures/block/monolith_code.png.mcmeta", {"animation": {"frametime": 2}})

# payphone
b = noise_img(64, (26, 44, 33), 4); dr = ImageDraw.Draw(b)
dr.rectangle([0, 0, 63, 63], outline=(0, 170, 60, 255)); dr.line([0, 32, 63, 32], fill=(10, 24, 16, 255))
for p_ in [(4, 4), (59, 4), (4, 59), (59, 59)]: dr.ellipse([p_[0]-1, p_[1]-1, p_[0]+1, p_[1]+1], fill=(0, 220, 80, 255))
for _ in range(25):
    x = random.randrange(64); y = random.randrange(64); dr.line([x, y, x+random.randint(2, 9), y+random.randint(-1, 1)], fill=(60, 90, 70, 255))
png(f"{A}/textures/block/payphone_body.png", b)

k = noise_img(64, (14, 20, 16), 3); dr = ImageDraw.Draw(k)
for r_ in range(4):
    for c_ in range(3):
        x0, y0 = 8 + c_ * 18, 6 + r_ * 14
        dr.rectangle([x0, y0, x0 + 12, y0 + 9], fill=(8, 60, 24, 255), outline=(0, 255, 65, 255))
png(f"{A}/textures/block/payphone_keys.png", k)

s = Image.new("RGBA", (64, 64), (4, 22, 10, 255)); dr = ImageDraw.Draw(s)
try: font = ImageFont.load_default(size=26)
except TypeError: font = ImageFont.load_default()
for off, col in [((1, 1), (0, 90, 30, 255)), ((0, 0), (120, 255, 150, 255))]:
    dr.text((32 + off[0], 32 + off[1]), "EXIT", font=font, fill=col, anchor="mm")
dr.rectangle([0, 0, 63, 63], outline=(0, 255, 65, 255))
png(f"{A}/textures/block/payphone_sign.png", s)

# pills (left half dark, right half bright, glossy highlight)
def pill(dark, bright):
    im = Image.new("RGBA", (16, 16)); p = im.load()
    for y in range(16):
        for x in range(16):
            c = dark if x < 8 else bright
            if y in (3, 4): c = tuple(min(255, v + 90) for v in c)
            if y > 12: c = tuple(int(v * .6) for v in c)
            p[x, y] = (*c, 255)
    return im
png(f"{A}/textures/item/red_pill.png", pill((110, 0, 10), (255, 40, 50)))
png(f"{A}/textures/item/blue_pill.png", pill((0, 20, 110), (40, 120, 255)))

# effect icons
def icon(kind):
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rectangle([1, 1, 16, 16], fill=(2, 14, 6, 255), outline=(0, 255, 65, 255))
    if kind == "rain":
        for x in range(3, 16, 3):
            y0 = random.randint(2, 8)
            for i in range(5): d.point((x, y0 + i), fill=(0, 255 - i * 45, 65, 255))
            d.point((x, y0 + 5), fill=(220, 255, 230, 255))
    else:
        d.ellipse([4, 4, 13, 13], outline=(125, 249, 255, 255)); d.line([2, 15, 15, 2], fill=(255, 255, 255, 255)); d.ellipse([8, 8, 9, 9], fill=(125, 249, 255, 255))
    return im
png(f"{A}/textures/mob_effect/matrix_vision.png", icon("rain"))
png(f"{A}/textures/mob_effect/bullet_time.png", icon("bt"))

# particles: glyphs + shards
for i in range(8):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    pts = [(random.choice([3, 8, 12]), random.choice([2, 8, 13])) for _ in range(5)]
    for a_, b_ in zip(pts, pts[1:]):
        if a_ != b_: d.line([a_, b_], fill=(255, 255, 255, 255), width=2)
    png(f"{A}/textures/particle/glyph_{i}.png", im)
for i in range(3):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    poly = [(8 + random.randint(-1, 1), 1), (14 + random.randint(-1, 1), 12), (3, 14 + random.randint(-1, 0))]
    d.polygon(poly, fill=(210, 255, 255, 235), outline=(255, 255, 255, 255)); d.line([poly[0], (8, 9)], fill=(120, 220, 240, 255))
    png(f"{A}/textures/particle/shard_{i}.png", im)
w(f"{A}/particles/code_glyph.json", {"textures": [f"matrixglass:glyph_{i}" for i in range(8)]})
w(f"{A}/particles/glass_shard.json", {"textures": [f"matrixglass:shard_{i}" for i in range(3)]})

# ------------------------------------------------------------------ 3D MODELS
SIDES = ["north", "east", "south", "west", "up", "down"]
def cube(frm, to, tex, uv=(0, 0, 16, 16), rot=None):
    e = {"from": frm, "to": to, "faces": {s_: {"uv": list(uv), "texture": f"#{tex}"} for s_ in SIDES}}
    if rot: e["rotation"] = rot
    return e
def rot(frm, to, angle, axis):
    return {"angle": angle, "axis": axis, "origin": [(a + b) / 2 for a, b in zip(frm, to)]}

mono = [
    cube([3, 0, 3], [13, 1, 13], "trim"),
    cube([4, 1, 6], [12, 15, 10], "glass"),
    cube([3, 15, 5], [13, 16, 11], "trim"),
    cube([4.5, 2, 5.9], [11.5, 14, 6.0], "code"),
    cube([4.5, 2, 10.0], [11.5, 14, 10.1], "code"),
    cube([3.9, 1, 6.5], [4.0, 15, 9.5], "trim"), cube([12.0, 1, 6.5], [12.1, 15, 9.5], "trim"),
]
for frm, to, a, ax in [([1, 4, 1.5], [2.2, 7, 2.7], 45, "y"), ([13.5, 8, 12], [14.8, 11, 13.2], 22.5, "z"),
                       ([12, 2, 2], [13.2, 4, 3.2], 45, "x"), ([2, 10, 12], [3.2, 13, 13.2], 22.5, "y"),
                       ([13, 12, 1], [14, 14.5, 2], 45, "z"), ([1, 1, 12], [2, 3, 13], 22.5, "x")]:
    mono.append(cube(frm, to, "glass", rot=rot(frm, to, a, ax)))
w(f"{A}/models/block/glass_monolith.json", {"parent": "minecraft:block/block", "textures": {
    "particle": "matrixglass:block/monolith_glass", "glass": "matrixglass:block/monolith_glass",
    "trim": "matrixglass:block/monolith_trim", "code": "matrixglass:block/monolith_code"}, "elements": mono})

phone = [
    cube([4, 0, 4], [12, 1, 12], "body"),
    cube([5, 1, 9], [11, 12, 12], "body"),
    cube([4, 12, 7], [12, 16, 13], "body"),
    cube([5.5, 12.5, 6.9], [10.5, 15.5, 7.0], "sign"),
    cube([6, 3, 8.6], [10, 7, 9.0], "keys"),
    cube([6, 7.5, 8.6], [10, 8.8, 9.0], "sign"),
    cube([5, 9.2, 7.6], [6.6, 11.2, 9.0], "body"), cube([9.4, 9.2, 7.6], [11, 11.2, 9.0], "body"),
    cube([6.6, 10.0, 8.0], [9.4, 10.8, 8.8], "body"),
    cube([11, 12, 11], [11.8, 16, 11.8], "body"),
]
w(f"{A}/models/block/payphone.json", {"parent": "minecraft:block/block", "textures": {
    "particle": "matrixglass:block/payphone_body", "body": "matrixglass:block/payphone_body",
    "keys": "matrixglass:block/payphone_keys", "sign": "matrixglass:block/payphone_sign"}, "elements": phone})

w(f"{A}/blockstates/glass_monolith.json", {"variants": {"": {"model": "matrixglass:block/glass_monolith"}}})
w(f"{A}/blockstates/payphone.json", {"variants": {
    f"facing={d}": {"model": "matrixglass:block/payphone", "y": y} for d, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]}})
for n in ["glass_monolith", "payphone"]:
    w(f"{A}/models/item/{n}.json", {"parent": f"matrixglass:block/{n}"})

def pill_model(name):
    L, Rr = (0, 0, 8, 16), (8, 0, 16, 16)
    els = [cube([4, 7, 7], [8, 9, 9], "capsule", L), cube([8, 7, 7], [12, 9, 9], "capsule", Rr),
           cube([3.4, 7.4, 7.4], [4, 8.6, 8.6], "capsule", L), cube([12, 7.4, 7.4], [12.6, 8.6, 8.6], "capsule", Rr)]
    w(f"{A}/models/item/{name}.json", {"textures": {"capsule": f"matrixglass:item/{name}", "particle": f"matrixglass:item/{name}"},
        "gui_light": "front", "elements": els, "display": {
        "gui": {"rotation": [25, -35, 35], "scale": [1.6, 1.6, 1.6]},
        "ground": {"translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
        "fixed": {"rotation": [0, 90, 0], "scale": [1.4, 1.4, 1.4]},
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.7, 0.7, 0.7]},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1, 2, 0], "scale": [0.9, 0.9, 0.9]}}})
pill_model("red_pill"); pill_model("blue_pill")

# ------------------------------------------------------------------ SOUNDS
sr = 44100
def T(d): return np.linspace(0, d, int(sr * d), endpoint=False)
def lp(x, k): return np.convolve(x, np.ones(k) / k, "same")
def echo(x, taps):
    y = x.copy()
    for dly, g_ in taps:
        n = int(sr * dly); y[n:] += g_ * x[:-n]
    return y
def save(name, x, peak=0.85):
    x = x / max(1e-9, np.abs(x).max()) * peak
    os.makedirs(f"{A}/sounds", exist_ok=True)
    sf.write(f"{A}/sounds/{name}.ogg", x.astype("float32"), sr, format="OGG", subtype="VORBIS")

# phone ring
t = T(2.4)
tone = 0.5 * np.sin(2 * np.pi * 1480 * t) + 0.5 * np.sin(2 * np.pi * 1180 * t)
gate = ((t < 0.9) | ((t > 1.2) & (t < 2.1))).astype(float); gate = lp(gate, 300)
chatter = (np.sin(2 * np.pi * 22 * t) > -0.2).astype(float); chatter = lp(chatter, 80)
save("phone_ring", echo(tone * gate * chatter, [(0.09, .35), (0.21, .2)]))

# glitch
parts = []
for _ in range(16):
    n = int(sr * random.uniform(0.02, 0.09)); k = random.choice("nsqz")
    if k == "n": seg = np.repeat(rng.uniform(-1, 1, n // 40 + 1), 40)[:n]
    elif k == "s": tt = np.arange(n) / sr; seg = np.sign(np.sin(2 * np.pi * random.uniform(80, 1800) * tt))
    elif k == "q" and parts: seg = parts[-1][::-1][:n] if len(parts[-1]) >= n else np.zeros(n)
    else: seg = np.zeros(n)
    parts.append(seg.astype(float) * random.uniform(.5, 1))
x = np.concatenate(parts); tt = T(len(x) / sr)
x = x + 0.6 * np.sin(2 * np.pi * np.cumsum(np.linspace(900, 90, len(x))) / sr) * np.exp(-tt * 3)
x = np.round(x * 12) / 12
save("glitch", echo(x, [(0.05, .3)]))

# wake up: sub swell + rising air + glassy chimes + boom
d = 3.6; t = T(d)
sub = np.sin(2 * np.pi * np.cumsum(40 + 25 * t / d) / sr) * (t / d) ** 2
air = lp(rng.normal(0, 1, len(t)), 6) * np.sin(np.pi * np.clip(t / 2.4, 0, 1)) ** 2 * 0.6
chime = np.zeros_like(t)
for i, f in enumerate([880, 1318.5, 1760, 2637, 3520]):
    s0 = int(sr * (0.9 + i * 0.28)); tt = t[: len(t) - s0]
    chime[s0:] += np.sin(2 * np.pi * f * tt) * np.exp(-tt * 3.2) * 0.35
boom = np.zeros_like(t); s0 = int(sr * 2.4); tt = t[: len(t) - s0]
boom[s0:] = np.sin(2 * np.pi * 45 * tt * (1 - 0.3 * tt)) * np.exp(-tt * 2.2) * 1.2
save("wake_up", echo(sub * 0.9 + air + chime + boom, [(0.18, .3), (0.37, .18)]))

# bullet time: heartbeat + sweeping whoosh + dropping warble
d = 3.0; t = T(d)
beat = np.zeros_like(t)
for s0t in [0.0, 0.42, 1.2, 1.7]:
    s0 = int(sr * s0t); tt = t[: len(t) - s0]
    beat[s0:] += np.sin(2 * np.pi * 55 * tt) * np.exp(-tt * 14) + 0.3 * rng.normal(0, 1, len(tt)) * np.exp(-tt * 70)
sweep = lp(rng.normal(0, 1, len(t)), 3 + int(40 * 0)) * 0
n_ = rng.normal(0, 1, len(t)); sweep = np.zeros_like(t)
for k_ in [4, 12, 30]: sweep += lp(n_, k_) * (1 if k_ > 4 else 0.6)
sweep *= np.sin(np.pi * t / d) ** 2 * 0.5
warb = np.sin(2 * np.pi * np.cumsum(220 * np.exp(-t * 0.9) * (1 + 0.03 * np.sin(2 * np.pi * 3 * t))) / sr) * 0.25 * np.sin(np.pi * t / d)
save("bullet_time", echo(beat * 1.2 + sweep + warb, [(0.25, .35), (0.5, .2)]))

# monolith hum (seamless-ish loop)
d = 4.0; t = T(d)
hum = sum(a * np.sin(2 * np.pi * f * t + p) for f, a, p in [(55, 1, 0), (110.4, .5, 1), (165.9, .3, 2), (221, .15, 0.5)])
hum *= 0.7 + 0.3 * np.sin(2 * np.pi * 0.5 * t) + 0.0
hum += 0.15 * lp(rng.normal(0, 1, len(t)), 40) * 8
hum *= np.minimum(1, np.minimum(t / 0.5, (d - t) / 0.5))
save("monolith_hum", hum, 0.6)

subs = {n: {"sounds": [f"matrixglass:{n}"], "subtitle": f"subtitles.matrixglass.{n}"} for n in
        ["phone_ring", "glitch", "wake_up", "bullet_time", "monolith_hum"]}
w(f"{A}/sounds.json", subs)

# ------------------------------------------------------------------ LANG / DATA
w(f"{A}/lang/en_us.json", {
    "itemGroup.matrixglass": "Matrix Glass", "block.matrixglass.glass_monolith": "Glass Monolith", "block.matrixglass.payphone": "Payphone",
    "item.matrixglass.red_pill": "Red Pill", "item.matrixglass.blue_pill": "Blue Pill",
    "effect.matrixglass.matrix_vision": "Matrix Vision", "effect.matrixglass.bullet_time": "Bullet Time",
    "subtitles.matrixglass.phone_ring": "Payphone rings", "subtitles.matrixglass.glitch": "Reality glitches",
    "subtitles.matrixglass.wake_up": "Waking up", "subtitles.matrixglass.bullet_time": "Time bends", "subtitles.matrixglass.monolith_hum": "Monolith hums"})

w(f"{R}/data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": ["matrixglass:glass_monolith", "matrixglass:payphone"]})
for n in ["glass_monolith", "payphone"]:
    w(f"{D}/loot_table/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"matrixglass:{n}"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
def i(x): return {"item": f"minecraft:{x}"}
w(f"{D}/recipe/red_pill.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [i("redstone"), i("glowstone_dust"), i("amethyst_shard")], "result": {"id": "matrixglass:red_pill", "count": 2}})
w(f"{D}/recipe/blue_pill.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [i("lapis_lazuli"), i("glowstone_dust"), i("amethyst_shard")], "result": {"id": "matrixglass:blue_pill", "count": 2}})
w(f"{D}/recipe/glass_monolith.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["OGO", "OLO", "OGO"],
    "key": {"O": i("obsidian"), "G": i("glass"), "L": i("glowstone_dust")}, "result": {"id": "matrixglass:glass_monolith", "count": 1}})
w(f"{D}/recipe/payphone.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["III", "GRG", "ICI"],
    "key": {"I": i("iron_ingot"), "G": i("glass_pane"), "R": i("redstone"), "C": i("copper_ingot")}, "result": {"id": "matrixglass:payphone", "count": 1}})
print("assets generated")

# ================================================================== G.L.A.S.S. SCREEN SYSTEM
# lens glow + powered glass textures
lens = Image.new("RGBA", (64, 64)); lp_ = lens.load()
for y in range(64):
    for x in range(64):
        d_ = math.hypot(x - 31.5, y - 31.5) / 32
        v = max(0, 1 - d_)
        lp_[x, y] = (int(40 + 200 * v ** 3), 255 if v > .1 else 60, int(65 + 170 * v ** 3), 255)
png(f"{A}/textures/block/projector_lens.png", lens)

pg = Image.new("RGBA", (64, 64), (0, 40, 14, 38)); dr = ImageDraw.Draw(pg)
dr.rectangle([0, 0, 63, 63], outline=(0, 255, 65, 210), width=3)
dr.rectangle([6, 6, 57, 57], outline=(0, 160, 45, 70))
for _ in range(14):
    x = random.randrange(8, 56); y = random.randrange(8, 56); dr.line([x, y, x + random.choice([0, 6, 10]), y + random.choice([0, 6])], fill=(0, 255, 65, 110))
for cx, cy in [(2, 2), (61, 2), (2, 61), (61, 61)]: dr.rectangle([cx - 2, cy - 2, cx + 2, cy + 2], fill=(200, 255, 215, 255))
png(f"{A}/textures/block/powered_glass.png", pg)

# models
w(f"{A}/models/block/powered_glass.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "matrixglass:block/powered_glass"}})
proj = [
    cube([2, 0, 3], [14, 2, 14], "body"), cube([3, 2, 4], [13, 11, 13], "body"),
    cube([5, 4, 2], [11, 10, 4], "body"), cube([5.5, 4.5, 1.6], [10.5, 9.5, 2.0], "lens"),
    cube([4, 11, 6], [12, 12, 7], "body"), cube([4, 11, 8], [12, 12, 9], "body"), cube([4, 11, 10], [12, 12, 11], "body"),
    cube([12.5, 11, 11.5], [13, 15, 12], "body"),
]
w(f"{A}/models/block/projector.json", {"parent": "minecraft:block/block", "textures": {
    "particle": "matrixglass:block/payphone_body", "body": "matrixglass:block/payphone_body", "lens": "matrixglass:block/projector_lens"}, "elements": proj})
term = [
    cube([2, 0, 3], [14, 2, 13], "body"), cube([3, 2, 4], [13, 12, 12], "body"), cube([4, 12, 6], [12, 13, 11], "body"),
    cube([3.4, 3.4, 3.9], [12.6, 11.6, 4.0], "trim"),        # dark bezel behind the screen
    cube([4, 4, 3.6], [12, 11, 3.9], "screen"),               # animated falling code
    cube([12, 13, 7], [12.6, 15.5, 7.6], "body"),
    cube([4, 2.4, 3.6], [5, 3.4, 4.0], "led"),
]
w(f"{A}/models/block/terminal.json", {"parent": "minecraft:block/block", "textures": {
    "particle": "matrixglass:block/payphone_body", "body": "matrixglass:block/payphone_body", "trim": "matrixglass:block/monolith_trim",
    "screen": "matrixglass:block/monolith_code", "led": "matrixglass:block/projector_lens"}, "elements": term})
for n in ["powered_glass", "projector", "terminal"]:
    w(f"{A}/models/item/{n}.json", {"parent": f"matrixglass:block/{n}"})
w(f"{A}/blockstates/powered_glass.json", {"variants": {"": {"model": "matrixglass:block/powered_glass"}}})
for n in ["projector", "terminal"]:
    w(f"{A}/blockstates/{n}.json", {"variants": {f"channel={c},facing={d}": {"model": f"matrixglass:block/{n}", "y": y}
        for c in range(8) for d, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]}})

# lang, tags, loot, recipes
lf = f"{A}/lang/en_us.json"; lang = json.load(open(lf))
lang.update({"block.matrixglass.powered_glass": "Powered Glass", "block.matrixglass.projector": "Matrix Projector", "block.matrixglass.terminal": "Matrix Terminal"})
w(lf, lang)
w(f"{R}/data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": [f"matrixglass:{n}" for n in
    ["glass_monolith", "payphone", "powered_glass", "projector", "terminal"]]})
for n in ["powered_glass", "projector", "terminal"]:
    w(f"{D}/loot_table/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"matrixglass:{n}"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
w(f"{D}/recipe/powered_glass.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [i("glass")] * 4 + [i("redstone"), i("glowstone_dust")], "result": {"id": "matrixglass:powered_glass", "count": 4}})
w(f"{D}/recipe/projector.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["III", "GEG", "IRI"],
    "key": {"I": i("iron_ingot"), "G": i("glowstone_dust"), "E": i("ender_pearl"), "R": i("redstone")}, "result": {"id": "matrixglass:projector", "count": 1}})
w(f"{D}/recipe/terminal.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["III", "PGP", "IRI"],
    "key": {"I": i("iron_ingot"), "P": i("glass_pane"), "G": i("glowstone_dust"), "R": i("redstone")}, "result": {"id": "matrixglass:terminal", "count": 1}})
print("screen assets generated")
