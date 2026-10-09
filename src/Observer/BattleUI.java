package Observer;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.StatusEffect;
import Main.Easing;
import Main.GameFeel;
import Main.GamePanel;
import Entity.Entity;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

public class BattleUI {
    GamePanel gp;
    public int commandNum = 0;
    public String turnMessage = "";
    public int turnMessageTimer = 0;

    public boolean isTransitioning = false;
    public int transitionTimer = 0;
    private String transitionPhaseName = "", transitionSubtitle = "";
    private Color transitionColor;
    private int transitionLength = 40;

    // --- Animații (se actualizează în update(), în ticks fixe, nu în draw()) ---
    private static final int HIT_ANIM_TICKS = 14;
    private static final int TURN_MESSAGE_TICKS = 30;
    private final float[] cardLift = new float[16];        // 0 = jos, 1 = ridicată (selectată), cu easing
    private final int[] cardDealDelay = new int[16];       // întârziere până apare cartea trasă
    private final int[] cardDealAge = new int[16];
    private int dealtCards = 0;                            // câte cărți din mână au început să intre
    private int enemyHitTimer = 0, playerHitTimer = 0;

    public BattleUI(GamePanel gp) {
        this.gp = gp;
    }

    /** Banner mare (titlu + rând de explicație) la începutul unei lupte speciale sau la schimbarea fazei unui boss. */
    public void announcePhase(String title, String subtitle, Color color) {
        transitionPhaseName = title == null ? "" : title;
        transitionSubtitle = subtitle == null ? "" : subtitle;
        transitionColor = color == null ? Color.WHITE : color;
        transitionLength = 130;
        transitionTimer = 0;
        isTransitioning = true;
    }

    public void announceTurn(String text) {

        this.turnMessage = text;
        this.turnMessageTimer = TURN_MESSAGE_TICKS;
    }

    /** Apelată la începutul fiecărei lupte: cărțile vor intra în mână una câte una. */
    public void onBattleStart() {
        dealtCards = 0;
        enemyHitTimer = 0;
        playerHitTimer = 0;
        Arrays.fill(cardLift, 0f);
        Arrays.fill(cardDealDelay, 0);
        Arrays.fill(cardDealAge, 0);
    }

    public void onEnemyHit() { enemyHitTimer = HIT_ANIM_TICKS; }

    public void onPlayerHit() { playerHitTimer = HIT_ANIM_TICKS; }

    // Pozițiile personajelor pe ecran (aceleași formule ca la desenare), pentru particule
    public int groundY() { return (int)(gp.screenHeight * 0.87); }
    public int playerCenterX() { return (int)(gp.screenWidth * 0.08) + (int)(gp.screenWidth * 0.10) / 2; }
    public int playerCenterY() { return groundY() - (int)(gp.screenWidth * 0.10); }
    /** Un inamic așezat pe ecran: x (stânga), dimensiunea sprite-ului. */
    private static final class Slot {
        Enemy e; int x, size;
        int cx() { return x + size / 2; }
    }

    /** Așezarea inamicilor în luptă: unul singur ca înainte, mai mulți se micșorează și se înșiră de la dreapta spre stânga. */
    private java.util.List<Slot> slots() {
        java.util.List<Slot> out = new ArrayList<>();
        var p = gp.player;
        java.util.List<Enemy> alive = new ArrayList<>();
        for (Enemy e : p.team) if (e.alive) alive.add(e);
        if (p.team.isEmpty() && p.currentEnemy != null) alive.add(p.currentEnemy);
        int n = alive.size();
        if (n == 0) return out;

        double k = n == 1 ? 1.0 : n == 2 ? 0.85 : n == 3 ? 0.7 : 0.58;
        int gap = (int) (gp.screenWidth * 0.01);
        int[] sizes = new int[n];
        int total = gap * (n - 1);
        for (int i = 0; i < n; i++) { sizes[i] = (int) (gp.screenWidth * 0.20 * alive.get(i).battleSize * k); total += sizes[i]; }
        int avail = (int) (gp.screenWidth * 0.55);
        if (n > 1 && total > avail) {
            double shrink = (double) (avail - gap * (n - 1)) / (total - gap * (n - 1));
            total = gap * (n - 1);
            for (int i = 0; i < n; i++) { sizes[i] = (int) (sizes[i] * shrink); total += sizes[i]; }
        }
        int right = (int) (gp.screenWidth * (n == 1 ? 0.9 : 0.95));
        for (int i = 0; i < n; i++) {
            Slot s = new Slot();
            s.e = alive.get(i);
            s.size = sizes[i];
            s.x = right - s.size;
            right = s.x - gap;
            s.e.battleCenterX = s.cx();
            out.add(s);
        }
        return out;
    }

