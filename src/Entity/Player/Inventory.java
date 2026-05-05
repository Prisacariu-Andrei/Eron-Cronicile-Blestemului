package Entity.Player;

import Entity.Items.Card;
import Entity.Items.Rune; // Importăm noua clasă
import java.util.ArrayList;
import java.util.Collections;

public class Inventory {
    // Pachetul de cărți (Deck)
    public ArrayList<Card> cards = new ArrayList<>();

    // Pachetul de rune (Pasive)
    public ArrayList<Rune> runes = new ArrayList<>();

    // Mâna curentă (Max 8)
    public ArrayList<Card> hand = new ArrayList<>();
    // Cărțile folosite (Discard)
    public ArrayList<Card> discardPile = new ArrayList<>();

    public final int MAX_RUNES = 5;

    public void addItem(Card card) {
        cards.add(card);
        System.out.println("Carte adăugată în pachet: " + card.name);
    }

    // Metodă separată pentru adăugarea unei rune în inventar
    public void addRune(Rune rune) {
        if (runes.size() < MAX_RUNES) {
            runes.add(rune);
            rune.isEquipped = true;
            System.out.println("Runa echipată: " + rune.name);
        } else {
            System.out.println("Inventarul de rune este plin!");
        }
    }

    public void addCardToDeck(String cardName) {
        Card newCard = CardManager.createCard(cardName);
        if (newCard != null) {
            cards.add(newCard);
        }
    }

    public void drawCard() {
        if (hand.size() < 8) {
            if (cards.isEmpty()) {
                refreshDeckFromDiscard();
            }

            if (!cards.isEmpty()) {
                hand.add(cards.removeFirst());
            }
        }
    }

    private void refreshDeckFromDiscard() {
        System.out.println("Deck gol! Reciclăm discard pile...");
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
        cards.addAll(hand);
        hand.clear();
        cards.addAll(discardPile);
        discardPile.clear();

        Collections.shuffle(cards);

        for(int i = 0; i < 5; i++) {
            drawCard();
        }
    }

    public void resetDeckAfterBattle() {
        cards.addAll(hand);
        cards.addAll(discardPile);

        hand.clear();
        discardPile.clear();

        System.out.println("Pachetul a fost reîntregit: " + cards.size() + " cărți.");
    }
}