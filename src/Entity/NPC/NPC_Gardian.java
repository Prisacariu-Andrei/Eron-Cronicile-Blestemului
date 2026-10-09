package Entity.NPC;

import Main.GamePanel;
import GameState.*;
import Main.Exception.GameExceptionHandler;
import ProgressManagement.QuestManager.Quest;
import javax.imageio.ImageIO;
import java.util.Objects;

public class NPC_Gardian extends NPC {

    private final ProgressManagement.StoryProgress.Gate gate; // res/Data/gates.json

    public NPC_Gardian(GamePanel gp, ProgressManagement.StoryProgress.Gate gate) {
        super(gp, gp.tileSize * gate.guardCol, gp.tileSize * gate.guardRow);
        this.name = "Garda de Fier";
        this.gate = gate;
        if (gp.story.isOpen(gate)) stepAside(); // pasajul e deja deschis (salvare încărcată, revenire în zonă)
        getImage();
    }

    private void stepAside() {
        worldX = gp.tileSize * gate.asideCol;
        worldY = gp.tileSize * gate.asideRow;
    }

    @Override
    public void getImage() {
        try {
            // Poți folosi textura ta de NPC sau una specifică de gardian dacă ai (/NPC/guard.png)
            down1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/GardaDeFierStaticFata.png")));
            up1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/GardaDeFierStaticSpate.png")));
            left1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/GardaDeFierStaticStanga.png")));
            right1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/GardaDeFierStaticDreapta.png")));
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

        if (isShowingDialogue) {
            isShowingDialogue = false;
            gp.activeNpcIndex = -1;
            gp.changeState(new PlayState(gp));
            return;
        }
        isShowingDialogue = true;

        gp.changeState(new DialogueState(gp));
        dialogueIndex = 0;

        if (gp.story.isOpen(gate)) {
            // Boss-ul zonei a căzut: Garda dă trecere și se retrage din drum
            gp.dialogueUI.currentDialogue = gate.openText;
            stepAside();
            if (gate.quest > 0) {
                Quest q = gp.questManager.getQuestById(gate.quest, true);
                if (q != null && q.isReceived && !q.isCompleted) {
                    gp.questManager.updateQuestProgress(gate.quest, true, 1);
                    gp.ui.message = "Misiune Actualizată!";
                    gp.ui.messageOn = true;
                }
            }
        } else {
            gp.dialogueUI.currentDialogue = gate.closedText;
            switch (gp.player.direction) { // nu rămâne blocat în Gardă
                case "up" -> gp.player.worldY += 10;
                case "down" -> gp.player.worldY -= 10;
                case "left" -> gp.player.worldX += 10;
                case "right" -> gp.player.worldX -= 10;
            }
        }

        gp.dialogueUI.resetCharIndex();
    }
}