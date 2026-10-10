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
 * (Player.damagePlayer -> redirectHit, oprit cât timp party.targetChance = 0). Starea se salvează în saves.party.
 * Aliații cu intrare în res/Data/ally_ai.json (Jean) folosesc logica de sprijin de acolo, în funcție de context.
 */
public class Party {
    private final GamePanel gp;
    private final Random rng = new Random();
    private final Map<String, Map<String, Object>> defs = new LinkedHashMap<>();
    private double targetChance = 0.3, healAfterBattle = 0.5;
    private int maxSize = 2;

    private final Map<String, Object> ai = new LinkedHashMap<>();   // ally_ai.json: logica de sprijin pe id de aliat

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
        for (Map.Entry<String, Object> e : Json.map(Json.parseResource("/Data/ally_ai.json")).entrySet()) {
            if (!e.getKey().startsWith("_")) ai.put(e.getKey(), e.getValue());
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
            Map<String, Object> cfg = Json.map(ai.get(a.id));
            if (!cfg.isEmpty()) { support(a, cfg, p, enemy); continue; }
            for (Ally.Ability ab : a.abilities) {
                if (!conditionHolds(ab.when, a, p)) continue;
                perform(a, ab, p, enemy);
                break;
            }
        }
    }

    private int roll(Object range, int def) {
        List<Object> l = Json.list(range);
        if (l.isEmpty()) return def;
        int lo = Json.integer(l.get(0), def), hi = Json.integer(l.size() > 1 ? l.get(1) : null, lo);
        return lo + (hi > lo ? rng.nextInt(hi - lo + 1) : 0);
    }

    private void say(Ally a, String text, int ticks) {
        a.say = text;
        a.sayTicks = ticks;
        a.lastAction = text;
    }

    /**
     * Sprijinul lui Jean (ally_ai.json), după context: Eron rănit -> îl vindecă; o lovitură mare pe care armura nu o acoperă -> armură;
     * altfel lovește, slăbește un inamic sau, rar, dă o mică întărire.
     */
    private void support(Ally a, Map<String, Object> cfg, Player p, Enemy target) {
        int ticks = Json.integer(cfg.get("sayTicks"), 150);
        int ax = allyX(a), ay = allyHeadY();

        // 1. Eron sub pragul de viață -> vindecare mică
        if (p.life < p.maxLife * Json.number(cfg.get("healBelow"), 0.4)) {
            int v = roll(cfg.get("heal"), 4);
            p.life = Math.min(p.maxLife, p.life + v);
            p.notifyObservers();
            gp.popups.add(new DamagePopUp(gp.tileSize * 3, gp.tileSize * 2, "+" + v + " HP", Color.GREEN));
            say(a, "Te vindecă +" + v, ticks);
            gp.fx.allyCast(a.color, ax, ay, false);
            return;
        }

        // 2. un inamic pregătește o lovitură mare pe care armura lui Eron nu o acoperă -> armură
        int big = Json.integer(cfg.get("bigAttack"), 10);
        for (Enemy e : p.livingEnemies()) {
            if (e.nextAction == Enemy.EnemyAction.ATTACK && e.intentValue >= big && p.shield < e.intentValue) {
                int v = roll(cfg.get("armor"), 7);
                p.addShield(v);
                say(a, "Armură +" + v, ticks);
                gp.fx.allyCast(a.color, ax, ay, false);
                return;
            }
        }

        // 3. altfel: atac, slăbire sau o mică întărire, după greutăți
        Map<String, Object> atk = Json.map(cfg.get("attack")), deb = Json.map(cfg.get("debuff")), buf = Json.map(cfg.get("buff"));
        int wa = Json.integer(atk.get("weight"), 5), wd = Json.integer(deb.get("weight"), 3), wb = Json.integer(buf.get("weight"), 1);
        int r = rng.nextInt(Math.max(1, wa + wd + wb));
        boolean hits = false;
        if (r < wa) {
            int v = roll(atk.get("damage"), 10);
            target.takeDamage(p, v);
            say(a, "Lovește -" + v, ticks);
            hits = true;
        } else if (r < wa + wd && !Json.list(deb.get("options")).isEmpty()) {
            List<Object> opts = Json.list(deb.get("options"));
            Map<String, Object> o = Json.map(opts.get(rng.nextInt(opts.size())));
            try {
                target.applyStatus(StatusEffect.Type.valueOf(Json.str(o.get("status"), "WEAK")), Json.integer(o.get("turns"), 2), Json.integer(o.get("value"), 0));
            } catch (IllegalArgumentException ignored) { /* status necunoscut în JSON */ }
            say(a, Json.str(o.get("text"), "Slăbiciune") + "!", ticks);
            hits = true;
        } else {
            int v = roll(buf.get("armor"), 3);
            p.addShield(v);
            say(a, "Încurajare: armură +" + v, ticks);
        }
        gp.fx.allyCast(a.color, ax, ay, hits);
    }

    private boolean conditionHolds(String when, Ally a, Player p) {
        if (when == null || when.equals("always")) return true;
        String[] t = when.split(":");
        double v = t.length > 1 ? Double.parseDouble(t[1]) : 0;
        return switch (t[0]) {
            case "selfLifeBelow" -> a.life < a.maxLife * v;
            case "playerLifeBelow" -> p.life < p.maxLife * v;
            case "playerShieldBelow" -> p.shield < v;
            case "teamLifeBelow" -> weakestFraction(p) < v;   // Eron sau oricare aliat sub procentul dat
            default -> false;
        };
    }

    private void perform(Ally a, Ally.Ability ab, Player p, Enemy enemy) {
        int ax = allyX(a), ay = allyHeadY();
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
            case "shieldLowest" -> {   // protejează pe cel mai slab din echipă (Eron sau un aliat, poate chiar pe sine)
                Ally w = weakestAlly();
                if (w != null && (double) w.life / w.maxLife < (double) p.life / p.maxLife) {
                    w.shield += ab.value;
                    gp.popups.add(new DamagePopUp(allyX(w), allyHeadY(), "+" + ab.value + " BLOCK", Color.CYAN));
                } else {
                    p.addShield(ab.value);
                }
            }
            case "healLowest" -> {     // vindecă pe cel mai slab din echipă
                Ally w = weakestAlly();
                if (w != null && (double) w.life / w.maxLife < (double) p.life / p.maxLife) {
                    w.life = Math.min(w.maxLife, w.life + ab.value);
                    gp.popups.add(new DamagePopUp(allyX(w), allyHeadY(), "+" + ab.value + " HP", Color.GREEN));
                } else {
                    p.life = Math.min(p.maxLife, p.life + ab.value);
                    p.notifyObservers();
                    gp.popups.add(new DamagePopUp(gp.tileSize * 3, gp.tileSize * 2, "+" + ab.value + " HP", Color.GREEN));
                }
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

    /** Aliatul în viață cu cea mai mică viață (în procente), sau null dacă nu e niciunul. */
    private Ally weakestAlly() {
        Ally best = null;
        for (Ally a : members) if (a.alive() && (best == null || (double) a.life / a.maxLife < (double) best.life / best.maxLife)) best = a;
        return best;
    }

    private double weakestFraction(Player p) {
        double min = (double) p.life / p.maxLife;
        for (Ally a : members) if (a.alive()) min = Math.min(min, (double) a.life / a.maxLife);
        return min;
    }

    /** x-ul (centrul) aliatului: puțin în stânga lui Eron, ca să stea în spatele lui. */
    public int allyX(Ally a) {
        int idx = Math.max(0, members.indexOf(a));
        return (int) (gp.screenWidth * (0.075 + 0.14 * idx));
    }

    /** Înălțimea aliatului: cam cât Eron (în luptă Eron are 20% din lățimea ecranului). */
    public int allyHeight() { return (int) (gp.screenWidth * 0.20); }

    /** Linia solului aliaților: puțin mai sus decât a lui Eron (stă "în spate"). */
    public int allyGroundY() { return (int) (gp.screenHeight * 0.87) - (int) (gp.screenHeight * 0.05); }

    /** Deasupra capului aliatului (pentru popup-uri și textul acțiunii). */
    public int allyHeadY() { return allyGroundY() - allyHeight() - 8; }

    /**
     * O lovitură a inamicului (apelată din Player.damagePlayer, în luptă). Cu șansa din allies.json ajunge la un aliat
     * în viață. Returnează true dacă aliatul a primit lovitura.
     */
    public boolean redirectHit(int amount) {
        List<Ally> alive = new ArrayList<>();
        for (Ally a : members) if (a.alive()) alive.add(a);
        if (alive.isEmpty() || rng.nextDouble() >= targetChance) return false;
        Ally a = alive.get(rng.nextInt(alive.size()));
        int ax = allyX(a), ay = allyHeadY();
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
