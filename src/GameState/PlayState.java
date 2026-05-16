package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class PlayState implements GameState {
    private final GamePanel gp;

    public PlayState(GamePanel gp) {
        this.gp = gp;
    }

    @Override public void enter() {}

    @Override
    public void update() {
        gp.gameTimer.update();
        gp.player.update();
        gp.eHandler.checkEvent();

        for (int i = 0; i < gp.enemy.length; i++) {
            if (gp.enemy[i] != null) {
                if (gp.enemy[i].alive) gp.enemy[i].update();
                else gp.enemy[i] = null;
            }
        }

        if (gp.keyH.escPressed) {
            gp.keyH.escPressed = false;
           // gp.changeState(new PauseState(gp)); // Trecem direct în starea de Pauză
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.tileM.draw(g2);
        for (var n : gp.npc) if (n != null) n.draw(g2);
        for (var e : gp.enemy) if (e != null) e.draw(g2);
        gp.player.draw(g2);
        gp.ui.draw(g2);
    }

    @Override public void exit() {}
}