package Observer;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.Cards.CardCatalog;
import Entity.Cards.Combos;
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
        dying.clear();
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
        for (Enemy e : p.team) if (e.alive || (dying.containsKey(e) && dying.get(e) < GameFeel.DEATH_ANIM_TICKS)) alive.add(e);   // și cei care cad acum
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

    /** Inamicii care tocmai au căzut: vârsta animației (în ticks). Se desenează și după moarte până se termină. */
    private final java.util.Map<Enemy, Integer> dying = new java.util.IdentityHashMap<>();

    private void updateDying() {
        for (Enemy e : gp.player.team) {
            if (!e.alive && !dying.containsKey(e)) {
                dying.put(e, 0);
                Slot s = slotOf(e);
                if (s != null) gp.fx.enemyDeath(s.cx(), groundY() - s.size / 2);
            }
        }
        for (var en : dying.entrySet()) en.setValue(en.getValue() + 1);
    }

    /** Moartea unui inamic: pâlpâie, apoi se scufundă puțin, se micșorează și se stinge. */
    private void drawDying(Graphics2D g2, Slot s, int groundY) {
        int age = dying.getOrDefault(s.e, 0);
        float t = Easing.clamp01(age / (float) GameFeel.DEATH_ANIM_TICKS);
        float flicker = age < 14 ? (age % 4 < 2 ? 0.45f : 1f) : 1f;
        float alpha = flicker * (1f - Easing.clamp01((t - 0.25f) / 0.75f));
        int dw = Math.round(s.size * (1f - 0.22f * t)), dh = dw;
        int shake = age < 14 ? Math.round((float) Math.sin(age * 2.2) * 5f) : 0;
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, alpha)));
        g2.drawImage(s.e.fightStand, s.x + (s.size - dw) / 2 + shake, groundY - dh + Math.round(t * s.size * 0.12f), dw, dh, null);
        g2.setComposite(old);
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
        gp.player.combo.tick();
        updateDying();
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

        for (var a : gp.party.members) if (a.sayTicks > 0) a.sayTicks--;
        if (enemyHitTimer > 0) enemyHitTimer--;
        if (playerHitTimer > 0) playerHitTimer--;
    }

    /** Desenează un personaj de luptă: respiră ușor când stă și se strivește/dă înapoi când e lovit. */
    /** Siluete colorate (alb / roșu) ale sprite-urilor, făcute o singură dată: fulgerul de pe personajul lovit. */
    private final java.util.Map<BufferedImage, BufferedImage> whiteSilhouettes = new java.util.IdentityHashMap<>(),
            redSilhouettes = new java.util.IdentityHashMap<>();

    private static BufferedImage silhouette(java.util.Map<BufferedImage, BufferedImage> cache, BufferedImage src, Color c) {
        return cache.computeIfAbsent(src, s -> {
            BufferedImage out = new BufferedImage(s.getWidth(), s.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(s, 0, 0, null);
            g.setComposite(AlphaComposite.SrcAtop);   // doar peste pixelii desenați ai sprite-ului
            g.setColor(c);
            g.fillRect(0, 0, s.getWidth(), s.getHeight());
            g.dispose();
            return out;
        });
    }

    private void drawFighter(Graphics2D g2, BufferedImage img, int x, int groundY, int w, int h, int hitTimer, float phase) {
        drawFighter(g2, img, x, groundY, w, h, hitTimer, phase, false);
    }

    /** redFlash: Eron a fost lovit (fulger roșu); altfel inamicul lovit de Eron are fulger alb. */
    private void drawFighter(Graphics2D g2, BufferedImage img, int x, int groundY, int w, int h, int hitTimer, float phase, boolean redFlash) {
        float breath = (float) Math.sin((gp.fx.ticks + phase) * 0.07) * GameFeel.BATTLE_BREATH_SCALE;
        float t = hitTimer / (float) HIT_ANIM_TICKS;               // 1 la lovitură, scade spre 0
        float squash = 0.14f * Easing.outQuad(t);
        int recoil = (int) (Math.sin(hitTimer * 1.9) * t * 16);
        int dw = Math.round(w * (1 + squash));
        int dh = Math.round(h * (1 + breath - squash));
        g2.drawImage(img, x - (dw - w) / 2 + recoil, groundY - dh, dw, dh, null);
        if (hitTimer > 0) {   // fulger scurt: alb când Eron lovește, roșu când e lovit Eron
            BufferedImage tint = redFlash ? silhouette(redSilhouettes, img, new Color(255, 40, 40)) : silhouette(whiteSilhouettes, img, Color.WHITE);
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, GameFeel.FLASH_ALPHA * Math.min(1f, t * 1.5f)));
            g2.drawImage(tint, x - (dw - w) / 2 + recoil, groundY - dh, dw, dh, null);
            g2.setComposite(old);
        }
    }

    public void draw(Graphics2D g2) {
        // --- DINAMIC ANCHORS ---
        int groundY = (int)(gp.screenHeight * 0.87); // Ground line at 75% height

        // 1. FUNDAL
        drawBackground(g2);

        drawAllySprites(g2);   // aliații stau în spatele lui Eron: se desenează înaintea lui

        // 2. DESENARE ERON (PLAYER)
        // Scale width to 10% of screen width, height is double that
        int eronWidth = (int)(gp.screenWidth * 0.10);
        int eronHeight = eronWidth * 2;

        if (gp.player.fightStand != null) {
            int playerX = (int)(gp.screenWidth * 0.08);
            drawFighter(g2, gp.player.fightStand, playerX, groundY, eronWidth, eronHeight, playerHitTimer, 0f, true);
        }

        // 3. DESENARE INAMICI (unul sau o echipă)
        java.util.List<Slot> slots = slots();
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            if (s.e.fightStand == null) continue;
            if (!s.e.alive) { drawDying(g2, s, groundY); continue; }
            boolean target = s.e == gp.player.currentEnemy;
            drawFighter(g2, s.e.fightStand, s.x, groundY, s.size, s.size, target ? enemyHitTimer : 0, 40f + i * 17f);

        }

        drawAllyTags(g2);          // numele aliaților și textul acțiunii lor, peste toate sprite-urile
        // 4. INTERFAȚĂ (Static methods converted to dynamic)
        drawPlayerStats(g2, groundY);
        drawEnemyStats(g2, groundY);
        drawTargetHint(g2);
        drawCardHand(g2);
        drawManaPool(g2,groundY);
        drawAbilities(g2);
        drawComboCounter(g2);
        drawEnemyIntent(g2);
        drawControlsAndPopups(g2);

        // 5. TRANZIȚII ȘI MESAJE
        drawMessages(g2);
    }

    /** Fundalul zonei, pus de BattleState la începutul luptei (null = rămâne fundalul vechi al inamicului). */
    public java.awt.image.BufferedImage zoneBackground;

    private void drawBackground(Graphics2D g2) {
        java.awt.image.BufferedImage zoneBg = zoneBackground != null ? zoneBackground : Main.ZoneInfo.battleImage(gp.tileM.currentZone, gp.screenWidth, gp.screenHeight);
        if (zoneBg != null) {
            g2.drawImage(zoneBg, 0, 0, gp.screenWidth, gp.screenHeight, null);
        } else if (gp.player.currentEnemy != null && gp.player.currentEnemy.battleBackground != null) {
            g2.drawImage(gp.player.currentEnemy.battleBackground, 0, 0, gp.screenWidth, gp.screenHeight, null);
        } else {
            g2.setColor(new Color(20, 20, 40));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
    }

    /** Aliații lui Eron (allies.json): sprite cam cât Eron, în spatele lui (puțin în stânga și mai sus). Inamicii nu îi pot ținti, deci nu au bară de viață. */
    private void drawAllySprites(Graphics2D g2) {
        for (var a : gp.party.members) {
            var img = a.image();
            if (img == null) continue;
            int cx = gp.party.allyX(a), h = gp.party.allyHeight();
            int w = h * img.getWidth() / Math.max(1, img.getHeight());
            float bob = (float) Math.sin(gp.fx.ticks * 0.08 + cx) * 3f;
            g2.drawImage(img, cx - w / 2, gp.party.allyGroundY() - h - Math.round(bob), w, h, null);
        }
    }

    /** Numele aliatului și, câteva secunde, ce a făcut tura asta (text care urcă și se stinge deasupra lui). */
    private void drawAllyTags(Graphics2D g2) {
        Font old = g2.getFont();
        for (var a : gp.party.members) {
            int cx = gp.party.allyX(a), head = gp.party.allyHeadY();
            g2.setFont(old.deriveFont(Font.BOLD, gp.screenWidth / 115f));
            g2.setColor(new Color(0, 0, 0, 170));
            g2.drawString(a.name, cx - g2.getFontMetrics().stringWidth(a.name) / 2 + 1, head + 1);
            g2.setColor(new Color(a.color));
            g2.drawString(a.name, cx - g2.getFontMetrics().stringWidth(a.name) / 2, head);
            if (a.sayTicks > 0 && !a.say.isEmpty()) {
                float life = a.sayTicks / 150f;
                int rise = (int) ((1f - life) * 18);
                g2.setFont(old.deriveFont(Font.BOLD, gp.screenWidth / 80f));
                int tw = g2.getFontMetrics().stringWidth(a.say);
                int tx = Math.max(8, cx - tw / 2), ty = head - 22 - rise;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, a.sayTicks / 30f)));
                g2.setColor(new Color(0, 0, 0, 190));
                g2.fillRoundRect(tx - 8, ty - g2.getFontMetrics().getAscent() - 2, tw + 16, g2.getFontMetrics().getHeight() + 6, 10, 10);
                g2.setColor(new Color(a.color));
                g2.drawString(a.say, tx, ty);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
        }
        g2.setFont(old);
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
            if (!enemy.alive) continue;
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
        for (Slot s : slots()) if (s.e.alive) drawIntentFor(g2, s);
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

    private static java.util.List<String> wrapText(Graphics2D g2, String text, int width) {
        java.util.List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) {
            if (cur.length() > 0 && g2.getFontMetrics().stringWidth(cur + " " + w) > width) { lines.add(cur.toString()); cur.setLength(0); }
            if (cur.length() > 0) cur.append(' ');
            cur.append(w);
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    /** "COMBO FOC x2" cât ține tura, și numele ultimei reacții. */
    private void drawComboCounter(Graphics2D g2) {
        var st = gp.player.combo;
        if (st.bestCount >= 2 && st.bestTag != null) {
            String text = "COMBO " + CardCatalog.tagLabel(st.bestTag).toUpperCase() + " x" + st.bestCount;
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 48f));
            int x = gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(text) / 2, y = (int) (gp.screenHeight * 0.20);
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(text, x + 2, y + 2);
            g2.setColor(CardCatalog.tagColor(st.bestTag));
            g2.drawString(text, x, y);
        }
        if (st.bannerTicks > 0 && !st.banner.isEmpty()) {
            float a = Easing.clamp01(st.bannerTicks / 20f);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 38f));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, a)));
            g2.setColor(new Color(255, 232, 160));
            g2.drawString(st.banner, gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(st.banner) / 2, (int) (gp.screenHeight * 0.26));
            g2.setComposite(old);
        }
    }

    private void drawCardHand(Graphics2D g2) {

        ArrayList<Card> currentHand = gp.player.inventory.hand;
        if (currentHand.isEmpty()) return;

        // --- 1. DYNAMIC ANCHORS (Păstrăm cu strictețe zona cerută) ---
        int cardZoneWidth = gp.screenWidth / 2;
        int cardZoneStartX = (int)(gp.screenWidth / 4.5);

        // Mărim dimensiunea cărții: 10% din lățimea ecranului le oferă destul spațiu pentru text
        int cardWidth = (int)(gp.screenWidth * 0.10);
        int cardHeight = Math.round(cardWidth * CardFace.ASPECT); // raport 5:7, ca în schiță

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

        // --- 2. DRAWING LOOP --- (cartea selectată/ridicată se desenează ultima, deasupra celorlalte)
        Integer[] order = new Integer[numCards];
        for (int i = 0; i < numCards; i++) order[i] = i;
        java.util.Arrays.sort(order, java.util.Comparator.comparingDouble(i -> (i == commandNum ? 1000 : 0) + cardLift[i]));
        for (int i : order) {
            if (i >= dealtCards || cardDealDelay[i] > 0) continue; // încă nu a fost trasă
            Card card = currentHand.get(i);
            int x = startX + (i * (cardWidth + spacing));

            // Cartea selectată se ridică lin (easing) și crește ~1.3x, ca să poată fi citită perfect
            int selectedY = gp.screenHeight - cardHeight - 20;
            int y = Math.round(baseSlotY + (selectedY - baseSlotY) * cardLift[i]);
            // O carte nou trasă intră de jos, cu un mic overshoot
            float deal = Easing.outBack(cardDealAge[i] / (float) GameFeel.CARD_DEAL_TICKS);
            y += Math.round((1f - deal) * cardHeight * 1.2f);
            float scale = 1f + 0.3f * cardLift[i];
            int w = Math.round(cardWidth * scale), h = Math.round(cardHeight * scale);
            int dx = x - (w - cardWidth) / 2, dy = y - (h - cardHeight);

            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(dx + 4, dy + 4, w, h, 18, 18);

            int comboReady = Combos.ready(gp.player, card, gp.player.currentEnemy);   // 2 = combo/reacție gata, 1 = continuă un lanț
            boolean hot = comboReady == 2 && gp.player.isPlayerTurn;
            CardFace.draw(g2, card, gp.player, dx, dy, w, h, hot, i == commandNum && gp.player.isPlayerTurn);

            Stroke originalStroke = g2.getStroke();
            if (i == commandNum) {   // evidențiere galbenă pentru cartea selectată
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(dx, dy, w, h, 18, 18);
            }
            if (comboReady > 0 && gp.player.isPlayerTurn) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(gp.fx.ticks * 0.16 + i);
                Color glow = comboReady == 2 ? new Color(255, 190, 60, 120 + Math.round(120 * pulse)) : new Color(110, 220, 255, 90 + Math.round(70 * pulse));
                g2.setColor(glow);
                g2.setStroke(new BasicStroke(comboReady == 2 ? 5f : 3f));
                g2.drawRoundRect(dx - 3, dy - 3, w + 6, h + 6, 22, 22);
            }
            g2.setStroke(originalStroke);

            // numărul tastei care alege cartea (1-8), într-un mic marcaj sub elementul cărții
            int bs = Math.max(18, (int) (w * 0.13));
            g2.setColor(new Color(30, 20, 12, 220));
            g2.fillRoundRect(dx + w - bs - 6, dy + h - bs - 6, bs, bs, 8, 8);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, bs * 0.75f));
            g2.setColor(i == commandNum ? Color.YELLOW : new Color(225, 215, 190));
            String num = String.valueOf(i + 1);
            g2.drawString(num, dx + w - bs - 6 + (bs - g2.getFontMetrics().stringWidth(num)) / 2, dy + h - 6 - bs * 0.22f);
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

    /** Statusurile ca "chip-uri" colorate: nume + ture rămase (Ars 3, Ud 2, Înghețat 3, Încărcat 2...). */
    private void drawEntityStatuses(Graphics2D g2, Entity entity, int x, int y) {
        if (entity.activeStatuses.isEmpty()) return;
        float fs = Math.max(11f, gp.screenWidth / 120f);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, fs));
        int cx = x, cy = y, pad = (int) (fs * 0.45), h = (int) (fs * 1.45), maxX = x + (int) (gp.screenWidth * 0.17);
        for (StatusEffect s : entity.activeStatuses) {
            String label = CardCatalog.statusLabel(s.type) + " " + s.duration;
            int w = g2.getFontMetrics().stringWidth(label) + pad * 2;
            if (cx + w > maxX && cx > x) { cx = x; cy += h + 3; }
            Color c = CardCatalog.statusColor(s.type);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
            g2.fillRoundRect(cx, cy - h + 4, w, h, 10, 10);
            g2.setColor(c);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(cx, cy - h + 4, w, h, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString(label, cx + pad, cy);
            cx += w + 4;
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
     * Abilitățile (progression.json) în colțul de sus-stânga: tasta, numele și efectul (cu bonusurile din rune, live).
     * Pe dreapta: nivelul necesar (blocate, estompate), turele rămase până se pot folosi din nou sau "1/luptă".
     */
    private void drawAbilities(Graphics2D g2) {
        var list = gp.progression.abilities;
        int x = 16, y = 96, w = 250, h = 48;
        Font old = g2.getFont();
        for (int i = 0; i < list.size() && i < 9; i++) {
            var a = list.get(i);
            boolean unlocked = gp.progression.isUnlocked(a);
            int wait = unlocked ? gp.player.abilityWait(a) : 0;
            boolean ready = unlocked && wait == 0 && gp.player.isPlayerTurn;
            int ry = y + i * (h + 6);

            g2.setColor(ready ? new Color(40, 38, 70, 235) : new Color(24, 24, 36, 215));
            g2.fillRoundRect(x, ry, w, h, 12, 12);
            g2.setColor(ready ? a.color : new Color(90, 90, 105));
            g2.setStroke(new BasicStroke(ready ? 2.5f : 1.5f));
            g2.drawRoundRect(x, ry, w, h, 12, 12);

            g2.setFont(old.deriveFont(Font.BOLD, 16F));
            g2.setColor(unlocked ? Color.WHITE : new Color(110, 110, 125));
            g2.drawString("[" + a.key + "]", x + 10, ry + 20);
            g2.setFont(old.deriveFont(Font.BOLD, 15F));
            g2.setColor(unlocked ? (ready ? Color.WHITE : new Color(165, 165, 180)) : new Color(110, 110, 125));
            g2.drawString(a.name, x + 44, ry + 20);
            // efectul, calculat din runele echipate acum
            g2.setFont(old.deriveFont(Font.PLAIN, 12F));
            g2.setColor(unlocked ? new Color(200, 200, 215, ready ? 255 : 150) : new Color(110, 110, 125));
            g2.drawString(gp.player.abilityText(a), x + 44, ry + 38);

            if (!unlocked) {
                g2.setFont(old.deriveFont(Font.PLAIN, 13F));
                g2.setColor(new Color(150, 150, 165));
                g2.drawString("Nv. " + a.unlockLevel, x + w - 52, ry + 20);
            } else if (wait != 0) {
                g2.setColor(new Color(90, 90, 110, 230));
                g2.fillOval(x + w - 36, ry + 8, 26, 26);
                g2.setColor(Color.WHITE);
                g2.setFont(old.deriveFont(Font.BOLD, 15F));
                String label = wait > 0 ? String.valueOf(wait) : "✓";
                g2.drawString(label, x + w - 23 - g2.getFontMetrics().stringWidth(label) / 2, ry + 27);
            } else if (a.oncePerBattle) {
                g2.setFont(old.deriveFont(Font.PLAIN, 11F));
                g2.setColor(new Color(150, 150, 165));
                g2.drawString("1/luptă", x + w - 50, ry + 20);
            }
        }
        g2.setFont(old);
    }

    private void drawControlsAndPopups(Graphics2D g2) {
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 110f));
        g2.setColor(Color.WHITE);
        g2.drawString("[T] END TURN", (int)(gp.screenWidth * 0.88), gp.screenHeight - (int)(gp.screenHeight * 0.03));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, gp.screenWidth / 125f));
        g2.setColor(new Color(200, 200, 215));
        String keysHint = "[1-8] alege carte   [ENTER] joacă   [Q W E R] abilități   [↑/↓/TAB] țintă   [T] termină tura";
        g2.drawString(keysHint, gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(keysHint) / 2, (int)(gp.screenHeight * 0.115));

        Font baseFont = g2.getFont();
        // Copie a listei: popup-urile se pot adăuga/șterge între timp (alt thread), iar un indice vechi ar da eroare
        DamagePopUp[] popups = gp.popups.toArray(new DamagePopUp[0]);
        for (DamagePopUp p : popups) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha));
            // "Pop": apare mai mare decât final și se așază (easing cu overshoot)
            float fs = baseFont.getSize2D() * p.scale * p.size;
            if (Math.abs(fs - baseFont.getSize2D()) > 0.02f) g2.setFont(baseFont.deriveFont(Font.BOLD, fs));
            else g2.setFont(baseFont);
            // numerele lui Eron (x = 3 sau 4 dale, vezi Player) apar deasupra capului lui, nu peste panoul de abilități
            int px = p.x, py = p.y;
            if (p.x == gp.tileSize * 3 || p.x == gp.tileSize * 4) {
                px = playerCenterX() - gp.tileSize / 2 + (p.x - gp.tileSize * 3);
                py = p.y + (int) (gp.screenHeight * 0.30);
            }
            g2.setColor(Color.BLACK);
            g2.drawString(p.text, px + 2, py + 2);
            g2.setColor(p.color);
            g2.drawString(p.text, px, py);
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