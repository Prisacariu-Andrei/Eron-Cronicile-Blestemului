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
        // Adaugă aici controalele specifice inventarului tău (săgeți pentru navigat prin cărți/rune)
        // if (gp.keyH.escPressed || gp.keyH.iPressed) { // Exemplu: i sau ESC închide inventarul
        //     gp.keyH.escPressed = false;
        //     gp.keyH.iPressed = false;
        //     gp.changeState(new PlayState(gp));
        // }
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.tileM.draw(g2);
        gp.player.draw(g2);
        gp.ui.draw(g2); // randarea grafică efectivă o face InventoryUI din interiorul clasei UI
    }

    @Override public void exit() {}
}