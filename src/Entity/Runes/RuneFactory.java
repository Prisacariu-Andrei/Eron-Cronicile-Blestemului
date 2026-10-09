package Entity.Runes;

import Entity.Items.Rune;
import Main.Exception.GameExceptionHandler;
import Main.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Catalogul de rune, citit din res/Data/runes.json (nu există nicio rună scrisă în cod). */
public class RuneFactory {
    private static final Map<String, Map<String, Object>> defs = new LinkedHashMap<>();
    private static final Random rng = new Random();
    private static int slots = 3, bagSize = 12;
    private static boolean loaded = false;

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try {
            Map<String, Object> root = Json.map(Json.parseResource("/Data/runes.json"));
            slots = Json.integer(root.get("slots"), 3);
            bagSize = Json.integer(root.get("bagSize"), 12);
            for (Object o : Json.list(root.get("runes"))) {
                Map<String, Object> d = Json.map(o);
                String id = Json.str(d.get("id"), null);
                if (id != null) defs.put(id, d);
            }
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Nu s-a putut citi res/Data/runes.json", e);
        }
    }

    public static int slots() { load(); return slots; }
    public static int bagSize() { load(); return bagSize; }
    public static List<String> ids() { load(); return new ArrayList<>(defs.keySet()); }
    public static boolean exists(String id) { load(); return defs.containsKey(id); }

    /** Creează runa după id (ex. "vapaie"); null dacă id-ul nu există. */
    public static Rune createRune(String id) {
        load();
        Map<String, Object> d = defs.get(id);
        if (d == null) {
            GameExceptionHandler.getInstance().handleWarning("Runa '" + id + "' nu este definită în runes.json.", null);
            return null;
        }
        Map<String, Integer> buffs = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : Json.map(d.get("buffs")).entrySet()) buffs.put(e.getKey(), Json.integer(e.getValue(), 0));
        String hex = Json.str(d.get("color"), "#FFFFFF").replace("#", "");
        int color;
        try { color = Integer.parseInt(hex, 16); } catch (NumberFormatException e) { color = 0xFFFFFF; }
        return new Rune(id, Json.str(d.get("name"), id), Json.str(d.get("description"), ""), Json.str(d.get("glyph"), "flame"),
                color, Json.str(d.get("rarity"), "Comună"), Json.integer(d.get("price"), 100), buffs);
    }

    /** Aruncă zarul pentru inamicul învins (numele dinaintea virgulei). Returnează id-ul unei rune sau null. */
    public static String rollDrop(String enemyName, java.util.function.Predicate<String> alreadyOwned) {
        load();
        for (Map.Entry<String, Map<String, Object>> e : defs.entrySet()) {
            if (alreadyOwned.test(e.getKey())) continue;
            double chance = Json.number(Json.map(e.getValue().get("drops")).get(enemyName), 0);
            if (chance > 0 && rng.nextDouble() < chance) return e.getKey();
        }
        return null;
    }
}
