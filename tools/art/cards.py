"""Artă pixel pentru cărți: desenată la 110x52, mărită 2x (nearest) la 220x104 = fereastra de artă din CardFace.
Rulare: python3 cards.py <folder_iesire>"""
import sys, os, math, random
from PIL import Image, ImageDraw

LW, LH = 110, 52
OUT = (14, 10, 18, 255)
BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def hx(s, a=255):
    s = s.lstrip('#'); return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


PAL = {
    'FOC':    dict(top='1E0806', bot='5A1E0E', d='A82410', m='EE5A16', l='FFB030', w='FFF0A8', dust='FF8A30'),
    'GHEATA': dict(top='061426', bot='1A4664', d='2E6EA8', m='7CC4EE', l='D4F2FF', w='FFFFFF', dust='BFEFFF'),
    'FULGER': dict(top='0E0A22', bot='2C1E52', d='B8841C', m='FFD84A', l='FFF6B8', w='FFFFFF', dust='FFE88A'),
    'UMBRA':  dict(top='06040C', bot='261434', d='5A2E92', m='9A62DA', l='DCBCFF', w='F4ECFF', dust='B48CFF'),
    'NEUTRU': dict(top='160E08', bot='483018', d='9A7020', m='E0B040', l='FFE6A0', w='FFF8E0', dust='F0C870'),
}


class Art:
    def __init__(self, el, seed):
        self.p = {k: hx(v) for k, v in PAL[el].items()}
        self.rng = random.Random(seed)
        self.bg = Image.new('RGBA', (LW, LH))
        self.fx = Image.new('RGBA', (LW, LH))      # lumină, fără contur
        self.fg = Image.new('RGBA', (LW, LH))      # obiecte, cu contur
        self.gradient(self.p['top'], self.p['bot'])

    # -------- fundal
    def gradient(self, top, bot, steps=6):
        px = self.bg.load()
        for y in range(LH):
            t = y / (LH - 1) * (steps - 1)
            for x in range(LW):
                lvl = int(t) + (1 if (t - int(t)) * 16 > BAYER[y % 4][x % 4] else 0)
                px[x, y] = mix(top, bot, min(lvl, steps - 1) / (steps - 1))

    def stars(self, n, col=None, y1=None):
        d = ImageDraw.Draw(self.bg)
        for _ in range(n):
            x, y = self.rng.randrange(LW), self.rng.randrange(y1 or LH)
            c = col or mix(self.p['top'], (255, 255, 255, 255), self.rng.choice([0.35, 0.5, 0.8]))
            d.point((x, y), fill=c)

    def hills(self, col, base=44, amp=4, seed=0):
        d = ImageDraw.Draw(self.bg)
        r = random.Random(seed)
        ph = r.random() * 6
        for x in range(LW):
            h = base + int(amp * math.sin(x * 0.07 + ph) + 2 * math.sin(x * 0.19 + ph * 2))
            d.line([(x, h), (x, LH)], fill=col)

    def ground(self, col, y=44):
        ImageDraw.Draw(self.bg).rectangle([0, y, LW, LH], fill=col)

    # -------- lumină
    def glow(self, cx, cy, r, col, rings=4, alpha=150):
        d = ImageDraw.Draw(self.fx)
        for i in range(rings, 0, -1):
            rr = r * i / rings
            a = int(alpha * (1 - (i - 1) / rings) * 0.6)
            layer = Image.new('RGBA', (LW, LH))
            ImageDraw.Draw(layer).ellipse([cx - rr, cy - rr * 0.9, cx + rr, cy + rr * 0.9], fill=col[:3] + (a,))
            self.fx = Image.alpha_composite(self.fx, layer)

    def dust(self, n, col=None, box=(0, 0, LW, LH)):
        d = ImageDraw.Draw(self.fx)
        for _ in range(n):
            x = self.rng.randrange(box[0], box[2]); y = self.rng.randrange(box[1], box[3])
            d.point((x, y), fill=col or self.p['dust'])

    # -------- desen cu contur
    def D(self):
        return ImageDraw.Draw(self.fg)

    def render(self):
        fg = self.fg
        a = fg.load()
        out = fg.copy(); o = out.load()
        for y in range(LH):
            for x in range(LW):
                if a[x, y][3] > 0:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < LW and 0 <= yy < LH and a[xx, yy][3] > 0:
                        o[x, y] = OUT; break
        im = Image.alpha_composite(self.bg, self.fx)
        im = Image.alpha_composite(im, out)
        # vignetă în trepte
        v = Image.new('RGBA', (LW, LH)); vp = v.load()
        for y in range(LH):
            for x in range(LW):
                dx = (x - LW / 2) / (LW / 2); dy = (y - LH / 2) / (LH / 2)
                dd = dx * dx * 0.7 + dy * dy * 0.9
                lvl = 0 if dd < 0.55 else 1 if dd < 0.8 else 2 if dd < 1.05 else 3
                if lvl and (lvl > 1 or BAYER[y % 4][x % 4] < 8):
                    vp[x, y] = (0, 0, 0, [0, 50, 85, 120][lvl])
        im = Image.alpha_composite(im, v)
        return im.resize((LW * 2, LH * 2), Image.NEAREST)


