package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class PlayState implements GameState {
    private final GamePanel gp;

    public PlayState(GamePanel gp) {
        this.gp = gp;
    }

    @Override public void enter() {}

    @Override
    public void update() {
        if (gp.storyScenes.playPending()) return;   // scenă din story.json declanșată de un boss învins, un flag sau o zonă
        gp.ui.update(); // expirarea mesajului din HUD
        gp.pickups.update(); // obiectele de misiune pe care le atinge Eron
        gp.gameTimer.update();
        gp.player.update();
        gp.eHandler.checkEvent();
        for (var n : gp.npc) if (n != null) n.update();   // se întorc spre Eron, rătăcesc
        gp.party.updateTrail();                            // aliații merg după Eron

        for (int i = 0; i < gp.enemy.length; i++) {
            if (gp.enemy[i] != null) {
                if (gp.enemy[i].alive) gp.enemy[i].update();
                else gp.enemy[i] = null;
            }
        }

        if (gp.keyH.escPressed) {
            gp.keyH.escPressed = false;
           // gp.changeState(new PauseState(gp)); // Trecem direct în starea de Pauză
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        gp.tileM.draw(g2);
        for (var n : gp.npc) if (n != null) n.draw(g2);
        for (var e : gp.enemy) if (e != null) e.draw(g2);
        gp.party.drawFollowers(g2);
        gp.player.draw(g2);
        gp.fx.drawWorld(g2);   // praf, scântei de vindecare
        if (!gp.fx.ambientAboveFog()) gp.fx.drawAmbient(g2); // ambianța zonei (licurici, scântei...), sub interfață
        gp.ui.draw(g2);
    }

    @Override public void exit() {}
}