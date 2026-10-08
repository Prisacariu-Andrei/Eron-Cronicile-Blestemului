package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class MenuState implements GameState {
    private final GamePanel gp;

    public MenuState(GamePanel gp) { this.gp = gp; }

    @Override public void enter() { gp.gameTimer.stop(); gp.ui.commandNum = 0; }

    @Override
    public void update() {
        // Logica de meniu rămâne la fel, dar la START (0) trimitem la InputNameState
        if (gp.keyH.upPressed) {
            gp.ui.commandNum = (gp.ui.commandNum <= 0) ? 4 : gp.ui.commandNum - 1;
            gp.keyH.upPressed = false;
        }
        if (gp.keyH.downPressed) {
            gp.ui.commandNum = (gp.ui.commandNum >= 4) ? 0 : gp.ui.commandNum + 1;
            gp.keyH.downPressed = false;
        }
        // În MenuState.java
        if (gp.keyH.enterPressed) {
            gp.keyH.enterPressed = false;

            switch (gp.ui.commandNum) {
                case 0: // Începe aventura
                        gp.changeState(new InputNameState(gp, 1));
                    break;
                case 1: // Continuă aventura
                    boolean incarcareReusita = gp.saveLoadManager.loadGame(gp, 1);

                    if (incarcareReusita) {
                        gp.changeState(new PlayState(gp)); // Dacă a găsit salvare, intră în joc!
                    } else {
                        // Dacă nu are salvare, dă un bip de eroare sau pune un mesaj "Nu există salvare!"
                        System.out.println("Fisier de salvare inexistent!");
                    }                    break;
                case 2: // Top Scoruri
                    gp.changeState(new LeaderboardState(gp));
                    break;
                case 3: // Opțiuni
                    // gp.changeState(new OptionsState(gp));
                    break;
                case 4: // Părăsește tărâmul
                    Database.DatabaseManager.getInstance().close();
                    System.exit(0);
                    break;
            }
        }
    }

    @Override public void draw(Graphics2D g2) { gp.ui.draw(g2); }
    @Override public void exit() { gp.gameTimer.start(); }
}