    private Slot slotOf(Enemy e) {
        for (Slot s : slots()) if (s.e == e) return s;
        return null;
    }

    private int enemySize() {
        Slot s = slotOf(gp.player.currentEnemy);
        return s == null ? 0 : s.size;
    }
    public int enemyCenterX() {
        Slot s = slotOf(gp.player.currentEnemy);
        return s == null ? (int) (gp.screenWidth * 0.8) : s.cx();
    }
    public int enemyCenterY() { return groundY() - enemySize() / 2; }

    /**
     * Un tick de animație. Înainte, timerele banerelor creșteau în draw(), deci dacă desenarea încetinea,
     * mesajele rămâneau mai mult pe ecran. Acum durează la fel indiferent de FPS.
     */
    public void update() {
        if (isTransitioning) {
            transitionTimer++;
            if (transitionTimer > transitionLength) { isTransitioning = false; transitionTimer = 0; transitionLength = 40; transitionSubtitle = ""; }
        } else if (turnMessageTimer > 0) {
            turnMessageTimer--;
        }

        int n = Math.min(gp.player.inventory.hand.size(), 8);
        if (n > dealtCards) { // cărți nou trase: intră pe rând
            for (int i = dealtCards; i < n; i++) {
                cardDealDelay[i] = (i - dealtCards) * GameFeel.CARD_DEAL_STAGGER_TICKS;
                cardDealAge[i] = 0;
            }
        }
        dealtCards = n;

        for (int i = 0; i < cardLift.length; i++) {
            float target = (i == commandNum && i < n) ? 1f : 0f;
            cardLift[i] = Easing.approach(cardLift[i], target, GameFeel.CARD_LIFT_SMOOTHING);
            if (cardDealDelay[i] > 0) cardDealDelay[i]--;
            else if (cardDealAge[i] < GameFeel.CARD_DEAL_TICKS) cardDealAge[i]++;
        }

        if (enemyHitTimer > 0) enemyHitTimer--;
        if (playerHitTimer > 0) playerHitTimer--;
    }

    /** Desenează un personaj de luptă: respiră ușor când stă și se strivește/dă înapoi când e lovit. */
    private void drawFighter(Graphics2D g2, BufferedImage img, int x, int groundY, int w, int h, int hitTimer, float phase) {
        float breath = (float) Math.sin((gp.fx.ticks + phase) * 0.07) * GameFeel.BATTLE_BREATH_SCALE;
        float t = hitTimer / (float) HIT_ANIM_TICKS;               // 1 la lovitură, scade spre 0
        float squash = 0.14f * Easing.outQuad(t);
        int recoil = (int) (Math.sin(hitTimer * 1.9) * t * 16);
        int dw = Math.round(w * (1 + squash));
        int dh = Math.round(h * (1 + breath - squash));
        g2.drawImage(img, x - (dw - w) / 2 + recoil, groundY - dh, dw, dh, null);
    }

