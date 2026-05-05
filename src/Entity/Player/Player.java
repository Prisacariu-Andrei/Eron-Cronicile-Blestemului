package Entity.Player;

import Entity.Entity;
import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Cards.GeamatulEcoului;
import Entity.Generali.Enemy_Malakor;
import Main.GamePanel;
import Main.KeyHandler;
import Observer.Observer;
import Observer.DamagePopUp;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Player extends Entity {

    // DEPENDENȚE
    KeyHandler keyH;
    public Inventory inventory = new Inventory();
    private final List<Observer> observers = new ArrayList<>();

    // COORDONATE ECRAN (CENTRALE)
    public  int screenX;
    public  int screenY;

    // STATS DE BAZĂ (EXPLORARE)
    public final int NORMAL_SPEED = 4;
    public float exploreMana;
    public final float maxExploreMana = 100f;
    public float manaRegen = 0.25f;

    // DASH SISTEM
    public boolean isDashing = false;
    public int dashSpeed = 16;
    public int dashCounter = 0;
    public final int DASH_DURATION = 20;
    public final int DASH_COST = 30;

    // STATS LUPTĂ (CĂRȚI)
    public int maxMana = 3;
    public int currentMana;
    public boolean isPlayerTurn = true;
    public Enemy currentEnemy;

    // --- VARIABILE PENTRU ABILITĂȚI NOI ---
    public Card lastCardPlayed = null;
    public int voltCount = 0;
    public boolean isInvulnerable = false;
    // -------------------------------------

    // LOGICĂ EVENIMENTE / COLIZIUNE
    int lastTileX = -1, lastTileY = -1;
    private boolean hasTakenHeal = false;

    public Player(GamePanel gp, KeyHandler keyH) {
        this.gp = gp;
        this.keyH = keyH;

        screenX = gp.screenWidth / 2 - (gp.tileSize / 2);
        screenY = gp.screenHeight / 2 - (gp.tileSize / 2);

        hitBox = new Rectangle(gp.tileSize / 4, gp.tileSize / 2 + 1, gp.tileSize / 2, gp.tileSize / 2);

        setDefaultValues();
        getPlayerImage();
    }

    public void setDefaultValues() {
        worldX = gp.tileSize * 12;
        worldY = gp.tileSize * 12;
        speed = NORMAL_SPEED;
        direction = "down";
        spriteNum = 1;

        name = "Eron";
        life = maxLife;
        currentMana = maxMana;
        exploreMana = maxExploreMana;

        // Inițializare pachet
        this.inventory.cards = CardManager.getStartingDeck();
    }

    // --- LOGICA PRINCIPALĂ (UPDATE) ---
    public void update() {
        updateStatus();
        if (gp.gameState == GamePanel.GameState.PLAY) {
            handleExploration();
        }
        else if (gp.gameState == GamePanel.GameState.BATTLE) {
            handleBattleInput();
        }
        else if (gp.gameState == GamePanel.GameState.REWARD) {
            handleRewardInput();
        }
    }

    private void updateStatus() {
        if (exploreMana < maxExploreMana) exploreMana += manaRegen;

        if (isDashing) {
            speed = dashSpeed;
            dashCounter++;
            if (dashCounter > DASH_DURATION) {
                isDashing = false;
                speed = NORMAL_SPEED;
            }
        } else {
            speed = NORMAL_SPEED;
        }
    }

    private void handleExploration() {
        if (worldX < 0) worldX = 0;
        if (worldY < 0) worldY = 0;
        if (worldX > (gp.maxWorldCol * gp.tileSize) - gp.tileSize) worldX = (gp.maxWorldCol * gp.tileSize) - gp.tileSize;
        if (worldY > (gp.maxWorldRow * gp.tileSize) - gp.tileSize) worldY = (gp.maxWorldRow * gp.tileSize) - gp.tileSize;


        if (keyH.dashPressed && !isDashing && exploreMana >= DASH_COST) {
            isDashing = true;
            dashCounter = 0;
            exploreMana -= 1; // DASH_COST
        }
        if (keyH.upPressed || keyH.downPressed || keyH.leftPressed || keyH.rightPressed) {
            updateMovement();
        }
        gp.eHandler.checkEvent();
        checkEnemyCollision();
    }

    private void updateMovement() {
        if (keyH.upPressed) direction = "up";
        if (keyH.downPressed) direction = "down";
        if (keyH.leftPressed) direction = "left";
        if (keyH.rightPressed) direction = "right";

        collisionOn = false;
        //gp.cChecher.checkTile(this);

        if (!collisionOn) {
            switch (direction) {
                case "up": worldY -= speed; break;
                case "down": worldY += speed; break;
                case "left": worldX -= speed; break;
                case "right": worldX += speed; break;
            }
            checkTileEffect();
        } else if (isDashing) {
            isDashing = false;
        }

        spriteCounter++;
        if (spriteCounter > 12) {
            spriteNum = (spriteNum == 1) ? 2 : 1;
            spriteCounter = 0;
        }
    }

    private void handleBattleInput() {
        if (this.life <= 0) {
            return; // Oprește orice acțiune dacă player-ul este mort
        }

        if (isPlayerTurn) {
            if (gp.keyH.enterPressed) {
                playSelectedCard();
                gp.keyH.enterPressed = false;
            }
            if (gp.keyH.tPressed) {
                endTurn();
                gp.keyH.tPressed = false;
            }
        }
    }

    private void handleRewardInput() {
        if (gp.keyH.leftPressed) {
            gp.keyH.leftPressed = false;
            gp.rewardUI.commandNum--;
            if (gp.rewardUI.commandNum < 0) gp.rewardUI.commandNum = gp.rewardUI.rewardCards.size() - 1;
        }
        if (gp.keyH.rightPressed) {
            gp.keyH.rightPressed = false;
            gp.rewardUI.commandNum++;
            if (gp.rewardUI.commandNum >= gp.rewardUI.rewardCards.size()) gp.rewardUI.commandNum = 0;
        }

        if (gp.keyH.enterPressed) {
            gp.keyH.enterPressed = false;
            Card selected = gp.rewardUI.getSelectedCard();
            if (selected != null) {
                inventory.addCardToDeck(selected.name);
                gp.rewardUI.commandNum = 0;
                gp.gameState = GamePanel.GameState.PLAY;
            }
        }

        if (gp.keyH.escPressed) {
            gp.keyH.escPressed = false;
            gp.rewardUI.commandNum = 0;
            gp.gameState = GamePanel.GameState.PLAY;
        }
    }

    // --- SISTEM DE LUPTĂ & CĂRȚI ---
    // --- SISTEM DE LUPTĂ & CĂRȚI ---
    public void playSelectedCard() {
        if (inventory.hand.isEmpty()) return;

        int index = gp.battleUI.commandNum;
        if (index >= 0 && index < inventory.hand.size()) {
            Card card = inventory.hand.get(index);

            if (currentMana >= card.manaCost) {
                currentMana -= card.manaCost;

                card.play(this, currentEnemy);

                if (!(card instanceof GeamatulEcoului)) {
                    lastCardPlayed = card;
                }

                inventory.discard(inventory.hand.remove(index));

                // Bătălia se câștigă doar dacă inamicul are 0 HP ȘI alive este false (adică a trecut de faza 3)
                if (currentEnemy != null && currentEnemy.life <= 0 && !currentEnemy.alive) {
                    winBattle();
                }

                if (gp.battleUI.commandNum >= inventory.hand.size() && gp.battleUI.commandNum > 0) {
                    gp.battleUI.commandNum = inventory.hand.size() - 1;
                }
            }
        }
    }

    public void endTurn() {
        isPlayerTurn = false;
        isInvulnerable = false;

        // Resetarea scutului se face doar dacă inamicul NU este un boss
        if (currentEnemy != null) {
            String enemyName = currentEnemy.name;
            if (!"Fenris cel Alb".equals(enemyName) && !"Valerius, Cavalerul Neinvins".equals(enemyName)) {
                currentEnemy.shield = 0;
            }
        }

        // 1. Curățăm mâna jucătorului
        while(!inventory.hand.isEmpty()) {
            inventory.discard(inventory.hand.removeFirst());
        }
        gp.battleUI.commandNum = 0;

        // 2. ACTUALIZĂM STATUSURILE INAMICULUI
        if (currentEnemy != null) {
            currentEnemy.updateStatuses();

            // Verificăm dacă inamicul a murit definitiv din cauza otrăvii/arsurii etc.
            if (currentEnemy.life <= 0 && !currentEnemy.alive) {
                Timer deathTimer = new Timer(800, e -> { winBattle(); ((Timer)e.getSource()).stop(); });
                deathTimer.setRepeats(false); deathTimer.start();
                return;
            }
        }

        // 3. TURA INAMICULUI
        gp.battleUI.announceTurn("TURNUL INAMICULUI");

        Timer enemyTurnTimer = new Timer(1500, e -> {
            if (currentEnemy != null && currentEnemy.alive) {
                currentEnemy.executeIntent(this);
                finishEnemyTurn();
            }
            ((Timer)e.getSource()).stop();
        });
        enemyTurnTimer.setRepeats(false);
        enemyTurnTimer.start();

        this.currentMana = this.maxMana;
    }

    private void finishEnemyTurn() {
        if (this.life <= 0) {
            return; // Dacă jucătorul a murit, oprim execuția timerelor
        }

        gp.battleUI.commandNum = 0;

        this.updateStatuses();

        Timer t1 = new Timer(1000, e -> {
            gp.battleUI.announceTurn("TURNUL TĂU");
            ((Timer)e.getSource()).stop();
        });
        t1.start();

        Timer t2 = new Timer(1500, e -> {
            isPlayerTurn = true;
            for(int i = 0; i < 5; i++) inventory.drawCard();
            ((Timer)e.getSource()).stop();
        });
        t2.start();
        shield = 0;

        if (currentEnemy != null && currentEnemy.stunTurns > 0) {
            currentEnemy.stunTurns -= 1;
        }
    }

    // --- UTILITARE & COLIZIUNI ---
    public void damagePlayer(int amount) {
        if (isInvulnerable) {
            gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, "IMUN", Color.WHITE));
            return;
        }

        if (this.shield < amount) {
            int remaining = amount - shield;
            shield = 0;
            life -= remaining;
            gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, "-" + remaining + " HP", Color.RED));
        } else {
            shield -= amount;
            gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, "-" + amount + " BLOCK", Color.GRAY));
        }

        if (this.life <= 0) {
            this.life = 0;

            // 1. Oprește acțiunile jucătorului
            isPlayerTurn = false;

            // 2. Oprește inamicul activ
            if (currentEnemy != null) {
                currentEnemy = null;
            }

            // 3. Setează starea pe ecranul de moarte (Death Screen)
            gp.gameState = GamePanel.GameState.DEATH;
        }
        notifyObservers();
    }

    public void addShield(int amount) {
        this.shield += amount;
        gp.popups.add(new DamagePopUp(gp.tileSize * 3, gp.tileSize * 2, "+" + amount + " BLOCK", Color.CYAN));
        notifyObservers();
    }

    public void addMana(int amount) {
        this.currentMana += amount;
        gp.popups.add(new DamagePopUp(gp.tileSize * 3, gp.tileSize * 2, "+" + amount + " ESENTA", Color.BLUE));
        notifyObservers();
    }

    private void checkEnemyCollision() {
        Rectangle pRect = new Rectangle(worldX + hitBox.x, worldY + hitBox.y, hitBox.width, hitBox.height);
        for (int i = 0; i < gp.enemy.length; i++) {
            if (gp.enemy[i] != null && gp.enemy[i].alive) {
                Rectangle eRect = new Rectangle(gp.enemy[i].worldX + gp.enemy[i].hitBox.x,
                        gp.enemy[i].worldY + gp.enemy[i].hitBox.y,
                        gp.enemy[i].hitBox.width, gp.enemy[i].hitBox.height);
                if (pRect.intersects(eRect)) {
                    this.currentEnemy = gp.enemy[i];
                    gp.gameState = GamePanel.GameState.BATTLE;
                    startBattle();
                    break;
                }
            }
        }
    }

    public void interactNPC() {
        int oldX = hitBox.x, oldY = hitBox.y, oldW = hitBox.width, oldH = hitBox.height;

        hitBox.x -= gp.tileSize;
        hitBox.y -= gp.tileSize;
        hitBox.width += gp.tileSize * 2;
        hitBox.height += gp.tileSize * 2;

        int npcIndex = gp.cChecher.checkEntity(this, gp.npc);

        hitBox.x = oldX;
        hitBox.y = oldY;
        hitBox.width = oldW;
        hitBox.height = oldH;

        if (npcIndex != 999) {
            gp.gameState = GamePanel.GameState.DIALOGUE;
            gp.npc[npcIndex].speak();
        }
    }

    public void checkTileEffect() {
        int col = (worldX + hitBox.x + hitBox.width/2) / gp.tileSize;
        int row = (worldY + hitBox.y + hitBox.height/2) / gp.tileSize;
        if (col >= 0 && col < gp.maxWorldCol && row >= 0 && row < gp.maxWorldRow) {
            int tileNum = gp.tileM.mapTileNum[col][row];
            if (tileNum == 38 && (col != lastTileX || row != lastTileY)) {
                lastTile_Damage_Logic: lastTileX = col; lastTileY = row;
                damagePlayer(20);
            }
            if (tileNum == 37 && !hasTakenHeal) {
                this.life = Math.min(this.life + 50, maxLife);
                hasTakenHeal = true;
                notifyObservers();
            }
        }
    }

    public void startBattle() {
        this.isPlayerTurn = true;
        this.currentMana = this.maxMana;
        this.shield = 0;
        this.voltCount = 0;
        this.lastCardPlayed = null;
        this.inventory.prepareForBattle();
        if (currentEnemy != null) currentEnemy.setNextIntent();
    }

    public void winBattle() {
        this.life = Math.min(this.life + 5, maxLife);
        notifyObservers();
        if (currentEnemy != null) {
            currentEnemy.alive = false;
            currentEnemy = null;
        }
        this.inventory.resetDeckAfterBattle();
        gp.rewardUI.generateRewards();
        gp.gameState = GamePanel.GameState.REWARD;
    }

    public void resetPlayer() {
        setDefaultValues();
        inventory.hand.clear();
        inventory.discardPile.clear();
        for(int i=0; i<gp.enemy.length; i++) if(gp.enemy[i]!=null) { gp.enemy[i].alive = true; gp.enemy[i].life = gp.enemy[i].maxLife; }
        notifyObservers();
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void notifyObservers() {
        for (Observer ob : observers) ob.OnNotify(this.life + "/" + this.maxLife);
    }

    public void getPlayerImage() {
        try {
            up1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Eron_up1.png")));
            up2 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Eron_up2.png")));
            down1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Eron_down1.png")));
            down2 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Eron_down2.png")));
            left1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Eron_left1.png")));
            left2 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Eron_left2.png")));
            right1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Eron_right1.png")));
            right2 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Eron_right2.png")));
            fightStand = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/Eron_Battle.png")));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g2) {

        int tempScreenX = screenX;
        int tempScreenY = screenY;

        if (worldX < screenX) {
            tempScreenX = worldX;
        }
        if (gp.screenWidth - screenX > (gp.maxWorldCol * gp.tileSize) - worldX) {
            tempScreenX = gp.screenWidth - ((gp.maxWorldCol * gp.tileSize) - worldX);
        }
        if (worldY < screenY) {
            tempScreenY = worldY;
        }
        if (gp.screenHeight - screenY > (gp.maxWorldRow * gp.tileSize) - worldY) {
            tempScreenY = gp.screenHeight - ((gp.maxWorldRow * gp.tileSize) - worldY);
        }

        BufferedImage image = switch (direction) {
            case "up" -> (spriteNum == 1) ? up1 : up2;
            case "down" -> (spriteNum == 1) ? down1 : down2;
            case "left" -> (spriteNum == 1) ? left1 : left2;
            case "right" -> (spriteNum == 1) ? right1 : right2;
            default -> down1;
        };

        g2.drawImage(image, tempScreenX, tempScreenY, gp.tileSize, gp.tileSize, null);
    }
}