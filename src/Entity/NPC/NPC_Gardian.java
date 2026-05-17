package Entity.NPC;

import Main.GamePanel;
import GameState.*;
import Main.Exception.GameExceptionHandler;
import ProgressManagement.QuestManager.Quest;
import javax.imageio.ImageIO;
import java.util.Objects;

public class NPC_Gardian extends NPC {

    private final int questIdBariera; // ID-ul quest-ului necesar pentru a trece de acest gardian
    private final String textRefuz;
    private final String textPermisiune;

    public NPC_Gardian(GamePanel gp, int x, int y, int questIdBariera, String textRefuz, String textPermisiune) {
        super(gp, x, y);
        this.name = "Gardianul Porții";
        this.questIdBariera = questIdBariera;
        this.textRefuz = textRefuz;
        this.textPermisiune = textPermisiune;

        getImage();
    }

    @Override
    public void getImage() {
        try {
            // Poți folosi textura ta de NPC sau una specifică de gardian dacă ai (/NPC/guard.png)
            down1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/AIGup.png")));
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Nu s-a putut încărca textura pentru NPC Gardian", e);
        }
    }

    @Override
    public void setDialogue() {
        // Nu mai avem nevoie de ea aici, deoarece textele vin prin constructor dinamici
    }

    @Override
    public void speak() {
        Quest q = gp.questManager.getQuestById(questIdBariera, true);

        gp.changeState(new DialogueState(gp));
        dialogueIndex = 0;

        // Cazul 1: Jucătorul are quest-ul activ (sau l-a terminat deja) -> Gardianul îl lasă să treacă
        if (q != null && q.isReceived) {

            // Dacă misiunea nu era completată (ex: Quest 4 unde trebuie doar să vorbești cu ei), o completăm acum!
            if (!q.isCompleted) {
                gp.questManager.updateQuestProgress(questIdBariera, true, 1);
                gp.ui.message = "Misiune Actualizată!";
                gp.ui.messageOn = true;
            }

            gp.dialogueUI.currentDialogue = textPermisiune;

            // LOGICĂ BONUS: Împingem gardianul un tile mai încolo ca să elibereze fizic trecerea!
            this.worldX += gp.tileSize; // Sau schimbi coordonata lui să nu mai blocheze ușa

        }
        // Cazul 2: Jucătorul nu a primit încă misiunea de la Cronicar -> Refuz total
        else {
            gp.dialogueUI.currentDialogue = textRefuz;

            // Opțional: Împingem jucătorul puțin înapoi ca să nu rămână blocat în gardian
            switch (gp.player.direction) {
                case "up" -> gp.player.worldY += 10;
                case "down" -> gp.player.worldY -= 10;
                case "left" -> gp.player.worldX += 10;
                case "right" -> gp.player.worldX -= 10;
            }
        }

        gp.dialogueUI.resetCharIndex();
    }
}