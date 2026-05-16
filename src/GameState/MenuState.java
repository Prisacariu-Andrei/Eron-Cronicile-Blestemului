package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class MenuState implements GameState {
    private final GamePanel gp;

    public MenuState(GamePanel gp) {
        this.gp = gp;
    }

    @Override public void enter() {}

    @Override
    public void update() {
        if (gp.keyH.upPressed) {
            gp.currentMenuOption = (gp.currentMenuOption <= 0) ? 4 : gp.currentMenuOption - 1; // Ajustat pt Top Scoruri
            gp.keyH.upPressed = false;
        }
        if (gp.keyH.downPressed) {
            gp.currentMenuOption = (gp.currentMenuOption >= 4) ? 0 : gp.currentMenuOption + 1;
            gp.keyH.downPressed = false;
        }
        if (gp.keyH.enterPressed) {
            gp.keyH.enterPressed = false;
            if (gp.currentMenuOption == 0) gp.changeState(new PlayState(gp));
            if (gp.currentMenuOption == 2) gp.changeState(new LeaderboardState(gp));
            if (gp.currentMenuOption == 4) System.exit(0);
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.ui.draw(g2); // Trimitem desenul către clasa ta de UI ca și până acum
    }

    @Override public void exit() {}
}