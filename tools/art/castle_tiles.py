"""Dalele Castelului de Obsidian (32x32), în stilul Podului: piatră neagră-violet, lumini reci, rune violet stinse.
Rulare din rădăcina proiectului:  python tools/art/castle_tiles.py res/Tiles/Castelul

Ce dală e ce (vezi harta res/Maps/Castelul și TileManager, cazul "Castelul"):
  W1..W6  zidul: bloc de 2 x 3 dale (W1 W2 = creneluri, W3 W4 = fața zidului, W5 W6 = baza)
  R1..R8  turnul din stânga: R1-R6 bloc de 2 x 3, R7 R8 = baza lui sub poartă
  T1..T8  turnul din dreapta, la fel
  L, R    zidurile laterale (2 dale lățime, pe verticală)
  path    lespezile din curte (se repetă fără margini vizibile)
  CastleGrass, Abis  iarba moartă dintre lespezi și golul negru (aceleași nume ca la Pod, dar în paleta castelului)
"""
import os
import random
import sys
from PIL import Image

S = 32
OUT = sys.argv[1] if len(sys.argv) > 1 else "res/Tiles/Castelul"

STONE = [(26, 20, 34), (34, 27, 44), (42, 33, 54), (50, 40, 64)]   # de la umbră la lumină
MORTAR = (14, 10, 19)
EDGE = (8, 6, 12)
HILITE = (92, 84, 120)        # muchii reci, rare
RUNE = (118, 70, 170)         # rune violet stinse
FLOOR = [(46, 41, 58), (52, 47, 64), (58, 52, 70)]
GRASS = (25, 6, 32)


def img(fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (S, S), fill)


def px(im, x, y, c):
    if 0 <= x < S and 0 <= y < S:
        im.putpixel((x, y), c if len(c) == 4 else c + (255,))


def rect(im, x0, y0, x1, y1, c):
    for y in range(max(0, y0), min(S, y1 + 1)):
        for x in range(max(0, x0), min(S, x1 + 1)):
            px(im, x, y, c)


