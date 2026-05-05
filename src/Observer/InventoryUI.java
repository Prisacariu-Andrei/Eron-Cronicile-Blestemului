package Observer;

import Main.GamePanel;
import Entity.Items.Card;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public class InventoryUI {
    GamePanel gp;
    public int slotRow = 0;
    private final int MAX_VISIBLE_ROWS = 15; // Definit ca variabilă de clasă pentru a fi accesibilă peste tot

    public InventoryUI(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2) {
        if(gp.player == null) return;

        // 1. CALCULĂM DIMENSIUNILE (Prima dată)
        int x = gp.tileSize * 2;
        int y = gp.tileSize;
        int width = gp.screenWidth - (gp.tileSize * 4);
        int height = gp.screenHeight - (gp.tileSize * 2);

        // 2. DESENĂM FUNDALUL
        g2.setColor(new Color(20, 20, 30, 240));
        g2.fillRoundRect(x, y, width, height, 35, 35);

        g2.setColor(new Color(255, 215, 0, 200));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(x, y, width, height, 35, 35);

        // 3. TITLU ȘI LINIE
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 36F));
        g2.drawString("GRIMOARUL LUI ERON", x + gp.tileSize, y + gp.tileSize);
        g2.setStroke(new BasicStroke(1));
        g2.drawLine(x + gp.tileSize, y + gp.tileSize + 10, x + width - gp.tileSize, y + gp.tileSize + 10);

        // 4. SECȚIUNE RUNE
        drawRuneSection(g2, x, y);

        // 5. SECȚIUNE CĂRȚI
        ArrayList<Card> sortedCards = new ArrayList<>(gp.player.inventory.cards);
        sortedCards.sort(Comparator.comparing(card -> card.name));

        int startX = x + width / 2;
        int startY = y + gp.tileSize * 3;

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22F));
        g2.setColor(new Color(255, 100, 100));
        g2.drawString("PACHETUL DE LUPTĂ:", startX, y + gp.tileSize * 2 + 20);

        g2.setFont(g2.getFont().deriveFont(18F));
        for (int i = 0; i < MAX_VISIBLE_ROWS; i++) {
            int cardIndex = i + slotRow;
            if (cardIndex < sortedCards.size()) {
                Card c = sortedCards.get(cardIndex);
                int currentY = startY + (i * 30);

                g2.setColor(new Color(100, 200, 255));
                g2.drawString("[" + c.manaCost + "]", startX, currentY);
                g2.setColor(Color.WHITE);
                g2.drawString(c.name, startX + 40, currentY);
            }
        }

        // 6. INDICATOR SCROLL
        if (sortedCards.size() > MAX_VISIBLE_ROWS) {
            drawScrollIndicator(g2, startX + (width / 2) - 60, startY, sortedCards.size());
        }

        // 7. INFO SUBSOL
        g2.setFont(g2.getFont().deriveFont(14F));
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("Cărți: " + sortedCards.size() + " | Rune: " + gp.player.inventory.runes.size() + "/" + gp.player.inventory.MAX_RUNES, x + gp.tileSize, y + height - 30);
        g2.drawString("Apasă [I] pentru a închide", x + width - 250, y + height - 30);
    }

    private void drawRuneSection(Graphics2D g2, int x, int y) {
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22F));
        g2.setColor(Color.CYAN);
        g2.drawString("RUNE ACTIVE:", x + gp.tileSize, y + gp.tileSize * 2 + 20);

        g2.setFont(g2.getFont().deriveFont(18F));
        if(gp.player.inventory.runes.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.drawString("Nicio rună echipată", x + gp.tileSize + 20, y + gp.tileSize * 3);
        } else {
            g2.setColor(Color.WHITE);
            for(int i = 0; i < gp.player.inventory.runes.size(); i++) {
                g2.drawString("◆ " + gp.player.inventory.runes.get(i).name, x + gp.tileSize + 20, y + gp.tileSize * 3 + (i * 30));
            }
        }
    }

    private void drawScrollIndicator(Graphics2D g2, int scrollX, int startY, int totalCards) {
        int barHeight = MAX_VISIBLE_ROWS * 30;
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(scrollX, startY - 20, 5, barHeight);

        int knobHeight = Math.max(20, (int)((double)MAX_VISIBLE_ROWS / totalCards * barHeight));
        int knobY = startY - 20 + (slotRow * barHeight / totalCards);

        g2.setColor(Color.YELLOW);
        g2.fillRect(scrollX - 2, knobY, 9, knobHeight);
    }
}