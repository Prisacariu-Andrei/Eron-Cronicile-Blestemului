"""Sprite-uri 32x32 pentru NPC-uri, în stilul lui Eron: contur negru, umbră spre dreapta-jos,
4 direcții x 2 cadre de mers.  Rulare: python3 sprites.py <folder_iesire>"""
import sys, os
from PIL import Image

W = H = 32
T = (0, 0, 0, 0)
OUTLINE = (16, 12, 20, 255)


def hx(s, a=255):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (c[3],)


class Canvas:
    def __init__(self):
        self.p = [[T] * W for _ in range(H)]

    def px(self, x, y, c):
        if 0 <= x < W and 0 <= y < H and c is not None:
            self.p[y][x] = c

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, c)

    def row(self, y, x0, x1, c):
        self.rect(x0, y, x1, y, c)

    def get(self, x, y):
        return self.p[y][x] if 0 <= x < W and 0 <= y < H else T

    def mirror(self):
        out = Canvas()
        for y in range(H):
            out.p[y] = list(reversed(self.p[y]))
        return out

    def finish(self):
        """umbră dreapta/jos + contur negru"""
        src = [r[:] for r in self.p]
        for y in range(H):
            for x in range(W):
                c = src[y][x]
                if c[3] == 0 or c == OUTLINE:
                    continue
                k = 1.0
                if x + 1 < W and src[y][x + 1][3] == 0:
                    k *= 0.72
                elif y + 1 < H and src[y + 1][x][3] == 0:
                    k *= 0.82
                if k != 1.0:
                    self.p[y][x] = shade(c, k)
        out = [r[:] for r in self.p]
        for y in range(H):
            for x in range(W):
                if src[y][x][3] != 0:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < W and 0 <= yy < H and src[yy][xx][3] != 0:
                        out[y][x] = OUTLINE
                        break
        self.p = out
        return self

    def image(self):
        im = Image.new('RGBA', (W, H))
        im.putdata([c for r in self.p for c in r])
        return im


# ------------------------------------------------------------------ desen pe părți
# Direcțiile se desenează pentru "down", "up" și "left"; "right" e oglinda lui "left".

