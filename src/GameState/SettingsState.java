package GameState;

import Main.GamePanel;
import Main.Settings;
import Main.SoundManager;

import java.awt.*;

/** Setările (tasta T): volum, scuturatul ecranului, viteza textului, ecran complet. W/S aleg rândul, A/D schimbă valoarea, T sau ESC închid și salvează. */
public class SettingsState implements GameState {
    private final GamePanel gp;
    private final GameState back;
    private int selected = 0;
    private static final String[] ROWS = {"Volum", "Scuturat ecran", "Viteză text", "Ecran complet"};

    public SettingsState(GamePanel gp, GameState back) {
        this.gp = gp;
        this.back = back;
    }

    public void move(int dir) { selected = Math.floorMod(selected + dir, ROWS.length); }

    /** dir = -1/+1 (A/D) sau 0 (ENTER: comută/rotește). */
    public void change(int dir) {
        switch (selected) {
            case 0 -> {
                int v = Math.round(Settings.volume * 10) + (dir == 0 ? 1 : dir);
                if (dir == 0 && v > 10) v = 0;
                Settings.volume = Math.max(0, Math.min(10, v)) / 10f;
                SoundManager.getInstance().applyVolume();
                SoundManager.getInstance().playSE("click");
            }
            case 1 -> Settings.shake = !Settings.shake;
            case 2 -> Settings.textSpeed = Math.floorMod(Settings.textSpeed + (dir == 0 ? 1 : dir), 3);
            case 3 -> gp.toggleFullscreen();   // se salvează singur; același lucru fac F11 și Alt+Enter
            default -> { }
        }
    }

    public void close() {
        Settings.save();
        gp.changeState(back instanceof PauseState ? back : new PlayState(gp));
    }

    @Override public void enter() {}
    @Override public void update() { gp.gameTimer.update(); }

    @Override
    public void draw(Graphics2D g2) {
        gp.tileM.draw(g2);
        gp.player.draw(g2);
        gp.ui.draw(g2);
        int W = gp.screenWidth, H = gp.screenHeight;
        g2.setColor(new Color(0, 0, 0, 175));
        g2.fillRect(0, 0, W, H);

        int pw = 640, ph = 400, px = W / 2 - pw / 2, py = H / 2 - ph / 2;
        g2.setColor(new Color(24, 22, 36, 245));
        g2.fillRoundRect(px, py, pw, ph, 26, 26);
        g2.setColor(new Color(212, 175, 55));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(px, py, pw, ph, 26, 26);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 32F));
        g2.drawString("SETĂRI", px + 36, py + 56);

        String[] values = {volumeBar(), Settings.shake ? "Pornit" : "Oprit", Settings.textSpeedLabel(), Settings.fullscreen ? "Pornit" : "Oprit"};
        for (int i = 0; i < ROWS.length; i++) {
            int y = py + 120 + i * 62;
            if (i == selected) {
                g2.setColor(new Color(70, 60, 100));
                g2.fillRoundRect(px + 20, y - 34, pw - 40, 50, 14, 14);
            }
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 24F));
            g2.setColor(i == selected ? Color.YELLOW : Color.WHITE);
            g2.drawString(ROWS[i], px + 40, y);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 24F));
            g2.setColor(new Color(220, 220, 235));
            g2.drawString("<  " + values[i] + "  >", px + pw - 40 - g2.getFontMetrics().stringWidth("<  " + values[i] + "  >"), y);
        }
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16F));
        g2.setColor(new Color(150, 150, 170));
        g2.drawString("[W/S] alege   [A/D] schimbă   [F11] ecran complet   [T] sau [ESC] închide și salvează", px + 36, py + ph - 22);
    }

    private static String volumeBar() {
        int n = Math.round(Settings.volume * 10);
        return "#".repeat(n) + "-".repeat(10 - n) + "  " + (n * 10) + "%";
    }

    @Override public void exit() {}
}
