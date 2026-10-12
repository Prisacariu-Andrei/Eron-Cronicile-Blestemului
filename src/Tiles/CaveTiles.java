package Tiles;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Decor desenat procedural (Java2D), pentru Abisul și Bârlogul lui Fenris, acolo unde nu există încă dale desenate de mână.
 * Se desenează o singură dată la încărcarea zonei, "pixel art" pe o grilă de 32x32 (ca restul dalelor), peste dala de podea
 * a zonei, și se pune în tile[] ca orice altă dală, cu coliziunea ei. Dalele mari (tron, grămada de oase) se desenează
 * dintr-o bucată și se taie în dale. O dală deja încărcată pe același id (din PNG, prin setup) nu se atinge.
 */
public final class CaveTiles {
    private CaveTiles() {}

    // Abisul (0 = podea, 1 = zid, 2 = ieșire sunt dalele vechi)
    public static final int TORCH = 3, CRYSTAL_B = 4, CRYSTAL_G = 5, MUSHROOM = 6, PUDDLE = 7, ROCKS = 8, BONES = 9,
            RAIL_H = 10, RAIL_V = 11, CART = 12, SUPPORT = 13, CARPET = 14, GOLD = 15, CHEST = 16, GOBLETS = 17,
            BRAZIER = 18, PILLAR = 19, THRONE = 20 /* 20..28, 3x3 pe rânduri */, GOLD_COINS = 29, CHEST_OPEN = 30;
    // Bârlogul (0..3 podea, 4 copac, 5 apă sunt dalele vechi)
    public static final int DEN_BONES = 6, DEN_BEDDING = 7, DEN_TORCH = 8, DEN_ROCKS = 9, DEN_SKULL = 10, BONE_PILE = 11 /* 11..22, 4x3 pe rânduri */;

    private static final Random RNG = new Random(77);
    private static Graphics2D g;       // desenăm într-o grilă de 32 de unități pe dală, mărită de 2 ori fără netezire
    private static int ts = 64;

    public static void install(Tile[] tile, String zone, int tileSize) {
        ts = tileSize;
        if (zone.equals("Abisul") && tile[0] != null && tile[1] != null) installAbisul(tile);
        else if (zone.equals("Barlogul") && tile[3] != null) installDen(tile);
    }

    // =====================================================================================
    // ABISUL
    // =====================================================================================

    private static void installAbisul(Tile[] t) {
        BufferedImage floor = t[0].image, wall = t[1].image;
        put(t, TORCH, canvas(wall, 1, 1, () -> torch(0, 0)), true);
        put(t, CRYSTAL_B, canvas(floor, 1, 1, () -> crystals(0, 0, 0x2B58B8, 0x4F8FF0, 0xB4DCFF)), true);
        put(t, CRYSTAL_G, canvas(floor, 1, 1, () -> crystals(0, 0, 0x1F8A58, 0x3FD08A, 0xB8FFD6)), true);
        put(t, MUSHROOM, canvas(floor, 1, 1, () -> mushrooms(0, 0)), false);
        put(t, PUDDLE, canvas(floor, 1, 1, () -> puddle(0, 0)), false);
        put(t, ROCKS, canvas(floor, 1, 1, () -> rocks(0, 0)), true);
        put(t, BONES, canvas(floor, 1, 1, () -> bones(0, 0)), false);
        put(t, RAIL_H, canvas(floor, 1, 1, () -> rails(0, 0, false)), false);
        put(t, RAIL_V, canvas(floor, 1, 1, () -> rails(0, 0, true)), false);
        put(t, CART, canvas(floor, 1, 1, () -> { rails(0, 0, false); cart(0, 0); }), true);
        put(t, SUPPORT, canvas(floor, 1, 1, () -> support(0, 0)), true);
        put(t, CARPET, canvas(floor, 1, 1, () -> carpet(0, 0)), false);
        put(t, GOLD, canvas(floor, 1, 1, () -> goldPile(0, 0)), true);
        put(t, CHEST, canvas(floor, 1, 1, () -> chest(0, 0, false)), true);
        put(t, CHEST_OPEN, canvas(floor, 1, 1, () -> chest(0, 0, true)), true);
        put(t, GOBLETS, canvas(floor, 1, 1, () -> goblets(0, 0)), true);
        put(t, BRAZIER, canvas(floor, 1, 1, () -> brazier(0, 0)), true);
        put(t, PILLAR, canvas(floor, 1, 1, () -> pillar(0, 0)), true);
        put(t, GOLD_COINS, canvas(floor, 1, 1, () -> coins(0, 0)), false);
        BufferedImage throne = canvas(floor, 3, 3, () -> throne(0, 0));
        slice(t, THRONE, throne, 3, 3, true);
    }

