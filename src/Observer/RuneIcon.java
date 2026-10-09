package Observer;

import Entity.Items.Rune;

import java.awt.*;
import java.awt.geom.Path2D;

/** Iconița unei runei, desenată în cod (piatră hexagonală cu un simbol): nu are nevoie de fișiere imagine. */
public final class RuneIcon {
    private RuneIcon() {}

    public static void draw(Graphics2D g2, Rune r, int x, int y, int size, boolean glow) {
        Color c = new Color(r.color);
        Color dark = c.darker().darker();
        float cx = x + size / 2f, cy = y + size / 2f, rad = size * 0.46f;

        if (glow) {
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
            g2.fillOval(Math.round(cx - rad * 1.25f), Math.round(cy - rad * 1.25f), Math.round(rad * 2.5f), Math.round(rad * 2.5f));
        }
        Path2D hex = new Path2D.Float();
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 3 * i - Math.PI / 2;
            float px = cx + (float) Math.cos(a) * rad, py = cy + (float) Math.sin(a) * rad;
            if (i == 0) hex.moveTo(px, py); else hex.lineTo(px, py);
        }
        hex.closePath();
        g2.setPaint(new GradientPaint(cx, cy - rad, dark.brighter(), cx, cy + rad, dark.darker()));
        g2.fill(hex);
        g2.setColor(c);
        g2.setStroke(new BasicStroke(Math.max(2f, size / 22f)));
        g2.draw(hex);

        // simbolul
        float s = size * 0.24f;
        g2.setColor(new Color(Math.min(255, c.getRed() + 60), Math.min(255, c.getGreen() + 60), Math.min(255, c.getBlue() + 60)));
        Path2D p = new Path2D.Float();
        switch (r.glyph) {
            case "shield" -> {
                p.moveTo(cx - s, cy - s); p.lineTo(cx + s, cy - s); p.lineTo(cx + s, cy + s * 0.2f);
                p.quadTo(cx + s * 0.8f, cy + s, cx, cy + s * 1.3f); p.quadTo(cx - s * 0.8f, cy + s, cx - s, cy + s * 0.2f); p.closePath();
            }
            case "heart" -> {
                p.moveTo(cx, cy + s * 1.1f);
                p.curveTo(cx - s * 2f, cy - s * 0.2f, cx - s * 1.1f, cy - s * 1.5f, cx, cy - s * 0.5f);
                p.curveTo(cx + s * 1.1f, cy - s * 1.5f, cx + s * 2f, cy - s * 0.2f, cx, cy + s * 1.1f);
            }
            case "drop" -> {
                p.moveTo(cx, cy - s * 1.3f);
                p.curveTo(cx + s * 0.4f, cy - s * 0.4f, cx + s * 1.2f, cy, cx + s * 0.9f, cy + s * 0.7f);
                p.curveTo(cx + s * 0.5f, cy + s * 1.3f, cx - s * 0.5f, cy + s * 1.3f, cx - s * 0.9f, cy + s * 0.7f);
                p.curveTo(cx - s * 1.2f, cy, cx - s * 0.4f, cy - s * 0.4f, cx, cy - s * 1.3f);
            }
            case "fang" -> {
                p.moveTo(cx - s, cy - s * 1.2f); p.lineTo(cx + s, cy - s * 1.2f); p.lineTo(cx + s * 0.3f, cy + s * 1.3f);
                p.lineTo(cx - s * 0.2f, cy + s * 1.3f); p.closePath();
            }
            default -> { // flame
                p.moveTo(cx, cy - s * 1.5f);
                p.curveTo(cx + s * 0.3f, cy - s * 0.6f, cx + s * 1.2f, cy - s * 0.2f, cx + s * 0.9f, cy + s * 0.7f);
                p.curveTo(cx + s * 0.6f, cy + s * 1.3f, cx - s * 0.6f, cy + s * 1.3f, cx - s * 0.9f, cy + s * 0.7f);
                p.curveTo(cx - s * 1.1f, cy, cx - s * 0.3f, cy - s * 0.5f, cx, cy - s * 1.5f);
            }
        }
        g2.fill(p);
        g2.setStroke(new BasicStroke(1));
    }

    public static Color rarityColor(String rarity) {
        return switch (rarity) {
            case "Epică" -> new Color(200, 120, 255);
            case "Rară" -> new Color(90, 180, 255);
            default -> new Color(200, 200, 210);
        };
    }
}
