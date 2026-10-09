package GameState;

import Main.GamePanel;
import java.awt.Color;
import java.awt.Graphics2D;

/** Prologul: vezi CutsceneUI. Se termină singur în PlayState, la Eldoria. */
public class CutsceneState implements GameState {
    private final GamePanel gp;

    private final Observer.CutsceneUI.Script script;

    public CutsceneState(GamePanel gp) {
        this(gp, Observer.CutsceneUI.Script.PROLOGUE);
    }

    public CutsceneState(GamePanel gp, Observer.CutsceneUI.Script script) {
        this.gp = gp;
        this.script = script;
    }

    @Override
    public void enter() {
        // ENTER-ul din InputNameState e încă apăsat; îl consumăm ca să nu sară prima pagină
        gp.keyH.enterPressed = false;
        if (script == Observer.CutsceneUI.Script.EPILOGUE) {   // epilogul se joacă peste Eldoria, cu Eron în sat
            gp.eHandler.applyMap("/Maps/Eldoria", "Eldoria", 15, 12);
        }
        gp.camera.beginCinematic();
        gp.cutsceneUI.load(script);
        gp.cutsceneUI.reset();
    }

    @Override
    public void update() {
        // SPACE ținut apăsat (keyH.dashPressed) = text mai rapid
        gp.cutsceneUI.update(gp.keyH.dashPressed);

        if (gp.keyH.enterPressed) {
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
                gp.changeState(new PlayState(gp));
            }
        }
    }

    /** ESC: sare peste prolog, dar cu aceeași ieșire lină (benzile se retrag, ecranul se întunecă). */
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
