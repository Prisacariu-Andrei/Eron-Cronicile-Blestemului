package Main;

import java.util.Map;

/** Date despre zone din res/Data/zones.json: titlul din ecranul de tranziție și ieșirea de după boss. */
public final class ZoneInfo {
    public record Exit(String map, String zone, int col, int row) {}

    private static Map<String, Object> root;

    private ZoneInfo() {}

    private static synchronized Map<String, Object> root() {
        if (root == null) root = Json.map(Json.parseResource("/Data/zones.json"));
        return root;
    }

    private static Map<String, Object> zone(String name) { return Json.map(Json.map(root().get("zones")).get(name)); }

    public static String title(String zone) { return Json.str(zone(zone).get("title"), zone); }

    public static String subtitle(String zone) { return Json.str(zone(zone).get("subtitle"), ""); }

    public static int loadingTicks() { return Json.integer(root().get("loadingTicks"), 80); }

    /** Unde apare Eron când iese din zona al cărei boss a căzut; null dacă zona nu are ieșire. */
    public static Exit exitOf(String zone) {
        Map<String, Object> e = Json.map(zone(zone).get("exit"));
        if (e.isEmpty()) return null;
        return new Exit(Json.str(e.get("map"), ""), Json.str(e.get("zone"), ""), Json.integer(e.get("col"), 0), Json.integer(e.get("row"), 0));
    }
}
