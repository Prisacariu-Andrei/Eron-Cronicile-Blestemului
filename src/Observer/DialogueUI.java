package Observer;

import Main.GamePanel;
import java.awt.*;

public class DialogueUI {
    GamePanel gp;
    public String currentDialogue = "";
    private int charIndex = 0;
    private int counter = 0;

    public DialogueUI(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2) {
        // SETĂRI FEREASTRĂ
        int x = gp.tileSize * 3;
        int y = gp.tileSize ;
        int width = gp.screenWidth - (gp.tileSize * 6);
        int height = gp.tileSize * 4;

        drawSubWindow(x, y, width, height, g2);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 24F));
        g2.setColor(Color.WHITE);

        // Verificăm dacă mai avem litere de afișat
        if (charIndex < currentDialogue.length()) {
            counter++;
            if (counter >= 2) { // Viteza: cu cât e numărul mai mare, cu atât e mai lent (2-3 e ideal)
                charIndex++;
                counter = 0;
                // Aici poți adăuga un mic sunet: gp.playSE(soundIndex);
            }
        }
        int Y=y;
        String textToDisplay = currentDialogue.substring(0, charIndex);
        for (String line : textToDisplay.split("\n")) {
            g2.drawString(line, x +gp.tileSize/2, Y+gp.tileSize);
            Y += 40;
        }

        g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 16F));
        g2.drawString("Apasă [E] pentru a continua...", x + width - gp.tileSize * 4, y + height - gp.tileSize/2);
    }

    private void drawSubWindow(int x, int y, int width, int height, Graphics2D g2) {
        Color c = new Color(0, 0, 0, 210); // Negru transparent
        g2.setColor(c);
        g2.fillRoundRect(x, y, width, height, 35, 35);

        c = new Color(255, 255, 255); // Contur alb
        g2.setColor(c);
        g2.setStroke(new BasicStroke(5));
        g2.drawRoundRect(x + 5, y + 5, width - 10, height - 10, 25, 25);
    }

    public void resetCharIndex() {
        this.charIndex = 0;   // Resetăm poziția la prima literă
        this.counter = 0;     // Resetăm și cronometrul de viteză
    }

    // În DialogueUI
    public void skipTypewriter() {
        charIndex = currentDialogue.length();
    }

    public boolean isWriting() {
        return charIndex < currentDialogue.length();
    }
}