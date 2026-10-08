package Entity.Player;

import Entity.Entity;
import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Cards.GeamatulEcoului;
import Main.GamePanel;
import Main.KeyHandler;
import Main.Exception.GameExceptionHandler;
import Observer.Observer;
import Observer.DamagePopUp;
import GameState.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Player extends Entity {

    public String playerName ;

    KeyHandler keyH;
    public Inventory inventory = new Inventory();
    private final List<Observer> observers = new ArrayList<>();

    public int screenX;
    public int screenY;

    public boolean areDinteChilimbar = false;  // Fenris
    public boolean areDiamantulNoptii = false; // Feldgug
    public boolean arePalosLegendar = false;   // Jean
    public boolean areInelDeGheata = false;    // Glaciara
    public boolean arePlatosaValerius = false; // Valerius

    public final int NORMAL_SPEED = 4;
    public float exploreMana;
    public final float maxExploreMana = 100f;
    public float manaRegen = 0.25f;

    public boolean isDashing = false;
    public int dashSpeed = 16;
    public int dashCounter = 0;
    public final int DASH_DURATION = 20;
    public final int DASH_COST = 30;

    public int maxMana = 3;
    public int currentMana;
    public boolean isPlayerTurn = true;
    public Enemy currentEnemy;

    public Card lastCardPlayed = null;
    public int voltCount = 0;
    public boolean isInvulnerable = false;

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

        this.inventory.cards.addAll(CardManager.getStartingDeck(this));
    }

    public void update() {
        updateStatus();

        var state = gp.getCurrentState();

        if (state instanceof PlayState) {
            handleExploration();
        }
        else if (state instanceof BattleState) {
            handleBattleInput();
        }
        else if (state instanceof RewardState) {
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
            exploreMana -= DASH_COST;
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
        gp.cChecher.checkTile(this);

        if (!collisionOn) {
            switch (direction) {
                case "up": worldY -= speed; break;
                case "down": worldY += speed; break;
                case "left": worldX -= speed; break;
                case "right": worldX += speed; break;
            }
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
            return;
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
                inventory.addCardToDeck(selected.name, this);
                gp.rewardUI.commandNum = 0;
                gp.changeState(new PlayState(gp));
            }
        }

        if (gp.keyH.escPressed) {
            gp.keyH.escPressed = false;
            gp.rewardUI.commandNum = 0;
            gp.changeState(new PlayState(gp));
        }
    }

    public void playSelectedCard() {
        try {
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

                    if (currentEnemy != null && currentEnemy.life <= 0 && !currentEnemy.alive) {
                        winBattle();
                    }

                    if (gp.battleUI.commandNum >= inventory.hand.size() && gp.battleUI.commandNum > 0) {
                        gp.battleUI.commandNum = inventory.hand.size() - 1;
                    }
                }
            }
        } catch (IndexOutOfBoundsException e) {
            GameExceptionHandler.getInstance().handleWarning("Gestiune index cursor eronată în timpul bătăliei la playSelectedCard", e);
            if (gp.battleUI.commandNum >= inventory.hand.size()) {
                gp.battleUI.commandNum = Math.max(0, inventory.hand.size() - 1);
            }
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare neprevăzută la utilizarea cărții selectate", e);
        }
    }

    public void endTurn() {
        isPlayerTurn = false;
        isInvulnerable = false;

        if (currentEnemy != null) {
            String enemyName = currentEnemy.name;
            if (!"Valerius, Cavalerul Neinvins".equals(enemyName)) {
                currentEnemy.shield = 0;
            }
        }

        while(!inventory.hand.isEmpty()) {
            inventory.discard(inventory.hand.removeFirst());
        }
        gp.battleUI.commandNum = 0;

        if (currentEnemy != null) {

            currentEnemy.updateStatuses();

            if (currentEnemy.life <= 0 && !currentEnemy.alive) {
                Timer deathTimer = new Timer(800, e -> { winBattle(); ((Timer)e.getSource()).stop(); });
                deathTimer.setRepeats(false); deathTimer.start();
                return;
            }
        }

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
            return;
        }

        gp.battleUI.commandNum = 0;

        this.updateStatuses();

        Timer t1 = new Timer(1000, e -> {
            gp.battleUI.announceTurn("TURNUL TĂU");
            ((Timer)e.getSource()).stop();
        });
        t1.start();

        if (this.arePlatosaValerius) {
            // Apelăm direct addShield pentru a declanșa și popup-ul grafic pe ecran!
            shield = 0;
            this.addShield(5);
            System.out.println("DEBUG: Platoșa lui Valerius s-a activat! +5 Scut.");
        }
        else{
            shield = 0;
        }

        Timer t2 = new Timer(1500, e -> {
            isPlayerTurn = true;
            for(int i = 0; i < 5; i++) inventory.drawCard();
            ((Timer)e.getSource()).stop();
        });
        t2.start();

        if (currentEnemy != null && currentEnemy.stunTurns > 0) {
            currentEnemy.stunTurns -= 1;
        }
    }

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
            isPlayerTurn = false;

            if (currentEnemy != null) {
                currentEnemy = null;
            }

            gp.changeState(new DeathState(gp));
        }
        notifyObservers();
    }

    public void addShield(int amount) {
        if(this.areInelDeGheata){
            amount += 3;
        }
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
                    gp.changeState(new BattleState(gp));
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
            gp.activeNpcIndex = npcIndex;
            gp.changeState(new DialogueState(gp));
            gp.npc[npcIndex].speak();
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
            String inamicNume = currentEnemy.name;

            // --- 1. ATRIBUIRE RUNE DE LA GENERALI ---
            switch (inamicNume) {
                case "Fenris":
                case "Fenris cel Alb":
                    this.areDinteChilimbar = true;
                    System.out.println("⭐ DEBLOCAT: Dintele de Chilimbar (+3 Base Damage)!");
                    break;

                case "Feldgug":
                    this.primesteDiamantulNoptii();
                    System.out.println("⭐ DEBLOCAT: Diamantul Nopții (+10 Max HP)!");
                    break;

                case "Jean":
                case "Jean Pleșa":
                    this.arePalosLegendar = true;
                    System.out.println("⭐ DEBLOCAT: Paloșul Legendar (Dezolarea dă x1.75 damage)!");
                    break;

                case "Glaciara":
                    this.areInelDeGheata = true;
                    System.out.println("⭐ DEBLOCAT: Inelul de Gheață (+3 Base Armor)!");
                    break;

                case "Valerius":
                case "Valerius, Cavalerul Neinvins":
                    this.arePlatosaValerius = true;
                    System.out.println("⭐ DEBLOCAT: Platoșa lui Valerius (+5 Scut pe tură)!");
                    break;
            }

            // --- 2. ACTUALIZARE AUTOMATĂ PROGRES QUEST-URI PRINCIPALE ---
            // Verificăm ce monstru a murit și trimitem progresul către QuestManager
            switch (inamicNume) {

                case "Lup":
                case "Cioara":
                case "Lup de Elită":
                case "Lup Slăbit":
                    // Quest 2: Învinge 5 inamici în Bârlog
                    var q2 = gp.questManager.getQuestById(2, true);
                    if (q2 != null && q2.isReceived && !q2.isCompleted) {
                        gp.questManager.updateQuestProgress(2, true, 1);
                        gp.ui.message = "Progres Bârlog: " + q2.currentAmount + "/5";
                        gp.ui.messageOn = true;
                    }

                    // Quest 1: Dacă Tutorialul cere doar uciderea primului inamic din pădure (un lup normal)
                    var q1 = gp.questManager.getQuestById(1, true);
                    if (q1 != null && q1.isReceived && !q1.isCompleted) {
                        gp.questManager.updateQuestProgress(1, true, 1);
                        gp.ui.message = "Misiune completată: Tutorial";
                        gp.ui.messageOn = true;
                    }
                    break;

                case "Fenris":
                case "Fenris cel Alb":
                case "Lupul cel Bătrân":
                    // Quest 3: Învinge Lupul cel Bătrân din Bârlog
                    gp.questManager.updateQuestProgress(3, true, 1);
                    break;

                case "Orc":
                case "InamicAbis": // Înlocuiește cu numele text al inamicilor normali din Abis
                    // Quest 5: Învinge 5 inamici în Abisul de Piatra
                    var q5 = gp.questManager.getQuestById(5, true);
                    if (q5 != null && q5.isReceived && !q5.isCompleted) {
                        gp.questManager.updateQuestProgress(5, true, 1);
                        gp.ui.message = "Progres Abis: " + q5.currentAmount + "/5";
                        gp.ui.messageOn = true;
                    }
                    break;

                case "Mini-Boss Abis":
                case "Locotenent Orc": // Înlocuiește cu numele exact din clasa mini-boss-ului
                    // Quest 6: Învinge mini-boss-ul din zona Abisul de Piatra
                    gp.questManager.updateQuestProgress(6, true, 1);
                    break;

                case "Feldgug":
                    // Quest 7: Învinge-l pe Feldgug
                    gp.questManager.updateQuestProgress(7, true, 1);
                    break;

                case "Jean":
                case "Jean Pleșa":
                    // Quest 9: Învinge-l / Recrutează-l pe Jean Pleșa
                    gp.questManager.updateQuestProgress(9, true, 1);
                    break;

                case "Glaciara":
                    // Quest 11: Învinge-o pe Glaciara
                    gp.questManager.updateQuestProgress(11, true, 1);
                    break;

                case "Charon":
                    // Quest 12: Învinge-l pe Charon
                    gp.questManager.updateQuestProgress(12, true, 1);
                    break;

                case "Malakor":
                case "Malakor, Monarhul Amurgului":
                    // Quest 14: Învinge-l pe Malakor
                    gp.questManager.updateQuestProgress(14, true, 1);

                    // Executăm direct logica ta de final de joc
                    currentEnemy.alive = false;
                    currentEnemy = null;
                    this.inventory.resetDeckAfterBattle();
                    long finalTime = gp.ui.getPlayTime();
                    gp.leaderboardUI.checkAndSaveNewScore(finalTime, this.name);
                    gp.creditsUI.resetPosition();
                    gp.changeState(new CreditsState(gp));
                    return;
            }

            // Decomentează și folosește dacă vrei ca Valerius să completeze un Side Quest la moarte
            if ("Valerius".equals(inamicNume) || "Valerius, Cavalerul Neinvins".equals(inamicNume)) {
                gp.questManager.updateQuestProgress(3, false, 1); // Side Quest 3
            }

            currentEnemy.alive = false;
            currentEnemy = null;
        }
        this.inventory.resetDeckAfterBattle();
        gp.rewardUI.generateRewards();
        gp.changeState(new RewardState(gp));
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
        catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare la încărcarea setului de imagini pentru personajul principal (Player)", e);
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

    public void primesteDiamantulNoptii() {
        this.areDiamantulNoptii = true;
        this.maxLife += 10;
        this.maxLife += 10;
        this.life += 10; // Îi dăm și viața curentă ca bonus instant
        this.notifyObservers(); // Actualizează HealthBarUI-ul!
    }
}