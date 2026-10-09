package GameState;

import Entity.Allies.Ally;
import Main.GamePanel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Scena în care un aliat se alătură echipei: replicile vin din allies.json (scene), ENTER trece mai departe, iar la final
 * aliatul intră în echipă. Se joacă după ecranul de recompensă al boss-ului care îl recrutează.
 */
public class AllySceneState implements GameState {
    private final GamePanel gp;
    private final Ally ally;
    private int page = 0, chars = 0, ticks = 0;
    private boolean done = false;

    public AllySceneState(GamePanel gp, String allyId) {
        this.gp = gp;
        this.ally = gp.party.define(allyId);
    }

    @Override
    public void enter() {
        gp.keyH.enterPressed = false;
        if (ally == null || ally.scene.isEmpty()) finish();
    }

    private String text() { return ally.scene.get(page).text; }

    @Override
    public void update() {
        gp.gameTimer.update();
        if (done) return;
        ticks++;
        if (chars < text().length()) chars = Math.min(text().length(), chars + 2);

        if (gp.keyH.enterPressed) {
            gp.keyH.enterPressed = false;
            if (ticks < 15) return; // ENTER-ul de pe ecranul de recompensă nu sare prima replică
            if (chars < text().length()) {
                chars = text().length();
            } else if (page + 1 < ally.scene.size()) {
                page++;
                chars = 0;
            } else {
                finish();
            }
        }
    }

    private void finish() {
        if (done) return;
        done = true;
        if (ally != null) {
            gp.party.recruit(ally.id);
            gp.ui.message = ally.name + " s-a alăturat echipei!";
            gp.ui.messageOn = true;
            gp.fx.questSound(true);
        }
        gp.finishPostBattle();
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.tileM.draw(g2);
        gp.player.draw(g2);
        gp.fx.drawWorld(g2);
        gp.ui.draw(g2);
        if (ally == null || ally.scene.isEmpty() || done) return;

        int W = gp.screenWidth, H = gp.screenHeight;
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, W, H);

        Ally.Line line = ally.scene.get(page);
        boolean allyTalks = line.speaker.equals(ally.name);

        // portretele: aliatul în stânga, Eron în dreapta; cel care vorbește e luminat
        BufferedImage ai = ally.image(), ei = gp.player.fightStand;
        int ph = (int) (H * 0.46);
        if (ai != null) drawPortrait(g2, ai, (int) (W * 0.12), H - (int) (H * 0.30) - ph, ph, allyTalks);
        if (ei != null) drawPortrait(g2, ei, (int) (W * 0.70), H - (int) (H * 0.30) - ph, ph, !allyTalks);

        // caseta de dialog
        int bx = (int) (W * 0.08), by = H - (int) (H * 0.28), bw = W - 2 * bx, bh = (int) (H * 0.23);
        g2.setColor(new Color(18, 16, 28, 235));
        g2.fillRoundRect(bx, by, bw, bh, 26, 26);
        g2.setColor(new Color(212, 175, 55));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(bx, by, bw, bh, 26, 26);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28F));
        g2.setColor(allyTalks ? new Color(ally.color) : new Color(190, 150, 255));
        g2.drawString(line.speaker, bx + 30, by + 44);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 24F));
        g2.setColor(Color.WHITE);
        int ty = by + 84;
        for (String l : wrap(line.text.substring(0, Math.min(chars, line.text.length())), g2.getFontMetrics(), bw - 60)) {
            g2.drawString(l, bx + 30, ty);
            ty += 32;
        }

        boolean ready = chars >= line.text.length();
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 17F));
        g2.setColor(new Color(170, 170, 185));
        String hint = ready ? (page + 1 < ally.scene.size() ? "[ENTER] continuă" : "[ENTER] primește-l în echipă") : "[ENTER] sari peste text";
        g2.drawString(hint, bx + bw - 30 - g2.getFontMetrics().stringWidth(hint), by + bh - 18);
    }

    private void drawPortrait(Graphics2D g2, BufferedImage img, int x, int y, int h, boolean lit) {
        int w = h * img.getWidth() / Math.max(1, img.getHeight());
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, lit ? 1f : 0.45f));
        g2.drawImage(img, x, y + (lit ? 0 : 10), w, h, null);
        g2.setComposite(old);
    }

    private static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ", -1)) {
            if (cur.length() > 0 && fm.stringWidth(cur + " " + w) > width) { lines.add(cur.toString()); cur.setLength(0); }
            if (cur.length() > 0) cur.append(' ');
            cur.append(w);
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    @Override public void exit() {}
}