def bricks(im, rnd, y0, y1, row_h=8, brick_w=16, offset=0, x0=0, x1=S - 1):
    """Zidărie: rânduri de blocuri cu rosturi închise, fiecare bloc cu o nuanță și un pic de textură."""
    row = 0
    y = y0
    while y <= y1:
        h = min(row_h, y1 - y + 1)
        off = (offset + (row % 2) * (brick_w // 2)) % brick_w
        x = x0 - off
        while x <= x1:
            shade = rnd.choice(STONE[:3])
            rect(im, max(x0, x), y, min(x1, x + brick_w - 2), y + h - 2, shade)
            # textură: câțiva pixeli mai deschiși/mai închiși
            for _ in range(3):
                tx, ty = rnd.randint(max(x0, x), min(x1, x + brick_w - 2)), rnd.randint(y, y + h - 2)
                px(im, tx, ty, rnd.choice([STONE[0], STONE[3]]))
            # muchia de sus luminată rar (lumina vine de sus-stânga)
            if rnd.random() < 0.35:
                for hx in range(max(x0, x), min(x1, x + brick_w - 2) + 1):
                    if rnd.random() < 0.6:
                        px(im, hx, y, STONE[3])
            x += brick_w
        rect(im, x0, y + h - 1, x1, y + h - 1, MORTAR)
        xx = x0 - off
        while xx <= x1:
            if xx + brick_w - 1 <= x1:
                rect(im, xx + brick_w - 1, y, xx + brick_w - 1, y + h - 1, MORTAR)
            xx += brick_w
        y += row_h
        row += 1


def rune(im, rnd, cx, cy):
    """O rună mică, stinsă (5 x 7)."""
    shapes = [[(0, 0), (0, 1), (0, 2), (0, 3), (0, 4), (1, 1), (2, 2), (1, 3)],
              [(1, 0), (1, 1), (1, 2), (1, 3), (1, 4), (0, 1), (2, 1), (0, 3), (2, 3)],
              [(0, 0), (1, 1), (2, 2), (1, 3), (0, 4), (2, 0), (2, 4)]]
    for dx, dy in rnd.choice(shapes):
        px(im, cx + dx, cy + dy, RUNE)


def crenels(im, rnd, base_y):
    """Creneluri: dinți de piatră pe marginea de sus, cu golurile negre dintre ei."""
    rect(im, 0, 0, S - 1, base_y, GRASS)   # între dinți se vede iarba de afară
    for x0 in (0, 16):
        rect(im, x0 + 1, 2, x0 + 10, base_y, STONE[2])
        rect(im, x0 + 1, 2, x0 + 10, 2, STONE[3])
        rect(im, x0 + 11, 2, x0 + 11, base_y, EDGE)
        rect(im, x0 + 12, base_y - 3, x0 + 15, base_y, STONE[0])
    rect(im, 0, base_y + 1, S - 1, base_y + 1, HILITE)


def wall_tile(rnd, part, side):
    im = img(EDGE + (255,))
    if part == "top":
        bricks(im, rnd, 12, S - 1, offset=side * 8)
        crenels(im, rnd, 11)
    elif part == "mid":
        bricks(im, rnd, 0, S - 1, offset=side * 8)
        if rnd.random() < 0.5:
            rune(im, rnd, rnd.randint(6, 22), rnd.randint(8, 18))
    else:   # bază: blocuri mai mari, umbră spre pământ
        bricks(im, rnd, 0, 23, row_h=12, brick_w=16, offset=side * 8)
        rect(im, 0, 24, S - 1, S - 1, STONE[0])
        rect(im, 0, 24, S - 1, 24, HILITE)
        for x in range(S):
            for y in range(27, S):
                if rnd.random() < 0.5:
                    px(im, x, y, EDGE)
    return im


def tower_tile(rnd, part, side):
    """Turn: ca zidul, dar mai masiv: creneluri mai înalte, fantă de săgeți, un brâu de piatră."""
    im = img(EDGE + (255,))
    if part == "top":
        bricks(im, rnd, 10, S - 1, row_h=8, brick_w=12, offset=side * 6)
        crenels(im, rnd, 9)
        rect(im, 0, 30, S - 1, 31, STONE[3])
    elif part == "mid":
        bricks(im, rnd, 0, S - 1, row_h=8, brick_w=12, offset=side * 6)
        # fanta de săgeți, spre mijlocul turnului (marginea dinspre cealaltă jumătate)
        fx = S - 4 if side == 0 else 2
        rect(im, fx - 1, 8, fx + 1, 22, EDGE)
        rect(im, fx, 9, fx, 21, (60, 30, 90))
    elif part == "low":
        bricks(im, rnd, 0, S - 1, row_h=8, brick_w=12, offset=side * 6)
        rect(im, 0, 14, S - 1, 16, STONE[3])   # brâul
        rect(im, 0, 17, S - 1, 17, EDGE)
        if rnd.random() < 0.6:
            rune(im, rnd, 13, 21)
    else:   # baza turnului de sub poartă
        bricks(im, rnd, 0, 19, row_h=10, brick_w=16, offset=side * 8)
        rect(im, 0, 20, S - 1, S - 1, STONE[0])
        rect(im, 0, 20, S - 1, 20, HILITE)
        for x in range(S):
            for y in range(24, S):
                if rnd.random() < 0.55:
                    px(im, x, y, EDGE)
    return im


def side_wall(rnd, side):
    """Zidul lateral văzut de sus: o coamă de piatră pe verticală, cu margine luminată spre curte."""
    im = img(GRASS + (255,))
    for y in range(0, S, 8):
        for x0 in range(0, S, 16):
            rect(im, x0, y, x0 + 14, y + 6, rnd.choice(STONE[:3]))
            rect(im, x0 + 15, y, x0 + 15, y + 7, MORTAR)
        rect(im, 0, y + 7, S - 1, y + 7, MORTAR)
    if side == 0:   # L: marginea dinspre exterior, întunecată
        rect(im, 0, 0, 2, S - 1, EDGE)
    else:           # R: marginea dinspre curte, cu lumină rece
        rect(im, S - 3, 0, S - 1, S - 1, EDGE)
        rect(im, S - 4, 0, S - 4, S - 1, HILITE)
    return im


def path_tile(rnd):
    """Lespezi mari (16 x 16), rosturi fine; marginile se potrivesc cu orice vecin."""
    im = img(MORTAR + (255,))
    for (x0, y0) in ((0, 0), (16, 0), (0, 16), (16, 16)):
        c = rnd.choice(FLOOR)
        rect(im, x0, y0, x0 + 14, y0 + 14, c)
        for _ in range(10):
            px(im, rnd.randint(x0, x0 + 14), rnd.randint(y0, y0 + 14), rnd.choice(FLOOR + [STONE[2]]))
        rect(im, x0, y0, x0 + 14, y0, tuple(min(255, v + 10) for v in c))
    return im


def grass_tile(rnd):
    im = img(GRASS + (255,))
    for _ in range(26):
        x, y = rnd.randint(0, S - 1), rnd.randint(2, S - 1)
        px(im, x, y, (35, 28, 43)); px(im, x, y - 1, (35, 28, 43))
        if rnd.random() < 0.3:
            px(im, x + 1, y - 2, (48, 36, 58))
    for _ in range(8):
        px(im, rnd.randint(0, S - 1), rnd.randint(0, S - 1), (0, 0, 0))
    return im


def void_tile(rnd):
    im = img((0, 0, 0, 255))
    for _ in range(14):   # ceață care se ridică din gol
        px(im, rnd.randint(0, S - 1), rnd.randint(0, S - 1), (16, 12, 22))
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    tiles = {}
    parts = ["top", "mid", "bot"]
    for i in range(6):   # W1..W6: rândul (i // 2), jumătatea (i % 2)
        tiles[f"W{i + 1}"] = wall_tile(random.Random(100 + i), parts[i // 2], i % 2)
    tparts = ["top", "mid", "low", "base"]
    for prefix, seed in (("R", 200), ("T", 300)):
        for i in range(8):
            tiles[f"{prefix}{i + 1}"] = tower_tile(random.Random(seed + i), tparts[i // 2], i % 2)
    tiles["L"] = side_wall(random.Random(400), 0)
    tiles["R"] = side_wall(random.Random(401), 1)
    tiles["path"] = path_tile(random.Random(500))
    tiles["CastleGrass"] = grass_tile(random.Random(600))
    tiles["Abis"] = void_tile(random.Random(700))
    for name, im in tiles.items():
        im.save(os.path.join(OUT, name + ".png"))
    print(f"{len(tiles)} dale scrise în {OUT}")


if __name__ == "__main__":
    main()