# ===================================================================== primitive de motive

def flame(a, cx, by, w, h, cols=None):
    """o flacără verticală cu bază la (cx, by)"""
    d = a.D(); p = a.p
    cols = cols or [p['d'], p['m'], p['l'], p['w']]
    for i, c in enumerate(cols):
        k = 1 - i * 0.24
        ww, hh = w * k, h * k
        d.polygon([(cx - ww / 2, by), (cx - ww * 0.42, by - hh * 0.45), (cx - ww * 0.12, by - hh * 0.75),
                   (cx, by - hh), (cx + ww * 0.18, by - hh * 0.62), (cx + ww * 0.45, by - hh * 0.4),
                   (cx + ww / 2, by)], fill=c)


def orb(a, cx, cy, r, cols):
    d = a.D()
    for i, c in enumerate(cols):
        rr = r * (1 - i * 0.28)
        ox = -r * 0.12 * i
        d.ellipse([cx - rr + ox, cy - rr + ox, cx + rr + ox, cy + rr + ox], fill=c)


def bolt(a, pts, w=2, cols=None):
    d = a.D(); p = a.p
    cols = cols or [p['d'], p['m'], p['w']]
    d.line(pts, fill=cols[0], width=w + 2)
    d.line(pts, fill=cols[1], width=w)
    if w >= 2:
        d.line(pts, fill=cols[2], width=1)


def zig(x0, y0, x1, y1, n, amp, rng):
    pts = [(x0, y0)]
    for i in range(1, n):
        t = i / n
        pts.append((x0 + (x1 - x0) * t + rng.uniform(-amp, amp), y0 + (y1 - y0) * t + rng.uniform(-amp, amp)))
    pts.append((x1, y1))
    return pts


def shield(a, cx, cy, s, body, rim, emblem=None):
    d = a.D()
    pts = [(cx - s, cy - s * 0.9), (cx + s, cy - s * 0.9), (cx + s, cy), (cx, cy + s * 1.1), (cx - s, cy)]
    d.polygon(pts, fill=rim)
    s2 = s - 2
    d.polygon([(cx - s2, cy - s2 * 0.9 + 1), (cx + s2, cy - s2 * 0.9 + 1), (cx + s2, cy), (cx, cy + s2 * 1.1), (cx - s2, cy)], fill=body)
    d.line([(cx - s2 + 2, cy - s2 * 0.9 + 3), (cx - s2 + 2, cy - 1)], fill=mix(body, (255, 255, 255, 255), 0.45))
    if emblem:
        emblem(d, cx, cy)


def snowflake(d, cx, cy, r, c, c2):
    for k in range(6):
        ang = k * math.pi / 3
        x1, y1 = cx + r * math.cos(ang), cy + r * math.sin(ang)
        d.line([(cx, cy), (x1, y1)], fill=c, width=2)
        for t in (0.55,):
            bx, by = cx + r * t * math.cos(ang), cy + r * t * math.sin(ang)
            for s in (1, -1):
                d.line([(bx, by), (bx + r * 0.3 * math.cos(ang + s * 0.8), by + r * 0.3 * math.sin(ang + s * 0.8))], fill=c2)
    d.rectangle([cx - 1, cy - 1, cx + 1, cy + 1], fill=c2)


def shard(d, x, y, w, h, c, c2, lean=0):
    d.polygon([(x, y + h), (x + w, y + h), (x + w / 2 + lean, y)], fill=c)
    d.line([(x + w / 2 + lean, y + 1), (x + w * 0.35, y + h - 1)], fill=c2)


def cloud(a, cx, cy, w, c, c2):
    d = a.D()
    d.ellipse([cx - w * 0.5, cy - 5, cx - w * 0.05, cy + 5], fill=c)
    d.ellipse([cx - w * 0.25, cy - 9, cx + w * 0.2, cy + 4], fill=c)
    d.ellipse([cx, cy - 6, cx + w * 0.5, cy + 5], fill=c)
    d.rectangle([cx - w * 0.45, cy, cx + w * 0.45, cy + 5], fill=c)
    d.line([(cx - w * 0.42, cy + 5), (cx + w * 0.42, cy + 5)], fill=c2)


def spear(d, x0, y0, x1, y1, shaft, tip):
    d.line([(x0, y0), (x1, y1)], fill=shaft, width=2)
    ang = math.atan2(y1 - y0, x1 - x0)
    tx, ty = x1 + 7 * math.cos(ang), y1 + 7 * math.sin(ang)
    px, py = -math.sin(ang) * 3, math.cos(ang) * 3
    d.polygon([(x1 + px, y1 + py), (tx, ty), (x1 - px, y1 - py)], fill=tip)


def sword(d, x0, y0, x1, y1, blade, hilt):
    d.line([(x0, y0), (x1, y1)], fill=blade, width=3)
    ang = math.atan2(y1 - y0, x1 - x0)
    px, py = -math.sin(ang) * 5, math.cos(ang) * 5
    gx, gy = x0 + 4 * math.cos(ang), y0 + 4 * math.sin(ang)
    d.line([(gx + px, gy + py), (gx - px, gy - py)], fill=hilt, width=2)
    d.line([(x0, y0), (x0 - 4 * math.cos(ang), y0 - 4 * math.sin(ang))], fill=hx('6A4A2A'), width=2)


