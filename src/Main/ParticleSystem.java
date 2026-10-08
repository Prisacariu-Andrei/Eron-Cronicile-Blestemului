package Main;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Particule într-un pool cu dimensiune fixă (tablouri de primitive): nu se creează obiecte la fiecare particulă,
 * deci nu se adună gunoi, iar numărul maxim de particule e limitat (nu poate crește la nesfârșit).
 *
 * Trei straturi:
 *   WORLD   - în coordonatele lumii, se mișcă odată cu harta (praf sub picioare, scântei de vindecare)
 *   AMBIENT - pe ecran, DESENAT SUB interfață (zăpadă, licurici); are paralaxă față de cameră
 *   FRONT   - pe ecran, desenat peste tot (scântei de luptă, efecte din cutscene)
 */
public class ParticleSystem {
    public static final int WORLD = 0, AMBIENT = 1, FRONT = 2;
    public static final int SQUARE = 0, CIRCLE = 1, GLOW = 2, STREAK = 3;

    public static final int MAX = 700;

    private final float[] x = new float[MAX], y = new float[MAX];
    private final float[] vx = new float[MAX], vy = new float[MAX];
    private final float[] gravity = new float[MAX], drag = new float[MAX];
    private final float[] size0 = new float[MAX], size1 = new float[MAX];
    private final float[] alpha = new float[MAX];
    private final float[] depth = new float[MAX];          // paralaxă pentru AMBIENT (0 = nu se mișcă cu camera, 1 = ca lumea)
    private final float[] wobble = new float[MAX], wobbleSpeed = new float[MAX];
    private final int[] age = new int[MAX], life = new int[MAX];
    private final int[] rgb = new int[MAX];
    private final byte[] layer = new byte[MAX], shape = new byte[MAX];
    private int count;

    // Culorile se refolosesc, ca să nu alocăm un obiect Color pentru fiecare particulă la fiecare cadru
    private final Map<Integer, Color> colors = new HashMap<>();

    /** Adaugă o particulă. Returnează indexul ei (pentru setDepth/setWobble) sau -1 dacă poolul e plin. */
    public synchronized int spawn(int layer, int shape, float x, float y, float vx, float vy,
                                  float gravity, float drag, int life, float size0, float size1, float alpha, int rgb) {
        if (count >= MAX) return -1;
        int i = count++;
        this.x[i] = x; this.y[i] = y; this.vx[i] = vx; this.vy[i] = vy;
        this.gravity[i] = gravity; this.drag[i] = drag;
        this.life[i] = Math.max(1, life); this.age[i] = 0;
        this.size0[i] = size0; this.size1[i] = size1; this.alpha[i] = alpha;
        this.rgb[i] = rgb;
        this.layer[i] = (byte) layer; this.shape[i] = (byte) shape;
        this.depth[i] = 0; this.wobble[i] = 0; this.wobbleSpeed[i] = 0;
        return i;
    }

    public synchronized void setDepth(int i, float d) { if (i >= 0 && i < count) depth[i] = d; }

    public synchronized void setWobble(int i, float amplitude, float speed) {
        if (i >= 0 && i < count) { wobble[i] = amplitude; wobbleSpeed[i] = speed; }
    }

    public synchronized int countLayer(int l) {
        int n = 0;
        for (int i = 0; i < count; i++) if (layer[i] == l) n++;
        return n;
    }

    public synchronized int size() { return count; }

    public synchronized void clear() { count = 0; }

    public synchronized void clearLayer(int l) {
        for (int i = 0; i < count; i++) if (layer[i] == l) { remove(i); i--; }
    }

    /** camDx/camDy = cât s-a mișcat camera în acest tick (pentru paralaxa straturilor AMBIENT). */
    public synchronized void update(float camDx, float camDy) {
        for (int i = 0; i < count; i++) {
            age[i]++;
            if (age[i] >= life[i]) { remove(i); i--; continue; }

            vx[i] *= drag[i];
            vy[i] = vy[i] * drag[i] + gravity[i];
            x[i] += vx[i];
            y[i] += vy[i];
            if (wobble[i] != 0) x[i] += (float) Math.sin((age[i] + i * 7) * wobbleSpeed[i]) * wobble[i];
            if (layer[i] == AMBIENT) { x[i] -= camDx * depth[i]; y[i] -= camDy * depth[i]; }
        }
    }

