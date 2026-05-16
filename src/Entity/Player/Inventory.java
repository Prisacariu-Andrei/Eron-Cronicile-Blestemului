package Entity.Player;

import Entity.Cards.CardFactory;
import Entity.Items.Card;
import Entity.Items.Rune;
import Main.Exception.GameExceptionHandler;
import java.util.ArrayList;
import java.util.Collections;

public class Inventory {
    public final ArrayList<Card> cards = new ArrayList<>();
    public final ArrayList<Rune> runes = new ArrayList<>();
    public final ArrayList<Card> hand = new ArrayList<>();
    public final ArrayList<Card> discardPile = new ArrayList<>();

    public final int MAX_RUNES = 5;
    public final int MAX_HAND_SIZE = 8;

    public void addItem(Card card) {
        if (card != null) {
            cards.add(card);
        } else {
            GameExceptionHandler.getInstance().handleWarning("S-a încercat adăugarea unei cărți NULL în pachet prin addItem.", null);
        }
    }

    public void addRune(Rune rune) {
        if (rune == null) {
            GameExceptionHandler.getInstance().handleWarning("S-a încercat echiparea unei rune de tip NULL.", null);
            return;
        }

        if (runes.size() < MAX_RUNES) {
            runes.add(rune);
            rune.isEquipped = true;
        } else {
            System.out.println("Inventarul de rune este plin!");
        }
    }

    public void addCardToDeck(String cardName, Player player) {
        try {
            Card newCard = CardFactory.createCard(cardName, player);
            cards.add(newCard);
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare la adăugarea cărții '" + cardName + "' în inventar.", e);
        }
    }

    public void drawCard() {
        try {
            if (hand.size() < MAX_HAND_SIZE) {
                if (cards.isEmpty() && !discardPile.isEmpty()) {
                    refreshDeckFromDiscard();
                }

                if (!cards.isEmpty()) {
                    hand.add(cards.removeFirst());
                }
            }
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare structurală întâmpinată în timpul tragerii unei cărți (drawCard).", e);
        }
    }

    private void refreshDeckFromDiscard() {
        try {
            cards.addAll(discardPile);
            discardPile.clear();
            Collections.shuffle(cards);
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare la reîmprospătarea pachetului din decartare.", e);
        }
    }

    public void discard(Card card) {
        if (card != null) {
            discardPile.add(card);
        }
    }

    public void prepareForBattle() {
        try {
            cards.addAll(hand);
            hand.clear();
            cards.addAll(discardPile);
            discardPile.clear();

            Collections.shuffle(cards);

            for (int i = 0; i < MAX_HAND_SIZE; i++) {
                drawCard();
            }
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare la pregătirea pachetului înainte de luptă.", e);
        }
    }

    public void resetDeckAfterBattle() {
        try {
            cards.addAll(hand);
            cards.addAll(discardPile);

            hand.clear();
            discardPile.clear();
        } catch (Exception e) {
            GameExceptionHandler.getInstance().handleWarning("Eroare la resetarea pachetului după finalizarea bătăliei.", e);
        }
    }
}