def draw_char(cfg, d, f):
    """d in down/up/left, f in 0/1 (cadrul de mers)"""
    cv = Canvas()
    skin = hx(cfg.get('skin', 'E8BC98'))
    hair = hx(cfg['hair']) if cfg.get('hair') else None
    tunic = hx(cfg['tunic'])
    trim = hx(cfg['trim']) if cfg.get('trim') else None
    pants = hx(cfg.get('pants', '4A3A30'))
    boots = hx(cfg.get('boots', '3A2A22'))
    belt = hx(cfg['belt']) if cfg.get('belt') else None
    cape = hx(cfg['cape']) if cfg.get('cape') else None
    sleeve = hx(cfg['sleeve']) if cfg.get('sleeve') else tunic
    fat = cfg.get('fat', False)
    robe = cfg.get('robe', False)            # haină lungă până la glezne
    stride = 1 if f == 0 else -1             # alternează piciorul din față
    bx0, bx1 = (9, 22) if fat else (10, 21)  # lățimea trunchiului (față/spate)

    # --- pelerina în spate (se vede în lateral și din față pe margini)
    if cape and cfg.get('cape_long', True):
        if d == 'up':
            pass  # desenată peste trunchi mai jos
        elif d == 'down':
            cv.rect(bx0 - 1, 15, bx0 - 1, 28, shade(cape, 0.8))
            cv.rect(bx1 + 1, 15, bx1 + 1, 28, shade(cape, 0.8))
        else:
            cv.rect(17, 15, 21, 29, shade(cape, 0.85))
            cv.rect(20, 25, 22, 29, shade(cape, 0.75))

    # --- picioare
    if robe:
        hem = hx(cfg.get('robe_color', cfg['tunic']))
        if d in ('down', 'up'):
            cv.rect(bx0, 24, bx1, 28, hem)
            cv.row(29, bx0 + 1, bx1 - 1, hem)
            # vârfurile ghetelor se văd alternativ
            if d == 'down':
                if f == 0:
                    cv.rect(12, 29, 14, 29, boots)
                else:
                    cv.rect(17, 29, 19, 29, boots)
        else:
            cv.rect(12, 24, 19, 28, hem)
            cv.row(29, 12 + (1 if f else 0), 18 + (1 if f else 0), hem)
            cv.rect(11 if f == 0 else 13, 29, 13 if f == 0 else 15, 29, boots)
    else:
        if d in ('down', 'up'):
            ly = 0 if f == 0 else -1   # piciorul stâng ridicat în cadrul 2
            ry = -1 if f == 0 else 0
            cv.rect(11, 25, 14, 27 + ly, pants)
            cv.rect(11, 28 + ly, 14, 29 + ly, boots)
            cv.rect(17, 25, 20, 27 + ry, pants)
            cv.rect(17, 28 + ry, 20, 29 + ry, boots)
        else:
            if f == 0:   # pas lung
                cv.rect(10, 25, 12, 27, pants); cv.rect(9, 28, 12, 29, boots)     # față
                cv.rect(17, 25, 19, 27, shade(pants, 0.85)); cv.rect(17, 28, 20, 29, shade(boots, 0.85))
            else:        # picioare apropiate
                cv.rect(13, 25, 15, 27, pants); cv.rect(12, 28, 15, 29, boots)
                cv.rect(15, 25, 17, 27, shade(pants, 0.85)); cv.rect(15, 28, 18, 29, shade(boots, 0.85))

    # --- trunchi
    if d in ('down', 'up'):
        cv.rect(bx0, 15, bx1, 24, tunic)
        cv.row(15, bx0 + 1, bx1 - 1, tunic)
        if belt:
            cv.row(22, bx0, bx1, belt)
            if d == 'down' and cfg.get('buckle'):
                cv.rect(15, 22, 16, 22, hx(cfg['buckle']))
        if d == 'down' and trim:
            cv.rect(15, 15, 16, 21, trim)       # tivul de pe piept
        if d == 'down' and cfg.get('apron'):
            cv.rect(12, 18, 19, 26 if not robe else 28, hx(cfg['apron']))
        if d == 'down' and cfg.get('embroidery'):   # ie românească
            e = hx(cfg['embroidery'])
            for x in range(bx0 + 1, bx1, 2):
                cv.px(x, 17, e)
            cv.rect(15, 16, 16, 19, e)
    else:
        cv.rect(12, 15, 19, 24, tunic)
        if belt:
            cv.row(22, 12, 19, belt)
        if cfg.get('apron'):
            cv.rect(11, 18, 13, 26, hx(cfg['apron']))
        if cfg.get('embroidery'):
            cv.rect(12, 17, 13, 17, hx(cfg['embroidery']))

    # --- pelerina peste spate
    if cape and d == 'up':
        cv.rect(bx0 - 1, 15, bx1 + 1, 28 if cfg.get('cape_long', True) else 23, cape)
        cv.row(15, bx0, bx1, shade(cape, 1.1))
    if cape and d == 'down' and cfg.get('clasp'):
        cv.rect(bx0, 15, bx0 + 1, 16, cape); cv.rect(bx1 - 1, 15, bx1, 16, cape)
        cv.rect(15, 15, 16, 16, hx(cfg['clasp']))

    # --- brațe
    hand = skin if not cfg.get('gloves') else hx(cfg['gloves'])
    if d in ('down', 'up'):
        la = 0 if f == 0 else 1
        ra = 1 if f == 0 else 0
        cv.rect(bx0 - 2, 16, bx0 - 1, 21 + la, sleeve); cv.rect(bx0 - 2, 22 + la, bx0 - 1, 22 + la, hand)
        cv.rect(bx1 + 1, 16, bx1 + 2, 21 + ra, sleeve); cv.rect(bx1 + 1, 22 + ra, bx1 + 2, 22 + ra, hand)
    else:
        ax = 13 if f == 0 else 15
        cv.rect(ax, 16, ax + 2, 21, shade(sleeve, 1.05)); cv.rect(ax, 22, ax + 2, 22, hand)

    # --- cap
    if d in ('down', 'up'):
        cv.row(5, 12, 19, skin); cv.row(6, 11, 20, skin)
        cv.rect(10, 7, 21, 13, skin); cv.row(14, 11, 20, skin)
    else:
        cv.row(5, 12, 18, skin); cv.rect(11, 6, 19, 13, skin); cv.row(14, 12, 18, skin)
        cv.px(10, 10, skin); cv.px(10, 11, skin)    # nasul

    # păr
    hs = cfg.get('hair_style', 'short')
    if hair:
        if d == 'down':
            if hs in ('short', 'long', 'bun', 'braid'):
                cv.rect(10, 4, 21, 7, hair); cv.row(3, 12, 19, hair)
                cv.rect(10, 8, 10, 10, hair); cv.rect(21, 8, 21, 10, hair)
                if hs == 'long':
                    cv.rect(9, 8, 10, 17, hair); cv.rect(21, 8, 22, 17, hair)
                if hs == 'braid':
                    cv.rect(21, 8, 22, 20, hair); cv.px(22, 21, hx('C04040'))
                if hs == 'bun':
                    cv.rect(13, 1, 18, 3, hair)
            elif hs == 'bald':
                cv.rect(10, 8, 10, 11, hair); cv.rect(21, 8, 21, 11, hair)
                cv.rect(10, 7, 11, 7, hair); cv.rect(20, 7, 21, 7, hair)
            elif hs == 'temples':   # păr închis, tâmple cărunte
                cv.rect(10, 4, 21, 6, hair); cv.row(3, 12, 19, hair)
                cv.rect(10, 7, 10, 10, hx(cfg.get('temple', 'A8A8B4'))); cv.rect(21, 7, 21, 10, hx(cfg.get('temple', 'A8A8B4')))
        elif d == 'up':
            cv.rect(10, 3, 21, 14, hair); cv.row(2, 12, 19, hair)
            if hs == 'bald':
                cv.rect(11, 3, 20, 9, skin)
            if hs == 'long':
                cv.rect(10, 14, 21, 18, hair)
            if hs == 'braid':
                cv.rect(15, 14, 16, 21, hair); cv.px(15, 22, hx('C04040'))
            if hs == 'bun':
                cv.rect(13, 0, 18, 2, hair)
            if hs == 'temples':
                cv.rect(10, 10, 21, 13, hx(cfg.get('temple', 'A8A8B4')))
        else:
            if hs == 'bald':
                cv.rect(15, 7, 19, 12, hair)
            else:
                cv.rect(12, 3, 19, 6, hair); cv.rect(14, 7, 19, 12, hair); cv.rect(18, 13, 19, 13, hair)
                if hs == 'long':
                    cv.rect(16, 13, 20, 18, hair)
                if hs == 'braid':
                    cv.rect(19, 13, 20, 20, hair)
                if hs == 'bun':
                    cv.rect(17, 1, 20, 4, hair)
                if hs == 'temples':
                    cv.rect(14, 8, 15, 11, hx(cfg.get('temple', 'A8A8B4')))
            cv.px(15, 10, shade(skin, 0.8))   # urechea

    # ochi + barbă
    eye = hx('1A1420')
    if d == 'down':
        cv.rect(13, 10, 13, 11, eye); cv.rect(18, 10, 18, 11, eye)
        if cfg.get('brows'):
            b = hx(cfg['brows']); cv.row(9, 12, 13, b); cv.row(9, 18, 19, b)
    elif d == 'left':
        cv.rect(12, 10, 12, 11, eye)
    beard = cfg.get('beard')
    if beard and d != 'up':
        bc = hx(cfg['beard_color'])
        if d == 'down':
            if beard == 'short':
                cv.rect(11, 12, 20, 14, bc); cv.rect(14, 12, 17, 12, skin); cv.rect(12, 15, 19, 15, bc)
            elif beard == 'long':
                cv.rect(11, 12, 20, 14, bc); cv.rect(14, 12, 17, 12, skin)
                cv.rect(12, 15, 19, 19, bc); cv.rect(13, 20, 18, 21, bc); cv.rect(14, 22, 17, 22, bc)
            elif beard == 'moustache':
                cv.rect(12, 12, 19, 12, bc); cv.px(11, 13, bc); cv.px(20, 13, bc)
        else:
            if beard == 'short':
                cv.rect(11, 12, 15, 14, bc); cv.rect(12, 15, 14, 15, bc)
            elif beard == 'long':
                cv.rect(11, 12, 15, 14, bc); cv.rect(11, 15, 15, 20, bc); cv.rect(12, 21, 14, 22, bc)
            elif beard == 'moustache':
                cv.rect(10, 12, 13, 12, bc)

    # --- acoperământ
    hw = cfg.get('head')
    hc = hx(cfg['head_color']) if cfg.get('head_color') else None
    if hw == 'hood':
        if d == 'down':
            cv.rect(9, 3, 22, 7, hc); cv.row(2, 11, 20, hc); cv.rect(9, 8, 10, 15, hc); cv.rect(21, 8, 22, 15, hc)
        elif d == 'up':
            cv.rect(9, 2, 22, 16, hc)
        else:
            cv.rect(11, 2, 20, 6, hc); cv.rect(14, 7, 21, 15, hc); cv.px(10, 4, hc)
    elif hw == 'cap':
        if d == 'down':
            cv.rect(10, 2, 21, 5, hc); cv.row(6, 9, 22, shade(hc, 0.8))
        elif d == 'up':
            cv.rect(10, 1, 21, 5, hc)
        else:
            cv.rect(12, 2, 19, 5, hc); cv.row(6, 9, 19, shade(hc, 0.8))
    elif hw == 'feather_hat':
        if d == 'down':
            cv.rect(10, 2, 21, 5, hc); cv.row(6, 8, 23, shade(hc, 0.8)); cv.rect(20, 0, 21, 3, hx(cfg['feather']))
        elif d == 'up':
            cv.rect(10, 1, 21, 5, hc); cv.row(6, 8, 23, shade(hc, 0.8))
        else:
            cv.rect(12, 2, 19, 5, hc); cv.row(6, 9, 21, shade(hc, 0.8)); cv.rect(18, 0, 20, 2, hx(cfg['feather']))
    elif hw == 'straw':
        if d in ('down', 'up'):
            cv.rect(11, 2, 20, 5, hc); cv.row(6, 7, 24, shade(hc, 0.85)); cv.row(5, 11, 20, hx('8A3A2A'))
        else:
            cv.rect(12, 2, 19, 5, hc); cv.row(6, 8, 22, shade(hc, 0.85)); cv.row(5, 12, 19, hx('8A3A2A'))
    elif hw == 'scarf':   # basma
        if d == 'down':
            cv.rect(9, 3, 22, 7, hc); cv.row(2, 11, 20, hc); cv.rect(9, 8, 10, 13, hc); cv.rect(21, 8, 22, 13, hc)
        elif d == 'up':
            cv.rect(9, 2, 22, 13, hc); cv.rect(13, 14, 18, 16, hc)
        else:
            cv.rect(11, 2, 20, 7, hc); cv.rect(15, 8, 21, 13, hc); cv.rect(19, 14, 21, 16, hc)
    elif hw == 'cusma':   # căciulă de oaie
        if d in ('down', 'up'):
            cv.rect(10, 0, 21, 6, hc); cv.row(6, 9, 22, shade(hc, 0.75))
        else:
            cv.rect(11, 0, 20, 6, hc); cv.row(6, 10, 20, shade(hc, 0.75))
    elif hw == 'helmet':
        steel = hc
        if d == 'down':
            cv.rect(10, 2, 21, 7, steel); cv.row(1, 12, 19, steel); cv.rect(10, 8, 11, 12, steel); cv.rect(20, 8, 21, 12, steel)
            cv.rect(15, 8, 16, 11, shade(steel, 0.8))   # nazala
        elif d == 'up':
            cv.rect(10, 1, 21, 13, steel)
        else:
            cv.rect(11, 1, 20, 7, steel); cv.rect(15, 8, 20, 13, steel); cv.rect(10, 7, 11, 8, steel)
        if cfg.get('plume'):
            p = hx(cfg['plume'])
            if d == 'left':
                cv.rect(15, 0, 21, 0, p); cv.rect(20, 1, 22, 2, p)
            else:
                cv.rect(14, 0, 17, 0, p)
    elif hw == 'crown':
        g = hc
        if d in ('down', 'up'):
            cv.row(3, 11, 20, g); cv.px(11, 2, g); cv.px(15, 1, g); cv.px(16, 1, g); cv.px(20, 2, g)
            cv.px(13, 2, g); cv.px(18, 2, g)
            if d == 'down':
                cv.px(15, 3, hx('C03050')); cv.px(16, 3, hx('C03050'))
        else:
            cv.row(3, 12, 19, g); cv.px(12, 2, g); cv.px(15, 1, g); cv.px(18, 2, g)
    elif hw == 'circlet':
        if d in ('down', 'up'):
            cv.row(5, 10, 21, hc)
        else:
            cv.row(5, 11, 19, hc)

    # --- obiect în mână
    item = cfg.get('item')
    ic = hx(cfg['item_color']) if cfg.get('item_color') else hx('6A4A2A')
    if item and d != 'up':
        if d == 'down':
            hxp = bx1 + 2
            if item == 'staff':
                cv.rect(hxp + 1, 6, hxp + 1, 29, ic); cv.rect(hxp, 4, hxp + 2, 6, hx(cfg.get('gem', 'F0B040')))
            elif item == 'spear':
                cv.rect(hxp + 1, 3, hxp + 1, 29, ic); cv.rect(hxp, 0, hxp + 2, 2, hx('C8CCD8')); cv.px(hxp + 1, 0, hx('E8ECF4'))
            elif item == 'cane':
                cv.rect(hxp + 1, 20, hxp + 1, 29, ic); cv.rect(hxp, 20, hxp, 20, ic)
            elif item == 'scroll':
                cv.rect(hxp - 1, 19, hxp + 2, 23, hx('EDE0C0')); cv.row(19, hxp - 1, hxp + 2, hx('B02020'))
            elif item == 'club':
                cv.rect(hxp + 1, 17, hxp + 2, 24, ic)
            elif item == 'bread':
                cv.rect(hxp - 1, 21, hxp + 2, 23, hx('C88A40'))
        else:
            if item == 'staff':
                cv.rect(10, 6, 10, 29, ic); cv.rect(9, 4, 11, 6, hx(cfg.get('gem', 'F0B040')))
            elif item == 'spear':
                cv.rect(10, 3, 10, 29, ic); cv.rect(9, 0, 11, 2, hx('C8CCD8'))
            elif item == 'cane':
                cv.rect(11, 21, 11, 29, ic)
            elif item == 'scroll':
                cv.rect(11, 19, 14, 23, hx('EDE0C0'))
            elif item == 'club':
                cv.rect(12, 17, 13, 24, ic)
            elif item == 'bread':
                cv.rect(11, 21, 14, 23, hx('C88A40'))
    if cfg.get('bow_back') and d != 'down':
        bc = hx('7A5230')
        if d == 'up':
            for i in range(12):
                cv.px(11 + i, 15 + i // 2 if i < 6 else 26 - (i - 6) // 2 - 3, bc)
            cv.rect(11, 16, 11, 26, hx('D8D0B8'))
        else:
            cv.rect(20, 14, 20, 26, bc); cv.px(21, 13, bc); cv.px(21, 27, bc)

    return cv


def frames_for(cfg):
    out = {}
    for d in ('down', 'up', 'left'):
        for f in (0, 1):
            cv = draw_char(cfg, d, f)
            if cfg.get('child'):
                im = cv.finish().image()
                small = im.resize((24, 24), Image.NEAREST)
                base = Image.new('RGBA', (32, 32))
                base.alpha_composite(small, (4, 7))
                out[(d, f)] = base
            else:
                out[(d, f)] = cv.finish().image()
    for f in (0, 1):
        out[('right', f)] = out[('left', f)].transpose(Image.FLIP_LEFT_RIGHT)
    return out


CHARS = {
    # Regele, așa cum îl vede poporul: fără coroană, mantie cenușie, tunică violet (indiciu), agrafă de aur
    'Malakor': dict(hair='4A3E48', hair_style='temples', temple='B8B8C4', beard='short', beard_color='5A4C56',
                    brows='3A2E38', tunic='4E3478', trim='C8A040', sleeve='8C909E', cape='A9AEBC', clasp='E0B040',
                    pants='3A2C4A', boots='2A2030', belt='2E2238', buckle='E0B040', robe=True, robe_color='4E3478'),
    # Regele pe tron (camera tronului): coroană de aur, mantie violet închis
    'Malakor_Rege': dict(hair='4A3E48', hair_style='temples', temple='B8B8C4', beard='short', beard_color='5A4C56',
                         brows='3A2E38', tunic='2E1E48', trim='E0B040', sleeve='3A2858', cape='5A2A78', clasp='E0B040',
                         head='crown', head_color='E8C048', robe=True, robe_color='2E1E48', boots='1E1626', gloves='1E1626'),
    'Cronicar': dict(skin='E0B898', hair='E8E8EC', hair_style='bald', beard='long', beard_color='F0F0F4',
                     tunic='6A4A30', trim='B89058', belt='3A2A1A', robe=True, robe_color='5E4028', item='staff', gem='F0B040'),
    'Vlaicu': dict(skin='D8A880', hair='3A2A20', tunic='3A4A80', trim='C0C4D0', sleeve='8890A0', belt='3A2A1A', buckle='C0C4D0',
                   pants='5A6070', boots='2A2A30', head='helmet', head_color='9098A8', plume='B02828', beard='moustache',
                   beard_color='3A2A20', item='spear', gloves='5A6070'),
    'GardaDeFier': dict(skin='D8A880', hair='3A2A20', tunic='4A4E60', trim='9098A8', sleeve='8890A0', belt='2A2A30',
                        pants='5A6070', boots='2A2A30', head='helmet', head_color='8088A0', item='spear', gloves='5A6070'),
    'Crainic': dict(hair='6A4428', tunic='B03030', trim='E0B040', sleeve='8A2020', belt='E0B040', pants='3A2A2A',
                    boots='2A1E1E', head='feather_hat', head_color='7A2020', feather='F0E0A0', item='scroll'),
    'Mira': dict(skin='F0C8A8', hair='9A4A28', hair_style='braid', tunic='E090A8', sleeve='F4ECE0', apron='F4ECE0',
                 robe=True, robe_color='D07890', boots='5A3A2A'),
    'Elda': dict(skin='DCB49A', hair='C8C8D0', hair_style='short', tunic='3E5A40', sleeve='6A4A30', head='scarf',
                 head_color='8A2A2A', robe=True, robe_color='2E4430', item='cane'),
    'Radu': dict(skin='D8A880', hair='5A3A20', tunic='4A6A9A', trim='7A5230', sleeve='4A6A9A', belt='5A3A20', buckle='C0A040',
                 pants='4A3A30', head='cap', head_color='3A4A6A', beard='short', beard_color='5A3A20', item='club'),
    'Vasile': dict(skin='E8B898', hair='4A3020', tunic='D4A020', sleeve='F0E8D8', apron='C8B898', fat=True, belt='5A3A20',
                   pants='5A4030', head='cap', head_color='6A4A2A', beard='moustache', beard_color='3A2418'),
    'Ilinca': dict(skin='EAC0A0', hair='6A3A20', hair_style='long', tunic='5A4028', sleeve='3E6A40', belt='3A2A1A',
                   pants='4A3A2A', boots='3A2A1A', head='hood', head_color='3E7A40', bow_back=True),
    'Copil': dict(skin='F0C8A8', hair='8A5A30', tunic='E8DCC0', embroidery='C03030', belt='8A3A2A', pants='6A5040', child=True),
    'Satean': dict(skin='D8A880', hair='3A2A20', tunic='F0ECE0', embroidery='C03030', sleeve='F0ECE0', belt='8A2A2A',
                   pants='F0ECE0', boots='3A2A22', head='straw', head_color='D8B860', beard='moustache', beard_color='3A2A20',
                   item='bread'),
    'Sateanca': dict(skin='EAC0A0', hair='3A2418', tunic='F0ECE0', embroidery='C03030', sleeve='F0ECE0', robe=True,
                     robe_color='2A2028', head='scarf', head_color='B02828', apron='8A2A2A'),
    'Jean': dict(skin='D8A880', hair='2A1E18', tunic='EDE6D4', embroidery='B03030', sleeve='EDE6D4', belt='6A3A20',
                 buckle='C0A040', pants='EDE6D4', boots='3A2A1E', head='cusma', head_color='2A2420', beard='short',
                 beard_color='2A1E18', item='spear'),
}


def main(outdir):
    os.makedirs(outdir, exist_ok=True)
    sheet = Image.new('RGBA', (8 * 34 * 3, len(CHARS) * 34 * 3), (70, 74, 64, 255))
    for r, (name, cfg) in enumerate(CHARS.items()):
        fr = frames_for(cfg)
        for c, (d, f) in enumerate([(d, f) for d in ('down', 'up', 'left', 'right') for f in (0, 1)]):
            im = fr[(d, f)]
            im.save(os.path.join(outdir, f'{name}_{d}{f + 1}.png'))
            sheet.alpha_composite(im.resize((96, 96), Image.NEAREST), (c * 102 + 3, r * 102 + 3))
    sheet.save(os.path.join(outdir, '_preview.png'))


if __name__ == '__main__':
    main(sys.argv[1])
