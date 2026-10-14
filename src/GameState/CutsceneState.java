package GameState;

import Main.GamePanel;
import ProgressManagement.StoryScenes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.function.Supplier;

/**
 * O scenă a poveștii (prologul, strângerea de mână, ...) din story.json, sau epilogul de la sfârșitul jocului.
 * Aspectul vine din CutsceneUI. ENTER sau SPACE trece la următoarea replică, SPACE ținut apăsat accelerează textul, ESC sare scena.
 */
public class CutsceneState implements GameState {
    private final GamePanel gp;

    private final Observer.CutsceneUI.Script script;
    private final StoryScenes.Scene scene;          // null pentru epilog
    private final Supplier<GameState> then;         // starea de după scenă (null = jocul)
    private boolean prevDash;

    /** Epilogul. */
    public CutsceneState(GamePanel gp, Observer.CutsceneUI.Script script) {
        this.gp = gp;
        this.script = script;
        this.scene = null;
        this.then = null;
    }

    /** O scenă din story.json; după ea urmează "then" sau jocul (și acțiunile "onEnd" ale scenei). */
    public CutsceneState(GamePanel gp, StoryScenes.Scene scene, Supplier<GameState> then) {
        this.gp = gp;
        this.script = Observer.CutsceneUI.Script.SCENE;
        this.scene = scene;
        this.then = then;
    }

    @Override
    public void enter() {
        // ENTER-ul/SPACE-ul care a pornit scena (ex. din InputNameState) nu trebuie să sară prima replică
        gp.keyH.enterPressed = false;
        prevDash = true;
        if (script == Observer.CutsceneUI.Script.EPILOGUE) {   // epilogul se joacă peste Eldoria, cu Eron în sat
            gp.eHandler.applyMap("/Maps/Eldoria", "Eldoria", 15, 12);
        }
        gp.camera.beginCinematic();
        if (scene != null) gp.cutsceneUI.loadScene(scene); else gp.cutsceneUI.load(script);
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

        if (gp.cutsceneUI.isFinished()) {
            if (script == Observer.CutsceneUI.Script.EPILOGUE) {   // după epilog: genericul
                gp.creditsUI.resetPosition();
                gp.changeState(new CreditsState(gp));
            } else {
                gp.changeState(gp.storyScenes.after(then));
                if (then == null) gp.storyScenes.runOnEnd(scene);   // ex. lupta de tutorial după prolog
            }
        }
    }

    /** ESC: sare peste scenă, dar cu aceeași ieșire lină (benzile se retrag, ecranul se întunecă). */
    public void skip() {
        gp.cutsceneUI.beginOutro();
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.cutsceneUI.draw(g2);
        if (script == Observer.CutsceneUI.Script.EPILOGUE) {   // lumina răsăritului crește de la un cadru la altul
            int a = Math.min(70, 18 + gp.cutsceneUI.shotIndex() * 16);
            g2.setColor(new Color(255, 196, 120, a));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }
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
