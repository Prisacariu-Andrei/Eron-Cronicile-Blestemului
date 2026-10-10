package Main;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** Date despre zone din res/Data/zones.json: titluri, imaginile ecranelor de încărcare și ale fundalurilor de luptă, ieșirea de după boss. */
public final class ZoneInfo {
    public record Exit(String map, String zone, int col, int row) {}

    private static Map<String, Object> root;
    private static final Map<String, BufferedImage> images = new HashMap<>();

    private ZoneInfo() {}

    private static synchronized Map<String, Object> root() {
        if (root == null) root = Json.map(Json.parseResource("/Data/zones.json"));
        return root;
    }

    private static Map<String, Object> zone(String name) { return Json.map(Json.map(root().get("zones")).get(name)); }

    public static String title(String zone) { return Json.str(zone(zone).get("title"), zone); }

    public static String subtitle(String zone) { return Json.str(zone(zone).get("subtitle"), ""); }

    /** Poziția zonei pe harta din tasta M (x, y între 0 și 1), sau null. */
    public static double[] mapPos(String zone) {
        java.util.List<Object> l = Json.list(zone(zone).get("map"));
        if (l.size() < 2) return null;
        return new double[]{Json.number(l.get(0), 0), Json.number(l.get(1), 0)};
    }

    /** Flag-ul de poveste care deschide zona ("" = mereu deschisă). */
    public static String requires(String zone) { return Json.str(zone(zone).get("requires"), ""); }

    public static int loadingTicks()
 { return Json.integer(root().get("loadingTicks"), 110); }

    /** Titlul/subtitlul/imaginea unei scene speciale: "prologue", "epilogue" sau "map". */
    public static Map<String, Object> special(String key) { return Json.map(Json.map(root().get("special")).get(key)); }

    /** Unde apare Eron când iese din zona al cărei boss a căzut; null dacă zona nu are ieșire. */
    public static Exit exitOf(String zone) {
        Map<String, Object> e = Json.map(zone(zone).get("exit"));
        if (e.isEmpty()) return null;
        return new Exit(Json.str(e.get("map"), ""), Json.str(e.get("zone"), ""), Json.integer(e.get("col"), 0), Json.integer(e.get("row"), 0));
    }

    /** Imaginea ecranului de încărcare a zonei (scalată la fereastră), sau null dacă lipsește. */
    public static BufferedImage loadingImage(String zone, int w, int h) { return image(Json.str(zone(zone).get("loading"), null), w, h); }

    /** Fundalul luptelor din zonă, sau null. */
    public static BufferedImage battleImage(String zone, int w, int h) { return image(Json.str(zone(zone).get("battle"), null), w, h); }

    /** O imagine din resurse, scalată o singură dată la w x h și ținută în cache (null dacă nu există). */
    public static synchronized BufferedImage image(String path, int w, int h) {
        if (path == null) return null;
        String key = path + "@" + w + "x" + h;
        if (images.containsKey(key)) return images.get(key);
        BufferedImage out = null;
        try (var in = ZoneInfo.class.getResourceAsStream(path)) {
            if (in != null) {
                BufferedImage src = ImageIO.read(in);
                out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = out.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(src, 0, 0, w, h, null);
                g.dispose();
            }
        } catch (Exception e) {
            out = null;
        }
        images.put(key, out);
        return out;
    }
}
