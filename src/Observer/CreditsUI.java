package Observer;

import Main.GamePanel;

import java.awt.*;

public class CreditsUI {
    GamePanel gp;
    private int textY; // Folosit pentru animația de scroll în sus

    public CreditsUI(GamePanel gp) {
        this.gp = gp;
        resetPosition();
    }

    public void resetPosition() {
        this.textY = 600; // Pornește de sub ecran (ajustează în funcție de gp.screenHeight)
    }

    public void update() {
        textY--; // Mișcă textul în sus cu 1 pixel pe cadru
        if (textY < -400) { // Dacă a trecut tot textul, resetează sau oprește-l
            textY = 600;
        }
    }

    public void draw(Graphics2D g2) {
        // Fundal Negru pentru Credits
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28F));
        g2.setColor(Color.YELLOW);

        String title = "FELICITĂRI! MALAKOR A FOST ÎNVINS!";
        int x = getXforCenteredText(title, g2);
        g2.drawString(title, x, textY);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 20F));
        g2.setColor(Color.WHITE);

        // Lista de credite
        String[][] credits = {
                {"POVESTE & DESIGN", "Prisacariu Andrei"},
                {"PROGRAMARE", "Mihalache Costin-Daniel & Prisacariu Andrei"},
                {"GRAFICĂ & ASSETS", "Prisacariu Andrei"},
                {"MULȚUMIRI SPECIALE", "Tuturor jucătorilor!"},
                {"ARCHITECTURA", "Mihalache Costin-Daniel"},
                {"DOCUMENTATIA", "Mihalache Costin-Daniel"},
                {"DATABASE MANAGE & SAVES", "Prisacariu Andrei"},
                {"USER INTERFACE (UI) & UX", "Mihalache Costin-Daniel & Prisacariu Andrei"},
                {"LEVEL DESIGN / MAP CREATOR", "Prisacariu Andrei"},
                {"CHARACTER DESIGN & ANIMATION", "Mihalache Costin-Daniel"},
                {"PRODUS ÎN", "Java JDK & IntelliJ IDEA"},
                {"UNELTE DE DESIGN", "GIMP"},
                {"BAZĂ DE DATE", "SQLite JDBC"},
                {"MULȚUMIRI SPECIALE", "Profesorilor coordonatori & Tuturor jucătorilor!"},
                {"",""},
                {"",""},
                {"",""},
                {"",""},
                {"",""},
                {"ERON:CRONICILE BLESTEMULUI"}
        };

        int currentY = textY + 80;
        for (String[] section : credits) {
            g2.setColor(Color.CYAN);
            g2.drawString(section[0], getXforCenteredText(section[0], g2), currentY);
            currentY += 30;
            g2.setColor(Color.WHITE);
            g2.drawString(section[1], getXforCenteredText(section[1], g2), currentY);
            currentY += 60; // Spațiu între secțiuni
        }

        // Indicație de ieșire la baza ecranului
        g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 14F));
        g2.setColor(Color.GRAY);
        g2.drawString("Apasă ESC sau ENTER pentru a reveni în meniu", 20, gp.screenHeight - 30);
    }

    private int getXforCenteredText(String text, Graphics2D g2) {
        int length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        return gp.screenWidth / 2 - length / 2;
    }
}