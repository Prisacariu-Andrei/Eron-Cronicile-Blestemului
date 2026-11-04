package Entity.Inamici;

import Main.Json;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Datele din res/Data/enemies.json: scalarea grupurilor, inamicii din date, aparițiile pe zone. */
public final class Groups {
    private static Map<String, Object> root;
    private static final Map<String, Map<String, Object>> types = new LinkedHashMap<>();

    private Groups() {}

    private static synchronized Map<String, Object> root() {
        if (root == null) {
            root = Json.map(Json.parseResource("/Data/enemies.json"));
            for (Object o : Json.list(root.get("types"))) {
                Map<String, Object> d = Json.map(o);
                types.put(Json.str(d.get("id"), ""), d);
            }
        }
        return root;
    }

    private static double pick(String key, int size, double def) {
        List<Object> l = Json.list(Json.map(root().get("groups")).get(key));
        if (l.isEmpty()) return def;
        return Json.number(l.get(Math.max(0, Math.min(size - 1, l.size() - 1))), def);
    }

    /** Cât de tare lovesc inamicii când sunt `size` în luptă (1.0 singur, mai puțin în grup). */
    public static double damageScale(int size) { return pick("damage", size, 1.0); }

    public static double hpScale(int size) { return pick("hp", size, 1.0); }

    public static int maxSize() { return Json.integer(Json.map(root().get("groups")).get("maxSize"), 4); }

    public static Map<String, Object> type(String id) {
        root();
        return types.get(id);
    }

    /** Cifrele unui boss din enemies.json -> bosses ("fenris", "feldgug"). */
    public static Map<String, Object> boss(String id) { return Json.map(Json.map(root().get("bosses")).get(id)); }

    /** Specificațiile de apariție ale zonei: o listă sau o singură specificație (obiect). */
    public static List<Object> spawns(String zone) {
        Object v = Json.map(root().get("spawns")).get(zone);
        return v instanceof Map ? List.of(v) : Json.list(v);
    }

    /** Regula de așezare a zonei (enemies.json -> spawnRules): tiles + spacing; goală dacă zona nu are. */
    public static Map<String, Object> spawnRule(String zone) { return Json.map(Json.map(root().get("spawnRules")).get(zone)); }

    public static List<Object> safe(String zone) { return Json.list(Json.map(root().get("safe")).get(zone)); }
}