def runes_ring(d, cx, cy, r, c, n=8, rng=None):
    d.ellipse([cx - r, cy - r * 0.45, cx + r, cy + r * 0.45], outline=c)
    for k in range(n):
        ang = k * 2 * math.pi / n
        x, y = cx + r * math.cos(ang), cy + r * 0.45 * math.sin(ang)
        d.line([(x, y - 2), (x, y + 1)], fill=c)
        d.point((x + 1, y - 2), fill=c)


# ===================================================================== cărțile

def c_ParjolulSferic(a):
    p = a.p; a.stars(14, y1=30); a.hills(hx('140604'), 46)
    a.glow(66, 22, 22, p['m'])
    d = a.D()
    for i in range(5):   # coada
        t = i / 5
        flame_w = 14 * (1 - t)
        d.ellipse([60 - 34 * t - flame_w / 2, 18 - 14 * t - flame_w / 3, 60 - 34 * t + flame_w / 2, 18 - 14 * t + flame_w / 3], fill=[p['d'], p['m'], p['m'], p['l'], p['l']][4 - i])
    orb(a, 66, 24, 10, [p['d'], p['m'], p['l'], p['w']])
    a.dust(24, box=(20, 4, 90, 40))


def c_CadereaStelelor(a):
    p = a.p; a.gradient(hx('0A0820'), hx('3A1A20')); a.stars(30, y1=28); a.hills(hx('120A10'), 44, 3)
    d = a.D()
    for (x, y, s) in ((30, 10, 5), (58, 6, 7), (84, 14, 4)):
        d.line([(x - 14, y - 9), (x, y)], fill=p['d'], width=3)
        d.line([(x - 10, y - 6), (x, y)], fill=p['l'], width=1)
        orb(a, x, y, s, [p['d'], p['m'], p['w']])
        a.glow(x, y, s * 2.6, p['m'], 3, 110)
    a.glow(55, 46, 26, p['m'], 3, 120)
    flame(a, 40, 46, 8, 7); flame(a, 66, 46, 10, 9); flame(a, 88, 46, 7, 6)


def c_FulgerDeSange(a):
    p = a.p; a.gradient(hx('140610'), hx('3A0E1A')); a.stars(10, y1=20)
    red = [hx('6A0A14'), hx('D02838'), hx('FFC8C8')]
    a.glow(55, 26, 22, hx('D02838'))
    bolt(a, zig(52, 0, 58, 50, 7, 7, a.rng), 3, red)
    d = a.D()
    for x, y in ((40, 34), (70, 30), (64, 42)):
        d.ellipse([x - 2, y - 1, x + 2, y + 3], fill=red[1]); d.point((x, y - 2), fill=red[1])


def c_UraganDeAzur(a):
    p = a.p; a.gradient(hx('081428'), hx('1E3C70')); a.stars(10, y1=20)
    d = a.D(); c = [hx('3A6AB8'), hx('7AB0F0'), hx('D8ECFF')]
    for i in range(9):
        y = 6 + i * 4.5; w = 30 - i * 3; cx = 55 + math.sin(i * 0.8) * 4
        d.ellipse([cx - w / 2, y - 2, cx + w / 2, y + 2], outline=c[i % 3], width=2)
    a.glow(55, 22, 24, hx('7AB0F0'), 3, 110)
    for _ in range(4):
        x = a.rng.randrange(25, 85); y = a.rng.randrange(6, 40)
        bolt(a, zig(x, y, x + a.rng.randrange(-8, 8), y + 7, 3, 2, a.rng), 1, [hx('B8841C'), hx('FFE060'), hx('FFFFFF')])


def c_ManiorulIernii(a):
    p = a.p; a.stars(10); a.ground(hx('163654'), 42)
    a.glow(55, 26, 24, p['m'])
    d = a.D()
    d.polygon([(40, 44), (36, 18), (46, 6), (66, 6), (74, 18), (70, 44)], fill=p['m'])
    d.polygon([(44, 40), (41, 19), (49, 10), (62, 10), (68, 19), (65, 40)], fill=p['l'])
    # silueta înghețată
    d.ellipse([50, 14, 60, 24], fill=hx('5A7C9A')); d.rectangle([48, 24, 62, 38], fill=hx('5A7C9A'))
    d.line([(44, 12), (48, 30)], fill=p['w']); d.line([(64, 14), (66, 24)], fill=p['w'])
    a.dust(26, p['w'])


def c_PavazaGlaciara(a):
    p = a.p; a.stars(10)
    a.glow(55, 26, 24, p['m'])
    shield(a, 55, 24, 15, p['m'], p['d'], lambda d, cx, cy: snowflake(d, cx, cy - 2, 7, p['l'], p['w']))
    d = a.D()
    for x in (30, 80):
        shard(d, x - 4, 30, 8, 16, p['m'], p['w'])
    a.dust(20, p['w'])


