package Observer;

import Main.Easing;
import Main.GameFeel;
import Main.GamePanel;
import Main.SoundManager;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Prologul jocului, jucat peste harta Eldoria reală: o cameră care se mișcă lent (cu easing și zoom),
 * bare "cinema" care alunecă, fade-uri între cadre, ceață cu paralaxă, licurici, text care apare literă cu literă
 * (cu un mic sunet) și personaje care nu stau înghețate. Reglaje: GameFeel (secțiunea CUTSCENE).
 *
 * Tot ce ține de timp rulează în update(), pe ticks fixe, nu în draw().
 */
public class CutsceneUI {
    GamePanel gp;

    /** Un cadru: ce text apare și cum se mișcă camera (centrul vederii, în coordonate de tile-uri). */
    private record Shot(String title, String text, double fromX, double fromY, double toX, double toY,
                        float zoomFrom, float zoomTo, int moveTicks) {}

    private static final Shot[] SHOTS = {
            new Shot("PROLOGUL",
                    "Nemurirea s-a scurs... Lumea s-a schimbat; o simți în adierea rece a magiei ce nu mai aduce viață, " +
                    "ci înlănțuie sufletele într-o nemurire fără tihnă.",
                    41.5, 8.0, 38.5, 9.4, 1.00f, 1.10f, 560),
            new Shot("PROLOGUL",
                    "Într-un tărâm unde descântecul s-a împletit fatal cu blestemul, tu ești Eron, ultimul păstrător " +
                    "al flăcării de demult. Un războinic vrăjitor prin ale cărui vene curge puterea timpurilor uitate.",
                    22.0, 11.0, 42.0, 11.0, 1.08f, 1.16f, 640),
            new Shot("PROLOGUL",
                    "Scopul tău este unul singur: să pătrunzi în inima întunericului și să dezlegi blestemul " +
                    "Regelui Vrăjitor, cel care a furat Inelul Eternității pentru a rescrie realitatea după voia sa perfidă.",
                    47.0, 12.0, 47.0, 25.0, 1.12f, 1.18f, 660),
            new Shot("ELDORIA - ECOUL SPERANȚEI",
                    "Te trezești în satul Eldoria, un ultim refugiu unde timpul pare să fi încremenit sub o mantie de ceață. " +
                    "Bătrânul Cronicar te așteaptă. Vorbește cu el (E) pentru a-ți primi prima misiune.",
                    20.0, 11.6, 13.6, 11.0, 1.26f, 1.14f, 520),
    };

    private enum Mode { INTRO, SHOT, CUT_OUT, CUT_IN, OUTRO, DONE }

    private static final int INTRO_TICKS = 70;
    private static final int CUT_OUT_TICKS = 12;
    private static final int CUT_IN_TICKS = 18;
    private static final int OUTRO_TICKS = 55;

    private Mode mode = Mode.INTRO;
    private int modeTicks = 0;     // de câte ticks e în modul curent
    private int shotIndex = 0;
    private int shotAge = 0;       // ticks de la începutul cadrului (pentru mișcarea camerei)
    private int totalTicks = 0;    // pentru legănarea camerei, ceață, personaje
    private float charProgress = 0f;
    private int charIndex = 0;
    private int blipCounter = 0;
    private float zoom = 1f;

    // Pre-desenate o singură dată (nu se creează nimic nou la fiecare cadru)
    private final BufferedImage cloud = makeCloud();
    private final BufferedImage vignette = makeVignette();

    public CutsceneUI(GamePanel gp) {
        this.gp = gp;
    }

    // =====================================================================================
    // CONTROL (apelat de CutsceneState)
    // =====================================================================================

    public void reset() {
        mode = Mode.INTRO;
        modeTicks = 0;
        totalTicks = 0;
        shotIndex = 0;
        resetShot();
    }

    private void resetShot() {
        shotAge = 0;
        charIndex = 0;
        charProgress = 0f;
        blipCounter = 0;
    }

    public boolean isWriting() {
        return charIndex < SHOTS[shotIndex].text().length();
    }

    public void skipTypewriter() {
        charIndex = SHOTS[shotIndex].text().length();
    }

    /** ENTER după ce textul e afișat: trece la cadrul următor sau încheie prologul. */
    public void next() {
        if (mode != Mode.SHOT) return;
        if (shotIndex >= SHOTS.length - 1) {
            beginOutro();
        } else {
            mode = Mode.CUT_OUT;
            modeTicks = 0;
            if (GameFeel.SOUND_FEEDBACK) SoundManager.getInstance().playSE("click");
        }
    }

