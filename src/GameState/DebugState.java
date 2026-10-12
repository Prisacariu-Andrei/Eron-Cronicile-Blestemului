package GameState;

import Main.DebugMode;
import Main.GamePanel;

import java.awt.*;
import java.awt.event.KeyEvent;

/** Panoul de admin (vezi Main.DebugMode), deschis cu F9. W/S alege rândul, ENTER îl execută, A/D schimbă zona, ESC sau F9 închide. */
public class DebugState implements GameState {
    private final GamePanel gp;
    private int selected = 0;
    private int zone = 0;

    private static final String[] ROWS = {
            "Sari la zona", "Deblochează toate zonele", "Sari peste TOATE misiunile", "Sari peste următoarea misiune",
            "Noclip (treci prin orice)", "Fără lupte (inamicii nu te opresc)", "Nemurire", "Runa Adminului (+1000 daune)",
            "+1000 monede", "Nivel maxim", "Viață plină"};

    public DebugState(GamePanel gp) {
        this.gp = gp;
        for (int i = 0; i < DebugMode.ZONES.length; i++) if (DebugMode.ZONES[i].zone().equals(gp.tileM.currentZone)) zone = i;
    }

    public void key(int code) {
        if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_F9) { close(); return; }
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) selected = Math.floorMod(selected - 1, ROWS.length);
        else if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) selected = Math.floorMod(selected + 1, ROWS.length);
        else if (selected == 0 && (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT)) zone = Math.floorMod(zone - 1, DebugMode.ZONES.length);
        else if (selected == 0 && (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT)) zone = Math.floorMod(zone + 1, DebugMode.ZONES.length);
        else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_7 && selected == 0) { zone = code - KeyEvent.VK_1; run(); }
        else if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) run();
    }

    private void run() {
        switch (selected) {
            case 0 -> DebugMode.skipTo(gp, zone);                    // te mută și închide panoul
            case 1 -> DebugMode.unlockAllZones(gp);
            case 2 -> DebugMode.skipAllQuests(gp);
            case 3 -> DebugMode.skipNextQuest(gp);
            case 4 -> DebugMode.noclip = !DebugMode.noclip;
            case 5 -> DebugMode.noFights = !DebugMode.noFights;
            case 6 -> DebugMode.god = !DebugMode.god;
            case 7 -> DebugMode.toggleAdminRune(gp);
            case 8 -> DebugMode.addCoins(gp, 1000);
            case 9 -> DebugMode.maxLevel(gp);
            case 10 -> DebugMode.heal(gp);
        }
    }

    public void close() { gp.changeState(new PlayState(gp)); }

    private String value(int i) {
        return switch (i) {
            case 0 -> "◄ " + DebugMode.ZONES[zone].label() + " ►";
            case 4 -> DebugMode.noclip ? "PORNIT" : "oprit";
            case 5 -> DebugMode.noFights ? "PORNIT" : "oprit";
            case 6 -> DebugMode.god ? "PORNIT" : "oprit";
            case 7 -> DebugMode.hasAdminRune(gp) ? "PORNIT" : "oprit";
            default -> "";
        };
    }

    @Override public void enter() {}
    @Override public void update() { gp.gameTimer.update(); }

    @Override
    public void draw(Graphics2D g2) {
        gp.tileM.draw(g2);
        gp.player.draw(g2);
        gp.ui.draw(g2);

        int W = gp.screenWidth, H = gp.screenHeight;
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, W, H);
        int pw = 760, ph = 130 + ROWS.length * 46, px = W / 2 - pw / 2, py = H / 2 - ph / 2;
        g2.setColor(new Color(24, 22, 36));
        g2.fillRoundRect(px, py, pw, ph, 24, 24);
        g2.setColor(new Color(255, 120, 90));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(px, py, pw, ph, 24, 24);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 30F));
        g2.drawString("PANOU ADMIN", px + 32, py + 50);
        for (int i = 0; i < ROWS.length; i++) {
            int y = py + 96 + i * 46;
            if (i == selected) {
                g2.setColor(new Color(70, 60, 100));
                g2.fillRoundRect(px + 20, y - 28, pw - 40, 40, 14, 14);
            }
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 21F));
            g2.setColor(i == selected ? Color.YELLOW : Color.WHITE);
            g2.drawString(ROWS[i], px + 40, y);
            String v = value(i);
            if (!v.isEmpty()) {
                boolean on = v.equals("PORNIT");
                g2.setColor(on ? new Color(110, 230, 130) : new Color(170, 170, 190));
                g2.drawString(v, px + pw - 40 - g2.getFontMetrics().stringWidth(v), y);
            }
        }
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16F));
        g2.setColor(new Color(150, 150, 170));
        g2.drawString("[W/S] rând   [ENTER] execută   [A/D] zona   [ESC / F9] închide", px + 32, py + ph - 20);
    }

    @Override public void exit() {}
}
