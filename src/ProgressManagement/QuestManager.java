package ProgressManagement;

import java.util.ArrayList;
import java.util.List;

public class QuestManager {

    public static class Quest {
        public int id;
        public String title;
        public String description;
        public boolean isMainQuest;
        public boolean isCompleted;
        public boolean isReceived; // Noua variabilă pentru a verifica dacă a fost primită
        public int requiredAmount;
        public int currentAmount;

        public Quest(int id, String title, String description, boolean isMainQuest, int requiredAmount) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.isMainQuest = isMainQuest;
            this.requiredAmount = requiredAmount;
            this.currentAmount = 0;
            this.isCompleted = false;
            this.isReceived = false; // Implicit, quest-ul este blocat/ne-primit
        }

        public void addProgress(int amount) {
            if (!isCompleted) {
                currentAmount = Math.min(requiredAmount, currentAmount + amount);
                if (currentAmount >= requiredAmount) {
                    isCompleted = true;
                }
            }
        }
    }

    // Metodă pentru a activa/primi o misiune de la un NPC sau eveniment
    public void receiveQuest(int id, boolean isMain) {
        Quest q = getQuestById(id, isMain);
        if (q != null) {
            q.isReceived = true;
        }
    }

    public List<Quest> mainQuests;
    public List<Quest> sideQuests;

    public QuestManager() {
        mainQuests = new ArrayList<>();
        sideQuests = new ArrayList<>();

        initMainQuests();
        initSideQuests();
    }

    private void initMainQuests() {
        mainQuests.add(new Quest(1, "Tutorial", "Învață mecanicile jocului și află ce trebuie să faci. Învinge lupul ce protejează Bârlogul", true, 1));
        mainQuests.add(new Quest(2, "Inamici în Bârlog", "Învinge 5 inamici în Bârlog.", true, 5));
        mainQuests.add(new Quest(3, "Lupul cel Bătrân", "Învinge Lupul cel Bătrân din Bârlog și obține runa sa.", true, 1));
        mainQuests.add(new Quest(4, "Deblochează Abisul", "Vorbește cu gardienii pentru a obține permisiunea de a trece în Abisul de Piatra.", true, 1));
        mainQuests.add(new Quest(5, "Inamici în Abis", "Învinge 5 inamici în Abisul de Piatra.", true, 5));
        mainQuests.add(new Quest(6, "Mini-Boss Abis", "Învinge mini-boss-ul din zona Abisul de Piatra.", true, 1));
        mainQuests.add(new Quest(7, "Runa lui Feldgug", "Învinge-l pe Feldgug și obține runa sa.", true, 1));
        mainQuests.add(new Quest(8, "Descopera Citadela", "Găsește drumul spre Citadela Haiducilor.", true, 1));
        mainQuests.add(new Quest(9, "Jean Pleșa", "Recrutează-l pe Jean Pleșa și obține-i runa.", true, 1));
        mainQuests.add(new Quest(10, "Inelul Blestemului", "Găsește o soluție pentru a purifica Inelul Blestemului.", true, 1));
        mainQuests.add(new Quest(11, "Stăpânul Gheții", "Învinge-o pe Glaciara și obține runa de gheață.", true, 1));
        mainQuests.add(new Quest(12, "Calea către Castel", "Eliberează calea spre Castelul de Obsidian, învingându-l pe Charon.", true, 1));
        mainQuests.add(new Quest(13, "Salvează tărâmul", "Pregătește-te pentru bătălia finală.", true, 1));
        mainQuests.add(new Quest(14, "Malakor, Monarhul Amurgului", "Învinge-l pe Malakor și salvează Eldoria.", true, 1));
    }

    private void initSideQuests() {
        sideQuests.add(new Quest(1, "Lupul de Elită", "Învinge un Lup de Elită.", false, 1));
        sideQuests.add(new Quest(2, "Comori pierdute", "Găsește toate comorile dintr-o zonă.", false, 1));
        sideQuests.add(new Quest(3, "Valerius", "Învinge-l pe Valerius.", false, 1));
        sideQuests.add(new Quest(4, "Vânătorul de Yeti", "Învinge un Yeti în Tundra.", false, 1));
    }

    public Quest getQuestById(int id, boolean isMain) {
        List<Quest> list = isMain ? mainQuests : sideQuests;
        for (Quest q : list) {
            if (q.id == id) return q;
        }
        return null;
    }

    public void updateQuestProgress(int id, boolean isMain, int amount) {
        Quest q = getQuestById(id, isMain);
        if (q != null) {
            q.addProgress(amount);
        }
    }

    // Adaugă această metodă în clasa QuestManager
    public void resetAllQuests() {
        for (Quest q : mainQuests) {
            q.isReceived = false;
            q.isCompleted = false;
            q.currentAmount = 0;
        }
        for (Quest q : sideQuests) {
            q.isReceived = false;
            q.isCompleted = false;
            q.currentAmount = 0;
        }
    }
}