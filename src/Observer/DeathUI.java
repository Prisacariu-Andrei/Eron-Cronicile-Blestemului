package Observer;

import Main.GamePanel;
import java.awt.*;

public class DeathUI {
    GamePanel gp;
    public int commandNum = 0; // 0: Main Menu, 1: Quit

    public DeathUI(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2) {
        // Fundal negru cu opacitate (overlay)
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 60F));
        String text = "AI PIERDUT";

        // Umbră text
        g2.setColor(Color.BLACK);
        g2.drawString(text, getXforCenteredText(text, g2) + 3, gp.tileSize * 4 + 3);
        // Text principal (Roșu aprins)
        g2.setColor(new Color(200, 0, 0));
        g2.drawString(text, getXforCenteredText(text, g2), gp.tileSize * 4);

        // Opțiuni
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28F));

        // MAIN MENU
        text = "MAIN MENU";
        int x = getXforCenteredText(text, g2);
        int y = gp.tileSize * 7;
        g2.setColor(Color.WHITE);
        if (commandNum == 0) {
            g2.setColor(Color.YELLOW);
            g2.drawString(">", x - 25, y);
        }
        g2.drawString(text, x, y);

        // QUIT
        text = "QUIT";
        x = getXforCenteredText(text, g2);
        y += gp.tileSize;
        g2.setColor(Color.WHITE);
        if (commandNum == 1) {
            g2.setColor(Color.YELLOW);
            g2.drawString(">", x - 25, y);
        }
        g2.drawString(text, x, y);
    }

    private int getXforCenteredText(String text, Graphics2D g2) {
        int length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        return gp.screenWidth / 2 - length / 2;
    }
}