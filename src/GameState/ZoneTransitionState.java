package GameState;

import Main.GamePanel;
import Main.ZoneInfo;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Tranziția dintre zone: o carte cu o imagine din locul unde ajungi (făcută din dalele hărții), numele zonei și o scurtă
 * bară de încărcare. Harta se încarcă în enter(), în spatele fade-ului; apoi cartea stă puțin și Eron apare în joc.
 * Durata: res/Data/zones.json (loadingTicks).
 */
public class ZoneTransitionState implements GameState {
    private final GamePanel gp;
    private final String mapPath, zone;
    private final int col, row;
    private final int duration = ZoneInfo.loadingTicks();
    private int ticks = 0;
    private BufferedImage preview;

    public ZoneTransitionState(GamePanel gp, String mapPath, String zone, int col, int row) {
        this.gp = gp;
        this.mapPath = mapPath;
        this.zone = zone;
        this.col = col;
        this.row = row;
    }

    @Override
    public void enter() {
        gp.eHandler.applyMap(mapPath, zone, col, row);   // încarcă harta, inamicii, NPC-urile și mută jucătorul
        preview = makePreview();
        ticks = 0;
    }

    @Override
    public void update() {
        ticks++;
        if (ticks >= duration) gp.changeState(new PlayState(gp));
    }

    /** O fotografie a locului unde ajungi: 16x9 dale în jurul punctului de apariție. */
    private BufferedImage makePreview() {
        int ts = gp.tileSize, cols = 16, rows = 9;
        BufferedImage img = new BufferedImage(cols * ts, rows * ts, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        var tm = gp.tileM;
        int c0 = Math.max(0, Math.min(col - cols / 2, Math.max(0, tm.actualMapCols - cols)));
        int r0 = Math.max(0, Math.min(row - rows / 2, Math.max(0, tm.actualMapRows - rows)));
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                int cc = c0 + c, rr = r0 + r;
                if (cc >= gp.maxWorldCol || rr >= gp.maxWorldRow) continue;
                int id = tm.mapTileNum[cc][rr];
                if (id >= 0 && id < tm.tile.length && tm.tile[id] != null && tm.tile[id].image != null) {
                    g.drawImage(tm.tile[id].image, c * ts, r * ts, ts, ts, null);
                }
            }
        }
        // Eron, în locul unde apare
        int px = (col - c0) * ts, py = (row - r0) * ts;
        if (gp.player.down1 != null) g.drawImage(gp.player.down1, px, py, ts, ts, null);
        g.dispose();
        return img;
    }

    @Override
    public void draw(Graphics2D g2) {
        int W = gp.screenWidth, H = gp.screenHeight;
        g2.setPaint(new GradientPaint(0, 0, new Color(10, 10, 20), 0, H, new Color(26, 20, 40)));
        g2.fillRect(0, 0, W, H);

        // imaginea încadrată
        int iw = (int) (W * 0.42), ih = iw * 9 / 16, ix = W / 2 - iw / 2, iy = (int) (H * 0.17);
        if (preview != null) {
            g2.drawImage(preview, ix, iy, iw, ih, null);
            // vignetă
            g2.setPaint(new RadialGradientPaint(W / 2f, iy + ih / 2f, iw * 0.62f, new float[]{0.55f, 1f}, new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 170)}));
            g2.fillRect(ix, iy, iw, ih);
        }
        g2.setColor(new Color(212, 175, 55));
        g2.setStroke(new BasicStroke(4));
        g2.drawRect(ix, iy, iw, ih);

        // titlu și subtitlu
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 44F));
        String title = ZoneInfo.title(zone);
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(title, W / 2 - g2.getFontMetrics().stringWidth(title) / 2 + 3, iy + ih + 73);
        g2.setColor(new Color(240, 220, 150));
        g2.drawString(title, W / 2 - g2.getFontMetrics().stringWidth(title) / 2, iy + ih + 70);
        g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 24F));
        g2.setColor(new Color(190, 190, 215));
        String sub = ZoneInfo.subtitle(zone);
        g2.drawString(sub, W / 2 - g2.getFontMetrics().stringWidth(sub) / 2, iy + ih + 108);

        // bara de încărcare
        int bw = (int) (W * 0.30), bh = 10, bx = W / 2 - bw / 2, by = iy + ih + 150;
        float p = Math.min(1f, ticks / (float) duration);
        g2.setColor(new Color(50, 48, 70));
        g2.fillRoundRect(bx, by, bw, bh, 10, 10);
        g2.setColor(new Color(212, 175, 55));
        g2.fillRoundRect(bx, by, (int) (bw * p), bh, 10, 10);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18F));
        g2.setColor(new Color(160, 160, 185));
        String loading = "Se încarcă" + ".".repeat(1 + (ticks / 15) % 3);
        g2.drawString(loading, W / 2 - g2.getFontMetrics().stringWidth(loading) / 2, by + 36);
    }

    @Override public void exit() {}
}
