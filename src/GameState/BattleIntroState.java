package GameState;

import Entity.Inamici.Enemy;
import Main.GamePanel;
import Main.Json;
import Main.Settings;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dinaintea fiecărei lupte: pentru boss-ii din res/Data/boss_intro.json un dialog (căsuță jos, numele celui care vorbește, replici
 * alternate boss / Eron, ENTER sau E); pentru inamicii obișnuiți nimic. Apoi tranziția în spirală (Main.SpiralWipe) care începe lupta
 * când ecranul e negru. Harta rămâne desenată dedesubt, nemișcată.
 */
public class BattleIntroState implements GameState {
    private record Line(String speaker, boolean boss, String text) {}

    private final GamePanel gp;
    private final Enemy first;
    private final PlayState scene;
    private final List<Line> lines = new ArrayList<>();
    private int index = 0, shown = 0, counter = 0, openTicks = 0;
    private boolean closing = false;

    public BattleIntroState(GamePanel gp, Enemy first) {
        this.gp = gp;
        this.first = first;
        this.scene = new PlayState(gp);
        String key = first.name.split(",")[0].trim();
        Map<String, Object> boss = Json.map(Json.map(Json.map(Json.parseResource("/Data/boss_intro.json")).get("bosses")).get(key));
        String bossName = Json.str(boss.get("name"), first.name);
        for (Object o : Json.list(boss.get("lines"))) {
            Map<String, Object> l = Json.map(o);
            boolean isBoss = !"eron".equals(Json.str(l.get("who"), "boss"));
            lines.add(new Line(isBoss ? bossName : "Eron", isBoss, Json.str(l.get("text"), "")));
        }
    }

    public boolean hasDialogue() { return !lines.isEmpty(); }

    @Override
    public void enter() {
        gp.keyH.enterPressed = false;
        gp.player.moveVelocity = 0f;   // Eron nu mai alunecă pe hartă cât durează scena
        if (lines.isEmpty()) startSpiral();
    }

    private void startSpiral() {
        closing = true;
        gp.spiral.close(() -> gp.player.startBattleNow(first));   // când ecranul e negru începe lupta; spirala se desface peste ea
    }

    /** ENTER / E: dacă textul încă apare, îl arată pe tot; altfel trece la replica următoare (după ultima pornește spirala). */
    public void advance() {
        if (closing) return;
        if (shown < lines.get(index).text().length()) { shown = lines.get(index).text().length(); return; }
        index++;
        shown = 0;
        counter = 0;
        if (index >= lines.size()) startSpiral();
    }

    @Override
    public void update() {
        gp.gameTimer.update();
        if (closing) return;
        if (openTicks < 12) openTicks++;
        String text = lines.get(index).text();
        if (shown < text.length() && ++counter >= Settings.dialogueTicksPerChar()) { shown++; counter = 0; }
    }

    @Override
    public void draw(Graphics2D g2) {
        scene.draw(g2);   // harta, în spatele dialogului și al spiralei
        if (closing || index >= lines.size()) return;

        Line line = lines.get(index);
        int W = gp.screenWidth, H = gp.screenHeight;
        int bw = W - gp.tileSize * 4, bh = (int) (H * 0.26), bx = gp.tileSize * 2, by = H - bh - gp.tileSize / 2;
        float open = Math.min(1f, openTicks / 12f);
        by += Math.round((1f - open) * 30);
        Composite oldC = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.01f, open)));

        Color accent = line.boss() ? new Color(212, 120, 120) : new Color(120, 200, 255);
        g2.setColor(new Color(10, 8, 20, 235));
        g2.fillRoundRect(bx, by, bw, bh, 26, 26);
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(bx, by, bw, bh, 26, 26);

        // numele celui care vorbește, pe un limb deasupra căsuței
        Font base = g2.getFont();
        g2.setFont(base.deriveFont(Font.BOLD, 26F));
        int nw = g2.getFontMetrics().stringWidth(line.speaker()) + 40;
        int nx = line.boss() ? bx + 30 : bx + bw - nw - 30;
        g2.setColor(new Color(10, 8, 20, 245));
        g2.fillRoundRect(nx, by - 22, nw, 44, 16, 16);
        g2.setColor(accent);
        g2.drawRoundRect(nx, by - 22, nw, 44, 16, 16);
        g2.drawString(line.speaker(), nx + 20, by + 10);

        // textul, rupt pe rânduri
        g2.setFont(base.deriveFont(Font.PLAIN, 25F));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        String visible = line.text().substring(0, Math.min(shown, line.text().length()));
        int y = by + 66, maxW = bw - 60;
        StringBuilder row = new StringBuilder();
        for (String word : visible.split(" ", -1)) {
            String trial = row.length() == 0 ? word : row + " " + word;
            if (fm.stringWidth(trial) > maxW && row.length() > 0) {
                g2.drawString(row.toString(), bx + 30, y);
                y += 36;
                row = new StringBuilder(word);
            } else row = new StringBuilder(trial);
        }
        g2.drawString(row.toString(), bx + 30, y);

        g2.setFont(base.deriveFont(Font.ITALIC, 16F));
        g2.setColor(new Color(170, 170, 190));
        boolean done = shown >= line.text().length();
        if (!done || gp.fx.ticks / 30 % 2 == 0) g2.drawString(index == lines.size() - 1 && done ? "ENTER / E  -  la luptă" : "ENTER / E  -  mai departe", bx + bw - 230, by + bh - 16);
        g2.setFont(base);
        g2.setComposite(oldC);
    }

    @Override public void exit() { }
}