    public void draw(Graphics2D g2) {
        // --- DINAMIC ANCHORS ---
        int groundY = (int)(gp.screenHeight * 0.87); // Ground line at 75% height

        // 1. FUNDAL
        drawBackground(g2);

        // 2. DESENARE ERON (PLAYER)
        // Scale width to 10% of screen width, height is double that
        int eronWidth = (int)(gp.screenWidth * 0.10);
        int eronHeight = eronWidth * 2;

        if (gp.player.fightStand != null) {
            int playerX = (int)(gp.screenWidth * 0.08);
            drawFighter(g2, gp.player.fightStand, playerX, groundY, eronWidth, eronHeight, playerHitTimer, 0f);
        }

        // 3. DESENARE INAMICI (unul sau o echipă)
        java.util.List<Slot> slots = slots();
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            if (s.e.fightStand == null) continue;
            boolean target = s.e == gp.player.currentEnemy;
            drawFighter(g2, s.e.fightStand, s.x, groundY, s.size, s.size, target ? enemyHitTimer : 0, 40f + i * 17f);
        }

        // 4. INTERFAȚĂ (Static methods converted to dynamic)
        drawPlayerStats(g2, groundY);
        drawEnemyStats(g2, groundY);
        drawTargetHint(g2);
        drawCardHand(g2);
        drawManaPool(g2,groundY);
        drawAbilities(g2);
        drawEnemyIntent(g2);
        drawControlsAndPopups(g2);