def c_PavazaAmurgului(a):
    p = a.p; a.gradient(hx('140A1E'), hx('4A2A30')); a.stars(16, y1=24); a.hills(hx('1A0E18'), 44)
    a.glow(55, 26, 24, p['m'])
    shield(a, 55, 24, 15, hx('5A2E92'), hx('2E1650'),
           lambda d, cx, cy: (d.ellipse([cx - 5, cy - 8, cx + 5, cy + 2], fill=hx('F0B060')), d.ellipse([cx - 2, cy - 10, cx + 7, cy], fill=hx('5A2E92'))))
    a.dust(18)


def c_Dezolare(a):
    p = a.p; a.gradient(hx('040306'), hx('16101C')); a.stars(6)
    d = a.D()
    d.ellipse([39, 10, 71, 42], fill=hx('2A1A3A')); d.ellipse([43, 14, 67, 38], fill=hx('0A060E')); d.ellipse([49, 20, 61, 32], fill=hx('000000'))
    for ang in (0.4, 1.9, 3.3, 4.6, 5.6):
        x0, y0 = 55 + 16 * math.cos(ang), 26 + 16 * math.sin(ang)
        d.line([(x0, y0), (x0 + 10 * math.cos(ang), y0 + 10 * math.sin(ang)), (x0 + 14 * math.cos(ang + 0.3), y0 + 14 * math.sin(ang + 0.3))], fill=hx('5A3A7A'))
    a.dust(14, hx('6A4A8A'))


def c_PovaraAnilor(a):
    p = a.p; a.stars(14)
    a.glow(55, 26, 22, p['m'])
    d = a.D(); wood = hx('6A4A2A'); glass = hx('3A2A4A')
    d.rectangle([42, 6, 68, 9], fill=wood); d.rectangle([42, 43, 68, 46], fill=wood)
    d.rectangle([43, 9, 45, 43], fill=wood); d.rectangle([65, 9, 67, 43], fill=wood)
    d.polygon([(47, 10), (63, 10), (56, 26), (63, 42), (47, 42), (54, 26)], fill=glass)
    d.polygon([(49, 12), (61, 12), (55, 22)], fill=p['l'])
    d.polygon([(49, 41), (61, 41), (55, 33)], fill=p['m'])
    d.line([(55, 22), (55, 34)], fill=p['l'])


def c_GeamatulEcoului(a):
    p = a.p; a.stars(12)
    d = a.D()
    for i, r in enumerate((6, 11, 16, 21, 26)):
        c = [p['w'], p['l'], p['m'], p['d'], p['d']][i]
        d.arc([30 - r, 26 - r, 30 + r, 26 + r], -50, 50, fill=c, width=2)
        d.arc([80 - r, 26 - r, 80 + r, 26 + r], 130, 230, fill=c, width=2)
    orb(a, 30, 26, 4, [p['d'], p['l']]); orb(a, 80, 26, 4, [p['d'], p['l']])
    a.glow(55, 26, 18, p['m'], 3, 90)


def c_DeslusireaRunelor(a):
    p = a.p; a.gradient(hx('140E08'), hx('3E2A14')); a.stars(8, y1=20)
    a.glow(55, 26, 26, p['m'])
    d = a.D(); page = hx('E8DCC0'); page2 = hx('C8B890')
    d.polygon([(30, 38), (54, 34), (54, 14), (30, 18)], fill=page); d.polygon([(56, 34), (80, 38), (80, 18), (56, 14)], fill=page)
    d.line([(55, 14), (55, 36)], fill=page2, width=2)
    d.polygon([(28, 40), (55, 36), (82, 40), (82, 41), (28, 41)], fill=hx('5A3A1E'))
    for i in range(4):
        d.line([(34, 22 + i * 4), (50, 20 + i * 4)], fill=page2)
    for (x, y) in ((62, 18), (70, 24), (64, 28)):
        d.line([(x, y), (x + 3, y + 4)], fill=p['m']); d.line([(x + 3, y), (x, y + 4)], fill=p['m'])
    a.dust(18, p['l'], (30, 2, 80, 20))


def c_FluxulStrabun(a):
    p = a.p; a.gradient(hx('061422'), hx('1C3A5A')); a.stars(16)
    a.glow(55, 26, 24, hx('6AC8FF'))
    d = a.D()
    for i in range(30):
        t = i / 30
        x = 10 + t * 90; y = 26 + math.sin(t * 9) * 10 * (1 - abs(t - 0.5) * 1.6)
        d.ellipse([x - 1.5, y - 1.5, x + 1.5, y + 1.5], fill=hx('7ADAFF') if i % 3 else hx('E0FAFF'))
    orb(a, 55, 26, 8, [hx('2A6AB0'), hx('6AC8FF'), hx('E8FBFF')])
    a.dust(20, hx('BFF0FF'))


def c_SuflulVietii(a):
    p = a.p; a.gradient(hx('081A10'), hx('1E4428')); a.stars(10, hx('A8F0B0'))
    a.glow(55, 26, 24, hx('7AE890'))
    d = a.D(); g = [hx('2A7A3A'), hx('5AC86A'), hx('C8FFD0')]
    d.ellipse([42, 12, 68, 38], fill=g[0]); d.ellipse([45, 14, 66, 35], fill=g[1])
    d.line([(46, 36), (64, 14)], fill=g[2], width=1)
    for k in range(3):
        d.line([(50 + k * 4, 32 - k * 5), (46 + k * 4, 26 - k * 5)], fill=g[2])
    d.line([(44, 40), (48, 34)], fill=hx('6A4A2A'), width=2)
    a.dust(30, hx('E0FFE4'))


