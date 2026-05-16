package Observer;

import Entity.Items.Card;
import Entity.Player.CardDatabase;
import Main.GamePanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RewardUI {
    GamePanel gp;
    public ArrayList<Card> rewardCards = new ArrayList<>();
    public int commandNum = 0;

    public RewardUI(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2) {
        // 1. Fundal semi-transparent
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // 2. Titlu
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 42F));
        g2.setColor(Color.WHITE);
        String title = "DESTINUL ÎȚI OFERĂ O ALEGERE...";
        int titleX = gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(title) / 2;
        g2.drawString(title, titleX, 120);

        // 3. Iterăm prin recompense
        for (int i = 0; i < rewardCards.size(); i++) {
            Card currentCard = rewardCards.get(i); // Luăm obiectul Card curent

            int cardX = gp.tileSize * 2-5 + (i * gp.tileSize * 6); // Spațiere mai generoasă
            int cardY = gp.screenHeight / 2 - gp.tileSize * 2;
            int cardWidth = gp.tileSize * 4;
            int cardHeight = gp.tileSize * 6;

            // --- EFECT DE SELECȚIE ---
            if (i == commandNum) {
                g2.setColor(new Color(255, 215, 0)); // Auriu
                g2.setStroke(new BasicStroke(6));
                g2.drawRoundRect(cardX - 10, cardY - 10, cardWidth + 20, cardHeight + 20, 25, 25);

                // Un mic glow interior
                g2.setColor(new Color(255, 255, 255, 50));
                g2.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 20, 20);
            }

            // --- CORPUL CĂRȚII ---
            g2.setColor(new Color(30, 30, 50)); // Fundal închis la culoare
            g2.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 20, 20);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(cardX, cardY, cardWidth, cardHeight, 20, 20);

            // --- MANA COST (Cerc sus-stânga) ---
            g2.setColor(new Color(0, 191, 255)); // Deep Sky Blue
            g2.fillOval(cardX - 15, cardY - 15, 40, 40);
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 24F));
            g2.drawString(String.valueOf(currentCard.manaCost), cardX - 3, cardY + 14);

            // --- NUME CARTE ---
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));
            int nameX = cardX + (cardWidth / 2) - (g2.getFontMetrics().stringWidth(currentCard.name) / 2);
            g2.drawString(currentCard.name, nameX, cardY + 50);

            // --- DESCRIERE (Folosim variabila 'descriere' din Card) ---
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16F));
            g2.setColor(Color.LIGHT_GRAY);

            // Dacă descrierea e lungă, o putem "împacheta" sau afișa simplu
            // Aici presupun că ai variabila 'descriere' în clasa Card
            String description = currentCard.description;
            int descY = cardY + 120;

            // Logica simplă de split pentru descriere (dacă are \n)
            for (String line : description.split("\n")) {
                g2.drawString(line, cardX + 20, descY);
                descY += 20;
            }

            // --- TIP CARTE (Dacă ai variabila 'type') ---
            g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 14F));
            g2.setColor(Color.CYAN);
            g2.drawString("[" + currentCard.type + "]", cardX + 20, cardY + cardHeight - 20);
        }

        // --- INSTRUCȚIUNI JOS ---
        g2.setFont(g2.getFont().deriveFont(18F));
        g2.setColor(Color.WHITE);
        String subText = "[ENTER] Alege Cartea    [ESC] Ignoră Recompensa";
        int subX = gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(subText) / 2;
        g2.drawString(subText, subX, gp.screenHeight - 80);
    }

    // În RewardUI.java adaugă această metodă la final:
    public Card getSelectedCard() {
        if (commandNum >= 0 && commandNum < rewardCards.size()) {
            return rewardCards.get(commandNum);
        }
        return null;
    }

    // În interiorul clasei RewardUI, modifică doar metoda generateRewards:
    public void generateRewards() {
        rewardCards.clear();

        // Apelăm Singleton-ul și pasăm jucătorul pentru că Factory are nevoie de el
        List<Card> newRewards = CardDatabase.getInstance().getRandomRewards(3, gp.player);
        rewardCards.addAll(newRewards);

        commandNum = 0;
    }
}