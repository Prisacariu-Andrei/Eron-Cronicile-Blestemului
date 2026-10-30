package Observer;

import Entity.Items.Card;
import Main.GamePanel;
import Main.SoundManager;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.function.IntConsumer;

/**
 * O alegere cu două opțiuni în mijlocul luptei (Vama lui Charon): ← → sau A D aleg, ENTER confirmă.
 * Cât ține alegerea, tura inamicilor așteaptă (Player.enemyActs). Mai conține și marcajul cărților înghețate (Glaciara).
 */
public final class ChoiceUI {
    private final GamePanel gp;
    private String title = "", text = "";
    private String[] options = new String[0];
    private boolean[] enabled = new boolean[0];
    private int sel = 0;
    private IntConsumer onChoose;

    public ChoiceUI(GamePanel gp) { this.gp = gp; }

    public boolean active() { return onChoose != null; }

    public void clear() { onChoose = null; }

    /** Pornește alegerea; opțiunile dezactivate nu pot fi alese. onChoose primește indicele opțiunii. */
    public void ask(String title, String text, String[] options, boolean[] enabled, IntConsumer onChoose) {
        this.title = title;
        this.text = text;
        this.options = options;
        this.enabled = enabled;
        this.sel = 0;
        while (sel < options.length - 1 && !enabled[sel]) sel++;
        this.onChoose = onChoose;
    }

    public void onKey(int code) {
        if (!active()) return;
        int dir = (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) ? -1 : (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) ? 1 : 0;
        if (dir != 0) {
            for (int i = sel + dir; i >= 0 && i < options.length; i += dir) {
                if (enabled[i]) { sel = i; SoundManager.getInstance().playSE("click"); break; }
            }
        } else if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) {
            IntConsumer cb = onChoose;
            onChoose = null;
            cb.accept(sel);
        }
    }

    public void draw(Graphics2D g2) {
        if (!active()) return;
        int w = gp.screenWidth, h = gp.screenHeight;
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, w, h);

        int pw = (int) (w * 0.62), ph = (int) (h * 0.42), px = (w - pw) / 2, py = (int) (h * 0.22);
        g2.setColor(new Color(24, 18, 40, 240));
        g2.fillRoundRect(px, py, pw, ph, 24, 24);
        g2.setColor(new Color(190, 150, 255));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(px, py, pw, ph, 24, 24);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, w / 34f));
        center(g2, Main.Lang.tr(title), w / 2, py + (int) (ph * 0.2), new Color(255, 232, 160));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, w / 62f));
        center(g2, Main.Lang.tr(text), w / 2, py + (int) (ph * 0.36), new Color(225, 215, 235));

        int n = options.length, bw = (int) (pw * 0.42), bh = (int) (ph * 0.30), gap = (int) (pw * 0.06);
        int sx = w / 2 - (n * bw + (n - 1) * gap) / 2, by = py + (int) (ph * 0.50);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, w / 66f));
        for (int i = 0; i < n; i++) {
            int bx = sx + i * (bw + gap);
            boolean on = i == sel;
            g2.setColor(!enabled[i] ? new Color(50, 46, 60) : on ? new Color(110, 70, 170) : new Color(60, 44, 90));
            g2.fillRoundRect(bx, by, bw, bh, 16, 16);
            g2.setColor(on ? Color.YELLOW : new Color(120, 100, 150));
            g2.setStroke(new BasicStroke(on ? 4 : 2));
            g2.drawRoundRect(bx, by, bw, bh, 16, 16);
            String label = Main.Lang.tr(options[i]);
            int y = by + bh / 2 - 4;
            for (String line : label.split("\n")) {
                center(g2, line, bx + bw / 2, y, !enabled[i] ? new Color(120, 115, 130) : Color.WHITE);
                y += g2.getFontMetrics().getHeight();
            }
        }
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, w / 90f));
        center(g2, Main.Lang.tr("← → alege   ENTER confirmă"), w / 2, py + ph - 14, new Color(180, 170, 200));
    }

    private static void center(Graphics2D g2, String s, int cx, int y, Color c) {
        int x = cx - g2.getFontMetrics().stringWidth(s) / 2;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(s, x + 2, y + 2);
        g2.setColor(c);
        g2.drawString(s, x, y);
    }

    /** O carte înghețată de Glaciara: văl de gheață peste ea și o etichetă (nu poate fi jucată în tura asta). */
    public static void drawFrozen(Graphics2D g2, Card c, int x, int y, int w, int h) {
        if (!c.frozen) return;
        g2.setColor(new Color(150, 220, 255, 140));
        g2.fillRoundRect(x, y, w, h, 18, 18);
        g2.setColor(new Color(230, 248, 255));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(x, y, w, h, 18, 18);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, Math.max(12f, w * 0.14f)));
        String s = Main.Lang.tr("ÎNGHEȚATĂ");
        g2.setColor(new Color(10, 40, 80));
        g2.drawString(s, x + (w - g2.getFontMetrics().stringWidth(s)) / 2, y + h / 2);
    }
}
