package Entity.Inamici;

import Main.GamePanel;
import java.awt.*;
import java.util.Random;

public class Enemy_Cioara extends Enemy {
    public Enemy_Cioara(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Cioara";
        this.maxLife = 30;
        this.life = maxLife;
        this.attackDamage = 16;
        this.speed = 1; // <--- ASIGURĂ-TE CĂ ARE VITEZĂ
        hitBox = new Rectangle(8, 16, gp.tileSize*visualSize, gp.tileSize*visualSize); // Hitbox mai mic decât tile-ul (recomandat)
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        getImage();
        setNextIntent();
    }


    @Override
    public void getImage() {
        // Exemplu: calea folderului tau din res/Inamici
        up1 = setup("/Enemy/Crow");
        up2 = setup("/Enemy/Crow");
        down1 = setup("/Enemy/Crow");
        down2 = setup("/Enemy/Crow");
        left1 = setup("/Enemy/Crow");
        left2 = setup("/Enemy/Crow");
        right1 = setup("/Enemy/Crow");
        right2 = setup("/Enemy/Crow");
        fightStand = setup("/Enemy/CrowFight");
    }

    @Override
    public void setNextIntent() {
        // Logica specifică Lupului: Atacă mult, nu se vindecă
        nextAction = EnemyAction.ATTACK;
        intentValue = attackDamage + new Random().nextInt(5);
    }
}