def c_RafalaDeBruma(a):
    p = a.p; a.stars(10); a.hills(hx('0E2A44'), 44)
    d = a.D()
    for i, y in enumerate((12, 20, 28, 36)):
        x0 = 10 + i * 6
        pts = [(x0 + k * 6, y + math.sin(k * 0.9 + i) * 3) for k in range(14)]
        d.line(pts, fill=[p['d'], p['m'], p['l'], p['m']][i], width=2)
        d.arc([pts[-1][0] - 4, y - 6, pts[-1][0] + 4, y + 2], 270, 90, fill=p['l'], width=1)
    a.dust(40, p['w'])


def c_PloaieRece(a):
    p = a.p; a.gradient(hx('0A1420'), hx('22384E')); a.ground(hx('16283A'), 45)
    cloud(a, 55, 12, 56, hx('4A5A70'), hx('2A3648'))
    d = a.D()
    for _ in range(30):
        x = a.rng.randrange(28, 84); y = a.rng.randrange(20, 44)
        d.line([(x, y), (x - 1, y + 3)], fill=p['m'])
    for x in (36, 58, 76):
        d.ellipse([x - 4, 45, x + 4, 47], outline=p['l'])


def c_ScanteiaStrabuna(a):
    p = a.p; a.gradient(hx('100C1E'), hx('2A2236')); a.ground(hx('1A1626'), 42)
    d = a.D(); stone = hx('6A6474')
    d.polygon([(42, 44), (44, 16), (55, 10), (66, 16), (68, 44)], fill=stone)
    d.polygon([(46, 42), (47, 18), (55, 13), (63, 18), (64, 42)], fill=hx('7E7888'))
    d.line([(51, 22), (55, 34), (59, 22)], fill=p['m']); d.line([(53, 28), (57, 28)], fill=p['m'])
    a.glow(55, 26, 16, p['m'], 3, 120)
    bolt(a, [(55, 4), (52, 9), (57, 11), (54, 16)], 1)
    a.dust(14, p['l'])


def c_SuflulPirjolit(a):
    p = a.p; a.hills(hx('1A0806'), 46, 2)
    a.glow(55, 34, 30, p['m'], 4, 130)
    for i, x in enumerate(range(12, 100, 9)):
        h = 14 + (i % 3) * 6 + a.rng.randrange(4)
        flame(a, x, 46, 12, h)
    a.dust(30, box=(10, 4, 100, 30))


def c_ScanteiaDeJar(a):
    p = a.p; a.gradient(hx('140806'), hx('2E120A'))
    a.glow(55, 30, 18, p['m'])
    d = a.D(); skin = hx('C88A68'); skin2 = hx('A06A50')
    d.polygon([(36, 46), (40, 34), (70, 34), (76, 46)], fill=skin)
    for k in range(4):
        d.rectangle([44 + k * 7, 28 + (k % 2), 48 + k * 7, 34], fill=skin2 if k % 2 else skin)
    orb(a, 56, 30, 5, [p['d'], p['m'], p['l'], p['w']])
    a.dust(16, box=(40, 6, 72, 28))


def c_MantiaNoptii(a):
    p = a.p; a.stars(30, y1=40)
    d = a.D()
    d.ellipse([74, 4, 90, 20], fill=hx('E8E0F8')); d.ellipse([78, 3, 94, 18], fill=hx('0C0814'))
    a.glow(80, 12, 12, hx('C8B8F0'), 3, 80)
    d.polygon([(55, 8), (66, 14), (72, 46), (38, 46), (44, 14)], fill=hx('3A1E5A'))
    d.polygon([(55, 8), (62, 13), (66, 46), (44, 46), (48, 13)], fill=hx('241238'))
    d.ellipse([50, 10, 60, 20], fill=hx('0A0610')); d.point((53, 15), fill=p['l']); d.point((57, 15), fill=p['l'])
    a.dust(14)


def c_LanceDeFoc(a):
    p = a.p; a.stars(8, y1=24); a.hills(hx('160604'), 46)
    a.glow(70, 26, 20, p['m'])
    d = a.D()
    d.polygon([(10, 22), (60, 23), (60, 29), (10, 30)], fill=p['d'])
    d.polygon([(18, 24), (64, 24), (64, 28), (18, 28)], fill=p['m'])
    d.polygon([(58, 18), (92, 26), (58, 34)], fill=p['l'])
    d.polygon([(62, 22), (86, 26), (62, 30)], fill=p['w'])
    a.dust(26, box=(6, 14, 70, 38))


def c_FulgerInLant(a):
    p = a.p; a.stars(12); a.ground(hx('1A1430'), 42)
    d = a.D(); en = hx('3A2E4A')
    pts = [(18, 34), (55, 30), (92, 36)]
    for x, y in pts:
        d.ellipse([x - 5, y - 10, x + 5, y], fill=en); d.rectangle([x - 6, y - 2, x + 6, y + 10], fill=en)
        a.glow(x, y, 9, p['m'], 2, 120)
    bolt(a, zig(18, 28, 55, 24, 4, 3, a.rng) + zig(55, 24, 92, 30, 4, 3, a.rng)[1:], 2)
    bolt(a, zig(18, 0, 18, 26, 3, 3, a.rng), 2)


