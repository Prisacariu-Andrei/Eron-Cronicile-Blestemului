package Entity.Player;

import Entity.Cards.CardFactory;
import Entity.Items.Card;
import Entity.Items.Rune;
import java.util.ArrayList;
import java.util.Collections;

public class Inventory {
    // Liste principale - Am păstrat ArrayList pentru acces rapid
    public final ArrayList<Card> cards = new ArrayList<>();
    public final ArrayList<Rune> runes = new ArrayList<>();
    public final ArrayList<Card> hand = new ArrayList<>();
    public final ArrayList<Card> discardPile = new ArrayList<>();

    // Constante neschimbate
    public final int MAX_RUNES = 5;
    public final int MAX_HAND_SIZE = 8;

    public void addItem(Card card) {
        if (card != null) {
            cards.add(card);
            // System.out.println("Carte adăugată în pachet: " + card.name);
        }
    }

    public void addRune(Rune rune) {
        if (rune != null && runes.size() < MAX_RUNES) {
            runes.add(rune);
            rune.isEquipped = true;
        } else {
            System.out.println("Inventarul de rune este plin!");
        }
    }

    public void addCardToDeck(String cardName, Player player) {
        // Folosim direct Factory-ul dacă ai făcut refactorizarea anterioară
        Card newCard = CardFactory.createCard(cardName, player);
        cards.add(newCard);
    }

    public void drawCard() {
        if (hand.size() < MAX_HAND_SIZE) {
            if (cards.isEmpty() && !discardPile.isEmpty()) {
                refreshDeckFromDiscard();
            }

            if (!cards.isEmpty()) {
                // Folosim remove(0) pentru compatibilitate sau removeFirst() dacă ești pe Java 21+
                hand.add(cards.removeFirst());
            }
        }
    }

    private void refreshDeckFromDiscard() {
        cards.addAll(discardPile);
        discardPile.clear();
        Collections.shuffle(cards);
    }

    public void discard(Card card) {
        if (card != null) {
            discardPile.add(card);
        }
    }

    public void prepareForBattle() {
        // Curățăm totul în Deck înainte de luptă
        cards.addAll(hand);
        hand.clear();
        cards.addAll(discardPile);
        discardPile.clear();

        Collections.shuffle(cards);

        // Tragem mâna inițială (8 cărți conform logicii tale)
        for (int i = 0; i < MAX_HAND_SIZE; i++) {
            drawCard();
        }
    }

    public void resetDeckAfterBattle() {
        cards.addAll(hand);
        cards.addAll(discardPile);

        hand.clear();
        discardPile.clear();
    }
}