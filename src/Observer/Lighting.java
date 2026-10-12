package Observer;

import Main.GamePanel;
import Main.Json;
import Main.ParticleSystem;
import GameState.PlayState;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Sistemul de lumini al zonelor (reutilizabil), configurat în res/Data/zones.json, în intrarea zonei:
 *   "darkness": 0.85            cât de întunecată e zona (0 = deloc; fără cheie = zona nu folosește sistemul, ex. Tundra are ceața ei)
 *   "lights": [[col, row, rază în dale, "#RRGGBB", "torch" | "crystal" | "steady"], ...]
 *   "fog": 0.10                 ceață joasă care se mișcă încet (opacitatea ei maximă)
 *   "dripTile": 7               id-ul dalei de băltoacă: din ea cad picături
 *
 * Cum e făcut ca să nu coste cadre: întunericul se desenează într-un buffer de 1/4 din rezoluție; fiecare lumină "taie" în el o gaură
 * moale (sprite radial pre-calculat, refolosit pe rază), apoi bufferul se mărește peste ecran cu interpolare (de aici marginile moi).
 * Luminile din afara ecranului nu se ating deloc. Torțele pâlpâie (rază și strălucire) și aruncă scântei din sistemul de particule.
 */
public class Lighting {
    private static final int SCALE = 4;                 // un pixel din buffer = 4x4 pixeli pe ecran
    private static final float PLAYER_RADIUS = 2.1f;    // în dale
    private static final int DARK_RGB = 0x050510;
    private static final int STEPS = 24;                // trepte de strălucire (AlphaComposite-urile se refolosesc)

    private enum Kind { STEADY, TORCH, CRYSTAL }

    private static final class Light {
        float x, y;          // centrul luminii, în pixeli de lume
        float fx, fy;        // unde e flacăra (scântei, strălucire), în pixeli de lume
        float radius;        // în dale
        int rgb;
        Kind kind;
        float phase;
    }

    private static final class FogBlob { float x, y, ampX, speed, phase, w, h, alpha; }

    private final GamePanel gp;
    private final Random rng = new Random();

    private String zone = "";
    private float darkness;
    private float fogAlpha;
    private final List<Light> lights = new ArrayList<>();
    private final List<int[]> puddles = new ArrayList<>();
    private final List<FogBlob> fog = new ArrayList<>();
    private final List<float[]> splashes = new ArrayList<>();   // {tick, x, y}: unde și când pică o picătură

    private BufferedImage buffer;
    private Graphics2D bg;
    private Color darkColor;
    private final Map<Integer, BufferedImage> cutSprites = new HashMap<>();
    private final Map<Long, BufferedImage> glowSprites = new HashMap<>();
    private final AlphaComposite[] cutAlpha = new AlphaComposite[STEPS + 1];
    private final AlphaComposite[] glowAlpha = new AlphaComposite[STEPS + 1];
    private BufferedImage fogSprite;

    public Lighting(GamePanel gp) {
        this.gp = gp;
        for (int i = 0; i <= STEPS; i++) {
            cutAlpha[i] = AlphaComposite.getInstance(AlphaComposite.DST_OUT, i / (float) STEPS);
            glowAlpha[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i / (float) STEPS);
        }
    }

    /** True dacă zona curentă își desenează lumina cu acest sistem (are "darkness" sau "lights" în zones.json). */
    public boolean active() {
        ensureLoaded();
        return darkness > 0 || !lights.isEmpty();
    }

    // =====================================================================================
    // ÎNCĂRCARE
    // =====================================================================================

