package GameState;

import Main.GamePanel;
import Main.Json;
import Main.ZoneInfo;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Harta ținutului (tasta M): imaginea din res/Loading/10_Harta_Aethelgard.png, cu zona curentă marcată (roșu, pulsează)
 * și zonele deschise încercuite cu auriu; zonele încă închise sunt întunecate cu un lacăt. Pozițiile și condițiile sunt în zones.json.
 */
public class MapState implements GameState {
    private final GamePanel gp;
    private BufferedImage art;
    private static final String[] ORDER = {"Eldoria", "Barlogul", "Abisul", "Citadela", "Tundra", "Podul", "Castelul"};

    public MapState(GamePanel gp) {
        this.gp = gp;
    }

    public void close() { gp.changeState(new PlayState(gp)); }

    /** Zona e deschisă dacă flag-ul cerut în zones.json e pus (sau nu cere niciunul). */
    public static boolean unlocked(GamePanel gp, String zone) {
        String need = ZoneInfo.requires(zone);
        return need.isEmpty() || gp.story.has(need);
    }

    @Override
    public void enter() {
        art = ZoneInfo.image(Json.str(ZoneInfo.special("map").get("image"), null), gp.screenWidth, gp.screenHeight);
    }

    @Override public void update() { gp.gameTimer.update(); }

    @Override
    public void draw(Graphics2D g2) {
        int W = gp.screenWidth, H = gp.screenHeight;
        if (art != null) g2.drawImage(art, 0, 0, W, H, null);
        else { g2.setColor(new Color(214, 196, 150)); g2.fillRect(0, 0, W, H); }

        float pulse = 0.5f + 0.5f * (float) Math.sin(gp.fx.ticks * 0.12);
        for (String zone : ORDER) {
            double[] pos = ZoneInfo.mapPos(zone);
            if (pos == null) continue;
            int x = (int) (pos[0] * W), y = (int) (pos[1] * H);
            boolean open = unlocked(gp, zone), here = zone.equals(gp.tileM.currentZone);

            if (!open) {   // zonă încă închisă: întunecată, cu un lacăt
                g2.setColor(new Color(20, 14, 10, 150));
                g2.fillOval(x - 44, y - 44, 88, 88);
                g2.setColor(new Color(235, 225, 200));
                g2.fillRoundRect(x - 11, y - 4, 22, 18, 5, 5);
                g2.setStroke(new BasicStroke(3));
                g2.drawArc(x - 8, y - 17, 16, 18, 0, 180);
            } else {
                g2.setColor(new Color(212, 160, 30, 150 + Math.round(70 * pulse)));
                g2.setStroke(new BasicStroke(5));
                g2.drawOval(x - 44, y - 44, 88, 88);
            }
            if (here) {
                g2.setColor(new Color(220, 40, 40, 190));
                g2.fillOval(x - 14, y - 14, 28, 28);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3));
                g2.drawOval(x - 14, y - 14, 28, 28);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(x - 24 - Math.round(8 * pulse), y - 24 - Math.round(8 * pulse), 48 + Math.round(16 * pulse), 48 + Math.round(16 * pulse));
                String label = "Ești aici";
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22F));
                int tw = g2.getFontMetrics().stringWidth(label);
                g2.setColor(new Color(30, 20, 12, 220));
                g2.fillRoundRect(x - tw / 2 - 10, y - 86, tw + 20, 32, 12, 12);
                g2.setColor(new Color(255, 225, 160));
                g2.drawString(label, x - tw / 2, y - 63);
            }
        }

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18F));
        g2.setColor(new Color(60, 40, 25));
        String legend = "Auriu = zonă deschisă   |   Lacăt = încă închisă   |   [M] sau [ESC] închide";
        g2.drawString(legend, W - 90 - g2.getFontMetrics().stringWidth(legend), H - 24);
    }

    @Override public void exit() {}
}
