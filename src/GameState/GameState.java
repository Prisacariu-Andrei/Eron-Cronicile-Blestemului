package GameState;

import java.awt.Graphics2D;

public interface GameState {
    void enter();              // Apelată când se intră în această stare (ex: resetări, muzică)
    void update();             // Logica jocului (60 FPS)
    void draw(Graphics2D g2);  // Desenarea stării
    void exit();               // Apelată când se părăsește starea (clean-up)
}