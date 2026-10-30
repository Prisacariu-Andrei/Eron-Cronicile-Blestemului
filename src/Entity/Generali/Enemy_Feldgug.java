package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Player.Player;
import Main.GamePanel;
import Observer.DamagePopUp;

import java.awt.*;
import java.util.Random;

public class Enemy_Feldgug extends Enemy {

    // trebuie implementat turele de Rage!!!
    private int rageTurns = 0;

    public Enemy_Feldgug(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Feldgug, Regele Trol";
        this.maxLife = 220;
        this.life = maxLife;
        this.attackDamage = 15;
        this.mapSize = 1.5;

        hitBox = new Rectangle(0, 0, (int)(gp.tileSize * mapSize), (int)(gp.tileSize * mapSize));
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        this.isStatic = true;

        getImage();
        setNextIntent();
    }

    @Override
    public void getImage() {
        // Înlocuiește calea dacă ai imagini specifice pentru Feldgug, sau o păstrezi pe cea de bază
        up1 = setup("/Enemy/FeldgugRegeleTrol");
        up2 = setup("/Enemy/FeldgugRegeleTrol");
        down1 = setup("/Enemy/FeldgugRegeleTrol");
        down2 = setup("/Enemy/FeldgugRegeleTrol");
        left1 = setup("/Enemy/FeldgugRegeleTrol");
        left2 = setup("/Enemy/FeldgugRegeleTrol");
        right1 = setup("/Enemy/FeldgugRegeleTrol");
        right2 = setup("/Enemy/FeldgugRegeleTrol");
        fightStand = setup("/Enemy/FeldgugRegeleTrol");
    }

    @Override
    public void setNextIntent() {
        // Alternăm între atacul simplu și capacitatea de apărare cu lovitură
        Random rand = new Random();
        int chance = rand.nextInt(100);

        // Dacă are sub 30% HP, devine mai agresiv
        if (life < maxLife * 0.3) {
            nextAction = EnemyAction.ATTACK;
            intentValue = attackDamage + 10; // Lovitură Zdrobitoare
        } else if (chance < 40) {
            // trebuie implementat UI de block, atunci cand isi da block hp-ul sau sa devina albastru!
            nextAction = EnemyAction.DEFEND; // Piele de Piatră
            intentValue = 10; // Valoarea scutului
        } else {
            nextAction = EnemyAction.ATTACK;
            intentValue = attackDamage; // Atac normal
        }
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns == 0) {
            // 1. Regenerare pasivă (Sânge de Trol)
            if (life < maxLife) {
                life = Math.min(maxLife, life + 5);
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 2, "+5 HP (Regen)", Color.GREEN));
            }

            // 2. Acțiunea aleasă
            if (nextAction == EnemyAction.DEFEND) {
                // Adăugare Scut Trol
                this.shield += intentValue;
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "+" + intentValue + " BLOCK", Color.CYAN));
            }
            else if (nextAction == EnemyAction.ATTACK) {
                player.damagePlayer(intentValue);
            }

            setNextIntent();
        } else {
            // Dacă este în stare de STUN, nu poate executa acțiunea dar regenerează oricum
            if (life < maxLife) {
                life = Math.min(maxLife, life + 10);
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 2, "+10 HP (Regen)", Color.GREEN));
            }
        }
    }
}