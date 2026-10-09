package Entity.Allies;

import Entity.Inamici.Enemy;
import Entity.Player.Player;
import Entity.StatusEffect;
import Main.GamePanel;
import Main.Json;
import Observer.DamagePopUp;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Echipa lui Eron: aliații recrutați, viața lor și tura lor în luptă (res/Data/allies.json).
 * Aliatul acționează când Eron își termină tura, înainte de inamic. Loviturile inamicului pot ajunge la aliat
 * (Player.damagePlayer -> redirectHit). Starea se salvează în saves.party.
 */
public class Party {
    private final GamePanel gp;
    private final Random rng = new Random();
    private final Map<String, Map<String, Object>> defs = new LinkedHashMap<>();
    private double targetChance = 0.3, healAfterBattle = 0.5;
    private int maxSize = 2;

    public final List<Ally> members = new ArrayList<>();
    /** Aliatul recrutat prin victoria curentă: după ecranul de recompensă se joacă scena (AllySceneState). */
    public String pendingRecruit = null;

    public Party(GamePanel gp) {
        this.gp = gp;
        Map<String, Object> root = Json.map(Json.parseResource("/Data/allies.json"));
        Map<String, Object> cfg = Json.map(root.get("party"));
        targetChance = Json.number(cfg.get("targetChance"), targetChance);
        healAfterBattle = Json.number(cfg.get("healAfterBattle"), healAfterBattle);
        maxSize = Json.integer(cfg.get("maxSize"), maxSize);
        for (Object o : Json.list(root.get("allies"))) {
            Map<String, Object> d = Json.map(o);
            defs.put(Json.str(d.get("id"), ""), d);
        }
    }

    public void reset() { members.clear(); pendingRecruit = null; }

    public boolean has(String id) { return get(id) != null; }

    public Ally get(String id) {
        for (Ally a : members) if (a.id.equals(id)) return a;
        return null;
    }

    /** Definiția unui aliat (pentru scena de recrutare), chiar dacă nu e încă în echipă. */
    public Ally define(String id) {
        Map<String, Object> d = defs.get(id);
        return d == null ? null : new Ally(d);
    }

    public Ally recruit(String id) {
        if (has(id) || members.size() >= maxSize) return get(id);
        Ally a = define(id);
        if (a != null) members.add(a);
        return a;
    }

    /** Apelat la victorie: dacă inamicul învins recrutează un aliat nou, scena se joacă după recompensă. */
    public void onEnemyDefeated(String enemyName) {
        for (Map.Entry<String, Map<String, Object>> e : defs.entrySet()) {
            if (Json.str(e.getValue().get("recruitFrom"), "").equals(enemyName) && !has(e.getKey()) && members.size() < maxSize) {
                pendingRecruit = e.getKey();
            }
        }
    }

    // ------------------------------------------------------------------ luptă

    public void startBattle() {
        for (Ally a : members) { a.shield = 0; a.lastAction = a.alive() ? "" : "la pământ"; }
    }

    public void afterBattle() {
        for (Ally a : members) {
            a.shield = 0;
            a.lastAction = "";
            a.life = Math.max(a.life, (int) Math.ceil(a.maxLife * healAfterBattle));
        }
    }

    /** Tura aliaților (după Eron, înainte de inamic). */
    public void actAll(Player p, Enemy enemy) {
        for (Ally a : members) {
            if (!a.alive() || enemy == null || !enemy.alive) continue;
            for (Ally.Ability ab : a.abilities) {
                if (!conditionHolds(ab.when, a, p)) continue;
                perform(a, ab, p, enemy);
                break;
            }
        }
    }

    private boolean conditionHolds(String when, Ally a, Player p) {
        if (when == null || when.equals("always")) return true;
        String[] t = when.split(":");
        double v = t.length > 1 ? Double.parseDouble(t[1]) : 0;
        return switch (t[0]) {
            case "selfLifeBelow" -> a.life < a.maxLife * v;
            case "playerLifeBelow" -> p.life < p.maxLife * v;
            case "playerShieldBelow" -> p.shield < v;
            default -> false;
        };
    }

