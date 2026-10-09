package Main;

import GameState.PlayState;

/**
 * Mod de test, OPRIT implicit. Se pornește cu opțiunea JVM  -Deron.debug=true  (IntelliJ: Run > Edit Configurations > VM options)
 * sau cu variabila de mediu ERON_DEBUG=1. Apoi, în joc, F9 deschide meniul "Sari la zonă": setează progresul necesar
 * (boss-ii dinainte, artefactele, aliatul, nivelul) și te teleportează în zona aleasă. Doar adaugă progres, nu scoate nimic.
 */
public final class DebugMode {
    public static final boolean ENABLED = Boolean.getBoolean("eron.debug") || "1".equals(System.getenv("ERON_DEBUG"));

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
        var p = gp.player;

        var q1 = gp.questManager.getQuestById(1, true);       // fără asta Eldoria rămâne fără inamici
        if (q1 != null) q1.isReceived = true;

        for (String f : z.flags()) gp.story.set(f);
        if (gp.story.has("fenris")) p.areDinteChilimbar = true;
        if (gp.story.has("feldgug") && !p.areDiamantulNoptii) p.primesteDiamantulNoptii();
        if (gp.story.has("jean")) {
            p.arePalosLegendar = true;
            gp.party.recruit("jean");
        }
        if (gp.story.has("glaciara")) p.areInelDeGheata = true;

        int guard = 0;
        while (gp.progression.level < z.level() && guard++ < 50) gp.progression.addXp(gp.progression.xpToNext());
        p.life = p.maxLife;
        p.coins = Math.max(p.coins, 200);
        p.notifyObservers();

        gp.changeState(new PlayState(gp));
        gp.eHandler.changeMap(z.map(), z.zone(), z.col(), z.row());
        gp.ui.message = "DEBUG: " + z.label() + " (nivel " + gp.progression.level + ")";
        gp.ui.messageOn = true;
    }
}
