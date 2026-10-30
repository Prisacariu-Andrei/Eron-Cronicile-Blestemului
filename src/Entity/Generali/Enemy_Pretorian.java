package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Inamici.Enemy_Data;
import Entity.Items.Card;
import Entity.Player.Player;
import Entity.StatusEffect;
import Main.GamePanel;
import Main.Json;
import Observer.DamagePopUp;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Cei trei Pretorieni din Castelul de Obsidian. Sprite-ul și numele vin din enemies.json (câmpul "behavior" = pretor1/2/3),
 * iar cifrele din res/Data/castle.json -> pretorians[].stats:
 *  pretor1 - Garda de Obsidian: doi soldați; cât sunt amândoi în viață unul lovește (hit), celălalt îl apără (guardBlock) și rolurile se schimbă
 *            la fiecare tură; cel rămas singur lovește cu soloHit.
 *  pretor2 - Generalii Trezirii: lovește cu startDamage, plus growPerTurn în fiecare tură.
 *  pretor3 - Bătrânul Cronicar: alternează heavyHit cu lightHit (acesta trece peste armură) și blestemă în fiecare tură o carte din mâna lui Eron (+curseCost mană).
 */
public class Enemy_Pretorian extends Enemy_Data {
    private final String kind;
    private final Map<String, Object> st;
    private final Random rng = new Random();
    private int turn = 0;

    public Enemy_Pretorian(GamePanel gp, int x, int y, Map<String, Object> d) {
        super(gp, x, y, d);   // atenție: constructorul de bază apelează setNextIntent() înainte ca câmpurile de aici să fie puse
        kind = Json.str(d.get("behavior"), "pretor1");
        st = statsFor(kind);
        maxLife = Json.integer(st.get("hp"), maxLife);
        life = maxLife;
        ignoreGroupScaling = true;   // cifrele din castle.json sunt cele din luptă, fără înmulțitorul pentru grupuri
        setNextIntent();
    }

    private static Map<String, Object> statsFor(String id) {
        for (Object o : Json.list(Json.map(Json.parseResource("/Data/castle.json")).get("pretorians"))) {
            Map<String, Object> p = Json.map(o);
            if (id.equals(Json.str(p.get("id"), ""))) return Json.map(p.get("stats"));
        }
        return Map.of();
    }

    private int n(String key, int def) { return Json.integer(st.get(key), def); }

    /** Pretorienii de același fel încă în viață (el însuși inclus). */
    private List<Enemy_Pretorian> living() {
        List<Enemy_Pretorian> out = new ArrayList<>();
        for (Enemy e : teamMates()) if (e instanceof Enemy_Pretorian p && p.kind != null && p.kind.equals(kind)) out.add(p);
        return out;
    }

    @Override
    public void setNextIntent() {
        if (st == null) { super.setNextIntent(); return; }   // în timpul constructorului de bază
        nextAction = EnemyAction.ATTACK;
        switch (kind) {
            case "pretor2" -> intentValue = n("startDamage", 11) + n("growPerTurn", 1) * turn;
            case "pretor3" -> intentValue = turn % 2 == 0 ? n("heavyHit", 15) : n("lightHit", 9);
            default -> {
                List<Enemy_Pretorian> mates = living();
                if (mates.size() >= 2) {
                    boolean guards = (turn + Math.max(0, mates.indexOf(this))) % 2 == 0;
                    nextAction = guards ? EnemyAction.DEFEND : EnemyAction.ATTACK;
                    intentValue = guards ? n("guardBlock", 8) : n("hit", 12);
                } else {
                    intentValue = n("soloHit", 16);
                }
            }
        }
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns != 0) return;
        switch (kind) {
            case "pretor2" -> strike(player, intentValue, false);
            case "pretor3" -> strike(player, intentValue, intentValue == n("lightHit", 9) && Json.bool(st.get("lightIgnoresBlock"), true));
            default -> {
                List<Enemy_Pretorian> mates = living();
                if (mates.size() >= 2 && nextAction == EnemyAction.DEFEND) {
                    for (Enemy_Pretorian m : mates) if (m != this) m.defend(intentValue);   // apără celălalt soldat
                    gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, name + " protejează", new Color(120, 190, 255)));
                } else {
                    strike(player, mates.size() >= 2 ? n("hit", 12) : n("soloHit", 16), false);   // singur = lovește mai tare
                }
            }
        }
        turn++;
        setNextIntent();
    }

    /** Lovitura lui (înjumătățită cât e Slăbit); ignoreBlock = armura lui Eron rămâne neatinsă și toată paguba trece. */
    private void strike(Player player, int dmg, boolean ignoreBlock) {
        for (StatusEffect s : activeStatuses) if (s.type == StatusEffect.Type.WEAK) { dmg /= 2; break; }
        if (ignoreBlock) {
            int armor = player.shield;
            player.shield = 0;
            player.damagePlayer(dmg);
            player.shield = armor;
        } else {
            player.damagePlayer(dmg);
        }
    }

    /** Bătrânul Cronicar blestemă o carte din mâna nouă: costă mai mult în tura asta. */
    @Override
    public void onPlayerTurnStart(Player player) {
        if (!"pretor3".equals(kind) || !alive) return;
        List<Card> free = new ArrayList<>();
        for (Card c : player.inventory.hand) if (c.curseCost == 0) free.add(c);
        if (free.isEmpty()) return;
        free.get(rng.nextInt(free.size())).curseCost = n("curseCost", 1);
    }

    /** Când un soldat cade, cel rămas își schimbă pe loc intenția (lovește cu soloHit). */
    @Override
    public void takeDamage(Player player, int amount) {
        super.takeDamage(player, amount);
        if (!alive && "pretor1".equals(kind)) for (Enemy e : teamMates()) if (e != this && e instanceof Enemy_Pretorian p) p.setNextIntent();
    }
}
