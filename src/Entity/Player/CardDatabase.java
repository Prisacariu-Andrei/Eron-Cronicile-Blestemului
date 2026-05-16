package Entity.Player;

import Entity.Items.Card;
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

    // --- METODA NOUĂ PENTRU RECOMPENSE ---
    public List<Card> getRandomRewards(int count, Player player) {
        List<Card> rewards = new ArrayList<>();
        Set<String> pickedNames = new HashSet<>();

        // Ne asigurăm că nu alegem aceeași carte de mai multe ori în același pool
        while (pickedNames.size() < count && pickedNames.size() < cardRegistry.size()) {
            pickedNames.add(cardRegistry.get(rand.nextInt(cardRegistry.size())));
        }

        for (String name : pickedNames) {
            rewards.add(CardFactory.createCard(name, player));
        }
        return rewards;
    }

    public String getRandomCardName() {
        return cardRegistry.get(rand.nextInt(cardRegistry.size()));
    }
}