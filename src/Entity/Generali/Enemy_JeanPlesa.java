package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Inamici.Groups;
import Entity.Items.Card;
import Entity.Player.Player;
import Entity.StatusEffect;
import Main.GamePanel;
import Main.Json;
import Observer.DamagePopUp;

import java.awt.*;
import java.util.Map;
import java.util.Random;

/**
 * Jean Pleșa. Cifrele sunt în res/Data/enemies.json -> bosses.jean: gardă, lovitură, atac dublu (2 x 5, apoi Fragilitate),
 * sabotaj (daune + esență pierdută), iar cărțile cu daune pot fi eschivate (dodgeChance). Sub phase2AtPercent% viață (faza II)
 * fiecare eschivă declanșează și o contralovitură.
 */
public class Enemy_JeanPlesa extends Enemy {

    private enum Move { GUARD, HIT, DOUBLE, SABOTAGE }

    private final Map<String, Object> cfg = Groups.boss("jean");
    private final Random rng = new Random();
    private Move move = Move.HIT;
    private boolean phaseTwo = false;

    public Enemy_JeanPlesa(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Jean Plesa";
        this.maxLife = Json.integer(cfg.get("hp"), 175);
        this.life = maxLife;
        this.attackDamage = Json.integer(cfg.get("hit"), 10);
        this.mapSize = 1.3;

        hitBox = new Rectangle(0, 0, (int)(gp.tileSize * mapSize), (int)(gp.tileSize * mapSize));
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        this.isStatic = true;
        this.ignoreGroupScaling = true;

        getImage();
        setNextIntent();
    }

    @Override
    public void getImage() {
        up1 = flipImage(setup("/Enemy/JeanPlesa"));
        up2 = flipImage(setup("/Enemy/JeanPlesa"));
        down1 = flipImage(setup("/Enemy/JeanPlesa"));
        down2 = flipImage(setup("/Enemy/JeanPlesa"));
        left1 = flipImage(setup("/Enemy/JeanPlesa"));
        left2 = flipImage(setup("/Enemy/JeanPlesa"));
        right1 = flipImage(setup("/Enemy/JeanPlesa"));
        right2 = flipImage(setup("/Enemy/JeanPlesa"));
        fightStand = setup("/Enemy/JeanPlesa");
    }

    private int n(String key, int def) { return Json.integer(cfg.get(key), def); }

    @Override
    public void setNextIntent() {
        intentText = null;
        if (rng.nextInt(100) < n("guardChance", 35)) {
            move = Move.GUARD;
            nextAction = EnemyAction.DEFEND;
            intentValue = n("guardBlock", 8);
            return;
        }
        Map<String, Object> w = Json.map(cfg.get("weights"));
        int wHit = Json.integer(w.get("hit"), 40), wDouble = Json.integer(w.get("double"), 30), wSab = Json.integer(w.get("sabotage"), 30);
        int r = rng.nextInt(Math.max(1, wHit + wDouble + wSab));
        if (r < wHit) {
            move = Move.HIT;
            nextAction = EnemyAction.ATTACK;
            intentValue = n("hit", 10);
        } else if (r < wHit + wDouble) {
            move = Move.DOUBLE;
            nextAction = EnemyAction.ATTACK;
            intentValue = n("doubleHit", 5);
            intentText = "⚔ " + intentValue + "×2";
        } else {
            move = Move.SABOTAGE;
            nextAction = EnemyAction.SPECIAL;
            intentValue = n("sabotageDamage", 8);
            intentText = "⚔ " + intentValue + " · -" + n("sabotageEssence", 1) + " ✦";
        }
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns != 0) return;
        switch (move) {
            case GUARD -> defend(intentValue);
            case HIT -> player.damagePlayer(intentValue);
            case DOUBLE -> {
                gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 4, "ATAC DUBLU!", Color.RED));
                player.damagePlayer(intentValue);
                player.damagePlayer(intentValue);
                if (player.life > 0) player.applyStatus(StatusEffect.Type.VULNERABLE, n("vulnerableTurns", 2), n("vulnerablePercent", 50));   // abia după a doua lovitură
            }
            case SABOTAGE -> {
                player.damagePlayer(intentValue);
                int lost = Math.min(player.currentMana, n("sabotageEssence", 1));   // esența se reface la sfârșitul turei lui Eron: pierderea se simte în tura următoare
                if (lost > 0) {
                    player.currentMana -= lost;
                    player.notifyObservers();
                    gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, "-" + lost + " ESENȚĂ", Color.BLUE));
                }
            }
        }
        setNextIntent();
    }

    /** Eschiva: o carte cu daune are dodgeChance% să dea greș; în faza II fiecare eschivă e urmată de o contralovitură. */
    @Override
    public boolean tryDodge(Player player, Card card) {
        if (rng.nextInt(100) >= n("dodgeChance", 20)) return false;
        gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "Eschivă!", Color.YELLOW));
        if (phaseTwo) {
            gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 4, "CONTRALOVITURĂ!", Color.RED));
            player.damagePlayer(n("counterDamage", 8));
        }
        return true;
    }

    @Override
    public void takeDamage(Player player, int amount) {
        super.takeDamage(player, amount);
        if (alive && !phaseTwo && life * 100 <= maxLife * n("phase2AtPercent", 40)) {
            phaseTwo = true;
            gp.battleUI.announcePhase("FAZA II: JEAN SE ÎNFURIE", "Fiecare carte eschivată primește o contralovitură.", Color.ORANGE);
            gp.fx.phaseChange();
        }
    }
}
