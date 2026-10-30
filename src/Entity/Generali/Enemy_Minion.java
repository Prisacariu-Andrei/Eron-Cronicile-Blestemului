package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Inamici.Enemy_Data;
import Entity.Player.Player;
import Main.GamePanel;
import Main.Json;

import java.util.Map;

/**
 * Minionii bossilor (enemies.json, "behavior": "minion"): lovesc cu dmg fix. hitOn = "always" (Minionul de Zăpadă al Glaciarei)
 * sau "toll" (Minionul de Umbră: lovește doar în turele de Vamă ale lui Charon, în rest stă pe loc).
 */
public class Enemy_Minion extends Enemy_Data {
    private final boolean onToll;
    private final int hit;
    private boolean ready = false;   // constructorul de bază apelează setNextIntent() înainte de câmpurile de aici

    public Enemy_Minion(GamePanel gp, int x, int y, Map<String, Object> d) {
        super(gp, x, y, d);
        onToll = "toll".equals(Json.str(d.get("hitOn"), "always"));
        hit = Json.integer(d.get("dmg"), 6);
        ignoreGroupScaling = true;   // cifra din enemies.json e cea din luptă
        ready = true;
        setNextIntent();
    }

    @Override
    public void setNextIntent() {
        if (!ready) { super.setNextIntent(); return; }
        intentText = null;
        if (onToll && !leaderTolls()) {
            nextAction = EnemyAction.BUFF;   // stă pe loc până la următoarea Vamă
            intentValue = 0;
            intentText = "…";
        } else {
            nextAction = EnemyAction.ATTACK;
            intentValue = hit;
        }
    }

    private boolean leaderTolls() {
        for (Enemy e : teamMates()) if (e instanceof Enemy_Charon c && c.tollNext()) return true;
        return false;
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns != 0) return;
        if (nextAction == EnemyAction.ATTACK) player.damagePlayer(intentValue);
        setNextIntent();
    }
}
