package Entity.Inamici;

import Main.GamePanel;
import java.awt.*;
import java.util.Random;

public class Enemy_LupElite extends Enemy {

    public Enemy_LupElite(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Lup de Elită";
        this.maxLife = 100;
        this.life = maxLife;
        this.attackDamage = 15;
        this.speed = 1; // <--- ASIGURĂ-TE CĂ ARE VITEZĂ
        hitBox = new Rectangle(8, 16, (int)(gp.tileSize * mapSize), (int)(gp.tileSize * mapSize)); // Hitbox mai mic decât tile-ul (recomandat)
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        // Poti schimba hitBox-ul daca e mai mare
        getImage();
        setNextIntent();
    }

    @Override
    public void getImage() {
        // Exemplu: calea folderului tau din res/Inamici
        up1 = setup("/Enemy/LupElite");
        up2 = setup("/Enemy/LupElite");
        down1 = setup("/Enemy/LupElite");
        down2 = setup("/Enemy/LupElite");
        left1 = setup("/Enemy/LupElite");
        left2 = setup("/Enemy/LupElite");
        right1 = setup("/Enemy/LupElite");
        right2 = setup("/Enemy/LupElite");
        fightStand = setup("/Enemy/LupEliteFight");

    }
    @Override
    public void setNextIntent() {
        // Logica specifică Lupului: Atacă mult, nu se vindecă
        nextAction = EnemyAction.ATTACK;
        intentValue = attackDamage + new Random().nextInt(5);
    }
}