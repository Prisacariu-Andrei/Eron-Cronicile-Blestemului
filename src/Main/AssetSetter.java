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
            gp.enemy[1] = new Enemy_LupBoss(gp, gp.tileSize * 15, gp.tileSize * 9 );
            gp.enemy[2] = new Enemy_Feldgug(gp, gp.tileSize* 18, gp.tileSize*9);
            gp.enemy[3] = new Enemy_Valerius(gp, gp.tileSize* 21, gp.tileSize*9);
            gp.enemy[4] = new Enemy_JeanPlesa(gp, gp.tileSize* 24, gp.tileSize*9);
            gp.enemy[5] = new Enemy_Glaciara(gp, gp.tileSize* 27, gp.tileSize*9);
            gp.enemy[6] = new Enemy_Charon(gp, gp.tileSize* 30, gp.tileSize*9);
            gp.enemy[7] = new Enemy_Malakor(gp, gp.tileSize* 33, gp.tileSize*9);
        }
        else if (mapPath.contains("Barlogul")) {

            gp.enemy[0] = new Enemy_LupBoss(gp, gp.tileSize * 76+16, gp.tileSize * 36);
            gp.enemy[1] = new Enemy_Cioara(gp, gp.tileSize * 48, gp.tileSize * 26);
            gp.enemy[2] = new Enemy_Cioara(gp, gp.tileSize * 47, gp.tileSize * 27);
            gp.enemy[3] = new Enemy_Cioara(gp, gp.tileSize * 49, gp.tileSize * 25);
            gp.enemy[4] = new Enemy_LupElite(gp, gp.tileSize * 21, gp.tileSize * 26);
            gp.enemy[5] = new Enemy_LupElite(gp, gp.tileSize * 23, gp.tileSize * 28);
        }
        // Adaugă restul zonelor aici...
    }

    public void setNPC(String currentZone) {

        // 1. Golește lista actuală de NPC-uri pentru a nu rămâne cei din zona veche
        Arrays.fill(gp.npc, null);

        // 2. Adaugă NPC-uri în funcție de zonă
        if (currentZone.contains("Eldoria")) {
            gp.npc[0] = new NPC_BatranulCronicar(gp, gp.tileSize * 15, gp.tileSize * 10);
            // Exemplu: gp.npc[1] = new NPC_Negustor(gp, ...);
        }

        else if (currentZone.contains("Barlogul")) {
            // Poate aici nu ai NPC-uri prietenoase, sau ai un prizonier
            // gp.npc[0] = new NPC_Prizonier(gp, gp.tileSize * 20, gp.tileSize * 25);
        }

        else if (currentZone.contains("Citadela")) {
            // gp.npc[0] = new NPC_Garda(gp, ...);
        }
    }
}