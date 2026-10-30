package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Inamici.Groups;
import Entity.Player.Player;
import Entity.StatusEffect;
import Main.Json;
import Main.GamePanel;
import Observer.DamagePopUp;

import java.awt.*;
import java.util.Map;
import java.util.Random;

/**
 * Feldgug, Regele Trol. Cifrele sunt în res/Data/enemies.json -> bosses.feldgug: lovește cu damage (27 când Eron e Fragil: +50%),
 * în turele fără atac își pune armură; regenerează pe tură (mai mult în faza II, sub phase2AtPercent% viață), iar în faza II
 * loviturile pot pune Fragilitate.
 */
public class Enemy_Feldgug extends Enemy {

    private final Map<String, Object> cfg = Groups.boss("feldgug");
    private final Random rand = new Random();

    public Enemy_Feldgug(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Feldgug, Regele Trol";
        this.maxLife = Json.integer(cfg.get("hp"), 180);
        this.life = maxLife;
        this.attackDamage = Json.integer(cfg.get("damage"), 18);
        this.mapSize = 1.5;

        hitBox = new Rectangle(0, 0, (int)(gp.tileSize * mapSize), (int)(gp.tileSize * mapSize));
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        this.isStatic = true;

        getImage();
        setNextIntent();
    }

    private boolean phaseTwo() { return life * 100 <= maxLife * Json.integer(cfg.get("phase2AtPercent"), 50); }

    /** Valoarea din pereche [faza I, faza II] după faza de acum. */
    private int byPhase(String key, int def1, int def2) {
        var l = Json.list(cfg.get(key));
        return Json.integer(l.get(Math.min(phaseTwo() ? 1 : 0, Math.max(0, l.size() - 1))), phaseTwo() ? def2 : def1);
    }

    @Override
    public void getImage() {
        // Înlocuiește calea dacă ai imagini specifice pentru Feldgug, sau o păstrezi pe cea de bază
        up1 = setup("/Enemy/FeldgugRegeleTrol");
        up2 = setup("/Enemy/FeldgugRegeleTrol");
        down1 = setup("/Enemy/FeldgugRegeleTrol");
        down2 = setup("/Enemy/FeldgugRegeleTrol");
        left1 = setup("/Enemy/FeldgugRegeleTrol");
        left2 = setup("/Enemy/FeldgugRegeleTrol");
        right1 = setup("/Enemy/FeldgugRegeleTrol");
        right2 = setup("/Enemy/FeldgugRegeleTrol");
        fightStand = setup("/Enemy/FeldgugRegeleTrol");
    }

    @Override
    public void setNextIntent() {
        if (rand.nextInt(100) < byPhase("attackChance", 40, 50)) {
            nextAction = EnemyAction.ATTACK;
            intentValue = attackDamage;
        } else {
            nextAction = EnemyAction.DEFEND;   // Piele de Piatră
            intentValue = Json.integer(cfg.get("shield"), 10);
        }
    }

    @Override
    public void executeIntent(Player player) {
        // 1. Regenerare pasivă (Sânge de Trol), și când e amețit
        int regen = byPhase("regen", 3, 5);
        if (life < maxLife && regen > 0) {
            life = Math.min(maxLife, life + regen);
            gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, "+" + regen + " HP (Regen)", Color.GREEN));
        }
        if (stunTurns != 0) return;

        // 2. Acțiunea aleasă
        if (nextAction == EnemyAction.DEFEND) {
            this.shield += intentValue;
            gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "+" + intentValue + " BLOCK", Color.CYAN));
        } else if (nextAction == EnemyAction.ATTACK) {
            player.damagePlayer(intentValue);
            if (phaseTwo() && rand.nextInt(100) < Json.integer(cfg.get("vulnerableChance"), 50)) {
                player.applyStatus(StatusEffect.Type.VULNERABLE, Json.integer(cfg.get("vulnerableTurns"), 1), Json.integer(cfg.get("vulnerablePercent"), 50));
            }
        }
        setNextIntent();
    }
}
