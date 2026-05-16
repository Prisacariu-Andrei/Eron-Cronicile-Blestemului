package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class PauseState implements GameState {
    private final GamePanel gp;

    public PauseState(GamePanel gp) {
        this.gp = gp;
    }

    @Override public void enter() { gp.pauseUI.commandNum = 0; }

    @Override
    public void update() {
        if (gp.keyH.escPressed) {
            gp.keyH.escPressed = false;
            gp.changeState(new PlayState(gp));
        }
        if (gp.keyH.upPressed) {
            gp.pauseUI.commandNum = (gp.pauseUI.commandNum <= 0) ? 3 : gp.pauseUI.commandNum - 1;
            gp.keyH.upPressed = false;
        }
        if (gp.keyH.downPressed) {
            gp.pauseUI.commandNum = (gp.pauseUI.commandNum >= 3) ? 0 : gp.pauseUI.commandNum + 1;
            gp.keyH.downPressed = false;
        }
        if (gp.keyH.enterPressed) {
            gp.keyH.enterPressed = false;
            if (gp.pauseUI.commandNum == 0) {
               gp.changeState(new PlayState(gp));
            }
            if (gp.pauseUI.commandNum == 1) { // SAVE
                Database.SaveManager sm = new Database.SaveManager(gp);
                sm.saveGame(1);
            }
            if (gp.pauseUI.commandNum == 3) {
                gp.changeState(new MenuState(gp));
            }
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        // Redăm lumea în fundal ca să se vadă jocul înghețat sub meniul de pauză
        gp.tileM.draw(g2);
        for (var n : gp.npc) if (n != null) n.draw(g2);
        for (var e : gp.enemy) if (e != null) e.draw(g2);
        gp.player.draw(g2);
        gp.ui.draw(g2);
    }

    @Override public void exit() {}
}