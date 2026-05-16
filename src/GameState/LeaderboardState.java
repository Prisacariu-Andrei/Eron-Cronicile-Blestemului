package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class LeaderboardState implements GameState {
    private final GamePanel gp;

    public LeaderboardState(GamePanel gp) {
        this.gp = gp;
    }

    @Override public void enter() {}

    @Override
    public void update() {
        if (gp.keyH.escPressed) {
            gp.keyH.escPressed = false;
            gp.changeState(new MenuState(gp));
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        // Apelăm direct clasa modulară dedicată pentru desenarea clasamentului ★★★
        gp.leaderboardUI.draw(g2);
    }

    @Override public void exit() {}
}