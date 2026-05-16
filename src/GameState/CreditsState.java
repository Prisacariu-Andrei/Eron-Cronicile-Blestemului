package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class CreditsState implements GameState {
    private final GamePanel gp;

    public CreditsState(GamePanel gp) {
        this.gp = gp;
    }

    @Override
    public void enter() {
        // Opțional: Poți opri muzica de fundal aici și porni o melodie de final
        // gp.playMusic(creditsMusicIndex);
    }

    @Override
    public void update() {
        // Actualizează mișcarea textului în sus (dacă ai implementat scroll în CreditsUI)
        gp.creditsUI.update();

        // Dacă apasă ESC, se întoarce la meniul principal
        if (gp.keyH.escPressed || gp.keyH.enterPressed) {
            gp.keyH.escPressed = false;
            gp.keyH.enterPressed = false;
            gp.changeState(new MenuState(gp));
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.creditsUI.draw(g2); // Trimite randarea către clasa grafică dedicată
    }

    @Override public void exit() {}
}