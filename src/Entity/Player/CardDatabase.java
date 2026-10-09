package Entity.Player;

import Entity.Items.Card;
import Entity.Cards.CardCatalog;
import Entity.Cards.CardFactory;
import java.util.*;

public class CardDatabase {
    private static CardDatabase instance;
    private final List<String> cardRegistry;
    private final Random rand = new Random();

    private CardDatabase() {
        cardRegistry = new ArrayList<>(Arrays.asList(
                "CadereaStelelor", "DeslusireaRunelor", "FluxulStrabun",
                "FulgerDeSange", "GeamatulEcoului", "ManiorulIernii",
                "ParjolulSferic", "PavazaAmurgului", "PavazaGlaciara",
                "PovaraAnilor", "SuflulVietii", "UraganDeAzur"
        ));
    }

    public static CardDatabase getInstance() {
        if (instance == null) instance = new CardDatabase();
        return instance;
    }

    /**
     * Recompensele de după luptă: întâi cărțile legate de boss-ul învins (cards.json -> drop.bosses), apoi cărți alese după zonă
     * (drop.zones) și raritate (rarityWeights). Cărțile din cod au greutate fixă (baseCardWeight).
     */
    public List<Card> getRandomRewards(int count, Player player) {
        List<String> picked = new ArrayList<>();
        String zone = player.gp.tileM.currentZone;

        for (String id : CardCatalog.dataIds()) {   // cartea de boss
            if (picked.size() >= 1) break;
            for (Object b : Main.Json.list(Main.Json.map(CardCatalog.def(id).get("drop")).get("bosses"))) {
                if (player.lastDefeated.contains(String.valueOf(b))) { picked.add(id); break; }
            }
        }

        Map<String, Integer> pool = new LinkedHashMap<>();
        for (String id : cardRegistry) pool.put(id, CardCatalog.baseCardWeight());
        for (String id : CardCatalog.dataIds()) {
            var def = CardCatalog.def(id);
            for (Object z : Main.Json.list(Main.Json.map(def.get("drop")).get("zones"))) {
                if (String.valueOf(z).equals(zone)) pool.put(id, CardCatalog.rarityWeight(Main.Json.str(def.get("rarity"), "Comună")));
            }
        }
        for (String id : picked) pool.remove(id);

        while (picked.size() < count && !pool.isEmpty()) {
            int total = 0;
            for (int w : pool.values()) total += w;
            int roll = rand.nextInt(Math.max(1, total));
            for (Iterator<Map.Entry<String, Integer>> it = pool.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                roll -= e.getValue();
                if (roll < 0) { picked.add(e.getKey()); it.remove(); break; }
            }
        }

        List<Card> rewards = new ArrayList<>();
        for (String id : picked) rewards.add(CardFactory.createCard(id, player));
        return rewards;
    }

    public String getRandomCardName() {
        return cardRegistry.get(rand.nextInt(cardRegistry.size()));
    }
}