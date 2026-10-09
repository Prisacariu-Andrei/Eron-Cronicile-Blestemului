package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Player.Player;
import Entity.StatusEffect;
import Main.GamePanel;
import Observer.DamagePopUp;

import java.awt.*;
import java.util.Random;

public class Enemy_JeanPlesa extends Enemy {

    private boolean isDodging = false;

    public Enemy_JeanPlesa(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Jean Plesa";
        this.maxLife = 260;
        this.life = maxLife;
        this.attackDamage = 12; // Va da 12 x 2 pe tură
        this.mapSize = 1.3;

        hitBox = new Rectangle(0, 0, (int)(gp.tileSize * mapSize), (int)(gp.tileSize * mapSize));
        hitBoxDefaultX = hitBox.x;
        hitBoxDefaultY = hitBox.y;
        this.isStatic = true;

        getImage();
        setNextIntent();
    }

    @Override
    public void getImage() {
        // Înlocuiește calea cu fișierele corespunzătoare, dacă este cazul
        up1 = flipImage(setup("/Enemy/JeanPlesa"));
        up2 = flipImage(setup("/Enemy/JeanPlesa"));
        down1 = flipImage(setup("/Enemy/JeanPlesa"));
        down2 = flipImage(setup("/Enemy/JeanPlesa"));
        left1 = flipImage(setup("/Enemy/JeanPlesa"));
        left2 = flipImage(setup("/Enemy/JeanPlesa"));
        right1 = flipImage(setup("/Enemy/JeanPlesa"));
        right2 = flipImage(setup("/Enemy/JeanPlesa"));
        fightStand = setup("/Enemy/JeanPlesa");
        battleBackground = setup("/Backgrounds/Citadela_bg");
    }

    @Override
    public void setNextIntent() {
        Random rand = new Random();
        int chance = rand.nextInt(100);

        // Dacă are sub 35% HP, devine extrem de agresiv
        if (life < maxLife * 0.35) {
            nextAction = EnemyAction.ATTACK;
            intentValue = attackDamage * 2; // Atac combo
            isDodging = false;
        }
        // 30% șansă să folosească Eschiva, pregătindu-se să evite un atac
        else if (chance < 30) {
            nextAction = EnemyAction.DEFEND;
            intentValue = 0; // Nu dă damage, dar se ferește
            isDodging = true;
        }
        // Atac normal sau sabotaj (îți scade o esență / 1 Mana)
        else if (chance < 60) {
            nextAction = EnemyAction.ATTACK;
            intentValue = attackDamage;
            isDodging = false;
        } else {
            nextAction = EnemyAction.SPECIAL; // Lovitură cu sabotaj de Mana
            intentValue = 15;
            isDodging = false;
        }
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns == 0) {

            // 1. Verificăm dacă urmează să se eschiveze
            if (nextAction == EnemyAction.DEFEND) {
                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "ESCHIVĂ ACTIVATĂ", Color.YELLOW));
                // Jucătorul trebuie să știe că Jean este imun la următorul atac fizic.
                // Poți gestiona această stare în logica player-ului.
            }
            // 2. Atacul clasic (de două ori)
            else if (nextAction == EnemyAction.ATTACK) {
                // Lovitura 1 marchează ținta: Vulnerabilitate (+50% damage primit, 2 ture); lovitura 2 și turele următoare lovesc mai tare
                player.damagePlayer(intentValue);
                player.applyStatus(StatusEffect.Type.VULNERABLE, 2, 50);
                // Lovitura 2
                player.damagePlayer(intentValue);

                gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 4, "ATAC DUBLU!", Color.RED));
            }
            // 3. Sabotaj (Scade mana și atacă)
            else if (nextAction == EnemyAction.SPECIAL) {
                player.damagePlayer(intentValue);
                player.applyStatus(StatusEffect.Type.VULNERABLE, 2, 50);
                if (player.currentMana > 0) {
                    player.currentMana -= 1;
                    player.notifyObservers();
                    gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 2, "-1 ESENȚĂ", Color.BLUE));
                }
            }

            setNextIntent();
        }
    }
}