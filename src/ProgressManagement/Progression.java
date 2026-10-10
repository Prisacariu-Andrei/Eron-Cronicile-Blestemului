package ProgressManagement;

import Main.GamePanel;
import Main.Json;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * XP, niveluri și abilități. Toate valorile vin din res/Data/progression.json (nu sunt în cod).
 * Abilitățile se deblochează automat la un anumit nivel (fără puncte de abilitate).
 */
public class Progression {
    /** O abilitate din JSON. Efectele sunt aplicate de Player.useAbility. */
    public static class Ability {
        public String id, name, description;
        public int unlockLevel, cost;
        public Color color;
        public List<Map<String, Object>> effects = new ArrayList<>();
    }

    private final GamePanel gp;
    private final Map<String, Object> root;
    public final List<Ability> abilities = new ArrayList<>(); // ordonate după nivelul de deblocare (tastele 1, 2, 3...)

    public int level = 1;
    public int xp = 0;
    public boolean leveledThisBattle = false; // pentru ecranul de recompensă (resetat la începutul luptei)

    public Progression(GamePanel gp) {
        this.gp = gp;
        root = Json.map(Json.parseResource("/Data/progression.json"));
        for (Object o : Json.list(root.get("abilities"))) {
            Map<String, Object> m = Json.map(o);
            Ability a = new Ability();
            a.id = Json.str(m.get("id"), "ability");
            a.name = Json.str(m.get("name"), a.id);
            a.description = Json.str(m.get("description"), "");
            a.unlockLevel = Json.integer(m.get("unlockLevel"), 1);
            a.cost = Json.integer(m.get("cost"), 1);
            a.color = parseColor(Json.str(m.get("color"), "#FFFFFF"));
            for (Object e : Json.list(m.get("effects"))) a.effects.add(Json.map(e));
            abilities.add(a);
        }
        abilities.sort(Comparator.comparingInt(a -> a.unlockLevel));
    }

    public int maxLevel() { return Json.integer(root.get("maxLevel"), 10); }

    /** XP necesar ca să treci de nivelul curent (0 la nivelul maxim). */
    public int xpToNext() {
        if (level >= maxLevel()) return 0;
        return (int) Math.round(Json.number(root.get("xpBase"), 40) * Math.pow(Json.number(root.get("xpGrowth"), 1.35), level - 1));
    }

    public int enemyXp(String enemyName) {
        return Json.integer(Json.map(root.get("enemyXp")).get(enemyName), Json.integer(root.get("defaultEnemyXp"), 5));
    }

    public Ability abilityAt(int slot) {
        return slot >= 0 && slot < abilities.size() ? abilities.get(slot) : null;
    }

    /** Nivelul de la care o carte apare în magazin și în recompense (progression.json -> cardLevels; implicit 1). */
    public int cardUnlockLevel(String cardId) {
        return Json.integer(Json.map(root.get("cardLevels")).get(cardId), 1);
    }

    public boolean cardUnlocked(String cardId) { return level >= cardUnlockLevel(cardId); }

    /** Numele cărților care se deblochează exact la nivelul dat. */
    private List<String> cardsUnlockedAt(int lvl) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Object> e : Json.map(root.get("cardLevels")).entrySet()) {
            if (Json.integer(e.getValue(), 1) != lvl) continue;
            try { out.add(Entity.Cards.CardFactory.createCard(e.getKey(), gp.player).name); } catch (Exception ignored) { }
        }
        return out;
    }

    public boolean isUnlocked(Ability a) { return level >= a.unlockLevel; }

    /** Joc nou: nivel 1, fără XP, esența de bază. */
    public void reset() {
        level = 1;
        xp = 0;
        gp.player.maxMana = Json.integer(root.get("baseMana"), 3);
    }

    /** După încărcarea unei salvări: viața maximă vine deja din baza de date, dar esența maximă depinde de nivel. */
    public void restore(int savedLevel, int savedXp) {
        level = Math.max(1, Math.min(maxLevel(), savedLevel));
        xp = Math.max(0, savedXp);
        gp.player.maxMana = Json.integer(root.get("baseMana"), 3) + manaBonusesUpTo(level);
    }

    private int manaBonusesUpTo(int lvl) {
        int n = 0;
        for (Object o : Json.list(root.get("manaBonusAtLevels"))) if (Json.integer(o, 99) <= lvl) n++;
        return n;
    }

    /** Adaugă XP; la nevoie urcă unul sau mai multe niveluri. */
    public void addXp(int amount) {
        if (amount <= 0) return;
        xp += amount;
        while (level < maxLevel() && xp >= xpToNext()) {
            xp -= xpToNext();
            level++;
            levelUp();
        }
        if (level >= maxLevel()) xp = 0;
    }

    private void levelUp() {
        int hp = Json.integer(root.get("hpPerLevel"), 0);
        gp.player.maxLife += hp;
        if (hp > 0) gp.player.life = Json.bool(root.get("healOnLevelUp"), false) ? gp.player.maxLife : Math.min(gp.player.maxLife, gp.player.life + hp);
        int manaBefore = manaBonusesUpTo(level - 1), manaNow = manaBonusesUpTo(level);
        if (manaNow > manaBefore) gp.player.maxMana += manaNow - manaBefore;
        gp.player.notifyObservers();
        leveledThisBattle = true;

        StringBuilder msg = new StringBuilder("NIVEL " + level + "!");
        if (hp > 0) msg.append("  +").append(hp).append(" viață maximă");
        List<String> fresh = cardsUnlockedAt(level);
        if (!fresh.isEmpty()) msg.append("  |  Cărți noi (magazin și recompense): ").append(String.join(", ", fresh));
        if (manaNow > manaBefore) msg.append(", +1 esență");
        for (Ability a : abilities) if (a.unlockLevel == level) msg.append("  |  Abilitate nouă: ").append(a.name);
        gp.ui.message = msg.toString();
        gp.ui.messageOn = true;
        gp.ui.messageCounter = 0;
        gp.fx.levelUp();
    }

    private static Color parseColor(String hex) {
        try { return Color.decode(hex); } catch (Exception e) { return Color.WHITE; }
    }
}
