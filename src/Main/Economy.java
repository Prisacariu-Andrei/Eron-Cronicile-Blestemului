package Main;

import Entity.Cards.CardFactory;
import Entity.Items.Card;
import Entity.Player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Economia jocului: monede câștigate, prețuri în magazin. Toate valorile vin din res/Data/economy.json
 * (nu sunt scrise în cod), deci se reglează fără recompilare.
 */
public final class Economy {
    private static Economy instance;

    public static synchronized Economy get() {
        if (instance == null) instance = new Economy();
        return instance;
    }

    private final Map<String, Object> root;
    private final Random rng = new Random();
    private final Map<String, String> keyByDisplayName = new HashMap<>(); // "Pârjolul Sferic" -> "ParjolulSferic"

    private Economy() {
        root = Json.map(Json.parseResource("/Data/economy.json"));
    }

    public int startingCoins() { return Json.integer(root.get("startingCoins"), 0); }

    public int minDeckSize() { return Json.integer(root.get("minDeckSize"), 5); }

    /** Monedele primite pentru un inamic învins (numele dinaintea virgulei, ca în Player.winBattle). */
    public int enemyDrop(String enemyName) {
        List<Object> range = Json.list(Json.map(root.get("enemyDrops")).get(enemyName));
        if (range.size() < 2) range = Json.list(root.get("defaultEnemyDrop"));
        if (range.size() < 2) return 0;
        int min = Json.integer(range.get(0), 0), max = Json.integer(range.get(1), min);
        return max <= min ? min : min + rng.nextInt(max - min + 1);
    }

    public int questReward(int questId, boolean main) {
        Map<String, Object> table = Json.map(Json.map(root.get("questRewards")).get(main ? "main" : "side"));
        return Json.integer(table.get(String.valueOf(questId)), Json.integer(root.get("defaultQuestReward"), 0));
    }

    public int buyPrice(String cardKey) {
        return Json.integer(Json.map(root.get("cardPrices")).get(cardKey), Json.integer(root.get("defaultCardPrice"), 40));
    }

    /** Prețul de vânzare = prețul de cumpărare * sellRatio (în jur de 50%), minim 1. */
    public int sellPrice(String cardKey) {
        return Math.max(1, (int) Math.round(buyPrice(cardKey) * Json.number(root.get("sellRatio"), 0.5)));
    }

    /** Prețul de vânzare pentru o carte din pachet (se știe doar numele afișat, nu cheia din CardFactory). */
    public int sellPriceForCardName(String displayName, Player player) {
        String key = keyForCardName(displayName, player);
        return key == null ? 1 : sellPrice(key);
    }

    private synchronized String keyForCardName(String displayName, Player player) {
        if (keyByDisplayName.isEmpty()) {
            List<String> keys = new ArrayList<>(Json.map(root.get("cardPrices")).keySet());
            for (String key : keys) {
                try {
                    Card c = CardFactory.createCard(key, player);
                    if (c != null) keyByDisplayName.put(c.name, key);
                } catch (Exception ignored) { /* cheie necunoscută: o sărim */ }
            }
        }
        return keyByDisplayName.get(displayName);
    }
}
