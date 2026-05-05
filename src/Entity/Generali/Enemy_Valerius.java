package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Player.Player;
import Main.GamePanel;
import Observer.DamagePopUp;

import java.awt.*;
import java.util.Random;

public class Enemy_Valerius extends Enemy {

    private boolean isCounterAttacking = false;

    public Enemy_Valerius(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Valerius, Cavalerul Neinvins";
        this.maxLife = 280;
        this.life = maxLife;
        this.attackDamage = 18;
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
        // Asigură-te că imaginea există la calea respectivă sau o poți schimba
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

    @Override
    public void setNextIntent() {
        Random rand = new Random();
        int chance = rand.nextInt(100);

        // Dacă are sub 40% HP, devine foarte agresiv
        if (life < maxLife * 0.4) {
            nextAction = EnemyAction.ATTACK;
            intentValue = attackDamage + 7; // Lovitură mai puternică
            isCounterAttacking = false;
        }
        // 40% șansă să folosească Scutul Divin și Riposta
        else if (chance < 90) {
            nextAction = EnemyAction.DEFEND;
            intentValue = 30; // Scutul
            isCounterAttacking = true;
        } else {
            nextAction = EnemyAction.ATTACK;
            intentValue = attackDamage;
            isCounterAttacking = false;
        }
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns == 0) {

            // 1. Verificăm dacă a fost activat contraatacul și dacă jucătorul a spart scutul
            if (nextAction == EnemyAction.DEFEND) {
                // Își pune scutul
                this.shield += intentValue;
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "+" + intentValue + " BLOCK", Color.CYAN));
            }
            else if (nextAction == EnemyAction.ATTACK) {
                // Verificăm contraatacul (dacă a avut scut și a rămas ceva din el până la începutul turei lui)
                if (isCounterAttacking && this.shield > 0) {
                    int counterDamage = intentValue * 2;
                    gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 2, "RIPOSTĂ! DMG X2", Color.RED));
                    player.damagePlayer(counterDamage);
                } else {
                    // Atac normal
                    player.damagePlayer(intentValue);
                }

                // Resetăm scutul și starea după atac
                this.shield = 0;
                isCounterAttacking = false;
            }

            setNextIntent();
        }
    }
}