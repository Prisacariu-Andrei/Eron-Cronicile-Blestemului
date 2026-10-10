package Entity.NPC;

import GameState.PlayState;
import Main.GamePanel;
import Main.Json;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ritul Luminii Captive din Tundra (res/Data/tundra.json): Axul Solstițiului (manivela) din labirint aprinde cristalul din Sala Oglinzilor,
 * iar fasciculul trebuie îndreptat din Oglinzi (F le întoarce '/' <-> '\') până la Poarta de Gheață, care se topește.
 * Fasciculul se recalculează după fiecare manivelă/oglindă. Starea durabilă = flag-urile tundra_crank și tundra_door.
 */
public class TundraPuzzle {
    public static final String CRANK_FLAG = "tundra_crank", DOOR_FLAG = "tundra_door";

    /** Pune obiectele puzzle-ului în primele locuri libere din gp.npc (emițătorul primul: fasciculul se desenează sub oglinzi). */
    public static void spawn(GamePanel gp) {
        Map<String, Object> cfg = Json.map(Json.parseResource("/Data/tundra.json"));
        List<NPC> objects = new ArrayList<>();
        var e = Json.map(cfg.get("emitter"));
        objects.add(new Emitter(gp, Json.integer(e.get("col"), 0), Json.integer(e.get("row"), 0), Json.str(e.get("dir"), "east")));
        for (Object o : Json.list(cfg.get("mirrors"))) {
            var m = Json.map(o);
            objects.add(new Mirror(gp, Json.integer(m.get("col"), 0), Json.integer(m.get("row"), 0), !"\\".equals(Json.str(m.get("start"), "/"))));
        }
        var c = Json.map(cfg.get("crank"));
        objects.add(new Crank(gp, Json.integer(c.get("col"), 0), Json.integer(c.get("row"), 0)));
        var d = Json.map(cfg.get("door"));
        objects.add(new Door(gp, Json.integer(d.get("col"), 0), Json.integer(d.get("row"), 0), Json.integer(d.get("widthTiles"), 3)));

        int slot = 0;
        for (NPC n : objects) {
            while (slot < gp.npc.length && gp.npc[slot] != null) slot++;
            if (slot >= gp.npc.length) break;
            gp.npc[slot++] = n;
        }
        refresh(gp);
    }

    /** Recalculează fasciculul; dacă ajunge la poartă, o deschide (o singură dată, flag-ul rămâne în salvare). */
    public static void refresh(GamePanel gp) {
        Emitter em = null;
        Door door = null;
        for (var n : gp.npc) {
            if (n instanceof Emitter x) em = x;
            else if (n instanceof Door x) door = x;
        }
        if (em == null) return;
        em.trace(gp.story.has(CRANK_FLAG), door);
        if (em.reachesDoor && door != null && !gp.story.has(DOOR_FLAG)) door.melt();
    }

    private static void hint(GamePanel gp, String text) {
        gp.activeNpcIndex = -1;
        gp.changeState(new PlayState(gp));
        gp.ui.message = text;
        gp.ui.messageOn = true;
    }

    private static void say(GamePanel gp, String text) {
        gp.ui.message = text;
        gp.ui.messageOn = true;
    }

    /** Obiect de puzzle cu o dală (sau mai multe) și hitbox-ul pe ea. */
    private abstract static class Piece extends NPC {
        final int col, row;
        Piece(GamePanel gp, int col, int row, String name, int widthTiles) {
            super(gp, gp.tileSize * col, gp.tileSize * row);
            this.col = col;
            this.row = row;
            this.name = name;
            hitBox = new Rectangle(0, 0, gp.tileSize * widthTiles, gp.tileSize);
        }
        @Override public void getImage() { }
        @Override public void setDialogue() { }
        boolean covers(int c, int r) { return worldX >= 0 && r == row && c >= col && c < col + hitBox.width / gp.tileSize; }
        boolean onScreen(int sx, int sy, int w, int h) { return sx + w >= 0 && sx <= gp.screenWidth && sy + h >= 0 && sy <= gp.screenHeight; }
    }

    /** Axul Solstițiului (manivela): F o trage, o singură dată. */
    public static class Crank extends Piece {
        public Crank(GamePanel gp, int col, int row) { super(gp, col, row, "Axul Solstițiului", 1); }
        private boolean pulled() { return gp.story.has(CRANK_FLAG); }
        @Override public boolean usesF() { return true; }
        @Override public int markerKind() { return pulled() ? MARKER_NONE : MARKER_AVAILABLE; }

        @Override public void use() {
            if (pulled()) { say(gp, "Axul Solstițiului e deja trezit."); return; }
            gp.story.set(CRANK_FLAG);
            gp.fx.phaseChange();
            gp.fx.burst(2, gp.camera.screenX(worldX) + gp.tileSize / 2f, gp.camera.screenY(worldY) + gp.tileSize / 2f, 30, 0x9FE8FF, 7f, 40, 6f, 0.04f);
            say(gp, "Axul se rotește: o Lance de Lumină se aprinde în Sala Oglinzilor.");
            refresh(gp);
        }

        @Override public void speak() { hint(gp, pulled() ? "Axul Solstițiului e deja trezit." : "Apasă [F] ca să trezești Axul Solstițiului."); }

        @Override public void draw(Graphics2D g2) {
            int sx = gp.camera.screenX(worldX), sy = gp.camera.screenY(worldY);
            if (!onScreen(sx, sy, gp.tileSize, gp.tileSize)) return;
            var tile = gp.tileM.tile[pulled() ? 2 : 3];
            if (tile != null && tile.image != null) g2.drawImage(tile.image, sx, sy, gp.tileSize, gp.tileSize, null);
            else { g2.setColor(new Color(150, 220, 255)); g2.fillRect(sx + 20, sy + 10, 24, 44); }
            if (!pulled()) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(gp.fx.ticks * 0.08);
                g2.setColor(new Color(150, 230, 255, 70 + Math.round(90 * pulse)));
                g2.setStroke(new BasicStroke(3));
                g2.drawOval(sx + 4, sy + 4, gp.tileSize - 8, gp.tileSize - 8);
            }
            markerOnly(g2, sx, sy);
        }

        private void markerOnly(Graphics2D g2, int sx, int sy) {
            if (pulled()) return;
            float hop = Math.abs((float) Math.sin(gp.fx.ticks * 0.12)) * 7f;
            Font old = g2.getFont();
            g2.setFont(old.deriveFont(Font.BOLD, 34F));
            int x = sx + gp.tileSize / 2 - g2.getFontMetrics().stringWidth("!") / 2, y = sy - 6 - Math.round(hop);
            g2.setColor(Color.BLACK);
            g2.drawString("!", x + 2, y + 2);
            g2.setColor(new Color(255, 221, 60));
            g2.drawString("!", x, y);
            g2.setFont(old);
        }
    }

    /** Cristalul care trimite Lancea de Lumină; desenează fasciculul. */
    public static class Emitter extends Piece {
        private final int dx, dy;
        boolean lit, reachesDoor;
        private final List<double[]> path = new ArrayList<>();   // colțurile fasciculului, în dale (centrele dalelor)

        public Emitter(GamePanel gp, int col, int row, String dir) {
            super(gp, col, row, "Cristal de Selenit", 1);
            switch (dir) {
                case "west" -> { dx = -1; dy = 0; }
                case "north" -> { dx = 0; dy = -1; }
                case "south" -> { dx = 0; dy = 1; }
                default -> { dx = 1; dy = 0; }
            }
        }

        /** Urmărește fasciculul dală cu dală: se oprește în zid, se reflectă în oglinzi, se oprește în poartă. */
        void trace(boolean on, Door door) {
            path.clear();
            lit = on;
            reachesDoor = false;
            if (!on) return;
            int c = col, r = row, vx = dx, vy = dy;
            path.add(new double[]{c + 0.5, r + 0.5});
            for (int step = 0; step < 120; step++) {
                c += vx;
                r += vy;
                if (c < 0 || r < 0 || c >= gp.maxWorldCol || r >= gp.maxWorldRow) break;
                if (door != null && door.covers(c, r)) {
                    path.add(new double[]{c + 0.5, r + 0.5});
                    reachesDoor = true;
                    return;
                }
                int id = gp.tileM.mapTileNum[c][r];
                if (id < 0 || id >= gp.tileM.tile.length || gp.tileM.tile[id] == null || gp.tileM.tile[id].collision) {
                    path.add(new double[]{c + 0.5 - vx * 0.5, r + 0.5 - vy * 0.5});
                    return;
                }
                Mirror m = mirrorAt(c, r);
                if (m != null) {
                    path.add(new double[]{c + 0.5, r + 0.5});
                    int nx = m.slash ? -vy : vy, ny = m.slash ? -vx : vx;   // '/' : (x,y)->(-y,-x); '\' : (x,y)->(y,x)
                    vx = nx;
                    vy = ny;
                }
            }
            path.add(new double[]{c + 0.5, r + 0.5});
        }

        private Mirror mirrorAt(int c, int r) {
            for (var n : gp.npc) if (n instanceof Mirror m && m.col == c && m.row == r) return m;
            return null;
        }

        @Override public void speak() { hint(gp, lit ? "Cristalul strălucește: călăuzește fasciculul spre Poarta de Gheață." : "Un cristal de selenit, stins. Undeva în labirint, un ax ar putea să-l trezească."); }

        @Override public void draw(Graphics2D g2) {
            int ts = gp.tileSize;
            if (lit && path.size() > 1) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(gp.fx.ticks * 0.15);
                int[] xs = new int[path.size()], ys = new int[path.size()];
                for (int i = 0; i < xs.length; i++) {
                    xs[i] = gp.camera.screenX((int) Math.round(path.get(i)[0] * ts));
                    ys[i] = gp.camera.screenY((int) Math.round(path.get(i)[1] * ts));
                }
                g2.setStroke(new BasicStroke(16, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(120, 220, 255, 50 + Math.round(40 * pulse)));
                g2.drawPolyline(xs, ys, xs.length);
                g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(190, 240, 255, 200));
                g2.drawPolyline(xs, ys, xs.length);
                g2.setStroke(new BasicStroke(2));
                g2.setColor(Color.WHITE);
                g2.drawPolyline(xs, ys, xs.length);
            }
            int sx = gp.camera.screenX(worldX), sy = gp.camera.screenY(worldY);
            if (!onScreen(sx, sy, ts, ts)) return;
            int cx = sx + ts / 2, cy = sy + ts / 2;
            g2.setColor(lit ? new Color(150, 230, 255) : new Color(70, 90, 120));
            g2.fillPolygon(new int[]{cx, cx + 18, cx, cx - 18}, new int[]{cy - 26, cy, cy + 26, cy}, 4);
            g2.setColor(lit ? Color.WHITE : new Color(110, 130, 160));
            g2.setStroke(new BasicStroke(2));
            g2.drawPolygon(new int[]{cx, cx + 18, cx, cx - 18}, new int[]{cy - 26, cy, cy + 26, cy}, 4);
        }
    }

    /** Oglindă de Selenit: F o întoarce între '/' și '\'. */
    public static class Mirror extends Piece {
        boolean slash;
        public Mirror(GamePanel gp, int col, int row, boolean slash) { super(gp, col, row, "Oglindă de Selenit", 1); this.slash = slash; }
        @Override public boolean usesF() { return true; }

        @Override public void use() {
            slash = !slash;
            gp.fx.burst(2, gp.camera.screenX(worldX) + gp.tileSize / 2f, gp.camera.screenY(worldY) + gp.tileSize / 2f, 8, 0xBFEFFF, 3f, 20, 4f, 0f);
            say(gp, "Oglinda se rotește (" + (slash ? "/" : "\\") + ").");
            refresh(gp);
        }

        @Override public void speak() { hint(gp, "Apasă [F] ca să întorci oglinda."); }

        @Override public void draw(Graphics2D g2) {
            int ts = gp.tileSize, sx = gp.camera.screenX(worldX), sy = gp.camera.screenY(worldY);
            if (!onScreen(sx, sy, ts, ts)) return;
            g2.setColor(new Color(34, 52, 84, 230));
            g2.fillRoundRect(sx + 6, sy + 6, ts - 12, ts - 12, 12, 12);
            g2.setColor(new Color(150, 200, 235));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(sx + 6, sy + 6, ts - 12, ts - 12, 12, 12);
            g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(215, 245, 255));
            if (slash) g2.drawLine(sx + 16, sy + ts - 16, sx + ts - 16, sy + 16);
            else g2.drawLine(sx + 16, sy + 16, sx + ts - 16, sy + ts - 16);
        }
    }

    /** Poarta de Gheață dinaintea Glaciarei: nu se deschide cu forța, doar fasciculul o topește. */
    public static class Door extends Piece {
        private final int widthTiles;
        private int melting = -1;   // tick-uri de topire rămase; -1 = nu se topește

        public Door(GamePanel gp, int col, int row, int widthTiles) {
            super(gp, col, row, "Poarta de Gheață", widthTiles);
            this.widthTiles = widthTiles;
            if (gp.story.has(DOOR_FLAG)) vanish();   // salvare încărcată după ce poarta se topise
        }

        private void vanish() { worldX = -100000; worldY = -100000; }

        /** Fasciculul a ajuns la poartă: flag-ul se pune imediat (și dacă jucătorul salvează), poarta dispare după o scurtă animație. */
        void melt() {
            gp.story.set(DOOR_FLAG);
            melting = 50;
            gp.fx.phaseChange();
            gp.fx.burst(2, gp.camera.screenX(worldX) + gp.tileSize * widthTiles / 2f, gp.camera.screenY(worldY) + gp.tileSize / 2f, 50, 0xBFEFFF, 9f, 50, 7f, 0.05f);
            say(gp, "Lancea de Lumină topește Poarta de Gheață: calea spre Glaciara e liberă!");
            gp.questManager.updateQuestProgress(10, true, 1);   // misiunea "Labirintul de Gheață"
        }

        @Override public void speak() { hint(gp, "O poartă de gheață veche de veacuri. Doar lumina captivă a Solstițiului o poate topi."); }

        @Override public void draw(Graphics2D g2) {
            if (worldX < -50000) return;
            if (melting > 0 && --melting == 0) { vanish(); return; }
            int ts = gp.tileSize, sx = gp.camera.screenX(worldX), sy = gp.camera.screenY(worldY), w = ts * widthTiles;
            if (!onScreen(sx, sy, w, ts)) return;
            float k = melting < 0 ? 1f : melting / 50f;
            int h = Math.max(2, Math.round(ts * k));
            g2.setColor(new Color(150, 205, 235, Math.round(235 * k)));
            g2.fillRect(sx, sy + ts - h, w, h);
            g2.setColor(new Color(230, 248, 255, Math.round(230 * k)));
            g2.setStroke(new BasicStroke(2));
            for (int i = 0; i <= widthTiles; i++) g2.drawLine(sx + i * ts, sy + ts - h, sx + i * ts, sy + ts);
            g2.drawRect(sx, sy + ts - h, w - 1, h - 1);
        }
    }
}
