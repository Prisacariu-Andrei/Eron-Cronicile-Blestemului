package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Inamici.Groups;
import Entity.Player.Player;
import Main.GamePanel;
import Main.Json;
import Observer.DamagePopUp;

import java.awt.*;
import java.util.Map;
import java.util.Random;

/**
 * Valerius, Cavalerul Neinvins. Cifrele sunt în res/Data/enemies.json -> bosses.valerius.
 *  Faza I  - scut persistent (se întărește în turele în care se apără), lovește cu hit; viața lui stă în spatele scutului.
 *  Faza II - scutul a cedat: viață nouă; a chargeEvery-a tură se încarcă (anunțat în intenție), iar în tura următoare lovește de chargedHits ori.
 */
public class Enemy_Valerius extends Enemy {

    private enum Move { BLOCK, HIT, CHARGE, TRIPLE }

    private final Map<String, Object> cfg = Groups.boss("valerius");
    private final Random rng = new Random();
    private Move move = Move.HIT;
    private int phase = 1;
    private int turn = 0;   // ture jucate în faza II

    public Enemy_Valerius(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Valerius, Cavalerul Neinvins";
        this.maxLife = n("phase1Hp", 200);
        this.life = maxLife;
        this.shield = n("shield", 50);
        this.attackDamage = n("hit", 14);
        this.mapSize = 1.8;

        hitBox = new Rectangle(0, 0, (int)(gp.tileSize * mapSize), (int)(gp.tileSize * mapSize));
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        this.isStatic = true;
        this.persistentShield = true;
        this.ignoreGroupScaling = true;

        getImage();
        setNextIntent();
    }

    @Override
    public void getImage() {
        up1 = setup("/Enemy/Valerius");
        up2 = setup("/Enemy/Valerius");
        down1 = setup("/Enemy/Valerius");
        down2 = setup("/Enemy/Valerius");
        left1 = setup("/Enemy/Valerius");
        left2 = setup("/Enemy/Valerius");
        right1 = setup("/Enemy/Valerius");
        right2 = setup("/Enemy/Valerius");
        fightStand = setup("/Enemy/Valerius");
    }

    private int n(String key, int def) { return Json.integer(cfg.get(key), def); }

    @Override
    public void setNextIntent() {
        intentText = null;
        if (phase == 1) {
            if (rng.nextInt(100) < n("blockChance", 40)) {
                move = Move.BLOCK;
                nextAction = EnemyAction.DEFEND;
                intentValue = n("blockGain", 10);
            } else {
                move = Move.HIT;
                nextAction = EnemyAction.ATTACK;
                intentValue = n("hit", 14);
            }
            return;
        }
        int every = Math.max(2, n("chargeEvery", 3)), t = turn + 1;
        if (t % every == 0) {   // se încarcă: fără daune, dar anunțat
            move = Move.CHARGE;
            nextAction = EnemyAction.SPECIAL;
            intentValue = 0;
            intentText = "⚡ SE ÎNCARCĂ";
        } else if (t > 1 && (t - 1) % every == 0) {   // tura de după încărcare: trei lovituri
            move = Move.TRIPLE;
            nextAction = EnemyAction.ATTACK;
            intentValue = n("chargedHit", 8);
            intentText = "⚔ " + intentValue + "×" + n("chargedHits", 3);
        } else {
            move = Move.HIT;
            nextAction = EnemyAction.ATTACK;
            intentValue = n("hit", 14);
        }
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns != 0) return;
        switch (move) {
            case BLOCK -> {
                shield += intentValue;
                gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "+" + intentValue + " BLOCK", Color.CYAN));
            }
            case HIT -> player.damagePlayer(intentValue);
            case CHARGE -> gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, "SE ÎNCARCĂ!", Color.ORANGE));
            case TRIPLE -> {
                gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, "TREI LOVITURI!", Color.RED));
                for (int i = 0; i < n("chargedHits", 3) && player.life > 0; i++) player.damagePlayer(intentValue);
            }
        }
        if (phase == 2) turn++;
        setNextIntent();
    }

    /** Faza II: scutul a cedat. Viață nouă, fără scut, ritmul cu încărcare; Eron primește o mână nouă. */
    private void enterPhaseTwo() {
        phase = 2;
        maxLife = n("phase2Hp", 200);
        life = maxLife;
        shield = 0;
        persistentShield = false;
        stunTurns = 0;
        turn = 0;
        setNextIntent();
        gp.battleUI.announcePhase("FAZA II: CAVALERUL FĂRĂ SCUT", "Se încarcă la fiecare a treia tură; apoi lovește de trei ori.", Color.RED);
        gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, "SCUTUL A CEDAT!", Color.ORANGE));
        gp.fx.phaseChange();
        restartPlayerTurn();
    }

    @Override
    public void takeDamage(Player player, int amount) {
        if (phase != 1) { super.takeDamage(player, amount); return; }

        int dmg = incomingDamage(player, amount);
        if (shield > 0) {   // scutul persistent absoarbe lovitura
            int absorbed = Math.min(shield, dmg);
            shield -= absorbed;
            dmg -= absorbed;
            gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, absorbed > 0 ? "BLOCAT -" + absorbed : "BLOCAT", new Color(130, 180, 235)));
            hitFeedback(0, true);
            if (shield <= 0) { enterPhaseTwo(); return; }   // scutul s-a spart
            if (dmg <= 0) return;
        }
        life -= dmg;
        gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "-" + dmg, Color.RED));
        hitFeedback(dmg, false);
        if (life <= 0) enterPhaseTwo();   // (fără scut, dar cu viața la zero: tot faza II)
    }
}
