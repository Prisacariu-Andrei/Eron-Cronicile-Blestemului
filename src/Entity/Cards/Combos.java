package Entity.Cards;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Player.Player;
import Entity.StatusEffect;
import Main.Json;
import Observer.DamagePopUp;

import java.awt.Color;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Motorul sinergiilor dintre cărți (datele sunt în res/Data/cards.json):
 *  1. Reacții element + status (Foc pe Înghețat = Topire, Fulger pe Ud/Înghețat = șoc în lanț pe tot grupul...).
 *  2. Lanțul: a doua carte cu același element în aceeași tură dă un bonus tot mai mare ("Combo x2, x3").
 *  3. Pregătire/răsplată și 4. sincron cu aliații: condițiile cărților din date (playedTag, targetStatus, ally, minTags).
 * Toate cărțile se joacă prin Combos.play, apelat din Player.playSelectedCard.
 */
public final class Combos {
    private Combos() {}

    /** Ce s-a jucat în tura curentă (se șterge la începutul fiecărei ture). */
    public static final class State {
        private final Map<String, Integer> counts = new LinkedHashMap<>();
        public String banner = "";
        public int bannerTicks = 0;
        public String bestTag = null;
        public int bestCount = 0;

        public void reset() { counts.clear(); bestTag = null; bestCount = 0; bannerTicks = 0; banner = ""; }
        public void tick() { if (bannerTicks > 0) bannerTicks--; }
        public int count(String tag) { return counts.getOrDefault(tag, 0); }
        public int distinctTags() { return counts.size(); }
        public boolean played(String tag) { return counts.containsKey(tag); }
        int bump(String tag) {
            int n = counts.merge(tag, 1, Integer::sum);
            if (n > bestCount) { bestCount = n; bestTag = tag; }
            return n;
        }
    }

    /** Joacă o carte: efectele ei, apoi reacțiile cu statusurile pe care le avea ținta ÎNAINTE, apoi bonusul de lanț. */
    public static void play(Player p, Card card, Enemy target) {
        if (target != null && card.offensive && target.tryDodge(p, card)) return;   // Eschiva lui Jean: cartea se pierde
        Set<StatusEffect.Type> before = EnumSet.noneOf(StatusEffect.Type.class);
        if (target != null) for (StatusEffect s : target.activeStatuses) before.add(s.type);

        card.play(p, target);

        if (target != null) {
            for (Map<String, Object> a : card.applies) {   // statusurile cărților din cod (Fulger de Sânge -> Încărcat...)
                try {
                    StatusEffect.Type st = StatusEffect.Type.valueOf(Json.str(a.get("status"), ""));
                    target.applyStatus(st, Json.integer(a.get("turns"), 2) + p.statusTurns(st), Json.integer(a.get("value"), 0));
                } catch (IllegalArgumentException ignored) { }
            }
            if (card.offensive && target.alive) {
                for (CardCatalog.Reaction r : CardCatalog.reactions()) {
                    if (!card.tags.contains(r.element())) continue;
                    StatusEffect.Type hit = null;
                    for (StatusEffect.Type t : r.requires()) if (before.contains(t)) { hit = t; break; }
                    if (hit != null) react(p, target, r, hit);
                }
            }
        }
        chain(p, card, target);
    }

    private static void react(Player p, Enemy target, CardCatalog.Reaction r, StatusEffect.Type consumed) {
        for (Iterator<StatusEffect> it = target.activeStatuses.iterator(); it.hasNext(); ) {
            if (it.next().type == consumed) { it.remove(); break; }
        }
        announce(p, "Reacție: " + r.name() + "!", r.color(), target);
        if (r.damage() > 0) target.takeDamage(p, r.damage() + p.runeSum("reactionDamage"));   // Runa Glaciarei
        if (r.aoeDamage() > 0) {
            for (Enemy e : p.livingEnemies()) {
                if (e == target) continue;
                e.takeDamage(p, r.aoeDamage());
                p.gp.fx.reaction(r.color().getRGB() & 0xFFFFFF, e.battleCenterX, p.gp.battleUI.groundY() - p.gp.screenHeight / 6, false);
            }
        }
        Map<String, Object> ap = r.apply();
        if (!ap.isEmpty() && target.alive) {
            try {
                target.applyStatus(StatusEffect.Type.valueOf(Json.str(ap.get("status"), "")), Json.integer(ap.get("turns"), 2), Json.integer(ap.get("value"), 0));
            } catch (IllegalArgumentException ignored) { }
        }
    }

