package Main;

import Entity.Inamici.*;
import Entity.NPC.NPC_BatranulCronicar;
import Entity.NPC.NPC_Gardian;
import Main.Exception.GameExceptionHandler;

import java.util.Arrays;

public class AssetSetter {
    GamePanel gp;

    public AssetSetter(GamePanel gp) {
        this.gp = gp;
    }

    public void setEnemyForMap(String mapPath) {
        Arrays.fill(gp.enemy, null); // Resetăm lista

        if (mapPath.contains("Eldoria")) spawnEldoria();
        else if (mapPath.contains("Barlogul")) spawnBarlogul();
        else if (mapPath.contains("Abisul")) spawnAbisul();
        else if (mapPath.contains("Citadela")) spawnCitadela();
        else if (mapPath.contains("Tundra")) spawnTundra();
        else if (mapPath.contains("Podul")) spawnPodul();
        else if (mapPath.contains("Castelul")) spawnCastelul();
    }

    // --- Metode dedicate pentru fiecare zonă ---

    private void spawnEldoria() {
        // Luăm informațiile despre Quest-ul 1 (Tutorial) din QuestManager
        var q1 = gp.questManager.getQuestById(1, true);

        // Inamicii se vor încărca DOAR dacă jucătorul a vorbit cu Cronicarul și a primit quest-ul
        if (q1 != null && q1.isReceived) {
            gp.enemy[0] = EnemyFactory.createEnemy("Lup Slăbit", gp, gp.tileSize * 31, gp.tileSize * 37);
            gp.enemy[1] = EnemyFactory.createEnemy("Valerius", gp, gp.tileSize * 39, gp.tileSize * 5);

            System.out.println("DEBUG: Eldoria populate - Quest-ul 1 este activ!");
        } else {
            System.out.println("DEBUG: Eldoria e goală - Jucătorul nu a acceptat încă misiunea de la Cronicar.");
        }
    }

    private void spawnBarlogul() {
        gp.enemy[0] = EnemyFactory.createEnemy("Fenris cel Alb", gp, gp.tileSize * 76 + 16, gp.tileSize * 36);
        gp.enemy[1] = EnemyFactory.createEnemy("Cioara", gp, gp.tileSize * 48, gp.tileSize * 26);
        gp.enemy[2] = EnemyFactory.createEnemy("Cioara", gp, gp.tileSize * 47, gp.tileSize * 27);
        gp.enemy[3] = EnemyFactory.createEnemy("Cioara", gp, gp.tileSize * 49, gp.tileSize * 25);
        gp.enemy[4] = EnemyFactory.createEnemy("Lup", gp, gp.tileSize * 45, gp.tileSize * 25);
        gp.enemy[5] = EnemyFactory.createEnemy("Lup", gp, gp.tileSize * 51, gp.tileSize * 26);
        gp.enemy[6] = EnemyFactory.createEnemy("Lup de Elită", gp, gp.tileSize * 21, gp.tileSize * 26);
        gp.enemy[7] = EnemyFactory.createEnemy("Lup de Elită", gp, gp.tileSize * 23, gp.tileSize * 28);
    }

    private void spawnAbisul() {
        // Quest 5 cere 5 inamici învinși în Abis, deci punem 6 troli, împrăștiați prin peșteră
        // (toate pozițiile sunt pe dale accesibile de la intrare)
        gp.enemy[0] = EnemyFactory.createEnemy("Trol", gp, gp.tileSize * 26, gp.tileSize * 37);
        gp.enemy[1] = EnemyFactory.createEnemy("Trol", gp, gp.tileSize * 38, gp.tileSize * 22);
        gp.enemy[2] = EnemyFactory.createEnemy("Feldgug", gp, gp.tileSize * 60, gp.tileSize * 51 + 32);
        gp.enemy[3] = EnemyFactory.createEnemy("Trol", gp, gp.tileSize * 53, gp.tileSize * 9);
        gp.enemy[4] = EnemyFactory.createEnemy("Trol", gp, gp.tileSize * 67, gp.tileSize * 28);
        gp.enemy[5] = EnemyFactory.createEnemy("Trol", gp, gp.tileSize * 20, gp.tileSize * 63);
        gp.enemy[6] = EnemyFactory.createEnemy("Trol", gp, gp.tileSize * 50, gp.tileSize * 69);
        // Quest 6: mini-boss-ul Abisului
        gp.enemy[7] = EnemyFactory.createEnemy("Trol de Elită", gp, gp.tileSize * 76, gp.tileSize * 62);
    }

