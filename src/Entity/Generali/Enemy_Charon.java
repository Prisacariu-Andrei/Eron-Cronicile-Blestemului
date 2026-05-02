package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Player.Player;
import Main.GamePanel;
import Observer.DamagePopUp;

import java.awt.*;
import java.util.Random;

public class Enemy_Charon extends Enemy {

    public Enemy_Charon(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Charon, Străjerul Umbrelor fără Tihnă";
        this.maxLife = 500;
        this.life = maxLife;
        this.attackDamage = 30;
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
        // Înlocuiește calea cu fișierele corespunzătoare
        up1 = setup("/Enemy/Charon");
        up2 = setup("/Enemy/Charon");
        down1 = setup("/Enemy/Charon");
        down2 = setup("/Enemy/Charon");
        left1 = setup("/Enemy/Charon");
        left2 = setup("/Enemy/Charon");
        right1 = setup("/Enemy/Charon");
        right2 = setup("/Enemy/Charon");
        fightStand = setup("/Enemy/Charon");
    }

    @Override
    public void setNextIntent() {
        Random rand = new Random();
        int chance = rand.nextInt(100);

        // Dacă are sub 30% HP, folosește atacuri eterice foarte puternice
        if (life < maxLife * 0.3) {
            nextAction = EnemyAction.ATTACK;
            intentValue = 40; // Atac greu ce trece prin armură
        }
        // 30% șansă să folosească Bântuirea (adaugă o carte blestemată)
        else if (chance < 30) {
            nextAction = EnemyAction.DEFEND;
            intentValue = 10; // Daune directe, ignoră scutul + blestem
        }
        // 40% șansă să folosească Săgeata Sufletului (Atac care ignoră scutul)
        else if (chance < 70) {
            nextAction = EnemyAction.SPECIAL;
            intentValue = 20;
        }
        // Atac fizic sau de bază
        else {
            nextAction = EnemyAction.ATTACK;
            intentValue = attackDamage;
        }
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns == 0) {

            // 1. Săgeata Sufletului (Ignoră complet scutul jucătorului)
            if (nextAction == EnemyAction.SPECIAL) {
                player.life = Math.max(0, player.life - intentValue);
                player.notifyObservers();
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "SĂGEATA SUFLETULUI! -20 HP", Color.MAGENTA));
            }
            // 2. Bântuire (Adaugă carte blestemată în pachet / efect de 10 daune ignorând scutul)
            else if (nextAction == EnemyAction.DEFEND) {
                player.life = Math.max(0, player.life - intentValue);
                player.notifyObservers();
                // Notificare suplimentară pentru jucător (efectul de blestem / pachet)
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 2, "BÂNTUIRE! -10 HP (Ignoră Scutul)", Color.DARK_GRAY));
            }
            // 3. Atac fizic de bază
            else if (nextAction == EnemyAction.ATTACK) {
                player.damagePlayer(intentValue);
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 4, "ATAC ETERIC", Color.RED));
            }

            setNextIntent();
        }
    }
}