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

    private static final String[][] CREDITS = {
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
                {"ERON: CRONICILE BLESTEMULUI", "Mulțumim că ai jucat!"}
    };

    public void resetPosition() {
        this.textY = gp.screenHeight; // Pornește de sub ecran
        this.finished = false;
    }

    /** Înălțimea (în pixeli) a întregii liste de credite, ca derularea să se oprească la final în loc să reînceapă. */
    private static final int ROW_HEIGHT = 90;
    private boolean finished = false;

    public void update() {
        int endY = gp.screenHeight / 2 - (CREDITS.length * ROW_HEIGHT) - 40; // ultima secțiune ajunge aproape de mijloc
        if (textY > endY) textY--;       // Mișcă textul în sus cu 1 pixel pe cadru
        else finished = true;            // la final rămâne pe ecran până apeși ESC/ENTER
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


        int currentY = textY + 80;
        for (String[] section : CREDITS) {
            g2.setColor(Color.CYAN);
            g2.drawString(section[0], getXforCenteredText(section[0], g2), currentY);
            currentY += 30;
            g2.setColor(Color.WHITE);
            String second = section.length > 1 ? section[1] : "";
            g2.drawString(second, getXforCenteredText(second, g2), currentY);
            currentY += ROW_HEIGHT - 30; // Spațiu între secțiuni
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