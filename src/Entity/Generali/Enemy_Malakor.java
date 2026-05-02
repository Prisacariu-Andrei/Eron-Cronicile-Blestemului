package Entity.Generali;

import Entity.Inamici.Enemy;
import Entity.Player.Player;
import Main.GamePanel;
import Observer.DamagePopUp;

import java.awt.*;

public class Enemy_Malakor extends Enemy {

    public int currentPhase = 1; // 1 = Regele Vrăjitor, 2 = Cavalerul Morții, 3 = Amurgul Absolut

    // Contor de ture pentru Faza 3 (Judecata Finală)
    private int phase3TurnsLeft = 10;

    public Enemy_Malakor(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Malakor, Monarhul Amurgului";
        this.maxLife = 250; // Viață faza 1
        this.life = maxLife;
        this.attackDamage = 20;
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
        // Asigură-te că imaginea există la calea respectivă
        up1 = setup("/Enemy/MalakorPhaseOne");
        up2 = setup("/Enemy/MalakorPhaseOne");
        down1 = setup("/Enemy/MalakorPhaseOne");
        down2 = setup("/Enemy/MalakorPhaseOne");
        left1 = setup("/Enemy/MalakorPhaseOne");
        left2 = setup("/Enemy/MalakorPhaseOne");
        right1 = setup("/Enemy/MalakorPhaseOne");
        right2 = setup("/Enemy/MalakorPhaseOne");
        if(currentPhase==1) {
            fightStand = setup("/Enemy/MalakorPhaseOne");
        }
        if(currentPhase==2) {
            fightStand = setup("/Enemy/MalakorPhaseTwo");
        }
        if(currentPhase==3) {
            fightStand = setup("/Enemy/MalakorPhaseThree");
        }
    }

    @Override
    public void setNextIntent() {
        switch (currentPhase) {
            case 1 -> {
                // Faza 1: Vrăji și sabotaj (Mana Burn)
                nextAction = EnemyAction.SPECIAL;
                intentValue = 15; // Damage magic
            }
            case 2 -> {
                // Faza 2: Atac fizic brutal și Lifesteal
                nextAction = EnemyAction.ATTACK;
                intentValue = 35; // Atac greu
            }
            case 3 -> {
                // Faza 3: Presiune (Judecata Finală)
                nextAction = EnemyAction.ATTACK;
                intentValue = 30;
            }
        }
    }

    @Override
    public void executeIntent(Player player) {
        if (stunTurns == 0) {

            // --- Verificare tranziție între faze ---
            if (this.life <= 0) {
                switch (currentPhase) {
                    case 1 -> transitionToPhase2();
                    case 2 -> transitionToPhase3();
                    case 3 -> {
                        // Malakor a fost învins de tot
                        gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 2, "MALAKOR A FOST ÎNVINS!", Color.GREEN));
                        //alive = false;
                        return; // Oprește lupta
                    }
                }
            }

            switch (currentPhase) {
                case 1 -> {
                    // Faza 1: Sabotaj
                    player.damagePlayer(intentValue);
                    if (player.currentMana > 0) {
                        player.currentMana -= 1;
                        player.notifyObservers();
                        gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "Mana furată!", Color.BLUE));
                    }
                }
                case 2 -> {
                    // Faza 2: Cavalerul Morții
                    player.damagePlayer(intentValue);
                    // Lifesteal (recuperează 20% din damage-ul dat)
                    this.life = Math.min(maxLife, this.life + intentValue/2);
                    gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 4, "+"+intentValue/2+" HP (Lifesteal)", Color.RED));
                }
                case 3 -> {
                    // Faza 3: Judecata Finală (Scădem numărătoarea inversă)
                    player.damagePlayer(intentValue);
                    phase3TurnsLeft--;

                    if (phase3TurnsLeft <= 0) {
                        // Atac absolut la tura 10, ucide jucătorul instant
                        player.life = 0;
                        player.notifyObservers();
                        gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 2, "JUDECATA FINALĂ", Color.BLACK));
                    } else {
                        gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "Ture rămase: " + phase3TurnsLeft, Color.ORANGE));
                    }
                }
            }

            setNextIntent();
        }
    }

    private void transitionToPhase2() {
        currentPhase = 2;
        name = "Malakor, Cavalerul Morții (Faza 2)";
        maxLife = 300;
        life = maxLife;
        getImage();

        // Tranziție vizuală
        if (gp.battleUI != null) {
            gp.battleUI.isTransitioning = true;
            gp.battleUI.transitionTimer = 0;
            try {
                java.lang.reflect.Field nameField = gp.battleUI.getClass().getDeclaredField("transitionPhaseName");
                nameField.setAccessible(true);
                nameField.set(gp.battleUI, "FAZA 2: CAVALERUL MORȚII");

                java.lang.reflect.Field colorField = gp.battleUI.getClass().getDeclaredField("transitionColor");
                colorField.setAccessible(true);
                colorField.set(gp.battleUI, Color.ORANGE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 2, "SCHIMBARE FAZĂ: CAVALERUL MORȚII", Color.ORANGE));
    }

    private void transitionToPhase3() {
        currentPhase = 3;
        name = "Malakor, Amurgul Absolut";
        maxLife = 400;
        life = maxLife;
        phase3TurnsLeft = 10;
        getImage();

        // Tranziție vizuală
        if (gp.battleUI != null) {
            gp.battleUI.isTransitioning = true;
            gp.battleUI.transitionTimer = 0;
            try {
                java.lang.reflect.Field nameField = gp.battleUI.getClass().getDeclaredField("transitionPhaseName");
                nameField.setAccessible(true);
                nameField.set(gp.battleUI, "FAZA 3: AMURGUL ABSOLUT");

                java.lang.reflect.Field colorField = gp.battleUI.getClass().getDeclaredField("transitionColor");
                colorField.setAccessible(true);
                colorField.set(gp.battleUI, Color.BLACK);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 2, "SCHIMBARE FAZĂ: AMURGUL ABSOLUT", Color.BLACK));
    }

    @Override
    public void takeDamage(int amount) {
        life -= amount;
        gp.popups.add(new DamagePopUp(gp.screenWidth - gp.tileSize * 6, gp.tileSize * 3, "-" + amount, Color.RED));

        // Când viața scade la 0, schimbăm faza instantaneu
        if (life <= 0) {
            if (currentPhase == 1) {
                transitionToPhase2();
                setNextIntent();
                endTurnSafely(); // <-- Sincronizăm tranziția turelor
            }
            else if (currentPhase == 2) {
                transitionToPhase3();
                setNextIntent();
                endTurnSafely(); // <-- Sincronizăm tranziția turelor
            }
            else {
                // De abia în Faza 3 moare de tot
                life = 0;
                alive = false;
            }
        }
    }

    // Metodă auxiliară pentru a curăța mâna și a pregăti faza nouă fără a rula tura inamicului
    private void endTurnSafely() {
        if (gp.player != null) {
            gp.player.isPlayerTurn = false;

            // Golim mâna, dar nu începem tura inamicului
            while (!gp.player.inventory.hand.isEmpty()) {
                gp.player.inventory.discard(gp.player.inventory.hand.removeFirst());
            }

            // Setăm o stare stabilă
            gp.player.shield = 0;
            gp.player.currentMana = gp.player.maxMana;

            // Redăm controlul jucătorului direct pe noua fază
            gp.player.isPlayerTurn = true;
            for(int i = 0; i < 5; i++) {
                gp.player.inventory.drawCard();
            }
            gp.battleUI.commandNum = 0;
            gp.battleUI.announceTurn("FAZA NOUĂ - TURNUL TĂU");
        }
    }
}