    /** Încheie prologul: benzile se retrag și ecranul se întunecă lin. */
    public void beginOutro() {
        if (mode == Mode.OUTRO || mode == Mode.DONE) return;
        mode = Mode.OUTRO;
        modeTicks = 0;
    }

    public boolean isFinished() {
        return mode == Mode.DONE;
    }

    // =====================================================================================
    // UPDATE (un tick)
    // =====================================================================================

    public void update(boolean speedUp) {
        if (mode == Mode.DONE) return;
        totalTicks++;
        modeTicks++;
        Shot shot = SHOTS[shotIndex];

        // --- schimbări de mod ---
        switch (mode) {
            case INTRO -> { if (modeTicks >= INTRO_TICKS) { mode = Mode.SHOT; modeTicks = 0; } }
            case CUT_OUT -> {
                if (modeTicks >= CUT_OUT_TICKS) {
                    shotIndex++;
                    shot = SHOTS[shotIndex];
                    resetShot();
                    mode = Mode.CUT_IN;
                    modeTicks = 0;
                }
            }
            case CUT_IN -> { if (modeTicks >= CUT_IN_TICKS) { mode = Mode.SHOT; modeTicks = 0; } }
            case OUTRO -> { if (modeTicks >= OUTRO_TICKS) { mode = Mode.DONE; modeTicks = 0; return; } }
            default -> { }
        }

        // --- camera: se mișcă tot timpul, cu easing, și apoi se oprește ușor la capăt ---
        shotAge++;
        float u = Easing.inOutCubic(shotAge / (float) shot.moveTicks());
        double cx = shot.fromX() + (shot.toX() - shot.fromX()) * u;
        double cy = shot.fromY() + (shot.toY() - shot.fromY()) * u;
        // mic tremur de cameră ținută în mână, ca imaginea să nu fie "pe șine"
        cx += Math.sin(totalTicks * 0.021) * 0.05;
        cy += Math.cos(totalTicks * 0.017) * 0.04;
        zoom = Easing.lerp(shot.zoomFrom(), shot.zoomTo(), u);
        gp.camera.setCinematicCenter(cx * gp.tileSize, cy * gp.tileSize);

        // --- personajele: Eron respiră (din Player.draw) și își întoarce capul din când în când ---
        gp.player.moveVelocity = 0f;
        if (shotIndex == SHOTS.length - 1) {
            String[] looks = {"down", "down", "right", "down", "left", "down"};
            gp.player.direction = looks[(totalTicks / 130) % looks.length];
        } else {
            gp.player.direction = "down";
        }

        // --- textul apare literă cu literă; SPACE ținut îl accelerează ---
        boolean typing = mode == Mode.SHOT || (mode == Mode.INTRO && modeTicks > 36) || (mode == Mode.CUT_IN && modeTicks > 6);
        if (typing && isWriting()) {
            float perTick = (speedUp ? GameFeel.CUTSCENE_SPEEDUP_FACTOR : 1f) / GameFeel.CUTSCENE_TEXT_TICKS_PER_CHAR;
            charProgress += perTick;
            while (charProgress >= 1f && isWriting()) {
                charProgress -= 1f;
                char c = shot.text().charAt(charIndex);
                charIndex++;
                if (!Character.isWhitespace(c) && ++blipCounter % GameFeel.TYPEWRITER_BLIP_EVERY_CHARS == 0) {
                    if (GameFeel.SOUND_FEEDBACK) SoundManager.getInstance().playSE("blip");
                }
            }
        }
    }

    // =====================================================================================
    // DESENARE
    // =====================================================================================

    public void draw(Graphics2D g2) {
        int W = gp.screenWidth, H = gp.screenHeight;
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, W, H);

        // 1. Lumea reală, cu zoom în jurul centrului ecranului
        AffineTransform saved = g2.getTransform();
        g2.translate(W / 2.0, H / 2.0);
        g2.scale(zoom, zoom);
        g2.translate(-W / 2.0, -H / 2.0);
        gp.tileM.draw(g2);
        for (var n : gp.npc) if (n != null) n.draw(g2);
        gp.player.draw(g2);
        drawTalkBubble(g2);
        gp.fx.drawWorld(g2);
        g2.setTransform(saved);

        // 2. Licurici + ceață pe straturi, care se mișcă diferit față de cameră (paralaxă)
        gp.fx.drawAmbient(g2);
        drawMist(g2);

