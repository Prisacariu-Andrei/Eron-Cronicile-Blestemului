package Entity.Player;

import Entity.Items.Card; // Importăm noua clasă Card
import java.util.ArrayList;
import java.util.Collections;

public class Inventory {
    // Pachetul de carti (Deck)
    public ArrayList<Card> cards = new ArrayList<>();
    // Pachetul de rune (Pasive) - Momentan tot de tip Card, pana vei face clasa Rune
    public ArrayList<Card> runes = new ArrayList<>();
    // Mâna curentă (Max 8)
    public ArrayList<Card> hand = new ArrayList<>();
    // Cărțile folosite (Discard)
    public ArrayList<Card> discardPile = new ArrayList<>();

    // public final int MAX_RUNES = 5;

    public void addItem(Card card) {
        // Presupunem că ai o proprietate boolean isRune în clasa Card
        // Dacă nu o ai încă, această metodă va funcționa oricum pentru cărți normale
        //if (card.isRune) {
        //    if (runes.size() < MAX_RUNES) {
        //        runes.add(card);
        //        System.out.println("Runa echipată: " + card.name);
        //    }
        //} else {
            cards.add(card);
            System.out.println("Carte adăugată în pachet: " + card.name);
        //}
    }

    public void addCardToDeck(String cardName) {
        Card newCard = CardManager.createCard(cardName);
        if (newCard != null) {
            cards.add(newCard);
        }
    }

    public void drawCard() {
        if (hand.size() < 8) {
            // Dacă nu mai avem cărți de tras, reciclăm decartările
            if (cards.isEmpty()) {
                refreshDeckFromDiscard();
            }

            // Dacă după refresh tot e gol (nu avem nicio carte în joc), nu tragem nimic
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

    // Folosim această metodă când jucăm o carte
    public void discard(Card card) {
        if (card != null) {
            discardPile.add(card);
        }
    }

    public void prepareForBattle() {
        // Resetăm totul în pachet înainte de amestecare
        cards.addAll(hand);
        hand.clear();
        cards.addAll(discardPile);
        discardPile.clear();

        // Amestecăm doar la începutul bătăliei
        Collections.shuffle(cards);

        // Tragem mâna de start
        for(int i = 0; i < 5; i++) {
            drawCard();
        }
    }

    public void resetDeckAfterBattle() {
        // Adunăm absolut tot ce este împrăștiat
        cards.addAll(hand);
        cards.addAll(discardPile);

        // Curățăm zonele temporare
        hand.clear();
        discardPile.clear();

        System.out.println("Pachetul a fost reîntregit: " + cards.size() + " cărți.");
    }
}