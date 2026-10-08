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
                // Anchored to the right (80%)
                int enemyX = (int)(gp.screenWidth * 0.9) - enemySize;
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
        g2.drawString("ERON", x, y - (int)(gp.screenHeight * 0.015));

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 130f));
        String healthText = gp.player.life + " / " + gp.player.maxLife;
        if (gp.player.shield > 0) healthText += " (+🛡️ " + gp.player.shield + ")";
        g2.drawString(healthText, x, y + barHeight + (int)(gp.screenHeight * 0.02));

        drawEntityStatuses(g2, gp.player, x, y + (int)(gp.screenHeight * 0.04));
    }

    private void drawEnemyStats(Graphics2D g2, int groundY) {
        if (gp.player.currentEnemy == null) return;
        Enemy enemy = gp.player.currentEnemy;

        int enemySize = (int)(gp.screenWidth * 0.1 * enemy.battleSize);
        int enemyX = (int)(gp.screenWidth * 0.85) - enemySize;

        int barWidth = (int)(gp.screenWidth * 0.10);
        int barHeight = (int)(gp.screenHeight * 0.015);
        int x = enemyX + (enemySize / 2) - (barWidth / 2);
        int y = groundY + (int)(gp.screenHeight * 0.02);

        // Fundalul barei
        g2.setColor(new Color(30, 30, 30, 200));
        g2.fillRoundRect(x, y, barWidth, barHeight, 8, 8);

        // HP Bar (Roșu)
        double hpPercent = (double) enemy.life / enemy.maxLife;
        int hpWidth = (int) (barWidth * Math.max(0, Math.min(1, hpPercent)));
        g2.setColor(new Color(231, 76, 60));
        g2.fillRoundRect(x, y, hpWidth, barHeight, 8, 8);

        // ★ MODIFICARE: Randare SCUT pe bara inamicului
        if (enemy.shield > 0) {
            double shieldPercent = (double) enemy.shield / enemy.maxLife;
            int shieldWidth = (int) (barWidth * Math.max(0, Math.min(1, shieldPercent)));

            g2.setColor(new Color(41, 128, 185, 210)); // Albastru scut corporatist
            g2.fillRoundRect(x, y, Math.min(barWidth, shieldWidth), barHeight, 8, 8);
        }

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, barWidth, barHeight, 8, 8);

        // Numele inamicului
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 115f));
        g2.drawString(enemy.name.toUpperCase(), x, y - (int)(gp.screenHeight * 0.015));

        // HP + Shield Text sub bară
        String hpText = enemy.life + " / " + enemy.maxLife;
        if (enemy.shield > 0) hpText += " (+🛡️ " + enemy.shield + ")";
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 130f));
        g2.drawString(hpText, x + (barWidth / 2) - (g2.getFontMetrics().stringWidth(hpText) / 2), y + barHeight + (int)(gp.screenHeight * 0.02));

        drawEntityStatuses(g2, enemy, x, y + barHeight + (int)(gp.screenHeight * 0.04));
    }

    private void drawEnemyIntent(Graphics2D g2) {
        if (gp.player.currentEnemy == null) return;
        Enemy e = gp.player.currentEnemy;

        // --- CALCUL POZIȚIE DINAMICĂ DUPĂ ÎNĂLȚIMEA ȘI LOCAȚIA INAMICULUI ---
        int groundY = (int)(gp.screenHeight * 0.87); // Aceeași linie de sol folosită la desenare
        int enemySize = (int)(gp.screenWidth * 0.20 * e.battleSize); // Dimensiunea exactă a sprite-ului de bătălie

        // Centrul inamicului pe axa X (calculat identic ca în secțiunea 3 din draw())
        int enemyX = (int)(gp.screenWidth * 0.9) - enemySize;
        int centerX = enemyX + (enemySize / 2);

        // Poziția Y: Solul - Înălțimea inamicului - 30 pixeli spațiu liber
        int y = groundY - enemySize - 30;

        // Setăm fontul dorit înainte de a calcula dimensiunea textului
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, gp.screenWidth / 50f));

        Color intentColor = Color.WHITE;
        String intentString = "❓";

        if (e.nextAction != null) {
            switch (e.nextAction) {
                case ATTACK -> {
                    intentColor = new Color(255, 50, 50);   // ROȘU
                    intentString = "⚔ " + e.intentValue;
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
            Card card = currentHand.get(i);
            int x = startX + (i * (cardWidth + spacing));

            // Ridicăm complet cartea când este selectată pentru a putea fi citită perfect
            int y = (i == commandNum) ? gp.screenHeight - cardHeight - 20 : baseSlotY;

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