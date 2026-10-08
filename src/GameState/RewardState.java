package GameState;

import Main.GamePanel;
import Entity.Items.Card;
import java.awt.Graphics2D;

public class RewardState implements GameState {
    private final GamePanel gp;

    public RewardState(GamePanel gp) {
        this.gp = gp;
    }

    @Override public void enter() { gp.rewardUI.onOpen(); }

    @Override
    public void update() {
        gp.rewardUI.update();
        if (gp.keyH.leftPressed) {
            gp.keyH.leftPressed = false;
            gp.rewardUI.commandNum--;
            if (gp.rewardUI.commandNum < 0) gp.rewardUI.commandNum = gp.rewardUI.rewardCards.size() - 1;
        }
        if (gp.keyH.rightPressed) {
            gp.keyH.rightPressed = false;
            gp.rewardUI.commandNum++;
            if (gp.rewardUI.commandNum >= gp.rewardUI.rewardCards.size()) gp.rewardUI.commandNum = 0;
        }

        if (gp.keyH.enterPressed) {
            gp.keyH.enterPressed = false;
            Card selected = gp.rewardUI.getSelectedCard();
            if (selected != null) {
                gp.fx.rewardBurst(gp.rewardUI.selectedCenterX(), gp.rewardUI.selectedCenterY());
                gp.player.inventory.addCardToDeck(selected.name, gp.player);
                gp.rewardUI.commandNum = 0;
                gp.changeState(new PlayState(gp));
            }
        }

        if (gp.keyH.escPressed) {
            gp.keyH.escPressed = false;
            gp.rewardUI.commandNum = 0;
            gp.changeState(new PlayState(gp));
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.ui.draw(g2);
    }

    @Override public void exit() {}
}