    private void perform(Ally a, Ally.Ability ab, Player p, Enemy enemy) {
        int ax = allyX(a), ay = allyGroundY() - gp.screenHeight / 5;
        boolean hitsEnemy = false;
        switch (ab.type) {
            case "damage" -> {
                enemy.takeDamage(p, ab.value);
                if (!ab.status.isEmpty()) {
                    try { enemy.applyStatus(StatusEffect.Type.valueOf(ab.status), ab.statusTurns, ab.statusValue); }
                    catch (IllegalArgumentException ignored) { /* status necunoscut în JSON */ }
                }
                hitsEnemy = true;
            }
            case "shieldPlayer" -> p.addShield(ab.value);
            case "shieldSelf" -> {
                a.shield += ab.value;
                gp.popups.add(new DamagePopUp(ax, ay, "+" + ab.value + " BLOCK", Color.CYAN));
            }
            case "healPlayer" -> {
                p.life = Math.min(p.maxLife, p.life + ab.value);
                p.notifyObservers();
                gp.popups.add(new DamagePopUp(gp.tileSize * 3, gp.tileSize * 2, "+" + ab.value + " HP", Color.GREEN));
            }
            case "healSelf" -> {
                a.life = Math.min(a.maxLife, a.life + ab.value);
                gp.popups.add(new DamagePopUp(ax, ay, "+" + ab.value + " HP", Color.GREEN));
            }
            default -> { }
        }
        a.lastAction = ab.name;
        gp.popups.add(new DamagePopUp(ax, ay - gp.tileSize, ab.name, new Color(a.color)));
        gp.fx.allyCast(a.color, ax, ay, hitsEnemy);
    }

    /** x-ul aliatului pe ecran (în dreapta lui Eron). */
    public int allyX(Ally a) {
        int idx = Math.max(0, members.indexOf(a));
        return (int) (gp.screenWidth * (0.30 + 0.13 * idx));
    }

    /** Linia solului aliaților: mai sus decât a lui Eron, ca să nu se suprapună cu mâna de cărți. */
    public int allyGroundY() { return (int) (gp.screenHeight * 0.64); }

    /**
     * O lovitură a inamicului (apelată din Player.damagePlayer, în luptă). Cu șansa din allies.json ajunge la un aliat
     * în viață. Returnează true dacă aliatul a primit lovitura.
     */
    public boolean redirectHit(int amount) {
        List<Ally> alive = new ArrayList<>();
        for (Ally a : members) if (a.alive()) alive.add(a);
        if (alive.isEmpty() || rng.nextDouble() >= targetChance) return false;
        Ally a = alive.get(rng.nextInt(alive.size()));
        int ax = allyX(a), ay = allyGroundY() - gp.screenHeight / 5;
        if (a.shield >= amount) {
            a.shield -= amount;
            gp.popups.add(new DamagePopUp(ax, ay, "-" + amount + " BLOCK", Color.GRAY));
        } else {
            int rest = amount - a.shield;
            a.shield = 0;
            a.life = Math.max(0, a.life - rest);
            gp.popups.add(new DamagePopUp(ax, ay, "-" + rest + " HP", Color.RED));
            gp.fx.allyHurt(ax, ay);
            if (!a.alive()) {
                a.lastAction = "la pământ";
                gp.popups.add(new DamagePopUp(ax, ay - gp.tileSize, a.name + " a căzut!", Color.ORANGE));
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ salvare

    /** Format: jean:75;altul:30 (id:viață curentă). */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (Ally a : members) { if (sb.length() > 0) sb.append(';'); sb.append(a.id).append(':').append(a.life); }
        return sb.toString();
    }

    public void deserialize(String s) {
        members.clear();
        pendingRecruit = null;
        if (s == null || s.isBlank()) return;
        for (String part : s.split(";")) {
            String[] t = part.split(":");
            Ally a = define(t[0]);
            if (a == null) continue;
            if (t.length > 1) {
                try { a.life = Math.max(1, Math.min(a.maxLife, Integer.parseInt(t[1]))); } catch (NumberFormatException ignored) { }
            }
            members.add(a);
        }
    }
}
