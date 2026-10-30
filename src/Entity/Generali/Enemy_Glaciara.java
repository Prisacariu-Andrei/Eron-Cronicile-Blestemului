package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Inamici.EnemyFactory;
import Entity.Inamici.Groups;
import Entity.Items.Card;
import Entity.Player.Player;
import Main.GamePanel;
import Main.Json;
import Observer.DamagePopUp;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Glaciara. Cifrele sunt în res/Data/enemies.json -> bosses.glaciara: lovitură de gheață, Suflu (daune + o carte înghețată în mâna
 * următoare a lui Eron), Degerătură (ignoră armura). La a 5-a lovitură primită e amețită o tură și invocă un Minion de Zăpadă,
 * pe care trebuie să-l ucizi primul (cât trăiește, ea nu poate fi rănită). Sub 50% viață e în faza II: lovește mai tare.
 */
public class Enemy_Glaciara extends Enemy {

    private enum Move { ICE, BREATH, FROST }

    private final Map<String, Object> cfg = Groups.boss("glaciara");
    private final Random rng = new Random();
    private Move move = Move.ICE;
    private boolean phaseTwo = false;
    private boolean freezePending = false;   // Suflul a lovit: o carte din mâna următoare se îngheață
    private int hitsTaken = 0;
    private Enemy minion;

    public Enemy_Glaciara(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Glaciara, Stăpânul Inelului de Gheață";
        this.maxLife = Json.integer(cfg.get("hp"), 225);
        this.life = maxLife;
        this.attackDamage = byPhase("iceHit", 16, 20);
        this.mapSize = 3;

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
        up1 = setup("/Enemy/GlaciaraPhaseOne");
        up2 = setup("/Enemy/GlaciaraPhaseOne");
        down1 = setup("/Enemy/GlaciaraPhaseOne");
        down2 = setup("/Enemy/GlaciaraPhaseOne");
        left1 = setup("/Enemy/GlaciaraPhaseOne");
        left2 = setup("/Enemy/GlaciaraPhaseOne");
        right1 = setup("/Enemy/GlaciaraPhaseOne");
        right2 = setup("/Enemy/GlaciaraPhaseOne");
        fightStand = setup("/Enemy/GlaciaraPhaseOne");
    }

    /** Valoarea din perechea [faza I, faza II] a cheii. */
    private int byPhase(String key, int def1, int def2) {
        List<Object> l = Json.list(cfg.get(key));
        if (l.isEmpty()) return phaseTwo ? def2 : def1;
        return Json.integer(l.get(Math.min(phaseTwo ? 1 : 0, l.size() - 1)), phaseTwo ? def2 : def1);
    }

    @Override
    public void setNextIntent() {
        intentText = null;
        Map<String, Object> w = Json.map(cfg.get("weights"));
        int wIce = Json.integer(w.get("ice"), 40), wBreath = Json.integer(w.get("breath"), 30), wFrost = Json.integer(w.get("frostbite"), 30);
        int r = rng.nextInt(Math.max(1, wIce + wBreath + wFrost));
        if (r < wIce) {
            move = Move.ICE;
            nextAction = EnemyAction.ATTACK;
            intentValue = byPhase("iceHit", 16, 20);
        } else if (r < wIce + wBreath) {
            move = Move.BREATH;
            nextAction = EnemyAction.SPECIAL;
            intentValue = Json.integer(cfg.get("breathDamage"), 10);
            intentText = "❄ " + intentValue + " + carte înghețată";
        } else {
            move = Move.FROST;
            nextAction = EnemyAction.SPECIAL;
            intentValue = byPhase("frostbite", 5, 7);
            intentText = "❄ " + intentValue + " (ignoră armura)";
        }
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns != 0) return;
        switch (move) {
            case ICE -> {
                player.damagePlayer(intentValue);
                gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 4, "ATAC GLACIAL", Color.RED));
            }
            case BREATH -> {
                player.damagePlayer(intentValue);
                freezePending = true;
                gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "SUFLU ÎNGHEȚAT!", Color.CYAN));
            }
            case FROST -> {
                hitIgnoringBlock(player, intentValue);
                gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, "DEGERĂTURĂ! -" + intentValue + " HP", Color.BLUE));
            }
        }
        setNextIntent();
    }

    /** Mâna nouă a lui Eron: după Suflu, o carte la întâmplare e înghețată cât ține tura. */
    @Override
    public void onPlayerTurnStart(Player player) {
        if (!freezePending || !alive) return;
        freezePending = false;
        List<Card> free = new ArrayList<>();
        for (Card c : player.inventory.hand) if (!c.frozen) free.add(c);
        if (free.isEmpty()) return;
        free.get(rng.nextInt(free.size())).frozen = true;
        gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, "O carte a înghețat!", Color.CYAN));
    }

    @Override
    public void takeDamage(Player player, int amount) {
        if (minion != null && minion.alive) {   // minionul trebuie ucis primul
            gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 3, "Minionul o apără!", new Color(160, 230, 255)));
            hitFeedback(0, true);
            return;
        }
        super.takeDamage(player, amount);
        if (!alive) { if (minion != null) minion.alive = false; return; }   // cade ea, se risipește și minionul

        if (!phaseTwo && life * 100 <= maxLife * Json.integer(cfg.get("phase2AtPercent"), 50)) {
            phaseTwo = true;
            gp.battleUI.announcePhase("FAZA II: GERUL SE ADÂNCEȘTE", "Glaciara lovește mai tare.", Color.CYAN);
            gp.fx.phaseChange();
        }
        if (++hitsTaken >= Json.integer(cfg.get("hitsToStun"), 5)) {
            hitsTaken = 0;
            stunTurns = Json.integer(cfg.get("stunTurns"), 1);
            gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, "AMEȚITĂ!", Color.CYAN));
            summonMinion();
        }
    }

    private void summonMinion() {
        minion = EnemyFactory.createEnemy("Minion de Zăpadă", gp, 0, 0);
        if (minion == null) return;
        minion.battleBackground = battleBackground;
        gp.player.team.add(minion);
        gp.player.currentEnemy = minion;   // ținta e minionul: ea nu poate fi rănită până nu cade
        gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, "INVOCĂ UN MINION!", new Color(160, 230, 255)));
        gp.fx.questSound(false);
    }
}
