package Main;

import GameState.PlayState;

/**
 * Panoul de admin (F9, din joc): sari la o zonă, deblochează toate zonele, sari peste misiuni, treci prin pereți (noclip),
 * fără lupte, nemurire, Runa Adminului (+1000 daune), monede, nivel maxim, viață plină. Doar adaugă progres, nu scoate nimic.
 * E pornit implicit; îl oprești cu opțiunea JVM  -Deron.debug=false  sau cu variabila de mediu ERON_DEBUG=0.
 */
public final class DebugMode {
    public static final boolean ENABLED = !(("false".equalsIgnoreCase(System.getProperty("eron.debug"))) || "0".equals(System.getenv("ERON_DEBUG")));

    /** Comutatoarele panoului (nu se salvează). */
    public static boolean noclip, noFights, god;

    public static final String ADMIN_RUNE = "admin";
    private static final String[] ALL_FLAGS = {"fiara", "fenris", "feldgug", "jean", "glaciara"};

    /** Zonele din meniu, în ordinea poveștii. flags = ce boss-i au fost înfrânți ca să ajungi aici (cumulativ, în ordine). */
    public record Zone(String label, String map, String zone, int col, int row, int level, String... flags) {}

    public static final Zone[] ZONES = {
            new Zone("Eldoria",              "/Maps/Eldoria",  "Eldoria",  15, 12, 1),
            new Zone("Munții de Miazăzi",    "/Maps/Barlogul", "Barlogul", 47, 14, 3, "fiara"),
            new Zone("Abisul de Piatră",     "/Maps/Abisul",   "Abisul",   13, 18, 5, "fiara", "fenris"),
            new Zone("Citadela Haiducilor",  "/Maps/Citadela", "Citadela", 66, 43, 7, "fiara", "fenris", "feldgug"),
            new Zone("Tundra Tăcerii",       "/Maps/Tundra",   "Tundra",   87, 60, 8, "fiara", "fenris", "feldgug", "jean"),
            new Zone("Podul Umbrelor",       "/Maps/Podul",    "Podul",     5, 24, 9, "fiara", "fenris", "feldgug", "jean", "glaciara"),
            new Zone("Castelul de Obsidian", "/Maps/Castelul", "Castelul",  6, 35, 10, "fiara", "fenris", "feldgug", "jean", "glaciara"),
    };

    private DebugMode() {}

    /** Dă progresul zonei și te mută acolo (cu ecranul de tranziție). */
    public static void skipTo(GamePanel gp, int index) {
        Zone z = ZONES[Math.max(0, Math.min(index, ZONES.length - 1))];
        grant(gp, z.level(), z.flags());
        gp.changeState(new PlayState(gp));
        gp.eHandler.changeMap(z.map(), z.zone(), z.col(), z.row());
        gp.ui.message = "DEBUG: " + z.label() + " (nivel " + gp.progression.level + ")";
        gp.ui.messageOn = true;
    }

    /** Deblochează toate zonele (boss-ii, artefactele, aliatul) fără să te teleporteze. */
    public static void unlockAllZones(GamePanel gp) {
        grant(gp, 0, ALL_FLAGS);
        if ("Eldoria".equals(gp.tileM.currentZone)) gp.aSetter.setNPC("Eldoria");   // Gărzile din fața pasajelor se retrag
        msg(gp, "Toate zonele sunt deblocate");
    }

    /** Predă toate misiunile (cele din carte și cele vechi), cu recompense. */
    public static void skipAllQuests(GamePanel gp) {
        for (var d : gp.questLog.all()) gp.questLog.adminSkip(d);
        for (var list : java.util.List.of(gp.questManager.mainQuests, gp.questManager.sideQuests)) {
            for (var q : list) { q.isReceived = true; gp.questManager.updateQuestProgress(q.id, list == gp.questManager.mainQuests, q.requiredAmount); }
        }
        msg(gp, "Toate misiunile au fost sărite");
    }

    /** Predă următoarea misiune din carte. */
    public static void skipNextQuest(GamePanel gp) {
        var d = gp.questLog.adminNext();
        if (d == null) { msg(gp, "Nu mai sunt misiuni de sărit"); return; }
        gp.questLog.adminSkip(d);
        msg(gp, "Misiune sărită: " + d.title);
    }

    /** Pune sau scoate Runa Adminului (+1000 daune), fără să ocupe slot. */
    public static void toggleAdminRune(GamePanel gp) {
        var inv = gp.player.inventory;
        for (var r : new java.util.ArrayList<>(inv.runes)) {
            if (r.id.equals(ADMIN_RUNE)) {
                r.isEquipped = false;
                inv.runes.remove(r);
                gp.player.refreshRunes();
                msg(gp, "Runa Adminului scoasă");
                return;
            }
        }
        var r = Entity.Runes.RuneFactory.createRune(ADMIN_RUNE);
        if (r == null) return;
        r.isEquipped = true;
        inv.runes.add(r);
        gp.player.refreshRunes();
        msg(gp, "Runa Adminului: +1000 daune");
    }

    public static boolean hasAdminRune(GamePanel gp) { return gp.player.inventory.ownsRune(ADMIN_RUNE); }

    public static void addCoins(GamePanel gp, int n) { gp.player.addCoins(n); msg(gp, "+" + n + " monede"); }

    public static void maxLevel(GamePanel gp) {
        int guard = 0;
        while (gp.progression.level < gp.progression.maxLevel() && guard++ < 100) gp.progression.addXp(gp.progression.xpToNext());
        gp.player.life = gp.player.maxLife;
        gp.player.notifyObservers();
        msg(gp, "Nivel " + gp.progression.level);
    }

    public static void heal(GamePanel gp) { gp.player.life = gp.player.maxLife; gp.player.notifyObservers(); msg(gp, "Viață plină"); }

    private static void msg(GamePanel gp, String text) { gp.ui.message = "ADMIN: " + text; gp.ui.messageOn = true; }

    /** Boss-ii, artefactele și aliatul pentru steagurile date; urcă la nivelul cerut (0 = nu schimbă nivelul). */
    private static void grant(GamePanel gp, int level, String... flags) {
        var p = gp.player;

        var q1 = gp.questManager.getQuestById(1, true);       // fără asta Eldoria rămâne fără inamici
        if (q1 != null) q1.isReceived = true;

        for (String f : flags) gp.story.set(f);
        if (gp.story.has("fenris")) p.areDinteChilimbar = true;
        if (gp.story.has("feldgug") && !p.areDiamantulNoptii) p.primesteDiamantulNoptii();
        if (gp.story.has("jean")) {
            p.arePalosLegendar = true;
            gp.party.recruit("jean");
        }
        if (gp.story.has("glaciara")) p.areInelDeGheata = true;

        int guard = 0;
        while (gp.progression.level < level && guard++ < 50) gp.progression.addXp(gp.progression.xpToNext());
        p.life = p.maxLife;
        p.coins = Math.max(p.coins, 200);
        p.notifyObservers();
    }
}
