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
            gp.dialogueUI.currentDialogue = gp.questManager.mainText(0, "complete", "Eldoria este salvată! Blestemul s-a risipit.\nEști un erou de legendă, Eron. Pacea s-a întors acasă.");
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

    /** "!" când are o misiune nouă de oferit, "?" când poți preda una (ca să știi când să te întorci la el). */
    @Override
    public int markerKind() {
        for (Quest q : gp.questManager.mainQuests) {
            if (q.isCompleted && !q.isReceived) continue;   // deja predată (sau făcută înainte să fie primită)
            if (!q.isReceived) return MARKER_AVAILABLE;
            return q.isCompleted ? MARKER_READY : MARKER_ACTIVE;
        }
        return MARKER_NONE;
    }

    // UTILS: Mutăm textele lungi în metode separate pentru lizibilitate curată
    private String getQuestStartDialogue(int id) {
        return gp.questManager.mainText(id, "start", startDefault(id));
    }

    private String startDefault(int id) {
        return switch (id) {
            case 2 -> "Îți mulțumesc pentru purificarea zonei! Ai dovedit curaj.\nIată o nouă sarcină: Găsește Bârlogul și învinge 5 lupi!";
            case 3 -> "Extraordinar! Ai eliminat amenințarea haitei de lupi.\nBut conducătorul lor, Lupul cel Bătrân, s-a trezit. Vânează-l!";
            case 4 -> "Lupul cel Bătrân a fost îmblânzit, iar Dintele de Chilimbar este al tău!\nAcum trebuie să obții permisiunea de a trece în Abis.";
            case 5 -> "Gardienii ți-au recunoscut valoarea și au deschis porțile.\nCoboară în Abisul de Piatra și curăță monștrii!";
            case 6 -> "Abisul ascunde un paznic mai puternic decât ceilalți: un Trol de Elită.\nÎnvinge-l ca să ajungi la tiranul de piatră.";
            case 7 -> "Feldgug, Zdrobi-Oase, domnește în adâncuri. Doboară-l și Diamantul Nopții\nva fi al tău, laolaltă cu runa lui.";
            case 8 -> "Cu Feldgug înfrânt, drumul spre Citadela Haiducilor se poate deschide.\nVorbește cu Garda de Fier din sat.";
            case 9 -> "În Citadelă îl vei găsi pe Jean Pleșa. Învinge-l: dacă te dovedești,\nse va alătura echipei tale, împreună cu runa și Paloșul lui.";
            case 10 -> "Garda te lasă acum spre Tundra Tăcerii. Acolo, un labirint de gheață pecetluiește\ncălea spre Glaciara. Găsește Axul Solstițiului și îndreaptă lumina spre Poarta de Gheață.";
            case 11 -> "Poarta de Gheață s-a topit! În vârful turnului te așteaptă Glaciara.\nÎnvinge-o și recuperează Inelul de Gheață.";
            case 12 -> "Inelul de Gheață a risipit ceața: Podul Umbrelor se arată la răsărit de sat.\nÎl păzește Charon. Treci peste el și drumul spre Castel e liber.";
            case 13 -> "Castelul de Obsidian se înalță în față. Ai adunat Inelul, Paloșul și Diamantul.\nPregătește-te: dincolo de porți nu mai e cale de întoarcere.";
            case 14 -> "Malakor, Monarhul Amurgului, te așteaptă în Sala Tronului.\nÎnvinge-l și risipește blestemul de pe Eldoria!";
            default -> "Am o nouă sarcină pentru tine, Eron.";
        };
    }

    private String getQuestProgressDialogue(int id) {
        return gp.questManager.mainText(id, "progress", progressDefault(id));
    }

    private String progressDefault(int id) {
        return switch (id) {
            case 1 -> "Încă nu ai învins primul inamic, tinere Eron.\nFii curajos și caută-l în pădure!";
            case 2 -> "Misiunea ta continuă în Bârlog. Mai ai de învins inamici!\nCurăță zona de cei 5 lupi sălbatici.";
            case 3 -> "Lupul cel Bătrân se ascunde în cele mai întunecate unghere din Bârlog.\nGăsește-l, învinge-l și adu înapoi relicva sacră!";
            case 10 -> "Labirintul Tundrei e un joc de oglinzi: trage manivela din fundătură\nși întoarce oglinzile din sală până când lumina atinge Poarta de Gheață.";
            case 11 -> "Glaciara veghează în turnul de gheață. Vino pregătit.";
            case 12 -> "Podul Umbrelor e la răsărit de sat. Charon nu lasă pe nimeni să treacă.";
            default -> "Încă lucrezi la misiune? Întoarce-te când termini.";
        };
    }

    private String getQuestCompleteDialogue(int id) {
        return gp.questManager.mainText(id, "complete", completeDefault(id));
    }

    private String completeDefault(int id) {
        return switch (id) {
            case 1 -> "Excelent! Ai învins primul inamic și ai înțeles elementele de bază.";
            case 2 -> "Ai curățat Bârlogul! Lupii nu mai reprezintă o amenințare directă.";
            case 3 -> "Ai înfrânt Lupul cel Bătrân! Simt cum puterea lui s-a risipit.";
            case 4 -> "Gardienii ți-au deschis Abisul. Fii prudent acolo, sub pământ.";
            case 5 -> "Abisul e mai liniștit acum, dar răul stă mai adânc.";
            case 6 -> "Trolul de Elită a căzut. Drumul spre Feldgug e deschis.";
            case 7 -> "Feldgug a căzut, iar Diamantul Nopții strălucește la tine.";
            case 8 -> "Citadela Haiducilor e acum la îndemână.";
            case 9 -> "Jean Pleșa luptă alături de tine. Paloșul Legendar deschide drumul spre Nord.";
            case 10 -> "Ai topit Poarta de Gheață cu propria lumină! Glaciara e la câțiva pași.";
            case 11 -> "Glaciara a căzut și Inelul de Gheață e al tău. Ghețurile se topesc și,\nprintre ele, ai găsit drumul spre Podul Umbrelor, la răsărit de Eldoria.";
            case 12 -> "Charon a fost înfrânt. Calea spre Castelul de Obsidian este liberă.";
            case 13 -> "Ești gata. Mergi, Eron, și sfârșește blestemul.";
            default -> "Minunat! Ai terminat treaba. Să trecem la următorul pas.";
        };
    }
}