        // 3. Margini întunecate
        Object oldHint = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(vignette, 0, 0, W, H, null);
        if (oldHint != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);

        // 4. Benzile cinema + subtitrarea
        int bar = barHeight();
        drawSubtitle(g2, bar);
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, W, bar);
        g2.fillRect(0, H - bar, W, bar);
        drawHints(g2, bar);

        // 5. Fade-ul spre/dinspre negru
        float black = blackAlpha();
        if (black > 0.004f) {
            g2.setColor(new Color(0, 0, 0, Math.min(255, Math.round(black * 255))));
            g2.fillRect(0, 0, W, H);
        }
    }

    /** În ultimul cadru Cronicarul "vorbește": trei puncte sar deasupra lui cât timp se scrie textul. */
    private void drawTalkBubble(Graphics2D g2) {
        if (shotIndex != SHOTS.length - 1 || gp.npc[0] == null || !isWriting()) return;
        int cx = gp.camera.screenX(gp.npc[0].worldX) + gp.tileSize / 2;
        int top = gp.camera.screenY(gp.npc[0].worldY) - 12;
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillRoundRect(cx - 24, top - 22, 48, 22, 14, 14);
        g2.fillPolygon(new int[]{cx - 5, cx + 5, cx}, new int[]{top - 1, top - 1, top + 7}, 3);
        g2.setColor(new Color(40, 40, 50));
        for (int i = 0; i < 3; i++) {
            int hop = Math.round((float) Math.abs(Math.sin(totalTicks * 0.2 - i * 0.7)) * 5);
            g2.fillOval(cx - 14 + i * 12, top - 14 - hop, 6, 6);
        }
    }

    /** Cât de departe au ajuns benzile (pixeli): intră la început, ies la sfârșit. */
    private int barHeight() {
        float open = switch (mode) {
            case INTRO -> Easing.outCubic(modeTicks / (float) GameFeel.LETTERBOX_SLIDE_TICKS);
            case OUTRO -> 1f - Easing.inOutCubic(modeTicks / (float) (OUTRO_TICKS - 6));
            case DONE -> 0f;
            default -> 1f;
        };
        return Math.round(GameFeel.LETTERBOX_HEIGHT * open);
    }

    private float blackAlpha() {
        return switch (mode) {
            case INTRO -> 1f - Easing.inOutCubic(modeTicks / 62f); // din negru, lent și lin
            case CUT_OUT -> Easing.inCubic(modeTicks / (float) CUT_OUT_TICKS);
            case CUT_IN -> 1f - Easing.outCubic(modeTicks / (float) CUT_IN_TICKS);
            case OUTRO -> Easing.inCubic(Math.max(0f, modeTicks - 8) / (float) (OUTRO_TICKS - 8));
            case DONE -> 1f;
            default -> 0f;
        };
    }

    private void drawSubtitle(Graphics2D g2, int bar) {
        if (bar < 20) return;
        Shot shot = SHOTS[shotIndex];
        int W = gp.screenWidth, H = gp.screenHeight;
        int bottom = H - bar - 36;

        // Fundal întunecat în degrade, ca textul să se citească pe orice decor
        g2.setPaint(new GradientPaint(0, bottom - 260, new Color(0, 0, 0, 0), 0, bottom + 36, new Color(0, 0, 0, 200)));
        g2.fillRect(0, bottom - 260, W, 296 + 1);

        Font body = g2.getFont().deriveFont(Font.PLAIN, 28F);
        g2.setFont(body);
        FontMetrics fm = g2.getFontMetrics();
        List<String> lines = wrap(shot.text(), fm, 1120);

        // Titlul cadrului
        g2.setFont(body.deriveFont(Font.BOLD, 26F));
        g2.setColor(new Color(212, 175, 55));
        String title = shot.title();
        g2.drawString(title, W / 2 - g2.getFontMetrics().stringWidth(title) / 2, bottom - lines.size() * 42 - 18);

        // Textul, afișat până la litera curentă
        g2.setFont(body);
        int y = bottom - (lines.size() - 1) * 42;
        int remaining = charIndex;
        for (String line : lines) {
            if (remaining <= 0) break;
            String part = line.substring(0, Math.min(line.length(), remaining));
            remaining -= line.length() + 1;
            int x = W / 2 - fm.stringWidth(line) / 2; // centrat pe rândul întreg, ca să nu "alerge" textul
            g2.setColor(Color.BLACK);
            g2.drawString(part, x + 2, y + 2);
            g2.setColor(Color.WHITE);
            g2.drawString(part, x, y);
            y += 42;
        }
    }

    private void drawHints(Graphics2D g2, int bar) {
        if (bar < 24) return;
        int W = gp.screenWidth, H = gp.screenHeight;
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 15F));

        // în banda de sus: cum se sare peste prolog și la ce pagină ești
        g2.setColor(new Color(150, 150, 150));
        g2.drawString("ESC - sari peste prolog      SPACE (ține apăsat) - mai repede", 40, bar / 2 + 5);
        String page = (shotIndex + 1) + " / " + SHOTS.length;
        g2.drawString(page, W - 40 - g2.getFontMetrics().stringWidth(page), bar / 2 + 5);

        // în banda de jos: indicația de continuare pulsează când tot textul e afișat
        if (mode == Mode.SHOT && !isWriting()) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(totalTicks * 0.14);
            g2.setColor(new Color(230, 230, 230, 120 + Math.round(110 * pulse)));
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18F));
            String hint = shotIndex >= SHOTS.length - 1 ? "ENTER - începe aventura  ▸" : "ENTER - continuă  ▸";
            g2.drawString(hint, W - 60 - g2.getFontMetrics().stringWidth(hint), H - bar / 2 + 6);
        }
    }

    // Ceață: nori mari și moi, pe trei straturi care se mișcă cu viteze diferite față de cameră
    private static final double[] MIST_DEPTH = {0.30, 0.55, 0.85};
    private static final float[] MIST_ALPHA = {0.16f, 0.22f, 0.26f};
    private static final float[] MIST_SCALE = {0.8f, 1.15f, 1.6f};
    private static final double[] MIST_DRIFT = {0.25, 0.45, 0.8};

    private void drawMist(Graphics2D g2) {
        int W = gp.screenWidth, H = gp.screenHeight;
        Composite old = g2.getComposite();
        int camX = gp.camera.getX(), camY = gp.camera.getY();
        for (int layer = 0; layer < MIST_DEPTH.length; layer++) {
            int cw = Math.round(cloud.getWidth() * MIST_SCALE[layer]);
            int ch = Math.round(cloud.getHeight() * MIST_SCALE[layer]);
            int span = W + cw;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, MIST_ALPHA[layer]));
            for (int k = 0; k < 4; k++) {
                double base = k * (span / 4.0) + layer * 137;
                int x = (int) Math.floorMod(Math.round(base - camX * MIST_DEPTH[layer] + totalTicks * MIST_DRIFT[layer]), (long) span) - cw;
                int y = Math.round(H * (0.18f + 0.2f * layer + 0.09f * k) - camY * (float) MIST_DEPTH[layer] * 0.4f
                        + (float) Math.sin(totalTicks * 0.01 + k * 1.7 + layer) * 14f);
                g2.drawImage(cloud, x, y - ch / 2, cw, ch, null);
            }
        }
        g2.setComposite(old);
    }

    private static List<String> wrap(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    // =====================================================================================
    // Imagini generate o singură dată
    // =====================================================================================

    /** Un nor moale: elipsă cu margini care se pierd. */
    private static BufferedImage makeCloud() {
        int w = 640, h = 280;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // mai multe "puf-uri" suprapuse dau un contur neregulat, nu o elipsă perfectă
        double[][] puffs = {{0.50, 0.55, 0.50}, {0.30, 0.60, 0.34}, {0.70, 0.58, 0.36}, {0.42, 0.40, 0.28}, {0.62, 0.42, 0.26}};
        for (double[] p : puffs) {
            float r = (float) (p[2] * w * 0.5);
            float cx = (float) (p[0] * w), cy = (float) (p[1] * h);
            g.setPaint(new RadialGradientPaint(new Point((int) cx, (int) cy), r,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{new Color(222, 230, 245, 150), new Color(210, 220, 240, 70), new Color(200, 210, 235, 0)}));
            g.fillOval((int) (cx - r), (int) (cy - r * 0.62f), (int) (2 * r), (int) (2 * r * 0.62f));
        }
        g.dispose();
        return img;
    }

    /** Margini întunecate: transparent la centru, tot mai opac spre colțuri (mică și întinsă la desenare). */
    private static BufferedImage makeVignette() {
        int w = 400, h = 225;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new RadialGradientPaint(new Point(w / 2, h / 2), w * 0.62f,
                new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(0, 0, 8, 0), new Color(0, 0, 8, 40), new Color(0, 0, 8, 215)}));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }
}
