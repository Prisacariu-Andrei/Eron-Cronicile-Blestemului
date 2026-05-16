package Main;

import Entity.Inamici.*;
import Entity.NPC.NPC_BatranulCronicar;
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
        gp.enemy[1] = EnemyFactory.createEnemy("Malakor, Monarhul Amurgului", gp, gp.tileSize * 13, gp.tileSize * 13);
        gp.enemy[0] = EnemyFactory.createEnemy("Lup", gp, gp.tileSize * 31, gp.tileSize * 37);
        gp.enemy[3] = EnemyFactory.createEnemy("Valerius", gp, gp.tileSize * 39, gp.tileSize * 5);
    }

    private void spawnBarlogul() {
        gp.enemy[0] = EnemyFactory.createEnemy("Fenris cel Alb", gp, gp.tileSize * 76 + 16, gp.tileSize * 36);
        gp.enemy[1] = EnemyFactory.createEnemy("Cioara", gp, gp.tileSize * 48, gp.tileSize * 26);
        gp.enemy[2] = EnemyFactory.createEnemy("Cioara", gp, gp.tileSize * 47, gp.tileSize * 27);
        gp.enemy[3] = EnemyFactory.createEnemy("Cioara", gp, gp.tileSize * 49, gp.tileSize * 25);
        gp.enemy[4] = EnemyFactory.createEnemy("Lup de Elită", gp, gp.tileSize * 21, gp.tileSize * 26);
        gp.enemy[5] = EnemyFactory.createEnemy("Lup de Elită", gp, gp.tileSize * 23, gp.tileSize * 28);
    }

    private void spawnAbisul() {
        gp.enemy[2] = EnemyFactory.createEnemy("Feldgug", gp, gp.tileSize * 60, gp.tileSize * 51 + 32);
    }

    private void spawnCitadela() {
        gp.enemy[4] = EnemyFactory.createEnemy("Jean Plesa", gp, gp.tileSize * 15, gp.tileSize * 16);
    }

    private void spawnTundra() {
        gp.enemy[5] = EnemyFactory.createEnemy("Glaciara, Stăpânul Inelului de Gheață", gp, gp.tileSize * 51+32, gp.tileSize * 11);
    }

    private void spawnPodul() {
        gp.enemy[6] = EnemyFactory.createEnemy("Charon, Străjerul Umbrelor fără Tihnă", gp, gp.tileSize * 150, gp.tileSize * 22);
    }

    private void spawnCastelul() {
        gp.enemy[7] = EnemyFactory.createEnemy("Malakor, Monarhul Amurgului", gp, gp.tileSize * 33, gp.tileSize * 9);
    }

    public void setNPC(String currentZone) {
        Arrays.fill(gp.npc, null);
        String zone = currentZone != null ? currentZone.toLowerCase() : "";

        if (zone.contains("eldoria")) {
            gp.npc[0] = new NPC_BatranulCronicar(gp, gp.tileSize * 15, gp.tileSize * 10);
        }
    }
}