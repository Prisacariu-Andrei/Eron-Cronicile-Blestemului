package Observer;

import Main.GamePanel;
import java.awt.*;

public class PauseUI {
    GamePanel gp;
    public int commandNum = 0;
    private final String[] options = {"CONTINUĂ", "SALVEAZĂ JOCUL", "OPȚIUNI", "MENIU PRINCIPAL"};

    public PauseUI(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2) {
        // 1. Fundal semi-transparent peste tot ecranul
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // 2. Titlu PAUZĂ
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 60F));
        String title = "PAUZĂ";
        int x = getXforCenteredText(title, g2);
        int y = gp.tileSize * 3;

        // Umbra titlului
        g2.setColor(Color.BLACK);
        g2.drawString(title, x + 3, y + 3);
        // Titlul principal
        g2.setColor(Color.WHITE);
        g2.drawString(title, x, y);

        // 3. Opțiuni Meniu
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28F));

        for (int i = 0; i < options.length; i++) {
            String text = options[i];
            int optionY = gp.tileSize * 5 + (i * 60);
            int optionX = getXforCenteredText(text, g2);

            if (i == commandNum) {
                // Efectul vizual "misto" de selecție
                String selectedText = "> " + text + " <";
                int selectedX = getXforCenteredText(selectedText, g2);
                int textWidth = (int) g2.getFontMetrics().getStringBounds(selectedText, g2).getWidth();

                // Fundalul auriu strălucitor pentru opțiunea selectată
                g2.setColor(new Color(255, 215, 0, 60));
                g2.fillRoundRect(selectedX - 20, optionY - 35, textWidth + 40, 50, 25, 25);

                g2.setColor(Color.YELLOW);
                g2.drawString(selectedText, selectedX, optionY);
            } else {
                g2.setColor(Color.WHITE);
                g2.drawString(text, optionX, optionY);
            }
        }
    }

    private int getXforCenteredText(String text, Graphics2D g2) {
        int length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        return gp.screenWidth / 2 - length / 2;
    }
}