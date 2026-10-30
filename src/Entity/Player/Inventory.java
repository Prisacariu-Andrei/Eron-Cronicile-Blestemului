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

    public final int MAX_HAND_SIZE = 8;

    /** Câte rune poate purta Eron odată (runes.json -> slots). */
    public int runeSlots() { return Entity.Runes.RuneFactory.slots(); }

    public int equippedCount() {
        int n = 0;
        for (Rune r : runes) if (r.isEquipped) n++;
        return n;
    }

    public boolean ownsRune(String id) {
        for (Rune r : runes) if (r.id.equals(id)) return true;
        return false;
    }

    /** Abilitățile (progression.json) se pot folosi doar cu o rună de poveste care le permite (Manuscrisul Străvechi). */
    public boolean canLearnAbilities() {
        for (Rune r : runes) if (r.grantsAbilities) return true;
        return false;
    }

    public boolean runeBagFull() { return runes.size() >= Entity.Runes.RuneFactory.bagSize(); }

    public void addItem(Card card) {
        if (card != null) {
            cards.add(card);
        } else {
            GameExceptionHandler.getInstance().handleWarning("S-a încercat adăugarea unei cărți NULL în pachet prin addItem.", null);
        }
    }

    /** Adaugă runa în rucsac (o singură rună de fiecare fel). Fals dacă e null, deja deținută sau rucsacul e plin. */
    public boolean addRune(Rune rune) {
        if (rune == null) {
            GameExceptionHandler.getInstance().handleWarning("S-a încercat adăugarea unei rune de tip NULL.", null);
            return false;
        }
        if (ownsRune(rune.id) || runeBagFull()) return false;
        runes.add(rune);
        return true;
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
            for (Card c : cards) c.curseCost = 0;

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