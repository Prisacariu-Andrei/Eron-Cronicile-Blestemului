package Observer;

import Entity.Items.Card;
import Entity.Player.CardDatabase;
import Main.Easing;
import Main.GamePanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RewardUI {
    GamePanel gp;
    public ArrayList<Card> rewardCards = new ArrayList<>();
    public int commandNum = 0;
    private int openTicks = 0; // de câte ticks e deschis ecranul (cărțile intră pe rând)

    public RewardUI(GamePanel gp) {
        this.gp = gp;
    }

    public void onOpen() { openTicks = 0; }

    public void update() { openTicks++; }

    // Centrul cărții selectate pe ecran (pentru explozia de particule la alegere)
    public int selectedCenterX() { return (int)(gp.screenWidth * 0.1) + (int)(commandNum * gp.screenWidth * 0.3) + (int)(gp.screenWidth * 0.1); }
    public int selectedCenterY() { return (int)(gp.screenHeight * 0.3) + (int)(gp.screenHeight * 0.25); }

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

        // Monedele primite de la inamicul învins
        if (gp.player.lastCoinReward > 0) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 26F));
            g2.setColor(new Color(255, 221, 100));
            String coinText = "+" + gp.player.lastCoinReward + " monede";
            int cw = g2.getFontMetrics().stringWidth(coinText);
            ShopUI.drawCoin(g2, gp.screenWidth / 2 - cw / 2 - 40, 150, 28);
            g2.drawString(coinText, gp.screenWidth / 2 - cw / 2, 175);
        }

        // 3. Iterăm prin recompense
        for (int i = 0; i < rewardCards.size(); i++) {
            Card currentCard = rewardCards.get(i); // Luăm obiectul Card curent

            int cardX = (int)(gp.screenWidth * 0.1)  + (int)(i * gp.screenWidth * 0.3); // Spațiere mai generoasă
            // Cărțile urcă pe rând din partea de jos, cu un mic overshoot, și apar din transparență
            float appear = (openTicks - i * 5) / 16f;
            int cardY = (int)(gp.screenHeight*0.3) + Math.round((1f - Easing.outBack(appear)) * 140);
            int cardWidth = (int)(gp.screenWidth * 0.2);
            int cardHeight = (int)(gp.screenHeight * 0.5);
            Composite cardComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, Easing.clamp01(appear * 1.6f))));

            // --- EFECT DE SELECȚIE ---
            if (i == commandNum) {
                // Conturul auriu pulsează încet, ca să se vadă că așteaptă alegerea
                float pulse = 0.5f + 0.5f * (float) Math.sin(gp.fx.ticks * 0.15);
                int grow = Math.round(4 * pulse);
                g2.setColor(new Color(255, 215, 0, 190 + Math.round(65 * pulse))); // Auriu
                g2.setStroke(new BasicStroke(6));
                g2.drawRoundRect(cardX - 10 - grow, cardY - 10 - grow, cardWidth + 20 + 2 * grow, cardHeight + 20 + 2 * grow, 25, 25);

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
            g2.setComposite(cardComposite);
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