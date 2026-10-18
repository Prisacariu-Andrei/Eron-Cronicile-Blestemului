package GameState;

import Main.GamePanel;
import ProgressManagement.StoryScenes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.function.Supplier;

/**
 * O scenă a poveștii (prologul, strângerea de mână, epilogul...) din story.json. Aspectul vine din CutsceneUI.
 * ENTER sau SPACE trece la următoarea replică, SPACE ținut apăsat accelerează textul,
 * iar ESC ținut apăsat o secundă (cu un inel de progres) sare peste toată scena, aplicându-i "onEnd".
 */
public class CutsceneState implements GameState {
    private static final int SKIP_HOLD_TICKS = 60;

    private final GamePanel gp;
    private final StoryScenes.Scene scene;
    private final Supplier<GameState> then;         // starea de după scenă (null = jocul)
    private boolean prevDash;
    private int escTicks = 0;

    /** O scenă din story.json; după ea urmează "then" sau jocul (și acțiunile "onEnd" ale scenei). */
    public CutsceneState(GamePanel gp, StoryScenes.Scene scene, Supplier<GameState> then) {
        this.gp = gp;
        this.scene = scene;
        this.then = then;
    }

    @Override
    public void enter() {
        // ENTER-ul/SPACE-ul care a pornit scena (ex. din InputNameState) nu trebuie să sară prima replică
        gp.keyH.enterPressed = false;
        prevDash = true;
        escTicks = 0;
        if (scene.mapZone != null) gp.eHandler.applyMap("/Maps/" + scene.mapZone, scene.mapZone, scene.mapCol, scene.mapRow);   // ex. epilogul: în sat
        gp.camera.beginCinematic();
        gp.cutsceneUI.loadScene(scene);
        gp.cutsceneUI.reset();
    }

    @Override
    public void update() {
        // SPACE ținut apăsat (keyH.dashPressed) = text mai rapid; o apăsare nouă de SPACE sau ENTER = mai departe
        boolean dash = gp.keyH.dashPressed;
        boolean advance = gp.keyH.enterPressed || (dash && !prevDash);
        prevDash = dash;
        gp.cutsceneUI.update(dash);

        if (advance) {
            gp.keyH.enterPressed = false;
            if (gp.cutsceneUI.isWriting()) {
                gp.cutsceneUI.skipTypewriter();
            } else {
                gp.cutsceneUI.next();
            }
        }

        // ESC ținut o secundă: sare toată scena
        if (gp.keyH.escHeld) {
            if (escTicks >= 0 && ++escTicks >= SKIP_HOLD_TICKS) { skip(); escTicks = -1; }
        } else if (escTicks > 0) {
            escTicks = Math.max(0, escTicks - 4);   // inelul se golește singur dacă dai drumul tastei
        }

        if (gp.cutsceneUI.isFinished()) {
            if (scene.onEnd.contains("credits")) {   // după epilog: genericul
                gp.creditsUI.resetPosition();
                gp.changeState(new CreditsState(gp));
            } else {
                gp.changeState(gp.storyScenes.after(then));
                if (then == null) gp.storyScenes.runOnEnd(scene);   // ex. lupta de tutorial după prolog
            }
        }
    }

    /** Sare peste scenă, dar cu aceeași ieșire lină (benzile se retrag, ecranul se întunecă). */
    public void skip() {
        gp.cutsceneUI.beginOutro();
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.cutsceneUI.draw(g2);
        if (escTicks > 0) drawSkipRing(g2);
    }

    /** Inelul de progres din colțul de sus: se umple cât ții ESC apăsat. */
    private void drawSkipRing(Graphics2D g2) {
        int r = 20, cx = gp.screenWidth - 130, cy = 56;
        float p = Math.min(1f, escTicks / (float) SKIP_HOLD_TICKS);
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillOval(cx - r - 6, cy - r - 6, 2 * r + 12, 2 * r + 12);
        g2.setStroke(new BasicStroke(5f));
        g2.setColor(new Color(90, 90, 100));
        g2.drawOval(cx - r, cy - r, 2 * r, 2 * r);
        g2.setColor(new Color(212, 175, 55));
        g2.drawArc(cx - r, cy - r, 2 * r, 2 * r, 90, -Math.round(360 * p));
        g2.setStroke(new BasicStroke(1f));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13F));
        String t = "ESC";
        g2.setColor(Color.WHITE);
        g2.drawString(t, cx - g2.getFontMetrics().stringWidth(t) / 2, cy + 5);
    }

    @Override
    public void exit() {
        // Nimic din cutscene nu rămâne activ după ea: camera revine la jucător, particulele dispar
        gp.camera.endCinematic();
        gp.fx.clearAll();
        gp.player.direction = "down";
        gp.player.moveVelocity = 0f;
    }
}
