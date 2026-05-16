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
        g2.setColor(new Color(20, 15, 25, 240));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // 2. Margine decorativă dublă (Stil RPG medieval)
        g2.setColor(new Color(255, 215, 0)); // Culoare aurie
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
        Database.SaveManager sm = new Database.SaveManager(gp);
        String[] topScores = sm.getTopScores();

        // 5. Desenarea tabelului cu scoruri
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 28F));
        int startY = 240;
        int spacing = 65;

        boolean hasScores = false;

        if (topScores != null) {
            for (int i = 0; i < topScores.length; i++) {
                if (topScores[i] != null && !topScores[i].trim().isEmpty()) {
                    hasScores = true;

                    // Scoatem numerotarea duplicată din string deoarece sm.getTopScores() o include deja! ★★★
                    String entryText = topScores[i];

                    if (i == 0) {
                        g2.setColor(new Color(255, 223, 0)); // Coroană de aur pentru Locul 1
                        g2.drawString("👑 " + entryText, gp.screenWidth / 2 - 200, startY );
                    } else if (i == 1) {
                        g2.setColor(new Color(192, 192, 192)); // Argint pentru Locul 2
                        g2.drawString("🥈 " + entryText, gp.screenWidth / 2 - 200, startY + (i * spacing));
                    } else if (i == 2) {
                        g2.setColor(new Color(205, 127, 50)); // Bronz pentru Locul 3
                        g2.drawString("🥉 " + entryText, gp.screenWidth / 2 - 200, startY + (i * spacing));
                    } else {
                        g2.setColor(Color.LIGHT_GRAY);
                        g2.drawString("   " + entryText, gp.screenWidth / 2 - 200, startY + (i * spacing));
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
        Database.SaveManager sm = new Database.SaveManager(gp);

        // Înregistrăm scorul brut (baza de date va face automat sortarea și limitarea la citire)
        sm.recordScore(playerName, (int) finalTimeSeconds);
    }
}