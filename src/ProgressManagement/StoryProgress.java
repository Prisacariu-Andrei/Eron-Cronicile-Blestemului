package ProgressManagement;

import Main.GamePanel;
import Main.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Progresul poveștii: ce boss-i au fost învinși (flag-uri) și ce pasaje dintre zone s-au deschis.
 * Totul vine din res/Data/gates.json. Flag-urile se salvează (coloana saves.story_flags).
 * Artefactele (Colțul de Chihlimbar, Diamantul Nopții...) sunt obiecte cheie: le ai dacă flag-ul boss-ului e pus.
 */
public class StoryProgress {
    public static class Artifact {
        public final String id, name, flag, purpose;
        public final int color;
        Artifact(String id, String name, String flag, String purpose, int color) {
            this.id = id; this.name = name; this.flag = flag; this.purpose = purpose; this.color = color;
        }
    }

    public static class Gate {
        public final String id, zone, requires, blocked, closedText, openText;
        public final int tile, quest, guardCol, guardRow, asideCol, asideRow;
        Gate(Map<String, Object> d) {
            id = Json.str(d.get("id"), "");
            zone = Json.str(d.get("zone"), "Eldoria");
            requires = Json.str(d.get("requires"), "");
            blocked = Json.str(d.get("blocked"), "Calea este închisă.");
            closedText = Json.str(d.get("closedText"), "Trecerea este închisă.");
            openText = Json.str(d.get("openText"), "Trecerea este liberă.");
            tile = Json.integer(d.get("tile"), -1);
            quest = Json.integer(d.get("quest"), 0);
            List<Object> g = Json.list(d.get("guard")), a = Json.list(d.get("aside"));
            guardCol = Json.integer(g.size() > 0 ? g.get(0) : null, 0);
            guardRow = Json.integer(g.size() > 1 ? g.get(1) : null, 0);
            asideCol = Json.integer(a.size() > 0 ? a.get(0) : null, guardCol);
            asideRow = Json.integer(a.size() > 1 ? a.get(1) : null, guardRow);
        }
    }

    private final GamePanel gp;
    private final Set<String> flags = new LinkedHashSet<>();
    private final Map<String, String> bosses = new LinkedHashMap<>();
    private final List<Artifact> artifacts = new ArrayList<>();
    private final List<Gate> gates = new ArrayList<>();
    private String beastEnemy = "Lup", beastZone = "Eldoria", beastFlag = "fiara";

    public StoryProgress(GamePanel gp) {
        this.gp = gp;
        Map<String, Object> root = Json.map(Json.parseResource("/Data/gates.json"));
        for (Map.Entry<String, Object> e : Json.map(root.get("bosses")).entrySet()) bosses.put(e.getKey(), Json.str(e.getValue(), ""));
        Map<String, Object> beast = Json.map(root.get("beast"));
        beastEnemy = Json.str(beast.get("enemy"), beastEnemy);
        beastZone = Json.str(beast.get("zone"), beastZone);
        beastFlag = Json.str(beast.get("flag"), beastFlag);
        for (Object o : Json.list(root.get("artifacts"))) {
            Map<String, Object> d = Json.map(o);
            int color;
            try { color = Integer.parseInt(Json.str(d.get("color"), "#FFFFFF").replace("#", ""), 16); } catch (NumberFormatException e) { color = 0xFFFFFF; }
            artifacts.add(new Artifact(Json.str(d.get("id"), ""), Json.str(d.get("name"), "?"), Json.str(d.get("flag"), ""),
                    Json.str(d.get("purpose"), ""), color));
        }
        for (Object o : Json.list(root.get("gates"))) gates.add(new Gate(Json.map(o)));
    }

    public void reset() { flags.clear(); }

    public boolean has(String flag) { return flags.contains(flag); }

    public void set(String flag) { if (flag != null && !flag.isEmpty()) flags.add(flag); }

    public List<Gate> gates() { return gates; }

    public Gate gateForTile(String zone, int tile) {
        for (Gate g : gates) if (g.tile == tile && g.zone.equals(zone)) return g;
        return null;
    }

    public boolean isOpen(Gate g) { return g.requires.isEmpty() || has(g.requires); }

    /** Artefactele pe care Eron le are acum (în ordinea din gates.json). */
    public List<Artifact> keyItems() {
        List<Artifact> out = new ArrayList<>();
        for (Artifact a : artifacts) if (has(a.flag)) out.add(a);
        return out;
    }

    /**
     * Apelat la victorie (Player.winBattle). Pune flag-ul boss-ului sau al fiarei din Eldoria.
     * Returnează artefactul primit acum (sau null) pentru mesajul de pe ecran.
     */
    public Artifact onEnemyDefeated(String enemyName) {
        if (enemyName.equals(beastEnemy) && beastZone.equals(gp.tileM.currentZone)) set(beastFlag);
        String flag = bosses.get(enemyName);
        if (flag == null || has(flag)) return null;
        set(flag);
        for (Artifact a : artifacts) if (a.flag.equals(flag)) return a;
        return null;
    }

    /** Salvările fără flag-uri: le deducem din relicvele deja primite. */
    public void deriveFromRelics() {
        var p = gp.player;
        if (p.areDinteChilimbar) set("fenris");
        if (p.areDiamantulNoptii) set("feldgug");
        if (p.arePalosLegendar) set("jean");
        if (p.areInelDeGheata) set("glaciara");
        if (has("fenris") || has("feldgug") || has("jean") || has("glaciara")) set(beastFlag);
    }

    public String serialize() { return String.join(",", flags); }

    public void deserialize(String s) {
        flags.clear();
        if (s == null || s.isEmpty()) { deriveFromRelics(); return; }
        for (String f : s.split(",")) if (!f.isBlank()) flags.add(f.trim());
    }
}
