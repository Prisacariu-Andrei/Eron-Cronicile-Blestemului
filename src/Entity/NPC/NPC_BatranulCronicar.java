package Entity.NPC;

import Main.GamePanel;
import GameState.*;
import Main.Exception.GameExceptionHandler;
import javax.imageio.ImageIO;
import java.util.Objects;

public class NPC_BatranulCronicar extends NPC {

    public NPC_BatranulCronicar(GamePanel gp, int x, int y) {
        super(gp, x, y);
        this.name = "Bătrânul Înțelept";
        getImage();
        setDialogue();
    }

    @Override
    public void getImage() {
        try {
            down1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Player/AIGup.png")));
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Nu s-a putut încărca textura pentru NPC Bătrânul Cronicar (/Player/AIGup.png)", e);
        }
    }

    @Override
    public void setDialogue() {
        dialogues[0] = "Salutare, tinere Eron! Bine ai venit în Eldoria.\nLumea s-a schimbat de când blestemul ne-a cuprins.";
        dialogues[1] = "Misiunea ta este grea, dar trebuie să fii curajos.\nPentru început, trebuie să înveți să te aperi.";
        dialogues[2] = "Apasă tasta [E] pentru a interacționa cu mine sau\ncu alți locuitori ai tărâmului.";
        dialogues[3] = "Găsește primul inamic și învinge-l. Acesta este\nprimul tău pas spre purificarea ținutului!";
    }

    @Override
    public void speak() {
        var quest1 = gp.questManager.getQuestById(1, true);
        var quest2 = gp.questManager.getQuestById(2, true);

        if (!quest1.isReceived) {
            if (dialogueIndex >= 4 || dialogues[dialogueIndex] == null) {
                dialogueIndex = 0;
                gp.changeState(new PlayState(gp));

                gp.questManager.receiveQuest(1, true);
                gp.ui.message = "Misiune primită: Tutorial";
                gp.ui.messageOn = true;
                return;
            }

            gp.changeState(new DialogueState(gp));
            gp.dialogueUI.currentDialogue = dialogues[dialogueIndex];
            gp.dialogueUI.resetCharIndex();

            if (dialogueIndex < 3) {
                dialogueIndex++;
            } else {
                dialogueIndex = 4;
            }
        }
        else if (!quest1.isCompleted) {
            gp.changeState(new DialogueState(gp));
            gp.dialogueUI.currentDialogue = "Încă nu ai învins primul inamic, tinere Eron.\nFii curajos și caută-l în pădure!";
            gp.dialogueUI.resetCharIndex();
            dialogueIndex = 0;
        }
        else{

            if (quest2 != null && !quest2.isReceived) {
                gp.questManager.receiveQuest(2, true);
                gp.changeState(new DialogueState(gp));
                gp.dialogueUI.currentDialogue = "Îți mulțumesc pentru purificarea zonei! Ai dovedit curaj.\nIată o nouă sarcină: Găsește Bârlogul și învinge 5 lupi!";
                gp.dialogueUI.resetCharIndex();

                gp.ui.message = "Misiune primită: Inamici în Bârlog";
                gp.ui.messageOn = true;
                dialogueIndex = 0;
            }
            else if (quest2 != null && !quest2.isCompleted) {
                gp.changeState(new DialogueState(gp));
                gp.dialogueUI.currentDialogue = "Misiunea ta continuă în Bârlog. Mai ai de învins inamici!";
                gp.dialogueUI.resetCharIndex();
                dialogueIndex = 0;
            }
            else {
                gp.changeState(new DialogueState(gp));
                gp.dialogueUI.currentDialogue = "Eldoria îți mulțumește pentru tot ce ai făcut, Eron.\nEști un adevărat erou!";
                gp.dialogueUI.resetCharIndex();
                dialogueIndex = 0;
            }
        }
    }
}