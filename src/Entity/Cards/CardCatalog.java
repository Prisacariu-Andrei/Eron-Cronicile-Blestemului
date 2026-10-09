package Entity.Cards;

import Entity.Items.Card;
import Entity.StatusEffect;
import Main.Json;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Datele sinergiilor din res/Data/cards.json: etichetele elementelor, reacțiile, bonusul de lanț, cărțile noi și etichetele cărților din cod. */
public final class CardCatalog {
    /** O reacție între un element și un status al țintei. */
    public record Reaction(String id, String name, String element, List<StatusEffect.Type> requires, int damage, int aoeDamage,
                           Map<String, Object> apply, Color color) {}

    private static Map<String, Object> root;
    private static final Map<String, Map<String, Object>> cards = new LinkedHashMap<>();
    private static final List<Reaction> reactions = new ArrayList<>();

    private CardCatalog() {}

    private static synchronized Map<String, Object> root() {
        if (root == null) {
            root = Json.map(Json.parseResource("/Data/cards.json"));
            for (Object o : Json.list(root.get("cards"))) {
                Map<String, Object> d = Json.map(o);
                cards.put(Json.str(d.get("id"), ""), d);
            }
            for (Object o : Json.list(root.get("reactions"))) {
                Map<String, Object> r = Json.map(o);
                List<StatusEffect.Type> req = new ArrayList<>();
                for (Object s : Json.list(r.get("requires"))) {
                    try { req.add(StatusEffect.Type.valueOf(String.valueOf(s))); } catch (IllegalArgumentException ignored) { }
                }
                reactions.add(new Reaction(Json.str(r.get("id"), ""), Json.str(r.get("name"), "Reacție"), Json.str(r.get("element"), ""), req,
                        Json.integer(r.get("damage"), 0), Json.integer(r.get("aoeDamage"), 0), Json.map(r.get("apply")), color(Json.str(r.get("color"), "#FFFFFF"))));
            }
        }
        return root;
    }

    public static Color color(String hex) {
        try { return new Color(Integer.parseInt(hex.replace("#", ""), 16)); } catch (NumberFormatException e) { return Color.WHITE; }
    }

    /** Cărțile noi, definite doar în JSON (id-urile lor). */
    public static List<String> dataIds() { root(); return new ArrayList<>(cards.keySet()); }

    public static Map<String, Object> def(String id) { root(); return cards.get(id); }

    public static List<Reaction> reactions() { root(); return reactions; }

    public static Map<String, Object> chain() { return Json.map(root().get("chain")); }

    public static int rarityWeight(String rarity) { return Json.integer(Json.map(root().get("rarityWeights")).get(rarity), 5); }

    public static int baseCardWeight() { return Json.integer(root().get("baseCardWeight"), 10); }

    public static String tagLabel(String tag) { return Json.str(Json.map(Json.map(root().get("tags")).get(tag)).get("label"), tag); }

    public static Color tagColor(String tag) { return color(Json.str(Json.map(Json.map(root().get("tags")).get(tag)).get("color"), "#FFFFFF")); }

    public static String statusLabel(StatusEffect.Type t) { return Json.str(Json.map(Json.map(root().get("statuses")).get(t.name())).get("label"), t.name()); }

    public static Color statusColor(StatusEffect.Type t) { return color(Json.str(Json.map(Json.map(root().get("statuses")).get(t.name())).get("color"), "#FFFFFF")); }

    /** Pune pe o carte creată în cod etichetele din "overrides" (elemente, ofensivă, statusuri aplicate, raritate). */
    @SuppressWarnings("unchecked")
    public static void decorate(Card card, String id) {
        Map<String, Object> o = Json.map(Json.map(root().get("overrides")).get(id));
        if (o.isEmpty()) return;
        if (o.containsKey("tags")) {
            card.tags.clear();
            for (Object t : Json.list(o.get("tags"))) card.tags.add(String.valueOf(t));
        }
        card.offensive = Json.bool(o.get("offensive"), card.offensive);
        card.rarity = Json.str(o.get("rarity"), card.rarity);
        for (Object a : Json.list(o.get("applies"))) card.applies.add(Json.map(a));
    }
}
