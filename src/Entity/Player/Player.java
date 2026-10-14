package Entity.Player;

import Entity.Entity;
import Entity.StatusEffect;
import Entity.Inamici.Enemy;
import Entity.Inamici.EnemyFactory;
import Entity.Inamici.Groups;
import Entity.Cards.CardCatalog;
import Entity.Items.Card;
import Entity.NPC.NPC;
import Entity.Cards.Combos;
import Entity.Items.Rune;
import Entity.Runes.RuneFactory;
import Entity.Cards.GeamatulEcoului;
import Main.Easing;
import Main.GameFeel;
import Main.GamePanel;
import Main.KeyHandler;
import Main.SoundManager;
import Main.ZoneInfo;
import Main.Exception.GameExceptionHandler;
import Observer.Observer;
import Observer.CutsceneUI;
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

    /** Numele tastat la început: apare DOAR în clasament. Eroul se numește mereu "Eron" (câmpul name). */
    public String playerProfileName = "Eron";

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

    /** Paguba de bază a unei cărți: bonusul general + bonusul runelor pentru elementele ei (tagDamage.FULGER etc.). Descrierile cărților o folosesc, deci arată mereu valoarea reală. */
    public int cardDamage(Card c) { return damageFor(c.tags); }

    /** Bonusul general + bonusul runelor pentru elementele date (cărți și abilități). */
    public int damageFor(java.util.Collection<String> tags) {
        int d = damage;
        for (String t : tags) d += runeSum("tagDamage." + t);
        return d;
    }

    // --- Abilitățile Q/W/E/R: fără esență, pe ture de așteptare (progression.json) ---
    private final java.util.Map<String, Integer> abilityCooldowns = new java.util.HashMap<>();
    private final java.util.Set<String> abilitiesUsedThisBattle = new java.util.HashSet<>();

    /** Câte ture mai are de așteptat abilitatea (0 = gata); -1 dacă s-a folosit deja (o dată pe luptă). */
    public int abilityWait(ProgressManagement.Progression.Ability a) {
        if (a.oncePerBattle && abilitiesUsedThisBattle.contains(a.id)) return -1;
        return abilityCooldowns.getOrDefault(a.id, 0);
    }

    /** Începutul turei lui Eron: toate cooldown-urile scad cu o tură. */
    private void tickAbilityCooldowns() { abilityCooldowns.replaceAll((k, v) -> Math.max(0, v - 1)); }

    /** Textul abilității, calculat din efecte și din runele echipate (se schimbă live, ca la cărți). */
    public String abilityText(ProgressManagement.Progression.Ability a) {
        StringBuilder sb = new StringBuilder();
        for (var e : a.effects) {
            int v = Main.Json.integer(e.get("value"), 0);
            String part = switch (Main.Json.str(e.get("type"), "")) {
                case "damage" -> "Paguba " + (v + damageFor(a.tags));
                case "shield" -> "Armura " + (v + runeSum("cardShield"));
                case "heal" -> "Vindecă " + v;
                case "status" -> {
                    String label = Main.Json.str(e.get("status"), "?");
                    try { label = CardCatalog.statusLabel(StatusEffect.Type.valueOf(label)); } catch (IllegalArgumentException ignored) { }
                    yield label + " " + Main.Json.integer(e.get("turns"), 1) + " ture";
                }
                default -> null;
            };
            if (part == null) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(part);
        }
        return sb.toString();
    }

    /** Cât de tare / cât de mult ține un status pus de o carte, după runele echipate (statusPower.BURN, statusTurns.FROZEN...). */
    public int statusPower(StatusEffect.Type t) { return runeSum("statusPower." + t.name()); }
    public int statusTurns(StatusEffect.Type t) { return runeSum("statusTurns." + t.name()); }

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
            inventory.addRune(r); // obiectele de poveste nu se echipează
        }
        runeLifeApplied = runeSum("maxLife");
        runeDamageApplied = 0;
        refreshRunes();
    }

    /** Echipează/dezechipează runa. Returnează un mesaj pentru jucător. */
    public String toggleRune(Rune r) {
        if (r.story) return r.name + " este un obiect de poveste: nu se echipează.";
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
        if (!r.story && inventory.equippedCount() < inventory.runeSlots()) { r.isEquipped = true; refreshRunes(); }
        return true;
    }

    public final int NORMAL_SPEED = 4;
    public float exploreMana;
    public final float maxExploreMana = 100f;
    public float manaRegen = 0.25f;

    // Mișcare cu accelerare (vezi GameFeel)
    public float moveVelocity = 0f;   // 0..1: cât de aproape de viteza maximă merge Eron acum
    private float moveRemainder = 0f;  // fracțiunea de pixel neconsumată între ticks (axa X)
    private float moveRemainderY = 0f; // idem, axa Y (mersul pe diagonală consumă fiecare axă separat)
    private float moveNx = 0f, moveNy = 0f;   // direcția de mers, vector normalizat (diagonalele nu sunt mai rapide)
    private float dashNx = 0f, dashNy = 0f;   // direcția dash-ului, fixată la începutul lui
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
    public Enemy currentEnemy;                              // ținta curentă (cea lovită de cărți)
    public final List<Enemy> team = new ArrayList<>();      // toți inamicii din luptă (inclusiv ținta)
    public final Combos.State combo = new Combos.State();   // elementele jucate în tura asta
    public final java.util.Set<String> lastDefeated = new java.util.LinkedHashSet<>(); // numele inamicilor învinși la ultima luptă (recompense de carte)
    public double enemyDmgScale = 1.0;                      // cu cât mai mulți inamici, cu atât lovesc mai slab (enemies.json)

    /**
     * Lupta cu inamicul de pe hartă și grupul lui (enemies.json -> packExtras). Întâi scena de dinaintea luptei (dialog la boss-i,
     * apoi tranziția în spirală); lupta propriu-zisă începe în startBattleNow, când ecranul e negru.
     */
    public void beginBattle(Enemy first) {
        if (gp.getCurrentState() instanceof BattleIntroState || gp.getCurrentState() instanceof BattleState) return;
        String bossFlag = gp.story.flagOf(first.name.split(",")[0].trim());
        if (!bossFlag.isEmpty() && gp.storyScenes.playNow("before_boss:" + bossFlag, () -> new BattleIntroState(gp, first))) return;   // scenă înainte de boss (story.json)
        gp.changeState(new BattleIntroState(gp, first));
    }

    /** Începe efectiv lupta (apelată de BattleIntroState când spirala a acoperit ecranul). */
    public void startBattleNow(Enemy first) {
        team.clear();
        currentEnemy = first;
        gp.changeState(new BattleState(gp));
        startBattle();
    }

    /** Adună grupul dacă lupta a început fără beginBattle (doar ținta e în echipă). */
    private void ensureTeam() {
        if (currentEnemy == null) { team.clear(); enemyDmgScale = 1.0; return; }
        if (team.contains(currentEnemy)) return;
        team.clear();
        team.add(currentEnemy);
        int cap = Groups.maxSize();
        for (String id : currentEnemy.packExtras) {
            if (team.size() >= cap) break;
            Enemy e = EnemyFactory.createEnemy(id, gp, 0, 0);
            if (e == null) continue;
            if (e.battleBackground == null) e.battleBackground = currentEnemy.battleBackground;
            team.add(e);
        }
        int n = team.size();
        enemyDmgScale = Groups.damageScale(n);
        double hs = Groups.hpScale(n);
        if (hs != 1.0) for (Enemy e : team) { e.maxLife = (int) Math.round(e.maxLife * hs); e.life = e.maxLife; }
    }

    /** Damage-ul dat de Eron cât timp e Slăbit (Slăbiciune: -25%). */
    public int weakened(int dmg) {
        for (StatusEffect s : activeStatuses) if (s.type == StatusEffect.Type.WEAK) return Math.max(1, (int) Math.round(dmg * 0.75));
        return dmg;
    }

    public List<Enemy> livingEnemies() {

        List<Enemy> out = new ArrayList<>();
        for (Enemy e : team) if (e.alive) out.add(e);
        if (team.isEmpty() && currentEnemy != null && currentEnemy.alive) out.add(currentEnemy);
        return out;
    }

    /** Dacă ținta a căzut dar mai sunt alții în viață, trecem pe următorul. */
    public void retarget() {
        if (currentEnemy != null && currentEnemy.alive) return;
        for (Enemy e : team) if (e.alive) { currentEnemy = e; return; }
    }

    /** W/S, săgeți sau Tab: schimbă ținta între inamicii în viață. */
    public void cycleTarget(int dir) {
        List<Enemy> alive = livingEnemies();
        if (alive.size() < 2) return;
        int i = alive.indexOf(currentEnemy);
        currentEnemy = alive.get(Math.floorMod(i + dir, alive.size()));
        SoundManager.getInstance().playSE("click");
    }

    /** Ticks rămase până la ecranul de recompensă (-1 = lupta nu s-a terminat). Cât timp rulează, inamicii cad cu animație. */
    public int winDelay = -1;

    /** Fals cât timp mai e un inamic în viață; altfel pornește pauza de victorie (animația de moarte), apoi winBattle(). */
    public boolean checkVictory() {
        if (currentEnemy == null) return false;
        retarget();
        if (!livingEnemies().isEmpty()) return false;
        startWinDelay();
        return true;
    }

    public void startWinDelay() {
        if (winDelay < 0) {
            winDelay = Main.GameFeel.WIN_DELAY_TICKS;
            isPlayerTurn = false;   // nu se mai poate juca nimic cât cad inamicii
        }
    }

    /** Apelat la fiecare tick din BattleState. */
    public void tickWin() {
        if (winDelay > 0 && --winDelay == 0) {
            winDelay = -1;
            winBattle();
        }
    }


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

        inventory.cards.clear();     // setDefaultValues rulează și la joc nou: fără clear pachetul s-ar dubla
        inventory.hand.clear();
        inventory.discardPile.clear();
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
            // dash-ul merge în direcția tastelor apăsate (inclusiv pe diagonală); fără taste, în direcția în care privește Eron
            int dix = (keyH.rightPressed ? 1 : 0) - (keyH.leftPressed ? 1 : 0), diy = (keyH.downPressed ? 1 : 0) - (keyH.upPressed ? 1 : 0);
            if (dix == 0 && diy == 0) {
                dix = "right".equals(direction) ? 1 : "left".equals(direction) ? -1 : 0;
                diy = "down".equals(direction) ? 1 : "up".equals(direction) ? -1 : 0;
            }
            float dlen = (float) Math.sqrt(dix * dix + diy * diy);
            dashNx = dlen == 0 ? 0 : dix / dlen;
            dashNy = dlen == 0 ? 0 : diy / dlen;
            gp.fx.dust(feetX(), feetY(), dashNx, dashNy, 9); // nor de praf la start
            if (GameFeel.SOUND_FEEDBACK) SoundManager.getInstance().playSE("dash");
        }

        // 8 direcții: două taste de direcție apăsate dau o diagonală, cu viteza normalizată (nu cu ~41% mai repede)
        int ix = (keyH.rightPressed ? 1 : 0) - (keyH.leftPressed ? 1 : 0);
        int iy = (keyH.downPressed ? 1 : 0) - (keyH.upPressed ? 1 : 0);
        boolean wantsMove = ix != 0 || iy != 0;
        if (wantsMove) {
            // sprite-urile au doar patru direcții: pe diagonală Eron privește pe orizontală (stânga/dreapta)
            if (ix != 0) direction = ix > 0 ? "right" : "left";
            else direction = iy > 0 ? "down" : "up";
            float len = (float) Math.sqrt(ix * ix + iy * iy);
            moveNx = ix / len;
            moveNy = iy / len;
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
            moveRemainderY = 0f;
        }
        gp.eHandler.checkEvent();
        checkEnemyCollision();
    }

    // Pozițiile picioarelor (pentru praf) și direcția de mers ca vector
    private float feetX() { return worldX + gp.tileSize / 2f; }
    private float feetY() { return worldY + gp.tileSize * 0.88f; }
    private float dirX() { return isDashing ? dashNx : moveNx; }
    private float dirY() { return isDashing ? dashNy : moveNy; }

    /** Un pas pe o singură axă (direcția "left"/"right"/"up"/"down"), dacă dalele din față sunt libere. Returnează fals dacă e zid. */
    private boolean stepAlong(String dir, int step) {
        String facing = direction;
        direction = dir;            // CollisionChecker.checkTile se uită în direcția dată, "step" pixeli înainte
        speed = step;
        collisionOn = false;
        gp.cChecher.checkTile(this);
        direction = facing;
        if (Main.DebugMode.noclip) collisionOn = false;   // panoul de admin: treci prin orice dală
        if (collisionOn) return false;
        switch (dir) {
            case "up" -> worldY -= step;
            case "down" -> worldY += step;
            case "left" -> worldX -= step;
            default -> worldX += step;
        }
        if (Main.DebugMode.noclip) {   // fără dale în cale, dar nu ieșim din matricea hărții
            worldX = Math.max(0, Math.min(worldX, (gp.maxWorldCol - 2) * gp.tileSize));
            worldY = Math.max(0, Math.min(worldY, (gp.maxWorldRow - 2) * gp.tileSize));
        }
        return true;
    }

    private void updateMovement() {
        // Viteza în pixeli pe tick: dash-ul e o explozie de viteză, mersul urmează curba de accelerare.
        // Viteza se împarte pe axe după vectorul normalizat; partea fracționară (ex. 4.4 px) se adună între ticks.
        float pxPerTick = isDashing ? dashSpeed
                : NORMAL_SPEED * GameFeel.PLAYER_SPEED_SCALE * Easing.outQuad(moveVelocity);
        float nx = isDashing ? dashNx : moveNx, ny = isDashing ? dashNy : moveNy;
        moveRemainder += pxPerTick * Math.abs(nx);
        moveRemainderY += pxPerTick * Math.abs(ny);
        int stepX = (int) moveRemainder, stepY = (int) moveRemainderY;
        moveRemainder -= stepX;
        moveRemainderY -= stepY;

        // Axele se rezolvă separat: pe diagonală, un zid oprește doar axa lui, iar Eron alunecă pe lângă el
        boolean tried = stepX > 0 || stepY > 0, moved = false;
        if (stepX > 0) {
            if (stepAlong(nx > 0 ? "right" : "left", stepX)) moved = true; else moveRemainder = 0f;
        }
        if (stepY > 0) {
            if (stepAlong(ny > 0 ? "down" : "up", stepY)) moved = true; else moveRemainderY = 0f;
        }
        if (tried && !moved) {
            // Lovit de un zid pe toate axele: un mic nor de praf și oprire pe loc (nu mai "împinge" în perete)
            if (moveVelocity > 0.6f) gp.fx.dust(feetX(), feetY(), nx, ny, 3);
            moveVelocity = 0f;
            moveRemainder = 0f;
            moveRemainderY = 0f;
            if (isDashing) isDashing = false;
        }
        speed = isDashing ? dashSpeed : NORMAL_SPEED;

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
        if (winDelay >= 0) return;
        try {
            if (inventory.hand.isEmpty()) return;

            int index = gp.battleUI.commandNum;
            if (index >= 0 && index < inventory.hand.size()) {
                Card card = inventory.hand.get(index);

                if (currentMana >= card.manaCost) {
                    currentMana -= card.manaCost;

                    Combos.play(this, card, currentEnemy);   // efectele cărții + reacții + lanț (cards.json)

                    if (!(card instanceof GeamatulEcoului)) {
                        lastCardPlayed = card;
                    }

                    inventory.discard(inventory.hand.remove(index));

                    checkVictory();

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
    public void useAbility(ProgressManagement.Progression.Ability ability) {
        if (winDelay >= 0) return;
        if (ability == null || !isPlayerTurn || currentEnemy == null || !currentEnemy.alive || life <= 0) return;

        if (!inventory.canLearnAbilities()) { // fără Manuscrisul Străvechi nu poți învăța abilități
            gp.fx.denied();
            gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, "Ai nevoie de Manuscris", Color.ORANGE));
            return;
        }
        if (!gp.progression.isUnlocked(ability)) {
            gp.fx.denied();
            gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, "Nivel " + ability.unlockLevel + " necesar", Color.ORANGE));
            return;
        }
        int wait = abilityWait(ability);
        if (wait != 0) {
            gp.fx.denied();
            gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, wait < 0 ? "O dată pe luptă" : "Mai așteaptă " + wait + (wait == 1 ? " tură" : " ture"), Color.ORANGE));
            return;
        }
        if (ability.oncePerBattle) abilitiesUsedThisBattle.add(ability.id);
        else if (ability.cooldown > 0) abilityCooldowns.put(ability.id, ability.cooldown);
        boolean hitsEnemy = false;
        for (var effect : ability.effects) {
            int value = Main.Json.integer(effect.get("value"), 0);
            switch (Main.Json.str(effect.get("type"), "")) {
                case "damage" -> {
                    hitsEnemy = true;
                    currentEnemy.takeDamage(this, value + damageFor(ability.tags));
                }
                case "shield" -> addShield(value + runeSum("cardShield"));
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

        checkVictory();
    }

    public void endTurn() {
        if (winDelay >= 0) return;
        isPlayerTurn = false;
        isInvulnerable = false;

        List<Enemy> foes = new ArrayList<>(team);
        if (foes.isEmpty() && currentEnemy != null) foes.add(currentEnemy);
        for (Enemy f : foes) {
            if (!f.persistentShield && !"Valerius, Cavalerul Neinvins".equals(f.name)) f.shield = 0;
        }

        while(!inventory.hand.isEmpty()) {
            inventory.discard(inventory.hand.removeFirst());
        }
        gp.battleUI.commandNum = 0;

        if (currentEnemy != null) {
            for (Enemy f : foes) f.updateStatuses();   // arsuri, otravă
            if (victoryAfter(800)) return;

            // Tura aliaților (allies.json): acționează după Eron, înainte de inamici
            retarget();
            gp.party.actAll(this, currentEnemy);
            if (victoryAfter(900)) return;
        }

        gp.battleUI.announceTurn("TURNUL INAMICULUI");

        Timer enemyTurnTimer = new Timer(1500, e -> {
            enemyActs(foes, 0);
            ((Timer)e.getSource()).stop();
        });
        enemyTurnTimer.setRepeats(false);
        enemyTurnTimer.start();

        this.currentMana = this.maxMana;
    }

    /** Dacă toți inamicii au căzut, câștigă lupta după o scurtă pauză. */
    private boolean victoryAfter(int delayMs) {
        if (currentEnemy == null) return false;
        retarget();
        if (!livingEnemies().isEmpty()) return false;
        startWinDelay();
        return true;
    }

    /** Fiecare inamic în viață își joacă intenția, pe rând (cu o mică pauză între ei). */
    private void enemyActs(List<Enemy> order, int i) {
        if (life <= 0 || currentEnemy == null && team.isEmpty()) return;
        if (i >= order.size()) { finishEnemyTurn(); return; }
        Enemy e = order.get(i);
        boolean acted = e.alive;
        if (acted) e.executeIntent(this);
        if (life <= 0) return;                       // Eron a căzut: damagePlayer a trecut deja pe ecranul de moarte
        Timer next = new Timer(acted ? 650 : 1, ev -> { enemyActs(order, i + 1); ((Timer)ev.getSource()).stop(); });
        next.setRepeats(false);
        next.start();
    }

    private void finishEnemyTurn() {
        if (this.life <= 0) {
            return;
        }

        gp.battleUI.commandNum = 0;
        combo.reset();   // tura nouă: lanțul de elemente o ia de la capăt

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
            tickAbilityCooldowns();
            for(int i = 0; i < 5; i++) inventory.drawCard();
            ((Timer)e.getSource()).stop();
        });
        t2.start();

        for (Enemy f : team) if (f.stunTurns > 0) f.stunTurns -= 1;
        if (team.isEmpty() && currentEnemy != null && currentEnemy.stunTurns > 0) currentEnemy.stunTurns -= 1;
        retarget();
    }

    public void damagePlayer(int amount) {
        if (Main.DebugMode.god) return;   // panoul de admin: nemurire
        if (isInvulnerable) {
            gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, "IMUN", Color.WHITE));
            return;
        }

        boolean inBattle = gp.getCurrentState() instanceof BattleState;

        // Cu cât sunt mai mulți inamici în luptă, cu atât lovesc mai slab (enemies.json -> groups)
        if (inBattle && amount > 0 && enemyDmgScale != 1.0) amount = Math.max(1, (int) Math.round(amount * enemyDmgScale));

        // Cu un aliat în viață, o parte din lovituri ajung la el (allies.json -> party.targetChance)
        if (inBattle && gp.party.redirectHit(amount)) return;

        // Vulnerabilitate (ex. Jean Pleșa): primești mai mult damage cât timp statusul e activ
        for (StatusEffect s : activeStatuses) {
            if (s.type == StatusEffect.Type.VULNERABLE) {
                amount += amount * s.value / 100;
                gp.popups.add(new DamagePopUp(gp.tileSize * 3, gp.tileSize * 4, "VULNERABIL", new Color(255, 120, 200)));
                break;
            }
        }

        if (this.shield < amount) {
            int remaining = amount - shield;
            shield = 0;
            life -= remaining;
            gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, "-" + remaining + " HP", Color.RED));
            if (inBattle) gp.fx.playerHurt(remaining);   // shake, hit-stop, scântei
            else gp.fx.mapHurt(remaining);               // spike-uri pe hartă
        } else {
            shield -= amount;
            gp.popups.add(new DamagePopUp(gp.tileSize * 4, gp.tileSize * 3, "BLOCAT", new Color(130, 180, 235)));
            if (inBattle) gp.fx.blocked();               // scutul a oprit lovitura
        }

        if (this.life <= 0) {
            this.life = 0;
            isPlayerTurn = false;

            if (currentEnemy != null) {
                currentEnemy = null;
            }
            team.clear();

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
        if (Main.DebugMode.noFights) return;
        Rectangle pRect = new Rectangle(worldX + hitBox.x, worldY + hitBox.y, hitBox.width, hitBox.height);
        for (int i = 0; i < gp.enemy.length; i++) {
            if (gp.enemy[i] != null && gp.enemy[i].alive) {
                Rectangle eRect = new Rectangle(gp.enemy[i].worldX + gp.enemy[i].hitBox.x,
                        gp.enemy[i].worldY + gp.enemy[i].hitBox.y,
                        gp.enemy[i].hitBox.width, gp.enemy[i].hitBox.height);
                if (pRect.intersects(eRect)) {
                    beginBattle(gp.enemy[i]);
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

    /** F: deschide un cufăr sau folosește o poartă din apropiere (obiectele NPC cu usesF()). */
    public void interactObject() {
        int t = gp.tileSize;
        Rectangle pr = new Rectangle(worldX + hitBox.x - t, worldY + hitBox.y - t, hitBox.width + 2 * t, hitBox.height + 2 * t);
        NPC nearest = null;
        long best = Long.MAX_VALUE;
        for (int i = 0; i < gp.npc.length; i++) {
            NPC n = gp.npc[i];
            if (n == null || !n.usesF()) continue;
            Rectangle nr = new Rectangle(n.worldX + n.hitBox.x, n.worldY + n.hitBox.y, n.hitBox.width, n.hitBox.height);
            if (!pr.intersects(nr)) continue;
            long dx = nr.x + nr.width / 2 - (worldX + hitBox.x + hitBox.width / 2), dy = nr.y + nr.height / 2 - (worldY + hitBox.y + hitBox.height / 2);
            if (dx * dx + dy * dy < best) { best = dx * dx + dy * dy; nearest = n; }   // cel mai apropiat (oglinzile stau aproape una de alta)
        }
        if (nearest != null) nearest.use();
    }

    public void startBattle() {

        winDelay = -1;
        this.moveVelocity = 0f; // nu mai alunecă când revine pe hartă
        this.moveRemainder = 0f;
        this.moveRemainderY = 0f;
        gp.progression.leveledThisBattle = false;
        this.isPlayerTurn = true;
        this.currentMana = this.maxMana + runeSum("firstTurnMana");   // Runa Haiducului: esență în plus doar în prima tură
        this.shield = 0;
        if (runeSum("battleShield") > 0) addShield(runeSum("battleShield"));   // Runa lui Feldgug
        this.voltCount = 0;
        this.lastCardPlayed = null;
        activeStatuses.clear();
        abilityCooldowns.clear();
        abilitiesUsedThisBattle.clear();
        gp.party.startBattle();
        this.inventory.prepareForBattle();
        combo.reset();
        ensureTeam();
        for (Enemy f : team) f.setNextIntent();
        if (currentEnemy != null && currentEnemy.introTitle != null) gp.battleUI.announcePhase(currentEnemy.introTitle, currentEnemy.introSubtitle, new java.awt.Color(200, 160, 255));
        if (team.isEmpty() && currentEnemy != null) currentEnemy.setNextIntent();
    }

    public void winBattle() {
        this.life = Math.min(this.life + 5 + runeSum("battleHeal"), maxLife);
        notifyObservers();

        List<Enemy> defeated = new ArrayList<>(team);
        if (defeated.isEmpty() && currentEnemy != null) defeated.add(currentEnemy);
        team.clear();
        enemyDmgScale = 1.0;
        lastXpReward = 0;
        lastCoinReward = 0;
        lastRuneDrop = null;
        lastKeyItem = null;
        lastDefeated.clear();
        for (Enemy en : defeated) {
            en.alive = false;
            currentEnemy = en;
            if (!rewardFor()) return;     // finalul jocului (Malakor) a preluat ecranul
        }
        currentEnemy = null;
        finishWin();
    }

    /** Recompensele pentru inamicul currentEnemy: XP, monede, rune, progresul poveștii și al misiunilor. */
    private boolean rewardFor() {
        {
            // Folosim doar partea dinaintea virgulei: "Feldgug, Regele Trol" -> "Feldgug",
            // "Glaciara, Stăpânul Inelului de Gheață" -> "Glaciara" etc., ca să se potrivească cu case-urile de mai jos
            String inamicNume = currentEnemy.name.split(",")[0].trim();
            lastDefeated.add(inamicNume);

            // XP (progression.json -> enemyXp) și progresul misiunilor de tip "învinge"
            int xpGain = gp.progression.enemyXp(inamicNume);
            lastXpReward += xpGain;
            gp.questLog.onKill(inamicNume);
            gp.progression.addXp(xpGain);

            // Monede pentru inamicul învins (cât se primește: economy.json -> enemyDrops)
            int coinDrop = Main.Economy.get().enemyDrop(inamicNume);
            lastCoinReward += coinDrop;
            if (coinDrop > 0) {
                addCoins(coinDrop);
                gp.ui.message = "+" + coinDrop + " monede";
                gp.ui.messageOn = true;
            }

            // Rune căzute de la inamic (runes.json -> drops)
            String dropId = RuneFactory.rollDrop(inamicNume, inventory::ownsRune);
            if (dropId != null && giveRune(RuneFactory.createRune(dropId))) {
                lastRuneDrop = inventory.runes.get(inventory.runes.size() - 1);
                gp.ui.message = "Rună nouă: " + lastRuneDrop.name;
                gp.ui.messageOn = true;
                gp.fx.questSound(false);
            }

            gp.party.onEnemyDefeated(inamicNume); // boss-ii care recrutează un aliat (scena se joacă după recompensă)

            // Progresul poveștii: boss-ul învins deschide pasajul spre zona următoare și dă artefactul
            boolean areaBoss = gp.story.isBoss(inamicNume) && !gp.story.has(gp.story.flagOf(inamicNume)); // primul triumf asupra lui
            var artifact = gp.story.onEnemyDefeated(inamicNume);
            if (areaBoss && ZoneInfo.exitOf(gp.tileM.currentZone) != null) gp.pendingExitZone = gp.tileM.currentZone;
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

                case "Yeti":
                    // Side Quest 4: Vânătorul de Yeti
                    gp.questManager.updateQuestProgress(4, false, 1);
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
                    // Quest 11: Învinge-o pe Glaciara; gheața se topește și arată drumul spre Podul Umbrelor
                    gp.questManager.updateQuestProgress(11, true, 1);
                    gp.ui.message = "Ghețurile se topesc: ai găsit drumul spre Podul Umbrelor!";
                    gp.ui.messageOn = true;
                    break;

                case "Charon":
                    // Quest 12: Învinge-l pe Charon
                    gp.questManager.updateQuestProgress(12, true, 1);
                    break;

                case "Malakor":
                case "Malakor, Monarhul Amurgului":
                    // Quest 14: Învinge-l pe Malakor
                    gp.questManager.updateQuestProgress(14, true, 1);

                    // Finalul jocului: scorul intră în clasament, apoi epilogul și genericul
                    currentEnemy.alive = false;
                    currentEnemy = null;
                    this.inventory.resetDeckAfterBattle();
                    activeStatuses.clear();
                    long finalTime = gp.ui.getPlayTime();
                    gp.leaderboardUI.checkAndSaveNewScore(finalTime, playerProfileName);
                    gp.changeState(new ZoneTransitionState(gp, "epilogue", () -> new CutsceneState(gp, CutsceneUI.Script.EPILOGUE)));
                    return false;
            }

            // Decomentează și folosește dacă vrei ca Valerius să completeze un Side Quest la moarte
            if ("Valerius".equals(inamicNume) || "Valerius, Cavalerul Neinvins".equals(inamicNume)) {
                gp.questManager.updateQuestProgress(3, false, 1); // Side Quest 3
            }

            currentEnemy.alive = false;
            currentEnemy = null;
        }
        return true;
    }

    private void finishWin() {
        activeStatuses.clear();      // nicio vulnerabilitate rămasă din luptă
        gp.party.afterBattle();      // aliații se refac parțial (allies.json)
        this.inventory.resetDeckAfterBattle();
        gp.rewardUI.generateRewards();
        gp.changeState(new RewardState(gp));
    }

    public void resetPlayer() {
        clearRunes();           // fără rune și fără bonusurile lor
        gp.party.reset();       // fără aliați
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