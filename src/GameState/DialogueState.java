package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class DialogueState implements GameState {
    private final GamePanel gp;

    public DialogueState(GamePanel gp) {
        this.gp = gp;
    }

    @Override public void enter() {}

    @Override
    public void update() {
        if (gp.keyH.enterPressed) {
            gp.keyH.enterPressed = false;
            // Aici pui logica ta de avansat textul NPC-ului sau ieșirea în PlayState la finalul dialogului
            gp.changeState(new PlayState(gp));
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.tileM.draw(g2);
        for (var n : gp.npc) if (n != null) n.draw(g2);
        gp.player.draw(g2);
        gp.ui.draw(g2);
        for (var e : gp.enemy) if (e != null) e.draw(g2);

    }

    @Override public void exit() {}
}