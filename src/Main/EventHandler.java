package Main;

public class EventHandler {
    GamePanel gp;
    int eventCooldown = 0; // Previne teleportarea instantanee înapoi
    public boolean[][] usedHealTiles;  // Matrice care reține dacă un tile de heal a fost deja folosit (true/false)

    public EventHandler(GamePanel gp) {
        this.gp = gp;
        usedHealTiles = new boolean[gp.maxWorldCol][gp.maxWorldRow];
    }

    public void checkEvent() {
        // 1. Verificăm cooldown-ul pentru a evita teleportările în buclă sau damage-ul instantaneu
        if (eventCooldown > 0) {
            eventCooldown--;
            return;
        }

        // 2. Calculăm poziția jucătorului pe grila de tile-uri
        int playerCol = (gp.player.worldX + gp.player.hitBox.x + gp.player.hitBox.width/2) / gp.tileSize;
        int playerRow = (gp.player.worldY + gp.player.hitBox.y + gp.player.hitBox.height/2) / gp.tileSize;

        // 3. Siguranță pentru margini
        if(playerCol < 0 || playerCol >= gp.maxWorldCol || playerRow < 0 || playerRow >= gp.maxWorldRow) return;

        // 4. Luăm numărul tile-ului de sub jucător
        int tileNum = gp.tileM.mapTileNum[playerCol][playerRow];

        // ==========================================
        // LOGICĂ SPECIFICĂ PENTRU FIECARE ZONĂ
        // ==========================================
        switch (gp.tileM.currentZone) {

            case "Eldoria" -> {
                // AM ȘTERS resetHealTiles() DE AICI

                // DAMAGE: Spike-uri (ID 41)
                if (tileNum == 41) {
                    if (gp.player.life > 0) {
                        gp.player.damagePlayer(15);
                        System.out.println("HP: " + gp.player.life + " (Călcat pe Spike în Eldoria!)");
                        eventCooldown = 60;
                    }
                }

                // HEAL: Campfire (ID 40)
                if (tileNum == 40) {
                    if (!usedHealTiles[playerCol][playerRow]) {
                        if (gp.player.life < gp.player.maxLife) {
                            gp.player.life = Math.min(gp.player.life + 50, gp.player.maxLife);
                            gp.player.notifyObservers();
                            usedHealTiles[playerCol][playerRow] = true;
                            System.out.println("Campfire Eldoria folosit la [" + playerCol + "," + playerRow + "]");
                            eventCooldown = 60;
                        }
                    }
                }

                // TELEPORTARE:
                if (tileNum == 42) changeMap("/Maps/Barlogul", "Barlogul", 47, 14);
                if (tileNum == 43) changeMap("/Maps/Abisul", "Abisul", 13, 18);
                if (tileNum == 44) changeMap("/Maps/Citadela", "Citadela", 66, 43);
                if (tileNum == 45) changeMap("/Maps/Tundra", "Tundra", 87, 60);
                if (tileNum == 46) changeMap("/Maps/Podul", "Podul", 5, 24);
            }

            case "Barlogul" -> {
                if (tileNum == 1) {
                    changeMap("/Maps/Eldoria", "Eldoria", 34, 39);
                }
            }
            case "Abisul" -> {
                if (tileNum == 2) {
                    changeMap("/Maps/Eldoria", "Eldoria", 65, 34);
                }
            }
            case "Citadela" -> {
                if (tileNum == 15) {
                    changeMap("/Maps/Eldoria", "Eldoria", 11, 12);
                }
            }
            case "Tundra" -> {
                if (tileNum == 4) {
                    changeMap("/Maps/Eldoria", "Eldoria", 12, 39);
                }
            }
            case "Castelul" -> {
                if (tileNum == 1) {
                    changeMap("/Maps/Eldoria", "Eldoria", 31, 39);
                }
            }
        }
    }

    public void changeMap(String mapPath, String zoneFolder, int nextCol, int nextRow) {

        // --- RESETĂM HEAL-URILE CÂND JUCĂTORUL SCHIMBĂ MAPA ---
        resetHealTiles();

        // 1. Schimbăm imaginile hărții
        gp.tileM.loadZoneTiles(zoneFolder);

        // 2. Schimbăm matricea hărții (.txt)
        gp.tileM.loadMap(mapPath);

        // 3. ACTUALIZĂM ENTITĂȚILE
        gp.aSetter.setEnemyForMap(mapPath);
        gp.aSetter.setNPC(zoneFolder);

        // 4. Mutăm jucătorul
        gp.player.worldX = gp.tileSize * nextCol;
        gp.player.worldY = gp.tileSize * nextRow - 2;

        eventCooldown = 60;
    }

    public void resetHealTiles() {
        for(int i = 0; i < gp.maxWorldCol; i++) {
            for(int j = 0; j < gp.maxWorldRow; j++) {
                usedHealTiles[i][j] = false;
            }
        }
    }
}