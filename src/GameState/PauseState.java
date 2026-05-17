package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class PauseState implements GameState {
    private final GamePanel gp;

    public PauseState(GamePanel gp) {
        this.gp = gp;
    }

    @Override public void enter() {
        gp.pauseUI.commandNum = 0;
        gp.gameTimer.stop();
    }

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
                // Preluăm zona curentă în care se află jucătorul
                String zonaCurenta = gp.tileM.currentZone;

                // Preluăm timpul (adaptează dacă folosești altă metodă de get)
                long timpJucat = gp.gameTimer.getElapsedSeconds(); // sau gp.ui.getPlayTime() dacă ai în UI

                // Salvăm pe slotul 1 (poți schimba pe viitor dacă faci multiple sloturi)
                gp.saveLoadManager.saveGame(gp.player, 1, zonaCurenta, timpJucat);

                // Opțional: Afișăm un mesaj pe ecran ca jucătorul să știe că a mers
                System.out.println("Joc salvat cu succes în: " + zonaCurenta);
                gp.changeState(new PlayState(gp)); // Opțional: Scoatem jocul din pauză după salvare
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

    @Override public void exit() {
        gp.gameTimer.start();
    }
}