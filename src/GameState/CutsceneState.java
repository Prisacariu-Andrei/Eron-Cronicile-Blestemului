package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class CutsceneState implements GameState {
    private final GamePanel gp;

    public CutsceneState(GamePanel gp) {
        this.gp = gp;
    }

    @Override
    public void enter() {
        // ENTER-ul din InputNameState e încă apăsat; îl consumăm ca să nu sară prima pagină
        gp.keyH.enterPressed = false;
        gp.cutsceneUI.reset();
    }

    @Override
    public void update() {
        gp.cutsceneUI.update();

        if (gp.keyH.enterPressed) {
            gp.keyH.enterPressed = false;
            if (gp.cutsceneUI.isWriting()) {
                gp.cutsceneUI.skipTypewriter();
            } else if (!gp.cutsceneUI.nextPage()) {
                skip();
            }
        }
    }

    /** Încheie prologul și pornește jocul în Eldoria. */
    public void skip() {
        gp.changeState(new PlayState(gp));
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.cutsceneUI.draw(g2);
    }

    @Override public void exit() {}
}