    private static void installDen(Tile[] t) {
        BufferedImage floor = t[3].image;   // podeaua bârlogului e Dirt3
        put(t, DEN_BONES, canvas(floor, 1, 1, () -> bones(0, 0)), false);
        put(t, DEN_BEDDING, canvas(floor, 1, 1, () -> bedding(0, 0)), false);
        put(t, DEN_TORCH, canvas(floor, 1, 1, () -> denTorch(0, 0)), true);
        put(t, DEN_ROCKS, canvas(floor, 1, 1, () -> rocks(0, 0)), true);
        put(t, DEN_SKULL, canvas(floor, 1, 1, () -> skull(0, 0, 8, 12, 1)), false);
        slice(t, BONE_PILE, canvas(floor, 4, 3, () -> bonePile(0, 0)), 4, 3, true);
    }

    private static void put(Tile[] tile, int id, BufferedImage img, boolean collision) {
        if (tile[id] == null) tile[id] = new Tile();   // un PNG pus de mână (setup) are prioritate
        else return;
        tile[id].image = img;
        tile[id].collision = collision;
    }

    private static void slice(Tile[] tile, int firstId, BufferedImage big, int cols, int rows, boolean collision) {
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            put(tile, firstId + r * cols + c, big.getSubimage(c * ts, r * ts, ts, ts), collision);
        }
    }

    /** O imagine de cols x rows dale: podeaua repetată, apoi desenul (în unități de 1/32 de dală, pe grila de 32). */
    private static BufferedImage canvas(BufferedImage base, int cols, int rows, Runnable art) {
        BufferedImage img = new BufferedImage(cols * ts, rows * ts, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = img.createGraphics();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) gg.drawImage(base, c * ts, r * ts, null);
        gg.scale(ts / 32.0, ts / 32.0);
        g = gg;
        RNG.setSeed(1234 + cols * 31 + rows);
        art.run();
        gg.dispose();
        g = null;
        return img;
    }

    // --- unelte de desen ---

    private static void box(int x, int y, int w, int h, int rgb) { g.setColor(new Color(rgb, true)); g.fillRect(x, y, w, h); }

    private static void oval(int x, int y, int w, int h, int rgb) { g.setColor(new Color(rgb, true)); g.fillOval(x, y, w, h); }

    private static void poly(int rgb, int... xy) {
        int n = xy.length / 2;
        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) { xs[i] = xy[2 * i]; ys[i] = xy[2 * i + 1]; }
        g.setColor(new Color(rgb, true));
        g.fillPolygon(xs, ys, n);
    }

    private static void line(int x1, int y1, int x2, int y2, int w, int rgb) {
        g.setColor(new Color(rgb, true));
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.drawLine(x1, y1, x2, y2);
        g.setStroke(new BasicStroke(1));
    }

    private static int shade(int rgb, double f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * f)), gr = Math.min(255, (int) (((rgb >> 8) & 255) * f)), b = Math.min(255, (int) ((rgb & 255) * f));
        return (r << 16) | (gr << 8) | b;
    }

    private static void shadow(int x, int y, int w, int h) { oval(x, y, w, h, 0x66000000); }

    // --- flăcări ---

    private static void flame(int cx, int baseY, int h, int w, int outer, int mid, int core) {
        oval(cx - w / 2, baseY - h, w, h, outer);
        oval(cx - w / 2 + w / 5, baseY - h * 3 / 4, w * 3 / 5, h * 3 / 4, mid);
        oval(cx - w / 6, baseY - h / 2, Math.max(2, w / 3), h / 2, core);
        poly(outer, cx - 1, baseY - h - 3, cx + 1, baseY - h + 2, cx - 2, baseY - h + 2);
    }

    private static void torch(int ox, int oy) {
        box(14, 14, 4, 13, 0x4A3320);                 // mânerul de lemn
        box(15, 14, 1, 13, 0x6B4A2E);
        box(11, 12, 10, 4, 0x4B4B52);                 // cupa de fier
        box(11, 12, 10, 1, 0x7A7A84);
        box(10, 24, 12, 2, 0x303036);                 // suportul prins în zid
        box(13, 26, 6, 2, 0x303036);
        flame(16, 12, 11, 9, 0xE8651A, 0xFFB830, 0xFFF3A8);
    }

    private static void denTorch(int ox, int oy) {
        shadow(7, 24, 18, 5);
        box(14, 11, 4, 16, 0xCFC8B0);                 // țăruș de os
        box(14, 11, 1, 16, 0xEFE9D4);
        box(12, 25, 8, 3, 0xB5AE96);
        oval(10, 8, 12, 5, 0x9AA3B8);                 // cupa
        flame(16, 9, 11, 8, 0x4A7FE0, 0x9CC8FF, 0xF2FAFF);
    }

    private static void brazier(int ox, int oy) {
        shadow(5, 24, 22, 6);
        box(8, 20, 3, 7, 0x2E2E34); box(21, 20, 3, 7, 0x2E2E34); box(14, 22, 4, 6, 0x2E2E34);
        oval(5, 14, 22, 9, 0x3A3A42);
        oval(7, 14, 18, 6, 0x15151A);
        oval(9, 15, 14, 4, 0xC84A12);                 // jar
        box(5, 17, 22, 1, 0x5A5A64);
        flame(16, 17, 13, 12, 0xE8651A, 0xFFB830, 0xFFF3A8);
    }

    // --- decor de peșteră ---

    private static void crystals(int ox, int oy, int dark, int mid, int light) {
        oval(4, 20, 24, 8, shade(mid, 0.5) | 0x55000000);     // lumina de pe podea
        shadow(7, 23, 20, 5);
        crystal(7, 13, 6, 14, dark, mid, light);
        crystal(19, 15, 6, 11, dark, mid, light);
        crystal(12, 6, 8, 20, dark, mid, light);
        crystal(25, 20, 3, 6, dark, mid, light);
    }

    /** Un cristal: baza jos (x, y+h), vârful sus, o față luminoasă și una întunecată. */
    private static void crystal(int x, int y, int w, int h, int dark, int mid, int light) {
        int c = x + w / 2;
        poly(0xFF000000 | dark, x, y + h, x, y + h / 4, c, y, x + w, y + h / 4, x + w, y + h);
        poly(0xFF000000 | mid, x + 1, y + h, x + 1, y + h / 4 + 1, c, y + 2, c, y + h);
        poly(0xFF000000 | light, x + 1, y + h / 3, c, y + 2, c, y + h / 2);
        box(c, y + h / 3, 1, h / 2, 0xFF000000 | light);
    }

    private static void mushrooms(int ox, int oy) {
        oval(3, 19, 26, 9, 0x4400FFC0);
        shadow(6, 23, 20, 5);
        mushroom(7, 14, 8, 0x1FA89A, 0x7FFFD8);
        mushroom(17, 11, 10, 0x2EB56B, 0xB8FFA8);
        mushroom(14, 20, 6, 0x1FA89A, 0x7FFFD8);
    }

    private static void mushroom(int x, int y, int w, int cap, int spot) {
        box(x + w / 2 - 1, y + 3, 2, w - 1, 0xD8E6D4);
        oval(x, y, w, w / 2 + 2, cap);
        box(x, y + w / 4 + 1, w, 1, shade(cap, 0.6));
        box(x + w / 4, y + 1, 1, 1, spot);
        box(x + w * 2 / 3, y + 2, 1, 1, spot);
    }

    private static void puddle(int ox, int oy) {
        oval(3, 8, 26, 17, 0xFF3C4A5C);
        oval(5, 10, 22, 13, 0xFF1E2A38);
        oval(7, 11, 10, 4, 0xFF3A5068);
        box(20, 15, 3, 1, 0xFF6A8CAA);
        box(10, 19, 4, 1, 0xFF4A6684);
    }

    private static void rocks(int ox, int oy) {
        shadow(3, 20, 26, 8);
        boulder(4, 12, 15, 13, 0x6A6C76);
        boulder(16, 8, 12, 12, 0x7A7C86);
        boulder(14, 19, 9, 7, 0x5C5E68);
    }

    private static void boulder(int x, int y, int w, int h, int rgb) {
        oval(x, y, w, h, shade(rgb, 0.65));
        oval(x, y, w - 2, h - 2, rgb);
        oval(x + 2, y + 1, w / 2, h / 3, shade(rgb, 1.25));
        box(x + w / 3, y + h / 2, w / 3, 1, shade(rgb, 0.55));
    }

    private static void bones(int ox, int oy) {
        shadow(4, 22, 24, 5);
        bone(5, 22, 20, 8, 0xE2DAC2);
        bone(7, 9, 18, 21, 0xD2CAB2);
        skull(18, 12, 9, 12, 1);
        box(4, 25, 3, 1, 0xB8B09A);
    }

    private static void bone(int x1, int y1, int x2, int y2, int rgb) {
        line(x1, y1, x2, y2, 2, rgb);
        box(x1 - 1, y1 - 1, 3, 3, rgb);
        box(x2 - 1, y2 - 1, 3, 3, rgb);
    }

    /** Craniu: (x,y) = colț stâng-sus, w x h; dir = 1 privește în față. */
    private static void skull(int x, int y, int w, int h, int dir) {
        shadow(x - 1, y + h - 3, w + 2, 4);
        oval(x, y, w, h - 3, 0xE8E0CA);
        box(x + 2, y + h - 5, w - 4, 4, 0xDAD2BA);
        box(x + 1, y + (h - 3) / 2 - 1, 2, 2, 0xFF1A1418);
        box(x + w - 3, y + (h - 3) / 2 - 1, 2, 2, 0xFF1A1418);
        box(x + w / 2, y + h - 6, 1, 2, 0xFF3A3028);
    }

    private static void bedding(int ox, int oy) {
        box(0, 0, 32, 32, 0x33B8A27A);
        for (int i = 0; i < 26; i++) {
            int x = RNG.nextInt(30), y = RNG.nextInt(30);
            line(x, y, x + 2 + RNG.nextInt(4), y + RNG.nextInt(3) - 1, 1, RNG.nextBoolean() ? 0xFFC9B58A : 0xFFE8DCC0);
        }
        for (int i = 0; i < 6; i++) box(RNG.nextInt(28), RNG.nextInt(28), 2, 1, 0xFFF4F0E4);   // smocuri de blană albă
    }

    private static void rails(int ox, int oy, boolean vertical) {
        // dalele se desenează orizontal; pentru vertical rotim sistemul de coordonate
        java.awt.geom.AffineTransform old = g.getTransform();
        if (vertical) { g.translate(32, 0); g.rotate(Math.PI / 2); }
        for (int x = 2; x < 32; x += 14) box(x, 7, 4, 18, 0x5F4226);           // traverse
        for (int x = 2; x < 32; x += 14) box(x, 7, 1, 18, 0x7E5A36);
        box(0, 9, 32, 2, 0x8C8C96); box(0, 11, 32, 1, 0x4A4A52);               // șinele
        box(0, 21, 32, 2, 0x8C8C96); box(0, 23, 32, 1, 0x4A4A52);
        g.setTransform(old);
    }

    private static void cart(int ox, int oy) {
        shadow(4, 24, 25, 5);
        oval(6, 22, 6, 6, 0xFF222226); oval(20, 22, 6, 6, 0xFF222226);
        box(5, 9, 22, 15, 0xFF4B4F58);
        box(5, 9, 22, 3, 0xFF787D88);
        box(7, 13, 18, 9, 0xFF6B4A2A);
        for (int x = 9; x < 25; x += 5) box(x, 13, 1, 9, 0xFF4A3220);
        box(5, 21, 22, 2, 0xFF30343C);
        oval(7, 5, 18, 8, 0xFF8E8E98);                 // minereu în vagonet
        oval(10, 4, 8, 5, 0xFFB0B0BA);
        box(19, 6, 3, 2, 0xFFF0C04A);
    }

    private static void support(int ox, int oy) {
        shadow(5, 24, 24, 5);
        box(9, 22, 14, 6, 0xFF55575F);                 // piatra de la bază
        box(11, 4, 10, 20, 0xFF6B4A2A);                // stâlpul
        box(11, 4, 2, 20, 0xFF8A6238); box(19, 4, 2, 20, 0xFF4F361E);
        for (int y = 8; y < 22; y += 5) box(13, y, 5, 1, 0xFF4A3220);
        box(3, 2, 26, 6, 0xFF7C5A34);                  // grinda de sus
        box(3, 2, 26, 1, 0xFF9A7444); box(3, 7, 26, 1, 0xFF4F361E);
        box(7, 8, 3, 4, 0xFF5A3F22); box(22, 8, 3, 4, 0xFF5A3F22);
    }

    // --- sala tronului ---

    private static void carpet(int ox, int oy) {
        box(0, 4, 32, 24, 0xFF7A1F2B);
        box(0, 4, 32, 2, 0xFFD8A83A); box(0, 26, 32, 2, 0xFFD8A83A);
        box(0, 6, 32, 1, 0xFF4A0F18); box(0, 25, 32, 1, 0xFF4A0F18);
        for (int x = 4; x < 32; x += 12) {
            poly(0xFFD8A83A, x + 4, 11, x + 8, 16, x + 4, 21, x, 16);
            poly(0xFF7A1F2B, x + 4, 13, x + 6, 16, x + 4, 19, x + 2, 16);
        }
    }

    private static void goldPile(int ox, int oy) {
        shadow(2, 21, 28, 8);
        oval(3, 12, 26, 15, 0xFFC48A12);
        oval(6, 7, 20, 14, 0xFFE0A81E);
        oval(10, 3, 12, 11, 0xFFF2C83C);
        for (int i = 0; i < 16; i++) {
            int x = 5 + RNG.nextInt(21), y = 6 + RNG.nextInt(18);
            box(x, y, 2, 1, RNG.nextInt(3) == 0 ? 0xFFFFF4A8 : 0xFFA86E0A);
        }
        box(13, 4, 3, 1, 0xFFFFF8C0);
        box(9, 18, 2, 2, 0xFFD23C46); box(21, 14, 2, 2, 0xFF3CD2C0);   // două pietre scumpe
    }

    private static void coins(int ox, int oy) {
        for (int i = 0; i < 9; i++) {
            int x = 3 + RNG.nextInt(24), y = 5 + RNG.nextInt(21);
            oval(x, y, 4, 3, 0xFFE0A81E);
            box(x + 1, y, 2, 1, 0xFFFFE680);
        }
    }

    private static void chest(int ox, int oy, boolean open) {
        shadow(3, 23, 26, 6);
        box(5, 14, 22, 12, 0xFF7A4A22);
        box(5, 14, 22, 2, 0xFF9A6234);
        for (int x : new int[]{5, 15, 25}) box(x, 14, 2, 12, 0xFF3A3A44);
        box(5, 24, 22, 2, 0xFF3A3A44);
        if (open) {
            box(5, 6, 22, 7, 0xFF5A3418);
            oval(6, 8, 20, 9, 0xFFE0A81E);
            oval(9, 6, 14, 6, 0xFFF2C83C);
            box(13, 7, 3, 1, 0xFFFFF4A8);
        } else {
            box(5, 8, 22, 7, 0xFF8E5A2A);
            box(5, 8, 22, 1, 0xFFB0763C);
            for (int x : new int[]{5, 15, 25}) box(x, 8, 2, 7, 0xFF3A3A44);
            box(14, 14, 4, 5, 0xFFF2C83C);
            box(15, 16, 2, 2, 0xFF5A3A08);
        }
    }

    private static void goblets(int ox, int oy) {
        shadow(4, 22, 24, 6);
        goblet(6, 12);
        goblet(18, 15);
        oval(14, 22, 3, 3, 0xFFD23C46);
        box(1, 25, 2, 1, 0xFFE0A81E);
    }

    private static void goblet(int x, int y) {
        poly(0xFFE0A81E, x, y, x + 8, y, x + 6, y + 6, x + 2, y + 6);
        box(x + 1, y, 6, 1, 0xFFFFF4A8);
        box(x + 3, y + 6, 2, 5, 0xFFC48A12);
        box(x + 1, y + 11, 6, 2, 0xFFE0A81E);
        box(x + 2, y + 1, 4, 1, 0xFF8A1E2E);                  // vin
    }

    private static void pillar(int ox, int oy) {
        shadow(4, 24, 24, 6);
        box(7, 23, 18, 5, 0xFF565864);
        box(9, 4, 14, 20, 0xFF6C6E7A);
        box(9, 4, 3, 20, 0xFF8A8C98); box(20, 4, 3, 20, 0xFF4A4C56);
        for (int y = 9; y < 22; y += 6) box(9, y, 14, 1, 0xFF55576A);
        box(6, 1, 20, 4, 0xFF80828E);
        box(6, 1, 20, 1, 0xFFA0A2AE);
        box(14, 12, 4, 3, 0xFFD8A83A);                         // inel de aur
    }

    /** Tronul de piatră: 3x3 dale (96x96 unități), orientat cu fața în jos, spre cel care intră. */
    private static void throne(int ox, int oy) {
        shadow(8, 78, 80, 14);
        box(10, 80, 76, 10, 0xFF4A4C58); box(10, 80, 76, 2, 0xFF70727E);       // treapta de jos
        box(16, 72, 64, 9, 0xFF565864); box(16, 72, 64, 2, 0xFF7C7E8A);        // treapta a doua
        // spătarul: piatră crăpată, cu vârf zimțat
        poly(0xFF5B5D68, 26, 72, 26, 14, 34, 6, 40, 14, 48, 2, 56, 14, 62, 6, 70, 14, 70, 72);
        poly(0xFF6E7080, 30, 72, 30, 18, 36, 12, 42, 18, 48, 8, 54, 18, 60, 12, 66, 18, 66, 72);
        box(26, 14, 3, 58, 0xFF7C7E8C); box(67, 14, 3, 58, 0xFF42444E);
        line(40, 22, 44, 36, 1, 0xFF3E404A); line(44, 36, 40, 50, 1, 0xFF3E404A);
        line(58, 28, 54, 42, 1, 0xFF3E404A);
        // brațele
        box(14, 46, 14, 28, 0xFF565864); box(14, 46, 14, 3, 0xFF8A8C98); box(14, 46, 3, 28, 0xFF6C6E7A);
        box(68, 46, 14, 28, 0xFF565864); box(68, 46, 14, 3, 0xFF8A8C98); box(79, 46, 3, 28, 0xFF42444E);
        // șezutul și perna
        box(28, 54, 40, 20, 0xFF4E505C); box(28, 54, 40, 2, 0xFF7C7E8A);
        box(32, 50, 32, 12, 0xFF8A1E2E); box(32, 50, 32, 2, 0xFFB23042); box(32, 60, 32, 2, 0xFF5A0F1C);
        box(32, 55, 32, 1, 0xFFD8A83A);
        // ornamente: coroană de aur și cristale pe spătar
        poly(0xFFE0A81E, 36, 30, 36, 20, 42, 25, 48, 16, 54, 25, 60, 20, 60, 30);
        box(36, 28, 24, 3, 0xFFFFD84A);
        oval(46, 21, 4, 4, 0xFFD23C46);
        crystal(30, 10, 5, 12, 0x2B58B8, 0x4F8FF0, 0xB4DCFF);
        crystal(62, 10, 5, 12, 0x2B58B8, 0x4F8FF0, 0xB4DCFF);
        // monede și pietre prețioase pe trepte
        for (int i = 0; i < 20; i++) {
            int x = 12 + RNG.nextInt(70), y = 74 + RNG.nextInt(14);
            box(x, y, 2, 1, RNG.nextInt(3) == 0 ? 0xFFFFF4A8 : 0xFFE0A81E);
        }
    }

    // --- bârlogul lui Fenris ---

    /** Grămada uriașă de oase: 4x3 dale (128x96 unități): un deal zimțat de oase cu coaste, cranii de lup și un craniu enorm deasupra. */
    private static void bonePile(int ox, int oy) {
        shadow(0, 70, 128, 24);
        int n = 24;
        int[] xs = new int[n + 3], ys = new int[n + 3];
        int[] top = new int[130];                                   // înălțimea dealului pe fiecare coloană (pentru oase)
        xs[0] = 4; ys[0] = 86;
        for (int k = 0; k <= n; k++) {
            double t = k / (double) n;
            xs[k + 1] = 4 + (int) (120 * t);
            ys[k + 1] = 86 - (int) (Math.sin(Math.PI * t) * (50 + RNG.nextInt(9)));
        }
        xs[n + 2] = 124; ys[n + 2] = 86;
        for (int x = 4; x <= 124; x++) {
            int k = Math.min(n - 1, (x - 4) * n / 120);
            double f = ((x - 4) * n / 120.0) - k;
            top[x] = (int) (ys[k + 1] * (1 - f) + ys[k + 2] * f);
        }
        poly(0xFF6E6652, interleave(xs, ys));
        for (int i = 0; i < xs.length; i++) ys[i] = Math.min(86, ys[i] + 6);        // al doilea strat, mai deschis, puțin mai jos: dă volum dealului
        poly(0xFF9A927C, interleave(xs, ys));
        for (int i = 0; i < 190; i++) {
            int x = 10 + RNG.nextInt(108), y = top[x] + 3 + RNG.nextInt(Math.max(2, 82 - top[x]));
            int len = 8 + RNG.nextInt(9), ang = RNG.nextInt(180);
            int x2 = x + (int) (Math.cos(Math.toRadians(ang)) * len), y2 = y + (int) (Math.sin(Math.toRadians(ang)) * len * 0.5);
            int rgb = RNG.nextInt(4) == 0 ? 0xFFB5AD96 : (RNG.nextBoolean() ? 0xFFEFE8D2 : 0xFFD8D0B8);
            line(x, y, x2, y2, 4, 0xFF5A5342);                       // contur închis, ca oasele să se deslușească pe deal
            bone(x, y, x2, y2, rgb);
        }
        g.setStroke(new BasicStroke(5)); g.setColor(new Color(0xFF5A5342));
        for (int i = 0; i < 3; i++) g.drawArc(14 + i * 34, 14 + (i % 2) * 6, 34, 44, 15, 150);
        g.setStroke(new BasicStroke(3)); g.setColor(new Color(0xFFF2ECD8));
        for (int i = 0; i < 3; i++) g.drawArc(14 + i * 34, 14 + (i % 2) * 6, 34, 44, 15, 150);
        g.setStroke(new BasicStroke(1));
        skull(8, 66, 14, 16, 1); skull(106, 68, 14, 16, 1); skull(34, 72, 11, 13, 1); skull(84, 74, 11, 13, 1);
        // craniul uriaș, în vârf
        shadow(36, 28, 56, 8);
        poly(0xFFEDE5CE, 44, 14, 42, 2, 54, 10); poly(0xFFEDE5CE, 84, 14, 86, 2, 74, 10);        // urechi scurte
        oval(40, 6, 48, 30, 0xFFEDE5CE);
        box(48, 28, 32, 11, 0xFFDCD4BC);
        oval(48, 14, 12, 11, 0xFF14101A); oval(68, 14, 12, 11, 0xFF14101A);
        oval(52, 17, 4, 4, 0xFF7FB6FF); oval(72, 17, 4, 4, 0xFF7FB6FF);                          // luminițe reci în orbite
        poly(0xFF14101A, 62, 22, 66, 22, 64, 28);
        for (int x = 50; x < 80; x += 5) box(x, 32, 3, 6, 0xFFF6F0DC);                           // colți
    }

    private static int[] interleave(int[] xs, int[] ys) {
        int[] out = new int[xs.length * 2];
        for (int i = 0; i < xs.length; i++) { out[2 * i] = xs[i]; out[2 * i + 1] = ys[i]; }
        return out;
    }
}
