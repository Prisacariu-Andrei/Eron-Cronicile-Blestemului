package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class DialogueState implements GameState {
    private final GamePanel gp;

    public DialogueState(GamePanel gp) {
        this.gp = gp;
    }

    @Override public void enter() { gp.dialogueUI.onOpen(); }

    @Override
    public void update() {
        gp.dialogueUI.update(); // literele apar pe ticks fixe
        gp.gameTimer.update();
        if (gp.keyH.enterPressed) {
            gp.keyH.enterPressed = false;
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.tileM.draw(g2);
        for (var n : gp.npc) if (n != null) n.draw(g2);
        gp.player.draw(g2);
        gp.fx.drawWorld(g2);
        gp.fx.drawAmbient(g2);
        gp.ui.draw(g2);
        for (var e : gp.enemy) if (e != null) e.draw(g2);

    }

    @Override public void exit() {}
}