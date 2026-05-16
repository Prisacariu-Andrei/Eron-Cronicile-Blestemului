package Entity.Inamici;

import Main.GamePanel;
import java.awt.*;
import java.util.Random;

public class Enemy_Lup extends Enemy {
    public Enemy_Lup(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Lup";
        this.maxLife = 80;
        this.life = maxLife;
        this.attackDamage = 12;
        this.speed = 1; // <--- ASIGURĂ-TE CĂ ARE VITEZĂ
        getImage();
        setNextIntent();
        hitBox = new Rectangle(8, 16, (int)(gp.tileSize * mapSize), (int)(gp.tileSize * mapSize)); // Hitbox mai mic decât tile-ul (recomandat)
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
    }

    @Override
    public void getImage() {
        // Exemplu: calea folderului tau din res/Inamici
        up1 = setup("/Enemy/Wolf");
        up2 = setup("/Enemy/Wolf");
        down1 = setup("/Enemy/Wolf");
        down2 = setup("/Enemy/Wolf");
        left1 = setup("/Enemy/Wolf");
        left2 = setup("/Enemy/Wolf");
        right1 = setup("/Enemy/Wolf");
        right2 = setup("/Enemy/Wolf");
        fightStand = setup("/Enemy/WolfFight");
        battleBackground = setup("/Enemy/Barlog");

    }

    @Override
    public void setNextIntent() {
        // Logica specifică Lupului: Atacă mult, nu se vindecă
        nextAction = EnemyAction.ATTACK;
        intentValue = attackDamage + new Random().nextInt(5);
    }
}