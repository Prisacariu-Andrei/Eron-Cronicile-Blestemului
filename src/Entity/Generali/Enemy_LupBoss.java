package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Player.Player;
import Main.GamePanel;
import Observer.DamagePopUp;

import java.awt.*;
import java.util.Random;

public class Enemy_LupBoss extends Enemy {

    private int damageBuffTurns = 0;
    private boolean isBuffing = false;

    public Enemy_LupBoss(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Fenris cel Alb";
        this.maxLife = 150;
        this.life = maxLife;
        this.attackDamage = 10;
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
        Random rand = new Random();
        int chance = rand.nextInt(100);

        if (damageBuffTurns <= 0 && chance < 30) {
            isBuffing = true;
            nextAction = EnemyAction.BUFF;
            intentValue = 0;
        } else {
            isBuffing = false;
            nextAction = EnemyAction.ATTACK;
            intentValue = attackDamage + (damageBuffTurns > 0 ? 5 : 0);
        }
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns == 0) {
            if (isBuffing) {
                damageBuffTurns = 3;
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "URLET: +5 DMG!", Color.ORANGE));
            } else if (nextAction == EnemyAction.ATTACK) {
                int shieldBefore = player.shield;
                player.damagePlayer(intentValue);
                int unblockedDamage = Math.max(0, intentValue - shieldBefore);

                if (unblockedDamage > 0) {
                    int healAmount = unblockedDamage / 2;
                    if (healAmount > 0) {
                        this.life = Math.min(this.maxLife, this.life + healAmount);
                        gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 4, "+" + healAmount + " HP", Color.GREEN));
                    }
                }
            }

            if (damageBuffTurns > 0 && !isBuffing) {
                damageBuffTurns--;
            }

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