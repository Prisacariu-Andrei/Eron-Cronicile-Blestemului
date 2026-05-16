package Observer;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.StatusEffect;
import Main.GamePanel;
import Entity.Entity;

import java.awt.*;
import java.util.ArrayList;

public class BattleUI {
    GamePanel gp;
    public int commandNum = 0;
    public String turnMessage = "";
    public int turnMessageTimer = 0;

    public boolean isTransitioning = false;
    public int transitionTimer = 0;
    private String transitionPhaseName = "";
    private Color transitionColor;

    public BattleUI(GamePanel gp) {
        this.gp = gp;
    }

    public void announceTurn(String text) {
        this.turnMessage = text;
        this.turnMessageTimer = 30;
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
            g2.drawImage(gp.player.fightStand, playerX, groundY - eronHeight, eronWidth, eronHeight, null);
        }

        // 3. DESENARE INAMIC
        if (gp.player.currentEnemy != null) {
            Enemy enemy = gp.player.currentEnemy;
            int enemySize = (int)(gp.screenWidth * 0.20 * enemy.battleSize);

            if (enemy.fightStand != null) {
                // Anchored to the right (85%)
                int enemyX = (int)(gp.screenWidth * 0.95) - enemySize;
                g2.drawImage(enemy.fightStand, enemyX, groundY - enemySize, enemySize, enemySize, null);
            }
        }

        // 4. INTERFAȚĂ (Static methods converted to dynamic)
        drawPlayerStats(g2, groundY);
        drawEnemyStats(g2, groundY);
        drawCardHand(g2);
        drawManaPool(g2,groundY);
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

    private void drawEnemyStats(Graphics2D g2, int groundY) {
        if (gp.player.currentEnemy == null) return;
        Enemy enemy = gp.player.currentEnemy;

        int enemySize = (int)(gp.screenWidth * 0.1 * enemy.battleSize);
        int enemyX = (int)(gp.screenWidth * 0.95) - enemySize;

        int barWidth = (int)(gp.screenWidth * 0.10);
        int barHeight = (int)(gp.screenHeight * 0.015);
        int x = enemyX + (enemySize / 2) - (barWidth / 2);
        int y = groundY + (int)(gp.screenHeight * 0.02);

        // Bar Background
        g2.setColor(new Color(30, 30, 30, 200));
        g2.fillRoundRect(x, y, barWidth, barHeight, 8, 8);

        // HP Bar
        double hpPercent = (double) enemy.life / enemy.maxLife;
        int hpWidth = (int) (barWidth * Math.max(0, Math.min(1, hpPercent)));
        g2.setColor(new Color(231, 76, 60));
        g2.fillRoundRect(x, y, hpWidth, barHeight, 8, 8);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, barWidth, barHeight, 8, 8);

