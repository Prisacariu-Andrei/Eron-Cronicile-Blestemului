package Entity.Player;

import Entity.Entity;
import Entity.StatusEffect;
import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Items.Rune;
import Entity.Runes.RuneFactory;
import Entity.Cards.GeamatulEcoului;
import Main.Easing;
import Main.GameFeel;
import Main.GamePanel;
import Main.KeyHandler;
import Main.SoundManager;
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

    // --- Rune (runes.json): bonusurile aplicate acum stării lui Eron, ca să putem scădea exact ce am adăugat ---
    private int runeDamageApplied = 0, runeLifeApplied = 0;
    public Rune lastRuneDrop = null; // runa primită la ultima luptă (ecranul de recompensă)
    public ProgressManagement.StoryProgress.Artifact lastKeyItem = null; // artefactul primit la ultima luptă

    /** Suma unui efect peste toate runele echipate (ex. "turnShield"). */
    public int runeSum(String stat) {
        int sum = 0;
        for (Rune r : inventory.runes) if (r.isEquipped) sum += r.buff(stat);
        return sum;
    }

    /** Repune daune și viață maximă conform runelor echipate acum (apelat la echipare/dezechipare). */
    public void refreshRunes() {
        int dmg = runeSum("damage"), hp = runeSum("maxLife");
        damage += dmg - runeDamageApplied;
        maxLife += hp - runeLifeApplied;
        if (hp > runeLifeApplied) life += hp - runeLifeApplied; // viața bonus vine "plină"
        life = Math.min(life, maxLife);
        runeDamageApplied = dmg;
        runeLifeApplied = hp;
        notifyObservers();
    }

    /** Scoate toate bonusurile de rune din stat-uri (înainte de încărcare sau joc nou). */
    public void clearRunes() {
        inventory.runes.clear();
        refreshRunes();
    }

    /** După încărcarea unei salvări: viața maximă salvată include deja bonusul, daunele nu. */
    public void restoreRunes() {
        for (String id : RuneFactory.startingIds()) { // salvările vechi nu au Manuscrisul
            if (inventory.ownsRune(id)) continue;
            Rune r = RuneFactory.createRune(id);
            if (inventory.addRune(r) && inventory.equippedCount() < inventory.runeSlots()) r.isEquipped = true;
        }
        runeLifeApplied = runeSum("maxLife");
        runeDamageApplied = 0;
        refreshRunes();
    }

    /** Echipează/dezechipează runa. Returnează un mesaj pentru jucător. */
    public String toggleRune(Rune r) {
        if (r.isEquipped) {
            r.isEquipped = false;
            refreshRunes();
            return r.name + " dezechipată";
        }
        if (inventory.equippedCount() >= inventory.runeSlots()) return "Toate sloturile sunt pline! Dezechipează o rună.";
        r.isEquipped = true;
        refreshRunes();
        return r.name + " echipată";
    }

    /** Adaugă runa în rucsac și o echipează dacă mai e un slot liber. */
    public boolean giveRune(Rune r) {
        if (!inventory.addRune(r)) return false;
        if (inventory.equippedCount() < inventory.runeSlots()) { r.isEquipped = true; refreshRunes(); }
        return true;
    }

    public final int NORMAL_SPEED = 4;
    public float exploreMana;
    public final float maxExploreMana = 100f;
    public float manaRegen = 0.25f;

    // Mișcare cu accelerare (vezi GameFeel)
    public float moveVelocity = 0f;   // 0..1: cât de aproape de viteza maximă merge Eron acum
    private float moveRemainder = 0f; // fracțiunea de pixel neconsumată între ticks
    private int dustCounter = 0;
    private int stepSoundCounter = 0;

    // Monede (valorile câștigurilor și prețurile sunt în res/Data/economy.json)
    public int coins = 0;
    public int lastCoinReward = 0; // cât a primit la ultima luptă (afișat pe ecranul de recompensă)
    public int lastXpReward = 0;

    public void addCoins(int amount) {
        coins += amount;
        gp.ui.coinPulse = 18; // HUD-ul "sare" când primești monede
    }

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
        coins = Main.Economy.get().startingCoins();
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
        for (String id : RuneFactory.startingIds()) giveRune(RuneFactory.createRune(id)); // ex. Manuscrisul Străvechi
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
            gp.fx.dust(feetX(), feetY(), dirX(), dirY(), 9); // nor de praf la start
            if (GameFeel.SOUND_FEEDBACK) SoundManager.getInstance().playSE("dash");
        }

        boolean wantsMove = keyH.upPressed || keyH.downPressed || keyH.leftPressed || keyH.rightPressed;
        if (wantsMove) {
            if (keyH.upPressed) direction = "up";
            if (keyH.downPressed) direction = "down";
            if (keyH.leftPressed) direction = "left";
            if (keyH.rightPressed) direction = "right";
            // Accelerează treptat până la viteza maximă...
            moveVelocity = Math.min(1f, moveVelocity + 1f / GameFeel.PLAYER_ACCEL_TICKS);
        } else {
            // ...și alunecă puțin înainte să se oprească, în loc să înghețe pe loc
            moveVelocity = Math.max(0f, moveVelocity - 1f / GameFeel.PLAYER_DECEL_TICKS);
        }

        if (moveVelocity > 0f) {
            updateMovement();
        } else {
            moveRemainder = 0f;
        }
        gp.eHandler.checkEvent();
        checkEnemyCollision();
    }

    // Pozițiile picioarelor (pentru praf) și direcția de mers ca vector
    private float feetX() { return worldX + gp.tileSize / 2f; }
    private float feetY() { return worldY + gp.tileSize * 0.88f; }
    private float dirX() { return "left".equals(direction) ? -1f : "right".equals(direction) ? 1f : 0f; }
    private float dirY() { return "up".equals(direction) ? -1f : "down".equals(direction) ? 1f : 0f; }

    private void updateMovement() {
        // Viteza în pixeli pe tick: dash-ul e o explozie de viteză, mersul urmează curba de accelerare.
        // Viteza fracționară (ex. 4.4 px) se adună între ticks, iar coliziunea se verifică pentru pasul întreg.
        float pxPerTick = isDashing ? dashSpeed
                : NORMAL_SPEED * GameFeel.PLAYER_SPEED_SCALE * Easing.outQuad(moveVelocity);
        moveRemainder += pxPerTick;
        int step = (int) moveRemainder;
        moveRemainder -= step;

        if (step > 0) {
            speed = step; // CollisionChecker.checkTile se uită cu "speed" pixeli înainte
            collisionOn = false;
            gp.cChecher.checkTile(this);

            if (!collisionOn) {
                switch (direction) {
                    case "up": worldY -= step; break;
                    case "down": worldY += step; break;
                    case "left": worldX -= step; break;
                    case "right": worldX += step; break;
                }
            } else {
                // Lovit de un zid: un mic nor de praf și oprire pe loc (nu mai "împinge" în perete)
                if (moveVelocity > 0.6f) gp.fx.dust(feetX(), feetY(), dirX(), dirY(), 3);
                moveVelocity = 0f;
                moveRemainder = 0f;
                if (isDashing) isDashing = false;
            }
            speed = isDashing ? dashSpeed : NORMAL_SPEED;
        }

        spriteCounter++;
        if (spriteCounter > 12) {
            spriteNum = (spriteNum == 1) ? 2 : 1;
            spriteCounter = 0;
        }

        // Praf sub picioare și pași
        if (isDashing) {
            if (++dustCounter % 2 == 0) gp.fx.dashTrail(feetX(), worldY + gp.tileSize / 2f, dirX(), dirY());
        } else if (moveVelocity > 0.5f && ++dustCounter >= GameFeel.DUST_EVERY_TICKS) {
            dustCounter = 0;
            gp.fx.dust(feetX(), feetY(), dirX(), dirY(), 2);
        }
        if (!isDashing && GameFeel.FOOTSTEPS && moveVelocity > 0.6f && ++stepSoundCounter >= GameFeel.FOOTSTEP_EVERY_TICKS) {
            stepSoundCounter = 0;
            if (GameFeel.SOUND_FEEDBACK) SoundManager.getInstance().playSE("step");
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

    /**
     * Folosește abilitatea de pe slotul dat (tastele 1-4). Costă esență ca o carte; se deblochează la nivel
     * (progression.json). Efectele: damage, shield, heal, mana, status.
     */
    public void useAbility(int slot) {
        var ability = gp.progression.abilityAt(slot);
        if (ability == null || !isPlayerTurn || currentEnemy == null || !currentEnemy.alive || life <= 0) return;

        if (!gp.progression.isUnlocked(ability)) {
            gp.fx.denied();
            gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, "Nivel " + ability.unlockLevel + " necesar", Color.ORANGE));
            return;
        }
        if (currentMana < ability.cost) {
            gp.fx.denied();
            gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, "Esență insuficientă", Color.ORANGE));
            return;
        }

        currentMana -= ability.cost;
        boolean hitsEnemy = false;
        for (var effect : ability.effects) {
            int value = Main.Json.integer(effect.get("value"), 0);
            switch (Main.Json.str(effect.get("type"), "")) {
                case "damage" -> {
                    hitsEnemy = true;
                    currentEnemy.takeDamage(this, value + (Main.Json.bool(effect.get("scalesWithDamage"), true) ? damage : 0));
                }
                case "shield" -> addShield(value);
                case "heal" -> {
                    int healed = Math.min(value, maxLife - life);
                    life += healed;
                    gp.popups.add(new DamagePopUp(gp.tileSize * 3, gp.tileSize * 2, "+" + healed + " HP", Color.GREEN));
                    notifyObservers();
                }
                case "mana" -> addMana(value);
                case "status" -> {
                    hitsEnemy = true;
                    try {
                        currentEnemy.applyStatus(StatusEffect.Type.valueOf(Main.Json.str(effect.get("status"), "WEAK")),
                                Main.Json.integer(effect.get("turns"), 1), value);
                    } catch (IllegalArgumentException e) {
                        GameExceptionHandler.getInstance().handleWarning("Status necunoscut într-o abilitate: " + effect.get("status"), e);
                    }
                }
                default -> GameExceptionHandler.getInstance().handleWarning("Efect necunoscut într-o abilitate: " + effect.get("type"), null);
            }
        }
        gp.fx.abilityCast(ability.color.getRGB() & 0xFFFFFF, hitsEnemy);
        notifyObservers();

        if (currentEnemy != null && currentEnemy.life <= 0 && !currentEnemy.alive) winBattle();
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
        if (runeSum("turnShield") > 0) addShield(runeSum("turnShield")); // runele echipate

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

        boolean inBattle = gp.getCurrentState() instanceof BattleState;
        if (this.shield < amount) {
            int remaining = amount - shield;
            shield = 0;
            life -= remaining;
            gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, "-" + remaining + " HP", Color.RED));
            if (inBattle) gp.fx.playerHurt(remaining);   // shake, hit-stop, scântei
            else gp.fx.mapHurt(remaining);               // spike-uri pe hartă
        } else {
            shield -= amount;
            gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, "-" + amount + " BLOCK", Color.GRAY));
            if (inBattle) gp.fx.blocked();               // scutul a oprit lovitura
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
        this.moveVelocity = 0f; // nu mai alunecă când revine pe hartă
        this.moveRemainder = 0f;
        gp.progression.leveledThisBattle = false;
        this.isPlayerTurn = true;
        this.currentMana = this.maxMana;
        this.shield = 0;
        this.voltCount = 0;
        this.lastCardPlayed = null;
        this.inventory.prepareForBattle();
        if (currentEnemy != null) currentEnemy.setNextIntent();
    }

    public void winBattle() {
        this.life = Math.min(this.life + 5 + runeSum("battleHeal"), maxLife);
        notifyObservers();

        if (currentEnemy != null) {
            // Folosim doar partea dinaintea virgulei: "Feldgug, Regele Trol" -> "Feldgug",
            // "Glaciara, Stăpânul Inelului de Gheață" -> "Glaciara" etc., ca să se potrivească cu case-urile de mai jos
            String inamicNume = currentEnemy.name.split(",")[0].trim();

            // XP (progression.json -> enemyXp) și progresul misiunilor de tip "învinge"
            lastXpReward = gp.progression.enemyXp(inamicNume);
            gp.questLog.onKill(inamicNume);
            gp.progression.addXp(lastXpReward);

            // Monede pentru inamicul învins (cât se primește: economy.json -> enemyDrops)
            lastCoinReward = Main.Economy.get().enemyDrop(inamicNume);
            if (lastCoinReward > 0) {
                addCoins(lastCoinReward);
                gp.ui.message = "+" + lastCoinReward + " monede";
                gp.ui.messageOn = true;
            }

            // Rune căzute de la inamic (runes.json -> drops)
            lastRuneDrop = null;
            lastKeyItem = null;
            String dropId = RuneFactory.rollDrop(inamicNume, inventory::ownsRune);
            if (dropId != null && giveRune(RuneFactory.createRune(dropId))) {
                lastRuneDrop = inventory.runes.get(inventory.runes.size() - 1);
                gp.ui.message = "Rună nouă: " + lastRuneDrop.name;
                gp.ui.messageOn = true;
                gp.fx.questSound(false);
            }

            // Progresul poveștii: boss-ul învins deschide pasajul spre zona următoare și dă artefactul
            var artifact = gp.story.onEnemyDefeated(inamicNume);
            if (artifact != null) {
                gp.ui.message = "Obiect cheie: " + artifact.name + "!";
                gp.ui.messageOn = true;
                lastKeyItem = artifact;
                gp.fx.questSound(true);
            }

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
                case "Jean Plesa":
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

                    // Side Quest 1: Învinge un Lup de Elită
                    if ("Lup de Elită".equals(inamicNume)) {
                        gp.questManager.updateQuestProgress(1, false, 1);
                    }
                    break;

                case "Fenris":
                case "Fenris cel Alb":
                case "Lupul cel Bătrân":
                    // Quest 3: Învinge Lupul cel Bătrân din Bârlog
                    gp.questManager.updateQuestProgress(3, true, 1);
                    break;

                case "Trol de Pestera":
                    // Quest 5: Învinge 5 inamici în Abisul de Piatra
                    var q5 = gp.questManager.getQuestById(5, true);
                    if (q5 != null && q5.isReceived && !q5.isCompleted) {
                        gp.questManager.updateQuestProgress(5, true, 1);
                        gp.ui.message = "Progres Abis: " + q5.currentAmount + "/5";
                        gp.ui.messageOn = true;
                    }
                    break;

                case "Trol de Elită":
                    // Quest 6: Învinge mini-boss-ul din zona Abisul de Piatra
                    gp.questManager.updateQuestProgress(6, true, 1);
                    break;

                case "Feldgug":
                    // Quest 7: Învinge-l pe Feldgug
                    gp.questManager.updateQuestProgress(7, true, 1);
                    break;

                case "Jean":
                case "Jean Pleșa":
                case "Jean Plesa":
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
        clearRunes();           // fără rune și fără bonusurile lor
        gp.story.reset();       // niciun boss învins, pasajele se închid la loc
        areDinteChilimbar = areDiamantulNoptii = arePalosLegendar = areInelDeGheata = arePlatosaValerius = false;
        setDefaultValues();
        gp.progression.reset(); // nivel 1, fără XP, esența de bază
        gp.questLog.reset();    // misiunile de la capăt
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

        int tempScreenX = gp.camera.screenX(worldX);
        int tempScreenY = gp.camera.screenY(worldY);

        // Eron nu stă niciodată perfect nemișcat: respiră când stă, sare ușor la fiecare pas
        // și se alungește pe direcția dash-ului. Picioarele rămân pe loc, doar corpul se deformează.
        float scaleX = 1f, scaleY = 1f, bob = 0f;
        if (isDashing) {
            boolean horizontal = "left".equals(direction) || "right".equals(direction);
            float stretch = GameFeel.DASH_STRETCH;
            scaleX = horizontal ? 1f + stretch : 1f - stretch * 0.5f;
            scaleY = horizontal ? 1f - stretch * 0.5f : 1f + stretch;
        } else if (moveVelocity > 0.15f) {
            bob = (float) Math.sin(spriteCounter / 13f * Math.PI) * GameFeel.WALK_BOB_PX * moveVelocity;
        } else {
            scaleY += (float) Math.sin(gp.fx.ticks * 0.06) * GameFeel.PLAYER_BREATH_SCALE;
        }

        BufferedImage image = switch (direction) {
            case "up" -> (spriteNum == 1) ? up1 : up2;
            case "down" -> (spriteNum == 1) ? down1 : down2;
            case "left" -> (spriteNum == 1) ? left1 : left2;
            case "right" -> (spriteNum == 1) ? right1 : right2;
            default -> down1;
        };

        int dw = Math.round(gp.tileSize * scaleX);
        int dh = Math.round(gp.tileSize * scaleY);
        g2.drawImage(image, tempScreenX - (dw - gp.tileSize) / 2, tempScreenY - (dh - gp.tileSize) - Math.round(bob), dw, dh, null);
    }

    public void primesteDiamantulNoptii() {
        this.areDiamantulNoptii = true;
        this.maxLife += 10;
        this.maxLife += 10;
        this.life += 10; // Îi dăm și viața curentă ca bonus instant
        this.notifyObservers(); // Actualizează HealthBarUI-ul!
    }
}