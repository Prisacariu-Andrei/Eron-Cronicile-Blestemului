package Observer;

import Main.GamePanel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class FogOfWar {

    private static final int EDGE_ALPHA = 245; // Întunericul maxim, în afara cercului de lumină

    private final GamePanel gp;
    private final int circleSize; // Diametrul cercului de lumină

    // Discul de lumină se desenează o singură dată per culoare și apoi se reutilizează la fiecare cadru
    private final Map<Integer, BufferedImage> discs = new HashMap<>();

    public FogOfWar(GamePanel gp, int circleSize) {
        this.gp = gp;
        this.circleSize = circleSize;
    }

    /**
     * Ceața = o culoare aproape opacă peste tot ecranul, cu un disc de lumină (gradient) în jurul lui Eron.
     * Înainte, la fiecare cadru se crea o imagine de 1600x900 și se umplea cu gradient (~6 MB gunoi pe cadru).
     * Acum se desenează discul pre-calculat și patru dreptunghiuri simple în jurul lui.
     */
    public void draw(Graphics2D g2, int colour) {
        // Centrul luminii = unde se desenează Eron pe ecran (nu mereu centrul ecranului: lângă marginile hărții se mută)
        int centerX = gp.camera.screenX(gp.player.worldX) + gp.tileSize / 2;
        int centerY = gp.camera.screenY(gp.player.worldY) + gp.tileSize / 2;

        BufferedImage disc = discs.computeIfAbsent(colour, this::buildDisc);
        int r = disc.getWidth() / 2;
        int left = centerX - r, top = centerY - r, size = disc.getWidth();
        int right = left + size, bottom = top + size;
        int w = gp.screenWidth, h = gp.screenHeight;

        g2.setColor(fogColor(colour, EDGE_ALPHA));
        if (top > 0) g2.fillRect(0, 0, w, Math.min(top, h));                              // deasupra
        if (bottom < h) g2.fillRect(0, Math.max(bottom, 0), w, h - Math.max(bottom, 0));  // dedesubt
        int bandTop = Math.max(top, 0), bandBottom = Math.min(bottom, h);
        if (bandBottom > bandTop) {
            if (left > 0) g2.fillRect(0, bandTop, Math.min(left, w), bandBottom - bandTop);                          // stânga
            if (right < w) g2.fillRect(Math.max(right, 0), bandTop, w - Math.max(right, 0), bandBottom - bandTop);   // dreapta
        }
        g2.drawImage(disc, left, top, null);
    }

    private static Color fogColor(int colour, int alpha) {
        return new Color(5 + colour, 5 + colour, 15 + colour, alpha);
    }

    private BufferedImage buildDisc(int colour) {
        int radius = (int) Math.ceil(circleSize / 2f);
        int size = radius * 2;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // GRADIENT INVERS (Lumină -> Întuneric)
        float[] fractions = {0.0f, 0.2f, 0.6f, 1.0f};
        Color[] colors = {
                fogColor(colour, 0),     // Centru: complet transparent (se vede Eron perfect)
                fogColor(colour, 30),    // Început de penumbră
                fogColor(colour, 150),   // Ceață deasă
                fogColor(colour, EDGE_ALPHA) // Margine: întunericul maxim
        };

        // NO_CYCLE: tot ce depășește raza se umple cu ultima culoare, deci colțurile pătratului se potrivesc cu restul ceții
        g.setPaint(new RadialGradientPaint(new Point(radius, radius), circleSize / 2f, fractions, colors,
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.setComposite(AlphaComposite.Src);
        g.fillRect(0, 0, size, size);
        g.dispose();
        return img;
    }
}
