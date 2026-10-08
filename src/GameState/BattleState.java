package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class BattleState implements GameState {
    private final GamePanel gp;

    public BattleState(GamePanel gp) {
        this.gp = gp;
    }

    @Override public void enter() { gp.battleUI.onBattleStart(); }

    @Override
    public void update() {
        gp.battleUI.update(); // timerele și animațiile din luptă merg pe ticks, nu pe cadre desenate
        if (gp.player.life <= 0) return;

        gp.gameTimer.update();
        gp.player.update();

        // Popup-urile se actualizează acum în GamePanel.update(), pentru toate stările

        if (gp.player.currentEnemy != null && !gp.player.currentEnemy.alive) {
            gp.player.winBattle();
            gp.changeState(new RewardState(gp));
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.ui.draw(g2); // Randarea completă a arenei de luptă, cărților din mână și pop-up-urilor
    }

    @Override public void exit() {}
}