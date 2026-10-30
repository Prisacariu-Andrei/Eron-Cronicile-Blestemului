package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Inamici.Groups;
import Entity.Player.Player;
import Entity.StatusEffect;
import Main.GamePanel;
import Main.Json;
import Observer.DamagePopUp;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Charon. Cifrele sunt în res/Data/enemies.json -> bosses.charon: lovitură, Săgeata Sufletului și Bântuirea (ignoră armura),
 * iar la fiecare tollEvery-a tură cere Vama: Eron plătește esență (tura următoare) sau primește Săgeata. La începutul luptei
 * apare un Minion de Umbră, care lovește doar în turele de Vamă. Sub phase2AtPercent% viață (faza II) lovește mai tare.
 */
public class Enemy_Charon extends Enemy {

    private enum Move { HIT, ARROW, HAUNT, TOLL }

    private final Map<String, Object> cfg = Groups.boss("charon");
    private final Random rng = new Random();
    private Move move = Move.HIT;
    private int turn = 0;   // ture jucate până acum
    private boolean phaseTwo = false;

    public Enemy_Charon(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Charon, Străjerul Umbrelor fără Tihnă";
        this.maxLife = Json.integer(cfg.get("hp"), 180);
        this.life = maxLife;
        this.attackDamage = hitValue();
        this.mapSize = 1.3;
        this.battleSize = 1;

        hitBox = new Rectangle(0, 0, (int)(gp.tileSize * mapSize), (int)(gp.tileSize * mapSize));
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        this.isStatic = true;
        this.ignoreGroupScaling = true;
        this.packExtras.add("Minion de Umbră");   // apare alături de el de la începutul luptei

        getImage();
        setNextIntent();
    }

    @Override
    public void getImage() {
        up1 = setup("/Enemy/Charon");
        up2 = setup("/Enemy/Charon");
        down1 = setup("/Enemy/Charon");
        down2 = setup("/Enemy/Charon");
        left1 = setup("/Enemy/Charon");
        left2 = setup("/Enemy/Charon");
        right1 = setup("/Enemy/Charon");
        right2 = setup("/Enemy/Charon");
        fightStand = setup("/Enemy/Charon");
    }

    private int n(String key, int def) { return Json.integer(cfg.get(key), def); }

    private int hitValue() {
        List<Object> l = Json.list(cfg.get("hit"));
        return l.isEmpty() ? 15 : Json.integer(l.get(Math.min(phaseTwo ? 1 : 0, l.size() - 1)), 15);
    }

    /** Tura următoare e tura Vamei? (Minionul de Umbră lovește doar atunci.) */
    public boolean tollNext() { return move == Move.TOLL; }

    @Override
    public void setNextIntent() {
        intentText = null;
        if ((turn + 1) % Math.max(1, n("tollEvery", 3)) == 0) {
            move = Move.TOLL;
            nextAction = EnemyAction.SPECIAL;
            intentValue = n("arrow", 12);
            intentText = "☠ Vamă: " + n("tollEssence", 2) + " ✦ sau " + intentValue;
            return;
        }
        Map<String, Object> w = Json.map(cfg.get("weights"));
        int wHit = Json.integer(w.get("hit"), 40), wArrow = Json.integer(w.get("arrow"), 30), wHaunt = Json.integer(w.get("haunt"), 30);
        int r = rng.nextInt(Math.max(1, wHit + wArrow + wHaunt));
        if (r < wHit) {
            move = Move.HIT;
            nextAction = EnemyAction.ATTACK;
            intentValue = hitValue();
        } else if (r < wHit + wArrow) {
            move = Move.ARROW;
            nextAction = EnemyAction.SPECIAL;
            intentValue = n("arrow", 12);
            intentText = "☠ " + intentValue + " (ignoră armura)";
        } else {
            move = Move.HAUNT;
            nextAction = EnemyAction.SPECIAL;
            intentValue = n("haunt", 6);
            intentText = "☠ " + intentValue + " (ignoră armura) + fragil";
        }
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns != 0) return;
        switch (move) {
            case HIT -> {
                player.damagePlayer(intentValue);
                gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 4, "ATAC ETERIC", Color.RED));
            }
            case ARROW -> {
                hitIgnoringBlock(player, intentValue);
                gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "SĂGEATA SUFLETULUI!", Color.MAGENTA));
            }
            case HAUNT -> {
                hitIgnoringBlock(player, intentValue);
                if (player.life > 0) player.applyStatus(StatusEffect.Type.VULNERABLE, n("vulnerableTurns", 2), n("vulnerablePercent", 50));
                gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, "BÂNTUIRE!", Color.LIGHT_GRAY));
            }
            case TOLL -> toll(player);
        }
        turn++;
        setNextIntent();
    }

    /** Vama: Eron alege între esență (pierdută în tura următoare) și Săgeata Sufletului. Fără destulă esență, primește Săgeata. */
    private void toll(Player player) {
        int fee = n("tollEssence", 2), arrow = n("arrow", 12);
        gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "VAMA LUI CHARON", new Color(200, 160, 255)));
        boolean canPay = player.currentMana >= fee;
        gp.choiceUI.ask("VAMA LUI CHARON", "Plătește drumul, sau Săgeata Sufletului te va găsi.",
                new String[]{"Plătești " + fee + " esență\n(le pierzi tura viitoare)", "Primești Săgeata Sufletului\n(" + arrow + " daune, ignoră armura)"},
                new boolean[]{canPay, true},
                choice -> {
                    if (choice == 0) {
                        player.currentMana -= fee;
                        player.notifyObservers();
                        gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, "-" + fee + " ESENȚĂ", Color.BLUE));
                    } else {
                        hitIgnoringBlock(player, arrow);
                        gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "SĂGEATA SUFLETULUI!", Color.MAGENTA));
                    }
                });
    }

    @Override
    public void takeDamage(Player player, int amount) {
        super.takeDamage(player, amount);
        if (!alive) {   // cade Charon, se risipește și minionul
            for (Enemy e : gp.player.team) if (e != this && e instanceof Enemy_Minion) e.alive = false;
            return;
        }
        if (!phaseTwo && life * 100 <= maxLife * n("phase2AtPercent", 40)) {
            phaseTwo = true;
            gp.battleUI.announcePhase("FAZA II: UMBRA SE ADÂNCEȘTE", "Charon lovește mai tare.", new Color(190, 140, 255));
            gp.fx.phaseChange();
        }
    }
}
