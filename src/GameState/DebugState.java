package GameState;

import Main.DebugMode;
import Main.GamePanel;

import java.awt.*;

/** Meniul "Sari la zonă" al modului de test (vezi Main.DebugMode). Deschis cu F9 doar când modul e pornit. */
public class DebugState implements GameState {
    private final GamePanel gp;
    private int selected = 0;

    public DebugState(GamePanel gp) {
        this.gp = gp;
    }

    public void move(int dir) {
        selected = Math.floorMod(selected + dir, DebugMode.ZONES.length);
    }

    /** Alege zona (index 0-6): setează progresul și te teleportează. */
    public void choose(int index) {
        if (index < 0 || index >= DebugMode.ZONES.length) return;
        DebugMode.skipTo(gp, index);
    }

    public void chooseSelected() { choose(selected); }

    public void close() { gp.changeState(new PlayState(gp)); }

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
        int pw = 640, ph = 120 + DebugMode.ZONES.length * 52, px = W / 2 - pw / 2, py = H / 2 - ph / 2;
        g2.setColor(new Color(24, 22, 36));
        g2.fillRoundRect(px, py, pw, ph, 24, 24);
        g2.setColor(new Color(255, 120, 90));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(px, py, pw, ph, 24, 24);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 30F));
        g2.drawString("DEBUG: SARI LA ZONĂ", px + 32, py + 50);
        for (int i = 0; i < DebugMode.ZONES.length; i++) {
            var z = DebugMode.ZONES[i];
            int y = py + 90 + i * 52;
            if (i == selected) {
                g2.setColor(new Color(70, 60, 100));
                g2.fillRoundRect(px + 20, y - 30, pw - 40, 44, 14, 14);
            }
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22F));
            g2.setColor(i == selected ? Color.YELLOW : Color.WHITE);
            g2.drawString((i + 1) + "   " + z.label(), px + 40, y);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16F));
            g2.setColor(new Color(170, 170, 190));
            g2.drawString("nivel " + z.level() + (z.flags().length > 0 ? "  |  " + z.flags()[z.flags().length - 1] : ""), px + pw - 220, y);
        }
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16F));
        g2.setColor(new Color(150, 150, 170));
        g2.drawString("[1-7] sau [W/S + ENTER] alege   [ESC / F9] închide   (adaugă progres, nu scoate nimic)", px + 32, py + ph - 20);
    }

    @Override public void exit() {}
}
