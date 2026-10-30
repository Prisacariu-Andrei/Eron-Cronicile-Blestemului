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
 * Fenris cel Alb. Cifrele sunt în res/Data/enemies.json -> bosses.fenris: lovitură normală hitMin-hitMax, la fiecare howlEvery-a tură
 * o mușcătură cu urlet (howlDamage, anunțată dinainte); se vindecă cu healFraction din daunele care trec de armură, iar acestea îl Slăbesc pe Eron.
 */
public class Enemy_LupBoss extends Enemy {

    private final Map<String, Object> cfg = Groups.boss("fenris");
    private final Random rand = new Random();
    private int turn = 0;          // câte ture a jucat până acum
    private boolean howling = false;

    public Enemy_LupBoss(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Fenris cel Alb";
        this.maxLife = Json.integer(cfg.get("hp"), 140);
        this.life = maxLife;
        this.attackDamage = Json.integer(cfg.get("hitMin"), 13);
        this.mapSize = 1.8;

        hitBox = new Rectangle(0, 0, (int)(gp.tileSize * mapSize), (int)(gp.tileSize * mapSize));
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        this.isStatic = true;

        getImage();
        setNextIntent();
    }

    @Override
    public void getImage() {
        up1 = setup("/Enemy/LupBossFight");
        up2 = setup("/Enemy/LupBossFight");
        down1 = setup("/Enemy/LupBossFight");
        down2 = setup("/Enemy/LupBossFight");
        left1 = setup("/Enemy/LupBossFight");
        left2 = setup("/Enemy/LupBossFight");
        right1 = setup("/Enemy/LupBossFight");
        right2 = setup("/Enemy/LupBossFight");
        fightStand = setup("/Enemy/LupBossFight");
    }

    @Override
    public void setNextIntent() {
        int every = Math.max(1, Json.integer(cfg.get("howlEvery"), 3));
        nextAction = EnemyAction.ATTACK;
        howling = (turn + 1) % every == 0;   // a treia tură, a șasea...: mușcătura cu urlet, vizibilă dinainte în intenție
        int lo = Json.integer(cfg.get("hitMin"), 13), hi = Math.max(lo, Json.integer(cfg.get("hitMax"), 15));
        intentValue = howling ? Json.integer(cfg.get("howlDamage"), 18) : lo + rand.nextInt(hi - lo + 1);
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns == 0) {
            if (howling) gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 2, "URLET!", Color.ORANGE));
            int lifeBefore = player.life;
            player.damagePlayer(intentValue);
            int unblocked = Math.max(0, lifeBefore - player.life);   // cât a trecut de armură

            if (unblocked > 0) {
                int healAmount = (int) (unblocked * Json.number(cfg.get("healFraction"), 0.5));
                if (healAmount > 0) {
                    this.life = Math.min(this.maxLife, this.life + healAmount);
                    gp.popups.add(new DamagePopUp(popX(), gp.tileSize * 4, "+" + healAmount + " HP", Color.GREEN));
                }
                player.applyStatus(StatusEffect.Type.WEAK, Json.integer(cfg.get("weakTurns"), 1), Json.integer(cfg.get("weakPercent"), 25));
            }

            turn++;
            setNextIntent();
        }
    }

    @Override
    public void takeDamage(Player player, int amount) {
        if(player.areDinteChilimbar){
            amount+=3;
        }
        life -= amount;
        hitFeedback(amount, false);
        // Verificăm dacă viața a ajuns la 0 și inamicul a murit
        if (life <= 0) {
            life = 0;
            alive = false;   // fără asta lupta nu se încheia decât prin arsură
            var quest3 = gp.questManager.getQuestById(3, true);

            if (quest3 != null && quest3.isReceived && !quest3.isCompleted) {
                // Actualizăm progresul quest-ului
                gp.questManager.updateQuestProgress(3, true, 1);

                // Generăm runa
                Entity.Items.Rune runaLupului = Entity.Runes.RuneFactory.createRune("fenris");

                // Adăugăm runa în inventar
                gp.player.giveRune(runaLupului);

                // Afișăm mesajul pe ecran
                gp.ui.message = "Misiune completată: Lupul cel Bătrân";
                gp.ui.messageOn = true;
            }
        }
    }
}