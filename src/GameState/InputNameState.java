package GameState;

import Main.GamePanel;
import java.awt.*;
import java.awt.event.KeyEvent;

public class InputNameState implements GameState {
    private final GamePanel gp;
    private StringBuilder name = new StringBuilder();
    private final int slot;

    public InputNameState(GamePanel gp, int slot) {
        this.gp = gp;
        this.slot = slot;
    }

    @Override
    public void enter() {
        name.setLength(0);
    }

    @Override
    public void update() {
        // Enter pentru confirmare
        if (gp.keyH.enterPressed && name.length() > 0) {
            System.out.println("DEBUG: Se inițializează aventura pentru slotul: " + slot);

            // 1. Resetăm starea tastei pentru a nu intra în buclă
            gp.keyH.enterPressed = false;

            // 2. Inițializăm un joc nou de la zero

            // Apelăm resetPlayer() pentru a curăța deck-ul, itemele și a reface viața inamicilor
            gp.player.resetPlayer();

            // IMPORTANT: Setează numele DUPĂ resetPlayer(), deoarece setDefaultValues() resetează numele înapoi la "Eron"
            gp.player.name = name.toString();

            // Ne asigurăm că pornim pe harta de început (Eldoria) cu inamicii și NPC-urile ei proaspete
            gp.tileM.loadZoneTiles("Eldoria");
            gp.tileM.loadMap("/Maps/Eldoria");
            gp.aSetter.setEnemyForMap("/Maps/Eldoria");
            gp.aSetter.setNPC("Eldoria");

            // Resetăm cronometrul jocului la 0 (adaptează dacă ai altă metodă de reset în GameTimer)
            // gp.gameTimer.reset();

            // 3. Salvăm această stare inițială „curată” direct în DB pentru slotul selectat
            // Parametri: Jucătorul, Slotul curent, Zona de start ("Eldoria"), Timp jucat inițial (0 secunde)
            gp.saveLoadManager.saveGame(gp.player, slot, "Eldoria", 0);

            // 4. Forțăm tranziția către Cutscene
            System.out.println("DEBUG: Tranziție către CutsceneState...");
            gp.changeState(new ZoneTransitionState(gp, "prologue", () -> new CutsceneState(gp)));   // ecranul de încărcare, apoi prologul
        }
    }

    public void addChar(char c) {
        if (name.length() < 12) name.append(c);
    }

    public void removeChar() {
        if (name.length() > 0) name.deleteCharAt(name.length() - 1);
    }

    @Override
    public void draw(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        g2.drawString("INTRODU NUMELE EROULUI:", 450, 400);
        g2.drawString(name.toString() + "_", 650, 500);

        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("APASĂ ENTER PENTRU A ÎNCEPE AVENTURA", 550, 600);
    }

    @Override
    public void exit() {}
}