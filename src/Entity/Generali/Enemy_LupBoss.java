package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Player.Player;
import Main.GamePanel;
import Observer.DamagePopUp;

import java.awt.*;
import java.util.Random;

public class Enemy_LupBoss extends Enemy {

    // Variabile speciale pentru mecanica Boss-ului
    private int damageBuffTurns = 0;
    private boolean isBuffing = false;

    public Enemy_LupBoss(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Fenris cel Alb";
        this.maxLife = 150;
        this.life = maxLife;
        this.attackDamage = 12;
        this.visualSize = 3;

        hitBox = new Rectangle(0, 0, gp.tileSize * visualSize, gp.tileSize * visualSize);
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        this.isStatic = true;

        getImage();
        setNextIntent();
    }

    @Override
    public void getImage() {
        // Asigură-te că imaginile există la calea respectivă
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

        // ABILITATE: Urletul Haitei
        // Dacă nu are deja buff-ul activ, are 30% șansă să se buffeze tura asta
        if (damageBuffTurns <= 0 && chance < 30) {
            isBuffing = true;
            nextAction = EnemyAction.BUFF; // Folosim "DEFEND" pentru iconița de status non-atac
            intentValue = 0;
        }
        // ABILITATE: Mușcătură Adâncă
        else {
            isBuffing = false;
            nextAction = EnemyAction.ATTACK;
            // Dă damage-ul de bază + încă 5 dacă are buff-ul activ
            intentValue = attackDamage + (damageBuffTurns > 0 ? 5 : 0);
        }
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns == 0) {
            if (isBuffing) {
                // Executăm buff-ul: Inamicul nu atacă, dar câștigă buff pentru următoarele 3 ture
                damageBuffTurns = 3;
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "URLET: +5 DMG!", Color.ORANGE));
            }
            else if (nextAction == EnemyAction.ATTACK) {
                // Salvăm scutul jucătorului înainte de atac pentru a calcula cât damage a intrat "pe curat"
                int shieldBefore = player.shield;

                // Jucătorul își ia damage
                player.damagePlayer(intentValue);

                // Calculăm damage-ul neblocat pentru LIFESTEAL (jumătate din damage-ul dat efectiv)
                int unblockedDamage = Math.max(0, intentValue - shieldBefore);

                if (unblockedDamage > 0) {
                    int healAmount = unblockedDamage / 2;
                    if (healAmount > 0) {
                        this.life = Math.min(this.maxLife, this.life + healAmount);
                        // Popup verde ca să vadă jucătorul că lupul s-a vindecat din el
                        gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 4, "+" + healAmount + " HP", Color.GREEN));
                    }
                }
            }

            // Scădem un rând de buff după ce a atacat
            if (damageBuffTurns > 0 && !isBuffing) {
                damageBuffTurns--;
            }

            setNextIntent();
        }
    }
}