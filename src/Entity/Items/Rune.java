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
    public final int price;         // prețul de cumpărare
    public final Map<String, Integer> buffs;
    public boolean isEquipped;

    public Rune(String id, String name, String description, String glyph, int color, String rarity, int price, boolean bound, Map<String, Integer> buffs) {
        this.bound = bound;
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
        append(sb, "damage", "daune");
        append(sb, "maxLife", "viață maximă");
        append(sb, "turnShield", "armură pe tură");
        append(sb, "battleHeal", "viață după luptă");
        return sb.length() == 0 ? "fără efect" : sb.toString();
    }

    private void append(StringBuilder sb, String stat, String label) {
        int v = buff(stat);
        if (v == 0) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(v > 0 ? "+" : "").append(v).append(' ').append(label);
    }
}