    // Mută ultima particulă în locul celei moarte (ordinea nu contează)
    private void remove(int i) {
        int last = --count;
        if (i == last) return;
        x[i] = x[last]; y[i] = y[last]; vx[i] = vx[last]; vy[i] = vy[last];
        gravity[i] = gravity[last]; drag[i] = drag[last];
        size0[i] = size0[last]; size1[i] = size1[last]; alpha[i] = alpha[last];
        depth[i] = depth[last]; wobble[i] = wobble[last]; wobbleSpeed[i] = wobbleSpeed[last];
        age[i] = age[last]; life[i] = life[last]; rgb[i] = rgb[last];
        layer[i] = layer[last]; shape[i] = shape[last];
    }

    private final BasicStroke[] strokes = new BasicStroke[8];

    private BasicStroke stroke(float width) {
        int k = Math.min(7, Math.max(1, Math.round(width)));
        if (strokes[k] == null) strokes[k] = new BasicStroke(k);
        return strokes[k];
    }

    private Color color(int rgb, int a) {
        int key = (rgb & 0xFFFFFF) | (a << 24);
        Color c = colors.get(key);
        if (c == null) {
            if (colors.size() > 4000) colors.clear();
            c = new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a);
            colors.put(key, c);
        }
        return c;
    }

    /** Desenează particulele unui strat. camX/camY se folosesc doar pentru WORLD. */
    public synchronized void draw(Graphics2D g, int which, int camX, int camY, int screenW, int screenH) {
        Stroke oldStroke = g.getStroke();
        for (int i = 0; i < count; i++) {
            if (layer[i] != which) continue;

            float t = (float) age[i] / life[i];
            // apare repede (primele 15%), apoi se stinge treptat
            float fade = t < 0.15f ? t / 0.15f : 1f - (t - 0.15f) / 0.85f;
            int a = (int) (alpha[i] * fade * 255);
            if (a <= 0) continue;
            if (a > 255) a = 255;

            float s = size0[i] + (size1[i] - size0[i]) * t;
            float sx = which == WORLD ? x[i] - camX : x[i];
            float sy = which == WORLD ? y[i] - camY : y[i];
            if (sx < -40 || sy < -40 || sx > screenW + 40 || sy > screenH + 40) continue;

            switch (shape[i]) {
                case SQUARE -> {
                    g.setColor(color(rgb[i], a));
                    g.fillRect(Math.round(sx - s / 2), Math.round(sy - s / 2), Math.max(1, Math.round(s)), Math.max(1, Math.round(s)));
                }
                case CIRCLE -> {
                    g.setColor(color(rgb[i], a));
                    g.fillOval(Math.round(sx - s / 2), Math.round(sy - s / 2), Math.max(1, Math.round(s)), Math.max(1, Math.round(s)));
                }
                case GLOW -> { // un halou larg și slab, cu un miez mai luminos
                    g.setColor(color(rgb[i], a / 4));
                    int big = Math.max(2, Math.round(s * 2.6f));
                    g.fillOval(Math.round(sx - big / 2f), Math.round(sy - big / 2f), big, big);
                    g.setColor(color(rgb[i], a));
                    int core = Math.max(1, Math.round(s));
                    g.fillOval(Math.round(sx - core / 2f), Math.round(sy - core / 2f), core, core);
                }
                case STREAK -> {
                    g.setColor(color(rgb[i], a));
                    g.setStroke(stroke(s));
                    g.drawLine(Math.round(sx), Math.round(sy), Math.round(sx - vx[i] * 2.2f), Math.round(sy - vy[i] * 2.2f));
                }
            }
        }
        g.setStroke(oldStroke);
    }
}
