package GameState;

import Main.GamePanel;
import java.awt.Graphics2D;

public class QuestGameState implements GameState {
    private final GamePanel gp;

    public QuestGameState(GamePanel gp) {
        this.gp = gp;
    }

    @Override public void enter() {}

    @Override
    public void update() {
        // Închidem jurnalul cu ESC sau cu J (așa cum e configurat în KeyHandler)
        if (gp.keyH.escPressed || gp.keyH.tPressed) { // tPressed sau J în funcție de mapare, dar KeyHandler blochează tot în QuestGameState
            // Resetăm tastele ca să nu avem dublu input la revenirea în joc
            gp.keyH.escPressed = false;
            gp.changeState(new PlayState(gp));
        }

        // Notă: Logica pentru tasta Q (switchTab) și tasta J (închidere) este deja
        // procesată direct în KeyHandler-ul tău, deci aici ne asigurăm doar de fallback-ul de siguranță.
    }

    @Override
    public void draw(Graphics2D g2) {
        // 1. Desenăm lumea în fundal ca să se vadă jocul sub jurnal
        gp.tileM.draw(g2);
        for (var n : gp.npc) if (n != null) n.draw(g2);
        for (var e : gp.enemy) if (e != null) e.draw(g2);
        gp.player.draw(g2);

        // 2. Desenăm HUD-ul de bază (viață, mesaje)
        gp.ui.draw(g2);

        // 3. Desenăm EFECTIV cartea de quest-uri deasupra lumii ★★★
        gp.questBookUI.draw(g2);
    }

    @Override public void exit() {}
}