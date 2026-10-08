package GameState;

import Main.GamePanel;

import java.awt.Graphics2D;
import java.util.List;

/** Magazinul unui negustor (vezi ShopUI). Se deschide din dialogul unui NPC de tip "merchant". */
public class ShopState implements GameState {
    private final GamePanel gp;
    private final String merchant;
    private final List<String> stock;

    public ShopState(GamePanel gp, String merchant, List<String> stock) {
        this.gp = gp;
        this.merchant = merchant;
        this.stock = stock;
    }

    @Override
    public void enter() {
        gp.shopUI.open(merchant, stock);
        // tastele apăsate în dialog nu trebuie să cumpere ceva imediat
        gp.keyH.enterPressed = gp.keyH.upPressed = gp.keyH.downPressed = gp.keyH.leftPressed = gp.keyH.rightPressed = false;
    }

    @Override
    public void update() {
        gp.gameTimer.update();
        gp.shopUI.update();

        // Fiecare apăsare se consumă o singură dată (ca în MenuState); ESC e tratat în KeyHandler
        if (gp.keyH.upPressed)    { gp.keyH.upPressed = false;    gp.shopUI.move(-1); }
        if (gp.keyH.downPressed)  { gp.keyH.downPressed = false;  gp.shopUI.move(1); }
        if (gp.keyH.leftPressed)  { gp.keyH.leftPressed = false;  gp.shopUI.switchTab(-1); }
        if (gp.keyH.rightPressed) { gp.keyH.rightPressed = false; gp.shopUI.switchTab(1); }
        if (gp.keyH.enterPressed) { gp.keyH.enterPressed = false; gp.shopUI.confirm(); }
    }

    @Override
    public void draw(Graphics2D g2) {
        // Lumea rămâne vizibilă în spatele panoului
        gp.tileM.draw(g2);
        for (var n : gp.npc) if (n != null) n.draw(g2);
        gp.player.draw(g2);
        gp.fx.drawWorld(g2);
        gp.ui.draw(g2);
    }

    @Override public void exit() {}
}
