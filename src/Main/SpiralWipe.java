package Main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Tranziția în spirală dintre hartă și luptă: ecranul se întunecă ca o cochilie de melc (o spirală neagră care se strânge dinspre margine
 * spre centru, ~0,8 s), rămâne negru ~0,2 s, apoi spirala se desface dinspre centru spre margine (~0,8 s) și arată lupta.
 * Se desenează peste orice stare (GamePanel.renderFrame) și nu consumă taste: starea care o pornește hotărăște ce se întâmplă cu input-ul.
 */
public final class SpiralWipe {
    public static final int CLOSE_TICKS = 48, HOLD_TICKS = 12, OPEN_TICKS = 48;   // 60 de ticks = o secundă
    private static final int SAMPLES = 120;                                        // câte puncte are conturul spiralei

    private enum Phase { IDLE, CLOSING, HOLD, OPENING }

    private Phase phase = Phase.IDLE;
    private int ticks = 0;
    private Runnable atBlack;

    /** Pornește închiderea; când ecranul e complet negru (după pauză) se rulează atBlack (ex. începe lupta), apoi spirala se deschide. */
    public void close(Runnable atBlack) {
        this.atBlack = atBlack;
        phase = Phase.CLOSING;
        ticks = 0;
    }

    public boolean active() { return phase != Phase.IDLE; }

    /** 0 = ecranul se vede, 1 = complet negru. */
    public float progress() {
        return switch (phase) {
            case CLOSING -> ease(ticks / (float) CLOSE_TICKS);
            case HOLD -> 1f;
            case OPENING -> 1f - ease(ticks / (float) OPEN_TICKS);
            default -> 0f;
        };
    }

    private static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);   // pornește și se oprește lin
    }

    public void update() {
        if (phase == Phase.IDLE) return;
        ticks++;
        switch (phase) {
            case CLOSING -> { if (ticks >= CLOSE_TICKS) { phase = Phase.HOLD; ticks = 0; } }
            case HOLD -> {
                if (ticks >= HOLD_TICKS) {
                    Runnable r = atBlack;
                    atBlack = null;
                    phase = Phase.OPENING;
                    ticks = 0;
                    if (r != null) r.run();
                }
            }
            case OPENING -> { if (ticks >= OPEN_TICKS) phase = Phase.IDLE; }
            default -> { }
        }
    }

    public void draw(Graphics2D g2, int w, int h) {
        float p = progress();
        if (p <= 0f) return;
        if (p >= 1f) {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
            return;
        }
        double cx = w / 2.0, cy = h / 2.0;
        double rmax = Math.hypot(cx, cy) + 4;
        double pitch = rmax / 2.5;                       // cât se strânge raza la fiecare tură: ~2,5 ture până la centru
        double thr = p * (rmax / pitch + 1);             // "cât din spirală" e acoperit

        // Zona vizibilă: tot ce e în interiorul curbei r(φ) = rmax - pitch * (thr - φ / 2π). Marginea ei e o spirală cu o treaptă la φ = 0 (cochilia).
        Path2D hole = new Path2D.Double();
        for (int i = 0; i <= SAMPLES; i++) {
            double phi = 2 * Math.PI * i / SAMPLES;
            double r = Math.max(0, rmax - pitch * (thr - phi / (2 * Math.PI)));
            double x = cx + r * Math.cos(phi - Math.PI / 2), y = cy + r * Math.sin(phi - Math.PI / 2);
            if (i == 0) hole.moveTo(x, y); else hole.lineTo(x, y);
        }
        hole.closePath();
        Path2D black = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        black.append(new Rectangle2D.Double(-8, -8, w + 16, h + 16), false);
        black.append(hole, false);
        g2.setColor(Color.BLACK);
        g2.fill(black);
    }
}
