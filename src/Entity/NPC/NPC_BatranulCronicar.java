package Entity.NPC;

import Main.GamePanel;
import javax.imageio.ImageIO;
import java.io.IOException;
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setDialogue() {
        // Dialogul inițial de întâmpinare
        dialogues[0] = "Salutare, tinere Eron! Bine ai venit în Eldoria.\nLumea s-a schimbat de când blestemul ne-a cuprins.";
        dialogues[1] = "Misiunea ta este grea, dar trebuie să fii curajos.\nPentru început, trebuie să înveți să te aperi.";
        dialogues[2] = "Apasă tasta [E] pentru a interacționa cu mine sau\ncu alți locuitori ai tărâmului.";
        dialogues[3] = "Găsește primul inamic și învinge-l. Acesta este\nprimul tău pas spre purificarea ținutului!";
    }

    @Override
    public void speak() {
        var quest1 = gp.questManager.getQuestById(1, true);
        var quest2 = gp.questManager.getQuestById(2, true);

        // 1. Dacă primul quest nu a fost primit încă
        if (!quest1.isReceived) {
            if (dialogueIndex >= 4 || dialogues[dialogueIndex] == null) {
                dialogueIndex = 0;
                gp.gameState = Main.GamePanel.GameState.PLAY;

                // Activăm primul quest
                gp.questManager.receiveQuest(1, true);
                gp.ui.message = "Misiune primită: Tutorial";
                gp.ui.messageOn = true;
                return;
            }

            gp.gameState = Main.GamePanel.GameState.DIALOGUE;
            gp.dialogueUI.currentDialogue = dialogues[dialogueIndex];
            gp.dialogueUI.resetCharIndex();

            if (dialogueIndex < 3) {
                dialogueIndex++;
            } else {
                dialogueIndex = 4;
            }
        }
        // 2. Dacă primul quest a fost primit dar nu este finalizat
        else if (!quest1.isCompleted) {
            gp.gameState = Main.GamePanel.GameState.DIALOGUE;
            gp.dialogueUI.currentDialogue = "Încă nu ai învins primul inamic, tinere Eron.\nFii curajos și caută-l în pădure!";
            gp.dialogueUI.resetCharIndex();
            dialogueIndex = 0;
        }
        // 3. Dacă primul quest este finalizat
        else if (quest1.isCompleted) {

            // Dacă nu am primit al doilea quest
            if (quest2 != null && !quest2.isReceived) {
                gp.questManager.receiveQuest(2, true);
                gp.gameState = Main.GamePanel.GameState.DIALOGUE;
                gp.dialogueUI.currentDialogue = "Îți mulțumesc pentru purificarea zonei! Ai dovedit curaj.\nIată o nouă sarcină: Găsește Bârlogul și învinge 5 lupi!";
                gp.dialogueUI.resetCharIndex();

                gp.ui.message = "Misiune primită: Inamici în Bârlog";
                gp.ui.messageOn = true;
                dialogueIndex = 0;
            }
            // Dacă al doilea quest este primit dar nu este completat
            else if (quest2 != null && !quest2.isCompleted) {
                gp.gameState = Main.GamePanel.GameState.DIALOGUE;
                gp.dialogueUI.currentDialogue = "Misiunea ta continuă în Bârlog. Mai ai de învins inamici!";
                gp.dialogueUI.resetCharIndex();
                dialogueIndex = 0;
            }
            // Dacă ambele quest-uri sunt finalizate / sau suntem la final
            else {
                gp.gameState = Main.GamePanel.GameState.DIALOGUE;
                gp.dialogueUI.currentDialogue = "Eldoria îți mulțumește pentru tot ce ai făcut, Eron.\nEști un adevărat erou!";
                gp.dialogueUI.resetCharIndex();
                dialogueIndex = 0;
            }
        }
    }
}