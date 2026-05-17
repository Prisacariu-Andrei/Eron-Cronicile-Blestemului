package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class InventoryState implements GameState {
    private final GamePanel gp;

    public InventoryState(GamePanel gp) {
        this.gp = gp;
    }

    @Override public void enter() {}

    @Override
    public void update() {
        gp.gameTimer.update();
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.tileM.draw(g2);
        gp.player.draw(g2);
        gp.ui.draw(g2);
    }

    @Override public void exit() {}
}