def c_SocTermic(a):
    p = a.p; a.gradient(hx('0A0E1C'), hx('2A1A2A'))
    fire = PAL['FOC']; ice = PAL['GHEATA']
    a.glow(45, 26, 18, hx(fire['m'])); a.glow(65, 26, 18, hx(ice['m']))
    d = a.D()
    d.pieslice([39, 10, 71, 42], 90, 270, fill=hx(fire['m'])); d.pieslice([39, 10, 71, 42], 270, 90, fill=hx(ice['m']))
    d.pieslice([44, 15, 66, 37], 90, 270, fill=hx(fire['l'])); d.pieslice([44, 15, 66, 37], 270, 90, fill=hx(ice['l']))
    bolt(a, [(55, 8), (52, 18), (58, 26), (53, 34), (56, 44)], 1, [hx('FFFFFF'), hx('FFFFFF'), hx('FFFFFF')])


def c_ExecutieUmbrita(a):
    p = a.p; a.stars(10)
    a.glow(55, 26, 22, p['m'])
    d = a.D()
    d.line([(30, 46), (72, 6)], fill=hx('4A3A30'), width=2)
    d.chord([56, 2, 96, 34], 180, 300, fill=p['l'])
    d.chord([60, 6, 92, 30], 180, 300, fill=hx('06040C'))
    for k in range(6):
        y = 34 + k * 2
        d.line([(16 + k * 5, y), (40 + k * 6, y + 1)], fill=p['d'])
    a.dust(14)


def c_ArmoniaElementelor(a):
    a.gradient(hx('0E0A14'), hx('2E2430')); a.stars(16)
    a.glow(55, 26, 20, hx('FFF0C8'), 3, 90)
    d = a.D()
    d.ellipse([41, 12, 69, 40], outline=hx('C8B890'))
    for k, el in enumerate(('FOC', 'GHEATA', 'FULGER', 'UMBRA')):
        ang = k * math.pi / 2 - math.pi / 4
        x, y = 55 + 16 * math.cos(ang), 26 + 13 * math.sin(ang)
        pp = PAL[el]
        orb(a, x, y, 5, [hx(pp['d']), hx(pp['m']), hx(pp['w'])])
        a.glow(x, y, 9, hx(pp['m']), 2, 100)
    orb(a, 55, 26, 4, [hx('C8B890'), hx('FFF8E0')])


def c_ChemareaHaiducilor(a):
    p = a.p; a.gradient(hx('1A100A'), hx('4E3A20')); a.stars(8, y1=16)
    a.hills(hx('241808'), 40, 5, 3)
    d = a.D()
    for x in range(22, 92, 7):   # sulițele haiducilor pe culme
        spear(d, x, 46, x + 2, 26 + (x % 3) * 2, hx('6A4A2A'), hx('C8CCD8'))
    horn = [hx('D8C8A0'), hx('8A6A3A')]
    d.polygon([(38, 18), (66, 10), (70, 22), (42, 24)], fill=horn[0])
    d.ellipse([64, 8, 74, 24], fill=horn[1])
    for r in (6, 10, 14):
        d.arc([72 - r, 16 - r, 72 + r, 16 + r], -40, 40, fill=p['l'])


def c_SabieSiVraja(a):
    p = a.p; a.stars(8); a.glow(55, 26, 24, p['m'])
    d = a.D()
    sword(d, 34, 42, 76, 8, hx('D8DCE8'), hx('C8A040'))
    flame(a, 72, 46, 16, 34)
    d2 = a.D()
    spear(d2, 78, 44, 40, 8, hx('6A4A2A'), hx('C8CCD8'))


def c_FurtunaHaiducilor(a):
    p = a.p; a.gradient(hx('0A0818'), hx('241A3A'))
    cloud(a, 55, 10, 80, hx('3A3458'), hx('201C34'))
    d = a.D()
    for i, x in enumerate(range(18, 96, 10)):
        spear(d, x - 6, 18 + (i % 2) * 4, x + 2, 34 + (i % 3) * 2, hx('6A4A2A'), hx('E0E4EC'))
    for x in (30, 62, 86):
        bolt(a, zig(x, 14, x + a.rng.randrange(-6, 6), 46, 4, 4, a.rng), 2)
    a.glow(55, 30, 28, p['m'], 3, 90)


def c_Turtura(a):
    p = a.p; a.gradient(hx('081A2A'), hx('1E4A68'))
    d = a.D()
    d.rectangle([0, 0, LW, 6], fill=hx('2E5A80')); d.line([(0, 6), (LW, 6)], fill=p['l'])
    for x, h in ((14, 14), (24, 22), (34, 10), (46, 30), (58, 18), (70, 26), (82, 12), (94, 20)):
        d.polygon([(x - 4, 6), (x + 4, 6), (x, 6 + h)], fill=p['m'])
        d.line([(x - 1, 7), (x, 4 + h)], fill=p['w'])
    a.glow(46, 24, 16, p['m'], 2, 90)
    a.dust(16, p['w'], (0, 30, LW, LH))


