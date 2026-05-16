package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class DeathState implements GameState {
    private final GamePanel gp;

    public DeathState(GamePanel gp) {
        this.gp = gp;
    }

    @Override public void enter() {}

    @Override
    public void update() {
        if (gp.keyH.enterPressed) {
            gp.keyH.enterPressed = false;
            gp.player.resetPlayer();
            gp.changeState(new MenuState(gp));
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.ui.draw(g2);
    }

    @Override public void exit() {}
}