    /** A doua, a treia... carte cu același element în tură: bonus tot mai mare. */
    private static void chain(Player p, Card card, Enemy target) {
        if (card.tags.isEmpty()) return;
        String bestTag = null;
        int best = 0;
        for (String tag : card.tags) {
            int n = p.combo.bump(tag);
            if (n > best) { best = n; bestTag = tag; }
        }
        if (best < 2) return;
        Map<String, Object> cfg = CardCatalog.chain();
        int level = Math.min(best, Json.integer(cfg.get("maxLevel"), 4)) - 1;
        String text = "COMBO " + CardCatalog.tagLabel(bestTag).toUpperCase() + " x" + best;
        if (card.offensive && target != null && target.alive) {
            announce(p, text, CardCatalog.tagColor(bestTag), target);
            target.takeDamage(p, Json.integer(cfg.get("perLevel"), 4) * level);
        } else if (!card.offensive) {
            announce(p, text, CardCatalog.tagColor(bestTag), target);
            p.addShield(Json.integer(cfg.get("shieldPerLevel"), 3) * level);
        }
    }

    /** Numele combo-ului sau al reacției: pop-up deasupra inamicului + scuturat și particule. */
    public static void announce(Player p, String text, Color color, Enemy target) {
        p.combo.banner = text;
        p.combo.bannerTicks = 90;
        int x = target != null && target.battleCenterX > 0 ? target.battleCenterX : p.gp.battleUI.enemyCenterX();
        p.gp.popups.add(new DamagePopUp(x, p.gp.tileSize * 2, text, color));
        p.gp.fx.reaction(color.getRGB() & 0xFFFFFF, x, p.gp.battleUI.groundY() - p.gp.screenHeight / 6, true);
    }

    // ------------------------------------------------------------------ condiții și indicii

    /** Condițiile "when" ale cărților din date. */
    public static boolean conditionMet(Player p, Enemy target, Map<String, Object> when) {
        String tag = Json.str(when.get("playedTag"), null);
        if (tag != null && !p.combo.played(tag)) return false;
        String st = Json.str(when.get("targetStatus"), null);
        if (st != null) {
            if (target == null) return false;
            boolean has = false;
            for (StatusEffect s : target.activeStatuses) if (s.type.name().equals(st)) has = true;
            if (!has) return false;
        }
        String ally = Json.str(when.get("ally"), null);
        if (ally != null) {
            var a = p.gp.party.get(ally);
            if (a == null || !a.alive()) return false;
        }
        int min = Json.integer(when.get("minTags"), 0);
        return min <= 0 || p.combo.distinctTags() >= min;
    }

    /** 0 = nimic, 1 = continuă un lanț (același element jucat deja), 2 = combo/reacție gata (evidențiere puternică). */
    public static int ready(Player p, Card card, Enemy target) {
        if (card.comboMet(p, target)) return 2;
        if (target != null && card.offensive) {
            for (CardCatalog.Reaction r : CardCatalog.reactions()) {
                if (!card.tags.contains(r.element())) continue;
                for (StatusEffect s : target.activeStatuses) if (r.requires().contains(s.type)) return 2;
            }
        }
        for (String t : card.tags) if (p.combo.played(t)) return 1;
        return 0;
    }

    /** "Reacție: Topire pe Înghețat" pentru elementele cărții (din reacțiile din date), sau null. */
    public static String reactionHint(Card card) {
        StringBuilder sb = new StringBuilder();
        for (CardCatalog.Reaction r : CardCatalog.reactions()) {
            if (!card.offensive || !card.tags.contains(r.element())) continue;
            StringBuilder on = new StringBuilder();
            for (StatusEffect.Type t : r.requires()) { if (on.length() > 0) on.append("/"); on.append(CardCatalog.statusLabel(t)); }
            if (sb.length() > 0) sb.append("; ");
            sb.append(r.name()).append(" pe ").append(on);
        }
        return sb.length() == 0 ? null : "Reacție: " + sb;
    }

    /** Elementele cărții pentru afișare: "Foc · Fulger". */
    public static String tagsLine(Card card) {
        StringBuilder sb = new StringBuilder();
        for (String t : card.tags) { if (sb.length() > 0) sb.append(" · "); sb.append(CardCatalog.tagLabel(t)); }
        return sb.toString();
    }
}