def c_ValDeCeata(a):
    p = a.p; a.gradient(hx('0C1820'), hx('24404A')); a.hills(hx('16303A'), 40, 4, 5)
    shield(a, 55, 22, 12, hx('3E6A8A'), hx('22405A'))
    for i, y in enumerate((18, 26, 34, 42)):
        layer = Image.new('RGBA', (LW, LH)); dd = ImageDraw.Draw(layer)
        for k in range(8):
            x = (k * 18 + i * 9) % 120 - 10
            dd.ellipse([x, y - 3, x + 26, y + 3], fill=(200, 225, 235, 70 + i * 15))
        a.fx = Image.alpha_composite(a.fx, layer)


def c_ArcElectric(a):
    p = a.p; a.stars(10); a.ground(hx('1A1430'), 42)
    d = a.D(); metal = hx('6A6A7A')
    for x in (24, 86):
        d.rectangle([x - 2, 18, x + 2, 44], fill=metal); d.ellipse([x - 5, 12, x + 5, 22], fill=hx('8A8AA0'))
        a.glow(x, 17, 9, p['m'], 2, 140)
    for k in range(3):
        bolt(a, zig(26, 17, 84, 17, 9, 6 - k, a.rng), 2 if k == 0 else 1)


def c_ClesteDeUmbra(a):
    p = a.p; a.stars(10); a.ground(hx('120A1C'), 44)
    d = a.D()
    for s in (1, -1):
        cx = 55 - s * 20
        pts = [(cx - s * 14, 46), (cx - s * 8, 30), (cx + s * 4, 16), (cx + s * 16, 14), (cx + s * 8, 20), (cx - s * 2, 32), (cx - s * 4, 46)]
        d.polygon(pts, fill=p['d'])
        d.line(pts[1:4], fill=p['m'])
    d.ellipse([49, 18, 61, 30], fill=hx('5A3A4A')); d.point((53, 23), fill=p['w']); d.point((57, 23), fill=p['w'])
    a.glow(55, 22, 18, p['m'], 3, 100)