    private void ensureLoaded() {
        String z = gp.tileM.currentZone;
        if (z == null || z.equals(zone)) return;
        zone = z;
        lights.clear(); puddles.clear(); fog.clear(); splashes.clear();
        darkness = 0; fogAlpha = 0;

        var cfg = Json.map(Json.map(Json.map(Json.parseResource("/Data/zones.json")).get("zones")).get(z));
        darkness = (float) Math.max(0, Math.min(1, Json.number(cfg.get("darkness"), 0)));
        fogAlpha = (float) Json.number(cfg.get("fog"), 0);
        int ts = gp.tileSize;
        for (Object o : Json.list(cfg.get("lights"))) {
            var l = Json.list(o);
            if (l.size() < 4) continue;
            Light light = new Light();
            light.x = (float) ((Json.number(l.get(0), 0) + 0.5) * ts);
            light.radius = (float) Json.number(l.get(2), 2);
            String color = Json.str(l.get(3), "#FFFFFF");
            try { light.rgb = Integer.parseInt(color.replace("#", ""), 16); } catch (NumberFormatException e) { light.rgb = 0xFFFFFF; }
            String kind = l.size() > 4 ? Json.str(l.get(4), "steady") : "steady";
            light.kind = kind.equals("torch") ? Kind.TORCH : kind.equals("crystal") ? Kind.CRYSTAL : Kind.STEADY;
            light.y = (float) ((Json.number(l.get(1), 0) + (light.kind == Kind.TORCH ? 0.45 : 0.5)) * ts);
            light.fx = light.x;
            light.fy = (float) ((Json.number(l.get(1), 0) + 0.3) * ts);
            light.phase = rng.nextFloat() * 6.28f;
            lights.add(light);
        }

        int dripTile = Json.integer(cfg.get("dripTile"), -1);
        var tm = gp.tileM;
        if (dripTile >= 0) {
            for (int c = 0; c < tm.actualMapCols; c++) for (int r = 0; r < tm.actualMapRows; r++)
                if (tm.mapTileNum[c][r] == dripTile) puddles.add(new int[]{c, r});
        }
        if (fogAlpha > 0) buildFog();
        if (darkness > 0) darkColor = new Color(5, 5, 16, Math.round(darkness * 255));
    }

    /** Petice de ceață pe dale libere, la întâmplare dar la fel la fiecare încărcare. */
    private void buildFog() {
        var tm = gp.tileM;
        Random r = new Random(zone.hashCode());
        int ts = gp.tileSize;
        for (int tries = 0; tries < 4000 && fog.size() < 46; tries++) {
            int c = 2 + r.nextInt(Math.max(1, tm.actualMapCols - 4)), row = 2 + r.nextInt(Math.max(1, tm.actualMapRows - 4));
            int id = tm.mapTileNum[c][row];
            if (id < 0 || id >= tm.tile.length || tm.tile[id] == null || tm.tile[id].collision) continue;
            FogBlob b = new FogBlob();
            b.x = (c + 0.5f) * ts; b.y = (row + 0.8f) * ts;
            b.ampX = 30 + r.nextFloat() * 70; b.speed = 0.0015f + r.nextFloat() * 0.003f; b.phase = r.nextFloat() * 6.28f;
            b.w = 3.2f * ts + r.nextFloat() * 2.4f * ts; b.h = b.w * (0.22f + r.nextFloat() * 0.12f);
            b.alpha = 0.5f + r.nextFloat() * 0.5f;
            fog.add(b);
        }
    }

    // =====================================================================================
    // UPDATE (un tick): scântei, picături
    // =====================================================================================

    public void update() {
        if (!(gp.getCurrentState() instanceof PlayState) || gp.player == null) return;
        ensureLoaded();
        if (lights.isEmpty() && puddles.isEmpty()) return;
        int camX = gp.camera.getX(), camY = gp.camera.getY(), ts = gp.tileSize;
        int w = gp.screenWidth, h = gp.screenHeight;
        ParticleSystem p = gp.fx.particles;

        for (Light l : lights) {
            if (l.kind != Kind.TORCH || l.fx < camX - ts || l.fx > camX + w + ts || l.fy < camY - ts || l.fy > camY + h + ts) continue;
            if (rng.nextInt(9) != 0) continue;
            int i = p.spawn(ParticleSystem.WORLD, ParticleSystem.GLOW, l.fx + (rng.nextFloat() - 0.5f) * 8, l.fy,
                    (rng.nextFloat() - 0.5f) * 0.6f, -0.5f - rng.nextFloat() * 0.9f, 0f, 0.985f, 28 + rng.nextInt(34),
                    3.2f, 0.6f, 1f, pickEmber(l.rgb));
            p.setWobble(i, 0.18f, 0.12f);
        }

        int now = gp.fx.ticks;
        for (int[] pd : puddles) {
            float x = (pd[0] + 0.5f) * ts, y = (pd[1] + 0.5f) * ts;
            if (x < camX - ts || x > camX + w + ts || y < camY - ts || y > camY + h + ts) continue;
            if (rng.nextInt(140) != 0) continue;
            float px = x + (rng.nextFloat() - 0.5f) * ts * 0.4f;
            p.spawn(ParticleSystem.WORLD, ParticleSystem.CIRCLE, px, y - ts * 0.9f, 0, 0, 0.22f, 1f, 20, 2.4f, 1.8f, 0.9f, 0x9CC8F0);   // cade
            splashes.add(new float[]{now + 20, px, y});
        }
        for (int i = splashes.size() - 1; i >= 0; i--) {   // ... și face un cerc pe apă
            float[] s = splashes.get(i);
            if (now < s[0]) continue;
            p.spawn(ParticleSystem.WORLD, ParticleSystem.CIRCLE, s[1], s[2], 0, 0, 0, 1f, 22, 2f, 9f, 0.5f, 0xBFD8F2);
            splashes.remove(i);
        }
    }

