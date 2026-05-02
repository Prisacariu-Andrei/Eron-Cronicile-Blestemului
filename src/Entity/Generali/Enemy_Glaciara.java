package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Player.Player;
import Main.GamePanel;
import Observer.DamagePopUp;

import java.awt.*;
import java.util.Random;

public class Enemy_Glaciara extends Enemy {

    public Enemy_Glaciara(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Glaciara";
        this.maxLife = 400;
        this.life = maxLife;
        this.attackDamage = 25; // Damage de bază ridicat
        this.visualSize = 6;

        hitBox = new Rectangle(0, 0, gp.tileSize * visualSize, gp.tileSize * visualSize);
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        this.isStatic = true;

        getImage();
        setNextIntent();
    }

    @Override
    public void getImage() {
        // Înlocuiește calea cu fișierele corespunzătoare, dacă este cazul
        up1 = setup("/Enemy/GlaciaraPhaseOne");
        up2 = setup("/Enemy/GlaciaraPhaseOne");
        down1 = setup("/Enemy/GlaciaraPhaseOne");
        down2 = setup("/Enemy/GlaciaraPhaseOne");
        left1 = setup("/Enemy/GlaciaraPhaseOne");
        left2 = setup("/Enemy/GlaciaraPhaseOne");
        right1 = setup("/Enemy/GlaciaraPhaseOne");
        right2 = setup("/Enemy/GlaciaraPhaseOne");
        fightStand = setup("/Enemy/GlaciaraPhaseOne");
    }

    @Override
    public void setNextIntent() {
        Random rand = new Random();
        int chance = rand.nextInt(100);

        // Dacă are sub 30% HP, devine și mai periculoasă
        if (life < maxLife * 0.3) {
            nextAction = EnemyAction.ATTACK;
            intentValue = 35; // Atac de gheață devastator
        }
        // 35% șansă să folosească Suflu Înghețat (Stun + Damage)
        else if (chance < 35) {
            nextAction = EnemyAction.SPECIAL; // Suflu Înghețat
            intentValue = 20;
        }
        // 35% șansă să aplice Furtună de Zăpadă (Frostbite / Burn)
        else if (chance < 70) {
            nextAction = EnemyAction.DEFEND; // Efect de mediu / Burn
            intentValue = 5;
        }
        // Atac fizic sau de energie normal
        else {
            nextAction = EnemyAction.ATTACK;
            intentValue = attackDamage;
        }
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns == 0) {

            // 1. Suflu Înghețat (aplică Stun și damage)
            if (nextAction == EnemyAction.SPECIAL) {
                player.damagePlayer(intentValue);
                // Adăugăm un turn de stun pentru jucător
                player.stunTurns += 1;
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "CONGELAT! (STUN)", Color.CYAN));
            }
            // 2. Furtună de Zăpadă (Frostbite - ignoră scutul și scade 5 HP)
            else if (nextAction == EnemyAction.DEFEND) {
                player.life = Math.max(0, player.life - intentValue);
                player.notifyObservers();
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 2, "FROSTBITE! -5 HP", Color.BLUE));
            }
            // 3. Atac de bază / Puternic
            else if (nextAction == EnemyAction.ATTACK) {
                player.damagePlayer(intentValue);
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 4, "ATAC GLACIAL", Color.RED));
            }

            setNextIntent();
        }
    }
}