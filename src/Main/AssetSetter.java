package Main;

import Entity.Generali.*;
import Entity.Inamici.*;
import Entity.NPC.NPC_BatranulCronicar;

import java.util.Arrays;

public class AssetSetter {
    GamePanel gp;

    public AssetSetter(GamePanel gp) {
        this.gp = gp;
    }

    public void setEnemyForMap(String mapPath) {
        // Golește lista actuală de inamici
        Arrays.fill(gp.enemy, null);

        if (mapPath.contains("Eldoria")) {
            gp.enemy[0] = new Enemy_Lup(gp, gp.tileSize * 31, gp.tileSize * 37);
            gp.enemy[3] = new Enemy_Valerius(gp, gp.tileSize* 39, gp.tileSize*5);
        }
        else if (mapPath.contains("Barlogul")) {

            gp.enemy[0] = new Enemy_LupBoss(gp, gp.tileSize * 76+16, gp.tileSize * 36);
            gp.enemy[1] = new Enemy_Cioara(gp, gp.tileSize * 48, gp.tileSize * 26);
            gp.enemy[2] = new Enemy_Cioara(gp, gp.tileSize * 47, gp.tileSize * 27);
            gp.enemy[3] = new Enemy_Cioara(gp, gp.tileSize * 49, gp.tileSize * 25);
            gp.enemy[4] = new Enemy_LupElite(gp, gp.tileSize * 21, gp.tileSize * 26);
            gp.enemy[5] = new Enemy_LupElite(gp, gp.tileSize * 23, gp.tileSize * 28);
        }
        else if(mapPath.contains("Abisul")){
            gp.enemy[2] = new Enemy_Feldgug(gp, gp.tileSize* 60, gp.tileSize*51+32);
        }

        else if (mapPath.contains("Citadela")) {
            gp.enemy[4] = new Enemy_JeanPlesa(gp, gp.tileSize* 15, gp.tileSize*16);
        }

        else if (mapPath.contains("Tundra")) {
            gp.enemy[5] = new Enemy_Glaciara(gp, gp.tileSize* 40, gp.tileSize*6);        }

        else if (mapPath.contains("Podul")) {
            gp.enemy[6] = new Enemy_Charon(gp, gp.tileSize* 150, gp.tileSize*22);
        }

        else if (mapPath.contains("Castelul")) {
            gp.enemy[7] = new Enemy_Malakor(gp, gp.tileSize* 33, gp.tileSize*9);
        }
    }

    public void setNPC(String currentZone) {
        // 1. Golește lista actuală de NPC-uri
        Arrays.fill(gp.npc, null);

        // 2. Transformăm textul în litere mici pentru a evita erorile de scriere
        String zoneLower = currentZone != null ? currentZone.toLowerCase() : "";

        // 3. Adaugă NPC-uri în funcție de zonă
        if (zoneLower.contains("eldoria")) {
            gp.npc[0] = new NPC_BatranulCronicar(gp, gp.tileSize * 15, gp.tileSize * 10);
            System.out.println("DEBUG: NPC-ul Bătrânul Înțelept a fost plasat în Eldoria.");
        }
        else if(zoneLower.contains("barlog")){
            // gp.npc[0] = new NPC(gp,gp.tileSize,gp.tileSize)l
        }
    }
}