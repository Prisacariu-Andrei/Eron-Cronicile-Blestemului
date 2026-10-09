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
                            gp.fx.healSparkle();
                            System.out.println("Campfire Eldoria folosit la [" + playerCol + "," + playerRow + "]");
                            eventCooldown = 60;
                        }
                    }
                }

                // =====================================================================
                // BLOCAJ TELEPORTĂRI BAZAT PE QUEST-URI (Înlocuitor Gardian)
                // =====================================================================

                // Pasajele spre zonele 2-5 (res/Data/gates.json): se deschid doar după ce boss-ul zonei dinainte a fost învins
                var gate = gp.story.gateForTile("Eldoria", tileNum);
                if (gate != null) {
                    if (gp.story.isOpen(gate)) {
                        if (gate.quest > 0) {
                            var gq = gp.questManager.getQuestById(gate.quest, true);
                            if (gq != null && gq.isReceived && !gq.isCompleted) gp.questManager.updateQuestProgress(gate.quest, true, 1);
                        }
                        switch (gate.tile) {
                            case 42 -> changeMap("/Maps/Barlogul", "Barlogul", 47, 14);
                            case 43 -> changeMap("/Maps/Abisul", "Abisul", 13, 18);
                            case 44 -> changeMap("/Maps/Citadela", "Citadela", 66, 43);
                            case 45 -> changeMap("/Maps/Tundra", "Tundra", 87, 60);
                            case 46 -> changeMap("/Maps/Podul", "Podul", 5, 24);
                            default -> { }
                        }
                    } else {
                        gp.ui.message = gate.blocked;
                        gp.ui.messageOn = true;
                        eventCooldown = 30; // să nu spameze mesajul la fiecare cadru
                    }
                }

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
            case "Podul" ->{
                if (tileNum == 21){
                    changeMap("/Maps/Eldoria", "Eldoria", 62, 14);   // în fața pasajului spre Pod
                }
                else if(tileNum == 22){
                    changeMap("/Maps/Castelul", "Castelul", 6, 35);

                }
            }
            case "Castelul" -> {
                if (tileNum == 21) {
                    changeMap("/Maps/Eldoria", "Eldoria", 31, 39);
                }
            }
        }
    }

    /** Trecerea în altă zonă, cu ecranul de tranziție (imagine + nume + încărcare). Harta se încarcă în ZoneTransitionState. */
    public void changeMap(String mapPath, String zoneFolder, int nextCol, int nextRow) {
        if (gp.getCurrentState() instanceof GameState.ZoneTransitionState) return;
        gp.changeState(new GameState.ZoneTransitionState(gp, mapPath, zoneFolder, nextCol, nextRow));
    }

    /** Încarcă harta, inamicii și NPC-urile zonei și mută jucătorul (fără ecran de tranziție). */
    public void applyMap(String mapPath, String zoneFolder, int nextCol, int nextRow) {
        resetHealTiles();

        gp.tileM.currentZone = zoneFolder;

        System.out.println("DEBUG: Începe încărcarea texturilor pentru: " + zoneFolder);
        gp.tileM.loadZoneTiles(zoneFolder);

        System.out.println("DEBUG: Începe încărcarea matricei text din: " + mapPath);
        gp.tileM.loadMap(mapPath);

        System.out.println("DEBUG: Se populează inamicii pentru: " + mapPath);
        gp.aSetter.setEnemyForMap(mapPath);

        System.out.println("DEBUG: Se populează NPC-urile pentru: " + zoneFolder);
        gp.aSetter.setNPC(zoneFolder);

        gp.player.worldX = gp.tileSize * nextCol;
        gp.player.worldY = gp.tileSize * nextRow - 2;

        eventCooldown = 60;
        if ("Castelul".equals(zoneFolder)) gp.questManager.updateQuestProgress(13, true, 1);   // "Salvează tărâmul": ai ajuns la Castel
        gp.camera.snap();
        System.out.println("DEBUG: Schimbare mapă finalizată cu succes!");
    }

    public void resetHealTiles() {
        for(int i = 0; i < gp.maxWorldCol; i++) {
            for(int j = 0; j < gp.maxWorldRow; j++) {
                usedHealTiles[i][j] = false;
            }
        }
    }
}