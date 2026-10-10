package GameState;

import Main.GamePanel;
import Main.ZoneInfo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

/**
 * Ecranul de încărcare: imaginea zonei (res/Loading, definită în zones.json), numele ei și o bară de încărcare.
 * Două moduri: tranziție între zone (harta se încarcă în enter(), în spatele fade-ului) și "interludiu" înaintea unei scene
 * (prolog, epilog), după care pornește starea dată. Dacă imaginea lipsește, desenează o carte cu o fotografie făcută din dalele hărții.
 */
public class ZoneTransitionState implements GameState {
    private final GamePanel gp;
    private final String mapPath, zone;
    private final int col, row;
    private final int duration = ZoneInfo.loadingTicks();
    private int ticks = 0;

    // interludiu (prolog/epilog)
    private final String imagePath, title, subtitle;
    private final Supplier<GameState> next;

    private BufferedImage preview, art;

    /** Tranziție între zone. */
    public ZoneTransitionState(GamePanel gp, String mapPath, String zone, int col, int row) {
        this.gp = gp;
        this.mapPath = mapPath;
        this.zone = zone;
        this.col = col;
        this.row = row;
        this.imagePath = null;
        this.title = ZoneInfo.title(zone);
        this.subtitle = ZoneInfo.subtitle(zone);
        this.next = null;
    }

    /** Interludiu: imagine + titlu, apoi starea dată (ex. prologul). key = "prologue" sau "epilogue" din zones.json. */
    public ZoneTransitionState(GamePanel gp, String specialKey, Supplier<GameState> next) {
        this.gp = gp;
        var sp = ZoneInfo.special(specialKey);
        this.mapPath = null;
        this.zone = null;
        this.col = 0;
        this.row = 0;
        this.imagePath = Main.Json.str(sp.get("image"), null);
        this.title = Main.Json.str(sp.get("title"), "");
        this.subtitle = Main.Json.str(sp.get("subtitle"), "");
        this.next = next;
    }

    @Override
    public void enter() {
        ticks = 0;
        if (mapPath != null) gp.eHandler.applyMap(mapPath, zone, col, row);   // încarcă harta, inamicii, NPC-urile și mută jucătorul
        art = imagePath != null ? ZoneInfo.image(imagePath, gp.screenWidth, gp.screenHeight) : ZoneInfo.loadingImage(zone, gp.screenWidth, gp.screenHeight);
        if (art == null && mapPath != null) preview = makePreview();
    }

    @Override
    public void update() {
        ticks++;
        if (ticks >= duration) gp.changeState(next != null ? next.get() : new PlayState(gp));
    }

    /** O fotografie a locului unde ajungi: 16x9 dale în jurul punctului de apariție (folosită doar dacă lipsește imaginea). */
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
        g.dispose();
        return img;
    }

    @Override
    public void draw(Graphics2D g2) {
        int W = gp.screenWidth, H = gp.screenHeight;
        if (art != null) {
            g2.drawImage(art, 0, 0, W, H, null);
            g2.setPaint(new GradientPaint(0, H * 0.55f, new Color(0, 0, 0, 0), 0, H, new Color(0, 0, 0, 215)));
            g2.fillRect(0, (int) (H * 0.55f), W, H - (int) (H * 0.55f));
        } else {
            g2.setPaint(new GradientPaint(0, 0, new Color(10, 10, 20), 0, H, new Color(26, 20, 40)));
            g2.fillRect(0, 0, W, H);
            if (preview != null) {
                int iw = (int) (W * 0.42), ih = iw * 9 / 16, ix = W / 2 - iw / 2, iy = (int) (H * 0.12);
                g2.drawImage(preview, ix, iy, iw, ih, null);
                g2.setColor(new Color(212, 175, 55));
                g2.setStroke(new BasicStroke(4));
                g2.drawRect(ix, iy, iw, ih);
            }
        }

        int base = (int) (H * 0.70);
        if (art == null) {   // imaginile au titlul scris în ele; fără imagine îl scriem noi
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 46F));
            g2.setColor(new Color(0, 0, 0, 170));
            g2.drawString(title, W / 2 - g2.getFontMetrics().stringWidth(title) / 2 + 3, base + 3);
            g2.setColor(new Color(244, 226, 160));
            g2.drawString(title, W / 2 - g2.getFontMetrics().stringWidth(title) / 2, base);
            g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 24F));
            g2.setColor(new Color(215, 215, 235));
            g2.drawString(subtitle, W / 2 - g2.getFontMetrics().stringWidth(subtitle) / 2, base + 38);
        }

        int bw = (int) (W * 0.30), bh = 10, bx = W / 2 - bw / 2, by = art != null ? (int) (H * 0.905) : base + 70;
        float p = Math.min(1f, ticks / (float) duration);
        g2.setColor(new Color(40, 38, 60, 200));
        g2.fillRoundRect(bx, by, bw, bh, 10, 10);
        g2.setColor(new Color(212, 175, 55));
        g2.fillRoundRect(bx, by, (int) (bw * p), bh, 10, 10);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18F));
        g2.setColor(new Color(190, 190, 210));
        String loading = "Se încarcă" + ".".repeat(1 + (ticks / 15) % 3);
        g2.drawString(loading, W / 2 - g2.getFontMetrics().stringWidth(loading) / 2, by + 34);
    }

    @Override public void exit() {}
}
