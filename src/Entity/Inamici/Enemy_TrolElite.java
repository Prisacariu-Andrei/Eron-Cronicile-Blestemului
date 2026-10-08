package Entity.Inamici;

import Main.GamePanel;
import java.awt.*;
import java.util.Random;

// Mini-boss-ul Abisului (Quest 6): un trol mai mare, care alternează lovituri grele cu "Piele de Piatră"
public class Enemy_TrolElite extends Enemy {
    public Enemy_TrolElite(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Trol de Elită";
        this.maxLife = 120;
        this.life = maxLife;
        this.attackDamage = 14;
        this.mapSize = 2.5;
        hitBox = new Rectangle(0, 0, (int)(gp.tileSize * mapSize), (int)(gp.tileSize * mapSize));
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        this.isStatic = true; // Păzește un loc fix, ca boss-urile
        getImage();
        setNextIntent();
    }

    @Override public void getImage() {
        up1 = setup("/Enemy/Trol");
        down1 = setup("/Enemy/Trol");
        left1 = setup("/Enemy/Trol");
        right1 = setup("/Enemy/Trol");
        up2 = setup("/Enemy/Trol");
        down2 = setup("/Enemy/Trol");
        left2 = setup("/Enemy/Trol");
        right2 = setup("/Enemy/Trol");
        fightStand = setup("/Enemy/Trol");
        battleBackground = setup("/Backgrounds/Abisul_bg");
    }

    @Override
    public void setNextIntent() {
        int chance = new Random().nextInt(100);
        if (chance < 30) {
            nextAction = EnemyAction.DEFEND; // Piele de Piatră
            intentValue = 12;
        } else {
            nextAction = EnemyAction.ATTACK;
            intentValue = attackDamage + new Random().nextInt(7);
        }
    }
}