        // Name (Above Head)
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 115f));
        g2.drawString(enemy.name.toUpperCase(), x, y - (int)(gp.screenHeight * 0.015));

        // HP Text
        String hpText = enemy.life + " / " + enemy.maxLife;
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 130f));
        g2.drawString(hpText, x + (barWidth / 2) - (g2.getFontMetrics().stringWidth(hpText) / 2), y + barHeight + (int)(gp.screenHeight * 0.02));

        if (enemy.shield > 0) {
            g2.setColor(new Color(52, 152, 219));
            g2.drawString("🛡 " + enemy.shield, x + barWidth + 10, y + barHeight);
        }

        drawEntityStatuses(g2, enemy, x, y + barHeight + (int)(gp.screenHeight * 0.03));
    }

    private void drawPlayerStats(Graphics2D g2, int groundY) {
        int x = (int)(gp.screenWidth * 0.06);
        int y = groundY + (int)(gp.screenHeight * 0.02);
        int barWidth = (int)(gp.screenWidth * 0.10);
        int barHeight = (int)(gp.screenHeight * 0.012);

        g2.setColor(new Color(50, 50, 50, 200));
        g2.fillRoundRect(x, y, barWidth, barHeight, 5, 5);

        double hpPercent = (double) gp.player.life / gp.player.maxLife;
        int hpWidth = (int) (barWidth * Math.max(0, Math.min(1, hpPercent)));

        g2.setColor((gp.player.shield > 0) ? new Color(52, 152, 219) : new Color(46, 204, 113));
        g2.fillRoundRect(x, y, hpWidth, barHeight, 5, 5);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, barWidth, barHeight, 5, 5);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 115f));
        g2.drawString("ERON", x, y - (int)(gp.screenHeight * 0.015));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 130f));
        g2.drawString(gp.player.life + " / " + gp.player.maxLife, x+ (int)(gp.screenWidth*0.035), y + (int)(gp.screenWidth*0.015));

        if (gp.player.shield > 0) {
            g2.setColor(new Color(52, 152, 219));
            g2.drawString("🛡 " + gp.player.shield, x + barWidth + 10, y + 10);
        }
        drawEntityStatuses(g2, gp.player, x, y + (int)(gp.screenHeight * 0.025));
    }

    private void drawCardHand(Graphics2D g2) {
        ArrayList<Card> currentHand = gp.player.inventory.hand;
        if (currentHand.isEmpty()) return;

        // --- 1. DYNAMIC ANCHORS (Central 50% Zone) ---
        int cardZoneWidth = gp.screenWidth / 2;
        int cardZoneStartX = (int)(gp.screenWidth / 4.5);

        // Reducem cardWidth de la 0.1 la 0.06 (6% din ecran)
        // Raportul 1.6 oferă o formă elegantă, suficient de înaltă pentru descriere
        int cardWidth = (int)(gp.screenWidth * 0.06);
        int cardHeight = (int)(cardWidth * 1.6);

        int maxCards = 8;
        int numCards = Math.min(currentHand.size(), maxCards);

        // --- SPACING LOGIC ---
        // Calculăm spațiul disponibil pentru a le distribui frumos
        int spacing = (numCards > 1) ? (cardZoneWidth - (numCards * cardWidth)) / (numCards + 1) : 20;
        // Permitem un spațiu mai generos (până la 5% din lățimea ecranului)
        spacing = Math.min(spacing, (int)(gp.screenWidth * 0.05));

        int totalHandWidth = (numCards * cardWidth) + ((numCards - 1) * spacing);
        int startX = cardZoneStartX + (cardZoneWidth / 2) - (totalHandWidth / 2);

        // Poziția de bază (puțin mai jos ca să nu acopere centrul ecranului)
        int baseSlotY = gp.screenHeight - (int)(gp.screenHeight * 0.05);

        // --- 2. DRAWING LOOP ---
        for (int i = 0; i < numCards; i++) {
            Card card = currentHand.get(i);
            int x = startX + (i * (cardWidth + spacing));

            // Ridicăm cartea când este selectată (păstrăm 15% vizibil în mână)
            int y = (i == commandNum) ? baseSlotY - (int)(cardHeight * 0.85) : baseSlotY;

            // Randare Shadow (opțional, pentru profunzime)
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRoundRect(x + 3, y + 3, cardWidth, cardHeight, 15, 15);

            // Card Background
            g2.setColor(new Color(40, 40, 60, 250));
            g2.fillRoundRect(x, y, cardWidth, cardHeight, 15, 15);

            // Border
            if (i == commandNum) {
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(3));
            } else {
                g2.setColor(new Color(200, 200, 200));
                g2.setStroke(new BasicStroke(1));
            }
            g2.drawRoundRect(x, y, cardWidth, cardHeight, 15, 15);

            // --- 3. DYNAMIC CONTENT RENDERING ---

            // A. Card Name
            // Fontul este acum relativ la cardWidth (aprox 1/8 din lățimea cărții)
            float titleFontSize = cardWidth * 0.14f;
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, titleFontSize));
            g2.setColor(Color.WHITE);
            // Padding orizontal 10%, vertical 15%
            g2.drawString(card.name, x + (int)(cardWidth * 0.1), y + (int)(cardHeight * 0.15));

            // B. Description
            float descFontSize = cardWidth * 0.11f;
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, descFontSize));
            g2.setColor(new Color(220, 220, 220));

            String dynamicDesc = card.getDynamicDescription(gp.player);
            String[] lines = dynamicDesc.split("\n");

            for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
                // Începe descrierea de la 35% din înălțimea cărții
                int lineY = y + (int)(cardHeight * 0.35) + (lineIdx * (int)(descFontSize * 1.3));
                g2.drawString(lines[lineIdx], x + (int)(cardWidth * 0.1), lineY);
            }

            // C. Mana Crystal
            int crystalSize = (int)(cardWidth * 0.3);
            int crystalX = x - (crystalSize / 4);
            int crystalY = y - (crystalSize / 4);

            g2.setColor(new Color(41, 128, 185));
            g2.fillOval(crystalX, crystalY, crystalSize, crystalSize);

            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, titleFontSize));

            String cost = String.valueOf(card.manaCost);
            int costWidth = g2.getFontMetrics().stringWidth(cost);
            // Centrare perfectă în cristal
            g2.drawString(cost, crystalX + (crystalSize/2) - (costWidth/2), crystalY + (int)(crystalSize * 0.7));
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

    private void drawEnemyIntent(Graphics2D g2) {
        if (gp.player.currentEnemy == null) return;
        Enemy e = gp.player.currentEnemy;
        int x = (int)(gp.screenWidth * 0.75);
        int y = (int)(gp.screenHeight * 0.15);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 90f));
        g2.setColor(Color.YELLOW);
        g2.drawString("INTENȚIE: " + e.nextAction, x, y);
    }

    private void drawControlsAndPopups(Graphics2D g2) {
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 110f));
        g2.setColor(Color.WHITE);
        g2.drawString("[T] END TURN", (int)(gp.screenWidth * 0.88), gp.screenHeight - (int)(gp.screenHeight * 0.03));

        for (int i = 0; i < gp.popups.size(); i++) {
            DamagePopUp p = gp.popups.get(i);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha));
            g2.setColor(Color.BLACK);
            g2.drawString(p.text, p.x + 2, p.y + 2);
            g2.setColor(p.color);
            g2.drawString(p.text, p.x, p.y);
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private void drawMessages(Graphics2D g2) {
        if (isTransitioning) {
            transitionTimer++;
            g2.setColor(new Color(0, 0, 0, Math.min(200, transitionTimer * 5)));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
            g2.setColor(transitionColor);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 40f));
            String text = transitionPhaseName;
            int x = gp.screenWidth/2 - g2.getFontMetrics().stringWidth(text)/2;
            g2.drawString(text, x, gp.screenHeight/2);
            if (transitionTimer > 40) { isTransitioning = false; transitionTimer = 0; }
        } else if (turnMessageTimer > 0) {
            g2.setColor(Color.YELLOW);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 45f));
            int x = gp.screenWidth/2 - g2.getFontMetrics().stringWidth(turnMessage)/2;
            g2.drawString(turnMessage, x, gp.screenHeight/2);
            turnMessageTimer--;
        }
    }
}