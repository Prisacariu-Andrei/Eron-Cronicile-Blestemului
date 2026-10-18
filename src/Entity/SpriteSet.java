package Entity;

import Main.Exception.GameExceptionHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Cele 8 cadre ale unui personaj: res/NPC/&lt;Nume&gt;_&lt;down|up|left|right&gt;&lt;1|2&gt;.png (32x32, ca Eron).
 * În npcs.json și story.json apare ca "spriteSet": "/NPC/Malakor". Se încarcă o singură dată per nume.
 */
public final class SpriteSet {
    private static final String[] DIRS = {"down", "up", "left", "right"};
    private static final Map<String, SpriteSet> CACHE = new HashMap<>();
    /** Câte ticks (60/s) ține un pas când personajul merge. */
    public static final int STEP_TICKS = 14;

    private final BufferedImage[][] frames = new BufferedImage[4][2];   // [direcție][pas]

    /** Calea cu care a fost încărcat (ex. "/NPC/Malakor"). */
    public String base;

    private SpriteSet() { }

    /** Setul de la calea dată (ex. "/NPC/Malakor") sau null dacă nu există nici măcar cadrul "jos". */
    public static SpriteSet load(String base) {
        if (base == null || base.isBlank()) return null;
        if (CACHE.containsKey(base)) return CACHE.get(base);
        SpriteSet set = new SpriteSet();
        set.base = base;
        for (int d = 0; d < 4; d++) {
            for (int n = 0; n < 2; n++) set.frames[d][n] = read(base + "_" + DIRS[d] + (n + 1) + ".png");
        }
        if (set.frames[0][0] == null) {
            GameExceptionHandler.getInstance().handleWarning("Setul de sprite-uri lipsește: " + base + "_down1.png", null);
            set = null;
        } else {   // cadrele lipsă se înlocuiesc: celălalt pas, apoi privirea în jos
            for (int d = 0; d < 4; d++) {
                for (int n = 0; n < 2; n++) {
                    if (set.frames[d][n] == null) set.frames[d][n] = set.frames[d][1 - n] != null ? set.frames[d][1 - n] : set.frames[0][0];
                }
            }
        }
        CACHE.put(base, set);
        return set;
    }

    private static BufferedImage read(String path) {
        try (InputStream in = SpriteSet.class.getResourceAsStream(path)) {
            return in == null ? null : ImageIO.read(in);
        } catch (Exception e) {
            return null;
        }
    }

    private static int dirIndex(String dir) {
        for (int i = 0; i < 4; i++) if (DIRS[i].equals(dir)) return i;
        return 0;
    }

    /** Cadrul pentru direcție; când merge, cele două cadre alternează, în repaus rămâne primul. */
    public BufferedImage frame(String dir, boolean moving, int ticks) {
        return frames[dirIndex(dir)][moving ? (ticks / STEP_TICKS) % 2 : 0];
    }

    /** Direcția în care privește cine se mută cu (dx, dy) pe ecran; null dacă nu se mișcă. */
    public static String directionOf(double dx, double dy) {
        if (Math.abs(dx) < 1e-6 && Math.abs(dy) < 1e-6) return null;
        if (Math.abs(dx) >= Math.abs(dy)) return dx > 0 ? "right" : "left";
        return dy > 0 ? "down" : "up";
    }

    /** Direcția dintr-un punct spre altul (cine stă la (fx,fy) și se uită spre (tx,ty)). */
    public static String facing(double fx, double fy, double tx, double ty) {
        String d = directionOf(tx - fx, ty - fy);
        return d == null ? "down" : d;
    }
}
