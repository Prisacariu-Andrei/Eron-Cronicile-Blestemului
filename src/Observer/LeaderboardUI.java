package Observer;

import Main.GamePanel;
import java.awt.*;

public class LeaderboardUI {
    private final GamePanel gp;

    public LeaderboardUI(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2) {
        // 1. Fundal întunecat de atmosferă
        if (gp.ui.menuBackground != null) {
            g2.drawImage(gp.ui.menuBackground, -80, 0, gp.screenWidth+80, gp.screenHeight, null);
            // Opțional: adăugăm o mască neagră semi-transparentă (30%) ca să aibă textul contrast bun
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        } else {
            // Fallback în caz că imaginea nu s-a putut încărca, să nu rămână ecranul gol
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }

        // 2. Margine decorativă dublă (Stil RPG medieval)
        g2.setColor(new Color(153, 0, 255)); // Culoare aurie
        g2.setStroke(new BasicStroke(4));
        g2.drawRoundRect(50, 50, gp.screenWidth - 100, gp.screenHeight - 100, 30, 30);
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(58, 58, gp.screenWidth - 116, gp.screenHeight - 116, 25, 25);

        // 3. Titlu principal
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 48F));
        String title = "PANTEONUL EROILOR (TOP TIMPI)";
        int titleX = gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(title) / 2;
        g2.drawString(title, titleX, 130);

        // Linie de separare sub titlu
        g2.fillRect(titleX - 50, 150, g2.getFontMetrics().stringWidth(title) + 100, 3);

        // 4. Preluarea datelor din baza de date
        Database.SaveLoadManager sm = new Database.SaveLoadManager();
        String[] topScores = sm.getTopScores();

        // 5. Desenarea tabelului cu scoruri
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 28F));
        int startY = (int)(gp.screenHeight*0.45);
        int spacing = (int)(gp.screenHeight*0.05);

        boolean hasScores = false;

        if (topScores != null) {
            for (int i = 0; i < 5; i++) {
                if (topScores[i] != null && !topScores[i].trim().isEmpty()) {
                    hasScores = true;

                    // Scoatem numerotarea duplicată din string deoarece sm.getTopScores() o include deja! ★★★
                    String entryText = topScores[i];
                    gp.ui.getXforCenteredText(entryText);
                    if (i == 0) {
                        g2.setColor(new Color(153, 0, 255)); // ROYAL!!!
                        g2.drawString("👑 " + entryText, gp.ui.getXforCenteredText("👑 " + entryText) , startY );
                    } else if (i == 1) {
                        g2.setColor(new Color(192, 192, 192)); // Argint pentru Locul 2
                        g2.drawString("🥈 " + entryText, gp.ui.getXforCenteredText("🥈 " + entryText), startY + (i * spacing));
                    } else if (i == 2) {
                        g2.setColor(new Color(205, 127, 50)); // Bronz pentru Locul 3
                        g2.drawString("🥉 " + entryText, gp.ui.getXforCenteredText("🥉 " + entryText), startY + (i * spacing));
                    } else {
                        g2.setColor(Color.GRAY);
                        g2.drawString("  " + entryText, gp.ui.getXforCenteredText("  " + entryText), startY + (i * spacing));
                    }
                }
            }
        }

        if (!hasScores) {
            g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 24F));
            g2.setColor(Color.LIGHT_GRAY);
            String noScores = "Niciun erou nu a învins jocul încă. Fii tu primul!";
            g2.drawString(noScores, gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(noScores) / 2, gp.screenHeight / 2);
        }

        // 6. Subtext de ieșire
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18F));
        g2.setColor(Color.WHITE);
        String backText = "[ESC] Înapoi la Meniul Principal";
        int backX = gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(backText) / 2;
        g2.drawString(backText, backX, gp.screenHeight - 90);
    }

    /**
     * Trimite noul scor direct în baza de date folosind logica nativă din SaveManager. ★★★
     */
    public void checkAndSaveNewScore(long finalTimeSeconds, String playerName) {
        Database.SaveLoadManager sm = new Database.SaveLoadManager();

        // Înregistrăm scorul brut (baza de date va face automat sortarea și limitarea la citire)
        sm.recordScore(playerName, (int) finalTimeSeconds);
    }

}