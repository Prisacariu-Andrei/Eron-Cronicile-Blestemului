package ProgressManagement;

import Main.GamePanel;

import java.awt.*;

/**
 * Obiecte de cules din lume (pentru obiectivele "collect" din quests.json). Un obiect apare doar cât timp o misiune
 * activă îl cere și încă nu a fost cules; se culege atingându-l.
 */
public class Pickups {
    private final GamePanel gp;

    public Pickups(GamePanel gp) {
        this.gp = gp;
    }

    private boolean visible(QuestLog.Pickup p) {
        return p.zone.equals(gp.tileM.currentZone) && !gp.questLog.isCollected(p.id) && gp.questLog.needsItem(p.item);
    }

    /** Un tick: culege ce atinge Eron și lasă mici scântei în jurul obiectelor vizibile. */
    public void update() {
        float px = gp.player.worldX + gp.tileSize / 2f, py = gp.player.worldY + gp.tileSize / 2f;
        for (QuestLog.Pickup p : gp.questLog.pickups) {
            if (!visible(p)) continue;
            float cx = p.col * gp.tileSize + gp.tileSize / 2f, cy = p.row * gp.tileSize + gp.tileSize / 2f;

            if (Math.hypot(px - cx, py - cy) < gp.tileSize * 0.75) {
                gp.fx.pickupBurst(cx, cy, p.rgb);
                gp.questLog.collect(p);
                continue;
            }
            if (gp.fx.ticks % 14 == 0) gp.fx.pickupGlint(cx, cy, p.rgb);
        }
    }

    /** Un halou care pulsează și o floare mică, ca să se vadă de departe. */
    public void draw(Graphics2D g2) {
        for (QuestLog.Pickup p : gp.questLog.pickups) {
            if (!visible(p)) continue;
            int sx = gp.camera.screenX(p.col * gp.tileSize) + gp.tileSize / 2;
            int sy = gp.camera.screenY(p.row * gp.tileSize) + gp.tileSize / 2;
            if (sx < -60 || sy < -60 || sx > gp.screenWidth + 60 || sy > gp.screenHeight + 60) continue;

            double t = gp.fx.ticks * 0.08 + p.col;
            int bob = (int) Math.round(Math.sin(t) * 3);
            float pulse = 0.5f + 0.5f * (float) Math.sin(t * 1.3);
            Color base = new Color(p.rgb);

            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 40 + Math.round(40 * pulse)));
            g2.fillOval(sx - 26, sy - 26 + bob, 52, 52);
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 70));
            g2.fillOval(sx - 17, sy - 17 + bob, 34, 34);

            // petale în jurul unui centru luminos
            for (int i = 0; i < 5; i++) {
                double a = i * Math.PI * 2 / 5 + gp.fx.ticks * 0.01;
                int ox = (int) Math.round(Math.cos(a) * 8), oy = (int) Math.round(Math.sin(a) * 8);
                g2.setColor(base);
                g2.fillOval(sx + ox - 5, sy + oy - 5 + bob, 10, 10);
                g2.setColor(Color.WHITE);
                g2.drawOval(sx + ox - 5, sy + oy - 5 + bob, 10, 10);
            }
            g2.setColor(new Color(255, 236, 150));
            g2.fillOval(sx - 5, sy - 5 + bob, 10, 10);
        }
    }
}
