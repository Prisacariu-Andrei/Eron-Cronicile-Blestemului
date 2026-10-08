package Entity.Player;

import Entity.Items.Card;
import Entity.Cards.CardFactory;
import java.util.ArrayList;

public class CardManager {
    public static ArrayList<Card> getStartingDeck(Player player) {
        ArrayList<Card> starterDeck = new ArrayList<>();

        // Definim numele cărților de start
        String[] startNames = {
                "ParjolulSferic", "ParjolulSferic", "Dezolare", "PavazaGlaciara", "PavazaGlaciara",
                "ParjolulSferic", "ParjolulSferic", "SuflulVietii", "PavazaGlaciara", "PavazaGlaciara"
        };

        for (String name : startNames) {
            starterDeck.add(CardFactory.createCard(name, player));
        }

        return starterDeck;
    }
}