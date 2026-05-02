package Observer;

import Entity.Inamici.Enemy;
import Entity.Items.Card;
import Entity.StatusEffect;
import Main.GamePanel;
import Entity.Entity;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class BattleUI {
    GamePanel gp;
    public int commandNum = 0;
    private BufferedImage eronSprite;

    public String turnMessage = "";
    public int turnMessageTimer = 0;

    // Variabile pentru tranziția fazelor
    public boolean isTransitioning = false;
    public int transitionTimer = 0;
    private String transitionPhaseName = "";
    private Color transitionColor;

    public BattleUI(GamePanel gp) {
        this.gp = gp;
    }

    public void announceTurn(String text) {
        this.turnMessage = text;
        this.turnMessageTimer = 30; // Mesajul va sta pe ecran ~1 secundă (60 frames)
    }

    public void draw(Graphics2D g2) {
        // 1. FUNDALUL LUPTEI
        g2.setColor(new Color(20, 20, 40));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // 2. DESENARE ERON (PLAYER)
        int eronSize = gp.tileSize;
        if (gp.player.fightStand != null) {
            g2.drawImage(gp.player.fightStand, gp.tileSize * 2 + 16, gp.tileSize * (gp.maxScreenRow - 10) - 16, eronSize * 2, eronSize * 4, null);
        }

        // 3. DESENARE INAMIC DINAMIC
        if (gp.player.currentEnemy != null) {
            int enemySize = gp.tileSize * 4;

            // Asigură-te că sprite-ul este actualizat la faza corectă
            gp.player.currentEnemy.getImage();
            BufferedImage currentEnemyImg = gp.player.currentEnemy.fightStand;

            if (currentEnemyImg != null) {
                g2.drawImage(currentEnemyImg, gp.tileSize * (gp.maxScreenCol - 6) + 16, gp.tileSize * (gp.maxScreenRow - 10) - 16, enemySize, enemySize, null);
            }
        }

        // 4. INTERFEȚA STATS & CARDS
        drawPlayerStats(g2);
        drawEnemyStats(g2);
        drawCardHand(g2);
        drawManaPool(g2);
        drawEnemyIntent(g2);

        // 5. INSTRUCȚIUNI CONTROL
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14F));
        g2.setColor(Color.WHITE);
        g2.drawString("[T] END TURN", gp.screenWidth - gp.tileSize * 3, gp.screenHeight - gp.tileSize);

        // 6. DAMAGE POPUPS (Sistemul de particule pentru numere)
        for (int i = 0; i < gp.popups.size(); i++) {
            DamagePopUp p = gp.popups.get(i);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha));

            // Umbră text
            g2.setColor(Color.BLACK);
            g2.drawString(p.text, p.x + 2, p.y + 2);

            // Text colorat
            g2.setColor(p.color);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22F));
            g2.drawString(p.text, p.x, p.y);
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // Reset opacitate

        // 7. TRANZIȚIE FAZE (O randăm independent de turnMessageTimer)
        if (isTransitioning) {
            ++transitionTimer;

            // Efect de ecran negru
            g2.setColor(new Color(0, 0, 0, Math.min(200, transitionTimer * 3)));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

            // Textul central de tranziție
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 45F));
            int textWidth = g2.getFontMetrics().stringWidth(transitionPhaseName);
            int x = gp.screenWidth / 2 - textWidth / 2;
            int y = gp.screenHeight / 2;

            g2.setColor(transitionColor);
            g2.drawString(transitionPhaseName, x, y);


            if (transitionTimer > 40) {
                isTransitioning = false;
                transitionTimer = 0;
            }
        }

        // 8. MESAJE DE TURN (Afișate doar dacă tranziția nu este activă)
        else if (turnMessageTimer > 0) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 40F));
            int textWidth = g2.getFontMetrics().stringWidth(turnMessage);
            int x = gp.screenWidth / 2 - textWidth / 2;
            int y = gp.screenHeight / 2;

            // Overlay negru în spatele textului pentru lizibilitate
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, y - 40, gp.screenWidth, 60);

            g2.setColor(Color.YELLOW);
            g2.drawString(turnMessage, x, y + 10);

            turnMessageTimer--;
        }
    }

    private void drawEntityStatuses(Graphics2D g2, Entity entity, int x, int y) {
        if (entity.activeStatuses.isEmpty()) return;

        int iconSize = 20;
        int currentX = x;

        for (StatusEffect s : entity.activeStatuses) {
            String icon = "";
            Color color = Color.WHITE;

            switch (s.type) {
                case BURN -> { icon = "🔥"; color = Color.ORANGE; }
                case POISON -> { icon = "🧪"; color = Color.GREEN; }
                case WEAK -> { icon = "💢"; color = Color.MAGENTA; }
                case SHIELD -> { icon = "🛡️"; color = Color.CYAN; }
            }

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18F));
            g2.setColor(color);
            g2.drawString(icon, currentX, y);

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14F));
            g2.setColor(Color.WHITE);
            g2.drawString(String.valueOf(s.duration), currentX + iconSize, y);

            currentX += 40;
        }
    }

    private void drawPlayerStats(Graphics2D g2) {
        int x = gp.tileSize * 2;
        int y = gp.tileSize * 8;

        g2.setColor(Color.GRAY);
        g2.fillRect(x, y, 200, 20);
        double oneScale = (double) 200 / gp.player.maxLife;
        double hpBarValue = oneScale * gp.player.life;
        Color hudColor = (gp.player.shield > 0) ? new Color(43, 157, 182) : new Color(29, 138, 31);
        g2.setColor(hudColor);
        g2.fillRect(x, y, (int)hpBarValue, 20);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(x, y, 200, 20);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16F));
        g2.drawString("ERON", x, y - 25);
        g2.setColor(hudColor);
        g2.drawString("HP: " + gp.player.life + "/" + gp.player.maxLife, x, y - 5);

        if (gp.player.shield > 0) {
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 25F));
            g2.setColor(Color.BLACK);
            g2.drawString("🛡️", x - 34, y + 16);
            g2.setColor(Color.WHITE);
            g2.drawString("🛡️", x - 35, y + 15);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14F));
            g2.drawString("" + gp.player.shield, x + 96, y + 15);
        }

        drawEntityStatuses(g2, gp.player, x, y + 40);
    }

    private void drawEnemyStats(Graphics2D g2) {
        if (gp.player.currentEnemy == null) return;

        int x = gp.screenWidth - gp.tileSize * 5;
        int y = gp.tileSize * 8;

        g2.setColor(Color.GRAY);
        g2.fillRect(x, y, 200, 20);
        double oneScale = (double) 200 / gp.player.currentEnemy.maxLife;
        double hpBarValue = oneScale * gp.player.currentEnemy.life;
        g2.setColor(new Color(255, 0, 0));
        g2.fillRect(x, y, (int)hpBarValue, 20);
        g2.setColor(Color.WHITE);
        g2.drawRect(x, y, 200, 20);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16F));
        String name = gp.player.currentEnemy.name;
        int nameWidth = g2.getFontMetrics().stringWidth(name);
        g2.drawString(name, x + 200 - nameWidth, y - 25);
        String hpText = "HP: " + gp.player.currentEnemy.life + "/" + gp.player.currentEnemy.maxLife;
        int hpTextWidth = g2.getFontMetrics().stringWidth(hpText);
        g2.drawString(hpText, x + 200 - hpTextWidth, y - 5);

        drawEntityStatuses(g2, gp.player.currentEnemy, x, y + 40);
    }

    private void drawEnemyIntent(Graphics2D g2) {
        if (gp.player.currentEnemy == null) return;

        int x = gp.screenWidth - gp.tileSize * 4;
        int y = gp.tileSize * 3;

        String intentText = "";
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18F));

        if (gp.player.currentEnemy.nextAction == Enemy.EnemyAction.ATTACK) {
            g2.setColor(Color.RED);
            intentText = "⚔️ ATAC: " + gp.player.currentEnemy.intentValue;
        } else if (gp.player.currentEnemy.nextAction == Enemy.EnemyAction.HEAL) {
            g2.setColor(Color.GREEN);
            intentText = "➕ VINDECARE: " + gp.player.currentEnemy.intentValue;
        } else if (gp.player.currentEnemy.nextAction == Enemy.EnemyAction.SPECIAL) {
            g2.setColor(Color.MAGENTA);
            intentText = "✨ SPECIAL: " + gp.player.currentEnemy.intentValue;
        } else if (gp.player.currentEnemy.nextAction == Enemy.EnemyAction.DEFEND) {
            g2.setColor(Color.CYAN);
            intentText = "🛡️ APĂRARE: " + gp.player.currentEnemy.intentValue;
        } else if (gp.player.currentEnemy.nextAction == Enemy.EnemyAction.BUFF) {
            g2.setColor(Color.PINK);
            intentText = "❓❓❓";
        } else {
            g2.setColor(Color.WHITE);
            intentText = "?G?R?E?S?E?A?L?A?";
        }

        g2.drawString(intentText, x, y);
    }

    private void drawCardHand(Graphics2D g2) {
        ArrayList<Card> currentHand = gp.player.inventory.hand;
        int cardWidth = gp.tileSize * 2;
        int cardHeight = gp.tileSize * 3;
        int spacing = 15;
        int totalWidth = (currentHand.size() * cardWidth) + ((currentHand.size() - 1) * spacing);
        int startX = (gp.screenWidth / 2) - (totalWidth / 2);
        int startY = gp.screenHeight - cardHeight - 30;

        for (int i = 0; i < currentHand.size(); i++) {
            Card card = currentHand.get(i);
            int x = startX + (i * (cardWidth + spacing));
            int y = startY;

            if (i == commandNum) {
                y -= 25;
                g2.setColor(new Color(255, 215, 0));
                g2.setStroke(new BasicStroke(4));
                g2.drawRoundRect(x - 3, y - 3, cardWidth + 6, cardHeight + 6, 15, 15);
            }

            g2.setColor(new Color(45, 45, 70));
            g2.fillRoundRect(x, y, cardWidth, cardHeight, 15, 15);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(x, y, cardWidth, cardHeight, 15, 15);

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13F));
            int nameWidth = g2.getFontMetrics().stringWidth(card.name);
            g2.drawString(card.name, x + (cardWidth/2) - (nameWidth/2), y + 30);

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10F));
            g2.setColor(new Color(200, 200, 200));

            String[] lines = card.description.split("\n");
            for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
                g2.drawString(lines[lineIdx], x + 10, y + 60 + (lineIdx * 15));
            }

            g2.setColor(new Color(50, 150, 255));
            g2.fillOval(x - 10, y - 10, 30, 30);
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14F));
            g2.drawString(String.valueOf(card.manaCost), x + 1, y + 11);
        }
    }

    private void drawManaPool(Graphics2D g2) {
        int x = gp.tileSize;
        int y = gp.screenHeight - (gp.tileSize * 4);
        g2.setColor(new Color(0, 100, 255, 200));
        g2.fillOval(x, y, 60, 60);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawOval(x, y, 60, 60);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));
        g2.drawString(gp.player.currentMana + "/" + gp.player.maxMana, x + 12, y + 38);
        g2.setFont(g2.getFont().deriveFont(12F));
        g2.drawString("ESENȚĂ", x + 5, y + 75);
    }
}