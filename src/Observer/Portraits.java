package Observer;

import Entity.SpriteSet;
import Main.GamePanel;
import Main.Json;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Portretele de lângă numele celui care vorbește: capul din cadrul "jos" al personajului, mărit de 4 ori (fără estompare),
 * într-un chenar. Cine are portret: story.json -> "portraits" (nume -> set de cadre), apoi orice NPC de pe hartă cu numele
 * respectiv și un "spriteSet". Naratorul (nume gol) și cei fără sprite nu au portret.
 */
public final class Portraits {
    public static final int SIZE = 56;          // 14 x 14 pixeli de sprite, de 4 ori
    private static final int HEAD = 14;
    private static final Map<String, String> NAMES = new HashMap<>();
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();
    private static boolean loaded = false;

    private Portraits() { }

    private static void loadNames() {
        if (loaded) return;
        loaded = true;
        for (var e : Json.map(Json.map(Json.parseResource("/Data/story.json")).get("portraits")).entrySet()) {
            if (!e.getKey().startsWith("_")) NAMES.put(e.getKey(), Json.str(e.getValue(), ""));
        }
    }

    /** Portretul vorbitorului (SIZE x SIZE) sau null. */
    public static BufferedImage of(GamePanel gp, String speaker) {
        if (speaker == null || speaker.isBlank()) return null;
        loadNames();
        String base = NAMES.get(speaker);
        if (base == null && gp != null) {   // un NPC de pe hartă cu acest nume
            for (var n : gp.npc) if (n != null && speaker.equals(n.name) && n.spriteSet != null) { base = n.spriteSet.base; break; }
        }
        if (base == null || base.isEmpty()) return null;
        if (CACHE.containsKey(base)) return CACHE.get(base);
        BufferedImage frame = null;
        if (base.endsWith(".png")) {   // un singur sprite (ex. Eron)
            try (var in = Portraits.class.getResourceAsStream(base)) { if (in != null) frame = ImageIO.read(in); } catch (Exception ignored) { }
        } else {
            SpriteSet set = SpriteSet.load(base);
            if (set != null) frame = set.frame("down", false, 0);
        }
        BufferedImage p = frame == null ? null : crop(frame);
        CACHE.put(base, p);
        return p;
    }

    /** Capul: un pătrat de HEAD x HEAD pixeli, de la vârful siluetei, centrat pe ea; apoi 4x, nearest-neighbor. */
    private static BufferedImage crop(BufferedImage f) {
        int minX = f.getWidth(), maxX = -1, minY = f.getHeight(), maxY = -1;
        for (int y = 0; y < f.getHeight(); y++) {
            for (int x = 0; x < f.getWidth(); x++) {
                if ((f.getRGB(x, y) >>> 24) > 40) { minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y); }
            }
        }
        if (maxX < 0) return null;
        int cx = (minX + maxX) / 2;
        int sx = Math.max(0, Math.min(f.getWidth() - HEAD, cx - HEAD / 2));
        int sy = Math.max(0, Math.min(f.getHeight() - HEAD, minY - 1));
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(f, 0, 0, SIZE, SIZE, sx, sy, sx + HEAD, sy + HEAD, null);
        g.dispose();
        return out;
    }

    /** Portretul cu chenar auriu, cu colțul stânga-sus la (x, y); dimensiunea totală e SIZE + 12. */
    public static void draw(Graphics2D g2, BufferedImage portrait, int x, int y) {
        int total = SIZE + 12;
        g2.setColor(new Color(14, 12, 24, 235));
        g2.fillRoundRect(x, y, total, total, 10, 10);
        Object old = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(portrait, x + 6, y + 6, SIZE, SIZE, null);
        if (old != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
        Stroke s = g2.getStroke();
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(212, 175, 55));
        g2.drawRoundRect(x, y, total, total, 10, 10);
        g2.setStroke(s);
    }
}
