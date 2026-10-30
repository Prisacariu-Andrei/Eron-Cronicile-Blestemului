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
        this.maxLife = 450;
        this.life = maxLife;
        this.attackDamage = 18;
        this.mapSize = 1.8;

        hitBox = new Rectangle(0, 0, (int)(gp.tileSize * mapSize), (int)(gp.tileSize * mapSize));
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        this.isStatic = true;
        this.persistentShield = true;

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

    @Override
    public void setNextIntent() {
        Random rand = new Random();
        int chance = rand.nextInt(100);

        if (life < maxLife * 0.4) {
            nextAction = EnemyAction.ATTACK;
            intentValue = attackDamage + 7;
            isCounterAttacking = false;
        } else if (chance < 90) {
            nextAction = EnemyAction.DEFEND;
            intentValue = 30;
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

            if (nextAction == EnemyAction.DEFEND) {
                this.shield += intentValue;
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "+" + intentValue + " BLOCK", Color.CYAN));
            } else if (nextAction == EnemyAction.ATTACK) {
                if (isCounterAttacking && this.shield > 0) {
                    int counterDamage = intentValue * 2;
                    gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 2, "RIPOSTĂ! DMG X2", Color.RED));
                    player.damagePlayer(counterDamage);
                } else {
                    player.damagePlayer(intentValue);
                }
                isCounterAttacking = false;
            }

            setNextIntent();
        }
    }

    @Override
    public void takeDamage(Player player,int amount) {
        super.takeDamage(player,amount);

        if (life <= 0) {
            life = 0;
            alive = false;
        }
    }
}