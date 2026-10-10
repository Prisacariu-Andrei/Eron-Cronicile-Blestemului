package Entity.Items;

import java.util.Map;

/** O rună din inventar. Datele (nume, efecte, preț) vin din res/Data/runes.json prin RuneFactory. */
public class Rune {
    public final String id;
    public final String name;
    public final String description;
    public final String glyph;      // flame | shield | heart | drop | fang (desenat în cod)
    public final int color;         // 0xRRGGBB
    public final String rarity;
    public final boolean bound;     // nu se poate vinde (ex. Manuscrisul Străvechi)
    public final boolean story;           // obiect de poveste: nu se echipează, nu dă bonusuri
    public final boolean grantsAbilities; // deținătorul poate folosi abilitățile
    public final int price;         // prețul de cumpărare
    public final Map<String, Integer> buffs;
    public boolean isEquipped;

    public Rune(String id, String name, String description, String glyph, int color, String rarity, int price, boolean bound, boolean story, boolean grantsAbilities, Map<String, Integer> buffs) {
        this.bound = bound;
        this.story = story;
        this.grantsAbilities = grantsAbilities;
        this.id = id;
        this.name = name;
        this.description = description;
        this.glyph = glyph;
        this.color = color;
        this.rarity = rarity;
        this.price = price;
        this.buffs = buffs;
        this.isEquipped = false;
    }

    public int buff(String stat) {
        return buffs.getOrDefault(stat, 0);
    }

    /** Efectele pe scurt, pentru tooltip: ex. "+2 daune, +15 viață maximă". */
    public String buffText() {
        StringBuilder sb = new StringBuilder();
        for (String stat : buffs.keySet()) append(sb, stat, label(stat));
        return sb.length() == 0 ? "fără efect" : sb.toString();
    }

    /** Numele efectului pentru tooltip; cheile cu punct (tagDamage.FULGER, statusPower.BURN) folosesc numele elementului/statusului din cards.json. */
    private static String label(String stat) {
        int dot = stat.indexOf('.');
        String key = dot < 0 ? stat : stat.substring(0, dot), arg = dot < 0 ? "" : stat.substring(dot + 1);
        return switch (key) {
            case "damage" -> "daune";
            case "maxLife" -> "viață maximă";
            case "turnShield" -> "armură pe tură";
            case "battleShield" -> "armură la începutul luptei";
            case "battleHeal" -> "viață după luptă";
            case "cardShield" -> "armură la cărțile cu armură";
            case "comboDamage" -> "paguba la combo";
            case "reactionDamage" -> "paguba reacțiilor";
            case "firstTurnMana" -> "esență în prima tură";
            case "killHeal" -> "viață per inamic doborât";
            case "tagDamage" -> "paguba cărților " + Entity.Cards.CardCatalog.tagLabel(arg);
            case "statusPower" -> "putere " + statusName(arg);
            case "statusTurns" -> "ture de " + statusName(arg);
            default -> stat;
        };
    }

    private static String statusName(String type) {
        try { return Entity.Cards.CardCatalog.statusLabel(Entity.StatusEffect.Type.valueOf(type)); } catch (IllegalArgumentException e) { return type; }
    }

    private void append(StringBuilder sb, String stat, String label) {
        int v = buff(stat);
        if (v == 0) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(v > 0 ? "+" : "").append(v).append(' ').append(label);
    }
}
