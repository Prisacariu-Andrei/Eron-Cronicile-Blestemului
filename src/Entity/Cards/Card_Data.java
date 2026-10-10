package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;
import Entity.StatusEffect;
import Main.Json;

import java.util.List;
import java.util.Map;

/**
 * O carte definită doar în res/Data/cards.json (fără clasă proprie): efecte de bază + un combo opțional cu condiție
 * (element jucat mai devreme în tură, status pe țintă, aliat în echipă). Textul de pe carte se generează din efecte,
 * deci include automat bonusurile din rune și relicve.
 */
public class Card_Data extends Card {
    private final List<Object> effects, comboEffects;
    private final Map<String, Object> when;
    private final String comboText;
    private final Player owner;

    public Card_Data(Map<String, Object> def, Player player) {
        super(Json.str(def.get("name"), "?"), Json.integer(def.get("cost"), 1), typeOf(def), "");
        tags.clear();
        for (Object t : Json.list(def.get("tags"))) tags.add(String.valueOf(t));
        rarity = Json.str(def.get("rarity"), "Comună");
        offensive = Json.bool(def.get("offensive"), false);
        effects = Json.list(def.get("effects"));
        Map<String, Object> combo = Json.map(def.get("combo"));
        when = Json.map(combo.get("when"));
        comboText = Json.str(combo.get("text"), null);
        comboEffects = Json.list(combo.get("effects"));
        owner = player;
        description = describe(player);
    }

    private static CardType typeOf(Map<String, Object> def) {
        for (Object t : Json.list(def.get("tags"))) {
            switch (String.valueOf(t)) {
                case "FOC": return CardType.FOC;
                case "GHEATA": return CardType.INGHET;
                case "FULGER": return CardType.ELECTRIC;
                case "UMBRA": return CardType.UMBRA;
                default: break;
            }
        }
        return CardType.NEUTRU;
    }

    /** Textul combo-ului, cu bonusul din runa Umbrei (comboDamage) dacă o porți. */
    @Override public String comboHint() {
        int bonus = owner == null ? 0 : owner.runeSum("comboDamage");
        return comboText == null || bonus == 0 ? comboText : comboText + " (+" + bonus + " rună)";
    }

    @Override
    public boolean comboMet(Player player, Enemy target) {
        return !when.isEmpty() && Combos.conditionMet(player, target, when);
    }

    @Override
    public void play(Player player, Enemy enemy) {
        boolean combo = comboMet(player, enemy);   // se calculează înainte de efecte, ca să nu conteze ce pune cartea însăși
        run(player, enemy, effects);
        if (combo && !comboEffects.isEmpty()) {
            Combos.announce(player, "Combo: " + name + "!", new java.awt.Color(255, 215, 90), enemy);
            run(player, enemy, comboEffects);
            int bonus = player.runeSum("comboDamage");   // Runa Umbrei
            if (bonus > 0 && enemy != null && enemy.alive) enemy.takeDamage(player, bonus);
        }
    }

    private void run(Player p, Enemy target, List<Object> list) {
        for (Object o : list) {
            Map<String, Object> e = Json.map(o);
            int v = Json.integer(e.get("value"), 0);
            switch (Json.str(e.get("type"), "")) {
                case "damage" -> { if (target != null) target.takeDamage(p, v + p.cardDamage(this)); }
                case "damageAll" -> { for (Enemy x : p.livingEnemies()) x.takeDamage(p, v + p.cardDamage(this)); }
                case "damagePerTag" -> {
                    int n = Math.min(4, p.combo.distinctTags());
                    if (target != null) target.takeDamage(p, Json.integer(e.get("base"), 0) + v * n + p.cardDamage(this));
                }
                case "status" -> {
                    StatusEffect.Type type;
                    try { type = StatusEffect.Type.valueOf(Json.str(e.get("status"), "")); } catch (IllegalArgumentException ex) { continue; }
                    int turns = Json.integer(e.get("turns"), 2) + p.statusTurns(type);
                    int power = dotType(type) ? v + p.statusPower(type) : v;   // doar Arsura și Otrava cresc în putere; la restul "value" e procent
                    if (Json.bool(e.get("all"), false)) { for (Enemy x : p.livingEnemies()) x.applyStatus(type, turns, power); }
                    else if (target != null) target.applyStatus(type, turns, power);
                }
                case "shield" -> p.addShield(v + p.runeSum("cardShield"));
                case "heal" -> { p.life = Math.min(p.maxLife, p.life + v); p.notifyObservers(); }
                case "shieldAllies" -> { for (var a : p.gp.party.members) if (a.alive()) a.shield += v; }
                case "allyAction" -> p.gp.party.actAll(p, target);
                default -> { }
            }
        }
    }

    private static boolean dotType(StatusEffect.Type t) { return t == StatusEffect.Type.BURN || t == StatusEffect.Type.POISON; }

    @Override
    public String getDynamicDescription(Player player) { return describe(player); }

    /** Textul cărții, din efecte: ex. "Paguba 8\nAplică Înghețat (3 ture)". */
    private String describe(Player p) {
        StringBuilder sb = new StringBuilder();
        for (Object o : effects) {
            Map<String, Object> e = Json.map(o);
            int v = Json.integer(e.get("value"), 0);
            String line = switch (Json.str(e.get("type"), "")) {
                case "damage" -> "Paguba " + (v + p.cardDamage(this));
                case "damageAll" -> "Paguba " + (v + p.cardDamage(this)) + " tuturor";
                case "damagePerTag" -> "Paguba " + (Json.integer(e.get("base"), 0) + p.cardDamage(this)) + " +" + v + " per element jucat";
                case "status" -> {
                    String label = "?";
                    int turns = Json.integer(e.get("turns"), 2), power = v;
                    try {
                        var type = StatusEffect.Type.valueOf(Json.str(e.get("status"), ""));
                        label = CardCatalog.statusLabel(type);
                        turns += p.statusTurns(type);
                        if (dotType(type)) power += p.statusPower(type); else power = 0;
                    } catch (IllegalArgumentException ignored) { }
                    yield "Aplică " + label + (power > 0 ? " " + power : "") + " (" + turns + " ture)" + (Json.bool(e.get("all"), false) ? " tuturor" : "");
                }
                case "shield" -> "Armura " + (v + p.runeSum("cardShield"));
                case "heal" -> "Vindecă " + v;
                case "shieldAllies" -> "Aliații: armură " + v;
                case "allyAction" -> "Aliatul acționează";
                default -> null;
            };
            if (line == null) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
        }
        return sb.toString();
    }
}
