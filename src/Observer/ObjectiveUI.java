package Observer;

import Entity.NPC.NPC;
import Main.GamePanel;
import Main.Json;
import ProgressManagement.QuestManager.Quest;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Obiectivul curent, pe un singur rând, în stânga-sus (sub nivel), și o săgeată la marginea ecranului spre țintă când aceasta
 * e în afara vederii. Se calculează din misiunile principale: textul și locurile ("steps") sunt în quests.json -> mainQuests.
 */
public class ObjectiveUI {
    private final GamePanel gp;

    /** Ce se afișează acum: text, și (dacă țința e în zona curentă) unde e ea, în tile-uri. */
    public record Step(String text, boolean hasTarget, int col, int row) {}

    public ObjectiveUI(GamePanel gp) {
        this.gp = gp;
    }

    /** Misiunea principală de care e vorba acum, ca la Cronicar: prima nepredată. */
    private Quest current() {
        for (Quest q : gp.questManager.mainQuests) if (!q.isCompleted || q.isReceived) return q;
        return null;
    }

    public Step step() {
        Quest q = current();
        if (q == null) return null;
        // Nu am primit încă misiunea, sau e îndeplinită și așteaptă să fie predată: la Cronicar
        if (!q.isReceived || q.isCompleted) return cronicar(q.isReceived ? "Întoarce-te la Bătrânul Cronicar" : "Vorbește cu Bătrânul Cronicar");

        String zone = gp.tileM.currentZone;
        Map<String, Object> pick = null, generic = null;
        List<Map<String, Object>> steps = gp.questManager.mainSteps(q.id);
        for (Map<String, Object> s : steps) {
            String z = Json.str(s.get("zone"), null);
            if (zone != null && zone.equals(z)) { pick = s; break; }
            if (z == null && generic == null) generic = s;
        }
        if (pick == null) pick = generic;
        String text;
        boolean target = false;
        int col = 0, row = 0;
        if (pick == null) {
            text = q.title;
        } else {
            text = Json.str(pick.get("text"), q.title);
            if (pick.get("col") != null && zone != null && zone.equals(Json.str(pick.get("zone"), null))) {
                target = true;
                col = Json.integer(pick.get("col"), 0);
                row = Json.integer(pick.get("row"), 0);
            }
        }
        if (q.requiredAmount > 1 && !target) text += " (" + q.currentAmount + "/" + q.requiredAmount + ")";   // contorul doar la "învinge N inamici", nu la drumul spre zonă
        return new Step(text, target, col, row);
    }

    private Step cronicar(String text) {
        for (NPC n : gp.npc) {
            if (n != null && "Bătrânul Cronicar".equals(n.name)) return new Step(text, true, n.worldX / gp.tileSize, n.worldY / gp.tileSize);
        }
        return new Step(text, false, 0, 0);
    }

    public void draw(Graphics2D g2) {
        Step s = step();
        if (s == null) return;
        Font old = g2.getFont();

        // rândul de obiectiv, sub nivel
        int x = 22, y = 172;
        g2.setFont(old.deriveFont(Font.BOLD, 18F));
        FontMetrics fm = g2.getFontMetrics();
        String label = "OBIECTIV";
        int maxText = Math.min(gp.screenWidth / 2, 560);
        String text = ellipsize(fm, s.text(), maxText);
        int w = Math.max(fm.stringWidth(text), fm.stringWidth(label)) + 28;
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(x - 10, y - 20, w, 52, 12, 12);
        g2.setColor(new Color(212, 175, 55));
        g2.setFont(old.deriveFont(Font.BOLD, 12F));
        g2.drawString(label, x, y - 4);
        g2.setFont(old.deriveFont(Font.BOLD, 18F));
        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y + 20);

        if (s.hasTarget()) drawArrow(g2, s.col(), s.row());
        g2.setFont(old);
    }

    private static String ellipsize(FontMetrics fm, String t, int max) {
        if (fm.stringWidth(t) <= max) return t;
        while (t.length() > 1 && fm.stringWidth(t + "…") > max) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    /** Săgeată la marginea ecranului, spre țintă, dacă aceasta nu se vede. */
    private void drawArrow(Graphics2D g2, int col, int row) {
        int ts = gp.tileSize;
        double tx = gp.camera.screenX(col * ts) + ts / 2.0, ty = gp.camera.screenY(row * ts) + ts / 2.0;
        int W = gp.screenWidth, H = gp.screenHeight, margin = 70;
        if (tx > -ts / 2.0 && tx < W + ts / 2.0 && ty > -ts / 2.0 && ty < H + ts / 2.0) return;   // se vede: nu e nevoie de săgeată

        double cx = W / 2.0, cy = H / 2.0, dx = tx - cx, dy = ty - cy;
        double len = Math.hypot(dx, dy);
        if (len < 1) return;
        double ux = dx / len, uy = dy / len;
        // punctul de pe marginea (cu rest) dreptunghiului ecranului, în direcția țintei
        double scale = Math.min((W / 2.0 - margin) / Math.max(1e-6, Math.abs(ux)), (H / 2.0 - margin) / Math.max(1e-6, Math.abs(uy)));
        double ax = cx + ux * scale, ay = cy + uy * scale;

        float pulse = 0.5f + 0.5f * (float) Math.sin(gp.fx.ticks * 0.12);
        double bob = pulse * 6;
        double bx = ax + ux * bob, by = ay + uy * bob;
        double ang = Math.atan2(uy, ux);
        Polygon tri = new Polygon();
        tri.addPoint((int) Math.round(bx + Math.cos(ang) * 22), (int) Math.round(by + Math.sin(ang) * 22));
        tri.addPoint((int) Math.round(bx + Math.cos(ang + 2.5) * 18), (int) Math.round(by + Math.sin(ang + 2.5) * 18));
        tri.addPoint((int) Math.round(bx + Math.cos(ang - 2.5) * 18), (int) Math.round(by + Math.sin(ang - 2.5) * 18));
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillOval((int) bx - 26, (int) by - 26, 52, 52);
        g2.setColor(new Color(255, 214, 90, 210 + Math.round(45 * pulse)));
        g2.fillPolygon(tri);
        g2.setColor(new Color(60, 40, 0));
        g2.drawPolygon(tri);
    }
}
