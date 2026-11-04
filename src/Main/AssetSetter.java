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
        var rule = Groups.spawnRule(zone);
        boolean ruled = !rule.isEmpty();
        int spacing = Json.integer(rule.get("spacing"), 6);
        var safe = Groups.safe(zone);
        java.util.List<int[]> candidates = ruled ? ruledSpots(rule, rng) : floorSpots(rng);
        if (ruled) candidates.removeIf(c -> inSafe(c, safe));   // la zonele cu regulă, cercurile safe se scot dintr-o dată

        java.util.List<int[]> taken = new java.util.ArrayList<>();
        for (var e : gp.enemy) if (e != null) taken.add(new int[]{e.worldX / gp.tileSize, e.worldY / gp.tileSize});
        int wanted = 0, placed = 0;
        boolean[] warned = {false};

        // 1. grupurile fixe ({ at, group }) primele: pe cea mai apropiată dală bună
        boolean[][] ok = null;
        for (Object o : specs) {
            var spec = Json.map(o);
            if (spec.get("at") == null) continue;
            wanted++;
            if (ok == null) ok = spotGrid(candidates, safe);
            var at = Json.list(spec.get("at"));
            int c0 = Json.integer(at.size() > 0 ? at.get(0) : null, 0), r0 = Json.integer(at.size() > 1 ? at.get(1) : null, 0);
            int[] spot = nearestValid(c0, r0, ok, taken, 4);
            if (spot == null) {
                GameExceptionHandler.getInstance().handleWarning("SPAWN " + zone + ": nicio dală bună lângă [" + c0 + "," + r0 + "], grupul lipsește", null);
                continue;
            }
            if (spot[0] != c0 || spot[1] != r0) System.out.println("SPAWN moved " + zone + " [" + c0 + "," + r0 + "] -> [" + spot[0] + "," + spot[1] + "]");
            if (placeGroup(zone, spot, members(spec.get("group")), taken)) placed++;
        }

        // 2. restul: grupuri amestecate ({ count, groups }) sau vechiul format ({ enemy, count, pack, with })
        for (Object o : specs) {
            var spec = Json.map(o);
            if (spec.get("at") != null) continue;
            int count = Json.integer(spec.get("count"), 1);
            var mixed = Json.list(spec.get("groups"));
            String id = Json.str(spec.get("enemy"), "");
            var pack = Json.list(spec.get("pack"));
            int pmin = Json.integer(pack.size() > 0 ? pack.get(0) : null, 1), pmax = Json.integer(pack.size() > 1 ? pack.get(1) : null, pmin);
            var with = Json.list(spec.get("with"));
            wanted += count;

            for (int k = 0; k < count; k++) {
                int[] spot = pickSpot(zone, ruled, candidates, taken, safe, spacing, warned);
                if (spot == null) break;
                java.util.List<String> names = new java.util.ArrayList<>();
                if (!mixed.isEmpty()) {
                    names = members(mixed.get(k % mixed.size()));
                } else {
                    int size = pmin + (pmax > pmin ? rng.nextInt(pmax - pmin + 1) : 0);
                    for (int i = 0; i < size; i++) names.add(id);
                    for (Object w : with) names.add(Json.str(w, ""));
                }
                if (placeGroup(zone, spot, names, taken)) placed++;
            }
        }
        int used = 0;
        for (var e : gp.enemy) if (e != null) used++;
        System.out.println("SPAWN " + zone + ": placed " + placed + "/" + wanted + " groups (enemy[] slots used " + used + "/" + gp.enemy.length + ")");
    }

    /** Numele dintr-un grup, cel mult groups.maxSize (primul e inamicul de pe hartă, restul vin în luptă). */
    private static java.util.List<String> members(Object group) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (Object m : Json.list(group)) if (out.size() < Groups.maxSize()) out.add(Json.str(m, ""));
        return out;
    }

    /** Pune un grup pe hartă; false dacă nu mai e loc în enemy[] sau primul nume nu există. */
    private boolean placeGroup(String zone, int[] spot, java.util.List<String> names, java.util.List<int[]> taken) {
        int slot = freeEnemySlot();
        if (names.isEmpty() || slot < 0) {
            if (slot < 0) GameExceptionHandler.getInstance().handleWarning("SPAWN " + zone + ": enemy[] e plin", null);
            return false;
        }
        var enemy = EnemyFactory.createEnemy(names.get(0), gp, gp.tileSize * spot[0], gp.tileSize * spot[1]);
        if (enemy == null) return false;
        for (int i = 1; i < names.size(); i++) enemy.packExtras.add(names.get(i));
        gp.enemy[slot] = enemy;
        taken.add(spot);
        return true;
    }

    /**
     * Locul următorului grup la întâmplare. Zonele fără regulă: primul candidat la cel puțin 5 dale de ceilalți (ca înainte).
     * Zonele cu regulă: cel mai îndepărtat punct, adică dala cu cea mai mare distanță Chebyshev minimă față de tot ce e deja pus;
     * sub spacing se acceptă tot ce e la cel puțin 3 dale (cu un avertisment), sub 3 grupul se sare.
     */
    private int[] pickSpot(String zone, boolean ruled, java.util.List<int[]> candidates, java.util.List<int[]> taken,
                           java.util.List<Object> safe, int spacing, boolean[] warned) {
        if (!ruled) {
            for (int[] cand : candidates) if (!nearAny(cand, taken, 5) && !inSafe(cand, safe)) return cand;
            return null;
        }
        int[] best = null;
        int bestD = -1;
        for (int[] cand : candidates) {
            int d = minDist(cand, taken);
            if (d > bestD) { bestD = d; best = cand; }
        }
        if (best == null || bestD < 3) {
            GameExceptionHandler.getInstance().handleWarning("SPAWN " + zone + ": nu mai e loc la cel puțin 3 dale, grupul se sare", null);
            return null;
        }
        if (bestD < spacing && !warned[0]) {
            warned[0] = true;
            GameExceptionHandler.getInstance().handleWarning("spacing reduced in " + zone, null);
        }
        return best;
    }

    /** Cea mai mică distanță Chebyshev de la p la un loc luat (mare dacă nu e niciunul). */
    private static int minDist(int[] p, java.util.List<int[]> taken) {
        int d = Integer.MAX_VALUE;
        for (int[] o : taken) d = Math.min(d, Math.max(Math.abs(o[0] - p[0]), Math.abs(o[1] - p[1])));
        return d;
    }

    private boolean[][] spotGrid(java.util.List<int[]> candidates, java.util.List<Object> safe) {
        boolean[][] ok = new boolean[gp.tileM.actualMapCols][gp.tileM.actualMapRows];
        for (int[] c : candidates) if (!inSafe(c, safe)) ok[c[0]][c[1]] = true;
        return ok;
    }

    /** Cea mai apropiată dală bună de (c0, r0), pe inele Chebyshev până la maxRing; nu se pune lângă un loc luat. Null dacă nu există. */
    private int[] nearestValid(int c0, int r0, boolean[][] ok, java.util.List<int[]> taken, int maxRing) {
        for (int ring = 0; ring <= maxRing; ring++) {
            int[] best = null;
            int bestSq = Integer.MAX_VALUE;
            for (int dc = -ring; dc <= ring; dc++) for (int dr = -ring; dr <= ring; dr++) {
                if (Math.max(Math.abs(dc), Math.abs(dr)) != ring) continue;
                int c = c0 + dc, r = r0 + dr;
                if (c < 0 || r < 0 || c >= ok.length || r >= ok[0].length || !ok[c][r]) continue;
                if (minDist(new int[]{c, r}, taken) < 2) continue;
                int sq = dc * dc + dr * dr;
                if (sq < bestSq) { bestSq = sq; best = new int[]{c, r}; }
            }
            if (best != null) return best;
        }
        return null;
    }

    /** Dalele permise de spawnRules: id-ul dalei și al celor 4 vecine ortogonale e în "tiles"; la cel puțin 2 dale de margine. Amestecate cu generatorul dat. */
    private java.util.List<int[]> ruledSpots(java.util.Map<String, Object> rule, java.util.Random rng) {
        var tm = gp.tileM;
        int cols = tm.actualMapCols, rows = tm.actualMapRows;
        java.util.Set<Integer> tiles = new java.util.HashSet<>();
        for (Object t : Json.list(rule.get("tiles"))) tiles.add(Json.integer(t, -1));
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (int c = 2; c < cols - 2; c++) for (int r = 2; r < rows - 2; r++) {
            if (tiles.contains(tm.mapTileNum[c][r]) && tiles.contains(tm.mapTileNum[c - 1][r]) && tiles.contains(tm.mapTileNum[c + 1][r])
                    && tiles.contains(tm.mapTileNum[c][r - 1]) && tiles.contains(tm.mapTileNum[c][r + 1])) out.add(new int[]{c, r});
        }
        java.util.Collections.shuffle(out, rng);
        return out;
    }

    /** Dalele libere din jurul cărora totul e podea (nu zid, nu teleportare), amestecate cu generatorul dat. */
    private java.util.List<int[]> floorSpots(java.util.Random rng) {
        var tm = gp.tileM;
        int cols = tm.actualMapCols, rows = tm.actualMapRows;

        // dalele "de pardoseală" = cele mai frecvente dintre cele fără coliziune (restul sunt teleportări, capcane, decor)
        java.util.Map<Integer, Integer> freq = new java.util.HashMap<>();
        for (int c = 0; c < cols; c++) for (int r = 0; r < rows; r++) {
            int id = tm.mapTileNum[c][r];
            if (id >= 0 && id < tm.tile.length && tm.tile[id] != null && !tm.tile[id].collision) freq.merge(id, 1, Integer::sum);
        }
        java.util.Set<Integer> floor = new java.util.HashSet<>();
        int walkable = freq.values().stream().mapToInt(Integer::intValue).sum();
        freq.entrySet().stream().filter(e -> e.getValue() * 20 >= walkable).sorted((a, b) -> b.getValue() - a.getValue()).limit(3).forEach(e -> floor.add(e.getKey()));

        java.util.List<int[]> candidates = new java.util.ArrayList<>();
        for (int c = 2; c < cols - 2; c++) for (int r = 2; r < rows - 2; r++) {
            boolean ok = true;
            for (int dc = -1; dc <= 1 && ok; dc++) for (int dr = -1; dr <= 1 && ok; dr++) ok = floor.contains(tm.mapTileNum[c + dc][r + dr]);
            if (ok) candidates.add(new int[]{c, r});
        }
        java.util.Collections.shuffle(candidates, rng);
        return candidates;
    }

    /** Cufere (res/Data/chests.json): pe dale libere, departe de intrări, inamici și NPC-uri; aceleași locuri la fiecare încărcare. */
    private void spawnChests(String zone) {
        var all = Json.list(Json.map(Json.parseResource("/Data/chests.json")).get("chests"));
        java.util.Random rng = new java.util.Random(zone.hashCode() * 31L + 13);
        java.util.List<int[]> candidates = null;
        java.util.List<int[]> taken = new java.util.ArrayList<>();
        for (var e : gp.enemy) if (e != null) taken.add(new int[]{e.worldX / gp.tileSize, e.worldY / gp.tileSize});
        for (var n : gp.npc) if (n != null && n.worldX >= 0) taken.add(new int[]{n.worldX / gp.tileSize, n.worldY / gp.tileSize});
        var safe = Groups.safe(zone);
        int index = 0;
        for (Object o : all) {
            var d = Json.map(o);
            if (!zone.equals(Json.str(d.get("zone"), ""))) continue;
            String id = zone + "_" + index++;
            if (candidates == null) candidates = floorSpots(rng);
            int[] spot = null;
            for (int[] cand : candidates) {
                if (nearAny(cand, taken, 6) || inSafe(cand, safe)) continue;
                spot = cand;
                break;
            }
            int slot = -1;
            for (int i = 0; i < gp.npc.length; i++) if (gp.npc[i] == null) { slot = i; break; }
            if (spot == null || slot < 0) continue;
            taken.add(spot);
            gp.npc[slot] = new Entity.NPC.NPC_Chest(gp, spot[0], spot[1], id, d);
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
        if (q1 != null && q1.isReceived && !gp.story.has("fiara")) {
            gp.enemy[0] = EnemyFactory.createEnemy("Lup Slăbit", gp, gp.tileSize * 31, gp.tileSize * 37);

            System.out.println("DEBUG: Eldoria populate - Quest-ul 1 este activ!");
        } else {
            System.out.println("DEBUG: Eldoria e goală - Jucătorul nu a acceptat încă misiunea de la Cronicar.");
        }
    }

    private void spawnBarlogul() {
        if (!gp.story.has("fenris")) gp.enemy[0] = EnemyFactory.createEnemy("Fenris cel Alb", gp, gp.tileSize * 78 + 16, gp.tileSize * 36);   // în fața grămezii de oase din bârlog
        // lupii, corbii și lupii de elită sunt grupuri puse de mână în res/Data/enemies.json (spawns -> Barlogul)
    }

    private void spawnAbisul() {
        // Quest 5 cere 5 inamici învinși în Abis, deci punem 6 troli, împrăștiați prin peșteră
        // (toate pozițiile sunt pe dale accesibile de la intrare)
        gp.enemy[0] = EnemyFactory.createEnemy("Trol", gp, gp.tileSize * 26, gp.tileSize * 37);
        gp.enemy[1] = EnemyFactory.createEnemy("Trol", gp, gp.tileSize * 38, gp.tileSize * 22);
        if (!gp.story.has("feldgug")) gp.enemy[2] = EnemyFactory.createEnemy("Feldgug", gp, gp.tileSize * 57, gp.tileSize * 53);   // pe covor, în fața tronului
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
        // lupii și Yeti-ii din labirint vin din res/Data/enemies.json (spawns); aici doar boss-ul
        if (!gp.story.has("glaciara")) gp.enemy[5] = EnemyFactory.createEnemy("Glaciara, Stăpânul Inelului de Gheață", gp, gp.tileSize * 51+32, gp.tileSize * 11);
    }

    private void spawnPodul() {
        if (!gp.story.has("charon")) gp.enemy[6] = EnemyFactory.createEnemy("Charon, Străjerul Umbrelor fără Tihnă", gp, gp.tileSize * 130, gp.tileSize * 24);
    }

    private void spawnCastelul() {
        spawnCastleStory();
    }

    /**
     * Finalul din Castelul de Obsidian (res/Data/castle.json): cei trei Pretorieni apar pe rând (fiecare după ce a căzut cel dinainte),
     * apoi Malakor. Se apelează la încărcarea zonei și după fiecare luptă câștigată în castel.
     */
    public void spawnCastleStory() {
        for (int i = 0; i < gp.enemy.length; i++) {      // scoatem lupta de poveste veche (se recreează mai jos)
            if (gp.enemy[i] != null && gp.enemy[i].introTitle != null) gp.enemy[i] = null;
        }
        var cfg = Json.map(Json.parseResource("/Data/castle.json"));
        Enemy next = null;
        for (Object o : Json.list(cfg.get("pretorians"))) {
            var d = Json.map(o);
            if (gp.story.has(Json.str(d.get("id"), ""))) continue;
            var at = Json.list(d.get("at"));
            next = EnemyFactory.createEnemy(Json.str(d.get("enemy"), ""), gp,
                    gp.tileSize * Json.integer(at.get(0), 27), gp.tileSize * Json.integer(at.get(1), 18));
            if (next == null) return;
            for (Object w : Json.list(d.get("with"))) next.packExtras.add(Json.str(w, ""));
            next.isStatic = true;
            next.introTitle = Json.str(d.get("title"), "");
            next.introSubtitle = Json.str(d.get("subtitle"), "");
            break;
        }
        if (next == null && !gp.story.has("malakor")) {
            var at = Json.list(Json.map(cfg.get("malakor")).get("at"));
            next = EnemyFactory.createEnemy("Malakor, Monarhul Amurgului", gp,
                    gp.tileSize * Json.integer(at.get(0), 27), gp.tileSize * Json.integer(at.get(1), 12));
        }
        int slot = freeEnemySlot();
        if (next != null && slot >= 0) gp.enemy[slot] = next;
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

        if (currentZone.equals("Castelul")) {   // Poarta de Obsidian din coridorul castelului (castle.json)
            var gate = Json.map(Json.map(Json.parseResource("/Data/castle.json")).get("gate"));
            gp.npc[0] = new Entity.NPC.NPC_Poarta(gp, Json.integer(gate.get("col"), 26), Json.integer(gate.get("row"), 24), Json.integer(gate.get("widthTiles"), 2));
        }

        // NPC-urile din res/Data/npcs.json se plasează în locurile rămase libere

        try {
            Entity.NPC.DataNPC.spawnForZone(gp, currentZone, 1);
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Nu s-au putut încărca NPC-urile din npcs.json", e);
        }
        spawnChests(currentZone);
        if (currentZone.equals("Tundra")) Entity.NPC.TundraPuzzle.spawn(gp);   // manivela, cristalul, oglinzile și poarta (res/Data/tundra.json)
    }
}