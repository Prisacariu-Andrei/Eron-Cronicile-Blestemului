package Entity.Inamici;

import Main.GamePanel;
import java.awt.*;
import java.util.Random;

public class Enemy_Trol extends Enemy {
    public Enemy_Trol(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Trol de Pestera";
        this.maxLife = 120;
        this.life = maxLife;
        this.attackDamage = 15;
        this.mapSize = 2;
        getImage();
        setNextIntent();
    }

    @Override public void getImage() {
        up1 = setup("/Enemy/Trol");
        down1 = setup("/Enemy/Trol");
        left1 =setup("/Enemy/Trol");
        right1 =setup("/Enemy/Trol");
        up2 = setup("/Enemy/Trol");
        down2 = setup("/Enemy/Trol");
        left2 =setup("/Enemy/Trol");
        right2 = setup("/Enemy/Trol");
        fightStand = setup("/Enemy/Trol");

    }

    @Override
    public void setNextIntent() {
        int chance = new Random().nextInt(100);
        if (life < maxLife * 0.5 && chance < 40) {
            nextAction = EnemyAction.HEAL; // Regenerare
            intentValue = 20;
        } else {
            nextAction = EnemyAction.ATTACK;
            intentValue = attackDamage + new Random().nextInt(6);
        }
    }
}