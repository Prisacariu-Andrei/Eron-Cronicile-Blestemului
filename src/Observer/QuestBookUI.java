package Observer;

import Main.GamePanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import ProgressManagement.QuestManager;

public class QuestBookUI {
    GamePanel gp;
    public boolean isOpen = false;
    private int selectedTab = 0; // 0 = Principale, 1 = Secundare

    public QuestBookUI(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2) {
        // 1. Fundalul ferestrei jurnalului
        int x = gp.tileSize * 2;
        int y = gp.tileSize;
        int width = gp.screenWidth - (gp.tileSize * 4);
        int height = gp.screenHeight - (gp.tileSize * 2);

        Color bgColor = new Color(30, 30, 45, 230);
        g2.setColor(bgColor);
        g2.fillRoundRect(x, y, width, height, 30, 30);

        Color borderColor = new Color(212, 175, 55); // Auriu
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(4));
        g2.drawRoundRect(x + 5, y + 5, width - 10, height - 10, 20, 20);

        // 2. Antet / Titlu
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28F));
        g2.setColor(Color.WHITE);
        String title = (selectedTab == 0) ? "Jurnal - Misiuni Principale" : "Jurnal - Misiuni Secundare";
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, x + (width / 2) - (titleWidth / 2), y + 45);

        // 3. Filtrare misiuni - MODIFICATĂ ACUM ★★★
        List<QuestManager.Quest> allQuests = (selectedTab == 0) ? gp.questManager.mainQuests : gp.questManager.sideQuests;
        List<QuestManager.Quest> visibleQuests = new ArrayList<>();

        for (QuestManager.Quest q : allQuests) {
            // Condiția salvatoare: Dacă quest-ul e activ (received) SAU dacă a fost deja terminat (completed)
            if (q.isReceived || q.isCompleted) {
                visibleQuests.add(q);
            }
        }

        int startY = y + 90;
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18F));

        // Dacă nu avem nicio misiune activă sau istoric, afișăm un mesaj ajutător
        if (visibleQuests.isEmpty()) {
            g2.setColor(Color.LIGHT_GRAY);
            String emptyMsg = "Nu există misiuni active sau încheiate în acest moment.";
            int emptyWidth = g2.getFontMetrics().stringWidth(emptyMsg);
            g2.drawString(emptyMsg, x + (width / 2) - (emptyWidth / 2), startY + 40);
        } else {
            // Desenăm lista de misiuni active și completate
            for (int i = 0; i < Math.min(visibleQuests.size(), 11); i++) {
                QuestManager.Quest q = visibleQuests.get(i);

                if (q.isCompleted) {
                    g2.setColor(new Color(100, 255, 100)); // Verde dacă e completat
                    g2.drawString("✔ ", x + 30, startY);

                    g2.setColor(Color.GRAY); // Opțional: Textul devine gri discret pentru cele completate
                    g2.drawString(q.title + " [COMPLETATĂ]", x + 70, startY);
                } else {
                    g2.setColor(new Color(255, 200, 100)); // Galben dacă e activă
                    g2.drawString("➔ ", x + 30, startY);

                    g2.setColor(Color.WHITE);
                    g2.drawString(q.title + " [" + q.currentAmount + "/" + q.requiredAmount + "]", x + 70, startY);
                }

                // Descrierea quest-ului
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14F));
                if (q.isCompleted) {
                    g2.setColor(new Color(110, 130, 110)); // Verde închis mat pentru textul descrierii completate
                } else {
                    g2.setColor(new Color(180, 180, 180)); // Gri normal pentru cea activă
                }
                g2.drawString(q.description, x + 70, startY + 18);

                startY += 45;
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18F)); // Reset font pentru următorul element
            }
        }

        // 4. Instrucțiuni de navigare în meniu
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14F));
        g2.setColor(Color.YELLOW);
        g2.drawString("Apăsați [Q] pentru a schimba tab-ul / [J] pentru a închide", x + 40, y + height - 30);
    }

    public void switchTab() {
        selectedTab = (selectedTab == 0) ? 1 : 0;
    }
}