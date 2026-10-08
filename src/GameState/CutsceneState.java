package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

/** Prologul: vezi CutsceneUI. Se termină singur în PlayState, la Eldoria. */
public class CutsceneState implements GameState {
    private final GamePanel gp;

    public CutsceneState(GamePanel gp) {
        this.gp = gp;
    }

    @Override
    public void enter() {
        // ENTER-ul din InputNameState e încă apăsat; îl consumăm ca să nu sară prima pagină
        gp.keyH.enterPressed = false;
        gp.camera.beginCinematic();
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
            gp.changeState(new PlayState(gp));
        }
    }

    /** ESC: sare peste prolog, dar cu aceeași ieșire lină (benzile se retrag, ecranul se întunecă). */
    public void skip() {
        gp.cutsceneUI.beginOutro();
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.cutsceneUI.draw(g2);
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