def c_TortaVie(a):
    p = a.p; a.gradient(hx('140A08'), hx('2E1A12'))
    d = a.D()
    for y in range(4, 48, 8):   # zid de piatră
        for x in range((y // 8 % 2) * 7, LW, 14):
            ImageDraw.Draw(a.bg).rectangle([x, y, x + 12, y + 6], outline=hx('3A2618'))
    a.glow(55, 18, 26, p['m'])
    d.polygon([(52, 46), (58, 46), (60, 24), (50, 24)], fill=hx('6A4A2A'))
    d.rectangle([48, 22, 62, 26], fill=hx('8A8A9A'))
    flame(a, 55, 23, 16, 20)
    a.dust(20, box=(38, 0, 72, 20))


def c_SulitaDeFoc(a):
    p = a.p; a.stars(8, y1=24); a.hills(hx('160604'), 46)
    a.glow(78, 14, 18, p['m'])
    d = a.D()
    for k in range(6):
        t = k / 6
        d.ellipse([18 + t * 50 - 3, 42 - t * 24 - 2, 18 + t * 50 + 3, 42 - t * 24 + 2], fill=[p['d'], p['m'], p['l']][k % 3])
    spear(d, 20, 44, 78, 14, hx('6A4A2A'), p['l'])
    flame(a, 80, 14, 8, 10)


def c_InfernulMic(a):
    p = a.p; a.ground(hx('1E0A06'), 40)
    a.glow(55, 32, 30, p['m'], 4, 140)
    for i in range(7):
        ang = i / 7 * 2 * math.pi
        x = 55 + 26 * math.cos(ang); y = 40 + 6 * math.sin(ang)
        flame(a, x, y, 10, 14 + (i % 2) * 6)
    flame(a, 55, 40, 18, 28)


def c_PrabusireaGerului(a):
    p = a.p; a.stars(10); a.ground(hx('183A58'), 42)
    d = a.D()
    d.polygon([(40, 4), (72, 2), (80, 18), (66, 30), (44, 28), (34, 16)], fill=p['m'])
    d.polygon([(44, 6), (68, 5), (74, 17), (62, 26), (47, 24), (40, 15)], fill=p['l'])
    for k in range(6):
        d.line([(40 + k * 6, 30 + k % 2), (36 + k * 6, 36)], fill=p['m'])
    for x in (30, 50, 70, 86):
        shard(d, x - 3, 34, 7, 10, p['m'], p['w'], a.rng.randrange(-2, 3))
    a.glow(55, 40, 26, p['m'], 3, 110)
    a.dust(20, p['w'])


def c_RitualulUmbrelor(a):
    p = a.p; a.stars(12); a.ground(hx('140C1E'), 30)
    d = a.D()
    runes_ring(d, 55, 38, 30, p['m'], 10)
    runes_ring(d, 55, 38, 18, p['l'], 6)
    for k in range(5):
        ang = k * 2 * math.pi / 5 - math.pi / 2
        x, y = 55 + 30 * math.cos(ang), 38 + 13 * math.sin(ang)
        d.rectangle([x - 1, y - 6, x + 1, y], fill=hx('D8D0C0'))
        flame(a, x, y - 6, 4, 6, [hx('A82410'), hx('FFB030')])
    a.glow(55, 34, 22, p['m'], 3, 120)


def c_TrasnetulVidului(a):
    p = a.p; a.gradient(hx('04020A'), hx('1E1030')); a.stars(40)
    d = a.D()
    d.ellipse([40, 0, 70, 12], fill=hx('1A0A2A')); d.ellipse([44, 2, 66, 10], fill=hx('000000'))
    a.glow(55, 6, 16, hx('9A62DA'), 3, 130)
    bolt(a, zig(55, 8, 54, 50, 6, 6, a.rng), 3, [hx('5A2E92'), hx('FFD84A'), hx('FFFFFF')])
    a.dust(18, hx('B48CFF'))


def c_Rezonanta(a):
    a.gradient(hx('0E0A16'), hx('2A2236')); a.stars(14)
    d = a.D()
    cols = [PAL['FOC']['m'], PAL['GHEATA']['m'], PAL['FULGER']['m'], PAL['UMBRA']['m']]
    for i, r in enumerate((8, 13, 18, 23)):
        d.ellipse([55 - r, 26 - r * 0.8, 55 + r, 26 + r * 0.8], outline=hx(cols[i]), width=1)
    d.line([(55, 16), (55, 40)], fill=hx('C8C0D8'), width=2)
    d.line([(51, 10), (51, 20)], fill=hx('C8C0D8'), width=2); d.line([(59, 10), (59, 20)], fill=hx('C8C0D8'), width=2)
    d.line([(51, 20), (59, 20)], fill=hx('C8C0D8'), width=2)
    a.glow(55, 22, 14, hx('FFFFFF'), 3, 70)


def c_InimaFurtunii(a):
    p = a.p; a.gradient(hx('0C0820'), hx('2A1E50'))
    d = a.D()
    for i in range(36):
        t = i / 36 * 4 * math.pi; r = 4 + i * 0.75
        x, y = 55 + r * math.cos(t), 26 + r * 0.7 * math.sin(t)
        d.ellipse([x - 2, y - 1.5, x + 2, y + 1.5], fill=[hx('4A4070'), hx('6A5A90'), hx('8A7AB0')][i % 3])
    a.glow(55, 26, 14, p['m'], 3, 170)
    orb(a, 55, 26, 5, [p['d'], p['m'], p['w']])
    for ang in (0.6, 2.5, 4.4):
        x, y = 55 + 26 * math.cos(ang), 26 + 18 * math.sin(ang)
        bolt(a, zig(55 + 7 * math.cos(ang), 26 + 5 * math.sin(ang), x, y, 3, 2, a.rng), 1)


CARDS = {
    'ParjolulSferic': 'FOC', 'CadereaStelelor': 'FOC', 'FulgerDeSange': 'FULGER', 'UraganDeAzur': 'FULGER',
    'ManiorulIernii': 'GHEATA', 'PavazaGlaciara': 'GHEATA', 'PavazaAmurgului': 'UMBRA', 'Dezolare': 'UMBRA',
    'PovaraAnilor': 'UMBRA', 'GeamatulEcoului': 'NEUTRU', 'DeslusireaRunelor': 'NEUTRU', 'FluxulStrabun': 'NEUTRU',
    'SuflulVietii': 'NEUTRU', 'RafalaDeBruma': 'GHEATA', 'PloaieRece': 'GHEATA', 'ScanteiaStrabuna': 'FULGER',
    'SuflulPirjolit': 'FOC', 'ScanteiaDeJar': 'FOC', 'MantiaNoptii': 'UMBRA', 'LanceDeFoc': 'FOC',
    'FulgerInLant': 'FULGER', 'SocTermic': 'GHEATA', 'ExecutieUmbrita': 'UMBRA', 'ArmoniaElementelor': 'NEUTRU',
    'ChemareaHaiducilor': 'NEUTRU', 'SabieSiVraja': 'FOC', 'FurtunaHaiducilor': 'FULGER', 'Turtura': 'GHEATA',
    'ValDeCeata': 'GHEATA', 'ArcElectric': 'FULGER', 'ClesteDeUmbra': 'UMBRA', 'TortaVie': 'FOC',
    'SulitaDeFoc': 'FOC', 'InfernulMic': 'FOC', 'PrabusireaGerului': 'GHEATA', 'RitualulUmbrelor': 'UMBRA',
    'TrasnetulVidului': 'UMBRA', 'Rezonanta': 'NEUTRU', 'InimaFurtunii': 'FULGER',
}


def main(outdir):
    os.makedirs(outdir, exist_ok=True)
    tiles = []
    for i, (cid, el) in enumerate(CARDS.items()):
        a = Art(el, i * 7919 + 13)
        globals()['c_' + cid](a)
        im = a.render()
        im.save(os.path.join(outdir, cid + '.png'))
        tiles.append(im)
    cols = 5
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * 228, rows * 112), (30, 26, 34, 255))
    for i, im in enumerate(tiles):
        sheet.alpha_composite(im, ((i % cols) * 228 + 4, (i // cols) * 112 + 4))
    sheet.save(os.path.join(outdir, '_preview.png'))


if __name__ == '__main__':
    main(sys.argv[1])