    private int pickEmber(int rgb) {
        // scânteile urmează culoarea flăcării: portocalii pentru foc, albăstrui pentru flăcările reci
        boolean cold = (rgb & 255) > ((rgb >> 16) & 255);
        int[] warm = {0xFF9A3C, 0xFFC25A, 0xFF6A2B}, ice = {0x9CC8FF, 0xCFE6FF, 0x7FB0FF};
        int[] set = cold ? ice : warm;
        return set[rng.nextInt(set.length)];
    }

    // =====================================================================================
    // DESENARE
    // =====================================================================================

    public void draw(Graphics2D g2) {
        if (!active()) return;
        int w = gp.screenWidth, h = gp.screenHeight, ts = gp.tileSize;
        int camX = gp.camera.getX(), camY = gp.camera.getY();
        float t = gp.fx.ticks;

        if (fogAlpha > 0) drawFog(g2, camX, camY, w, h, t);
        drawFlames(g2, camX, camY, w, h, t);

        int bw = w / SCALE, bh = h / SCALE;
        if (buffer == null) {
            buffer = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
            bg = buffer.createGraphics();
        }
        bg.setComposite(AlphaComposite.Src);
        bg.setColor(darkness > 0 ? darkColor : new Color(0, 0, 0, 0));
        bg.fillRect(0, 0, bw, bh);

        // 1) găurile din întuneric: Eron întâi, apoi luminile de pe ecran
        if (darkness > 0) {
            int px = (gp.camera.screenX(gp.player.worldX) + ts / 2) / SCALE, py = (gp.camera.screenY(gp.player.worldY) + ts / 2) / SCALE;
            cut(px, py, Math.round(PLAYER_RADIUS * ts / SCALE), 1f);
            for (Light l : lights) {
                float f = flicker(l, t);
                int r = Math.round(l.radius * ts * f / SCALE);
                int sx = Math.round((l.x - camX) / SCALE), sy = Math.round((l.y - camY) / SCALE);
                if (sx + r < 0 || sx - r > bw || sy + r < 0 || sy - r > bh) continue;
                cut(sx, sy, r, brightness(l, t));
            }
        }
        // 2) nuanța luminii (portocaliu cald, albastru rece...) peste gaură
        for (Light l : lights) {
            float f = flicker(l, t);
            int r = Math.round(l.radius * ts * f * 0.85f / SCALE);
            int sx = Math.round((l.x - camX) / SCALE), sy = Math.round((l.y - camY) / SCALE);
            if (sx + r < 0 || sx - r > bw || sy + r < 0 || sy - r > bh) continue;
            BufferedImage s = glow(l.rgb, r);
            bg.setComposite(glowAlpha[Math.round(brightness(l, t) * STEPS)]);
            bg.drawImage(s, sx - r, sy - r, null);
        }

        Object oldHint = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(buffer, 0, 0, w, h, null);
        if (oldHint != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
    }

    private void cut(int cx, int cy, int r, float brightness) {
        if (r < 2) return;
        bg.setComposite(cutAlpha[Math.max(0, Math.min(STEPS, Math.round(brightness * STEPS)))]);
        bg.drawImage(cutSprite(r), cx - r, cy - r, null);
    }

    private float flicker(Light l, float t) {
        return switch (l.kind) {
            case TORCH -> 1f + 0.07f * (float) Math.sin(t * 0.23 + l.phase) + 0.05f * (float) Math.sin(t * 0.61 + l.phase * 1.7)
                    + 0.03f * (float) Math.sin(t * 1.37 + l.phase * 0.6);
            case CRYSTAL -> 1f + 0.06f * (float) Math.sin(t * 0.035 + l.phase);
            default -> 1f;
        };
    }

    private float brightness(Light l, float t) {
        return switch (l.kind) {
            case TORCH -> Math.min(1f, 0.9f + 0.1f * (float) Math.sin(t * 0.47 + l.phase) + 0.05f * (float) Math.sin(t * 1.1 + l.phase * 2));
            case CRYSTAL -> 0.85f + 0.15f * (float) Math.sin(t * 0.035 + l.phase + 1);
            default -> 1f;
        };
    }

    /** Gaură moale: alfa 255 în centru, scade spre margine. Se desenează o dată per rază. */
    private BufferedImage cutSprite(int r) {
        return cutSprites.computeIfAbsent(r, k -> {
            BufferedImage img = new BufferedImage(k * 2, k * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setPaint(new RadialGradientPaint(new Point(k, k), k, new float[]{0f, 0.4f, 0.72f, 1f},
                    new Color[]{new Color(255, 255, 255, 255), new Color(255, 255, 255, 215), new Color(255, 255, 255, 80), new Color(255, 255, 255, 0)}));
            g.fillRect(0, 0, k * 2, k * 2);
            g.dispose();
            return img;
        });
    }

    private BufferedImage glow(int rgb, int r) {
        r = Math.max(2, r);
        return glowSprites.computeIfAbsent(((long) rgb << 32) | r, key -> {
            int k = (int) (key & 0xFFFFFFFFL);
            BufferedImage img = new BufferedImage(k * 2, k * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            int cr = (rgb >> 16) & 255, cg = (rgb >> 8) & 255, cb = rgb & 255;
            g.setPaint(new RadialGradientPaint(new Point(k, k), k, new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(cr, cg, cb, 70), new Color(cr, cg, cb, 28), new Color(cr, cg, cb, 0)}));
            g.fillRect(0, 0, k * 2, k * 2);
            g.dispose();
            return img;
        });
    }

    /** Un punct luminos viu în locul flăcării fiecărei torțe (se vede și în întuneric, peste desenul dalei). */
    private void drawFlames(Graphics2D g2, int camX, int camY, int w, int h, float t) {
        int ts = gp.tileSize;
        for (Light l : lights) {
            if (l.kind != Kind.TORCH) continue;
            int sx = Math.round(l.fx - camX), sy = Math.round(l.fy - camY);
            if (sx < -ts || sx > w + ts || sy < -ts || sy > h + ts) continue;
            float f = flicker(l, t);
            int r = Math.round(ts * 0.38f * f);
            g2.setComposite(glowAlpha[Math.round(brightness(l, t) * STEPS)]);
            g2.drawImage(flameGlow(l.rgb, r), sx - r, sy - r, null);
        }
        g2.setComposite(AlphaComposite.SrcOver);
    }

    private BufferedImage flameGlow(int rgb, int r) {
        return glowSprites.computeIfAbsent((((long) rgb << 32) | r) ^ 0x4000000000000000L, key -> {
            BufferedImage img = new BufferedImage(r * 2, r * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setPaint(new RadialGradientPaint(new Point(r, r), r, new float[]{0f, 0.35f, 1f},
                    new Color[]{new Color(255, 244, 200, 230), new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 120), new Color(255, 255, 255, 0)}));
            g.fillRect(0, 0, r * 2, r * 2);
            g.dispose();
            return img;
        });
    }

    /** Ceață joasă: elipse mari și moi care plutesc încet, ancorate în lume (nu pe ecran). */
    private void drawFog(Graphics2D g2, int camX, int camY, int w, int h, float t) {
        if (fogSprite == null) {
            fogSprite = new BufferedImage(128, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = fogSprite.createGraphics();
            g.setPaint(new RadialGradientPaint(new Point(64, 64), 64, new float[]{0f, 1f}, new Color[]{new Color(170, 190, 215, 255), new Color(170, 190, 215, 0)},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE));
            g.setTransform(java.awt.geom.AffineTransform.getScaleInstance(1, 0.25));
            g.fillRect(0, 0, 128, 128);
            g.dispose();
        }
        for (FogBlob b : fog) {
            float x = b.x + (float) Math.sin(t * b.speed + b.phase) * b.ampX;
            int sx = Math.round(x - camX), sy = Math.round(b.y - camY);
            if (sx + b.w < 0 || sx - b.w > w || sy + b.h < 0 || sy - b.h > h) continue;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, fogAlpha * b.alpha)));
            g2.drawImage(fogSprite, Math.round(sx - b.w / 2), Math.round(sy - b.h / 2), Math.round(b.w), Math.round(b.h), null);
        }
        g2.setComposite(AlphaComposite.SrcOver);
    }
}
