package Main;

import java.awt.*;

/**
 * Contoare de performanță afișate peste joc când apeși F3.
 * Update-ul rulează pe thread-ul jocului, desenarea pe thread-ul Swing (EDT), deci metodele sunt synchronized.
 */
public final class DebugStats {
    public static volatile boolean visible = false; // comutat cu F3

    // Valori afișate (recalculate o dată pe secundă)
    public static volatile double fps, ups, updateMs, renderMs;
    // Valori setate direct de cod
    public static volatile int tilesDrawn;

    private static int frames, updates;
    private static long updateNs, renderNs;
    private static long windowStart = System.nanoTime();

    private DebugStats() {}

    public static synchronized void onUpdate(long ns) {
        updates++;
        updateNs += ns;
    }

    public static synchronized void onRender(long ns) {
        frames++;
        renderNs += ns;

        long now = System.nanoTime();
        long elapsed = now - windowStart;
        if (elapsed >= 1_000_000_000L) {
            double sec = elapsed / 1e9;
            fps = frames / sec;
            ups = updates / sec;
            updateMs = updates == 0 ? 0 : updateNs / 1e6 / updates;
            renderMs = frames == 0 ? 0 : renderNs / 1e6 / frames;
            frames = 0;
            updates = 0;
            updateNs = 0;
            renderNs = 0;
            windowStart = now;
        }
    }

    public static void draw(Graphics2D g2, GamePanel gp) {
        int enemies = 0, npcs = 0;
        for (var e : gp.enemy) if (e != null) enemies++;
        for (var n : gp.npc) if (n != null) npcs++;
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);

        String[] lines = {
                String.format("FPS %.0f   UPS %.0f", fps, ups),
                String.format("update %.2f ms   render %.2f ms", updateMs, renderMs),
                "tiles drawn " + tilesDrawn,
                "enemies " + enemies + "   npcs " + npcs + "   popups " + gp.popups.size(),
                "heap " + usedMb + " MB   threads " + Thread.activeCount(),
        };

        Font old = g2.getFont();
        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(6, 6, 330, lines.length * 18 + 10);
        g2.setColor(Color.GREEN);
        for (int i = 0; i < lines.length; i++) g2.drawString(lines[i], 12, 24 + i * 18);
        g2.setFont(old);
    }
}
