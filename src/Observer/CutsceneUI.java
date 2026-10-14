package Observer;

import Main.Easing;
import Main.GameFeel;
import Main.GamePanel;
import Main.SoundManager;
import ProgressManagement.StoryScenes;

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
                        float zoomFrom, float zoomTo, int moveTicks, StoryScenes.Line line) {
        Shot(String title, String text, double fromX, double fromY, double toX, double toY, float zoomFrom, float zoomTo, int moveTicks) {
            this(title, text, fromX, fromY, toX, toY, zoomFrom, zoomTo, moveTicks, null);
        }
    }

    /** Ce se joacă: epilogul (după Malakor, cadre fixe în cod) sau o scenă din story.json (prologul și celelalte). */
    public enum Script { SCENE, EPILOGUE }

    private Script script = Script.EPILOGUE;
    private Shot[] shots = EPILOGUE_SHOTS;
    private StoryScenes.Scene scene;

    public void load(Script s) {
        script = s;
        scene = null;
        shots = EPILOGUE_SHOTS;
    }

    /** O scenă din story.json: fiecare replică devine un cadru (titlu = cine vorbește, text, cameră, mutări, efecte). */
    public void loadScene(StoryScenes.Scene sc) {
        script = Script.SCENE;
        scene = sc;
        actors.clear();
        for (StoryScenes.Actor a : sc.actors) actors.add(new ActorState(a));
        int ts = gp.tileSize;
        if (sc.eron != null && sc.eron.length >= 2) {
            gp.player.worldX = (int) Math.round(sc.eron[0] * ts);
            gp.player.worldY = (int) Math.round(sc.eron[1] * ts);
        }
        gp.player.direction = "down";
        double cx = sc.cam != null && sc.cam.length >= 2 ? sc.cam[0] : gp.player.worldX / (double) ts + 0.5;
        double cy = sc.cam != null && sc.cam.length >= 2 ? sc.cam[1] : gp.player.worldY / (double) ts + 0.5;
        float zoom = 1.15f;
        List<Shot> list = new ArrayList<>();
        for (StoryScenes.Line l : sc.lines) {
            double[] c = l.cam();
            double fx = cx, fy = cy, tx = cx, ty = cy;
            float zf = zoom, zt = zoom + 0.03f;
            int ticks = 700;
            if (c != null && c.length == 2) { fx = c[0]; fy = c[1]; tx = fx; ty = fy + 0.15; }
            else if (c != null && c.length >= 4) {
                fx = c[0]; fy = c[1]; tx = c[2]; ty = c[3];
                if (c.length >= 6) { zf = (float) c[4]; zt = (float) c[5]; }
                if (c.length >= 7) ticks = (int) c[6];
            }
            cx = tx; cy = ty; zoom = zt;
            String title = l.speaker().isEmpty() ? sc.title : l.speaker();
            list.add(new Shot(title, l.text(), fx, fy, tx, ty, zf, zt, Math.max(1, ticks), l));
        }
        shots = list.toArray(new Shot[0]);
        tone = 0;
        toneTarget = new Color(0, 0, 0, 0);
        flash = 0f;
        shakeTicks = 0;
    }

    public StoryScenes.Scene scene() { return scene; }

    /** Personaj care apare doar în scenă. "eron" nu e aici: acela mută jucătorul real. */
    private static class ActorState {
        final String id;
        final BufferedImage img;
        double col, row, fromCol, fromRow, toCol, toRow;
        boolean visible;
        int startTick, moveTicks;

        ActorState(StoryScenes.Actor a) {
            id = a.id();
            BufferedImage im = null;
            try (var in = CutsceneUI.class.getResourceAsStream(a.sprite())) {
                if (in != null) {
                    im = javax.imageio.ImageIO.read(in);
                    Color t = Entity.NPC.DataNPC.parseColor(a.tint());
                    if (t != null) im = Entity.NPC.DataNPC.tinted(im, t);
                }
            } catch (Exception e) {
                Main.Exception.GameExceptionHandler.getInstance().handleWarning("Nu s-a putut încărca sprite-ul actorului " + a.id() + ": " + a.sprite(), e);
            }
            img = im;
            col = fromCol = toCol = a.col();
            row = fromRow = toRow = a.row();
            visible = a.visible();
        }
    }

    private final List<ActorState> actors = new ArrayList<>();
    private int tone;                      // cât din tonul de culoare (zi/amurg/noapte/zori) e aplicat, 0..TONE_TICKS
    private Color toneTarget = new Color(0, 0, 0, 0);
    private float flash;                   // 1 = ecran alb, scade singur
    private int shakeTicks;
    private static final int TONE_TICKS = 90;

    private ActorState actor(String id) {
        for (ActorState a : actors) if (a.id.equals(id)) return a;
        return null;
    }

    /** Începutul unui cadru: efectele și mutările replicii. */
    private void beginLine() {
        StoryScenes.Line line = shots[shotIndex].line();
        if (line == null) return;
        for (String fx : line.effects()) {
            String[] p = fx.split(":");
            switch (p[0]) {
                case "flash" -> flash = 1f;
                case "shake" -> shakeTicks = 70;
                case "tone" -> {
                    toneTarget = switch (p.length > 1 ? p[1] : "day") {
                        case "dusk" -> new Color(255, 120, 50, 70);
                        case "night" -> new Color(8, 14, 60, 120);
                        case "dawn" -> new Color(255, 190, 120, 60);
                        default -> new Color(0, 0, 0, 0);
                    };
                    tone = 0;
                }
                case "sound" -> { if (p.length > 1 && GameFeel.SOUND_FEEDBACK) SoundManager.getInstance().playSE(p[1]); }
                case "burst" -> {
                    if (p.length >= 4) {
                        try {
                            gp.fx.pickupBurst((float) ((Double.parseDouble(p[1]) + 0.5) * gp.tileSize), (float) (Double.parseDouble(p[2]) * gp.tileSize), Integer.parseInt(p[3], 16));
                        } catch (NumberFormatException ignored) { }
                    }
                }
                default -> { }
            }
        }
        for (StoryScenes.Move m : line.moves()) {
            if ("eron".equals(m.actor())) {
                if (m.dir() != null) gp.player.direction = m.dir();
                eronFromCol = gp.player.worldX / (double) gp.tileSize;
                eronFromRow = gp.player.worldY / (double) gp.tileSize;
                eronToCol = m.col();
                eronToRow = m.row();
                eronStart = totalTicks;
                eronTicks = m.ticks();
                continue;
            }
            ActorState a = actor(m.actor());
            if (a == null) continue;
            if (m.show() != null) a.visible = m.show();
            a.fromCol = a.col;
            a.fromRow = a.row;
            a.toCol = m.col();
            a.toRow = m.row();
            a.startTick = totalTicks;
            a.moveTicks = m.ticks();
        }
    }

    private double eronFromCol, eronFromRow, eronToCol, eronToRow;
    private int eronStart, eronTicks;

    private void moveActors() {
        for (ActorState a : actors) {
            if (a.moveTicks <= 0) continue;
            float u = Math.min(1f, (totalTicks - a.startTick) / (float) a.moveTicks);
            a.col = a.fromCol + (a.toCol - a.fromCol) * u;
            a.row = a.fromRow + (a.toRow - a.fromRow) * u;
            if (u >= 1f) a.moveTicks = 0;
        }
        if (eronTicks > 0) {
            float u = Math.min(1f, (totalTicks - eronStart) / (float) eronTicks);
            gp.player.worldX = (int) Math.round((eronFromCol + (eronToCol - eronFromCol) * u) * gp.tileSize);
            gp.player.worldY = (int) Math.round((eronFromRow + (eronToRow - eronFromRow) * u) * gp.tileSize);
            if (u >= 1f) eronTicks = 0;
        }
    }

    public int shotIndex() { return shotIndex; }

    private static final Shot[] EPILOGUE_SHOTS = {
            new Shot("EPILOGUL - RĂSĂRITUL DUPĂ VEȘNICIE",
                    "Odată cu căderea tiranului, moartea se topește ca zăpada sub soarele de primăvară. Inelul Eternității, purificat de Diamantul Nopții și de Inelul de Gheață, se frânge într-o lumină care nu mai arde.",
                    47.0, 22.0, 41.0, 14.0, 1.00f, 1.10f, 560),
            new Shot("EPILOGUL - RĂSĂRITUL DUPĂ VEȘNICIE",
                    "Te întorci în Eldoria sub un cer care, pentru prima dată după eoni, se pregătește să lase locul nopții naturale. Ceața s-a risipit de pe acoperișuri, iar satul respiră.",
                    34.0, 37.0, 22.0, 28.0, 1.10f, 1.18f, 620),
            new Shot("EPILOGUL - RĂSĂRITUL DUPĂ VEȘNICIE",
                    "Lumea a primit înapoi darul nemuririi și al odihnei. Blestemul se frânge pe veci, iar timpul începe, în sfârșit, să curgă din nou peste lume.",
                    20.0, 13.0, 15.5, 10.5, 1.20f, 1.12f, 560),
            new Shot("EPILOGUL - RĂSĂRITUL DUPĂ VEȘNICIE",
                    "Legenda lui Eron rămâne scrisă în cronicile tărâmului, ca fiind cel care a redat vieții din veșnicia de mult pierdută.",
                    14.4, 11.4, 13.8, 11.0, 1.30f, 1.24f, 520),
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
        eronTicks = 0;
        resetShot();
        beginLine();
    }

    private void resetShot() {
        shotAge = 0;
        charIndex = 0;
        charProgress = 0f;
        blipCounter = 0;
    }

    public boolean isWriting() {
        return charIndex < shots[shotIndex].text().length();
    }

    public void skipTypewriter() {
        charIndex = shots[shotIndex].text().length();
    }

    /** ENTER după ce textul e afișat: trece la cadrul următor sau încheie prologul. */
    public void next() {
        if (mode != Mode.SHOT) return;
        if (shotIndex >= shots.length - 1) {
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
        Shot shot = shots[shotIndex];

        // --- schimbări de mod ---
        switch (mode) {
            case INTRO -> { if (modeTicks >= INTRO_TICKS) { mode = Mode.SHOT; modeTicks = 0; } }
            case CUT_OUT -> {
                if (modeTicks >= CUT_OUT_TICKS) {
                    shotIndex++;
                    shot = shots[shotIndex];
                    resetShot();
                    beginLine();
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
        if (shakeTicks > 0) {   // zguduitură: scade spre zero
            float k = shakeTicks / 70f;
            cx += Math.sin(totalTicks * 1.9) * 0.16 * k;
            cy += Math.cos(totalTicks * 2.3) * 0.12 * k;
            shakeTicks--;
        }
        if (flash > 0f) flash = Math.max(0f, flash - 0.025f);
        if (tone < TONE_TICKS) tone++;
        zoom = Easing.lerp(shot.zoomFrom(), shot.zoomTo(), u);
        gp.camera.setCinematicCenter(cx * gp.tileSize, cy * gp.tileSize);

        // --- personajele: Eron respiră (din Player.draw) și își întoarce capul din când în când ---
        gp.player.moveVelocity = 0f;
        if (script == Script.SCENE) moveActors();   // direcția lui Eron o stabilesc mutările din scenă
        else gp.player.direction = "down";

        // --- textul apare literă cu literă; SPACE ținut îl accelerează ---
        boolean typing = mode == Mode.SHOT || (mode == Mode.INTRO && modeTicks > 36) || (mode == Mode.CUT_IN && modeTicks > 6);
        if (typing && isWriting()) {
            float perTick = (speedUp ? GameFeel.CUTSCENE_SPEEDUP_FACTOR : 1f) * Main.Settings.cutsceneSpeedFactor() / GameFeel.CUTSCENE_TEXT_TICKS_PER_CHAR;
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
        Entity.NPC.NPC.markersHidden = true;   // fără "!" / "?" de misiune în scene
        for (var n : gp.npc) {
            if (n == null || (scene != null && scene.hideNpcs.contains(n.name))) continue;
            n.draw(g2);
        }
        Entity.NPC.NPC.markersHidden = false;
        gp.player.draw(g2);
        drawActors(g2);
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

        // 3b. Tonul zilei (amurg, noapte, zori) și sclipirea
        if (toneTarget.getAlpha() > 0) {
            int al = Math.round(toneTarget.getAlpha() * Easing.inOutCubic(Math.min(1f, tone / (float) TONE_TICKS)));
            g2.setColor(new Color(toneTarget.getRed(), toneTarget.getGreen(), toneTarget.getBlue(), al));
            g2.fillRect(0, 0, W, H);
        }
        if (flash > 0.01f) {
            g2.setColor(new Color(255, 250, 235, Math.round(210 * flash)));
            g2.fillRect(0, 0, W, H);
        }

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

    private void drawActors(Graphics2D g2) {
        for (ActorState a : actors) {
            if (!a.visible || a.img == null) continue;
            int x = gp.camera.screenX((int) Math.round(a.col * gp.tileSize));
            int y = gp.camera.screenY((int) Math.round(a.row * gp.tileSize));
            y += a.moveTicks > 0 ? Math.round((float) Math.abs(Math.sin(totalTicks * 0.3)) * -5f)
                                 : Math.round((float) Math.sin((totalTicks + Math.abs(a.id.hashCode()) % 90) * 0.045) * Main.GameFeel.IDLE_BOB_PX);
            if (x + gp.tileSize < 0 || x > gp.screenWidth || y + gp.tileSize < 0 || y > gp.screenHeight) continue;
            int size = a.id.equals("copil") ? gp.tileSize * 3 / 4 : gp.tileSize;   // copilul e mai mic
            g2.drawImage(a.img, x, y + gp.tileSize - size, size, size, null);
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
        Shot shot = shots[shotIndex];
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
        g2.drawString(script == Script.EPILOGUE ? "ESC - sari peste epilog      SPACE (ține apăsat) - mai repede" : "ESC - sari peste scenă      ENTER / SPACE - continuă, SPACE ținut - mai repede", 40, bar / 2 + 5);
        String page = (shotIndex + 1) + " / " + shots.length;
        g2.drawString(page, W - 40 - g2.getFontMetrics().stringWidth(page), bar / 2 + 5);

        // în banda de jos: indicația de continuare pulsează când tot textul e afișat
        if (mode == Mode.SHOT && !isWriting()) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(totalTicks * 0.14);
            g2.setColor(new Color(230, 230, 230, 120 + Math.round(110 * pulse)));
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18F));
            String hint = shotIndex >= shots.length - 1 ? (script == Script.EPILOGUE ? "ENTER - sfârșit  ▸" : "ENTER - închide  ▸") : "ENTER - continuă  ▸";
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
