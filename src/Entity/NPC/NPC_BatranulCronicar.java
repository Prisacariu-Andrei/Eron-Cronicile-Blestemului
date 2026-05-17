package Entity.NPC;

import Main.GamePanel;
import GameState.*;
import ProgressManagement.QuestManager.Quest;
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
        // 1. Găsim dinamic care este prima misiune principală nefinalizată din listă
        Quest currentQuest = null;
        for (Quest q : gp.questManager.mainQuests) {
            if (!q.isCompleted || q.isReceived) {
                // Prinde ori misiunea activă, ori cea proaspăt terminată care așteaptă să fie predată
                currentQuest = q;
                break;
            }
        }

        // Dacă toate misiunile sunt terminate (Nu mai există nicio misiune activă)
        if (currentQuest == null) {
            gp.changeState(new DialogueState(gp));
            gp.dialogueUI.currentDialogue = "Eldoria este salvată! Blestemul lui Malakor s-a risipit.\nEști un erou de legendă, Eron. Pacea s-a întors acasă.";
            gp.dialogueUI.resetCharIndex();
            return;
        }

        // 2. Gestionăm comportamentul în funcție de starea misiunii curente determinată mai sus

        // CAZUL A: Misiunea nu a fost încă primită de jucător
        if (!currentQuest.isReceived) {

            // Tratat special pentru Quest 1 deoarece are pagini multiple de introducere (0, 1, 2, 3)
            if (currentQuest.id == 1) {
                if (dialogueIndex >= dialogues.length || dialogues[dialogueIndex] == null) {
                    dialogueIndex = 0;
                    gp.changeState(new PlayState(gp));

                    // 1. Jucătorul primește oficial misiunea
                    gp.questManager.receiveQuest(1, true);

                    // ★ REZOLVARE: Forțăm imediat harta să spawneze inamicii din Eldoria!
                    gp.aSetter.setEnemyForMap("Eldoria");

                    gp.ui.message = "Misiune primită: " + currentQuest.title;
                    gp.ui.messageOn = true;
                    return;
                }
                gp.changeState(new DialogueState(gp));
                gp.dialogueUI.currentDialogue = dialogues[dialogueIndex];
                gp.dialogueUI.resetCharIndex();
                dialogueIndex++;
            }
            // Pentru restul quest-urilor (2 -> 14), oferă textul de primire direct
            else {
                gp.questManager.receiveQuest(currentQuest.id, true);
                gp.changeState(new DialogueState(gp));
                gp.dialogueUI.currentDialogue = getQuestStartDialogue(currentQuest.id);
                gp.dialogueUI.resetCharIndex();

                gp.ui.message = "Misiune primită: " + currentQuest.title;
                gp.ui.messageOn = true;
            }
        }

        // CAZUL B: Misiunea este primită, dar NU este completată încă (Reminder)
        else if (!currentQuest.isCompleted) {
            if (isShowingDialogue) {
                // A doua apăsare pe [E] → ieșim din dialog
                isShowingDialogue = false;
                dialogueIndex = 0;
                gp.changeState(new PlayState(gp));
                return;
            }
            isShowingDialogue = true;
            gp.changeState(new DialogueState(gp));
            gp.dialogueUI.currentDialogue = getQuestProgressDialogue(currentQuest.id);
            gp.dialogueUI.resetCharIndex();
        }

        // CAZUL C: Misiunea este completată! Jucătorul o predă ACUM
        else{
            gp.changeState(new DialogueState(gp));
            gp.dialogueUI.currentDialogue = getQuestCompleteDialogue(currentQuest.id);
            gp.dialogueUI.resetCharIndex();

            // O eliminăm din starea de "activă" prin marcarea ei ca ne-primită (sau adaugi un flag isTurnedIn în Quest)
            // Pentru simplitatea sistemului tău curent, setăm isReceived pe false doar ca loop-ul de căutare să treacă la următoarea misiune din listă tura viitoare
            currentQuest.isReceived = false;
        }
    }

    // UTILS: Mutăm textele lungi în metode separate pentru lizibilitate curată
    private String getQuestStartDialogue(int id) {
        return switch (id) {
            case 2 -> "Îți mulțumesc pentru purificarea zonei! Ai dovedit curaj.\nIată o nouă sarcină: Găsește Bârlogul și învinge 5 lupi!";
            case 3 -> "Extraordinar! Ai eliminat amenințarea haitei de lupi.\nBut conducătorul lor, Lupul cel Bătrân, s-a trezit. Vânează-l!";
            case 4 -> "Lupul cel Bătrân a fost îmblânzit, iar Dintele de Chilimbar este al tău!\nAcum trebuie să obții permisiunea de a trece în Abis.";
            case 5 -> "Gardienii ți-au recunoscut valoarea și au deschis porțile.\nCoboară în Abisul de Piatra și curăță monștrii!";
            // Adaugă restul până la 14 ...
            default -> "Am o nouă sarcină pentru tine, Eron.";
        };
    }

    private String getQuestProgressDialogue(int id) {
        return switch (id) {
            case 1 -> "Încă nu ai învins primul inamic, tinere Eron.\nFii curajos și caută-l în pădure!";
            case 2 -> "Misiunea ta continuă în Bârlog. Mai ai de învins inamici!\nCurăță zona de cei 5 lupi sălbatici.";
            case 3 -> "Lupul cel Bătrân se ascunde în cele mai întunecate unghere din Bârlog.\nGăsește-l, învinge-l și adu înapoi relicva sacră!";
            // Adaugă restul ...
            default -> "Încă lucrezi la misiune? Întoarce-te când termini.";
        };
    }

    private String getQuestCompleteDialogue(int id) {
        return switch (id) {
            case 1 -> "Excelent! Ai învins primul inamic și ai înțeles elementele de bază.";
            case 2 -> "Ai curățat Bârlogul! Lupii nu mai reprezintă o amenințare directă.";
            case 3 -> "Ai înfrânt Lupul cel Bătrân! Simt cum puterea lui s-a risipit.";
            // Adaugă restul textelor de finalizare ...
            default -> "Minunat! Ai terminat treaba. Să trecem la următorul pas.";
        };
    }
}