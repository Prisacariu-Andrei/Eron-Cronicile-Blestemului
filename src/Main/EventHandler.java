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
                            System.out.println("Campfire Eldoria folosit la [" + playerCol + "," + playerRow + "]");
                            eventCooldown = 60;
                        }
                    }
                }

                // =====================================================================
                // BLOCAJ TELEPORTĂRI BAZAT PE QUEST-URI (Înlocuitor Gardian)
                // =====================================================================

                // TELEPORTARE BÂRLOG (ID 42) - Permisă dacă ai primit Quest 2
                if (tileNum == 42) {
                    var q2 = gp.questManager.getQuestById(2, true);
                    if (q2 != null && q2.isReceived) {
                        changeMap("/Maps/Barlogul", "Barlogul", 47, 14);
                    } else {
                        gp.ui.message = "Bârlogul e blocat! Vorbește cu Bătrânul Înțelept.";
                        gp.ui.messageOn = true;
                        eventCooldown = 30; // Cooldown mic să nu spameze mesajul la fiecare cadru
                    }
                }

                // TELEPORTARE ABIS (ID 43) - Quest 4: Trebuie să ai permisiunea/să fii la Quest 4 primit
                if (tileNum == 43) {
                    var q4 = gp.questManager.getQuestById(4, true);
                    if (q4 != null && q4.isReceived) {
                        // Dacă pășești aici și ai misiunea 4 activă, o completăm automat (în loc de gardian)!
                        if (!q4.isCompleted) {
                            gp.questManager.updateQuestProgress(4, true, 1);
                        }
                        changeMap("/Maps/Abisul", "Abisul", 13, 18);
                    } else {
                        gp.ui.message = "Porțile Abisului sunt pecetluite de Santinele!";
                        gp.ui.messageOn = true;
                        eventCooldown = 30;
                    }
                }

                // TELEPORTARE CITADELA (ID 44) - Quest 8: Descoperă Citadela Haiducilor
                if (tileNum == 44) {
                    var q8 = gp.questManager.getQuestById(8, true);
                    if (q8 != null && q8.isReceived) {
                        // Dacă ai ajuns aici, înseamnă că ai descoperit locația pentru Quest 8!
                        if (!q8.isCompleted) {
                            gp.questManager.updateQuestProgress(8, true, 1);
                        }
                        changeMap("/Maps/Citadela", "Citadela", 66, 43);
                    } else {
                        gp.ui.message = "Drumul spre Citadelă este periculos și ascuns acum.";
                        gp.ui.messageOn = true;
                        eventCooldown = 30;
                    }
                }

                // TELEPORTARE TUNDRA (ID 45) - Quest 11: Stăpânul Gheții (Glaciara)
                if (tileNum == 45) {
                    var q11 = gp.questManager.getQuestById(11, true);
                    if (q11 != null && q11.isReceived) {
                        changeMap("/Maps/Tundra", "Tundra", 87, 60);
                    } else {
                        gp.ui.message = "Un viscol teribil blochează trecerea spre Nord!";
                        gp.ui.messageOn = true;
                        eventCooldown = 30;
                    }
                }

                // TELEPORTARE POD / CASTEL (ID 46) - Quest 12: Învinge-l pe Charon
                if (tileNum == 46) {
                    var q12 = gp.questManager.getQuestById(12, true);
                    if (q12 != null && q12.isReceived) {
                        changeMap("/Maps/Podul", "Podul", 5, 24); // Sau direct spre "Castelul" depinzând de mapa ta
                    } else {
                        gp.ui.message = "Calea către Castelul de Obsidian este interzisă muritorilor.";
                        gp.ui.messageOn = true;
                        eventCooldown = 30;
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

        gp.tileM.currentZone = zoneFolder;

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

        // =====================================================================
        // 5. REFRESH AUDIO: Forțăm starea curentă să prindă noua piesă din zonă
        // =====================================================================
        gp.changeState(gp.getCurrentState());
        // =====================================================================
    }

    public void resetHealTiles() {
        for(int i = 0; i < gp.maxWorldCol; i++) {
            for(int j = 0; j < gp.maxWorldRow; j++) {
                usedHealTiles[i][j] = false;
            }
        }
    }
}