package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class BattleState implements GameState {
    private final GamePanel gp;

    public BattleState(GamePanel gp) {
        this.gp = gp;
    }

    /** Fundalul zonei curente (res/Background/bg_<Zonă>.png), încărcat o singură dată la intrarea în luptă; null dacă lipsește. */
    private java.awt.image.BufferedImage background;

    @Override
    public void enter() {
        String path = "/Background/bg_" + gp.tileM.currentZone + ".png";
        background = Main.ZoneInfo.image(path, gp.screenWidth, gp.screenHeight);
        if (background == null) System.out.println("[WARNING] Fundalul de luptă lipsește: " + path + " (rămâne fundalul vechi)");
        gp.battleUI.zoneBackground = background;   // BattleUI îl desenează primul, înaintea arenei
        gp.battleUI.onBattleStart();
    }

    @Override
    public void update() {
        gp.battleUI.update(); // timerele și animațiile din luptă merg pe ticks, nu pe cadre desenate
        gp.tutorial.update(); // indiciile primei lupte
        if (gp.player.life <= 0) return;

        gp.gameTimer.update();
        gp.player.update();

        // Popup-urile se actualizează acum în GamePanel.update(), pentru toate stările

        gp.player.retarget();
        if (gp.player.currentEnemy != null && gp.player.livingEnemies().isEmpty()) gp.player.startWinDelay();
        gp.player.tickWin();   // după pauza de victorie: recompensele și ecranul de recompensă (sau epilogul)

    }

    @Override
    public void draw(Graphics2D g2) {
        gp.ui.draw(g2); // Randarea completă a arenei de luptă, cărților din mână și pop-up-urilor
    }

    @Override public void exit() {}
}