    private void spawnCitadela() {
        gp.enemy[4] = EnemyFactory.createEnemy("Jean Plesa", gp, gp.tileSize * 15, gp.tileSize * 16);
    }

    private void spawnTundra() {
        gp.enemy[0] = EnemyFactory.createEnemy("Lup", gp, gp.tileSize * 61, gp.tileSize * 46);
        gp.enemy[1] = EnemyFactory.createEnemy("Lup", gp, gp.tileSize * 50, gp.tileSize * 60);
        gp.enemy[2] = EnemyFactory.createEnemy("Lup de Elită", gp, gp.tileSize * 32, gp.tileSize * 53);
        gp.enemy[3] = EnemyFactory.createEnemy("Lup de Elită", gp, gp.tileSize * 85, gp.tileSize * 14);
        gp.enemy[5] = EnemyFactory.createEnemy("Glaciara, Stăpânul Inelului de Gheață", gp, gp.tileSize * 51+32, gp.tileSize * 11);
    }

    private void spawnPodul() {
        gp.enemy[6] = EnemyFactory.createEnemy("Charon, Străjerul Umbrelor fără Tihnă", gp, gp.tileSize * 130, gp.tileSize * 24);
    }

    private void spawnCastelul() {
        gp.enemy[7] = EnemyFactory.createEnemy("Malakor, Monarhul Amurgului", gp, gp.tileSize * 27, gp.tileSize * 13);
    }

    public void setNPC(String currentZone) {
        Arrays.fill(gp.npc, null);
        // NPC-urile din res/Data/npcs.json se plasează după cei trei din cod (locurile 0-2)
        try {
            Entity.NPC.DataNPC.spawnForZone(gp, currentZone, 3);
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Nu s-au putut încărca NPC-urile din npcs.json", e);
        }

        if (currentZone.equals("Eldoria")) {

            // Bătrânul Cronicar stă la locul lui
            gp.npc[0] = new NPC_BatranulCronicar(gp, gp.tileSize * 15, gp.tileSize * 10);

            // GARDIANUL ABISULUI (Verifică Quest 4: "Deblochează Abisul")
            // Îl pui la coordonatele exacte din fața tile-ului de teleportare spre Abis (ID 43)
            gp.npc[1] = new NPC_Gardian(gp, gp.tileSize * 31, gp.tileSize * 37, 4,
                    "STAI PE LOC! Porțile Abisului de Piatră sunt închise din ordinul Consiliului.\nNu ai ce căuta aici, pleacă!",
                    "Ah, Eron! Bătrânul Înțelept ne-a trimis cuvânt despre faptele tale.\nPoarta este deschisă. Coboară cu prudență în Abis!");

            // GARDIANUL TUNDREI (Verifică Quest 11: "Stăpânul Gheții")
            // Îl pui în fața tile-ului de teleportare spre Tundra (ID 45)
            gp.npc[2] = new NPC_Gardian(gp, gp.tileSize * 33, gp.tileSize * 37, 11,
                    "Viscolul din Nord aduce doar moarte. Nu pot lăsa un nepregătit să treacă.\nÎntoarce-te când ești cu adevărat gata.",
                    "Runa lui Jean... Înțeleg. Ai dovedit că poți înfrunta urgia.\nTrecerea spre Tundra îți este permisă. Înarmează-te cu răbdare!");
        }
    }
}