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

        String zone = zoneOf(mapPath);
        // În Eldoria nu apar monștri înainte ca jucătorul să primească prima misiune de la Cronicar
        var q1 = gp.questManager.getQuestById(1, true);
        if (zone != null && (!zone.equals("Eldoria") || (q1 != null && q1.isReceived))) spawnExtras(zone);
    }

    private static String zoneOf(String mapPath) {
        for (String z : new String[]{"Eldoria", "Barlogul", "Abisul", "Citadela", "Tundra", "Podul", "Castelul"}) {
            if (mapPath.contains(z)) return z;
        }
        return null;
    }

    private int freeEnemySlot() {
        for (int i = 0; i < gp.enemy.length; i++) if (gp.enemy[i] == null) return i;
        return -1;
    }

    /**
     * Inamicii adăugați din res/Data/enemies.json (spawns): grupuri pe dale libere, alese la întâmplare dar la fel la fiecare
     * încărcare (generatorul pornește din numele zonei). Se evită intrările în zonă, NPC-urile, ceilalți inamici și marginile.
     */
    private void spawnExtras(String zone) {
        var specs = Groups.spawns(zone);
        if (specs.isEmpty()) return;
        java.util.Random rng = new java.util.Random(zone.hashCode() * 31L + 7);
        var tm = gp.tileM;
        int cols = tm.actualMapCols, rows = tm.actualMapRows;

        // dalele "de pardoseală" = cele mai frecvente dintre cele fără coliziune (restul sunt teleportări, capcane, decor)
        java.util.Map<Integer, Integer> freq = new java.util.HashMap<>();
        for (int c = 0; c < cols; c++) for (int r = 0; r < rows; r++) {
            int id = tm.mapTileNum[c][r];
            if (id >= 0 && id < tm.tile.length && tm.tile[id] != null && !tm.tile[id].collision) freq.merge(id, 1, Integer::sum);
        }
        java.util.Set<Integer> floor = new java.util.HashSet<>();
        freq.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(3).forEach(e -> floor.add(e.getKey()));

        java.util.List<int[]> candidates = new java.util.ArrayList<>();
        for (int c = 2; c < cols - 2; c++) for (int r = 2; r < rows - 2; r++) {
            boolean ok = true;
            for (int dc = -1; dc <= 1 && ok; dc++) for (int dr = -1; dr <= 1 && ok; dr++) ok = floor.contains(tm.mapTileNum[c + dc][r + dr]);
            if (ok) candidates.add(new int[]{c, r});
        }
        java.util.Collections.shuffle(candidates, rng);

        java.util.List<int[]> taken = new java.util.ArrayList<>();
        for (var e : gp.enemy) if (e != null) taken.add(new int[]{e.worldX / gp.tileSize, e.worldY / gp.tileSize});
        var safe = Groups.safe(zone);

        for (Object o : specs) {
            var spec = Json.map(o);
            String id = Json.str(spec.get("enemy"), "");
            int count = Json.integer(spec.get("count"), 1);
            var pack = Json.list(spec.get("pack"));
            int pmin = Json.integer(pack.size() > 0 ? pack.get(0) : null, 1), pmax = Json.integer(pack.size() > 1 ? pack.get(1) : null, pmin);
            var with = Json.list(spec.get("with"));

            for (int k = 0; k < count; k++) {
                int[] spot = null;
                for (int[] cand : candidates) {
                    if (nearAny(cand, taken, 5) || inSafe(cand, safe)) continue;
                    spot = cand;
                    break;
                }
                int slot = freeEnemySlot();
                if (spot == null || slot < 0) break;
                taken.add(spot);

                var enemy = EnemyFactory.createEnemy(id, gp, gp.tileSize * spot[0], gp.tileSize * spot[1]);
                if (enemy == null) continue;
                int size = pmin + (pmax > pmin ? rng.nextInt(pmax - pmin + 1) : 0);
                for (int i = 1; i < size; i++) enemy.packExtras.add(id);
                for (Object w : with) enemy.packExtras.add(Json.str(w, ""));
                gp.enemy[slot] = enemy;
            }
        }
    }

    private boolean nearAny(int[] p, java.util.List<int[]> others, int dist) {
        for (int[] o : others) if (Math.abs(o[0] - p[0]) < dist && Math.abs(o[1] - p[1]) < dist) return true;
        return false;
    }

    private boolean inSafe(int[] p, java.util.List<Object> safe) {
        for (Object s : safe) {
            var l = Json.list(s);
            if (l.size() < 3) continue;
            int dc = p[0] - Json.integer(l.get(0), 0), dr = p[1] - Json.integer(l.get(1), 0), rad = Json.integer(l.get(2), 0);
            if (dc * dc + dr * dr <= rad * rad) return true;
        }
        return false;
    }

    // --- Metode dedicate pentru fiecare zonă ---

    private void spawnEldoria() {
        // Luăm informațiile despre Quest-ul 1 (Tutorial) din QuestManager
        var q1 = gp.questManager.getQuestById(1, true);

        // Inamicii se vor încărca DOAR dacă jucătorul a vorbit cu Cronicarul și a primit quest-ul
        if (q1 != null && q1.isReceived) {
            gp.enemy[0] = EnemyFactory.createEnemy("Lup Slăbit", gp, gp.tileSize * 31, gp.tileSize * 37);

            System.out.println("DEBUG: Eldoria populate - Quest-ul 1 este activ!");
        } else {
            System.out.println("DEBUG: Eldoria e goală - Jucătorul nu a acceptat încă misiunea de la Cronicar.");
        }
    }

    private void spawnBarlogul() {
        if (!gp.story.has("fenris")) gp.enemy[0] = EnemyFactory.createEnemy("Fenris cel Alb", gp, gp.tileSize * 76 + 16, gp.tileSize * 36);
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
        if (!gp.story.has("feldgug")) gp.enemy[2] = EnemyFactory.createEnemy("Feldgug", gp, gp.tileSize * 60, gp.tileSize * 51 + 32);
        gp.enemy[3] = EnemyFactory.createEnemy("Trol", gp, gp.tileSize * 53, gp.tileSize * 9);
        gp.enemy[4] = EnemyFactory.createEnemy("Trol", gp, gp.tileSize * 67, gp.tileSize * 28);
        gp.enemy[5] = EnemyFactory.createEnemy("Trol", gp, gp.tileSize * 20, gp.tileSize * 63);
        gp.enemy[6] = EnemyFactory.createEnemy("Trol", gp, gp.tileSize * 50, gp.tileSize * 69);
        // Quest 6: mini-boss-ul Abisului
        gp.enemy[7] = EnemyFactory.createEnemy("Trol de Elită", gp, gp.tileSize * 76, gp.tileSize * 62);
    }

    private void spawnCitadela() {
        if (gp.story.has("jean")) return; // Jean a fost învins și s-a alăturat echipei: nu mai stă în Citadelă
        gp.enemy[4] = EnemyFactory.createEnemy("Jean Plesa", gp, gp.tileSize * 15, gp.tileSize * 16);
    }

    private void spawnTundra() {
        gp.enemy[0] = EnemyFactory.createEnemy("Lup", gp, gp.tileSize * 61, gp.tileSize * 46);
        gp.enemy[1] = EnemyFactory.createEnemy("Lup", gp, gp.tileSize * 50, gp.tileSize * 60);
        gp.enemy[2] = EnemyFactory.createEnemy("Lup de Elită", gp, gp.tileSize * 32, gp.tileSize * 53);
        gp.enemy[3] = EnemyFactory.createEnemy("Lup de Elită", gp, gp.tileSize * 85, gp.tileSize * 14);
        if (!gp.story.has("glaciara")) gp.enemy[5] = EnemyFactory.createEnemy("Glaciara, Stăpânul Inelului de Gheață", gp, gp.tileSize * 51+32, gp.tileSize * 11);
    }

    private void spawnPodul() {
        if (!gp.story.has("charon")) gp.enemy[6] = EnemyFactory.createEnemy("Charon, Străjerul Umbrelor fără Tihnă", gp, gp.tileSize * 130, gp.tileSize * 24);
    }

    private void spawnCastelul() {
        gp.enemy[7] = EnemyFactory.createEnemy("Malakor, Monarhul Amurgului", gp, gp.tileSize * 27, gp.tileSize * 13);
    }

    public void setNPC(String currentZone) {
        Arrays.fill(gp.npc, null);

        if (currentZone.equals("Eldoria")) {
            // Bătrânul Cronicar stă la locul lui
            gp.npc[0] = new NPC_BatranulCronicar(gp, gp.tileSize * 15, gp.tileSize * 10);

            // Câte o Gardă de Fier în fața fiecărui pasaj (res/Data/gates.json); se retrage când boss-ul zonei a fost învins
            int slot = 1;
            for (var g : gp.story.gates()) {
                if (g.zone.equals(currentZone) && slot < gp.npc.length) gp.npc[slot++] = new NPC_Gardian(gp, g);
            }
        }

        // NPC-urile din res/Data/npcs.json se plasează în locurile rămase libere
        try {
            Entity.NPC.DataNPC.spawnForZone(gp, currentZone, 1);
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Nu s-au putut încărca NPC-urile din npcs.json", e);
        }
    }
}