        // 5. TRANZIȚII ȘI MESAJE
        drawMessages(g2);
    }

    private void drawBackground(Graphics2D g2) {
        if (gp.player.currentEnemy != null && gp.player.currentEnemy.battleBackground != null) {
            g2.drawImage(gp.player.currentEnemy.battleBackground, 0, 0, gp.screenWidth, gp.screenHeight, null);
        } else {
            g2.setColor(new Color(20, 20, 40));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
    }

    /** Aliații lui Eron (allies.json): sprite lângă Eron, bară de viață, scut și ultima abilitate folosită. */
    private void drawAllies(Graphics2D g2, int groundY) {
        for (var a : gp.party.members) {
            int cx = gp.party.allyX(a);
            int h = (int) (gp.screenWidth * 0.15), barW = (int) (gp.screenWidth * 0.10), barH = (int) (gp.screenHeight * 0.012);

            var img = a.image();
            if (img != null) {
                int w = h * img.getWidth() / Math.max(1, img.getHeight());
                float bob = a.alive() ? (float) Math.sin(gp.fx.ticks * 0.08 + cx) * 3f : 0f;
                Composite old = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a.alive() ? 1f : 0.35f));
                g2.drawImage(img, cx - w / 2, gp.party.allyGroundY() - h - Math.round(bob), w, h, null);
                g2.setComposite(old);
            }

            int x = cx - barW / 2, y = gp.party.allyGroundY() + (int) (gp.screenHeight * 0.03);
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRoundRect(x, y, barW, barH, 5, 5);
            g2.setColor(new Color(a.color));
            g2.fillRoundRect(x, y, (int) (barW * Math.max(0, Math.min(1, (double) a.life / a.maxLife))), barH, 5, 5);
            if (a.shield > 0) {
                g2.setColor(new Color(52, 152, 219, 210));
                g2.fillRoundRect(x, y, Math.min(barW, (int) (barW * (double) a.shield / a.maxLife)), barH, 5, 5);
            }
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, barW, barH, 5, 5);

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 115f));
            g2.drawString(a.name, x, y - (int) (gp.screenHeight * 0.015));
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 130f));
            String hp = a.life + " / " + a.maxLife + (a.shield > 0 ? "  (+" + a.shield + " scut)" : "");
            g2.drawString(hp, x, y + barH + (int) (gp.screenHeight * 0.02));
            if (!a.lastAction.isEmpty()) {
                g2.setColor(a.alive() ? new Color(a.color) : new Color(220, 120, 120));
                g2.drawString(a.lastAction, x, y + barH + (int) (gp.screenHeight * 0.04));
            }
        }
    }

    private void drawPlayerStats(Graphics2D g2, int groundY) {
        int x = (int)(gp.screenWidth * 0.06);
        int y = groundY + (int)(gp.screenHeight * 0.02);
        int barWidth = (int)(gp.screenWidth * 0.10);
        int barHeight = (int)(gp.screenHeight * 0.012);

        // Fundalul barei (gri închis)
        g2.setColor(new Color(50, 50, 50, 200));
        g2.fillRoundRect(x, y, barWidth, barHeight, 5, 5);

        // Calcul procente viață
        double hpPercent = (double) gp.player.life / gp.player.maxLife;
        int hpWidth = (int) (barWidth * Math.max(0, Math.min(1, hpPercent)));

        // Randare Viață (Verde)
        g2.setColor(new Color(46, 204, 113));
        g2.fillRoundRect(x, y, hpWidth, barHeight, 5, 5);

        // ★ MODIFICARE: Randare SCUT direct peste/în continuarea barei de HP
        if (gp.player.shield > 0) {
            double shieldPercent = (double) gp.player.shield / gp.player.maxLife;
            int shieldWidth = (int) (barWidth * Math.max(0, Math.min(1, shieldPercent)));

            // Desenăm scutul semi-transparent albastru (se va suprapune elegant sau va extinde bara)
            g2.setColor(new Color(52, 152, 219, 210));
            // Îl desenăm pornind din același punct, sau din capătul HP-ului.
            // Standard RPG: se desenează de la stânga la dreapta cu un efect de overlays
            g2.fillRoundRect(x, y, Math.min(barWidth, shieldWidth), barHeight, 5, 5);
        }

        // Contur bară
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, barWidth, barHeight, 5, 5);

        // Text Info
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 115f));
        g2.setColor(Color.WHITE);
        g2.drawString("ERON  Nv. " + gp.progression.level, x, y - (int)(gp.screenHeight * 0.015));

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 130f));
        String healthText = gp.player.life + " / " + gp.player.maxLife;
        if (gp.player.shield > 0) healthText += " (+🛡️ " + gp.player.shield + ")";
        g2.drawString(healthText, x, y + barHeight + (int)(gp.screenHeight * 0.02));

        drawEntityStatuses(g2, gp.player, x, y + (int)(gp.screenHeight * 0.04));
    }

    private void drawEnemyStats(Graphics2D g2, int groundY) {
        java.util.List<Slot> slots = slots();
        boolean many = slots.size() > 1;
        for (Slot s : slots) {
            Enemy enemy = s.e;
            int barWidth = many ? Math.max((int) (gp.screenWidth * 0.07), Math.min((int) (gp.screenWidth * 0.10), s.size)) : (int) (gp.screenWidth * 0.10);
            int barHeight = (int) (gp.screenHeight * (many ? 0.013 : 0.015));
            int x = s.cx() - barWidth / 2;
            int y = groundY + (int) (gp.screenHeight * 0.02);

            g2.setColor(new Color(30, 30, 30, 200));
            g2.fillRoundRect(x, y, barWidth, barHeight, 8, 8);

            double hpPercent = (double) enemy.life / enemy.maxLife;
            int hpWidth = (int) (barWidth * Math.max(0, Math.min(1, hpPercent)));
            g2.setColor(new Color(231, 76, 60));
            g2.fillRoundRect(x, y, hpWidth, barHeight, 8, 8);

            if (enemy.shield > 0) {
                double shieldPercent = (double) enemy.shield / enemy.maxLife;
                int shieldWidth = (int) (barWidth * Math.max(0, Math.min(1, shieldPercent)));
                g2.setColor(new Color(41, 128, 185, 210));
                g2.fillRoundRect(x, y, Math.min(barWidth, shieldWidth), barHeight, 8, 8);
            }

            boolean target = enemy == gp.player.currentEnemy;
            g2.setColor(target && many ? new Color(255, 215, 0) : Color.WHITE);
            g2.setStroke(new BasicStroke(target && many ? 3f : 2f));
            g2.drawRoundRect(x, y, barWidth, barHeight, 8, 8);

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / (many ? 135f : 115f)));
            g2.setColor(target && many ? new Color(255, 230, 120) : Color.WHITE);
            String nm = enemy.name.toUpperCase();
            if (many) nm = nm.split(",")[0];
            g2.drawString(nm, x, y - (int) (gp.screenHeight * 0.015));

            String hpText = enemy.life + " / " + enemy.maxLife;
            if (enemy.shield > 0) hpText += " (+" + enemy.shield + ")";
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 130f));
            g2.setColor(Color.WHITE);
            g2.drawString(hpText, x + (barWidth / 2) - (g2.getFontMetrics().stringWidth(hpText) / 2), y + barHeight + (int) (gp.screenHeight * 0.02));

            drawEntityStatuses(g2, enemy, x, y + barHeight + (int) (gp.screenHeight * 0.04));
        }
    }

    /** Săgeata de deasupra țintei și indicația de tastă, doar când sunt mai mulți inamici. */
    private void drawTargetHint(Graphics2D g2) {
        java.util.List<Slot> slots = slots();
        if (slots.size() < 2) return;
        int groundY = groundY();
        for (Slot s : slots) {
            if (s.e != gp.player.currentEnemy) continue;
            int ay = groundY - s.size - (int) (gp.screenHeight * 0.075) + Math.round((float) Math.sin(gp.fx.ticks * 0.18) * 4f);
            int cx = s.cx(), w = (int) (gp.screenWidth * 0.012);
            g2.setColor(new Color(255, 215, 0));
            g2.fillPolygon(new int[]{cx - w, cx + w, cx}, new int[]{ay - w, ay - w, ay + w}, 3);
        }
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, gp.screenWidth / 120f));
        g2.setColor(new Color(220, 220, 235));
        String hint = "[W/S] sau [TAB] alege ținta";
        g2.drawString(hint, (int) (gp.screenWidth * 0.95) - g2.getFontMetrics().stringWidth(hint), (int) (gp.screenHeight * 0.09));
    }

    private void drawEnemyIntent(Graphics2D g2) {
        for (Slot s : slots()) drawIntentFor(g2, s);
    }

    private void drawIntentFor(Graphics2D g2, Slot slot) {
        Enemy e = slot.e;
        int groundY = (int)(gp.screenHeight * 0.87);
        int centerX = slot.cx();
        int y = groundY - slot.size - 30;
        // Setăm fontul dorit înainte de a calcula dimensiunea textului
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / (slot.size < gp.screenWidth * 0.17 ? 62f : 50f)));

        Color intentColor = Color.WHITE;
        String intentString = "❓";

        if (e.nextAction != null) {
            switch (e.nextAction) {
                case ATTACK -> {
                    intentColor = new Color(255, 50, 50);   // ROȘU
                    intentString = "⚔ " + Math.max(0, (int) Math.round(e.intentValue * gp.player.enemyDmgScale));
                }
                case DEFEND -> {
                    intentColor = new Color(52, 152, 219);  // ALBASTRU
                    intentString = "🛡 " + e.intentValue;
                }
                case HEAL -> {
                    intentColor = new Color(46, 204, 113);  // VERDE
                    intentString = "➕ " + e.intentValue;
                }
                case BUFF -> {
                    intentColor = new Color(255, 105, 180); // ROZ
                    intentString = "✨";
                }
                case SPECIAL -> {
                    intentColor = new Color(155, 89, 182); // MOV
                    intentString = "❓❓❓ (" + e.intentValue + ")";
                }
            }
        }

        // Centrarea perfectă a string-ului pe axa X în funcție de lungimea lui dinamică
        int stringWidth = g2.getFontMetrics().stringWidth(intentString);
        int x = centerX - (stringWidth / 2);

        // 1. Randare Umbră Text (Offset de 2px jos-dreapta)
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(intentString, x + 2, y + 2);

        // 2. Randare Text și Emoji Colorat în funcție de acțiune
        g2.setColor(intentColor);
        g2.drawString(intentString, x, y);
    }

    private void drawCardHand(Graphics2D g2) {
        ArrayList<Card> currentHand = gp.player.inventory.hand;
        if (currentHand.isEmpty()) return;

        // --- 1. DYNAMIC ANCHORS (Păstrăm cu strictețe zona cerută) ---
        int cardZoneWidth = gp.screenWidth / 2;
        int cardZoneStartX = (int)(gp.screenWidth / 4.5);

        // Mărim dimensiunea cărții: 10% din lățimea ecranului le oferă destul spațiu pentru text
        int cardWidth = (int)(gp.screenWidth * 0.10);
        int cardHeight = (int)(cardWidth * 1.5); // Raport 1.5 pentru un aspect standard de carte

        int maxCards = 8;
        int numCards = Math.min(currentHand.size(), maxCards);

        // --- LOGICĂ DE SPAȚIERE ȘI SUPRAPUNERE ---
        // Calculăm spațiul ideal dacă nu s-ar suprapune
        int spacing = (numCards > 1) ? (cardZoneWidth - (numCards * cardWidth)) / (numCards - 1) : 0;

        // Dacă numărul de cărți e mare, 'spacing' va deveni negativ în mod automat,
        // generând efectul de suprapunere elegantă (overlap), dar fără a ieși din zonă!

        int totalHandWidth = (numCards * cardWidth) + ((numCards - 1) * spacing);
        // Dacă avem puține cărți și lățimea totală e mai mică decât zona, le centrăm în zonă
        int startX = cardZoneStartX + (cardZoneWidth / 2) - (totalHandWidth / 2);

        // Poziția de bază: Coborâm cărțile mai jos (20% vizibile implicit)
        // Astfel, când nu sunt selectate, nu ocupă ecranul de joc, dar textul devine vizibil la hover.
        int baseSlotY = gp.screenHeight - (int)(cardHeight * 0.20);

        // --- 2. DRAWING LOOP ---
        for (int i = 0; i < numCards; i++) {
            if (i >= dealtCards || cardDealDelay[i] > 0) continue; // încă nu a fost trasă
            Card card = currentHand.get(i);
            int x = startX + (i * (cardWidth + spacing));

            // Cartea selectată se ridică lin (easing), ca să poată fi citită perfect
            int selectedY = gp.screenHeight - cardHeight - 20;
            int y = Math.round(baseSlotY + (selectedY - baseSlotY) * cardLift[i]);
            // O carte nou trasă intră de jos, cu un mic overshoot
            float deal = Easing.outBack(cardDealAge[i] / (float) GameFeel.CARD_DEAL_TICKS);
            y += Math.round((1f - deal) * cardHeight * 1.2f);

            // Salvează stroke-ul inițial
            Stroke originalStroke = g2.getStroke();

            // Randare Shadow
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(x + 4, y + 4, cardWidth, cardHeight, 18, 18);

            // Card Background
            g2.setColor(new Color(30, 30, 45, 255));
            g2.fillRoundRect(x, y, cardWidth, cardHeight, 18, 18);

            // Border (Evidențiere galbenă mai groasă pentru cartea selectată)
            if (i == commandNum) {
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(4));
            } else {
                g2.setColor(new Color(160, 160, 180));
                g2.setStroke(new BasicStroke(1.5f));
            }
            g2.drawRoundRect(x, y, cardWidth, cardHeight, 18, 18);
            g2.setStroke(originalStroke); // Resetare stroke

            // --- 3. DYNAMIC CONTENT RENDERING ---

            // A. Card Name
            float titleFontSize = cardWidth * 0.10f;
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, titleFontSize));
            g2.setColor(Color.WHITE);
            g2.drawString(card.name, x + (int)(cardWidth * 0.12), y + (int)(cardHeight * 0.18));

            // B. Description
            float descFontSize = cardWidth * 0.075f; // Text puțin mai mic pentru a nu ieși din carte
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, descFontSize));
            g2.setColor(new Color(210, 210, 215));

            String dynamicDesc = card.getDynamicDescription(gp.player);
            if (dynamicDesc != null) {
                String[] lines = dynamicDesc.split("\n");
                for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
                    int lineY = y + (int)(cardHeight * 0.40) + (lineIdx * (int)(descFontSize * 1.35));
                    // Ne asigurăm că textul nu depășește marginea de jos a cărții
                    if (lineY < y + cardHeight - 10) {
                        g2.drawString(lines[lineIdx], x + (int)(cardWidth * 0.12), lineY);
                    }
                }
            }

            // C. Mana Crystal (Mutat pe colțul din stânga sus, ușor decalat în exterior)
            int crystalSize = (int)(cardWidth * 0.28);
            int crystalX = x - (crystalSize / 3);
            int crystalY = y - (crystalSize / 3);

            g2.setColor(new Color(41, 128, 185));
            g2.fillOval(crystalX, crystalY, crystalSize, crystalSize);

            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, titleFontSize * 1.1f));

            String cost = String.valueOf(card.manaCost);
            int costWidth = g2.getFontMetrics().stringWidth(cost);
            g2.drawString(cost, crystalX + (crystalSize / 2) - (costWidth / 2), crystalY + (int)(crystalSize * 0.7));
        }
    }

    private static int getSpacing(int cardZoneWidth, int numCards, int cardWidth) {
        int totalAvailableSpace = cardZoneWidth - (numCards * cardWidth);

        // 2. Distribute that space evenly between the cards
        // We use numCards + 1 to create margins on the left and right of the hand
        int spacing = totalAvailableSpace / (numCards + 1);

        // 3. Set a reasonable MAX spacing so they don't fly off to the edges if you only have 1 card
        // and a MIN spacing so they don't overlap.
        spacing = Math.max(10, Math.min(spacing, 40)); // This allows up to 40px of space
        return spacing;
    }

    private void drawEntityStatuses(Graphics2D g2, Entity entity, int x, int y) {
        if (entity.activeStatuses.isEmpty()) return;
        int currentX = x;
        int iconSize = (int)(gp.screenWidth / 100f);
        for (StatusEffect s : entity.activeStatuses) {
            String icon = switch (s.type) {
                case BURN -> "🔥"; case POISON -> "🧪";
                case WEAK -> "💢"; case SHIELD -> "🛡";
                case VULNERABLE -> "💔"; default -> "?";
            };
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, (float)iconSize));
            g2.drawString(icon + s.duration, currentX, y);
            currentX += (int)(gp.screenWidth * 0.025);
        }
    }

    private void drawManaPool(Graphics2D g2, int groundY) {
        int size = (int)(gp.screenWidth * 0.03);
        int x = (int)(gp.screenWidth * 0.1675); // Near the start of the card zone
        int y = groundY + (int)(gp.screenWidth*0.002);

        g2.setColor(new Color(0, 100, 255, 200));
        g2.fillOval(x, y, size, size);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(x, y, size, size);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, size / 2.5f));
        String mana = gp.player.currentMana + "/" + gp.player.maxMana;
        int mWidth = g2.getFontMetrics().stringWidth(mana);
        g2.drawString(mana, x + (int)(size / 2.0) - (int)(mWidth / 2.0), y + (size / 1.6f));
    }

    /**
     * Abilitățile (progression.json) în colțul de sus-stânga: tasta, nume, cost în esență.
     * Cele nedeblocate arată nivelul necesar; cele pe care nu le poți plăti acum sunt estompate.
     */
    private void drawAbilities(Graphics2D g2) {
        var list = gp.progression.abilities;
        int x = 16, y = 96, w = 232, h = 40;
        Font old = g2.getFont();
        for (int i = 0; i < list.size() && i < 9; i++) {
            var a = list.get(i);
            boolean unlocked = gp.progression.isUnlocked(a);
            boolean affordable = unlocked && gp.player.isPlayerTurn && gp.player.currentMana >= a.cost;
            int ry = y + i * (h + 6);

            g2.setColor(affordable ? new Color(40, 38, 70, 235) : new Color(24, 24, 36, 215));
            g2.fillRoundRect(x, ry, w, h, 12, 12);
            g2.setColor(affordable ? a.color : new Color(90, 90, 105));
            g2.setStroke(new BasicStroke(affordable ? 2.5f : 1.5f));
            g2.drawRoundRect(x, ry, w, h, 12, 12);

            // tasta
            g2.setFont(old.deriveFont(Font.BOLD, 16F));
            g2.setColor(unlocked ? Color.WHITE : new Color(110, 110, 125));
            g2.drawString("[" + (i + 1) + "]", x + 10, ry + 26);
            // nume
            g2.setFont(old.deriveFont(Font.BOLD, 15F));
            g2.setColor(unlocked ? (affordable ? Color.WHITE : new Color(165, 165, 180)) : new Color(110, 110, 125));
            g2.drawString(a.name, x + 44, ry + 26);
            // cost sau nivelul necesar
            if (unlocked) {
                g2.setColor(new Color(41, 128, 185, affordable ? 255 : 130));
                g2.fillOval(x + w - 36, ry + 7, 26, 26);
                g2.setColor(Color.WHITE);
                g2.setFont(old.deriveFont(Font.BOLD, 15F));
                String cost = String.valueOf(a.cost);
                g2.drawString(cost, x + w - 23 - g2.getFontMetrics().stringWidth(cost) / 2, ry + 26);
            } else {
                g2.setFont(old.deriveFont(Font.PLAIN, 13F));
                g2.setColor(new Color(150, 150, 165));
                g2.drawString("Nv. " + a.unlockLevel, x + w - 52, ry + 25);
            }
        }
        g2.setFont(old);
    }

    private void drawControlsAndPopups(Graphics2D g2) {
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 110f));
        g2.setColor(Color.WHITE);
        g2.drawString("[T] END TURN", (int)(gp.screenWidth * 0.88), gp.screenHeight - (int)(gp.screenHeight * 0.03));

        Font baseFont = g2.getFont();
        // Copie a listei: popup-urile se pot adăuga/șterge între timp (alt thread), iar un indice vechi ar da eroare
        DamagePopUp[] popups = gp.popups.toArray(new DamagePopUp[0]);
        for (DamagePopUp p : popups) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha));
            // "Pop": apare mai mare decât final și se așază (easing cu overshoot)
            if (Math.abs(p.scale - 1f) > 0.02f) g2.setFont(baseFont.deriveFont(baseFont.getSize2D() * p.scale));
            else g2.setFont(baseFont);
            g2.setColor(Color.BLACK);
            g2.drawString(p.text, p.x + 2, p.y + 2);
            g2.setColor(p.color);
            g2.drawString(p.text, p.x, p.y);
        }
        g2.setFont(baseFont);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private void drawMessages(Graphics2D g2) {
        if (isTransitioning) {
            // Întunericul vine lin și pleacă lin; textul apare cu un "pop"
            float in = Easing.outCubic(transitionTimer / 18f);
            float out = Easing.clamp01((transitionLength - transitionTimer) / 10f);
            g2.setColor(new Color(0, 0, 0, Math.round(200 * in * out)));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
            float pop = Easing.outBack(transitionTimer / 16f);
            g2.setColor(transitionColor);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 40f * (0.6f + 0.4f * pop)));
            String text = transitionPhaseName;
            int x = gp.screenWidth/2 - g2.getFontMetrics().stringWidth(text)/2;
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, in * out)));
            g2.drawString(text, x, gp.screenHeight/2);
            if (!transitionSubtitle.isEmpty()) {
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, gp.screenWidth / 70f));
                g2.setColor(new Color(225, 225, 240));
                g2.drawString(transitionSubtitle, gp.screenWidth/2 - g2.getFontMetrics().stringWidth(transitionSubtitle)/2, gp.screenHeight/2 + gp.screenHeight / 14);
            }
            g2.setComposite(old);
        } else if (turnMessageTimer > 0) {
            float progress = 1f - (float) turnMessageTimer / TURN_MESSAGE_TICKS;   // 0 -> 1
            float pop = Easing.outBack(progress * 3f);                              // apare în primul sfert
            float alpha = Easing.clamp01(turnMessageTimer / 8f);                    // se stinge la final
            g2.setColor(Color.YELLOW);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 45f * (0.7f + 0.3f * pop)));
            int x = gp.screenWidth/2 - g2.getFontMetrics().stringWidth(turnMessage)/2;
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, alpha)));
            g2.drawString(turnMessage, x, gp.screenHeight/2);
            g2.setComposite(old);
        }
    }
}